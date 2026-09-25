package com.inditex.devtest;

import com.inditex.devtest.exception.NotFoundException;
import com.inditex.devtest.exception.RemoteException;
import com.inditex.devtest.mapper.ProductClientMapper;
import com.inditex.devtest.model.product.Product;
import com.inditex.devtest.product.client.api.DefaultApi;
import com.inditex.devtest.product.client.model.ProductDetail;
import com.inditex.devtest.service.InvokerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductAdapterTest {

	private ProductAdapter productAdapter;

	@Mock
	private DefaultApi defaultApi;

	@Mock
	private ProductClientMapper productMapper;

	@Mock
	private InvokerService invokerService;

	@BeforeEach
	void beforeEach() {
		this.productAdapter = new ProductAdapter(this.defaultApi, this.productMapper, this.invokerService);
	}

	@SuppressWarnings("unchecked")
	private <T> void whenInvokerReturns(final T value) {
		when(this.invokerService.invoke(any(Supplier.class), any(Function.class))).thenReturn(value);
	}

	@SuppressWarnings("unchecked")
	private void whenInvokerThrows(final RuntimeException exception) {
		when(this.invokerService.invoke(any(Supplier.class), any(Function.class))).thenThrow(exception);
	}

	private static Product product(final String id) {
		return new Product(id, "Product " + id, new BigDecimal("19.99"), true);
	}

	@Nested
	class FetchSimilarProductIds {

		@Test
		void when_ids_returned_expect_them() {
			whenInvokerReturns(Set.of("2", "3"));

			final Set<String> result = productAdapter.fetchSimilarProductIds("1");

			assertThat(result).containsExactlyInAnyOrder("2", "3");
		}

		@Test
		void when_upstream_returns_404_expect_not_found_exception() {
			whenInvokerThrows(new RemoteException("upstream 404", HttpStatus.NOT_FOUND.value(), null));

			final Throwable thrown = catchThrowable(() -> productAdapter.fetchSimilarProductIds("1"));

			assertThat(thrown).isInstanceOf(NotFoundException.class);
		}

		@Test
		void when_upstream_returns_other_error_expect_remote_exception_propagated() {
			final RemoteException remoteException = new RemoteException("upstream 500",
					HttpStatus.BAD_GATEWAY.value(), null);
			whenInvokerThrows(remoteException);

			assertThatThrownBy(() -> productAdapter.fetchSimilarProductIds("1")).isSameAs(remoteException);
		}
	}

	@Nested
	class FetchProductById {

		@Test
		void when_detail_returned_expect_mapped_product() {
			final ProductDetail detail = new ProductDetail();
			whenInvokerReturns(detail);
			when(productMapper.toProduct(detail)).thenReturn(product("1"));

			final Product result = productAdapter.fetchProductById("1");

			assertThat(result).isEqualTo(product("1"));
			verify(productMapper, times(1)).toProduct(detail);
		}

		@Test
		void when_upstream_returns_404_expect_not_found_exception() {
			whenInvokerThrows(new RemoteException("upstream 404", HttpStatus.NOT_FOUND.value(), null));

			final Throwable thrown = catchThrowable(() -> productAdapter.fetchProductById("1"));

			assertThat(thrown).isInstanceOf(NotFoundException.class);
		}

		@Test
		void when_upstream_returns_other_error_expect_remote_exception_propagated() {
			final RemoteException remoteException = new RemoteException("upstream 500",
					HttpStatus.BAD_GATEWAY.value(), null);
			whenInvokerThrows(remoteException);

			assertThatThrownBy(() -> productAdapter.fetchProductById("1")).isSameAs(remoteException);
		}
	}

	@Nested
	class Fallback {

		@Test
		void when_fallback_receives_not_found_expect_it_propagated_unchanged() {
			final NotFoundException notFound = new NotFoundException("legitimate absence");

			final Throwable thrown = catchThrowable(
					() -> productAdapter.fetchSimilarProductIdsFallback("1", notFound));

			assertThat(thrown).isSameAs(notFound);
		}

		@Test
		void when_detail_fallback_receives_not_found_expect_it_propagated_unchanged() {
			final NotFoundException notFound = new NotFoundException("legitimate absence");

			final Throwable thrown = catchThrowable(() -> productAdapter.fetchProductByIdFallback("1", notFound));

			assertThat(thrown).isSameAs(notFound);
		}

		@Test
		void when_fallback_receives_remote_exception_expect_remote_exception() {
			final RemoteException remoteException = new RemoteException("upstream error",
					HttpStatus.BAD_GATEWAY.value(), null);

			final Throwable thrown = catchThrowable(
					() -> productAdapter.fetchSimilarProductIdsFallback("1", remoteException));

			assertThat(thrown).isSameAs(remoteException);
		}

		@Test
		void when_fallback_receives_other_throwable_expect_wrapped_as_remote_exception() {
			final Throwable circuitOpen = new IllegalStateException("circuit open");

			final Throwable thrown = catchThrowable(
					() -> productAdapter.fetchSimilarProductIdsFallback("1", circuitOpen));

			assertThat(thrown).isInstanceOf(RemoteException.class);
			assertThat(((RemoteException) thrown).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
			assertThat(thrown.getCause()).isSameAs(circuitOpen);
		}
	}
}
