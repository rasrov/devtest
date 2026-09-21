package com.inditex.devtest.usecase;

import com.inditex.devtest.application.ParallelTaskExecutor;
import com.inditex.devtest.model.product.Product;
import com.inditex.devtest.port.input.product.SearchSimilarProductUseCase;
import com.inditex.devtest.port.output.ProductPort;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SerchSimilarProductUseCaseImpl implements SearchSimilarProductUseCase {

    private final ProductPort productPort;

    private final ParallelTaskExecutor parallelTaskExecutor;

    @Override
    public List<Product> fetchSimilarProducts(@Nonnull final Integer id) {
        final var productIds = this.productPort.fetchSimilarProductIds(id);

        return this.parallelTaskExecutor.executeInParallelToList(productIds, this.productPort::fetchProductById).stream()
                .flatMap(Optional::stream)
                .toList();
    }
}
