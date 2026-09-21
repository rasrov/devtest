package com.inditex.devtest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(proxyBeanMethods = false)
@EnableCaching
public class Application {

	public static void main(final String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
