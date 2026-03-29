package org.example.actlas.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "actlas")
@Component
@Data
public class ActlasProperties {
    private History history = new History();

    @Data
    public static class History {
        private int maxSize = 30;
    }
}