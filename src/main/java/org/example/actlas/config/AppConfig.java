package org.example.actlas.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Configuration
@RequiredArgsConstructor
public class AppConfig {

    private final ImdbProperties imdbProperties;

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl(imdbProperties.getApiBaseUrl())
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
