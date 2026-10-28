package com.fiap.workshop;

import org.springframework.boot.SpringApplication;

public class TestWorkshopApplication {

	public static void main(String[] args) {
		SpringApplication.from(WorkshopApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
