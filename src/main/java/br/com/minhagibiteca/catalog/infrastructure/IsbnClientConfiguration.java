package br.com.minhagibiteca.catalog.infrastructure;

import java.net.HttpURLConnection;
import java.io.IOException;
import java.time.Duration;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class IsbnClientConfiguration {
    @Bean RestClient isbnRestClient(RestClient.Builder builder,
            @Value("${app.isbn.user-agent:MinhaGibiteca/0.1}") String userAgent) {
        var factory = new SimpleClientHttpRequestFactory() {
            @Override protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
                super.prepareConnection(connection, method); connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return builder.baseUrl("https://openlibrary.org").requestFactory(factory)
                .defaultHeader("User-Agent", userAgent)
                .defaultHeader("Accept", "application/json").build();
    }
}
