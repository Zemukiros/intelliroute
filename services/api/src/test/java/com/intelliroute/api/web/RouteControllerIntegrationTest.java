package com.intelliroute.api.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.intelliroute.api.ranking.RankingClient;
import com.intelliroute.api.ranking.RankingDtos.ParsedPreferences;
import com.intelliroute.api.ranking.RankingDtos.RankResponse;
import com.intelliroute.api.ranking.RankingDtos.RankedRoute;
import com.intelliroute.api.ranking.RankingServiceException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full-stack API tests: HTTP layer, validation, service, and engine
 * against the real sample network. The external ranking service is mocked
 * at the client boundary.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RouteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RankingClient rankingClient;

    // ---------------------------------------------------------- calculate

    @Test
    @DisplayName("POST /calculate returns the shortest route with measured timing")
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
                .andExpect(jsonPath("$.error").value("ROUTE_UNREACHABLE"));
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

    // -------------------------------------------------------- alternatives

    @Test
    @DisplayName("POST /alternatives returns three distinct routes with metadata")
    void alternativesForAtoF() throws Exception {
        mockMvc.perform(post("/api/routes/alternatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origin\": \"A\", \"destination\": \"F\", \"maximumRoutes\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedRoutes").value(3))
                .andExpect(jsonPath("$.routes", hasSize(3)))
                .andExpect(jsonPath("$.routes[0].routeId").value("route-1"))
                .andExpect(jsonPath("$.routes[0].path", contains("A", "C", "D", "F")))
                .andExpect(jsonPath("$.routes[0].totalDistanceKm").value(14.0))
                .andExpect(jsonPath("$.routes[1].path", contains("A", "B", "D", "F")))
                .andExpect(jsonPath("$.routes[1].tollCost").value(5.50))
                .andExpect(jsonPath("$.routes[1].highwayPercentage").value(70.6))
                .andExpect(jsonPath("$.routes[2].path", contains("A", "C", "E", "F")))
                .andExpect(jsonPath("$.routes[2].tollCost").value(0.0))
                .andExpect(jsonPath("$.dijkstraInvocations", greaterThan(1)))
                .andExpect(jsonPath("$.executionTimeMs", greaterThanOrEqualTo(0.0)));
    }

    @Test
    @DisplayName("alternatives default to three routes when maximumRoutes omitted")
    void alternativesDefaultCount() throws Exception {
        mockMvc.perform(post("/api/routes/alternatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origin\": \"A\", \"destination\": \"F\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedRoutes").value(3));
    }

    @Test
    @DisplayName("alternatives validate the maximumRoutes bounds")
    void alternativesValidateBounds() throws Exception {
        mockMvc.perform(post("/api/routes/alternatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origin\": \"A\", \"destination\": \"F\", \"maximumRoutes\": 99}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("alternatives for unreachable destinations return 422")
    void alternativesUnreachable() throws Exception {
        mockMvc.perform(post("/api/routes/alternatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origin\": \"A\", \"destination\": \"J\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("ROUTE_UNREACHABLE"));
    }

    // ---------------------------------------------------------- recommend

    @Test
    @DisplayName("POST /recommend returns ranked routes from the ranking service")
    void recommendSuccess() throws Exception {
        when(rankingClient.rank(anyString(), anyList())).thenReturn(new RankResponse(
                "local-deterministic-v1",
                new ParsedPreferences(Map.of("toll", 1.0), List.of("avoid tolls"),
                        List.of(), 1.0, "Interpreted preference across: toll"),
                List.of(
                        new RankedRoute("route-3", 1, 1.0, "Best match: lowest toll"),
                        new RankedRoute("route-1", 2, 0.45, "Ranked #2"),
                        new RankedRoute("route-2", 3, 0.0, "Ranked #3")),
                "route-3"));

        mockMvc.perform(post("/api/routes/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"origin": "A", "destination": "F",
                                 "preference": "avoid tolls", "maximumRoutes": 3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("local-deterministic-v1"))
                .andExpect(jsonPath("$.fallbackUsed").value(false))
                .andExpect(jsonPath("$.recommendedRouteId").value("route-3"))
                .andExpect(jsonPath("$.candidates", hasSize(3)))
                .andExpect(jsonPath("$.rankedRoutes", hasSize(3)))
                .andExpect(jsonPath("$.parsedPreferences.weights.toll").value(1.0))
                .andExpect(jsonPath("$.routingTimeMs", greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.rankingTimeMs", greaterThanOrEqualTo(0.0)));
    }

    @Test
    @DisplayName("recommend degrades to the local fallback when ranking fails")
    void recommendFallback() throws Exception {
        when(rankingClient.rank(anyString(), anyList()))
                .thenThrow(new RankingServiceException("down"));

        mockMvc.perform(post("/api/routes/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"origin": "A", "destination": "F",
                                 "preference": "avoid tolls"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fallbackUsed").value(true))
                .andExpect(jsonPath("$.provider").value("java-local-fallback"))
                .andExpect(jsonPath("$.recommendedRouteId").value("route-1"))
                .andExpect(jsonPath("$.rankedRoutes", hasSize(3)));
    }

    @Test
    @DisplayName("recommend requires a non-blank preference")
    void recommendValidatesPreference() throws Exception {
        mockMvc.perform(post("/api/routes/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origin\": \"A\", \"destination\": \"F\", \"preference\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.preference").value("preference is required"));
    }

    @Test
    @DisplayName("recommend rejects unknown nodes with 404")
    void recommendUnknownNode() throws Exception {
        mockMvc.perform(post("/api/routes/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"origin": "A", "destination": "Z", "preference": "fastest"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("UNKNOWN_NODE"));
    }

    // ------------------------------------------------------------ network

    @Test
    @DisplayName("GET /network returns nodes and de-duplicated edges with metadata")
    void networkEndpoint() throws Exception {
        mockMvc.perform(get("/api/routes/network"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes", hasSize(10)))
                .andExpect(jsonPath("$.edges", hasSize(16)))
                .andExpect(jsonPath("$.nodes[0].id").value("A"))
                .andExpect(jsonPath("$.edges[0].roadType").isNotEmpty())
                .andExpect(jsonPath("$.edges[?(@.open == false)]", hasSize(1)));
    }
}
