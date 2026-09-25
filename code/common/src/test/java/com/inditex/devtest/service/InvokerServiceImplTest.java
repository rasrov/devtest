package com.inditex.devtest.service;

import com.inditex.devtest.exception.RemoteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;

import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

class InvokerServiceImplTest {

	private InvokerServiceImpl invokerService;

	@BeforeEach
	void beforeEach() {
		this.invokerService = new InvokerServiceImpl();
	}

	@Nested
	class Invoke {

		@Test
		void when_operation_succeeds_expect_result_returned() {
			final Supplier<String> operation = () -> "ok";
			final Function<RuntimeException, RemoteException> errorHandler = e -> new RemoteException("unused", 500, e);

			final String result = invokerService.invoke(operation, errorHandler);

			assertThat(result).isEqualTo("ok");
		}

		@Test
		void when_operation_throws_rest_client_exception_expect_error_handler_applied() {
			final RestClientException restClientException = new RestClientException("upstream failed");
			final Supplier<String> operation = () -> {
				throw restClientException;
			};
			final Function<RuntimeException, RemoteException> errorHandler = e -> new RemoteException("mapped",
					HttpStatus.BAD_GATEWAY.value(), e);

			final Throwable thrown = catchThrowable(() -> invokerService.invoke(operation, errorHandler));

			assertThat(thrown).isInstanceOf(RemoteException.class).hasMessage("mapped");
			assertThat(((RemoteException) thrown).getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
			assertThat(thrown.getCause()).isSameAs(restClientException);
		}

		@Test
		void when_operation_throws_non_rest_client_runtime_exception_expect_it_propagates_unwrapped() {
			final Supplier<String> operation = () -> {
				throw new IllegalStateException("bug");
			};
			final Function<RuntimeException, RemoteException> errorHandler = e -> new RemoteException("mapped", 502, e);

			assertThatThrownBy(() -> invokerService.invoke(operation, errorHandler))
					.isInstanceOf(IllegalStateException.class).hasMessage("bug");
		}
	}
}
