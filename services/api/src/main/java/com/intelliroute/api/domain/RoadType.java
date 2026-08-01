package com.intelliroute.api.domain;

/**
 * Classification of a road segment.
 *
 * <p>Each type carries a typical average speed used to derive deterministic
 * travel-time estimates for the sample network (documented in
 * {@code SampleNetworkConfig}; real networks would carry measured times).
 */
public enum RoadType {

    /** Grade-separated, high-speed road. */
    HIGHWAY(100),

    /** Major through road connecting towns. */
    ARTERIAL(65),

    /** Ordinary local road. */
    LOCAL(40),

    /** Designated scenic road — slower, high scenery value. */
    SCENIC(50);

    private final int typicalSpeedKmh;

    RoadType(int typicalSpeedKmh) {
        this.typicalSpeedKmh = typicalSpeedKmh;
    }

    public int typicalSpeedKmh() {
        return typicalSpeedKmh;
    }

    public boolean isHighway() {
        return this == HIGHWAY;
    }
}
