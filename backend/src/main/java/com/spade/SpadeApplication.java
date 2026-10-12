package com.spade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/**
 * Spade's backend (ADR 0012). The generated-user auto-configuration is excluded: it would print a
 * password to the log, and Spade's sessions are its own (ADR 0011).
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class SpadeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpadeApplication.class, args);
    }
}
