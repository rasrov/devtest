package com.inditex.devtest.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("ProductConfiguration Tests")
class ProductConfigurationTest {

	private final ProductConfiguration productConfiguration = new ProductConfiguration();

	@Test
	@DisplayName("productRestClient debe crear un RestClient no nulo")
	void shouldCreateProductRestClient() {
		// Act
		final RestClient restClient = this.productConfiguration.productRestClient("http://localhost:8080");

		// Assert
		assertNotNull(restClient);
	}

	@Test
	@DisplayName("productRestClient debe crear una instancia distinta en cada invocación")
	void shouldCreateNewInstanceEachTime() {
		// Act
		final RestClient first = this.productConfiguration.productRestClient("http://localhost:8080");
		final RestClient second = this.productConfiguration.productRestClient("http://localhost:8080");

		// Assert
		assertNotNull(first);
		assertNotNull(second);
	}
}

