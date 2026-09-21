package com.inditex.devtest.model.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = -8613430178657683764L;

	private Integer id;

	private String name;

	private Double price;

	private Boolean availability;

}
