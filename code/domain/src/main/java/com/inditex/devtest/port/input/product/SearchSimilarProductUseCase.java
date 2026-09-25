package com.inditex.devtest.port.input.product;

import com.inditex.devtest.model.product.Product;
import jakarta.annotation.Nonnull;

import java.util.Set;

public interface SearchSimilarProductUseCase {

	Set<Product> fetchSimilarProducts(@Nonnull String id);

}
