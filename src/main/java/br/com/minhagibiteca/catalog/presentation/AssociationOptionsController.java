package br.com.minhagibiteca.catalog.presentation;

import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser;
import br.com.minhagibiteca.catalog.application.AssociationOptionsService;
import br.com.minhagibiteca.catalog.application.AssociationOptionsService.Kind;
import br.com.minhagibiteca.catalog.domain.CreatorFunction;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class AssociationOptionsController {
    private final AssociationOptionsService options;
    private final CurrentAuthenticatedUser user;
    public AssociationOptionsController(AssociationOptionsService options, CurrentAuthenticatedUser user) { this.options = options; this.user = user; }
    @GetMapping("/edicoes/vinculos/{kind}")
    public ResponseEntity<?> search(@PathVariable Kind kind, @RequestParam(defaultValue="") String q,
            @RequestParam(defaultValue="0") int page, @RequestParam(required=false) CreatorFunction function) {
        try { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(options.search(user.id(), kind, q, function, page)); }
        catch (IllegalArgumentException error) { return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(java.util.Map.of("message", error.getMessage())); }
    }
}
