package com.inditex.devtest.mapper;

import com.inditex.devtest.model.product.Product;
import com.inditex.devtest.model.product.ProductEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("ProductMapper Tests")
class ProductMapperTest {

	private final ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);

	@Test
	@DisplayName("toProduct debe mapear todos los campos de ProductEntity a Product")
	void shouldMapAllFields() {
		// Arrange
		final ProductEntity entity = new ProductEntity(1, "Product 1", 19.99, Boolean.TRUE);

		// Act
		final Product result = this.productMapper.toProduct(entity);

		// Assert
		assertEquals(entity.getId(), result.id());
		assertEquals(entity.getName(), result.name());
		assertEquals(entity.getPrice(), result.price());
		assertEquals(entity.getAvailability(), result.availability());
	}

	@Test
	@DisplayName("toProduct debe devolver null cuando la entidad es null")
	void shouldReturnNullWhenEntityIsNull() {
		// Act
		final Product result = this.productMapper.toProduct(null);

		// Assert
		assertNull(result);
	}

	@Test
	@DisplayName("toProduct debe mapear correctamente cuando availability es false")
	void shouldMapWhenNotAvailable() {
		// Arrange
		final ProductEntity entity = new ProductEntity(2, "Product 2", 0.0, Boolean.FALSE);

		// Act
		final Product result = this.productMapper.toProduct(entity);

		// Assert
		assertEquals(2, result.id());
		assertEquals("Product 2", result.name());
		assertEquals(0.0, result.price());
		assertEquals(Boolean.FALSE, result.availability());
	}
}

