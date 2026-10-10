package br.com.minhagibiteca.backup;

import br.com.minhagibiteca.backup.presentation.BackupUploadAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import static org.assertj.core.api.Assertions.*;

class BackupUploadAdviceTest {
    @Test void oversizedBackupReturnsFriendlyFlashEvenBeforeHandlerSelection() {
        var request=new MockHttpServletRequest("POST","/backup/restaurar");var flash=new RedirectAttributesModelMap();
        assertThat(new BackupUploadAdvice().tooLarge(request,new MockHttpServletResponse(),new ExtendedModelMap(),flash)).isEqualTo("redirect:/backup");
        assertThat(flash.getFlashAttributes().get("backupError")).isEqualTo("O ZIP excede o limite de 250 MB.");
    }
    @Test void otherOversizedUploadsHaveAccessibleErrorInsteadOfWhitelabel() {
        var response=new MockHttpServletResponse();var model=new ExtendedModelMap();
        assertThat(new BackupUploadAdvice().tooLarge(new MockHttpServletRequest("POST","/edicoes/capa"),response,model,new RedirectAttributesModelMap())).isEqualTo("errors/upload");
        assertThat(response.getStatus()).isEqualTo(413);assertThat(model).containsKey("navigation");
    }
}
