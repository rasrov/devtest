package com.inditex.devtest.handler;

import com.inditex.devtest.exception.DomainException;
import com.inditex.devtest.exception.NotFoundException;
import com.inditex.devtest.exception.RemoteException;
import com.inditex.devtest.exception.rest.BadRequestException;
import com.inditex.devtest.exception.rest.RestClientErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.security.InvalidParameterException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalProblemDetailHandlerTest {

	private static final String STATUS_CODES_URL = "https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/%d";

	private GlobalProblemDetailHandler handler;

	@BeforeEach
	void beforeEach() {
		this.handler = new GlobalProblemDetailHandler();
	}

	@Nested
	class HandleBadRequestException {

		@Test
		void when_bad_request_expect_400_with_error_code_and_message() {
			final BadRequestException ex = new BadRequestException("Invalid input");

			final ResponseEntity<ProblemDetail> response = handler.handleBadRequestException(ex);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
			assertThat(response.getBody()).isNotNull().satisfies(body -> {
				assertThat(body.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
				assertThat(body.getDetail()).isEqualTo("Invalid input");
				assertThat(body.getType())
						.isEqualTo(URI.create(String.format(STATUS_CODES_URL, HttpStatus.BAD_REQUEST.value())));
				assertThat(body.getProperties()).containsEntry("errorCode", RestClientErrorCode.BAD_REQUEST.code())
						.containsEntry("message", RestClientErrorCode.BAD_REQUEST.message());
			});
		}
	}

	@Nested
	class HandleNotFoundException {

		@Test
		void when_not_found_expect_404_without_error_code_properties() {
			final NotFoundException ex = new NotFoundException("Product not found");

			final ResponseEntity<ProblemDetail> response = handler.handleNotFoundException(ex);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
			assertThat(response.getBody()).isNotNull().satisfies(body -> {
				assertThat(body.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
				assertThat(body.getDetail()).isEqualTo("Product not found");
				assertThat(body.getType())
						.isEqualTo(URI.create(String.format(STATUS_CODES_URL, HttpStatus.NOT_FOUND.value())));
				assertThat(body.getProperties()).isNull();
			});
		}
	}

	@Nested
	class HandleDomainException {

		@Test
		void when_domain_error_with_error_code_expect_422_with_error_code_and_message() {
			final DomainException ex = new DomainException("Business rule violated",
					RestClientErrorCode.UNPROCESSABLE_ENTITY);

			final ResponseEntity<ProblemDetail> response = handler.handleDomainException(ex);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
			assertThat(response.getBody()).isNotNull().satisfies(body -> {
				assertThat(body.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
				assertThat(body.getDetail()).isEqualTo("Business rule violated");
				assertThat(body.getType())
						.isEqualTo(URI.create(String.format(STATUS_CODES_URL, HttpStatus.UNPROCESSABLE_ENTITY.value())));
				assertThat(body.getProperties())
						.containsEntry("errorCode", RestClientErrorCode.UNPROCESSABLE_ENTITY.code())
						.containsEntry("message", RestClientErrorCode.UNPROCESSABLE_ENTITY.message());
			});
		}

		@Test
		void when_domain_error_without_error_code_expect_422_without_properties() {
			final DomainException ex = new DomainException("Generic domain error");

			final ResponseEntity<ProblemDetail> response = handler.handleDomainException(ex);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
			assertThat(response.getBody()).isNotNull().satisfies(body -> {
				assertThat(body.getDetail()).isEqualTo("Generic domain error");
				assertThat(body.getProperties()).isNull();
			});
		}
	}

	@Nested
	class HandleRemoteException {

		@Test
		void when_remote_error_expect_502_with_detail_overridden_by_remote_api_error() {
			final RemoteException ex = new RemoteException("Upstream timeout", 504, new RuntimeException("cause"));

			final ResponseEntity<ProblemDetail> response = handler.handleRemoteException(ex);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
			assertThat(response.getBody()).isNotNull().satisfies(body -> {
				assertThat(body.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
				assertThat(body.getType())
						.isEqualTo(URI.create(String.format(STATUS_CODES_URL, HttpStatus.BAD_GATEWAY.value())));
				assertThat(body.getDetail()).isEqualTo(RestClientErrorCode.REMOTE_API_ERROR.message());
				assertThat(body.getProperties())
						.containsEntry("errorCode", RestClientErrorCode.REMOTE_API_ERROR.code())
						.containsEntry("message", RestClientErrorCode.REMOTE_API_ERROR.message());
			});
		}
	}

	@Nested
	class HandleInvalidParameterException {

		@Test
		void when_invalid_parameter_expect_400_without_error_code_properties() {
			final InvalidParameterException ex = new InvalidParameterException("Invalid parameter");

			final ResponseEntity<ProblemDetail> response = handler.handleInvalidParameterException(ex);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
			assertThat(response.getBody()).isNotNull().satisfies(body -> {
				assertThat(body.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
				assertThat(body.getDetail()).isEqualTo("Invalid parameter");
				assertThat(body.getType())
						.isEqualTo(URI.create(String.format(STATUS_CODES_URL, HttpStatus.BAD_REQUEST.value())));
				assertThat(body.getProperties()).isNull();
			});
		}
	}
}
