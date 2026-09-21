package com.inditex.devtest.mapper;

import com.inditex.devtest.model.product.Product;
import com.inditex.devtest.model.product.ProductEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {

	Product toProduct(ProductEntity productEntity);

}
