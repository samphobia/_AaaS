package com.authservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "http.client")
public class HttpClientProperties {

    private int connectTimeoutMs = 2000;
    private int readTimeoutMs = 4000;
}
