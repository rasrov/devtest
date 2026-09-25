package com.inditex.devtest.usecase;

import com.inditex.devtest.exception.RemoteException;
import com.inditex.devtest.model.product.Product;
import com.inditex.devtest.port.input.product.SearchSimilarProductUseCase;
import com.inditex.devtest.port.output.ProductPort;
import com.inditex.devtest.service.FailureClassifier;
import com.inditex.devtest.service.ParallelTaskExecutor;
import com.inditex.devtest.service.TaskResult;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchSimilarProductUseCaseImpl implements SearchSimilarProductUseCase {

	private final ProductPort productPort;

	private final ParallelTaskExecutor parallelTaskExecutor;

	@Override
	public Set<Product> fetchSimilarProducts(@Nonnull final String id) {
		final var productIds = this.productPort.fetchSimilarProductIds(id);

		if (productIds.isEmpty()) {
			return Set.of();
		}

		final List<TaskResult<Product>> results = this.parallelTaskExecutor.executeInParallel(productIds,
				this.productPort::fetchProductById);

		final long technicalFailures = results.stream().filter(result -> !result.isSuccess())
				.filter(result -> FailureClassifier.isTechnicalFailure(result.error())).count();

		if (technicalFailures == productIds.size()) {
			throw new RemoteException("All similar product detail lookups failed for product " + id,
					HttpStatus.BAD_GATEWAY.value(), null);
		}

		if (technicalFailures > 0) {
			log.warn("Partial failure fetching similar product details for product {}: {} of {} lookups failed", id,
					technicalFailures, productIds.size());
		}

		return results.stream().filter(TaskResult::isSuccess).map(TaskResult::value).collect(Collectors.toSet());
	}
}
