package com.inditex.devtest.input.rest.controller;

import com.inditex.devtest.infrastructure.api.DefaultApi;
import com.inditex.devtest.infrastructure.api.model.ProductDetail;
import com.inditex.devtest.input.rest.mapper.ProductMapper;
import com.inditex.devtest.port.input.product.SearchSimilarProductUseCase;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequiredArgsConstructor
public class ProductController implements DefaultApi {

	private final SearchSimilarProductUseCase searchSimilarProductUseCase;

	private final ProductMapper productMapper;

	@Override
	public ResponseEntity<Set<ProductDetail>> getProductSimilar(@Nonnull final String productId) {
		final var products = this.searchSimilarProductUseCase.fetchSimilarProducts(productId);
		return ResponseEntity.ok(this.productMapper.toProductDetailSet(products));
	}
}
