package com.inditex.devtest.service;

import com.inditex.devtest.exception.RemoteException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.function.Function;
import java.util.function.Supplier;

@Service
public final class InvokerServiceImpl implements InvokerService {

	@Override
	public <T> T invoke(final Supplier<T> operation, final Function<RuntimeException, RemoteException> errorHandler) {
		try {
			return operation.get();
		} catch (final RestClientException e) {
			throw errorHandler.apply(e);
		}
	}
}
