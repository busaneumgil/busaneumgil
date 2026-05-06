package com.ssafy.e102.graphhopper.ieum;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;

public class IeumEnumTagParser<E extends Enum<E>> implements TagParser {
    private final EnumEncodedValue<E> encodedValue;
    private final String tagName;
    private final E fallback;

    public IeumEnumTagParser(EnumEncodedValue<E> encodedValue, String tagName, E fallback) {
        this.encodedValue = encodedValue;
        this.tagName = tagName;
        this.fallback = fallback;
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relationFlags) {
        encodedValue.setEnum(false, edgeId, edgeIntAccess, parseValue(way.getTag(tagName)));
    }

    private E parseValue(String value) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(fallback.getDeclaringClass(), value.trim());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
