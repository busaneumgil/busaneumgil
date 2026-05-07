package com.ssafy.e102.global.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import com.ssafy.e102.domain.bookmark.entity.FavoriteRoute;
import com.ssafy.e102.domain.route.entity.RoadNode;
import com.ssafy.e102.domain.route.entity.RoadSegment;
import com.ssafy.e102.domain.user.entity.User;
import com.ssafy.e102.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

class DatabaseNamingStrategyTest {

	@Test
	@DisplayName("Hibernate 물리 컬럼 네이밍은 snake_case 전략을 사용한다")
	void applicationUsesSnakeCasePhysicalNamingStrategy() throws IOException {
		ClassPathResource resource = new ClassPathResource("application.yml");
		String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

		assertThat(content)
			.contains("org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
			.doesNotContain("org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
	}

	@Test
	@DisplayName("사용자 관련 엔티티는 snake_case 물리 컬럼명을 명시한다")
	void userAndFavoriteRouteColumnsUseSnakeCase() {
		assertThat(columnName(User.class, "userId")).isEqualTo("user_id");
		assertThat(columnName(User.class, "socialProvider")).isEqualTo("social_provider");
		assertThat(columnName(User.class, "socialProviderUserId")).isEqualTo("social_provider_user_id");
		assertThat(columnName(User.class, "selectedPrimaryUserType")).isEqualTo("selected_primary_user_type");
		assertThat(columnName(User.class, "selectedMobilitySubtype")).isEqualTo("selected_mobility_subtype");

		Table userTable = User.class.getAnnotation(Table.class);
		assertThat(userTable.uniqueConstraints())
			.extracting(UniqueConstraint::name)
			.contains("uk_users_social_provider_user_id");
		assertThat(userTable.uniqueConstraints())
			.flatExtracting(UniqueConstraint::columnNames)
			.contains("social_provider", "social_provider_user_id");

		assertThat(columnName(FavoriteRoute.class, "favRouteId")).isEqualTo("fav_route_id");
		assertThat(columnName(FavoriteRoute.class, "routeName")).isEqualTo("route_name");
		assertThat(columnName(FavoriteRoute.class, "startLabel")).isEqualTo("start_label");
		assertThat(columnName(FavoriteRoute.class, "endLabel")).isEqualTo("end_label");
		assertThat(columnName(FavoriteRoute.class, "startPoint")).isEqualTo("start_point");
		assertThat(columnName(FavoriteRoute.class, "endPoint")).isEqualTo("end_point");
		assertThat(columnName(FavoriteRoute.class, "routeOption")).isEqualTo("route_option");
		assertThat(joinColumnName(FavoriteRoute.class, "user")).isEqualTo("user_id");
	}

	@Test
	@DisplayName("보행 네트워크 엔티티는 snake_case 물리 컬럼명을 명시한다")
	void roadNetworkColumnsUseSnakeCase() {
		assertThat(columnName(RoadNode.class, "vertexId")).isEqualTo("vertex_id");
		assertThat(columnName(RoadNode.class, "sourceNodeKey")).isEqualTo("source_node_key");

		Table roadNodeTable = RoadNode.class.getAnnotation(Table.class);
		assertThat(roadNodeTable.uniqueConstraints())
			.flatExtracting(UniqueConstraint::columnNames)
			.contains("source_node_key");

		assertThat(columnName(RoadSegment.class, "edgeId")).isEqualTo("edge_id");
		assertThat(columnName(RoadSegment.class, "fromNodeId")).isEqualTo("from_node_id");
		assertThat(columnName(RoadSegment.class, "toNodeId")).isEqualTo("to_node_id");
		assertThat(columnName(RoadSegment.class, "lengthMeter")).isEqualTo("length_meter");
		assertThat(columnName(RoadSegment.class, "walkAccess")).isEqualTo("walk_access");
		assertThat(columnName(RoadSegment.class, "avgSlopePercent")).isEqualTo("avg_slope_percent");
		assertThat(columnName(RoadSegment.class, "widthMeter")).isEqualTo("width_meter");
		assertThat(columnName(RoadSegment.class, "brailleBlockState")).isEqualTo("braille_block_state");
		assertThat(columnName(RoadSegment.class, "audioSignalState")).isEqualTo("audio_signal_state");
		assertThat(columnName(RoadSegment.class, "slopeState")).isEqualTo("slope_state");
		assertThat(columnName(RoadSegment.class, "widthState")).isEqualTo("width_state");
		assertThat(columnName(RoadSegment.class, "surfaceState")).isEqualTo("surface_state");
		assertThat(columnName(RoadSegment.class, "stairsState")).isEqualTo("stairs_state");
		assertThat(columnName(RoadSegment.class, "signalState")).isEqualTo("signal_state");
		assertThat(columnName(RoadSegment.class, "segmentType")).isEqualTo("segment_type");
	}

	@Test
	@DisplayName("감사 컬럼은 snake_case로 고정한다")
	void baseEntityAuditColumnsUseSnakeCase() {
		assertThat(columnName(BaseEntity.class, "createdAt")).isEqualTo("created_at");
		assertThat(columnName(BaseEntity.class, "updatedAt")).isEqualTo("updated_at");
	}

	private String columnName(Class<?> type, String fieldName) {
		Field field = findField(type, fieldName);
		Column column = field.getAnnotation(Column.class);
		assertThat(column)
			.as("%s.%s must declare @Column", type.getSimpleName(), fieldName)
			.isNotNull();
		return column.name();
	}

	private String joinColumnName(Class<?> type, String fieldName) {
		Field field = findField(type, fieldName);
		JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
		assertThat(joinColumn)
			.as("%s.%s must declare @JoinColumn", type.getSimpleName(), fieldName)
			.isNotNull();
		return joinColumn.name();
	}

	private Field findField(Class<?> type, String fieldName) {
		Class<?> current = type;
		while (current != null) {
			try {
				return current.getDeclaredField(fieldName);
			} catch (NoSuchFieldException ignored) {
				current = current.getSuperclass();
			}
		}
		throw new IllegalArgumentException("Field not found: " + type.getName() + "." + fieldName);
	}
}
