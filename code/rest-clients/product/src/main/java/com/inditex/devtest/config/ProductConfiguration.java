package com.inditex.devtest.config;

import com.inditex.devtest.product.client.ApiClient;
import com.inditex.devtest.product.client.api.DefaultApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ProductClientConfig.class)
public class ProductConfiguration {

	@Bean
	public ApiClient productApiClient(final ProductClientConfig productClientConfig,
			final RestTemplateBuilder restTemplateBuilder) {
		final var restTemplate = restTemplateBuilder.connectTimeout(productClientConfig.connectTimeout())
				.readTimeout(productClientConfig.readTimeout()).build();

		final var apiClient = new ApiClient(restTemplate);
		apiClient.setBasePath(productClientConfig.baseUrl());
		return apiClient;
	}

	@Bean
	public DefaultApi productDefaultApi(final ApiClient productApiClient) {
		return new DefaultApi(productApiClient);
	}
}
