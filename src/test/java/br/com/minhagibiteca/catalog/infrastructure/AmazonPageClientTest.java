package br.com.minhagibiteca.catalog.infrastructure;

import br.com.minhagibiteca.catalog.application.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class AmazonPageClientTest {
    final AmazonProductLink product=new AmazonProductLink("655448244X");
    @Test void parsesRealisticBrazilianProductBulletsWithUnicodeAndDoesNotImportPrice() {
        var html=Jsoup.parse("""
            <h1 id="productTitle">Thorgal: Série Clássica (Vol. 1)</h1>
            <div id="bylineInfo"><span class="author"><a class="a-link-normal">Jean Van Hamme</a></span></div>
            <div id="detailBullets_feature_div"><ul>
            <li><span class="a-text-bold">ISBN-13 ‏ : </span>‎ 978-6554482448</li>
            <li><span class="a-text-bold">Editora ‏ : </span>‎ Pipoca e Nanquim; 1ª edição (13 março 2025)</li>
            <li><span class="a-text-bold">Número de páginas ‏ : </span>‎ 420 páginas</li>
            <li><span class="a-text-bold">Capa dura ‏ : </span>‎ 420 páginas</li></ul></div>
            <div id="bookDescription_feature_div"><div class="a-expander-content">Uma aventura <b>ilustrada</b>.</div></div>
            <img id="landingImage" src="https://m.media-amazon.com/images/I/capa.jpg?tracking=private">
            <span class="a-price">R$ 123,45</span>
            """);
        var book=new AmazonPageClient().parse(product,html).orElseThrow();
        assertThat(book.isbn()).isEqualTo("9786554482448");assertThat(book.pages()).isEqualTo(420);
        assertThat(book.publishers()).containsExactly("Pipoca e Nanquim");assertThat(book.authors()).containsExactly("Jean Van Hamme");
        assertThat(book.format()).isEqualTo("Capa dura");assertThat(book.year()).isEqualTo(2025);
        assertThat(book.summary()).isEqualTo("Uma aventura ilustrada.");assertThat(book.coverUrl()).isEqualTo("https://m.media-amazon.com/images/I/capa.jpg");
        assertThat(book.toString()).doesNotContain("123,45","tracking=private");
    }
    @Test void parsesMatchingJsonLdAndRejectsRecommendationOrConflictingEdition() {
        String data="""
            <script type="application/ld+json">{"@graph":[{"@type":"Book","isbn":"9780140328721","name":"Outra edição"},
            {"@type":"Book","isbn":"9786554482448","name":"Thorgal","numberOfPages":420,"datePublished":"2025-03-13","bookFormat":"https://schema.org/Hardcover","author":[{"name":"Autor"}],"publisher":{"name":"Editora"},"image":"javascript:bad()"}]}</script>
            """;
        var book=new AmazonPageClient().parse(product,Jsoup.parse(data)).orElseThrow();
        assertThat(book.title()).isEqualTo("Thorgal");assertThat(book.pages()).isEqualTo(420);assertThat(book.coverUrl()).isNull();
        assertThatThrownBy(()->new AmazonPageClient().parse(product,Jsoup.parse("<h1 id=productTitle>Outra edição</h1><div id=detailBullets_feature_div><li><b class=a-text-bold>ISBN-13:</b>9780140328721</li></div>"))).isInstanceOf(AmazonImportService.ImportException.class);
    }
    @Test void recognizesBlockedAndIncompletePagesWithoutParsingGenericTitle() {
        var client=new AmazonPageClient();
        for(String html:List.of("<title>Amazon.com.br</title>","<h1 id=productTitle>Thorgal</h1><input id=captchacharacters>","<title>Robot Check</title><h1 id=productTitle>Thorgal</h1>"))
            assertThat(client.parse(product,Jsoup.parse(html))).isEmpty();
        for(String url:List.of("javascript:alert(1)","http://m.media-amazon.com/a.jpg","https://m.media-amazon.com.evil.test/a.jpg","https:/relative","https://user:pass@m.media-amazon.com/a.jpg"))
            assertThat(AmazonPageClient.safeCover(url)).isNull();
    }
    @Test void boundsUntrustedFieldsAndRejectsImpossiblePages() {
        var doc=Jsoup.parse("<h1 id=productTitle>"+"x".repeat(500)+"</h1><div id=detailBullets_feature_div><li><b class=a-text-bold>Número de páginas:</b>100001 páginas</li></div>");
        var book=new AmazonPageClient().parse(product,doc).orElseThrow();
        assertThat(book.title()).hasSize(240);assertThat(book.pages()).isNull();
    }
    @Test void requestsOnlyCanonicalHttpsWithoutRedirectsAndDecodesGzipWithinLimit() throws Exception {
        var out=new ByteArrayOutputStream();try(var gzip=new GZIPOutputStream(out)){gzip.write("<h1 id=productTitle>Thorgal</h1>".getBytes(StandardCharsets.UTF_8));}
        var connection=new FakeConnection(200,"text/html; charset=UTF-8","gzip",out.toByteArray());
        var client=new AmazonPageClient(uri->{assertThat(uri.toString()).isEqualTo(product.url());return connection;});
        assertThat(client.lookup(product)).isPresent();assertThat(connection.getInstanceFollowRedirects()).isFalse();
        assertThat(connection.getConnectTimeout()).isEqualTo(3000);assertThat(connection.getReadTimeout()).isEqualTo(5000);
        assertThat(connection.getRequestProperty("Cookie")).isNull();assertThat(connection.disconnected).isTrue();
    }
    @Test void refusesRedirectErrorNonHtmlAndOversizedResponseWithoutLeakingContent() {
        for(var connection:List.of(new FakeConnection(302,"text/html",null,new byte[0]),new FakeConnection(503,"text/html",null,"private-body".getBytes()),new FakeConnection(200,"application/json",null,new byte[0]),new FakeConnection(200,"text/html",null,new byte[2*1024*1024+1]))) {
            assertThatThrownBy(()->new AmazonPageClient(uri->connection).lookup(product)).isInstanceOf(AmazonImportService.ImportException.class).hasMessageNotContaining("private-body");
            assertThat(connection.disconnected).isTrue();
        }
    }
    static class FakeConnection extends HttpURLConnection {
        final int status;final String type,encoding;final byte[] body;boolean disconnected;
        FakeConnection(int status,String type,String encoding,byte[] body) { super(url());this.status=status;this.type=type;this.encoding=encoding;this.body=body; }
        static URL url(){try{return URI.create("https://www.amazon.com.br").toURL();}catch(Exception e){throw new IllegalStateException(e);}}
        @Override public int getResponseCode(){return status;}
        @Override public String getContentType(){return type;}
        @Override public String getContentEncoding(){return encoding;}
        @Override public InputStream getInputStream(){return new ByteArrayInputStream(body);}
        @Override public void disconnect(){disconnected=true;}
        @Override public boolean usingProxy(){return false;}
        @Override public void connect(){}
    }
}
