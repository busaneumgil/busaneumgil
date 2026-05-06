package com.ssafy.e102.graphhopper.ieum;

import com.graphhopper.routing.ev.DefaultImportRegistry;
import com.graphhopper.routing.ev.ImportUnit;

public class IeumImportRegistry extends DefaultImportRegistry {

    @Override
    public ImportUnit createImportUnit(String name) {
        ImportUnit ieumUnit = IeumEncodedValues.createImportUnit(name);
        if (ieumUnit != null) {
            return ieumUnit;
        }
        return super.createImportUnit(name);
    }
}
