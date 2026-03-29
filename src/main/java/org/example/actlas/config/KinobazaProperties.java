package org.example.actlas.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "kinobaza")
@Component
@Data
public class KinobazaProperties {
    private String searchUrl;
}