package com.intelliroute.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * IntelliRoute API — graph-based route intelligence engine.
 *
 * <p>Exposes REST endpoints for shortest-path route calculation over a
 * weighted road network, designed to be extended with AI-assisted route
 * re-ranking (see services/ai-service).
 */
@SpringBootApplication
public class IntelliRouteApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntelliRouteApiApplication.class, args);
    }
}
