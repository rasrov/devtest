package com.inditex.devtest.model.product;

import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.math.BigDecimal;

public record Product(@Nonnull String id, @Nonnull String name, @Nonnull BigDecimal price,
		@Nonnull Boolean availability) implements Serializable {
}
