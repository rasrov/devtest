package com.inditex.devtest.application;

import com.inditex.devtest.exception.RemoteException;

import java.util.function.Function;
import java.util.function.Supplier;

public interface InvokerService {

    <T> T invoke(final Supplier<T> operation, final Function<RuntimeException, RemoteException> errorHandler);

}
