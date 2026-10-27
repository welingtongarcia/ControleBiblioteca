package br.com.minhagibiteca.catalog;

import br.com.minhagibiteca.catalog.application.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AmazonImportServiceTest {
    AmazonPageProvider page=mock(AmazonPageProvider.class);
    IsbnLookupService isbn=mock(IsbnLookupService.class);
    UUID owner=UUID.randomUUID();
    @Test void canonicalizesExampleAndValidatesIsbnInsteadOfAssumingEveryAsinIsAnIsbn() {
        var link=AmazonProductLink.parse("https://www.amazon.com.br/Thorgal-Cl%C3%A1ssica-Jean-Van-Hamme/dp/655448244X/ref=asc_df_655448244X?tag=tracking#fragment");
        assertThat(link.url()).isEqualTo("https://www.amazon.com.br/dp/655448244X");
        assertThat(link.isbn()).isEqualTo("9786554482448");
        assertThat(AmazonProductLink.parse("https://amazon.com.br/gp/product/B012345678?foo=bar").isbn()).isNull();
        assertThat(AmazonProductLink.parse("https://amazon.com.br/dp/6554482441").isbn()).isNull();
    }
    @Test void rejectsUnsafeAndUnsupportedUrlsBeforeAnyNetworkRequest() {
        var service=new AmazonImportService(page,isbn);
        for(String link:List.of("http://www.amazon.com.br/dp/655448244X","https://amazon.com.br.evil.test/dp/655448244X","https://www.amazon.com.br@127.0.0.1/dp/655448244X","https://127.0.0.1/dp/655448244X","https://amazon.com.br:443/dp/655448244X","https://amzn.to/test","file:///etc/passwd","https://amazon.com.br/dp/%36655448244X","https://amazon.com.br/a%2fb/dp/655448244X","https://amazon.com.br/dp/655448244X/../../admin","https://amazon.com.br/dp/655448244X\n"))
            assertThatThrownBy(()->service.search(owner,link)).isInstanceOf(AmazonImportService.ImportException.class);
        verifyNoInteractions(page,isbn);
    }
    @Test void previewsPageWithoutCallingFallbackAndCachesCanonicalUrlOnlyForItsOwner() {
        var product=new AmazonProductLink("655448244X");
        var metadata=new AmazonMetadata(product.isbn(),"Thorgal",2025,420,"Capa dura",List.of("Editora"),List.of("Autor"),null,null,"Amazon Brasil",product.url());
        when(page.lookup(product)).thenReturn(Optional.of(metadata));
        var clock=mock(Clock.class);when(clock.instant()).thenReturn(Instant.parse("2026-10-27T12:00:00Z"));
        var service=new AmazonImportService(page,isbn,clock);
        var preview=service.search(owner,product.url()+"?tracking=private");
        assertThat(preview.productUrl()).doesNotContain("tracking");
        assertThat(service.search(owner,product.url()).token()).isEqualTo(preview.token());
        assertThat(service.select(owner,preview.token().toString()).metadata()).isEqualTo(metadata);
        assertThatThrownBy(()->service.select(UUID.randomUUID(),preview.token().toString())).isInstanceOf(AmazonImportService.ImportException.class);
        assertThatThrownBy(()->service.search(UUID.randomUUID(),product.url())).isInstanceOf(AmazonImportService.ImportException.class).hasMessageContaining("Aguarde");
        when(clock.instant()).thenReturn(Instant.parse("2026-10-27T12:11:00Z"));
        assertThatThrownBy(()->service.select(owner,preview.token().toString())).isInstanceOf(AmazonImportService.ImportException.class).hasMessageContaining("expirada");
        verify(page,times(1)).lookup(product);verifyNoInteractions(isbn);
    }
    @Test void fallsBackByValidatedCanonicalIsbnWithoutInventingPageCounts() {
        var product=new AmazonProductLink("655448244X");
        when(page.lookup(product)).thenThrow(new AmazonImportService.ImportException("Bloqueado"));
        when(isbn.lookup(product.isbn())).thenReturn(Optional.of(new IsbnMetadata(product.isbn(),"Thorgal",2025,List.of("Editora"),List.of("Autor"),false)));
        var preview=new AmazonImportService(page,isbn).search(owner,product.url());
        assertThat(preview.metadata().title()).isEqualTo("Thorgal");assertThat(preview.metadata().pages()).isNull();
        assertThat(preview.metadata().sourceName()).isEqualTo("Open Library");
        assertThat(preview.message()).contains("não da página Amazon");
    }
    @Test void unavailableProvidersStillOfferIdentifiedIsbnAndManualRegistration() {
        var product=new AmazonProductLink("655448244X");when(page.lookup(product)).thenReturn(Optional.empty());
        when(isbn.lookup(product.isbn())).thenThrow(new IsbnLookupService.LookupException("Sem chave"));
        var preview=new AmazonImportService(page,isbn).search(owner,product.url());
        assertThat(preview.metadata().isbn()).isEqualTo(product.isbn());assertThat(preview.metadata().title()).isNull();
        assertThat(preview.message()).contains("somente o ISBN");
    }
    @Test void kindleAsinDoesNotTriggerIsbnLookupOrInventMetadata() {
        var product=new AmazonProductLink("B012345678");when(page.lookup(product)).thenReturn(Optional.empty());
        var preview=new AmazonImportService(page,isbn).search(owner,product.url());
        assertThat(preview.metadata().isbn()).isNull();assertThat(preview.message()).contains("ASIN não é um ISBN");
        verifyNoInteractions(isbn);
    }
}
