package com.ssafy.e102.domain.route.entity;

import com.ssafy.e102.domain.route.type.AccessibilityState;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "routing_segment_overrides")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutingSegmentOverride {

	@Id
	@Column(name = "edge_id", nullable = false, updatable = false)
	private Long edgeId;

	@Enumerated(EnumType.STRING)
	@Column(name = "walk_access", nullable = false, length = 30)
	private AccessibilityState walkAccess;

	public static RoutingSegmentOverride of(Long edgeId, AccessibilityState walkAccess) {
		if (walkAccess != AccessibilityState.YES && walkAccess != AccessibilityState.NO) {
			throw new IllegalArgumentException("routing overlay walk_access must be YES or NO");
		}
		RoutingSegmentOverride override = new RoutingSegmentOverride();
		override.edgeId = edgeId;
		override.walkAccess = walkAccess;
		return override;
	}
}
