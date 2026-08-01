package com.intelliroute.api.domain;

/**
 * A location in the road network.
 *
 * @param id   short unique identifier used in routing requests (e.g. "A")
 * @param name human-readable location name (e.g. "Ashford")
 * @param x    abstract x-coordinate used for visualization (not geographic)
 * @param y    abstract y-coordinate used for visualization (not geographic)
 */
public record Node(String id, String name, double x, double y) {

    public Node {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Node id must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Node name must not be blank");
        }
    }
}
