package com.inditex.devtest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "rest-clients.product")
public record ProductClientConfig(String baseUrl, Duration connectTimeout, Duration readTimeout) {
}
