package com.inditex.devtest.service;

import com.inditex.devtest.exception.NotFoundException;
import com.inditex.devtest.exception.RemoteException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FailureClassifierTest {

	@Nested
	class IsTechnicalFailure {

		static Stream<Arguments> technicalFailures() {
			return Stream.of(Arguments.of(new RemoteException("boom", 500, null)),
					Arguments.of(new TimeoutException("slow")),
					Arguments.of(new RejectedExecutionException("saturated")));
		}

		@ParameterizedTest
		@MethodSource("technicalFailures")
		void when_technical_failure_expect_true(final Throwable error) {
			final boolean result = FailureClassifier.isTechnicalFailure(error);

			assertThat(result).isTrue();
		}

		@Test
		void when_not_found_expect_false() {
			final boolean result = FailureClassifier.isTechnicalFailure(new NotFoundException("missing"));

			assertThat(result).isFalse();
		}

		@Test
		void when_generic_runtime_error_expect_false() {
			final boolean result = FailureClassifier.isTechnicalFailure(new IllegalStateException("other"));

			assertThat(result).isFalse();
		}
	}
}
