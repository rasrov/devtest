package com.inditex.devtest.exception;

import com.inditex.devtest.exception.rest.RestClientErrorCode;
import lombok.Getter;

import java.io.Serial;

@Getter
public class RemoteException extends DomainException {

    @Serial
    private static final long serialVersionUID = 2374716148242718884L;

    private final int statusCode;

    public RemoteException(final String message, final int statusCode, final Exception exception) {
        super(message, RestClientErrorCode.REMOTE_API_ERROR, exception);
        this.statusCode = statusCode;
    }
}
