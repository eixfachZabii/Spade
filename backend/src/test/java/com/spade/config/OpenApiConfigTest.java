package com.spade.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** A renamed path must not silently drop the error codes the contract promises for it. */
class OpenApiConfigTest {

    private static OpenAPI specWith(String path) {
        return new OpenAPI().paths(new Paths().addPathItem(path, new PathItem().post(new Operation())));
    }

    @Test
    void anExtraErrorsKeyThatMatchesNoOperationIsAnError() {
        Map<String, String[]> extra = Map.of("POST /api/v1/old-name", new String[] {"404"});
        assertThatThrownBy(() -> OpenApiConfig.requireEveryExtraErrorToMatch(specWith("/api/v1/new-name"), extra))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("POST /api/v1/old-name");
    }

    @Test
    void aWrongMethodIsAlsoUnmatched() {
        Map<String, String[]> extra = Map.of("GET /api/v1/things", new String[] {"404"});
        assertThatThrownBy(() -> OpenApiConfig.requireEveryExtraErrorToMatch(specWith("/api/v1/things"), extra))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void keysThatMatchPass() {
        Map<String, String[]> extra = Map.of("POST /api/v1/things", new String[] {"404"});
        assertThatCode(() -> OpenApiConfig.requireEveryExtraErrorToMatch(specWith("/api/v1/things"), extra))
                .doesNotThrowAnyException();
    }
}
