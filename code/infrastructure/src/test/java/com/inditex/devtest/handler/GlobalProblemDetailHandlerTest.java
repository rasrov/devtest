package com.inditex.devtest.handler;

import com.inditex.devtest.exception.DomainException;
import com.inditex.devtest.exception.NotFoundException;
import com.inditex.devtest.exception.RemoteException;
import com.inditex.devtest.exception.rest.BadRequestException;
import com.inditex.devtest.exception.rest.RestClientErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("GlobalProblemDetailHandler Tests")
class GlobalProblemDetailHandlerTest {

	private static final String STATUS_CODES_URL = "https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/%d";

	private final GlobalProblemDetailHandler handler = new GlobalProblemDetailHandler();

	@Test
	@DisplayName("handleBadRequestException debe devolver 400 con errorCode y message")
	void shouldHandleBadRequestException() {
		// Arrange
		final BadRequestException ex = new BadRequestException("Invalid input");

		// Act
		final ResponseEntity<ProblemDetail> response = this.handler.handleBadRequestException(ex);

		// Assert
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		final ProblemDetail body = response.getBody();
		assertNotNull(body);
		assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
		assertEquals("Invalid input", body.getDetail());
		assertEquals(URI.create(String.format(STATUS_CODES_URL, HttpStatus.BAD_REQUEST.value())), body.getType());
		assertNotNull(body.getProperties());
		assertEquals(RestClientErrorCode.BAD_REQUEST.code(), body.getProperties().get("errorCode"));
		assertEquals(RestClientErrorCode.BAD_REQUEST.message(), body.getProperties().get("message"));
	}

	@Test
	@DisplayName("handleNotFoundException debe devolver 404 sin propiedades de errorCode")
	void shouldHandleNotFoundException() {
		// Arrange
		final NotFoundException ex = new NotFoundException("Product not found");

		// Act
		final ResponseEntity<ProblemDetail> response = this.handler.handleNotFoundException(ex);

		// Assert
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		final ProblemDetail body = response.getBody();
		assertNotNull(body);
		assertEquals(HttpStatus.NOT_FOUND.value(), body.getStatus());
		assertEquals("Product not found", body.getDetail());
		assertEquals(URI.create(String.format(STATUS_CODES_URL, HttpStatus.NOT_FOUND.value())), body.getType());
		// NotFoundException no es DomainException, no debe añadir propiedades
		assertNull(body.getProperties());
	}

	@Test
	@DisplayName("handleDomainException debe devolver 422 con errorCode cuando existe")
	void shouldHandleDomainExceptionWithErrorCode() {
		// Arrange
		final DomainException ex = new DomainException("Business rule violated", RestClientErrorCode.UNPROCESSABLE_ENTITY);

		// Act
		final ResponseEntity<ProblemDetail> response = this.handler.handleDomainException(ex);

		// Assert
		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
		final ProblemDetail body = response.getBody();
		assertNotNull(body);
		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), body.getStatus());
		assertEquals("Business rule violated", body.getDetail());
		assertEquals(URI.create(String.format(STATUS_CODES_URL, HttpStatus.UNPROCESSABLE_ENTITY.value())), body.getType());
		assertNotNull(body.getProperties());
		assertEquals(RestClientErrorCode.UNPROCESSABLE_ENTITY.code(), body.getProperties().get("errorCode"));
		assertEquals(RestClientErrorCode.UNPROCESSABLE_ENTITY.message(), body.getProperties().get("message"));
	}

	@Test
	@DisplayName("handleDomainException debe devolver 422 sin propiedades cuando no hay errorCode")
	void shouldHandleDomainExceptionWithoutErrorCode() {
		// Arrange
		final DomainException ex = new DomainException("Generic domain error");

		// Act
		final ResponseEntity<ProblemDetail> response = this.handler.handleDomainException(ex);

		// Assert
		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
		final ProblemDetail body = response.getBody();
		assertNotNull(body);
		assertEquals("Generic domain error", body.getDetail());
		assertNull(body.getProperties());
	}

	@Test
	@DisplayName("handleRemoteException debe devolver 502 con detail sobreescrito por REMOTE_API_ERROR")
	void shouldHandleRemoteException() {
		// Arrange
		final RemoteException ex = new RemoteException("Upstream timeout", 504, new RuntimeException("cause"));

		// Act
		final ResponseEntity<ProblemDetail> response = this.handler.handleRemoteException(ex);

		// Assert
		assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
		final ProblemDetail body = response.getBody();
		assertNotNull(body);
		assertEquals(HttpStatus.BAD_GATEWAY.value(), body.getStatus());
		assertEquals(URI.create(String.format(STATUS_CODES_URL, HttpStatus.BAD_GATEWAY.value())), body.getType());
		// El detail se sobreescribe con el mensaje del error code remoto
		assertEquals(RestClientErrorCode.REMOTE_API_ERROR.message(), body.getDetail());
		// RemoteException es DomainException con REMOTE_API_ERROR
		assertNotNull(body.getProperties());
		assertEquals(RestClientErrorCode.REMOTE_API_ERROR.code(), body.getProperties().get("errorCode"));
		assertEquals(RestClientErrorCode.REMOTE_API_ERROR.message(), body.getProperties().get("message"));
	}
}
