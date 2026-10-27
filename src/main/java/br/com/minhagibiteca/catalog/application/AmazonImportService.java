package br.com.minhagibiteca.catalog.application;

import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AmazonImportService {
    public record Preview(UUID token, String productUrl, AmazonMetadata metadata, String message) { }
    private record Cached(UUID owner, Preview preview, Instant expires) { }
    private final AmazonPageProvider page;
    private final IsbnLookupService isbn;
    private final Clock clock;
    private final Map<UUID,Cached> cache=new LinkedHashMap<>();
    private Instant nextAllowed=Instant.MIN;
    @Autowired public AmazonImportService(AmazonPageProvider page, IsbnLookupService isbn) { this(page,isbn,Clock.systemUTC()); }
    public AmazonImportService(AmazonPageProvider page, IsbnLookupService isbn, Clock clock) { this.page=page;this.isbn=isbn;this.clock=clock; }

    public Preview search(UUID owner, String input) {
        Objects.requireNonNull(owner);
        var product=AmazonProductLink.parse(input);
        synchronized(cache) {
            purge();
            for(var entry:cache.values()) if(entry.owner().equals(owner) && entry.preview().productUrl().equals(product.url())) return entry.preview();
            if(clock.instant().isBefore(nextAllowed)) throw new ImportException("Aguarde um instante antes de consultar outro link. Você pode continuar o cadastro manualmente.");
            nextAllowed=clock.instant().plusSeconds(2);
        }
        AmazonMetadata found=null;
        String warning="";
        try { found=page.lookup(product).orElse(null); }
        catch(ImportException e) { warning="A página da Amazon não pôde ser consultada. "; }
        if(found==null && product.isbn()!=null) {
            try {
                var fallback=isbn.lookup(product.isbn());
                if(fallback.isPresent()) {
                    var book=fallback.get();
                    found=new AmazonMetadata(book.isbn(),book.title(),book.year(),null,null,book.publishers(),book.authors(),null,null,book.sourceName(),book.sourceUrl());
                    warning+="Os dados abaixo vieram da busca bibliográfica pelo ISBN, não da página Amazon. ";
                }
            } catch(IsbnLookupService.LookupException e) { warning+="A busca bibliográfica também não retornou dados nesta consulta. "; }
        }
        if(found==null) found=new AmazonMetadata(product.isbn(),null,null,null,null,List.of(),List.of(),null,null,"Link Amazon",product.url());
        String message=warning+(found.title()==null
                ? (found.isbn()==null ? "Não foi possível obter os dados deste produto. O ASIN não é um ISBN válido. Continue manualmente ou pesquise pelo título."
                : "Identificamos somente o ISBN. Não foram encontrados outros dados; você pode aplicar o ISBN e completar manualmente.")
                : "Confira a edição e os campos encontrados. Aplicar sugestões preenche somente campos vazios e não salva o cadastro.");
        var preview=new Preview(UUID.randomUUID(),product.url(),found,message);
        synchronized(cache) {
            purge(); if(cache.size()>=200) cache.remove(cache.keySet().iterator().next());
            cache.put(preview.token(),new Cached(owner,preview,clock.instant().plusSeconds(600)));
        }
        return preview;
    }

    public Preview select(UUID owner,String token) {
        synchronized(cache) {
            purge();
            try {
                var entry=cache.get(UUID.fromString(token));
                if(entry!=null && entry.owner().equals(owner)) return entry.preview();
            } catch(IllegalArgumentException | NullPointerException ignored) { }
            throw new ImportException("Prévia inválida ou expirada. Busque o link novamente; seus dados foram preservados.");
        }
    }
    private void purge() { cache.entrySet().removeIf(e->!e.getValue().expires().isAfter(clock.instant())); }
    public static class ImportException extends RuntimeException { public ImportException(String message) { super(message); } }
}
