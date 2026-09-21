package com.inditex.devtest.application;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public interface ParallelTaskExecutor {

    <T, R> List<R> executeInParallelToList(Collection<T> input, Function<T, R> task);

}
