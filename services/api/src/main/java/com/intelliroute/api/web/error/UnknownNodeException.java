package com.intelliroute.api.web.error;

/** Thrown when a request references a node id that is not in the network. */
public class UnknownNodeException extends RuntimeException {

    private final String nodeId;
    private final String role;

    public UnknownNodeException(String nodeId, String role) {
        super("Unknown " + role + " node: '" + nodeId + "'");
        this.nodeId = nodeId;
        this.role = role;
    }

    public String nodeId() {
        return nodeId;
    }

    public String role() {
        return role;
    }
}
