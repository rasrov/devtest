package com.inditex.devtest.port.input.product;

import com.inditex.devtest.model.product.Product;
import jakarta.annotation.Nonnull;

import java.util.List;

public interface SearchSimilarProductUseCase {

    List<Product> fetchSimilarProducts(@Nonnull Integer id);

}
