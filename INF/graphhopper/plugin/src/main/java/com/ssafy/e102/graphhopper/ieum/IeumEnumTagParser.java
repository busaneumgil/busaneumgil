package com.ssafy.e102.graphhopper.ieum;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;

public class IeumEnumTagParser<E extends Enum<E>> implements TagParser {
    private final EnumEncodedValue<E> encodedValue;
    private final E fallback;

    public IeumEnumTagParser(EnumEncodedValue<E> encodedValue, E fallback) {
        this.encodedValue = encodedValue;
        this.fallback = fallback;
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relationFlags) {
        encodedValue.setEnum(false, edgeId, edgeIntAccess, fallback);
    }
}
