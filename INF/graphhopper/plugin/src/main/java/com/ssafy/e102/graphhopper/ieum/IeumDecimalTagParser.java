package com.ssafy.e102.graphhopper.ieum;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;

public class IeumDecimalTagParser implements TagParser {
    private final DecimalEncodedValue encodedValue;
    private final double fallback;

    public IeumDecimalTagParser(DecimalEncodedValue encodedValue, double fallback) {
        this.encodedValue = encodedValue;
        this.fallback = fallback;
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relationFlags) {
        encodedValue.setDecimal(false, edgeId, edgeIntAccess, fallback);
    }
}
