package com.spade.testworld;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** The testworld's time stands still, so its responses are the same on every run (the contract fixtures). */
@Configuration(proxyBeanMethods = false)
@Profile("testworld")
class TestworldClock {

    static final Instant NOW = Instant.parse("2026-10-09T19:00:00Z");

    @Bean
    Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}
