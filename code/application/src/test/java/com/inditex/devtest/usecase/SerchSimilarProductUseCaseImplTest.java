package com.inditex.devtest.usecase;

import com.inditex.devtest.application.ParallelTaskExecutor;
import com.inditex.devtest.model.product.Product;
import com.inditex.devtest.port.output.ProductPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SerchSimilarProductUseCaseImpl Tests")
class SerchSimilarProductUseCaseImplTest {

	@Mock
	private ProductPort productPort;

	@Mock
	private ParallelTaskExecutor parallelTaskExecutor;

	@InjectMocks
	private SerchSimilarProductUseCaseImpl useCase;

	private static final Integer PRODUCT_ID = 1;

	@Test
	@DisplayName("Debe devolver los productos similares correctamente")
	void shouldFetchSimilarProductsSuccessfully() {
		// Arrange
		final List<Integer> similarIds = List.of(2, 3, 4);
		final List<Optional<Product>> products = List.of(
				Optional.of(this.createProduct(2, "Product 2")),
				Optional.of(this.createProduct(3, "Product 3")),
				Optional.of(this.createProduct(4, "Product 4"))
		);

		when(this.productPort.fetchSimilarProductIds(PRODUCT_ID)).thenReturn(similarIds);
		when(this.parallelTaskExecutor.<Integer, Optional<Product>>executeInParallelToList(eq(similarIds), any()))
				.thenReturn(products);

		// Act
		final List<Product> result = this.useCase.fetchSimilarProducts(PRODUCT_ID);

		// Assert
		assertNotNull(result);
		assertEquals(3, result.size());
		assertEquals("Product 2", result.get(0).name());
		assertEquals("Product 3", result.get(1).name());
		assertEquals("Product 4", result.get(2).name());
		verify(this.productPort, times(1)).fetchSimilarProductIds(PRODUCT_ID);
		verify(this.parallelTaskExecutor, times(1)).executeInParallelToList(eq(similarIds), any());
	}

	@Test
	@DisplayName("Debe filtrar los Optional vacíos cuando algún producto no existe")
	void shouldFilterOutEmptyOptionals() {
		// Arrange
		final List<Integer> similarIds = List.of(2, 3, 4);
		final List<Optional<Product>> products = List.of(
				Optional.of(this.createProduct(2, "Product 2")),
				Optional.empty(),
				Optional.of(this.createProduct(4, "Product 4"))
		);

		when(this.productPort.fetchSimilarProductIds(PRODUCT_ID)).thenReturn(similarIds);
		when(this.parallelTaskExecutor.<Integer, Optional<Product>>executeInParallelToList(eq(similarIds), any()))
				.thenReturn(products);

		// Act
		final List<Product> result = this.useCase.fetchSimilarProducts(PRODUCT_ID);

		// Assert
		assertEquals(2, result.size());
		assertEquals("Product 2", result.get(0).name());
		assertEquals("Product 4", result.get(1).name());
	}

	@Test
	@DisplayName("Debe devolver lista vacía cuando no hay ids similares")
	void shouldReturnEmptyListWhenNoSimilarIds() {
		// Arrange
		when(this.productPort.fetchSimilarProductIds(PRODUCT_ID)).thenReturn(List.of());
		when(this.parallelTaskExecutor.<Integer, Optional<Product>>executeInParallelToList(eq(List.of()), any()))
				.thenReturn(List.of());

		// Act
		final List<Product> result = this.useCase.fetchSimilarProducts(PRODUCT_ID);

		// Assert
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	@DisplayName("Debe devolver lista vacía cuando todos los productos están vacíos")
	void shouldReturnEmptyListWhenAllOptionalsAreEmpty() {
		// Arrange
		final List<Integer> similarIds = List.of(2, 3);
		final List<Optional<Product>> products = List.of(Optional.empty(), Optional.empty());

		when(this.productPort.fetchSimilarProductIds(PRODUCT_ID)).thenReturn(similarIds);
		when(this.parallelTaskExecutor.<Integer, Optional<Product>>executeInParallelToList(eq(similarIds), any()))
				.thenReturn(products);

		// Act
		final List<Product> result = this.useCase.fetchSimilarProducts(PRODUCT_ID);

		// Assert
		assertTrue(result.isEmpty());
	}

	@Test
	@DisplayName("Debe invocar al executor paralelo con los ids obtenidos")
	void shouldInvokeParallelExecutorWithFetchedIds() {
		// Arrange
		final List<Integer> similarIds = List.of(2, 3);

		when(this.productPort.fetchSimilarProductIds(PRODUCT_ID)).thenReturn(similarIds);
		when(this.parallelTaskExecutor.<Integer, Optional<Product>>executeInParallelToList(eq(similarIds), any()))
				.thenReturn(List.of());

		// Act
		this.useCase.fetchSimilarProducts(PRODUCT_ID);

		// Assert
		verify(this.parallelTaskExecutor, times(1)).executeInParallelToList(eq(similarIds), any());
	}

	private Product createProduct(final Integer id, final String name) {
		return new Product(id, name, 19.99, Boolean.TRUE);
	}
}
