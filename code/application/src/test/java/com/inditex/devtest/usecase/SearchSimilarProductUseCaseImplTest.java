package com.inditex.devtest.usecase;

import com.inditex.devtest.exception.NotFoundException;
import com.inditex.devtest.exception.RemoteException;
import com.inditex.devtest.model.product.Product;
import com.inditex.devtest.port.output.ProductPort;
import com.inditex.devtest.service.ParallelTaskExecutor;
import com.inditex.devtest.service.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchSimilarProductUseCaseImplTest {

	private SearchSimilarProductUseCaseImpl useCase;

	@Mock
	private ProductPort productPort;

	@Mock
	private ParallelTaskExecutor parallelTaskExecutor;

	@Captor
	private ArgumentCaptor<Set<String>> idsCaptor;

	@BeforeEach
	void beforeEach() {
		this.useCase = new SearchSimilarProductUseCaseImpl(this.productPort, this.parallelTaskExecutor);
	}

	private static Product product(final String id) {
		return new Product(id, "Product " + id, new BigDecimal("19.99"), true);
	}

	@Nested
	class FetchSimilarProducts {

		@Test
		void when_no_similar_ids_expect_empty_set_without_executing_details() {
			when(SearchSimilarProductUseCaseImplTest.this.productPort.fetchSimilarProductIds("1")).thenReturn(Set.of());

			final Set<Product> result = SearchSimilarProductUseCaseImplTest.this.useCase.fetchSimilarProducts("1");

			assertThat(result).isEmpty();
			verify(SearchSimilarProductUseCaseImplTest.this.parallelTaskExecutor, never()).executeInParallel(any(),
					any());
		}

		@Test
		void when_all_details_succeed_expect_all_products() {
			when(SearchSimilarProductUseCaseImplTest.this.productPort.fetchSimilarProductIds("1"))
					.thenReturn(Set.of("2", "3"));
			when(SearchSimilarProductUseCaseImplTest.this.parallelTaskExecutor.executeInParallel(eq(Set.of("2", "3")),
					any())).thenReturn(List.of(TaskResult.success(product("2")), TaskResult.success(product("3"))));

			final Set<Product> result = SearchSimilarProductUseCaseImplTest.this.useCase.fetchSimilarProducts("1");

			assertThat(result).containsExactlyInAnyOrder(product("2"), product("3"));
		}

		@Test
		void when_some_details_fail_technically_expect_partial_best_effort() {
			when(SearchSimilarProductUseCaseImplTest.this.productPort.fetchSimilarProductIds("1"))
					.thenReturn(Set.of("2", "3", "4"));
			when(SearchSimilarProductUseCaseImplTest.this.parallelTaskExecutor.executeInParallel(any(), any()))
					.thenReturn(List.of(TaskResult.success(product("2")), TaskResult.success(product("3")),
							TaskResult.failure(new RemoteException("boom", 500, null))));

			final Set<Product> result = SearchSimilarProductUseCaseImplTest.this.useCase.fetchSimilarProducts("1");

			assertThat(result).containsExactlyInAnyOrder(product("2"), product("3"));
		}

		@Test
		void when_detail_fails_with_not_found_expect_it_not_counted_as_technical_failure() {
			when(SearchSimilarProductUseCaseImplTest.this.productPort.fetchSimilarProductIds("1"))
					.thenReturn(Set.of("2", "3"));
			when(SearchSimilarProductUseCaseImplTest.this.parallelTaskExecutor.executeInParallel(any(), any()))
					.thenReturn(List.of(TaskResult.success(product("2")),
							TaskResult.failure(new NotFoundException("missing detail"))));

			final Set<Product> result = SearchSimilarProductUseCaseImplTest.this.useCase.fetchSimilarProducts("1");

			assertThat(result).containsExactly(product("2"));
		}

		@Test
		void when_all_details_fail_technically_expect_remote_exception() {
			when(SearchSimilarProductUseCaseImplTest.this.productPort.fetchSimilarProductIds("1"))
					.thenReturn(Set.of("2", "3"));
			when(SearchSimilarProductUseCaseImplTest.this.parallelTaskExecutor.executeInParallel(any(), any()))
					.thenReturn(List.of(TaskResult.failure(new RemoteException("boom", 500, null)),
							TaskResult.failure(new TimeoutException("slow"))));

			final Throwable thrown = catchThrowable(
					() -> SearchSimilarProductUseCaseImplTest.this.useCase.fetchSimilarProducts("1"));

			assertThat(thrown).isInstanceOf(RemoteException.class);
			assertThat(((RemoteException) thrown).getStatusCode()).isEqualTo(502);
		}

		@Test
		void when_all_details_fail_but_some_are_not_found_expect_partial_not_remote_exception() {
			when(SearchSimilarProductUseCaseImplTest.this.productPort.fetchSimilarProductIds("1"))
					.thenReturn(Set.of("2", "3"));
			when(SearchSimilarProductUseCaseImplTest.this.parallelTaskExecutor.executeInParallel(any(), any()))
					.thenReturn(List.of(TaskResult.failure(new NotFoundException("missing 2")),
							TaskResult.failure(new NotFoundException("missing 3"))));

			final Set<Product> result = SearchSimilarProductUseCaseImplTest.this.useCase.fetchSimilarProducts("1");

			assertThat(result).isEmpty();
		}

		@Test
		void when_fetching_details_expect_ids_forwarded_to_executor() {
			when(SearchSimilarProductUseCaseImplTest.this.productPort.fetchSimilarProductIds("1"))
					.thenReturn(Set.of("2", "3"));
			when(SearchSimilarProductUseCaseImplTest.this.parallelTaskExecutor
					.executeInParallel(SearchSimilarProductUseCaseImplTest.this.idsCaptor.capture(), any()))
					.thenReturn(List.of(TaskResult.success(product("2")), TaskResult.success(product("3"))));

			SearchSimilarProductUseCaseImplTest.this.useCase.fetchSimilarProducts("1");

			assertThat(SearchSimilarProductUseCaseImplTest.this.idsCaptor.getValue()).containsExactlyInAnyOrder("2",
					"3");
			verify(SearchSimilarProductUseCaseImplTest.this.productPort, times(1)).fetchSimilarProductIds("1");
		}

		@Test
		void when_ids_propagates_not_found_expect_exception_bubbles_up() {
			when(SearchSimilarProductUseCaseImplTest.this.productPort.fetchSimilarProductIds("1"))
					.thenThrow(new NotFoundException("no similar ids"));

			assertThatThrownBy(() -> SearchSimilarProductUseCaseImplTest.this.useCase.fetchSimilarProducts("1"))
					.isInstanceOf(NotFoundException.class);
			verify(SearchSimilarProductUseCaseImplTest.this.parallelTaskExecutor, never()).executeInParallel(any(),
					any());
		}
	}

	@Nested
	class DetailTaskDelegation {

		@Test
		void when_executing_details_expect_task_delegates_to_product_port() {
			when(SearchSimilarProductUseCaseImplTest.this.productPort.fetchSimilarProductIds("1"))
					.thenReturn(Set.of("2"));
			final ArgumentCaptor<Function<String, Product>> taskCaptor = ArgumentCaptor.forClass(Function.class);
			when(SearchSimilarProductUseCaseImplTest.this.parallelTaskExecutor.executeInParallel(any(),
					taskCaptor.capture())).thenReturn(List.of(TaskResult.success(product("2"))));
			when(SearchSimilarProductUseCaseImplTest.this.productPort.fetchProductById("2")).thenReturn(product("2"));

			SearchSimilarProductUseCaseImplTest.this.useCase.fetchSimilarProducts("1");

			final Product resolved = taskCaptor.getValue().apply("2");
			assertThat(resolved).isEqualTo(product("2"));
			verify(SearchSimilarProductUseCaseImplTest.this.productPort, times(1)).fetchProductById("2");
		}
	}
}
