package com.inditex.devtest.controller;

import com.inditex.devtest.exception.NotFoundException;
import com.inditex.devtest.model.product.Product;
import com.inditex.devtest.port.input.product.SearchSimilarProductUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DisplayName("ProductController Integration Tests")
class ProductControllerIT {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SearchSimilarProductUseCase searchSimilarProductUseCase;

	private static final Integer PRODUCT_ID = 1;

	@Test
	@DisplayName("GET /products/{id}/similar debe devolver 200 con la lista de productos similares")
	void shouldReturnSimilarProducts() throws Exception {
		// Arrange
		final List<Product> products = List.of(
				new Product(2, "Product 2", 19.99, Boolean.TRUE),
				new Product(3, "Product 3", 29.99, Boolean.FALSE)
		);
		when(this.searchSimilarProductUseCase.fetchSimilarProducts(eq(PRODUCT_ID))).thenReturn(products);

		// Act & Assert
		this.mockMvc.perform(get("/products/{productId}/similar", PRODUCT_ID)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
				.andExpect(jsonPath("$[0].id").value(2))
				.andExpect(jsonPath("$[0].name").value("Product 2"))
				.andExpect(jsonPath("$[0].price").value(19.99))
				.andExpect(jsonPath("$[0].availability").value(true))
				.andExpect(jsonPath("$[1].id").value(3))
				.andExpect(jsonPath("$[1].availability").value(false));

		verify(this.searchSimilarProductUseCase, times(1)).fetchSimilarProducts(PRODUCT_ID);
	}

	@Test
	@DisplayName("GET /products/{id}/similar debe devolver 200 con lista vacía cuando no hay similares")
	void shouldReturnEmptyListWhenNoSimilarProducts() throws Exception {
		// Arrange
		when(this.searchSimilarProductUseCase.fetchSimilarProducts(eq(PRODUCT_ID))).thenReturn(List.of());

		// Act & Assert
		this.mockMvc.perform(get("/products/{productId}/similar", PRODUCT_ID)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
	}

	@Test
	@DisplayName("GET /products/{id}/similar debe devolver 404 cuando el producto no existe")
	void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
		// Arrange
		when(this.searchSimilarProductUseCase.fetchSimilarProducts(eq(PRODUCT_ID)))
				.thenThrow(new NotFoundException("Similar product IDs not found for product ID: " + PRODUCT_ID));

		// Act & Assert
		this.mockMvc.perform(get("/products/{productId}/similar", PRODUCT_ID)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("GET /products/{id}/similar debe devolver 400 cuando el id no es numérico")
	void shouldReturnBadRequestWhenProductIdIsNotNumeric() throws Exception {
		// Act & Assert
		this.mockMvc.perform(get("/products/{productId}/similar", "abc")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}
}

