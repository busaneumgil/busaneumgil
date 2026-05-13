package com.ssafy.e102.domain.route.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.e102.domain.route.entity.OdsayLoadLane;
import com.ssafy.e102.domain.route.repository.OdsayLoadLaneRepository;
import com.ssafy.e102.domain.route.type.TransportMode;
import com.ssafy.e102.global.external.odsay.OdsayLaneGeometry;

class OdsayLoadLaneStoreTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Mock
	private OdsayLoadLaneRepository odsayLoadLaneRepository;

	private OdsayLoadLaneStore store;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		store = new OdsayLoadLaneStore(odsayLoadLaneRepository, objectMapper);
	}

	@Test
	@DisplayName("유효한 loadLane JSON은 mapObj 기준 lane geometry로 복원한다")
	void restoresValidLaneGeometries() throws Exception {
		OdsayLoadLane row = OdsayLoadLane.create("map-1", json("""
			[
			  {"order": 1, "transportMode": "SUBWAY", "geometry": "LINESTRING(129.2 35.2, 129.3 35.3)"},
			  {"order": 0, "transportMode": "BUS", "geometry": "LINESTRING(129.0 35.0, 129.1 35.1)"}
			]
			"""));
		when(odsayLoadLaneRepository.findAllByMapObjIn(List.of("map-1"))).thenReturn(List.of(row));

		Map<String, List<OdsayLaneGeometry>> result = store.findValidByMapObjIn(List.of("map-1"));

		assertThat(result).containsOnlyKeys("map-1");
		assertThat(result.get("map-1"))
			.extracting(OdsayLaneGeometry::type)
			.containsExactly(TransportMode.BUS, TransportMode.SUBWAY);
	}

	@Test
	@DisplayName("malformed loadLane row는 정상 cache hit로 반환하지 않는다")
	void ignoresMalformedLaneGeometries() throws Exception {
		OdsayLoadLane row = OdsayLoadLane.create("map-1", json("""
			[
			  {"order": 0, "transportMode": "WALK", "geometry": "LINESTRING(129.0 35.0, 129.1 35.1)"}
			]
			"""));
		when(odsayLoadLaneRepository.findAllByMapObjIn(List.of("map-1"))).thenReturn(List.of(row));

		Map<String, List<OdsayLaneGeometry>> result = store.findValidByMapObjIn(List.of("map-1"));

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("신규 loadLane geometry는 ERD JSON 계약으로 저장한다")
	void savesNewLaneGeometries() {
		when(odsayLoadLaneRepository.findByMapObj("map-1")).thenReturn(Optional.empty());
		List<OdsayLaneGeometry> laneGeometries = List.of(
			new OdsayLaneGeometry(TransportMode.BUS, "LINESTRING(129.0 35.0, 129.1 35.1)"));

		store.saveIfAbsentOrRepairMalformed("map-1", laneGeometries);

		ArgumentCaptor<OdsayLoadLane> captor = ArgumentCaptor.forClass(OdsayLoadLane.class);
		verify(odsayLoadLaneRepository).save(captor.capture());
		assertThat(captor.getValue().getMapObj()).isEqualTo("map-1");
		assertThat(captor.getValue().getLaneGeometries().get(0).get("order").asInt()).isZero();
		assertThat(captor.getValue().getLaneGeometries().get(0).get("transportMode").asText()).isEqualTo("BUS");
		assertThat(captor.getValue().getLaneGeometries().get(0).get("geometry").asText())
			.isEqualTo("LINESTRING(129.0 35.0, 129.1 35.1)");
	}

	@Test
	@DisplayName("기존 malformed row는 재조회 성공 결과로 복구 저장한다")
	void repairsExistingMalformedRow() throws Exception {
		OdsayLoadLane row = OdsayLoadLane.create("map-1", json("""
			{"unexpected": true}
			"""));
		when(odsayLoadLaneRepository.findByMapObj("map-1")).thenReturn(Optional.of(row));

		store.saveIfAbsentOrRepairMalformed("map-1", List.of(
			new OdsayLaneGeometry(TransportMode.SUBWAY, "LINESTRING(129.0 35.0, 129.1 35.1)")));

		verify(odsayLoadLaneRepository, never()).save(any());
		assertThat(row.getLaneGeometries().isArray()).isTrue();
		assertThat(row.getLaneGeometries().get(0).get("transportMode").asText()).isEqualTo("SUBWAY");
	}

	private JsonNode json(String value) throws Exception {
		return objectMapper.readTree(value);
	}
}
