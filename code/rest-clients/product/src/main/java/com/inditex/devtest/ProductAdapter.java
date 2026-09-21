package com.inditex.devtest;

import com.inditex.devtest.application.InvokerService;
import com.inditex.devtest.exception.NotFoundException;
import com.inditex.devtest.exception.RemoteException;
import com.inditex.devtest.mapper.ProductMapper;
import com.inditex.devtest.model.product.Product;
import com.inditex.devtest.model.product.ProductEntity;
import com.inditex.devtest.port.output.ProductPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.Nonnull;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

import static com.inditex.devtest.exception.RestClientErrorMapper.handleRemoteException;
import static com.inditex.devtest.exception.RestClientErrorMapper.mapRemoteException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductAdapter implements ProductPort {

	private static final String PRODUCT = "PRODUCT_EXTERNAL_API";

	private static final String BASE_PATH = "/product";

	private final RestClient productRestClient;

	private final ProductMapper productMapper;

	private final InvokerService invokerService;

	@Override
	@Retry(name = PRODUCT)
	@CircuitBreaker(name = PRODUCT, fallbackMethod = "fetchSimilarProductIdsFallback")
	@Cacheable(value = "similarProductIds", key = "#root.targetClass+'-'+#root.methodName+'-'+#productId")
	public List<Integer> fetchSimilarProductIds(@Nonnull final Integer productId) {
		try {
			return this.invokerService.invoke(
					() -> this.productRestClient.get().uri(String.format("%s/%d/similarids", BASE_PATH, productId))
							.retrieve().body(new ParameterizedTypeReference<List<Integer>>() {
							}),
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
	@Cacheable(value = "similarProductIds", key = "#root.targetClass+'-'+#root.methodName+'-'+#productId")
	public Optional<Product> fetchProductById(@Nonnull final Integer productId) {
		try {
			final Optional<ProductEntity> productEntityOptional = this.invokerService
					.invoke(() -> this.productRestClient.get().uri(String.format("%s/%d", BASE_PATH, productId))
							.retrieve().body(new ParameterizedTypeReference<Optional<ProductEntity>>() {
							}), e -> mapRemoteException(e, PRODUCT));

			return productEntityOptional.map(this.productMapper::toProduct);

		} catch (final RemoteException e) {
			if (HttpStatus.NOT_FOUND.value() == e.getStatusCode()) {
				log.error("Product not found for product ID: {}", productId, e);
				return Optional.empty();
			}
			throw handleRemoteException(e, PRODUCT);
		}
	}

	@Generated
	public void fetchSimilarProductIdsFallback(@Nonnull final Integer productId, final RemoteException exception) {
		throw handleRemoteException(exception, PRODUCT);
	}

	@Generated
	public void fetchProductByIdFallback(@Nonnull final Integer productId, final RemoteException exception) {
		throw handleRemoteException(exception, PRODUCT);
	}

}
