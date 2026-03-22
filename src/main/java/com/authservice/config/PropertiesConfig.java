package com.authservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
@EnableConfigurationProperties({
	KeycloakProperties.class,
	HttpClientProperties.class,
	SecretsProperties.class,
	AuthProperties.class
})
public class PropertiesConfig {
}
