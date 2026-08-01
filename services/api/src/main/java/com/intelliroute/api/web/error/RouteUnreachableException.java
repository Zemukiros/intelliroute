package com.intelliroute.api.web.error;

/** Thrown when both nodes exist but no path connects them. */
public class RouteUnreachableException extends RuntimeException {

    private final String origin;
    private final String destination;
    private final int visitedNodes;

    public RouteUnreachableException(String origin, String destination, int visitedNodes) {
        super("No route exists from '" + origin + "' to '" + destination + "'");
        this.origin = origin;
        this.destination = destination;
        this.visitedNodes = visitedNodes;
    }

    public String origin() {
        return origin;
    }

    public String destination() {
        return destination;
    }

    public int visitedNodes() {
        return visitedNodes;
    }
}
