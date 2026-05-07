package com.ssafy.e102.global.external.bims;

import java.io.StringReader;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;

@Component
public class BusanBimsClient {

	private final RestTemplate restTemplate;
	private final BusanBimsProperties properties;

	public BusanBimsClient(RestTemplateBuilder builder, BusanBimsProperties properties) {
		this.restTemplate = builder
			.connectTimeout(properties.connectTimeout())
			.readTimeout(properties.readTimeout())
			.build();
		this.properties = properties;
	}

	public BusanBimsArrival findArrival(String stopId, String lineId, String routeNo) {
		if (!StringUtils.hasText(stopId)) {
			return new BusanBimsArrival(null, lineId, routeNo, null, null);
		}
		String resolvedLineId = StringUtils.hasText(lineId) ? lineId : findLineId(stopId, routeNo);
		if (!StringUtils.hasText(resolvedLineId)) {
			return new BusanBimsArrival(stopId, null, routeNo, null, null);
		}
		List<Element> items = requestItems("busStopArrByBstopidLineid", stopId, resolvedLineId);
		Element item = items.isEmpty() ? null : items.get(0);
		return new BusanBimsArrival(
			stopId,
			resolvedLineId,
			text(item, "lineno", routeNo),
			remainingMinute(item),
			lowFloor(item));
	}

	private String findLineId(String stopId, String routeNo) {
		if (!StringUtils.hasText(routeNo)) {
			return null;
		}
		return requestItems("stopArrByBstopid", stopId, null)
			.stream()
			.filter(item -> routeNo.equals(text(item, "lineno", null)))
			.findFirst()
			.map(item -> text(item, "lineid", null))
			.orElse(null);
	}

	private List<Element> requestItems(String endpoint, String stopId, String lineId) {
		if (!StringUtils.hasText(properties.serviceKey())) {
			throw new RouteException(RouteErrorCode.EXTERNAL_ROUTE_API_FAILED, "BIMS service key가 설정되지 않았습니다.");
		}
		try {
			UriComponentsBuilder uriBuilder = UriComponentsBuilder
				.fromUriString(properties.baseUrl())
				.path("/" + endpoint)
				.queryParam("serviceKey", properties.serviceKey())
				.queryParam("bstopid", stopId)
				.queryParam("pageNo", 1)
				.queryParam("numOfRows", 20);
			if (StringUtils.hasText(lineId)) {
				uriBuilder.queryParam("lineid", lineId);
			}
			String body = restTemplate.exchange(
				RequestEntity
					.method(HttpMethod.GET, uriBuilder.build().toUri())
					.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE)
					.build(),
				String.class)
				.getBody();
			return parseItems(body);
		} catch (HttpStatusCodeException exception) {
			throw externalFailure(exception);
		} catch (ResourceAccessException exception) {
			throw new RouteException(timeoutOrFailure(exception), timeoutOrFailure(exception).getMessage(), exception);
		} catch (RestClientException exception) {
			throw new RouteException(RouteErrorCode.EXTERNAL_ROUTE_API_FAILED,
				RouteErrorCode.EXTERNAL_ROUTE_API_FAILED.getMessage(), exception);
		}
	}

	private List<Element> parseItems(String body) {
		if (!StringUtils.hasText(body)) {
			return List.of();
		}
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(body)));
			NodeList nodes = document.getElementsByTagName("item");
			List<Element> items = new ArrayList<>();
			for (int index = 0; index < nodes.getLength(); index++) {
				items.add((Element)nodes.item(index));
			}
			return List.copyOf(items);
		} catch (Exception exception) {
			throw new RouteException(RouteErrorCode.EXTERNAL_ROUTE_API_FAILED,
				RouteErrorCode.EXTERNAL_ROUTE_API_FAILED.getMessage(), exception);
		}
	}

	private Integer remainingMinute(Element item) {
		return Arrays.asList(integer(item, "min1"), integer(item, "min2"))
			.stream()
			.filter(value -> value != null && value >= 0)
			.min(Comparator.naturalOrder())
			.orElse(null);
	}

	private Boolean lowFloor(Element item) {
		List<Boolean> values = Arrays.asList(booleanValue(item, "lowplate1"), booleanValue(item, "lowplate2"))
			.stream()
			.filter(value -> value != null)
			.toList();
		if (values.isEmpty()) {
			return null;
		}
		return values.contains(Boolean.TRUE);
	}

	private Integer integer(Element item, String tagName) {
		String value = text(item, tagName, null);
		if (!StringUtils.hasText(value)) {
			return null;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	private Boolean booleanValue(Element item, String tagName) {
		String value = text(item, tagName, null);
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return switch (value.trim().toUpperCase()) {
			case "1", "Y", "YES", "TRUE" -> Boolean.TRUE;
			case "0", "N", "NO", "FALSE" -> Boolean.FALSE;
			default -> null;
		};
	}

	private String text(Element item, String tagName, String defaultValue) {
		if (item == null) {
			return defaultValue;
		}
		NodeList nodes = item.getElementsByTagName(tagName);
		if (nodes.getLength() == 0) {
			return defaultValue;
		}
		String value = nodes.item(0).getTextContent();
		return StringUtils.hasText(value) ? value.trim() : defaultValue;
	}

	private RouteException externalFailure(HttpStatusCodeException exception) {
		return new RouteException(RouteErrorCode.EXTERNAL_ROUTE_API_FAILED,
			RouteErrorCode.EXTERNAL_ROUTE_API_FAILED.getMessage(), exception);
	}

	private RouteErrorCode timeoutOrFailure(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof SocketTimeoutException) {
				return RouteErrorCode.EXTERNAL_ROUTE_API_TIMEOUT;
			}
			current = current.getCause();
		}
		return RouteErrorCode.EXTERNAL_ROUTE_API_FAILED;
	}
}
