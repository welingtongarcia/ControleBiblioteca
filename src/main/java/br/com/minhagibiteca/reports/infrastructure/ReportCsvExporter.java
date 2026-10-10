package br.com.minhagibiteca.reports.infrastructure;

import br.com.minhagibiteca.reports.application.ReportData;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public class ReportCsvExporter {
    public byte[] export(ReportData data,boolean catalog) {
        var out=new StringBuilder("\ufeff");
        row(out,"Relatório",catalog?"Catálogo do acervo":"Resumo da coleção");
        row(out,"Emitido em",data.emissionLabel());
        for(String filter:data.filters()) row(out,"Filtro",filter);
        row(out,"Origem dos valores","Preço pago cadastrado na HQ; não inclui novamente valores dos exemplares nem valor de mercado.");
        if(catalog) {
            row(out,"Título","Número","ISBN","Coleção","Editora","Criadores e funções gerais","Personagens","Suporte","Leitura","Páginas","Páginas lidas","Páginas restantes","Preço pago (R$)","Data da compra","Exemplares físicos","Localizações");
            for(var e:data.entries()) row(out,e.title(),e.number(),e.isbn(),e.series(),e.publisher(),e.creators(),e.characters(),e.media(),e.readingLabel(),
                    e.totalPages(),e.totalPages()==null?null:e.pagesRead(),e.pagesUnread(),decimal(e.paid()),e.purchaseDate(),e.copies(),e.locations());
        } else {
            row(out,"Indicador","Valor"); var s=data.summary();
            row(out,"Edições",s.editions()); row(out,"Edições físicas",s.physical()); row(out,"Edições digitais",s.digital());
            row(out,"Suporte não informado",s.unknownMedia()); row(out,"Exemplares físicos",s.copies());
            row(out,"Total gasto registrado (R$)",s.editions()>0&&s.priced()==0?"Não informado":decimal(s.paid()));
            row(out,"Edições sem preço",s.unpriced()); row(out,"Edições lidas",s.read());
            row(out,"Páginas informadas",s.totalPages()); row(out,"Páginas lidas",s.pagesRead()); row(out,"Páginas restantes",s.pagesUnread());
            row(out,"Edições sem páginas",s.unpaged());
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }
    private Object decimal(java.math.BigDecimal amount) { return amount==null?null:amount.toPlainString().replace('.',','); }
    private void row(StringBuilder out,Object... cells) {
        for(int i=0;i<cells.length;i++) {
            if(i>0) out.append(';');
            String value=cells[i]==null?"Não informado":cells[i].toString();
            // Quoted CSV alone does not stop spreadsheet formula execution.
            String check=value.stripLeading();
            if((!check.isEmpty()&&"=+-@".indexOf(check.charAt(0))>=0) || value.startsWith("\t") || value.startsWith("\r") || value.startsWith("\n")) value="'"+value;
            out.append('"').append(value.replace("\"","\"\"")).append('"');
        }
        out.append("\r\n");
    }
}
