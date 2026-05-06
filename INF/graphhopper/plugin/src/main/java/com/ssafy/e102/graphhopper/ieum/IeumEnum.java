package com.ssafy.e102.graphhopper.ieum;

public final class IeumEnum {
    private IeumEnum() {
    }

    public enum YesNoUnknown {
        YES,
        NO,
        UNKNOWN
    }

    public enum SlopeState {
        FLAT,
        MODERATE,
        STEEP,
        UNKNOWN
    }

    public enum WidthState {
        ADEQUATE_150,
        NARROW_120,
        NARROW_90,
        UNKNOWN
    }

    public enum SurfaceState {
        PAVED,
        UNPAVED,
        UNKNOWN
    }

    public enum SegmentType {
        CROSS_WALK,
        SIDE_LINE
    }
}
