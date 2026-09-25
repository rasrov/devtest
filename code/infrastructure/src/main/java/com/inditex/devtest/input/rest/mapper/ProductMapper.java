package com.inditex.devtest.input.rest.mapper;

import com.inditex.devtest.infrastructure.api.model.ProductDetail;
import com.inditex.devtest.model.product.Product;
import org.mapstruct.Mapper;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface ProductMapper {

	Set<ProductDetail> toProductDetailSet(Set<Product> products);

	ProductDetail toProductDetail(Product product);

}
