package com.inditex.devtest.handler;

import com.inditex.devtest.exception.DomainException;
import com.inditex.devtest.exception.NotFoundException;
import com.inditex.devtest.exception.RemoteException;
import com.inditex.devtest.exception.rest.BadRequestException;
import com.inditex.devtest.exception.rest.RestClientErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.security.InvalidParameterException;

@RestControllerAdvice
public class GlobalProblemDetailHandler {

	private static final String STATUS_CODES_URL = "https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/%d";

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ProblemDetail> handleBadRequestException(final BadRequestException ex) {
		return this.handleException(HttpStatus.BAD_REQUEST, ex);
	}

	@ExceptionHandler({NotFoundException.class})
	public ResponseEntity<ProblemDetail> handleNotFoundException(final NotFoundException ex) {
		return this.handleException(HttpStatus.NOT_FOUND, ex);
	}

	@ExceptionHandler(DomainException.class)
	public ResponseEntity<ProblemDetail> handleDomainException(final DomainException ex) {
		return this.handleException(HttpStatus.UNPROCESSABLE_ENTITY, ex);
	}

	@ExceptionHandler(RemoteException.class)
	public ResponseEntity<ProblemDetail> handleRemoteException(final RemoteException ex) {
		final ResponseEntity<ProblemDetail> response = this.handleException(HttpStatus.BAD_GATEWAY, ex);
		response.getBody().setDetail(RestClientErrorCode.REMOTE_API_ERROR.message());
		return response;
	}

	@ExceptionHandler(InvalidParameterException.class)
	public ResponseEntity<ProblemDetail> handleInvalidParameterException(final InvalidParameterException ex) {
		return this.handleException(HttpStatus.BAD_REQUEST, ex);
	}

	private ResponseEntity<ProblemDetail> handleException(final HttpStatus status, final Throwable ex) {
		final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
		problemDetail.setType(URI.create(String.format(STATUS_CODES_URL, status.value())));
		if (ex instanceof final DomainException domainEx && domainEx.getErrorCode() != null) {
			problemDetail.setProperty("errorCode", domainEx.getErrorCode().code());
			problemDetail.setProperty("message", domainEx.getErrorCode().message());
		}
		return new ResponseEntity<>(problemDetail, status);
	}

}
