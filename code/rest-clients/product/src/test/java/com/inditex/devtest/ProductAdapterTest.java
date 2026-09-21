package com.inditex.devtest;

import com.inditex.devtest.application.InvokerService;
import com.inditex.devtest.exception.NotFoundException;
import com.inditex.devtest.exception.RemoteException;
import com.inditex.devtest.exception.rest.BadRequestException;
import com.inditex.devtest.mapper.ProductMapper;
import com.inditex.devtest.model.product.Product;
import com.inditex.devtest.model.product.ProductEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductAdapter Tests")
class ProductAdapterTest {

	@Mock
	private RestClient productRestClient;

	@Mock
	private ProductMapper productMapper;

	@Mock
	private InvokerService invokerService;

	@InjectMocks
	private ProductAdapter productAdapter;

	private static final Integer PRODUCT_ID = 1;

	// ---------- fetchSimilarProductIds ----------

	@Test
	@DisplayName("fetchSimilarProductIds debe devolver la lista de ids")
	void shouldFetchSimilarProductIds() {
		// Arrange
		final List<Integer> ids = List.of(2, 3, 4);
		when(this.invokerService.<List<Integer>>invoke(any(), any()))
				.thenReturn(ids);

		// Act
		final List<Integer> result = this.productAdapter.fetchSimilarProductIds(PRODUCT_ID);

		// Assert
		assertEquals(ids, result);
		verify(this.invokerService, times(1)).invoke(any(), any());
	}

	@Test
	@DisplayName("fetchSimilarProductIds debe lanzar NotFoundException cuando el remoto responde 404")
	void shouldThrowNotFoundWhenSimilarIdsNotFound() {
		// Arrange
		final RemoteException remote = new RemoteException("not found", HttpStatus.NOT_FOUND.value(), new RuntimeException());
		when(this.invokerService.<List<Integer>>invoke(any(), any()))
				.thenThrow(remote);

		// Act & Assert
		assertThrows(NotFoundException.class, () -> this.productAdapter.fetchSimilarProductIds(PRODUCT_ID));
	}

	@Test
	@DisplayName("fetchSimilarProductIds debe propagar el error mapeado cuando el remoto responde 400")
	void shouldPropagateMappedErrorWhenSimilarIdsBadRequest() {
		// Arrange
		final RemoteException remote = new RemoteException("bad request", HttpStatus.BAD_REQUEST.value(), new RuntimeException());
		when(this.invokerService.<List<Integer>>invoke(any(), any()))
				.thenThrow(remote);

		// Act & Assert
		assertThrows(BadRequestException.class, () -> this.productAdapter.fetchSimilarProductIds(PRODUCT_ID));
	}

	@Test
	@DisplayName("fetchSimilarProductIds debe propagar RemoteException cuando el remoto responde 500")
	void shouldPropagateRemoteExceptionWhenSimilarIdsServerError() {
		// Arrange
		final RemoteException remote = new RemoteException("server error", HttpStatus.INTERNAL_SERVER_ERROR.value(), new RuntimeException());
		when(this.invokerService.<List<Integer>>invoke(any(), any()))
				.thenThrow(remote);

		// Act & Assert
		assertThrows(RemoteException.class, () -> this.productAdapter.fetchSimilarProductIds(PRODUCT_ID));
	}

	// ---------- fetchProductById ----------

	@Test
	@DisplayName("fetchProductById debe devolver el producto mapeado")
	void shouldFetchProductById() {
		// Arrange
		final ProductEntity entity = new ProductEntity(PRODUCT_ID, "Product 1", 19.99, Boolean.TRUE);
		final Product product = new Product(PRODUCT_ID, "Product 1", 19.99, Boolean.TRUE);
		when(this.invokerService.<Optional<ProductEntity>>invoke(any(), any()))
				.thenReturn(Optional.of(entity));
		when(this.productMapper.toProduct(entity)).thenReturn(product);

		// Act
		final Optional<Product> result = this.productAdapter.fetchProductById(PRODUCT_ID);

		// Assert
		assertTrue(result.isPresent());
		assertEquals(product, result.get());
		verify(this.productMapper, times(1)).toProduct(entity);
	}

	@Test
	@DisplayName("fetchProductById debe devolver Optional vacío cuando el remoto devuelve vacío")
	void shouldReturnEmptyWhenRemoteReturnsEmpty() {
		// Arrange
		when(this.invokerService.<Optional<ProductEntity>>invoke(any(), any()))
				.thenReturn(Optional.empty());

		// Act
		final Optional<Product> result = this.productAdapter.fetchProductById(PRODUCT_ID);

		// Assert
		assertTrue(result.isEmpty());
	}

	@Test
	@DisplayName("fetchProductById debe devolver Optional vacío cuando el remoto responde 404")
	void shouldReturnEmptyWhenProductNotFound() {
		// Arrange
		final RemoteException remote = new RemoteException("not found", HttpStatus.NOT_FOUND.value(), new RuntimeException());
		when(this.invokerService.<Optional<ProductEntity>>invoke(any(), any()))
				.thenThrow(remote);

		// Act
		final Optional<Product> result = this.productAdapter.fetchProductById(PRODUCT_ID);

		// Assert
		assertTrue(result.isEmpty());
	}

	@Test
	@DisplayName("fetchProductById debe propagar el error mapeado cuando el remoto responde 400")
	void shouldPropagateMappedErrorWhenProductBadRequest() {
		// Arrange
		final RemoteException remote = new RemoteException("bad request", HttpStatus.BAD_REQUEST.value(), new RuntimeException());
		when(this.invokerService.<Optional<ProductEntity>>invoke(any(), any()))
				.thenThrow(remote);

		// Act & Assert
		assertThrows(BadRequestException.class, () -> this.productAdapter.fetchProductById(PRODUCT_ID));
	}

	// ---------- fallbacks ----------

	@Test
	@DisplayName("fetchSimilarProductIdsFallback debe lanzar el error mapeado")
	void shouldThrowOnSimilarIdsFallback() {
		// Arrange
		final RemoteException remote = new RemoteException("bad request", HttpStatus.BAD_REQUEST.value(), new RuntimeException());

		// Act & Assert
		assertThrows(BadRequestException.class,
				() -> this.productAdapter.fetchSimilarProductIdsFallback(PRODUCT_ID, remote));
	}

	@Test
	@DisplayName("fetchProductByIdFallback debe lanzar el error mapeado")
	void shouldThrowOnProductByIdFallback() {
		// Arrange
		final RemoteException remote = new RemoteException("bad request", HttpStatus.BAD_REQUEST.value(), new RuntimeException());

		// Act & Assert
		assertThrows(BadRequestException.class,
				() -> this.productAdapter.fetchProductByIdFallback(PRODUCT_ID, remote));
	}
}

