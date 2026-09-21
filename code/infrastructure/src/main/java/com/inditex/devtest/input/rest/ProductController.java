package com.inditex.devtest.input.rest;

import com.inditex.devtest.model.product.Product;
import com.inditex.devtest.port.input.product.SearchSimilarProductUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final SearchSimilarProductUseCase searchSimilarProductUseCase;

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/products/{productId}/similar",
            produces = { "application/json" }
    )
    public ResponseEntity<List<Product>> fetchSimilarProducts(@PathVariable final Integer productId) {
        final var products = this.searchSimilarProductUseCase.fetchSimilarProducts(productId);
        return ResponseEntity.ok(products);
    }

}
