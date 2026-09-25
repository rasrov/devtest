package com.inditex.devtest.mapper;

import com.inditex.devtest.model.product.Product;
import com.inditex.devtest.product.client.model.ProductDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductClientMapperTest {

	private ProductClientMapper mapper;

	@BeforeEach
	void beforeEach() {
		this.mapper = Mappers.getMapper(ProductClientMapper.class);
	}

	private static ProductDetail detail(final String id, final String name, final BigDecimal price,
			final Boolean availability) {
		return new ProductDetail().id(id).name(name).price(price).availability(availability);
	}

	@Nested
	class ToProduct {

		@Test
		void when_full_detail_expect_all_fields_mapped() {
			final ProductDetail source = detail("1", "Shirt", new BigDecimal("9.99"), true);

			final Product result = mapper.toProduct(source);

			assertThat(result).isNotNull();
			assertThat(result.id()).isEqualTo("1");
			assertThat(result.name()).isEqualTo("Shirt");
			assertThat(result.price()).isEqualByComparingTo("9.99");
			assertThat(result.availability()).isTrue();
		}

		@Test
		void when_null_detail_expect_null_product() {
			final Product result = mapper.toProduct(null);

			assertThat(result).isNull();
		}

		@Test
		void when_availability_false_expect_mapped_as_false() {
			final ProductDetail source = detail("2", "Blazer", new BigDecimal("29.99"), false);

			final Product result = mapper.toProduct(source);

			assertThat(result.availability()).isFalse();
		}
	}
}
