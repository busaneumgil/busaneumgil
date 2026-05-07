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
        RISK,
        UNKNOWN
    }

    public enum WidthState {
        ADEQUATE_150,
        ADEQUATE_120,
        NARROW,
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
