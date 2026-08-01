package com.intelliroute.api.web;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full-stack API tests: HTTP layer, validation, service, and engine
 * against the real sample network.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RouteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/routes/calculate returns the shortest route with measured timing")
    void calculateSuccessfulRoute() throws Exception {
        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origin\": \"A\", \"destination\": \"F\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origin").value("A"))
                .andExpect(jsonPath("$.destination").value("F"))
                .andExpect(jsonPath("$.path", contains("A", "C", "D", "F")))
                .andExpect(jsonPath("$.totalDistance").value(14.0))
                .andExpect(jsonPath("$.visitedNodes", greaterThan(0)))
                .andExpect(jsonPath("$.executionTimeMs", greaterThanOrEqualTo(0.0)));
    }

    @Test
    @DisplayName("origin equal to destination returns a zero-distance route")
    void calculateSameOriginDestination() throws Exception {
        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origin\": \"D\", \"destination\": \"D\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path", contains("D")))
                .andExpect(jsonPath("$.totalDistance").value(0.0));
    }

    @Test
    @DisplayName("unknown origin node returns 404 UNKNOWN_NODE")
    void unknownOriginReturns404() throws Exception {
        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origin\": \"Z\", \"destination\": \"F\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("UNKNOWN_NODE"))
                .andExpect(jsonPath("$.details.nodeId").value("Z"))
                .andExpect(jsonPath("$.details.role").value("origin"));
    }

    @Test
    @DisplayName("unreachable destination returns 422 ROUTE_UNREACHABLE")
    void unreachableDestinationReturns422() throws Exception {
        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origin\": \"A\", \"destination\": \"J\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("ROUTE_UNREACHABLE"))
                .andExpect(jsonPath("$.details.origin").value("A"))
                .andExpect(jsonPath("$.details.destination").value("J"));
    }

    @Test
    @DisplayName("blank fields return 400 VALIDATION_ERROR with field details")
    void blankFieldsReturn400() throws Exception {
        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origin\": \"\", \"destination\": \"F\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.origin").value("origin is required"));
    }

    @Test
    @DisplayName("missing request body returns 400 MALFORMED_REQUEST")
    void missingBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("GET /api/routes/network returns nodes and de-duplicated edges")
    void networkEndpoint() throws Exception {
        mockMvc.perform(get("/api/routes/network"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes", hasSize(10)))
                .andExpect(jsonPath("$.edges", hasSize(15)))
                .andExpect(jsonPath("$.nodes[0].id").value("A"))
                .andExpect(jsonPath("$.nodes[0].name").value("Ashford"));
    }
}
