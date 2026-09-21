package com.inditex.devtest.model.product;

import jakarta.annotation.Nonnull;

import java.io.Serializable;

public record Product(@Nonnull Integer id, @Nonnull String name, @Nonnull Double price, @Nonnull Boolean availability) implements Serializable {
}
