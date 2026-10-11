package com.spade.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** A missing or malformed invite-code hash stops the app at startup, on purpose. */
class SpadePropertiesTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SpadeProperties.class)
    static class Props {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Props.class)
            .withPropertyValues("spade.login.max-failures=5", "spade.login.lockout=PT5M");

    @Test
    void refusesToStartWithoutTheInviteCodeHash() {
        runner.run(context -> assertThat(context).hasFailed()
                .getFailure().hasStackTraceContaining("inviteCodeSha256"));
    }

    @Test
    void refusesAHashThatIsNotSha256Hex() {
        runner.withPropertyValues("spade.invite-code-sha256=letmein")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasStackTraceContaining("SHA-256"));
    }

    @Test
    void bindsAValidConfiguration() {
        runner.withPropertyValues("spade.invite-code-sha256=1c8bfe8f801d79745c4631d09fff36c82aa37fc4cce4fc946683d7b336b63032")
                .run(context -> {
                    SpadeProperties props = context.getBean(SpadeProperties.class);
                    assertThat(props.login().maxFailures()).isEqualTo(5);
                    assertThat(props.login().lockout()).isEqualTo(Duration.ofMinutes(5));
                });
    }
}
