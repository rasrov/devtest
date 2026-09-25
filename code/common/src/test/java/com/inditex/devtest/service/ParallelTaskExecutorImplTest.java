package com.inditex.devtest.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class ParallelTaskExecutorImplTest {

	private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

	@AfterEach
	void afterEach() {
		this.executorService.shutdownNow();
	}

	private ParallelTaskExecutorImpl executor(final int permits, final Duration globalTimeout) {
		return new ParallelTaskExecutorImpl(this.executorService, new Semaphore(permits), globalTimeout);
	}

	@Nested
	class ExecuteInParallel {

		@Test
		void when_all_tasks_succeed_expect_all_success_results() {
			final ParallelTaskExecutorImpl parallelExecutor = executor(10, Duration.ofSeconds(2));

			final List<TaskResult<String>> results = parallelExecutor.executeInParallel(List.of("a", "b", "c"),
					item -> item.toUpperCase());

			assertThat(results).hasSize(3);
			assertThat(results).allMatch(TaskResult::isSuccess);
			assertThat(results.stream().map(TaskResult::value)).containsExactlyInAnyOrder("A", "B", "C");
		}

		@Test
		void when_a_task_throws_expect_failure_result_with_error() {
			final ParallelTaskExecutorImpl parallelExecutor = executor(10, Duration.ofSeconds(2));
			final Function<String, String> task = item -> {
				if ("bad".equals(item)) {
					throw new IllegalStateException("boom");
				}
				return item;
			};

			final List<TaskResult<String>> results = parallelExecutor.executeInParallel(List.of("ok", "bad"), task);

			assertThat(results).hasSize(2);
			assertThat(results).filteredOn(TaskResult::isSuccess).extracting(TaskResult::value).containsExactly("ok");
			assertThat(results).filteredOn(r -> !r.isSuccess()).allSatisfy(
					r -> assertThat(r.error()).isInstanceOf(IllegalStateException.class).hasMessage("boom"));
		}

		@Test
		void when_task_exceeds_global_timeout_expect_timeout_failure() {
			final ParallelTaskExecutorImpl parallelExecutor = executor(10, Duration.ofMillis(100));
			final Function<String, String> slowTask = item -> {
				try {
					Thread.sleep(1000);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				return item;
			};

			final List<TaskResult<String>> results = parallelExecutor.executeInParallel(List.of("slow"), slowTask);

			assertThat(results).hasSize(1);
			assertThat(results.get(0).isSuccess()).isFalse();
			assertThat(results.get(0).error()).isInstanceOf(TimeoutException.class);
		}

		@Test
		void when_no_permits_available_expect_task_rejected() {
			final ParallelTaskExecutorImpl parallelExecutor = executor(0, Duration.ofSeconds(2));

			final List<TaskResult<String>> results = parallelExecutor.executeInParallel(List.of("a"), item -> item);

			assertThat(results).hasSize(1);
			assertThat(results.get(0).isSuccess()).isFalse();
			assertThat(results.get(0).error()).isInstanceOf(RejectedExecutionException.class);
		}

		@Test
		void when_permit_released_after_task_expect_next_task_can_acquire() {
			final ParallelTaskExecutorImpl parallelExecutor = executor(1, Duration.ofSeconds(2));

			final List<TaskResult<String>> first = parallelExecutor.executeInParallel(List.of("a"), item -> item);
			final List<TaskResult<String>> second = parallelExecutor.executeInParallel(List.of("b"), item -> item);

			assertThat(first.get(0).isSuccess()).isTrue();
			assertThat(second.get(0).isSuccess()).isTrue();
		}

		@Test
		void when_empty_input_expect_empty_results() {
			final ParallelTaskExecutorImpl parallelExecutor = executor(10, Duration.ofSeconds(2));

			final List<TaskResult<String>> results = parallelExecutor.executeInParallel(Set.<String>of(), item -> item);

			assertThat(results).isEmpty();
		}
	}
}
