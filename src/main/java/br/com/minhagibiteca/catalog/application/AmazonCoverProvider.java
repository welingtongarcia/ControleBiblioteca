package br.com.minhagibiteca.catalog.application;

public interface AmazonCoverProvider {
    Image download(String url);

    record Image(byte[] bytes, String contentType) {}
}
