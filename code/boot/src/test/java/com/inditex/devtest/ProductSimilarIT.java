package com.inditex.devtest;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductSimilarIT {

	private static final WireMockServer WIRE_MOCK = new WireMockServer(wireMockConfig().dynamicPort());

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@BeforeAll
	static void startWireMock() {
		WIRE_MOCK.start();
	}

	@AfterAll
	static void stopWireMock() {
		WIRE_MOCK.stop();
	}

	@AfterEach
	void resetWireMock() {
		WIRE_MOCK.resetAll();
	}

	@DynamicPropertySource
	static void wireMockProperties(final DynamicPropertyRegistry registry) {
		registry.add("rest-clients.product.base-url", WIRE_MOCK::baseUrl);
	}

	private ResponseEntity<String> callSimilar(final String productId) {
		return this.restTemplate.getForEntity("http://localhost:" + this.port + "/product/" + productId + "/similar",
				String.class);
	}

	private static void stubSimilarIds(final String productId, final String jsonArrayBody) {
		WIRE_MOCK.stubFor(get(urlEqualTo("/product/" + productId + "/similarids")).willReturn(
				aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(jsonArrayBody)));
	}

	private static void stubProductOk(final String productId, final String name, final String price,
			final boolean availability) {
		WIRE_MOCK.stubFor(get(urlEqualTo("/product/" + productId))
				.willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
						.withBody(String.format("{\"id\":\"%s\",\"name\":\"%s\",\"price\":%s,\"availability\":%s}",
								productId, name, price, availability))));
	}

	private static void stubProductStatus(final String productId, final int status) {
		WIRE_MOCK.stubFor(get(urlEqualTo("/product/" + productId)).willReturn(
				aResponse().withStatus(status).withHeader("Content-Type", "application/json").withBody("{}")));
	}

	private static void stubProductDelayed(final String productId, final String name, final int delayMillis) {
		WIRE_MOCK.stubFor(get(urlEqualTo("/product/" + productId))
				.willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
						.withBody(String.format("{\"id\":\"%s\",\"name\":\"%s\",\"price\":9.99,\"availability\":true}",
								productId, name))
						.withFixedDelay(delayMillis)));
	}

	@Nested
	class Normal {

		@Test
		void when_all_details_ok_expect_200_with_all_products() {
			stubSimilarIds("1", "[\"2\",\"3\",\"4\"]");
			stubProductOk("2", "Dress", "19.99", true);
			stubProductOk("3", "Blazer", "29.99", false);
			stubProductOk("4", "Boots", "39.99", true);

			final ResponseEntity<String> response = ProductSimilarIT.this.callSimilar("1");

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).contains("\"id\":\"2\"").contains("\"id\":\"3\"").contains("\"id\":\"4\"");
		}

		@Test
		void when_all_details_ok_expect_response_matches_openapi_contract() {
			stubSimilarIds("1", "[\"2\",\"3\",\"4\"]");
			stubProductOk("2", "Dress", "19.99", true);
			stubProductOk("3", "Blazer", "29.99", false);
			stubProductOk("4", "Boots", "39.99", true);

			final ResponseEntity<String> response = ProductSimilarIT.this.callSimilar("1");

			assertThat(ProductSimilarIT.this.validateAgainstContract("/product/1/similar", response)).isFalse();
		}

		@Test
		void when_no_similar_ids_expect_200_empty() {
			stubSimilarIds("1", "[]");

			final ResponseEntity<String> response = ProductSimilarIT.this.callSimilar("1");

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isEqualTo("[]");
		}
	}

	@Nested
	class NotFound {

		@Test
		void when_similar_ids_not_found_expect_404() {
			WIRE_MOCK.stubFor(get(urlEqualTo("/product/99/similarids")).willReturn(aResponse().withStatus(404)
					.withHeader("Content-Type", "application/json").withBody("{\"message\":\"not found\"}")));

			final ResponseEntity<String> response = ProductSimilarIT.this.callSimilar("99");

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		}

		@Test
		void when_one_detail_not_found_expect_200_partial_with_remaining() {
			stubSimilarIds("4", "[\"1\",\"2\",\"5\"]");
			stubProductOk("1", "Shirt", "9.99", true);
			stubProductOk("2", "Dress", "19.99", true);
			stubProductStatus("5", 404);

			final ResponseEntity<String> response = ProductSimilarIT.this.callSimilar("4");

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).contains("\"id\":\"1\"").contains("\"id\":\"2\"")
					.doesNotContain("\"id\":\"5\"");
		}
	}

	@Nested
	class Error {

		@Test
		void when_one_detail_fails_technically_expect_200_partial_best_effort() {
			stubSimilarIds("5", "[\"1\",\"2\",\"6\"]");
			stubProductOk("1", "Shirt", "9.99", true);
			stubProductOk("2", "Dress", "19.99", true);
			stubProductStatus("6", 500);

			final ResponseEntity<String> response = ProductSimilarIT.this.callSimilar("5");

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).contains("\"id\":\"1\"").contains("\"id\":\"2\"")
					.doesNotContain("\"id\":\"6\"");
		}

		@Test
		void when_all_details_fail_technically_expect_502() {
			stubSimilarIds("7", "[\"6\",\"8\",\"9\"]");
			stubProductStatus("6", 500);
			stubProductStatus("8", 500);
			stubProductStatus("9", 500);

			final ResponseEntity<String> response = ProductSimilarIT.this.callSimilar("7");

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
		}
	}

	@Nested
	class Slow {

		@Test
		void when_detail_slow_but_within_timeout_expect_200_with_product() {
			stubSimilarIds("2", "[\"3\"]");
			stubProductDelayed("3", "Blazer", 100);

			final ResponseEntity<String> response = ProductSimilarIT.this.callSimilar("2");

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).contains("\"id\":\"3\"");
		}

		@Test
		void when_some_details_very_slow_expect_200_partial_with_fast_ones() {
			stubSimilarIds("3", "[\"1\",\"1000\"]");
			stubProductOk("1", "Shirt", "9.99", true);
			stubProductDelayed("1000", "Coat", 5000);

			final ResponseEntity<String> response = ProductSimilarIT.this.callSimilar("3");

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).contains("\"id\":\"1\"").doesNotContain("\"id\":\"1000\"");
		}

		@Test
		void when_all_details_very_slow_expect_502() {
			stubSimilarIds("3", "[\"1000\",\"10000\"]");
			stubProductDelayed("1000", "Coat", 5000);
			stubProductDelayed("10000", "Leather jacket", 5000);

			final ResponseEntity<String> response = ProductSimilarIT.this.callSimilar("3");

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
		}
	}

	private boolean validateAgainstContract(final String path, final ResponseEntity<String> response) {
		final OpenApiInteractionValidator validator = OpenApiInteractionValidator
				.createForSpecificationUrl("openapi/similarProducts.yaml").build();
		final Request request = SimpleRequest.Builder.get(path).build();
		final SimpleResponse.Builder responseBuilder = new SimpleResponse.Builder(response.getStatusCode().value())
				.withContentType("application/json");
		if (response.getBody() != null) {
			responseBuilder.withBody(response.getBody());
		}
		final ValidationReport report = validator.validateResponse(path, Request.Method.GET, responseBuilder.build());
		return report.hasErrors();
	}
}
