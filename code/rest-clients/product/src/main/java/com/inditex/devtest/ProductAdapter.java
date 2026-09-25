package com.inditex.devtest;

import com.inditex.devtest.exception.DomainException;
import com.inditex.devtest.exception.NotFoundException;
import com.inditex.devtest.exception.RemoteException;
import com.inditex.devtest.mapper.ProductClientMapper;
import com.inditex.devtest.model.product.Product;
import com.inditex.devtest.port.output.ProductPort;
import com.inditex.devtest.product.client.api.DefaultApi;
import com.inditex.devtest.service.InvokerService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.Nonnull;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

import static com.inditex.devtest.exception.RestClientErrorMapper.handleRemoteException;
import static com.inditex.devtest.exception.RestClientErrorMapper.mapRemoteException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductAdapter implements ProductPort {

	private static final String PRODUCT = "product_external_api";

	private static final String SIMILAR_IDS_CACHE = "similarProductIds";

	private final DefaultApi defaultApi;

	private final ProductClientMapper productMapper;

	private final InvokerService invokerService;

	@Override
	@Retry(name = PRODUCT)
	@CircuitBreaker(name = PRODUCT, fallbackMethod = "fetchSimilarProductIdsFallback")
	@Cacheable(value = SIMILAR_IDS_CACHE, key = "#productId")
	public Set<String> fetchSimilarProductIds(@Nonnull final String productId) {
		try {
			return this.invokerService.invoke(() -> this.defaultApi.getProductSimilarids(productId),
					e -> mapRemoteException(e, PRODUCT));
		} catch (final RemoteException e) {
			if (HttpStatus.NOT_FOUND.value() == e.getStatusCode()) {
				throw new NotFoundException("Similar product IDs not found for product ID: " + productId, e);
			}
			throw handleRemoteException(e, PRODUCT);
		}
	}

	@Override
	@Retry(name = PRODUCT)
	@CircuitBreaker(name = PRODUCT, fallbackMethod = "fetchProductByIdFallback")
	public Product fetchProductById(@Nonnull final String productId) {
		try {
			final var productDetail = this.invokerService.invoke(() -> this.defaultApi.getProductProductId(productId),
					e -> mapRemoteException(e, PRODUCT));

			return this.productMapper.toProduct(productDetail);
		} catch (final RemoteException e) {
			if (HttpStatus.NOT_FOUND.value() == e.getStatusCode()) {
				throw new NotFoundException("Product not found for product ID: " + productId, e);
			}
			throw handleRemoteException(e, PRODUCT);
		}
	}

	@Generated
	public Set<String> fetchSimilarProductIdsFallback(@Nonnull final String productId, final Throwable throwable) {
		throw this.toRemoteException(throwable);
	}

	@Generated
	public Product fetchProductByIdFallback(@Nonnull final String productId, final Throwable throwable) {
		throw this.toRemoteException(throwable);
	}

	private DomainException toRemoteException(final Throwable throwable) {
		if (throwable instanceof final NotFoundException notFoundException) {
			throw notFoundException;
		}
		if (throwable instanceof RemoteException remoteException) {
			return handleRemoteException(remoteException, PRODUCT);
		}
		return new RemoteException(throwable.getMessage(), HttpStatus.SERVICE_UNAVAILABLE.value(),
				(Exception) throwable);
	}

}
