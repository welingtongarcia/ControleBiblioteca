package br.com.minhagibiteca.catalog.application;

import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public record AmazonProductLink(String asin) {
    private static final Pattern PRODUCT = Pattern.compile("^/(?:[^/]+/)?dp/([A-Za-z0-9]{10})(?:/ref=[^/]*)?/?$|^/gp/product/([A-Za-z0-9]{10})(?:/ref=[^/]*)?/?$");
    public AmazonProductLink {
        if (asin == null || !asin.matches("[A-Z0-9]{10}")) throw invalid();
    }
    public static AmazonProductLink parse(String input) {
        try {
            if (input == null || input.length() > 2048 || input.chars().anyMatch(Character::isISOControl)) throw invalid();
            URI uri = URI.create(input.strip());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getPort() != -1 || !Set.of("amazon.com.br", "www.amazon.com.br").contains(uri.getHost().toLowerCase(Locale.ROOT))) throw invalid();
            String path = uri.getRawPath();
            if (path == null || path.matches("(?i).*(%2f|%5c|%00).*")) throw invalid();
            var match = PRODUCT.matcher(path);
            if (!match.matches()) throw invalid();
            return new AmazonProductLink((match.group(1) == null ? match.group(2) : match.group(1)).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) { throw invalid(); }
    }
    public String url() { return "https://www.amazon.com.br/dp/" + asin; }
    public String isbn() {
        try { return IsbnLookupService.canonical(asin); }
        catch (IsbnLookupService.LookupException e) { return null; }
    }
    private static AmazonImportService.ImportException invalid() {
        return new AmazonImportService.ImportException("Cole um link HTTPS de produto da Amazon Brasil, com /dp/ ou /gp/product/. Para links curtos, abra o produto e copie o endereço completo. Seus campos foram preservados.");
    }
}
