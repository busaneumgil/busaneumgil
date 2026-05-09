package com.ssafy.e102.global.external.bims;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BusanBimsProperties.class)
public class BusanBimsClientConfig {}
