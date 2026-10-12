package com.spade.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.spade.SpadeIntegrationTest;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.autoconfigure.error.BasicErrorController;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import probe.ErrorProbeController;

/** Every error a client sees is application/problem+json, and internals never leak (#2). */
@SpadeIntegrationTest
@Import(ErrorProbeController.class)
@WithMockUser
class ApiErrorsTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationContext context;

    @Test
    void anUnexpectedErrorIsA500ProblemWithoutItsMessage() throws Exception {
        mvc.perform(get("/probe/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Something went wrong on our side."))
                .andExpect(content().string(not(containsString("hunter2"))))
                .andExpect(content().string(not(containsString("com.spade"))));
    }

    @Test
    void aDomainRuleIsAProblemWithItsMessage() throws Exception {
        mvc.perform(get("/probe/conflict"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("The table is full."));
    }

    @Test
    void aDatabaseClashIsA409WithoutTheSql() throws Exception {
        mvc.perform(get("/probe/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(content().string(not(containsString("seat_taken"))));
    }

    @Test
    void invalidInputIsA400Problem() throws Exception {
        mvc.perform(post("/probe/named").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(containsString("name")));
    }

    @Test
    void anUnknownPathIsA404Problem() throws Exception {
        mvc.perform(get("/api/v1/nowhere"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    /**
     * Errors that never reach a controller (a security filter's sendError, the firewall's 400, the container's
     * 413, an exception thrown in a filter) are forwarded to /error. MockMvc does not take that path by itself,
     * so the forward is simulated with the attributes the container sets.
     */
    @Test
    @WithAnonymousUser
    void bothWaysToAnswer401SayTheSameThing() throws Exception {
        mvc.perform(get("/api/v1/players/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Sign in first: this needs a valid session."));
        mvc.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 401))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Sign in first: this needs a valid session."));
    }

    @Test
    void aForwarded413SaysTheRequestWasTooLarge() throws Exception {
        mvc.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 413))
                .andExpect(status().isContentTooLarge())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("That request was too large."));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 413, 500})
    void anErrorForwardedToTheErrorPathIsAProblemWithoutInternals(int code) throws Exception {
        mvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, code)
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "secret-y")
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/secret-path")
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("secret-x")))
                .andExpect(status().is(code))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(code))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(content().string(not(containsString("secret-x"))))
                .andExpect(content().string(not(containsString("secret-y"))))
                .andExpect(content().string(not(containsString("secret-path"))));
    }

    @Test
    void ourErrorControllerReplacesBootsFallback() {
        assertThat(context.getBeansOfType(BasicErrorController.class)).isEmpty();
        assertThat(context.getBeansOfType(ErrorController.class).values())
                .hasSize(1).first().isInstanceOf(ProblemErrorController.class);
    }

    @Test
    void theErrorPathWithoutAStatusIsA500Problem() throws Exception {
        mvc.perform(get("/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Something went wrong on our side."));
    }

    @Test
    void aBrowserAskingForHtmlStillGetsAProblem() throws Exception {
        mvc.perform(get("/error").accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 403))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
