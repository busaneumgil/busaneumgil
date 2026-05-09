package com.ssafy.e102.domain.route.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.e102.domain.user.entity.User;
import com.ssafy.e102.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "route_ratings", uniqueConstraints = {
	@UniqueConstraint(name = "uk_route_ratings_user_route", columnNames = {"user_id", "route_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouteRating extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "rating_id", nullable = false, updatable = false)
	private Long ratingId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "route_id", nullable = false, length = 120)
	private String routeId;

	@Column(nullable = false)
	private Short score;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "route_context_json", columnDefinition = "jsonb")
	private JsonNode routeContextJson;

	public static RouteRating create(
		User user,
		String routeId,
		int score,
		JsonNode routeContextJson) {
		RouteRating routeRating = new RouteRating();
		routeRating.user = user;
		routeRating.routeId = routeId;
		routeRating.updateScore(score, routeContextJson);
		return routeRating;
	}

	public void updateScore(int score, JsonNode routeContextJson) {
		this.score = (short)score;
		this.routeContextJson = routeContextJson;
	}
}
