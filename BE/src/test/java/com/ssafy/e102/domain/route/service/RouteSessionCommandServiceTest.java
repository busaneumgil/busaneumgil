package com.ssafy.e102.domain.route.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.e102.domain.route.entity.RouteSession;
import com.ssafy.e102.domain.route.repository.RouteSessionRepository;
import com.ssafy.e102.domain.route.type.RouteSessionStatus;
import com.ssafy.e102.domain.user.entity.User;
import com.ssafy.e102.domain.user.repository.UserRepository;
import com.ssafy.e102.domain.user.type.PrimaryUserType;
import com.ssafy.e102.domain.user.type.SocialProvider;

class RouteSessionCommandServiceTest {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

	private final GeometryFactory geometryFactory = new GeometryFactory();
	private final ObjectMapper objectMapper = new ObjectMapper();
	private RouteSessionRepository routeSessionRepository;
	private UserRepository userRepository;
	private RouteSessionCommandService service;

	@BeforeEach
	void setUp() {
		routeSessionRepository = mock(RouteSessionRepository.class);
		userRepository = mock(UserRepository.class);
		service = new RouteSessionCommandService(routeSessionRepository, userRepository);
	}

	@Test
	@DisplayName("ACTIVE route session 저장 시 activeRouteKey를 routeId로 채워 unique 제약 대상이 되게 한다")
	void saveActiveSessionUsesActiveRouteKey() {
		User user = user(USER_ID);
		when(routeSessionRepository.findFirstByUser_UserIdAndRouteIdAndStatusOrderByUpdatedAtDesc(
			USER_ID, "rt_selected_001", RouteSessionStatus.ACTIVE)).thenReturn(Optional.empty());
		when(userRepository.getReferenceById(USER_ID)).thenReturn(user);

		service.saveActiveSessionIfAbsent(
			USER_ID,
			"rt_selected_001",
			point(128.936, 35.12),
			point(128.956, 35.14),
			snapshot("rt_selected_001"));

		ArgumentCaptor<RouteSession> captor = ArgumentCaptor.forClass(RouteSession.class);
		verify(routeSessionRepository).saveAndFlush(captor.capture());
		assertThat(captor.getValue().getUser()).isEqualTo(user);
		assertThat(captor.getValue().getRouteId()).isEqualTo("rt_selected_001");
		assertThat(captor.getValue().getActiveRouteKey()).isEqualTo("rt_selected_001");
		assertThat(captor.getValue().getStatus()).isEqualTo(RouteSessionStatus.ACTIVE);
	}

	@Test
	@DisplayName("이미 ACTIVE session이 있으면 추가 저장하지 않는다")
	void saveActiveSessionSkipsDuplicateActiveRoute() {
		when(routeSessionRepository.findFirstByUser_UserIdAndRouteIdAndStatusOrderByUpdatedAtDesc(
			USER_ID, "rt_selected_001", RouteSessionStatus.ACTIVE)).thenReturn(Optional.of(mock(RouteSession.class)));

		service.saveActiveSessionIfAbsent(
			USER_ID,
			"rt_selected_001",
			point(128.936, 35.12),
			point(128.956, 35.14),
			snapshot("rt_selected_001"));

		verify(userRepository, never()).getReferenceById(any());
		verify(routeSessionRepository, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("완료된 session은 activeRouteKey를 비워 같은 route를 다시 ACTIVE로 선택할 수 있게 한다")
	void completeClearsActiveRouteKey() {
		RouteSession session = RouteSession.create(
			user(USER_ID),
			"rt_selected_001",
			point(128.936, 35.12),
			point(128.956, 35.14),
			snapshot("rt_selected_001"));

		session.complete();

		assertThat(session.getStatus()).isEqualTo(RouteSessionStatus.COMPLETED);
		assertThat(session.getActiveRouteKey()).isNull();
	}

	@Test
	@DisplayName("ACTIVE session 존재 여부는 status=ACTIVE 조건으로 조회한다")
	void hasActiveSessionUsesActiveStatus() {
		when(routeSessionRepository.findFirstByUser_UserIdAndRouteIdAndStatusOrderByUpdatedAtDesc(
			eq(USER_ID), eq("rt_selected_001"), eq(RouteSessionStatus.ACTIVE)))
			.thenReturn(Optional.of(mock(RouteSession.class)));

		assertThat(service.hasActiveSession(USER_ID, "rt_selected_001")).isTrue();
	}

	private Point point(double lng, double lat) {
		Point point = geometryFactory.createPoint(new Coordinate(lng, lat));
		point.setSRID(4326);
		return point;
	}

	private JsonNode snapshot(String routeId) {
		return objectMapper.createObjectNode().put("routeId", routeId);
	}

	private User user(UUID userId) {
		User user = User.create(SocialProvider.KAKAO, "kakao-user-id", PrimaryUserType.LOW_VISION, null);
		ReflectionTestUtils.setField(user, "userId", userId);
		return user;
	}
}
