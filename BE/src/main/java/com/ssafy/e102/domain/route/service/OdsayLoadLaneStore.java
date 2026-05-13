package com.ssafy.e102.domain.route.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ssafy.e102.domain.route.entity.OdsayLoadLane;
import com.ssafy.e102.domain.route.repository.OdsayLoadLaneRepository;
import com.ssafy.e102.domain.route.type.TransportMode;
import com.ssafy.e102.global.external.odsay.OdsayLaneGeometry;

@Service
public class OdsayLoadLaneStore {

	private static final Logger log = LoggerFactory.getLogger(OdsayLoadLaneStore.class);

	private final OdsayLoadLaneRepository odsayLoadLaneRepository;
	private final ObjectMapper objectMapper;

	public OdsayLoadLaneStore(OdsayLoadLaneRepository odsayLoadLaneRepository, ObjectMapper objectMapper) {
		this.odsayLoadLaneRepository = odsayLoadLaneRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public Map<String, List<OdsayLaneGeometry>> findValidByMapObjIn(Collection<String> mapObjs) {
		List<String> distinctMapObjs = normalizeMapObjs(mapObjs);
		if (distinctMapObjs.isEmpty()) {
			return Map.of();
		}
		Map<String, List<OdsayLaneGeometry>> laneGeometryByMapObj = new LinkedHashMap<>();
		for (OdsayLoadLane row : odsayLoadLaneRepository.findAllByMapObjIn(distinctMapObjs)) {
			parseLaneGeometries(row.getLaneGeometries())
				.ifPresentOrElse(
					laneGeometries -> laneGeometryByMapObj.put(row.getMapObj(), laneGeometries),
					() -> log.warn("odsay loadLane cache malformed mapObj={}", row.getMapObj()));
		}
		return laneGeometryByMapObj;
	}

	@Transactional
	public void saveIfAbsentOrRepairMalformed(String mapObj, List<OdsayLaneGeometry> laneGeometries) {
		if (!StringUtils.hasText(mapObj) || laneGeometries == null || laneGeometries.isEmpty()) {
			return;
		}
		JsonNode laneGeometriesJson = toJson(laneGeometries);
		odsayLoadLaneRepository.findByMapObj(mapObj)
			.ifPresentOrElse(
				row -> row.replaceLaneGeometries(laneGeometriesJson),
				() -> saveNew(mapObj, laneGeometriesJson));
	}

	private void saveNew(String mapObj, JsonNode laneGeometriesJson) {
		try {
			odsayLoadLaneRepository.save(OdsayLoadLane.create(mapObj, laneGeometriesJson));
		} catch (DataIntegrityViolationException exception) {
			log.debug("odsay loadLane cache insert raced mapObj={}", mapObj, exception);
		}
	}

	private List<String> normalizeMapObjs(Collection<String> mapObjs) {
		if (mapObjs == null || mapObjs.isEmpty()) {
			return List.of();
		}
		Set<String> distinctMapObjs = new LinkedHashSet<>();
		for (String mapObj : mapObjs) {
			if (StringUtils.hasText(mapObj)) {
				distinctMapObjs.add(mapObj);
			}
		}
		return List.copyOf(distinctMapObjs);
	}

	private JsonNode toJson(List<OdsayLaneGeometry> laneGeometries) {
		ArrayNode arrayNode = objectMapper.createArrayNode();
		for (int index = 0; index < laneGeometries.size(); index++) {
			OdsayLaneGeometry laneGeometry = laneGeometries.get(index);
			ObjectNode objectNode = objectMapper.createObjectNode();
			objectNode.put("order", index);
			objectNode.put("transportMode", laneGeometry.type().name());
			objectNode.put("geometry", laneGeometry.geometry());
			arrayNode.add(objectNode);
		}
		return arrayNode;
	}

	private Optional<List<OdsayLaneGeometry>> parseLaneGeometries(JsonNode laneGeometriesJson) {
		if (laneGeometriesJson == null || !laneGeometriesJson.isArray() || laneGeometriesJson.isEmpty()) {
			return Optional.empty();
		}
		List<OrderedLaneGeometry> orderedLaneGeometries = new ArrayList<>();
		Set<Integer> orders = new LinkedHashSet<>();
		for (int index = 0; index < laneGeometriesJson.size(); index++) {
			JsonNode laneGeometryJson = laneGeometriesJson.get(index);
			int order = laneGeometryJson.hasNonNull("order") ? laneGeometryJson.path("order").asInt() : index;
			if (!orders.add(order)) {
				return Optional.empty();
			}
			TransportMode transportMode = transportMode(laneGeometryJson.path("transportMode").asText(null));
			String geometry = laneGeometryJson.path("geometry").asText(null);
			if (!isLaneTransportMode(transportMode) || !isLineString(geometry)) {
				return Optional.empty();
			}
			orderedLaneGeometries.add(new OrderedLaneGeometry(order, new OdsayLaneGeometry(transportMode, geometry)));
		}
		return Optional.of(orderedLaneGeometries.stream()
			.sorted(Comparator.comparingInt(OrderedLaneGeometry::order))
			.map(OrderedLaneGeometry::laneGeometry)
			.toList());
	}

	private TransportMode transportMode(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		try {
			return TransportMode.valueOf(value);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private boolean isLaneTransportMode(TransportMode transportMode) {
		return transportMode == TransportMode.BUS || transportMode == TransportMode.SUBWAY;
	}

	private boolean isLineString(String geometry) {
		return StringUtils.hasText(geometry)
			&& geometry.startsWith("LINESTRING(")
			&& geometry.endsWith(")");
	}

	private record OrderedLaneGeometry(
		int order,
		OdsayLaneGeometry laneGeometry) {
	}
}
