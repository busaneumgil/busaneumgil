package com.ssafy.e102.graphhopper.ieum;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;

public class IeumDecimalTagParser implements TagParser {
    private final DecimalEncodedValue encodedValue;
    private final String tagName;
    private final double fallback;

    public IeumDecimalTagParser(DecimalEncodedValue encodedValue, String tagName, double fallback) {
        this.encodedValue = encodedValue;
        this.tagName = tagName;
        this.fallback = fallback;
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relationFlags) {
        encodedValue.setDecimal(false, edgeId, edgeIntAccess, parseValue(way.getTag(tagName)));
    }

    private double parseValue(String value) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
