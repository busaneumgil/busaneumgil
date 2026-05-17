package com.ssafy.e102.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminRoadNetworkEditServiceTest {

	@Mock
	private JdbcTemplate jdbcTemplate;

	@Mock
	private AdminService adminService;

	@Mock
	private AdminAuditLogService adminAuditLogService;

	private AdminRoadNetworkEditService adminRoadNetworkEditService;

	@BeforeEach
	void setUp() {
		adminRoadNetworkEditService = new AdminRoadNetworkEditService(
			jdbcTemplate,
			adminService,
			adminAuditLogService);
	}

	@Test
	@DisplayName("bulk insert SQL은 resolved node 좌표로 저장 geometry endpoint를 다시 맞춘다")
	void insertBulkRoadSegmentsSynchronizesResolvedNodeCoordinatesIntoGeometry() {
		ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

		ReflectionTestUtils.invokeMethod(adminRoadNetworkEditService, "insertBulkRoadSegments");

		verify(jdbcTemplate, times(2)).execute(sqlCaptor.capture());
		List<String> sqlStatements = sqlCaptor.getAllValues();
		String insertSql = sqlStatements.get(1);

		assertThat(insertSql)
			.contains("join road_nodes from_node")
			.contains("join road_nodes to_node")
			.contains("ST_SetPoint")
			.contains("ST_NPoints(raw_geom) - 1");
	}

	@Test
	@DisplayName("새 segment endpoint 검증 SQL은 저장 geometry와 node point 일치를 다시 확인한다")
	void validateNewSegmentEndpointAlignmentChecksStoredGeometryAgainstNodePoints() {
		ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
		when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);

		ReflectionTestUtils.invokeMethod(adminRoadNetworkEditService, "validateNewSegmentEndpointAlignment");

		verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), eq(Long.class));
		assertThat(sqlCaptor.getValue())
			.contains("admin_edit_new_segments")
			.contains("ST_StartPoint")
			.contains("ST_EndPoint")
			.contains("ST_DWithin");
	}
}
