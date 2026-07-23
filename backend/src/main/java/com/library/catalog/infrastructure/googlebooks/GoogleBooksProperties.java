package com.library.catalog.infrastructure.googlebooks;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

@ConfigMapping(prefix = "google-books")
public interface GoogleBooksProperties {

    @WithDefault("true")
    boolean enabled();

    @WithDefault("https://www.googleapis.com/books/v1")
    String baseUrl();

    Optional<String> apiKey();

    @WithDefault("20")
    int maxResults();

    @WithDefault("pt")
    String defaultLanguage();

    @WithDefault("5000")
    long connectTimeout();

    @WithDefault("10000")
    long readTimeout();
}
