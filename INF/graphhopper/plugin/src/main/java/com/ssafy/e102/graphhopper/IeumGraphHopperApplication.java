package com.ssafy.e102.graphhopper;

import com.graphhopper.application.GraphHopperServerConfiguration;
import com.graphhopper.application.resources.RootResource;
import com.graphhopper.http.CORSFilter;
import com.graphhopper.navigation.NavigateResource;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import jakarta.servlet.DispatcherType;
import java.util.EnumSet;

public final class IeumGraphHopperApplication extends Application<GraphHopperServerConfiguration> {

    public static void main(String[] args) throws Exception {
        new IeumGraphHopperApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<GraphHopperServerConfiguration> bootstrap) {
        bootstrap.addBundle(new IeumGraphHopperBundle());
        bootstrap.addBundle(new AssetsBundle("/com/graphhopper/maps/", "/maps/", "index.html"));
        bootstrap.addBundle(new AssetsBundle("/META-INF/resources/webjars", "/webjars/", null, "webjars"));
    }

    @Override
    public void run(GraphHopperServerConfiguration configuration, Environment environment) {
        environment.jersey().register(new RootResource());
        environment.jersey().register(NavigateResource.class);
        environment.servlets()
            .addFilter("cors", CORSFilter.class)
            .addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "*");
    }
}
