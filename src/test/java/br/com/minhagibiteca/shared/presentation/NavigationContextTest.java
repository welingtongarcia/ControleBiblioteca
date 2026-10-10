package br.com.minhagibiteca.shared.presentation;
import static org.assertj.core.api.Assertions.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

class NavigationContextTest {
    @Test void preservesReportFiltersWhenReturningFromAnEditionAndRejectsExternalTargets() {
        String target="/relatorios?q=HQ&creatorFunction=DESENHISTA&reading=UNREAD&mediaType=UNSPECIFIED&type=CATALOG&page=2";
        var context=context("/edicoes/11111111-1111-1111-1111-111111111111/editar",target);
        assertThat(context.returnUrl()).isEqualTo(target);
        assertThat(context.getReturnLabel()).isEqualTo("Relatórios");
        assertThat(context.redirect("/edicoes")).isEqualTo(target);
        assertThat(context("/edicoes/nova","https://example.com"+target).returnUrl()).isEqualTo("/edicoes");
        assertThat(context("/edicoes/nova","/relatorios/pdf").returnUrl()).isEqualTo("/edicoes");
    }
    private static final String SERIES = "11111111-1111-1111-1111-111111111111";
    @ParameterizedTest @ValueSource(strings = {"/dashboard", "/visao-geral"})
    void separatesHomeOverviewAndReturnsOnlyToLocalLanding(String path) {
        var landing = context(path, null);
        assertThat(landing.key()).isEqualTo(path.substring(1));
        assertThat(landing.label()).isEqualTo(path.equals("/dashboard") ? "Início" : "Visão geral");
        var edition = context("/edicoes/" + SERIES + "/editar", path);
        assertThat(edition.returnUrl()).isEqualTo(path);
        assertThat(edition.getReturnLabel()).isEqualTo(landing.label());
        assertThat(edition.redirect("/edicoes/" + SERIES)).isEqualTo(path);
        assertThat(edition.redirect("/edicoes")).isEqualTo(path);
        assertThat(context("/edicoes/nova", "https://example.com" + path).returnUrl()).isEqualTo("/edicoes");
        assertThat(context("/edicoes/nova", path + "/evil").returnUrl()).isEqualTo("/edicoes");
        assertThat(context("/editoras/nova", path).returnUrl()).isEqualTo("/editoras");
    }
    @Test void editionCanReturnOnlyToValidatedFinancePanel() {
        String finance="/financeiro?start=2026-06-01&end=2026-06-10&mediaType=UNSPECIFIED&ranking=QUANTITY&page=2";
        var c=context("/edicoes/"+SERIES+"/editar",finance);
        assertThat(c.returnUrl()).isEqualTo(finance);
        assertThat(c.getReturnLabel()).isEqualTo("Financeiro");
        assertThat(c.redirect("/edicoes/"+SERIES)).isEqualTo(finance);
        assertThat(c.redirect("/edicoes")).isEqualTo(finance);
        assertThat(context("/edicoes/nova","/financeiro/compras/nova").returnUrl()).isEqualTo("/edicoes");
        assertThat(context("/edicoes/nova","//example.com/financeiro").returnUrl()).isEqualTo("/edicoes");
        assertThat(context("/editoras/nova",finance).returnUrl()).isEqualTo("/editoras");
    }
    private NavigationContext context(String path, String back) {
        var request = new MockHttpServletRequest("GET", path); if (back != null) request.setParameter("returnTo", back); return NavigationContext.from(request);
    }
    @ParameterizedTest @ValueSource(strings = {"/edicoes", "/series", "/editoras", "/obras", "/criadores", "/localizacoes"})
    void carriesNearestListQueryAndPage(String path) {
        var request = new MockHttpServletRequest("GET", path); request.setParameter("q", "Batman & Robin"); request.setParameter("page", "3");
        var context = NavigationContext.from(request);
        assertThat(context.pageReturnUrl()).isEqualTo(path + "?q=Batman%20%26%20Robin&page=3");
        assertThat(context.listUrl()).isEqualTo(path);
    }
    @ParameterizedTest @ValueSource(strings = {"https://example.com", "//example.com", "javascript:alert(1)", "/series", "/edicoes/../series", "/%65dicoes", "/edicoes%2F", "/edicoes#fragmento", "/edicoes/nova", "/edicoes?returnTo=https://example.com", "/edicoes?x=1", "/edicoes?page=texto"})
    void ignoresExternalWrongModuleAndNonListReturnTargets(String target) {
        var context = context("/edicoes/nova", target);
        assertThat(context.returnUrl()).isEqualTo("/edicoes");
        assertThat(context.redirect("/edicoes")).isEqualTo("/edicoes");
    }
    @Test void dropsUnknownParametersAndRejectsControlsAndOversizedInput() {
        assertThat(context("/edicoes/nova", "/edicoes?q=HQ&secret=oculto&mediaType=PHYSICAL&order=TITLE_DESC&page=2").returnUrl())
                .isEqualTo("/edicoes?q=HQ&mediaType=PHYSICAL&order=TITLE_DESC&page=2");
        for (String target : new String[]{"/edicoes\r\nLocation:https://example.com", "/edicoes\\evil", "/edicoes?q=" + "a".repeat(2049)})
            assertThat(context("/edicoes/nova", target).returnUrl()).isEqualTo("/edicoes");
        assertThat(context("/edicoes/nova", "/edicoes?q=%0d%0a&page=1").returnUrl()).isEqualTo("/edicoes?page=1");
    }
    @Test void propagatesCatalogContextToDetailsWithoutChangingOtherModules() {
        var context = context("/exemplares/11111111-1111-1111-1111-111111111111/editar", "/edicoes?q=HQ&page=2");
        assertThat(context.key()).isEqualTo("edicoes");
        assertThat(context.redirect("/edicoes")).isEqualTo("/edicoes?q=HQ&page=2");
        var destination = context.redirect("/edicoes/" + SERIES);
        assertThat(URLDecoder.decode(destination.split("returnTo=")[1], StandardCharsets.UTF_8)).isEqualTo(context.returnUrl());
        assertThat(context.redirect("/series")).isEqualTo("/series");
    }
    @Test void keepsMissingModeAndPageOnlyForTheSameSeries() {
        var context = context("/checklist/itens/" + SERIES + "/editar", "/series/" + SERIES + "/faltantes?page=2");
        assertThat(context.key()).isEqualTo("series");
        assertThat(context.parentUrl("/series/" + SERIES + "/checklist")).isEqualTo(context.returnUrl());
        assertThat(context.redirect("/series/" + SERIES + "/checklist")).isEqualTo(context.returnUrl());
        assertThat(context.redirect("/series/22222222-2222-2222-2222-222222222222/checklist")).startsWith("/series/22222222-2222-2222-2222-222222222222/checklist?returnTo=");
        assertThat(context.getListReturnUrl()).isEqualTo("/series");
    }
    @Test void supportsApplicationContextPathAndIgnoresReferer() {
        var request = new MockHttpServletRequest("GET", "/gibi/editoras/nova"); request.setContextPath("/gibi");
        request.addHeader("Referer", "https://example.com");
        assertThat(NavigationContext.from(request).returnUrl()).isEqualTo("/editoras");
    }
}
