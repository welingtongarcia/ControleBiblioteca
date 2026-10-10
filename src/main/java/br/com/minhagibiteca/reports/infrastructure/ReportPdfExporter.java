package br.com.minhagibiteca.reports.infrastructure;

import br.com.minhagibiteca.reports.application.ReportData;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class ReportPdfExporter {
    public byte[] export(ReportData data,boolean catalog) throws IOException {
        try(var doc=new PDDocument();var source=new ClassPathResource("fonts/DejaVuSans.ttf").getInputStream();var out=new ByteArrayOutputStream()) {
            var font=PDType0Font.load(doc,source);
            doc.getDocumentInformation().setTitle(catalog?"Catálogo do acervo":"Resumo da coleção");
            doc.getDocumentInformation().setAuthor("Minha Gibiteca");
            try(var writer=new Layout(doc,font,data.emissionLabel(),catalog?"Catálogo do acervo":"Resumo da coleção")) {
                writer.heading("Filtros utilizados");
                for(var filter:data.filters()) writer.paragraph(filter,10);
                writer.heading("Sua coleção em números");
                var s=data.summary();
                writer.paragraph("Edições: "+s.editions()+"   |   Exemplares físicos: "+s.copies(),12);
                writer.paragraph("Total gasto registrado: "+s.paidLabel(),12);
                writer.paragraph("Preços informados em "+s.priced()+" de "+s.editions()+" edições. Sem preço: "+s.unpriced()+".",10);
                writer.paragraph("Valores vêm do preço pago cadastrado nas HQs. Não somamos novamente os valores dos exemplares. Não é avaliação de mercado.",9);
                writer.paragraph("Edições lidas: "+s.read()+" de "+s.editions(),10);
                writer.paragraph("Páginas informadas: "+s.totalPages()+" | Lidas: "+s.pagesRead()+" | Restantes: "+s.pagesUnread(),10);
                writer.paragraph("Edições sem páginas: "+s.unpaged()+". Totais de páginas incluem somente edições com quantidade informada.",9);
                writer.heading("Distribuição das edições por suporte");
                writer.bar("Físico",s.physical(),s.editions());
                writer.bar("Digital",s.digital(),s.editions());
                writer.bar("Não informado",s.unknownMedia(),s.editions());
                if(catalog) {
                    writer.heading("Catálogo do acervo");
                    if(data.entries().isEmpty()) writer.paragraph("Nenhuma edição encontrada para os filtros selecionados.",11);
                    int index=0;
                    for(var e:data.entries()) {
                        String title=++index+". "+e.title();
                        var details=List.of("Número: "+missing(e.number())+" | ISBN: "+missing(e.isbn()),
                                "Coleção: "+e.series()+" | Editora: "+e.publisher(),
                                "Criadores e funções gerais: "+e.creators(), "Personagens: "+e.characters(),
                                "Suporte: "+e.media()+" | Leitura: "+e.readingLabel()+" | Páginas: "+e.pagesLabel(),
                                "Páginas restantes: "+missing(e.pagesUnread())+" | Preço pago: "+e.paidLabel()+" | Compra: "+e.dateLabel(),
                                "Exemplares físicos: "+e.copies()+" | Localizações: "+e.locations());
                        float block=writer.height(title,12)+22;
                        for(String detail:details) block+=writer.height(detail,10);
                        writer.ensure(Math.min(block,650));
                        writer.heading(title);
                        for(String detail:details) writer.paragraph(detail,10);
                        writer.rule();
                    }
                    writer.paragraph("Funções indicam a atuação geral dos criadores, não créditos específicos por edição.",9);
                }
                if(writer.replacedGlyph) writer.paragraph("Alguns caracteres sem glifo nesta fonte foram substituídos por ?. O CSV preserva o texto original.",9);
            }
            int count=doc.getNumberOfPages();
            for(int i=0;i<count;i++) try(var cs=new PDPageContentStream(doc,doc.getPage(i),PDPageContentStream.AppendMode.APPEND,true,true)) {
                cs.beginText(); cs.setFont(font,9); cs.newLineAtOffset(44,28); cs.showText("Minha Gibiteca - Página "+(i+1)+" de "+count); cs.endText();
            }
            doc.save(out); return out.toByteArray();
        }
    }
    private static String missing(Object value) { return value==null?"Não informado":value.toString(); }

    private static class Layout implements AutoCloseable {
        private static final float MARGIN=44, WIDTH=PDRectangle.A4.getWidth()-MARGIN*2;
        private final PDDocument document;
        private final PDType0Font font;
        private final String date,title;
        private PDPageContentStream stream;
        private float y;
        private boolean replacedGlyph;
        Layout(PDDocument document,PDType0Font font,String date,String title) throws IOException {
            this.document=document; this.font=font; this.date=date; this.title=title; newPage();
        }
        private void newPage() throws IOException {
            if(stream!=null) stream.close();
            var page=new PDPage(PDRectangle.A4); document.addPage(page); stream=new PDPageContentStream(document,page);
            y=page.getMediaBox().getHeight()-MARGIN;
            text(title,18,MARGIN,y); y-=23; text("Emitido em "+date,9,MARGIN,y); y-=24; rule();
        }
        void ensure(float height) throws IOException { if(y-height<55) newPage(); }
        void heading(String text) throws IOException { ensure(42); y-=8; paragraph(text,12); }
        void paragraph(String text,float size) throws IOException {
            for(var line:wrap(clean(text),size,WIDTH)) { ensure(size+6); text(line,size,MARGIN,y); y-=size+5; }
            y-=4;
        }
        float height(String text,float size) throws IOException { return wrap(clean(text),size,WIDTH).size()*(size+5)+4; }
        void rule() throws IOException { ensure(12); stream.setStrokingColor(new Color(205,210,215)); stream.moveTo(MARGIN,y);stream.lineTo(MARGIN+WIDTH,y);stream.stroke();y-=14; }
        void bar(String label,long value,long total) throws IOException {
            ensure(32); text(label+": "+value,10,MARGIN,y); y-=13;
            stream.setNonStrokingColor(new Color(230,232,235)); stream.addRect(MARGIN,y-5,WIDTH,7);stream.fill();
            stream.setNonStrokingColor(new Color(170,119,10));stream.addRect(MARGIN,y-5,total==0?0:WIDTH*value/total,7);stream.fill();y-=18;
        }
        private void text(String value,float size,float x,float y) throws IOException {
            stream.setNonStrokingColor(new Color(30,40,50));stream.beginText();stream.setFont(font,size);stream.newLineAtOffset(x,y);stream.showText(value);stream.endText();
        }
        private String clean(String value) throws IOException {
            var result=new StringBuilder();
            for(int cp:value.codePoints().toArray()) {
                if(Character.isISOControl(cp)) {result.append(' ');continue;}
                String character=new String(Character.toChars(cp));
                try { font.encode(character); result.append(character); }
                catch(IllegalArgumentException error) {result.append('?'); replacedGlyph=true;}
            }
            return result.toString();
        }
        private List<String> wrap(String value,float size,float width) throws IOException {
            var result=new ArrayList<String>();var line=new StringBuilder();
            // Split by code point when a single token is wider than the page; never truncate.
            for(String word:value.split(" +")) {
                if(word.isEmpty()) continue;
                String candidate=line.isEmpty()?word:line+" "+word;
                if(font.getStringWidth(candidate)/1000*size<=width) {line.setLength(0);line.append(candidate);continue;}
                if(!line.isEmpty()) {result.add(line.toString());line.setLength(0);}
                for(int cp:word.codePoints().toArray()) {
                    var character=new String(Character.toChars(cp));
                    if(font.getStringWidth(line.toString()+character)/1000*size>width&&!line.isEmpty()) {result.add(line.toString());line.setLength(0);}
                    line.append(character);
                }
            }
            if(!line.isEmpty()) result.add(line.toString());
            if(result.isEmpty()) result.add("");
            return result;
        }
        @Override public void close() throws IOException { if(stream!=null) stream.close(); }
    }
}
