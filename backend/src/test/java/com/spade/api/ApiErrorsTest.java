package com.spade.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.spade.SpadeIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
        mvc.perform(post("/probe/named").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))   // csrf(): Boot's default chain is still active in this task
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void anUnknownPathIsA404Problem() throws Exception {
        mvc.perform(get("/api/v1/nowhere"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
