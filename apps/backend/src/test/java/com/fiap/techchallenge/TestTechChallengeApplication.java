package com.fiap.techchallenge;

import org.springframework.boot.SpringApplication;

public class TestTechChallengeApplication {

    public static void main(String[] args) {
        SpringApplication.from(TechChallengeApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
