package com.ssafy.e102.global.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class SwaggerUiDomainSheetTransformer {

	private static final String SWAGGER_INITIALIZER = "swagger-initializer.js";
	private static final String CUSTOM_ASSET_LOADER = """

		;(() => {
		  const swaggerUiPath = window.location.pathname;
		  const contextPath = swaggerUiPath.replace(/\\/swagger-ui\\/.*/, "");
		  const assetBase = `${contextPath}/swagger-ui-custom`;
		  const appendAsset = (tagName, attributes) => {
		    const element = document.createElement(tagName);
		    Object.entries(attributes).forEach(([key, value]) => element.setAttribute(key, value));
		    document.head.appendChild(element);
		  };
		  appendAsset("link", { rel: "stylesheet", href: `${assetBase}/domain-sheet.css` });
		  appendAsset("script", { src: `${assetBase}/domain-sheet.js`, defer: "defer" });
		})();
		""";

	@Bean
	public SwaggerIndexTransformer swaggerIndexTransformer(
		SwaggerUiConfigProperties swaggerUiConfig,
		SwaggerUiOAuthProperties swaggerUiOAuthProperties,
		SwaggerWelcomeCommon swaggerWelcomeCommon,
		ObjectMapperProvider objectMapperProvider) {
		return new DomainSheetIndexTransformer(
			swaggerUiConfig,
			swaggerUiOAuthProperties,
			swaggerWelcomeCommon,
			objectMapperProvider);
	}

	private static class DomainSheetIndexTransformer extends SwaggerIndexPageTransformer {

		DomainSheetIndexTransformer(
			SwaggerUiConfigProperties swaggerUiConfig,
			SwaggerUiOAuthProperties swaggerUiOAuthProperties,
			SwaggerWelcomeCommon swaggerWelcomeCommon,
			ObjectMapperProvider objectMapperProvider) {
			super(swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider);
		}

		@Override
		public Resource transform(
			HttpServletRequest request,
			Resource resource,
			ResourceTransformerChain transformerChain) throws IOException {
			Resource transformedResource = super.transform(request, resource, transformerChain);
			if (!SWAGGER_INITIALIZER.equals(resource.getFilename())) {
				return transformedResource;
			}

			String script = new String(transformedResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			return new TransformedResource(
				transformedResource,
				(script + CUSTOM_ASSET_LOADER).getBytes(StandardCharsets.UTF_8));
		}
	}
}
