package com.inditex.devtest.exception.rest;

public enum RestClientErrorCode implements ErrorCode {

  INTERNAL_ERROR("INTERNAL_ERROR", "An unexpected error occurred."),

  RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "The requested resource was not found."),

  BAD_REQUEST("BAD_REQUEST", "The request is invalid."),

  UNPROCESSABLE_ENTITY("UNPROCESSABLE_ENTITY", "The request could not be processed."),

  AUTHENTICATION_EXPIRED("AUTHENTICATION_EXPIRED", "Authentication has expired. Please renew your session."),

  ACCESS_DENIED("ACCESS_DENIED", "You do not have permission to access this resource."),

  VERSION_CONFLICT("VERSION_CONFLICT", "Stale version detected. Reload the resource and retry your request."),

  REMOTE_API_ERROR("REMOTE_API_ERROR", "An error occurred while communicating with an external service.");

  private final String code;

  private final String message;

  RestClientErrorCode(final String code, final String message) {
    this.code = code;
    this.message = message;
  }

  @Override
  public String code() {
    return this.code;
  }

  @Override
  public String message() {
    return this.message;
  }
}
