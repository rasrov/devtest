package com.inditex.devtest.port.output;

import com.inditex.devtest.model.product.Product;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Optional;

public interface ProductPort {

    List<Integer> fetchSimilarProductIds(@Nonnull Integer productId);

    Optional<Product> fetchProductById(@Nonnull Integer productId);

}
