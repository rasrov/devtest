package com.inditex.devtest.service;

import com.inditex.devtest.application.ParallelTaskExecutor;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

@Service
@AllArgsConstructor
public class ParallelTaskExecutorImpl implements ParallelTaskExecutor {

    private final ExecutorService executorService;

    @Override
    public <T, R> List<R> executeInParallelToList(Collection<T> input, Function<T, R> task) {
        final List<CompletableFuture<R>> futures = input.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> task.apply(item), this.executorService))
                .toList();

        try {
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .map(CompletableFuture::join)
                            .toList())
                    .get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (final ExecutionException e) {
            return Collections.emptyList();
        }
    }
}
