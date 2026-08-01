package com.intelliroute.api.ranking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.intelliroute.api.engine.RouteCandidate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Mocked integration tests for the ranking-service HTTP client — no real
 * network involved.
 */
class RankingClientTest {

    private static final String BASE = "http://ranking.test";

    private MockRestServiceServer server;
    private RankingClient client;

    private final List<RouteCandidate> candidates = List.of(
            new RouteCandidate("route-1", List.of("A", "C", "D", "F"),
                    14.0, 16.5, 3.00, 6.71, 4.21, 35.7));

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RankingClient(
                new RankingProperties(BASE, 500, 500), builder);
    }

    @Test
    @DisplayName("successful ranking is parsed with camelCase contract")
    void successfulRanking() {
        server.expect(requestTo(BASE + "/rank"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.preference").value("avoid tolls"))
                .andExpect(jsonPath("$.candidates[0].routeId").value("route-1"))
                .andRespond(withSuccess("""
                        {
                          "provider": "local-deterministic-v1",
                          "parsedPreferences": {
                            "weights": {"toll": 1.0},
                            "recognizedTerms": ["avoid tolls"],
                            "unrecognizedTerms": [],
                            "confidence": 1.0,
                            "note": "Interpreted preference across: toll"
                          },
                          "results": [
                            {"routeId": "route-1", "rank": 1, "score": 1.0,
                             "explanation": "Best match"}
                          ],
                          "recommendedRouteId": "route-1"
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.rank("avoid tolls", candidates);

        assertThat(response.provider()).isEqualTo("local-deterministic-v1");
        assertThat(response.recommendedRouteId()).isEqualTo("route-1");
        assertThat(response.parsedPreferences().weights()).containsEntry("toll", 1.0);
        assertThat(response.results()).hasSize(1);
        server.verify();
    }

    @Test
    @DisplayName("server errors are translated to RankingServiceException")
    void serverErrorTranslated() {
        server.expect(requestTo(BASE + "/rank")).andRespond(withServerError());

        assertThatThrownBy(() -> client.rank("fastest", candidates))
                .isInstanceOf(RankingServiceException.class);
    }

    @Test
    @DisplayName("empty result payloads are rejected")
    void emptyResultsRejected() {
        server.expect(requestTo(BASE + "/rank"))
                .andRespond(withSuccess("""
                        {"provider": "x", "parsedPreferences": null,
                         "results": [], "recommendedRouteId": null}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.rank("fastest", candidates))
                .isInstanceOf(RankingServiceException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("malformed JSON is translated to RankingServiceException")
    void malformedJsonTranslated() {
        server.expect(requestTo(BASE + "/rank"))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.rank("fastest", candidates))
                .isInstanceOf(RankingServiceException.class);
    }
}
