package br.com.minhagibiteca.catalog.infrastructure;

import br.com.minhagibiteca.catalog.application.*;
import com.fasterxml.jackson.databind.*;
import java.io.*;
import java.net.*;
import java.text.Normalizer;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.springframework.stereotype.Component;

@Component
public class AmazonPageClient implements AmazonPageProvider {
    @FunctionalInterface interface ConnectionFactory { HttpURLConnection open(URI uri) throws IOException; }
    private final ConnectionFactory connections;
    private final ObjectMapper json=new ObjectMapper();
    public AmazonPageClient() { this(uri->(HttpURLConnection)uri.toURL().openConnection()); }
    AmazonPageClient(ConnectionFactory connections) { this.connections=connections; }

    @Override public Optional<AmazonMetadata> lookup(AmazonProductLink product) {
        HttpURLConnection connection=null;
        try {
            connection=connections.open(URI.create(product.url()));
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(3000); connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent","MinhaBiblioteca/0.1 (consulta de metadados solicitada pelo usuario)");
            connection.setRequestProperty("Accept","text/html");
            connection.setRequestProperty("Accept-Encoding","gzip");
            if(connection.getResponseCode()!=200 || connection.getContentType()==null
                    || !connection.getContentType().toLowerCase(Locale.ROOT).startsWith("text/html")) throw unavailable();
            try(InputStream raw=connection.getInputStream(); InputStream stream="gzip".equalsIgnoreCase(connection.getContentEncoding())?new GZIPInputStream(raw):raw) {
                var body=new ByteArrayOutputStream(); byte[] buffer=new byte[8192]; int size;
                Instant deadline=Instant.now().plusSeconds(12);
                while((size=stream.read(buffer))!=-1) {
                    if(body.size()+size>2*1024*1024 || Instant.now().isAfter(deadline)) throw unavailable();
                    body.write(buffer,0,size);
                }
                return parse(product,Jsoup.parse(new ByteArrayInputStream(body.toByteArray()),null,product.url()));
            }
        } catch(IOException | IllegalArgumentException e) { throw unavailable(); }
        finally { if(connection!=null) connection.disconnect(); }
    }

    Optional<AmazonMetadata> parse(AmazonProductLink product, Document doc) {
        if(!doc.select("#captchacharacters, form[action*=validateCaptcha]").isEmpty()
                || doc.title().toLowerCase(Locale.ROOT).contains("robot check")) return Optional.empty();
        var facts=new LinkedHashMap<String,String>();
        for(Element row:doc.select("#detailBullets_feature_div li, #productDetails_techSpec_section_1 tr, #productDetails_detailBullets_sections1 tr")) {
            Element label=row.selectFirst(".a-text-bold, th");
            if(label==null) continue;
            String key=clean(label.text(),120);
            if(key==null) continue;
            var copy=row.clone(); copy.select(".a-text-bold, th").remove();
            String value=clean(copy.text(),1000);
            facts.putIfAbsent(normalize(key),value==null?null:clean(value.replaceFirst("^:\\s*",""),1000));
        }
        for(Element row:doc.select(".rpi-attribute")) {
            Element label=row.selectFirst(".rpi-attribute-label"),value=row.selectFirst(".rpi-attribute-value");
            if(label!=null && value!=null) facts.putIfAbsent(normalize(label.text()),clean(value.text(),1000));
        }
        JsonNode book=findBook(doc,product);
        String title=text(doc.selectFirst("#productTitle"),240);
        if(title==null && book!=null) title=clean(book.path("name").asText(),240);
        if(title==null || title.isBlank()) return Optional.empty();
        String isbn=validIsbn(fact(facts,"isbn13","isbn10"));
        if(isbn==null && book!=null) isbn=validIsbn(book.path("isbn").asText());
        if(isbn!=null && product.isbn()!=null && !isbn.equals(product.isbn())) throw unavailable();
        if(isbn==null) isbn=product.isbn();
        String publisher=fact(facts,"editora","publisher");
        if(publisher==null && book!=null) publisher=person(book.path("publisher"));
        if(publisher!=null) publisher=clean(publisher.split(";",2)[0],180);
        Integer year=year(fact(facts,"datadapublicacao","publicationdate"));
        if(year==null && book!=null) year=year(book.path("datePublished").asText());
        if(year==null) year=year(fact(facts,"editora","publisher"));
        Integer pages=number(fact(facts,"numerodepaginas","comprimento","printlength"),100000);
        if(pages==null && book!=null) pages=number(book.path("numberOfPages").asText(),100000);
        String format=fact(facts,"acabamento","encadernacao","formato");
        if(format==null && book!=null) {
            String value=book.path("bookFormat").asText();
            if(value.endsWith("Hardcover")) format="Capa dura";
            if(value.endsWith("Paperback")) format="Brochura";
        }
        if(format==null) for(var key:facts.keySet()) if(key.startsWith("capadura")) format="Capa dura"; else if(key.startsWith("capacomum")) format="Brochura";
        var authors=new LinkedHashSet<String>();
        for(Element author:doc.select("#bylineInfo .author .a-link-normal")) {
            String name=text(author,180); if(name!=null && authors.size()<12) authors.add(name);
        }
        if(authors.isEmpty() && book!=null) {
            var author=book.path("author");
            if(author.isArray()) for(JsonNode a:author) { String name=person(a);if(name!=null && authors.size()<12) authors.add(name); }
            else { String name=person(author);if(name!=null) authors.add(name); }
        }
        String summary=text(doc.selectFirst("#bookDescription_feature_div .a-expander-content"),1800);
        if(summary==null && book!=null) summary=clean(Jsoup.parse(book.path("description").asText()).text(),1800);
        String cover=null;var image=doc.selectFirst("#landingImage, #imgBlkFront");
        if(image!=null) { cover=safeCover(image.attr("data-old-hires"));if(cover==null) cover=safeCover(image.attr("src")); }
        if(cover==null && book!=null) {
            var value=book.path("image");cover=safeCover(value.isTextual()?value.asText():value.path("url").asText());
        }
        return Optional.of(new AmazonMetadata(isbn,title,year,pages,clean(format,60),publisher==null?List.of():List.of(publisher),List.copyOf(authors),summary,cover,"Amazon Brasil",product.url()));
    }

    private JsonNode findBook(Document doc,AmazonProductLink product) {
        for(Element script:doc.select("script[type=application/ld+json]")) {
            try { var found=findBook(json.readTree(script.data()),product,0,new int[]{0}); if(found!=null) return found; }
            catch(com.fasterxml.jackson.core.JsonProcessingException ignored) { }
        }
        return null;
    }
    private JsonNode findBook(JsonNode node,AmazonProductLink product,int depth,int[] count) {
        if(node==null || depth>20 || ++count[0]>5000) return null;
        if(node.isObject()) {
            String type=node.path("@type").asText();
            boolean matches=product.asin().equalsIgnoreCase(node.path("sku").asText())
                    || (product.isbn()!=null && product.isbn().equals(validIsbn(node.path("isbn").asText())));
            if(matches && (type.equals("Book") || type.equals("Product"))) return node;
        }
        if(node.isContainerNode()) for(JsonNode child:node) { var found=findBook(child,product,depth+1,count);if(found!=null) return found; }
        return null;
    }
    private static String person(JsonNode n) { return clean(n.isTextual()?n.asText():n.path("name").asText(),180); }
    private static String text(Element element,int max) { return element==null?null:clean(element.text(),max); }
    private static String clean(String value,int max) {
        if(value==null) return null;
        value=value.replaceAll("[\\p{Cf}\\p{Cc}]"," ").replaceAll("\\s+"," ").strip();
        return value.isEmpty()?null:value.substring(0,Math.min(max,value.length()));
    }
    private static String normalize(String value) { return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]",""); }
    private static String fact(Map<String,String> facts,String... keys) { for(String key:keys) if(facts.get(key)!=null) return facts.get(key);return null; }
    private static Integer year(String value) {
        if(value==null) return null;var m=Pattern.compile("\\b(18\\d{2}|19\\d{2}|20\\d{2}|2100)\\b").matcher(value);return m.find()?Integer.valueOf(m.group(1)):null;
    }
    private static Integer number(String value,int max) {
        if(value==null) return null;var m=Pattern.compile("(?<![0-9])[0-9]{1,6}(?![0-9])").matcher(value);
        if(!m.find()) return null;int n=Integer.parseInt(m.group());return n>0 && n<=max?n:null;
    }
    private static String validIsbn(String value) {
        if(value==null) return null;
        try { return IsbnLookupService.canonical(value.replaceAll("[\\p{Cf}\\p{Cc}]","")); }
        catch(IsbnLookupService.LookupException e) { return null; }
    }
    static String safeCover(String value) {
        try {
            if(value==null || value.length()>2048) return null;URI uri=URI.create(value);
            if(!"https".equals(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null || uri.getPort()!=-1
                    || !Set.of("m.media-amazon.com","images-na.ssl-images-amazon.com").contains(uri.getHost())) return null;
            return new URI("https",uri.getHost(),uri.getPath(),null).toASCIIString();
        } catch(URISyntaxException | IllegalArgumentException e) { return null; }
    }
    private static AmazonImportService.ImportException unavailable() { return new AmazonImportService.ImportException("A Amazon não disponibilizou uma ficha válida nesta consulta. Não tentaremos contornar bloqueios; continue manualmente ou use a busca bibliográfica."); }
}
