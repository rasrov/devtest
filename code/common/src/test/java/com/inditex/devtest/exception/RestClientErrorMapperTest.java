package com.inditex.devtest.exception;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestClientErrorMapperTest {

	private static final String BC = "product_external_api";

	@Nested
	class MapRemoteException {

		@Test
		void when_http_status_code_exception_expect_remote_exception_with_same_status() {
			final HttpClientErrorException httpException = HttpClientErrorException.create(HttpStatus.NOT_FOUND,
					"Not Found", null, null, null);

			final RemoteException result = RestClientErrorMapper.mapRemoteException(httpException, BC);

			assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
			assertThat(result.getCause()).isSameAs(httpException);
			assertThat(result.getMessage()).doesNotContain("Not Found");
		}

		@Test
		void when_resource_access_with_socket_timeout_expect_gateway_timeout() {
			final ResourceAccessException timeoutException = new ResourceAccessException("read timed out",
					new SocketTimeoutException("Read timed out"));

			final RemoteException result = RestClientErrorMapper.mapRemoteException(timeoutException, BC);

			assertThat(result.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT.value());
			assertThat(result.getCause()).isSameAs(timeoutException);
		}

		@Test
		void when_resource_access_without_timeout_expect_service_unavailable() {
			final ResourceAccessException connectionException = new ResourceAccessException("Connection refused",
					new java.net.ConnectException("Connection refused"));

			final RemoteException result = RestClientErrorMapper.mapRemoteException(connectionException, BC);

			assertThat(result.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
		}

		@Test
		void when_other_rest_client_exception_expect_bad_gateway() {
			final RestClientException genericException = new RestClientException("deserialization error");

			final RemoteException result = RestClientErrorMapper.mapRemoteException(genericException, BC);

			assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
			assertThat(result.getCause()).isSameAs(genericException);
		}
	}

	@Nested
	class HandleRemoteException {

		@Test
		void when_remote_exception_expect_same_instance_rethrown() {
			final RemoteException remoteException = new RemoteException("upstream error", HttpStatus.BAD_GATEWAY.value(),
					null);

			assertThatThrownBy(() -> RestClientErrorMapper.handleRemoteException(remoteException, BC))
					.isSameAs(remoteException);
		}
	}
}
