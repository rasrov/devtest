package com.inditex.devtest.input.rest.mapper;

import com.inditex.devtest.infrastructure.api.model.ProductDetail;
import com.inditex.devtest.model.product.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

	private ProductMapper mapper;

	@BeforeEach
	void beforeEach() {
		this.mapper = Mappers.getMapper(ProductMapper.class);
	}

	private static Product product(final String id, final String name, final BigDecimal price,
			final Boolean availability) {
		return new Product(id, name, price, availability);
	}

	@Nested
	class ToProductDetail {

		@Test
		void when_full_product_expect_all_fields_mapped() {
			final Product source = product("1", "Shirt", new BigDecimal("9.99"), true);

			final ProductDetail result = mapper.toProductDetail(source);

			assertThat(result).isNotNull();
			assertThat(result.getId()).isEqualTo("1");
			assertThat(result.getName()).isEqualTo("Shirt");
			assertThat(result.getPrice()).isEqualByComparingTo("9.99");
			assertThat(result.getAvailability()).isTrue();
		}

		@Test
		void when_null_product_expect_null_detail() {
			final ProductDetail result = mapper.toProductDetail(null);

			assertThat(result).isNull();
		}
	}

	@Nested
	class ToProductDetailSet {

		@Test
		void when_products_present_expect_all_mapped() {
			final Set<Product> source = Set.of(product("1", "Shirt", new BigDecimal("9.99"), true),
					product("2", "Blazer", new BigDecimal("29.99"), false));

			final Set<ProductDetail> result = mapper.toProductDetailSet(source);

			assertThat(result).hasSize(2).extracting(ProductDetail::getId).containsExactlyInAnyOrder("1", "2");
		}

		@Test
		void when_empty_set_expect_empty_result() {
			final Set<ProductDetail> result = mapper.toProductDetailSet(Set.of());

			assertThat(result).isEmpty();
		}

		@Test
		void when_null_set_expect_null_result() {
			final Set<ProductDetail> result = mapper.toProductDetailSet(null);

			assertThat(result).isNull();
		}
	}
}
