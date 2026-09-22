package com.firmaa.techdept;

import org.springframework.boot.SpringApplication;

public class TestTechdeptApplication {

	public static void main(String[] args) {
		SpringApplication.from(TechdeptApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
