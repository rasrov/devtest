package com.inditex.devtest.port.output;

import com.inditex.devtest.model.product.Product;
import jakarta.annotation.Nonnull;

import java.util.Set;

public interface ProductPort {

	Set<String> fetchSimilarProductIds(@Nonnull String productId);

	Product fetchProductById(@Nonnull String productId);

}
