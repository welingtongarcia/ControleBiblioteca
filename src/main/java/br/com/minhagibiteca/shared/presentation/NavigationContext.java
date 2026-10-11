package br.com.minhagibiteca.shared.presentation;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.web.util.UriComponentsBuilder;

/** Contexto de apresentação, sem acesso a dados privados nem confiança em Referer. */
public record NavigationContext(String key, String label, String listUrl, String returnUrl, String pageReturnUrl) {
    private static final String UUID_PATTERN = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final Map<String, String> LABELS = Map.ofEntries(Map.entry("edicoes", "Catálogo de edições"), Map.entry("series", "Séries e checklist"),
            Map.entry("localizacoes", "Localizações"), Map.entry("editoras", "Editoras"), Map.entry("obras", "Obras"), Map.entry("criadores", "Criadores"),
            Map.entry("dashboard", "Início"), Map.entry("financeiro", "Financeiro"), Map.entry("personagens", "Personagens"),
            Map.entry("visao-geral", "Visão geral"), Map.entry("relatorios", "Relatórios"), Map.entry("backup", "Backup e migração"), Map.entry("desejos", "Lista de desejos"));

    public static NavigationContext from(HttpServletRequest request) {
        String path = request.getServletPath();
        // MockMvc e alguns despachantes fornecem apenas requestURI.
        if (path == null || path.isEmpty()) path = request.getRequestURI().substring(request.getContextPath().length());
        String key = Arrays.stream(path.split("/")).filter(part -> !part.isEmpty()).findFirst().orElse("dashboard");
        if (key.equals("exemplares")) key = "edicoes";
        if (key.equals("checklist")) key = "series";
        if (!LABELS.containsKey(key)) key = "dashboard";
        String list = "/" + key;
        String back = canonical(request.getParameter("returnTo"), key);
        // O Financeiro edita os próprios dados da edição; aceita só o painel local validado.
        if (back == null && key.equals("edicoes")) back = canonical(request.getParameter("returnTo"), "financeiro");
        if (back == null && key.equals("edicoes")) back = canonical(request.getParameter("returnTo"), "dashboard");
        if (back == null && key.equals("edicoes")) back = canonical(request.getParameter("returnTo"), "visao-geral");
        if (back == null && key.equals("edicoes")) back = canonical(request.getParameter("returnTo"), "relatorios");
        if (back == null) back = list;
        String page = back;
        if (request.getMethod().equals("GET") && allowedPath(path, key)) {
            var uri = UriComponentsBuilder.fromPath(path);
            for (String name : parameters(path)) {
                String value = request.getParameter(name);
                if (value != null) uri.queryParam(name, value);
            }
            String candidate = canonical(uri.build().encode().toUriString(), key);
            if (candidate != null) page = candidate;
        }
        return new NavigationContext(key, LABELS.get(key), list, back, page);
    }
    private static boolean allowedPath(String path, String key) {
        return path != null && (path.equals("/" + key) || (key.equals("series") && path.matches("/series/" + UUID_PATTERN + "/(checklist|faltantes)")));
    }
    private static List<String> parameters(String path) {
        if (path.equals("/desejos")) return List.of("q", "status", "page");
        if (path.equals("/edicoes")) return List.of("q", "publisherId", "seriesId", "format", "mediaType", "publicationType", "order", "page");
        if (path.equals("/financeiro")) return List.of("start", "end", "seriesId", "creatorId", "characterId", "publisherId", "mediaType", "ranking", "page");
        if (path.equals("/relatorios")) return List.of("q", "start", "end", "seriesId", "creatorId", "creatorFunction", "characterId", "publisherId", "mediaType", "reading", "type", "page");
        if (path.matches("/series/" + UUID_PATTERN + "/(checklist|faltantes)")) return List.of("page");
        return List.of("q", "page");
    }
    static String canonical(String destination, String key) {
        if (destination == null || destination.length() > 2048 || destination.chars().anyMatch(c -> c < 32 || c == 127) || destination.contains("\\")) return null;
        try {
            URI uri = URI.create(destination);
            if (uri.isAbsolute() || uri.getRawAuthority() != null || uri.getRawFragment() != null || !allowedPath(uri.getRawPath(), key)) return null;
            var builder = UriComponentsBuilder.fromPath(uri.getRawPath());
            Set<String> seen = new HashSet<>();
            if (uri.getRawQuery() != null) for (String pair : uri.getRawQuery().split("&")) {
                String[] parts = pair.split("=", 2);
                String name = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                String value = parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
                if (!parameters(uri.getRawPath()).contains(name) || !seen.add(name) || value.isBlank() || value.length() > 240 || value.chars().anyMatch(c -> c < 32 || c == 127)) continue;
                if (name.equals("page") && (Integer.parseInt(value) < 0)) continue;
                if (Set.of("publisherId","seriesId","creatorId","characterId").contains(name) && !value.matches(UUID_PATTERN)) continue;
                if (name.equals("ranking") && !Set.of("AMOUNT","QUANTITY").contains(value)) continue;
                if (name.equals("status") && !Set.of("PENDING","ACQUIRED","ALL").contains(value)) continue;
                if (name.equals("start") || name.equals("end")) java.time.LocalDate.parse(value);
                if (name.equals("mediaType") && !Set.of("PHYSICAL", "DIGITAL").contains(value) && !(Set.of("financeiro","relatorios").contains(key) && value.equals("UNSPECIFIED")) && !(key.equals("relatorios") && value.equals("ALL"))) continue;
                if (name.equals("publicationType") && !Set.of("COMIC","BOOK").contains(value)) continue;
                if (name.equals("reading") && !Set.of("ALL","READ","UNREAD").contains(value)) continue;
                if (name.equals("type") && !Set.of("SUMMARY","CATALOG").contains(value)) continue;
                if (name.equals("creatorFunction")) br.com.minhagibiteca.catalog.domain.CreatorFunction.valueOf(value);
                if (name.equals("order") && !Set.of("TITLE_ASC", "TITLE_DESC", "NEWEST", "OLDEST", "NUMBER_ASC", "NUMBER_DESC", "TITLE_NUMBER_ASC", "TITLE_NUMBER_DESC").contains(value)) continue;
                builder.queryParam(name, value);
            }
            return builder.build().encode().toUriString();
        } catch (IllegalArgumentException | java.time.DateTimeException error) { return null; }
    }
    public String getListReturnUrl() { return URI.create(returnUrl).getPath().equals(listUrl) ? returnUrl : listUrl; }
    public String getReturnLabel() {
        return switch (URI.create(returnUrl).getPath()) {
            case "/financeiro" -> "Financeiro";
            case "/dashboard" -> "Início";
            case "/visao-geral" -> "Visão geral";
            case "/relatorios" -> "Relatórios";
            default -> label;
        };
    }
    public String parentUrl(String fallback) {
        String path = URI.create(returnUrl).getPath();
        if (fallback.matches("/series/" + UUID_PATTERN + "/checklist") && path.matches("/series/" + UUID_PATTERN + "/(checklist|faltantes)")
                && fallback.split("/")[2].equals(path.split("/")[2])) return returnUrl;
        return withReturn(fallback);
    }
    public String withReturn(String destination) {
        return returnUrl.equals(listUrl) ? destination : UriComponentsBuilder.fromUriString(destination).queryParam("returnTo", returnUrl).build().encode().toUriString();
    }
    public String redirect(String destination) {
        if (returnUrl.equals(listUrl)) return destination;
        String targetPath = URI.create(destination).getPath();
        String backPath = URI.create(returnUrl).getPath();
        if (key.equals("edicoes") && Set.of("/financeiro", "/dashboard", "/visao-geral", "/relatorios").contains(backPath) && (targetPath.equals("/edicoes") || targetPath.matches("/edicoes/" + UUID_PATTERN))) return returnUrl;
        if (targetPath.equals(listUrl) && backPath.equals(listUrl)) return returnUrl;
        if (targetPath.matches("/series/" + UUID_PATTERN + "/checklist") && backPath.matches("/series/" + UUID_PATTERN + "/(checklist|faltantes)")
                && targetPath.split("/")[2].equals(backPath.split("/")[2])) return returnUrl;
        if (targetPath.startsWith(listUrl + "/") || (key.equals("edicoes") && targetPath.startsWith("/exemplares/")) || (key.equals("series") && targetPath.startsWith("/checklist/itens/"))) return withReturn(destination);
        return destination;
    }
}
