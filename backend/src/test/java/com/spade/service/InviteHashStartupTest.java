package com.spade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.spade.SpadeApplication;
import com.spade.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * The real application, started with the group's invite code typed where its hash belongs. It must refuse
 * to start, and the code (a secret) must reach neither the failure report nor the exception chain.
 */
@ExtendWith(OutputCaptureExtension.class)
class InviteHashStartupTest {

    @Test
    void aPlaintextCodeInTheHashSettingStopsStartupWithoutBeingPrinted(CapturedOutput output) {
        assertThatThrownBy(() -> new SpringApplicationBuilder(SpadeApplication.class, TestcontainersConfiguration.class)
                .web(WebApplicationType.NONE)
                .run("--spade.invite-code-sha256=letmein"))
                .satisfies(e -> {
                    for (Throwable t = e; t != null; t = t.getCause()) {
                        assertThat(String.valueOf(t.getMessage())).doesNotContain("letmein");
                    }
                });
        assertThat(output.getAll()).contains("SPADE_INVITE_CODE_SHA256").doesNotContain("letmein");
    }
}
