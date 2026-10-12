package com.spade;

import org.springframework.boot.SpringApplication;

/** Runs the app on a throwaway PostgreSQL: ./mvnw spring-boot:test-run -Dspring-boot.run.profiles=testworld */
public class TestSpadeApplication {

    public static void main(String[] args) {
        SpringApplication.from(SpadeApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
