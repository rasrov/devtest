package com.inditex.devtest.service;

import com.inditex.devtest.application.InvokerService;
import com.inditex.devtest.exception.RemoteException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class InvokerServiceImpl implements InvokerService {

    @Override
    public <T> T invoke(Supplier<T> operation, Function<RuntimeException, RemoteException> errorHandler) {
        try {
            return operation.get();
        } catch (final HttpClientErrorException | HttpServerErrorException e) {
            throw errorHandler.apply(e);
        }
    }
}
