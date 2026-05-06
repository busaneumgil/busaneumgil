package com.ssafy.e102.graphhopper;

import com.graphhopper.GraphHopper;
import com.graphhopper.GraphHopperConfig;
import com.ssafy.e102.graphhopper.ieum.IeumImportRegistry;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IeumGraphHopperManaged implements Managed {
    private static final Logger log = LoggerFactory.getLogger(IeumGraphHopperManaged.class);

    private final GraphHopper graphHopper;

    public IeumGraphHopperManaged(GraphHopperConfig configuration) {
        this.graphHopper = new GraphHopper()
            .setImportRegistry(new IeumImportRegistry())
            .init(configuration);
    }

    @Override
    public void start() {
        graphHopper.importOrLoad();
        log.info(
            "loaded ieum graph at:{}, data_reader_file:{}, encoded values:{}, {} bytes for edge flags, {}",
            graphHopper.getGraphHopperLocation(),
            graphHopper.getOSMFile(),
            graphHopper.getEncodingManager().toEncodedValuesAsString(),
            graphHopper.getEncodingManager().getBytesForFlags(),
            graphHopper.getBaseGraph().toDetailsString()
        );
    }

    public GraphHopper getGraphHopper() {
        return graphHopper;
    }

    @Override
    public void stop() {
        graphHopper.close();
    }
}
