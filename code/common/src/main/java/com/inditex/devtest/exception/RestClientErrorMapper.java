package com.inditex.devtest.exception;

import com.inditex.devtest.exception.rest.AccessDeniedException;
import com.inditex.devtest.exception.rest.AuthenticationExpiredException;
import com.inditex.devtest.exception.rest.BadRequestException;
import com.inditex.devtest.exception.rest.UnprocessableEntityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

@Slf4j
public class RestClientErrorMapper {

  private RestClientErrorMapper() {
  }

  public static RemoteException mapRemoteException(final RuntimeException e, final String boundedContextCode) {
    final int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

    if (e instanceof HttpStatusCodeException statusEx) {
      return new RemoteException(
          String.format("[%s] %s", boundedContextCode, statusEx.getResponseBodyAsString()),
          statusEx.getStatusCode().value(),
          e);
    }
      return new RemoteException(String.format("[%s] %s", boundedContextCode, e.getMessage()), statusCode, e);
  }

  public static DomainException handleRemoteException(final RemoteException e, final String boundedContextCode) {
    if (HttpStatus.BAD_REQUEST.value() == e.getStatusCode()) {
      log.warn("Bad request to {}. {}", boundedContextCode, e.getMessage());
      throw new BadRequestException(e.getMessage(), e);
    }
    if (HttpStatus.UNAUTHORIZED.value() == e.getStatusCode()) {
      log.warn("Unauthorized request to {}. {}", boundedContextCode, e.getMessage());
      throw new AuthenticationExpiredException(e);
    }
    if (HttpStatus.FORBIDDEN.value() == e.getStatusCode()) {
      log.warn("Forbidden request to {}. {}", boundedContextCode, e.getMessage());
      throw new AccessDeniedException("You do not have permission to access this resource.");
    }
    if (HttpStatus.UNPROCESSABLE_ENTITY.value() == e.getStatusCode()) {
      log.warn("Unprocessable entity from {}. {}", boundedContextCode, e.getMessage());
      throw new UnprocessableEntityException(e.getMessage(), e);
    }
    if (HttpStatus.INTERNAL_SERVER_ERROR.value() == e.getStatusCode()) {
      log.warn("Internal server error from {}. {}", boundedContextCode, e.getMessage());
      throw e;
    }
    log.warn("Unexpected error calling {} (status: {}): {}", boundedContextCode, e.getStatusCode(), e.getMessage());
    throw e;
  }
}
