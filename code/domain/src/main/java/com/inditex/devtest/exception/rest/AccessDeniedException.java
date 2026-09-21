package com.inditex.devtest.exception.rest;

import com.inditex.devtest.exception.DomainException;
import lombok.Getter;

import java.io.Serial;

@Getter
public class AccessDeniedException extends DomainException {

    @Serial
    private static final long serialVersionUID = -1942940241439714504L;

    public AccessDeniedException() {
        super("The product supplier info trying to be accessed is not on your scope!", RestClientErrorCode.ACCESS_DENIED);
    }

    public AccessDeniedException(final String message) {
        super(message, RestClientErrorCode.ACCESS_DENIED);
    }

    public AccessDeniedException(final String message, final Throwable cause) {
        super(message, RestClientErrorCode.ACCESS_DENIED, cause);
    }

    public AccessDeniedException(final Throwable cause) {
        super(RestClientErrorCode.ACCESS_DENIED, cause);
    }

}
