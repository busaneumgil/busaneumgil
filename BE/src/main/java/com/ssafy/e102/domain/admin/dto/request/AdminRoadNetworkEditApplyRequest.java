package com.ssafy.e102.domain.admin.dto.request;

import java.util.List;

import com.ssafy.e102.domain.route.type.SegmentType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record AdminRoadNetworkEditApplyRequest(
	@NotEmpty
	List<@Valid Edit> edits) {

	public record Edit(
		String action,
		SegmentType segmentType,
		Geometry geom,
		Long edgeId,
		Long vertexId,
		String reason) {
	}

	public record Geometry(
		String type,
		List<List<Double>> coordinates) {
	}
}
