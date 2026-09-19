package com.inditex.devtest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(proxyBeanMethods = false)
public class Application {

	static void main(final String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
