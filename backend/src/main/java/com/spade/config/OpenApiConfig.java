package com.spade.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * What {@code api/openapi.json} says beyond the controllers' signatures: the bearer scheme, the one error
 * shape (RFC 9457 problem details, {@code application/problem+json}) and which errors each operation can answer.
 *
 * <p>The error responses are listed per operation, not as one {@code default}, so a client generated from the
 * contract can handle exactly the failures that endpoint has. The rules are the ones the code follows:
 * <ul>
 * <li>400 where there is something to get wrong (a request body or a path variable);</li>
 * <li>401 on every operation that needs a session, and on login (wrong password);</li>
 * <li>500 everywhere;</li>
 * <li>the rest (403, 404, 409, 429) from {@link #EXTRA_ERRORS}, which mirrors the {@code DomainException}s the
 *     services throw. A new endpoint gets 400/401/500 for free and must add its own entry there.</li>
 * </ul>
 * Public operations (register, login) opt out of the global security requirement with an empty
 * {@code @SecurityRequirements} on the controller method; keep that in step with {@code SecurityConfig}.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    static final String BEARER = "bearer";
    static final String PROBLEM_SCHEMA = "ProblemDetail";
    private static final String PROBLEM_JSON = "application/problem+json";

    private static final Map<String, String> DESCRIPTIONS = Map.of(
            "400", "The request was not valid. `detail` names the field and how to fix it.",
            "401", "No valid session (or, for login, wrong username or password). Sign in again.",
            "403", "Not allowed for this player.",
            "404", "There is no such thing.",
            "409", "That clashes with the current state (a taken name, a full or closed table, a seat already held).",
            "429", "Too many attempts from this account or address. Wait and try again.",
            "500", "Something went wrong on the server.");

    /** Beyond 400/401/500: the status codes an operation can answer, keyed by {@code METHOD path}. */
    static final Map<String, String[]> EXTRA_ERRORS = Map.of(
            "POST /api/v1/players", new String[] {"403", "409", "429"},
            "POST /api/v1/sessions", new String[] {"429"},
            "DELETE /api/v1/players/me/devices/{sessionId}", new String[] {"404"},
            "POST /api/v1/tables", new String[] {"409"},
            "GET /api/v1/tables/{tableId}", new String[] {"404"},
            "POST /api/v1/tables/{tableId}/seats", new String[] {"404", "409"},
            "POST /api/v1/tables/{tableId}/seats/me/buy-ins", new String[] {"404", "409"},
            "POST /api/v1/tables/{tableId}/seats/me/cash-out", new String[] {"404", "409"},
            "POST /api/v1/tables/{tableId}/close", new String[] {"403", "404", "409"});

    @Bean
    OpenAPI spadeApi() {
        return new OpenAPI()
                .info(new Info().title("Spade API").version("v1"))
                .servers(List.of(new Server().url("/")))
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP).scheme("bearer")
                                .description("The token from POST /api/v1/sessions, sent as `Authorization: Bearer <token>`.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    @Bean
    GlobalOpenApiCustomizer errorResponses() {
        return openApi -> {
            // springdoc rebuilds the components from the controllers' types, so this one is added here, not on the bean
            openApi.getComponents().addSchemas(PROBLEM_SCHEMA, problemDetail());
            requireAllPropertiesOfResponses(openApi);
            if (openApi.getPaths() == null) {
                return;
            }
            requireEveryExtraErrorToMatch(openApi, EXTRA_ERRORS);
            openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach(
                    (method, operation) -> addErrors(method, path, operation)));
        };
    }

    /**
     * A key of {@link #EXTRA_ERRORS} that names no operation means a path or method was renamed and its error
     * codes would silently vanish from the contract. Fail instead; ContractTest builds the real spec, so it fails there.
     */
    static void requireEveryExtraErrorToMatch(OpenAPI openApi, Map<String, String[]> extraErrors) {
        Set<String> operations = new TreeSet<>();
        if (openApi.getPaths() != null) {
            openApi.getPaths().forEach((path, item) ->
                    item.readOperationsMap().keySet().forEach(method -> operations.add(method + " " + path)));
        }
        TreeSet<String> unmatched = new TreeSet<>(extraErrors.keySet());
        unmatched.removeAll(operations);
        if (!unmatched.isEmpty()) {
            throw new IllegalStateException("OpenApiConfig.EXTRA_ERRORS names operations that don't exist: " + unmatched
                    + ". Update the keys to the renamed path or method.");
        }
    }

    /** The server always sends every property of a response, so a generated client can treat none as optional. */
    private static void requireAllPropertiesOfResponses(OpenAPI openApi) {
        openApi.getComponents().getSchemas().forEach((name, schema) -> {
            if (name.endsWith("Response") && schema.getProperties() != null) {
                schema.setRequired(new ArrayList<>(new TreeSet<>(schema.getProperties().keySet())));
            }
        });
    }

    private static void addErrors(PathItem.HttpMethod method, String path, Operation operation) {
        // an empty list is "no security" (@SecurityRequirements with no value); null means "the global requirement"
        boolean needsSession = operation.getSecurity() == null || !operation.getSecurity().isEmpty();
        boolean hasInput = operation.getRequestBody() != null
                || (operation.getParameters() != null && !operation.getParameters().isEmpty());
        TreeSet<String> codes = new TreeSet<>();
        if (hasInput) {
            codes.add("400");
        }
        if (needsSession || path.equals("/api/v1/sessions")) {
            codes.add("401");
        }
        for (String code : EXTRA_ERRORS.getOrDefault(method + " " + path, new String[0])) {
            codes.add(code);
        }
        codes.add("500");
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        codes.forEach(code -> operation.getResponses().addApiResponse(code, problem(code)));
    }

    private static ApiResponse problem(String code) {
        return new ApiResponse().description(DESCRIPTIONS.get(code)).content(new Content().addMediaType(
                PROBLEM_JSON, new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM_SCHEMA))));
    }

    /** RFC 9457 as Spring's {@code ProblemDetail} writes it. {@code type} is {@code about:blank} unless stated. */
    private static Schema<?> problemDetail() {
        return new ObjectSchema()
                .description("An error, as RFC 9457 problem details. Show `detail` to the person.")
                .addProperty("type", new StringSchema().format("uri").description("Always about:blank for now."))
                .addProperty("title", new StringSchema().description("The status's reason phrase."))
                .addProperty("status", new IntegerSchema().format("int32").description("The HTTP status."))
                .addProperty("detail", new StringSchema().description("What went wrong and how to fix it, in English."))
                .addProperty("instance", new StringSchema().format("uri").description("The request path."));
    }
}
