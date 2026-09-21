package com.inditex.devtest.exception.rest;

import com.inditex.devtest.exception.DomainException;
import lombok.Getter;

import java.io.Serial;

@Getter
public class AuthenticationExpiredException extends DomainException {

    @Serial
    private static final long serialVersionUID = -8271940241439714504L;

    private static final String DEFAULT_MESSAGE = "Authentication token has expired. Please renew your session.";

    public AuthenticationExpiredException() {
        super(DEFAULT_MESSAGE, RestClientErrorCode.AUTHENTICATION_EXPIRED);
    }

    public AuthenticationExpiredException(final Throwable cause) {
        super(DEFAULT_MESSAGE, RestClientErrorCode.AUTHENTICATION_EXPIRED, cause);
    }

    public AuthenticationExpiredException(final String message) {
        super(message, RestClientErrorCode.AUTHENTICATION_EXPIRED);
    }

    public AuthenticationExpiredException(final String message, final Throwable cause) {
        super(message, RestClientErrorCode.AUTHENTICATION_EXPIRED, cause);
    }

}
