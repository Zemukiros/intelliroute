package com.intelliroute.api.config;

import com.intelliroute.api.domain.Graph;
import com.intelliroute.api.domain.Node;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the local sample road network used during development.
 *
 * <p>The network is a fictional region of ten towns connected by two-way
 * roads with distances in kilometres. Node "J" (Juniper Isle) is
 * intentionally disconnected from the mainland so unreachable-destination
 * behaviour can be exercised end to end.
 *
 * <p>Coordinates are abstract layout positions for the frontend
 * visualization, not geographic coordinates. In a later phase this
 * configuration will be replaced by a persistent network store.
 */
@Configuration
public class SampleNetworkConfig {

    @Bean
    public Graph roadNetwork() {
        return Graph.builder()
                .addNode(new Node("A", "Ashford", 80, 210))
                .addNode(new Node("B", "Brookhaven", 240, 80))
                .addNode(new Node("C", "Cedarville", 260, 300))
                .addNode(new Node("D", "Dunmore", 450, 190))
                .addNode(new Node("E", "Eastvale", 470, 380))
                .addNode(new Node("F", "Fairmont", 650, 280))
                .addNode(new Node("G", "Glenrock", 460, 40))
                .addNode(new Node("H", "Harborview", 680, 90))
                .addNode(new Node("I", "Ironbridge", 660, 470))
                .addNode(new Node("J", "Juniper Isle", 90, 450))
                // Mainland roads (two-way, distances in km)
                .addBidirectionalEdge("A", "B", 5)
                .addBidirectionalEdge("A", "C", 4)
                .addBidirectionalEdge("B", "C", 6)
                .addBidirectionalEdge("B", "D", 7)
                .addBidirectionalEdge("B", "G", 6)
                .addBidirectionalEdge("C", "D", 5)
                .addBidirectionalEdge("C", "E", 8)
                .addBidirectionalEdge("D", "E", 4)
                .addBidirectionalEdge("D", "F", 5)
                .addBidirectionalEdge("D", "G", 5)
                .addBidirectionalEdge("E", "F", 6)
                .addBidirectionalEdge("E", "I", 7)
                .addBidirectionalEdge("F", "H", 4)
                .addBidirectionalEdge("F", "I", 5)
                .addBidirectionalEdge("G", "H", 6)
                // "J" (Juniper Isle) has no road connections on purpose.
                .build();
    }
}
