package com.ssafy.e102.global.external.graphhopper;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GraphHopperProperties.class)
public class GraphHopperClientConfig {}
