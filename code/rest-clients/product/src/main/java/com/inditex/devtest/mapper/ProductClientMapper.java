package com.inditex.devtest.mapper;

import com.inditex.devtest.model.product.Product;
import com.inditex.devtest.product.client.model.ProductDetail;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductClientMapper {

	Product toProduct(ProductDetail productDetail);

}
