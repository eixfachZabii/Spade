package com.spade.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Every timestamp comes from this clock, so tests and the testworld can fix it. */
@Configuration(proxyBeanMethods = false)
class ClockConfig {

    @Bean
    @Profile("!testworld")
    Clock clock() {
        return Clock.systemUTC();
    }
}
