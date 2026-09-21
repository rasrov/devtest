package com.inditex.devtest.exception.rest;

import com.inditex.devtest.exception.DomainException;
import lombok.Getter;

import java.io.Serial;

@Getter
public class UnprocessableEntityException extends DomainException {

    @Serial
    private static final long serialVersionUID = 3712940241439714502L;

    public UnprocessableEntityException(final String message) {
        super(message, RestClientErrorCode.UNPROCESSABLE_ENTITY);
    }

    public UnprocessableEntityException(final String message, final Throwable cause) {
        super(message, RestClientErrorCode.UNPROCESSABLE_ENTITY, cause);
    }

    public UnprocessableEntityException(final Throwable cause) {
        super(RestClientErrorCode.UNPROCESSABLE_ENTITY, cause);
    }

}
