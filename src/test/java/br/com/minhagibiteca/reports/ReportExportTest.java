package br.com.minhagibiteca.reports;

import br.com.minhagibiteca.reports.application.*;
import br.com.minhagibiteca.reports.infrastructure.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportExportTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"=1+1"," +1+1","-2+1","@SUM(1)","\t=1+1","\n=1+1"})
    void neutralizesEverySpreadsheetFormulaPrefix(String value) {
        var sample=sample(1);
        var data=new ReportData(sample.summary(),sample.entries(),List.of(value),sample.emittedAt());
        assertThat(new String(new ReportCsvExporter().export(data,false),StandardCharsets.UTF_8)).contains("\"Filtro\";\"'"+value+"\"");
    }
    private ReportData sample(int count) {
        var entries=java.util.stream.IntStream.range(0,count).mapToObj(i -> new ReportData.Entry(UUID.randomUUID(),
                i==0?"=HYPERLINK(\"https://invalid\"); João, ação e coleção":"Edição "+i+" - "+"Uma história muito longa de heróis ".repeat(5),
                "1",null,"Coleção de teste","Editora brasileira","João (Roteirista, Desenhista)","Herói",i%2==0?"Físico":"Digital",false,
                100,25,new BigDecimal("15.50"),LocalDate.of(2026,1,1),1,"Estante A")).toList();
        return new ReportData(new ReportData.Summary(count,(count+1)/2,count/2,0,count,new BigDecimal("15.50").multiply(BigDecimal.valueOf(count)),count,0,
                count*100L,count*25L,count),new PageImpl<>(entries),List.of("Toda a coleção (sem filtros)"),ZonedDateTime.of(2026,10,10,12,30,0,0,ZoneId.of("America/Sao_Paulo")));
    }
    @Test void protectsCsvFromFormulasAndEscapesTextAndUnknownValues() {
        String csv=new String(new ReportCsvExporter().export(sample(1),true),StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\ufeff").contains("\"'=HYPERLINK(\"\"https://invalid\"\"); João, ação e coleção\"", "\"15,50\"", "\"Não informado\"", "\r\n");
        String summary=new String(new ReportCsvExporter().export(sample(1),false),StandardCharsets.UTF_8);
        assertThat(summary).contains("\"Páginas restantes\";\"75\"", "\"Filtro\";\"Toda a coleção (sem filtros)\"");
    }
    @Test void createsMultiPageUnicodePdfWithoutDroppingLongText() throws Exception {
        var bytes=new ReportPdfExporter().export(sample(22),true);
        try(var doc=Loader.loadPDF(bytes)) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(3);
            String text=new PDFTextStripper().getText(doc);
            assertThat(text).contains("João, ação e coleção", "Edição 21", "Páginas restantes: 75", "Página 1 de", "Estante A");
        }
        var output=Path.of("target/report-preview");Files.createDirectories(output);
        Files.write(output.resolve("catalogo.pdf"),bytes);
        Files.write(output.resolve("resumo.pdf"),new ReportPdfExporter().export(sample(22),false));
    }
    @Test void rejectsInvalidFiltersAndExcessExportWithoutSilentTruncation() {
        assertThatThrownBy(() -> filter("INVALID",null,null)).isInstanceOf(ReportFilter.InvalidReport.class);
        assertThatThrownBy(() -> filter("ALL",LocalDate.now(),null)).isInstanceOf(ReportFilter.InvalidReport.class);
        assertThatThrownBy(() -> filter("ALL",LocalDate.now(),LocalDate.now().minusDays(1))).isInstanceOf(ReportFilter.InvalidReport.class);
        var queries=mock(ReportQueries.class);var owner=UUID.randomUUID();var filter=filter("ALL",null,null);
        when(queries.summary(owner,filter)).thenReturn(new ReportData.Summary(10001,0,0,10001,0,BigDecimal.ZERO,0,0,0,0,0));
        var service=new ReportService(queries);
        assertThatThrownBy(() -> service.export(owner,filter,true)).isInstanceOf(ReportFilter.InvalidReport.class).hasMessageContaining("10.000");
        verify(queries,never()).entries(any(),any(),anyLong(),anyInt());
        assertThat(service.export(owner,filter,false).summary().editions()).isEqualTo(10001);
    }
    private ReportFilter filter(String media,LocalDate start,LocalDate end) {return new ReportFilter("",null,null,null,null,null,media,"ALL",start,end);}
}
