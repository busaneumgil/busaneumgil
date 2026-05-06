package com.ssafy.e102.graphhopper;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.graphhopper.GraphHopper;
import com.graphhopper.GraphHopperConfig;
import com.graphhopper.http.GHRequestTransformer;
import com.graphhopper.http.GraphHopperBundleConfiguration;
import com.graphhopper.http.GHJerseyViolationExceptionMapper;
import com.graphhopper.http.IllegalArgumentExceptionMapper;
import com.graphhopper.http.MultiExceptionGPXMessageBodyWriter;
import com.graphhopper.http.MultiExceptionMapper;
import com.graphhopper.http.ProfileResolver;
import com.graphhopper.http.TypeGPXFilter;
import com.graphhopper.http.health.GraphHopperHealthCheck;
import com.graphhopper.jackson.Jackson;
import com.graphhopper.resources.HealthCheckResource;
import com.graphhopper.resources.I18NResource;
import com.graphhopper.resources.InfoResource;
import com.graphhopper.resources.NearestResource;
import com.graphhopper.resources.RouteResource;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.util.TranslationMap;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

public class IeumGraphHopperBundle implements ConfiguredBundle<GraphHopperBundleConfiguration> {

    public static class TranslationMapFactory implements Factory<TranslationMap> {
        @Inject GraphHopper graphHopper;
        @Override public TranslationMap provide() { return graphHopper.getTranslationMap(); }
        @Override public void dispose(TranslationMap instance) { }
    }

    public static class EncodingManagerFactory implements Factory<EncodingManager> {
        @Inject GraphHopper graphHopper;
        @Override public EncodingManager provide() { return graphHopper.getEncodingManager(); }
        @Override public void dispose(EncodingManager instance) { }
    }

    public static class LocationIndexFactory implements Factory<LocationIndex> {
        @Inject GraphHopper graphHopper;
        @Override public LocationIndex provide() { return graphHopper.getLocationIndex(); }
        @Override public void dispose(LocationIndex instance) { }
    }

    public static class BaseGraphFactory implements Factory<BaseGraph> {
        @Inject GraphHopper graphHopper;
        @Override public BaseGraph provide() { return graphHopper.getBaseGraph(); }
        @Override public void dispose(BaseGraph instance) { }
    }

    public static class ProfileResolverFactory implements Factory<ProfileResolver> {
        @Inject GraphHopper graphHopper;
        @Override public ProfileResolver provide() { return new ProfileResolver(graphHopper.getProfiles()); }
        @Override public void dispose(ProfileResolver instance) { }
    }

    public static class GHRequestTransformerFactory implements Factory<GHRequestTransformer> {
        @Override public GHRequestTransformer provide() { return req -> req; }
        @Override public void dispose(GHRequestTransformer instance) { }
    }

    public static class HasElevationFactory implements Factory<Boolean> {
        @Inject GraphHopper graphHopper;
        @Override public Boolean provide() { return graphHopper.hasElevation(); }
        @Override public void dispose(Boolean instance) { }
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        bootstrap.setObjectMapper(io.dropwizard.jackson.Jackson.newMinimalObjectMapper());
        bootstrap.getObjectMapper().registerModule(new Jdk8Module());
        Jackson.initObjectMapper(bootstrap.getObjectMapper());
        bootstrap.getObjectMapper().setDateFormat(new StdDateFormat());
        bootstrap.getObjectMapper().setConfig(
            bootstrap.getObjectMapper().getDeserializationConfig().with(MapperFeature.ALLOW_EXPLICIT_PROPERTY_RENAMING)
        );
    }

    @Override
    public void run(GraphHopperBundleConfiguration configuration, Environment environment) {
        for (Object key : System.getProperties().keySet()) {
            if (key instanceof String && ((String) key).startsWith("graphhopper.")) {
                throw new IllegalArgumentException("Use '-Ddw.graphhopper.' instead of '-Dgraphhopper.'");
            }
        }

        environment.jersey().register(new GHJerseyViolationExceptionMapper());
        environment.jersey().register(new TypeGPXFilter());
        environment.jersey().register(new MultiExceptionMapper());
        environment.jersey().register(new MultiExceptionGPXMessageBodyWriter());
        environment.jersey().register(new IllegalArgumentExceptionMapper());

        IeumGraphHopperManaged managed = new IeumGraphHopperManaged(configuration.getGraphHopperConfiguration());
        environment.lifecycle().manage(managed);
        GraphHopper graphHopper = managed.getGraphHopper();

        environment.jersey().register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(configuration.getGraphHopperConfiguration()).to(GraphHopperConfig.class);
                bind(graphHopper).to(GraphHopper.class);
                bindFactory(ProfileResolverFactory.class).to(ProfileResolver.class);
                bindFactory(GHRequestTransformerFactory.class).to(GHRequestTransformer.class);
                bindFactory(HasElevationFactory.class).to(Boolean.class).named("hasElevation");
                bindFactory(LocationIndexFactory.class).to(LocationIndex.class);
                bindFactory(TranslationMapFactory.class).to(TranslationMap.class);
                bindFactory(EncodingManagerFactory.class).to(EncodingManager.class);
                bindFactory(BaseGraphFactory.class).to(BaseGraph.class);
            }
        });

        environment.jersey().register(RouteResource.class);
        environment.jersey().register(NearestResource.class);
        environment.jersey().register(I18NResource.class);
        environment.jersey().register(InfoResource.class);
        environment.healthChecks().register("graphhopper", new GraphHopperHealthCheck(graphHopper));
        environment.jersey().register(environment.healthChecks());
        environment.jersey().register(HealthCheckResource.class);
    }
}
