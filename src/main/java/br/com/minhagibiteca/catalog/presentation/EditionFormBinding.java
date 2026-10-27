package br.com.minhagibiteca.catalog.presentation;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice(assignableTypes={EditionController.class,AmazonImportController.class})
public class EditionFormBinding {
    @InitBinder("editionForm")
    void bind(WebDataBinder binder) {
        binder.setAllowedFields("title","editorialNumber","variant","isbn","publicationYear","format","notes","publicationType","mediaType","coverPrice","referencePaidPrice","publisherId","seriesId","workIds","creatorIds","characterIds","purchaseDate","totalPages","pagesRead");
        binder.registerCustomEditor(BigDecimal.class,new PropertyEditorSupport() {
            @Override public void setAsText(String text) {
                if(text==null || text.isBlank()) { setValue(null);return; }
                String value=text.trim();
                if(!value.matches("-?[0-9]+([.,][0-9]+)?")) throw new IllegalArgumentException("Valor monetário inválido.");
                setValue(new BigDecimal(value.replace(',','.')));
            }
            @Override public String getAsText() { return getValue()==null?"":((BigDecimal)getValue()).toPlainString().replace('.',','); }
        });
    }
}
