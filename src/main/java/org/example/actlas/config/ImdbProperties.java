package org.example.actlas.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "imdb")
@Component
@Data
public class ImdbProperties {
    private String apiBaseUrl;
    private Jsoup jsoup = new Jsoup();

    @Data
    public static class Jsoup {
        private String userAgent;
        private int timeout = 5000;
    }
}