package com.ssafy.e102.domain.admin.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e102.domain.admin.dto.request.AdminRoadNetworkEditApplyRequest;
import com.ssafy.e102.domain.admin.dto.response.AdminRoadNetworkEditApplyResponse;
import com.ssafy.e102.domain.route.type.SegmentType;
import com.ssafy.e102.global.exception.BusinessException;
import com.ssafy.e102.global.exception.CommonErrorCode;

@Service
public class AdminRoadNetworkEditService {

	private static final double SNAP_DISTANCE_METER = 1.5;
	private static final int SRID = 4326;

	private final JdbcTemplate jdbcTemplate;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public AdminRoadNetworkEditService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
	}

	@Transactional
	public AdminRoadNetworkEditApplyResponse apply(AdminRoadNetworkEditApplyRequest request) {
		List<AdminRoadNetworkEditApplyRequest.Edit> edits = request.edits();
		Set<Long> deleteEdgeIds = new LinkedHashSet<>();
		Set<Long> deleteNodeIds = new LinkedHashSet<>();

		for (AdminRoadNetworkEditApplyRequest.Edit edit : edits) {
			if ("delete_segment".equals(edit.action())) {
				deleteEdgeIds.add(requirePositiveId(edit.edgeId(), "삭제할 segment ID는 필수입니다."));
				continue;
			}
			if ("delete_node".equals(edit.action())) {
				deleteNodeIds.add(requirePositiveId(edit.vertexId(), "삭제할 node ID는 필수입니다."));
			}
		}

		if (!deleteNodeIds.isEmpty()) {
			deleteEdgeIds.addAll(findIncidentEdgeIds(deleteNodeIds));
		}

		Set<Long> orphanCleanupCandidateNodeIds = new LinkedHashSet<>(deleteNodeIds);
		orphanCleanupCandidateNodeIds.addAll(findEndpointNodeIds(deleteEdgeIds));
		List<Long> deletedEdgeIds = deleteSegments(deleteEdgeIds);
		ApplyCounters counters = new ApplyCounters();
		List<Long> addedEdgeIds = new ArrayList<>();
		List<Long> createdNodeIds = new ArrayList<>();
		List<Long> snappedNodeIds = new ArrayList<>();

		for (AdminRoadNetworkEditApplyRequest.Edit edit : edits) {
			if (!"add_segment".equals(edit.action())) {
				continue;
			}
			LineInput lineInput = requireLineInput(edit);
			NodeRef fromNode = ensureNode(lineInput.first());
			NodeRef toNode = ensureNode(lineInput.last());
			orphanCleanupCandidateNodeIds.add(fromNode.vertexId());
			orphanCleanupCandidateNodeIds.add(toNode.vertexId());
			if (fromNode.vertexId().equals(toNode.vertexId())) {
				continue;
			}
			recordNodeResult(fromNode, counters, createdNodeIds, snappedNodeIds);
			recordNodeResult(toNode, counters, createdNodeIds, snappedNodeIds);

			Long edgeId = nextSequenceValue("road_segments_edge_id_seq");
			insertRoadSegment(edgeId, fromNode.vertexId(), toNode.vertexId(), lineInput,
				segmentTypeOrDefault(edit.segmentType()));
			addedEdgeIds.add(edgeId);
			counters.createdSegmentFeatures += insertSegmentFeatures(edgeId);
			counters.updatedSegmentAttributes += updateSegmentAttributes(edgeId);
		}

		int removedOrphanNodes = removeOrphanNodes(orphanCleanupCandidateNodeIds);
		return new AdminRoadNetworkEditApplyResponse(
			addedEdgeIds.size(),
			deletedEdgeIds.size(),
			counters.createdNodes,
			counters.snappedNodes,
			removedOrphanNodes,
			counters.createdSegmentFeatures,
			counters.updatedSegmentAttributes,
			addedEdgeIds,
			deletedEdgeIds,
			createdNodeIds,
			snappedNodeIds);
	}

	private List<Long> findIncidentEdgeIds(Set<Long> deleteNodeIds) {
		return namedParameterJdbcTemplate.queryForList(
			"""
				select edge_id
				from road_segments
				where from_node_id in (:nodeIds)
					or to_node_id in (:nodeIds)
				order by edge_id
				""",
			Map.of("nodeIds", deleteNodeIds),
			Long.class);
	}

	private List<Long> findEndpointNodeIds(Set<Long> edgeIds) {
		if (edgeIds.isEmpty()) {
			return List.of();
		}
		return namedParameterJdbcTemplate.queryForList(
			"""
				select distinct node_id
				from (
					select from_node_id as node_id
					from road_segments
					where edge_id in (:edgeIds)
					union
					select to_node_id as node_id
					from road_segments
					where edge_id in (:edgeIds)
				) endpoint_nodes
				order by node_id
				""",
			Map.of("edgeIds", edgeIds),
			Long.class);
	}

	private List<Long> deleteSegments(Set<Long> edgeIds) {
		if (edgeIds.isEmpty()) {
			return List.of();
		}
		MapSqlParameterSource parameters = new MapSqlParameterSource("edgeIds", edgeIds);
		namedParameterJdbcTemplate.update(
			"delete from segment_features where edge_id in (:edgeIds)",
			parameters);
		return namedParameterJdbcTemplate.queryForList(
			"""
				delete from road_segments
				where edge_id in (:edgeIds)
				returning edge_id
				""",
			parameters,
			Long.class);
	}

	private NodeRef ensureNode(CoordinateInput coordinate) {
		List<Long> snappedNodeIds = jdbcTemplate.queryForList(
			"""
				select vertex_id
				from road_nodes
				where ST_DWithin(
					"point"::geography,
					ST_SetSRID(ST_MakePoint(?, ?), ?)::geography,
					?
				)
				order by ST_Distance(
					"point"::geography,
					ST_SetSRID(ST_MakePoint(?, ?), ?)::geography
				) asc,
				vertex_id asc
				limit 1
				""",
			Long.class,
			coordinate.lng(),
			coordinate.lat(),
			SRID,
			SNAP_DISTANCE_METER,
			coordinate.lng(),
			coordinate.lat(),
			SRID);
		if (!snappedNodeIds.isEmpty()) {
			return new NodeRef(snappedNodeIds.get(0), true);
		}

		Long vertexId = nextSequenceValue("road_nodes_vertex_id_seq");
		jdbcTemplate.update(
			"""
				insert into road_nodes (vertex_id, source_node_key, "point")
				values (?, ?, ST_SetSRID(ST_MakePoint(?, ?), ?))
				""",
			vertexId,
			"editor:manual:" + vertexId,
			coordinate.lng(),
			coordinate.lat(),
			SRID);
		return new NodeRef(vertexId, false);
	}

	private void insertRoadSegment(
		Long edgeId,
		Long fromNodeId,
		Long toNodeId,
		LineInput lineInput,
		SegmentType segmentType) {
		String lineWkt = lineInput.toWkt();
		jdbcTemplate.update(
			"""
				insert into road_segments (
					edge_id,
					from_node_id,
					to_node_id,
					"geom",
					length_meter,
					walk_access,
					braille_block_state,
					audio_signal_state,
					width_state,
					surface_state,
					stairs_state,
					signal_state,
					segment_type
				)
				values (
					?, ?, ?,
					ST_GeomFromText(?, ?),
					round(ST_Length(ST_GeomFromText(?, ?)::geography)::numeric, 2),
					'UNKNOWN',
					'UNKNOWN',
					'UNKNOWN',
					'UNKNOWN',
					'UNKNOWN',
					'UNKNOWN',
					'UNKNOWN',
					?
				)
				""",
			edgeId,
			fromNodeId,
			toNodeId,
			lineWkt,
			SRID,
			lineWkt,
			SRID,
			segmentType.name());
	}

	private int insertSegmentFeatures(Long edgeId) {
		return jdbcTemplate.update(
			"""
				with source_candidates as (
					select
						sf.feature_type,
						sf.geom,
						sf.state,
						sf.value_number,
						ST_Distance(ST_Transform(sf.geom, 5179), ST_Transform(rs.geom, 5179)) as distance_meter
					from source_features sf
					join road_segments rs on rs.edge_id = ?
					where sf.feature_type in ('CROSSWALK', 'AUDIO_SIGNAL', 'BRAILLE_BLOCK', 'STAIRS')
						and ST_DWithin(
							ST_Transform(sf.geom, 5179),
							ST_Transform(rs.geom, 5179),
							source_match_threshold_meter(sf.source_file)
						)
				),
				deduped as (
					select distinct on (feature_type, coalesce(state, ''))
						feature_type,
						geom,
						state,
						value_number
					from source_candidates
					order by feature_type, coalesce(state, ''), distance_meter asc
				)
				insert into segment_features (feature_id, edge_id, feature_type, geom, state, value_number)
				select
					nextval('segment_features_feature_id_seq'),
					?,
					feature_type,
					geom,
					state,
					value_number
				from deduped
				""",
			edgeId,
			edgeId);
	}

	private int updateSegmentAttributes(Long edgeId) {
		return jdbcTemplate.update(
			"""
				with source_candidates as (
					select
						sf.feature_type,
						sf.state,
						sf.value_number
					from source_features sf
					join road_segments rs on rs.edge_id = ?
					where ST_DWithin(
						ST_Transform(sf.geom, 5179),
						ST_Transform(rs.geom, 5179),
						source_match_threshold_meter(sf.source_file)
					)
				),
				updates as (
					select
						count(*) as match_count,
						bool_or(feature_type = 'WALK_ACCESS' and state = 'YES') as walk_access_yes,
						bool_or(feature_type = 'CROSSWALK' and state = 'YES') as crosswalk_yes,
						bool_or(feature_type = 'SIGNAL' and state = 'YES') as signal_yes,
						bool_or(feature_type = 'AUDIO_SIGNAL' and state = 'YES') as audio_signal_yes,
						bool_or(feature_type = 'STAIRS' and state = 'YES') as stairs_yes,
						case
							when bool_or(feature_type = 'BRAILLE_BLOCK' and state = 'YES') then 'YES'
							when bool_or(feature_type = 'BRAILLE_BLOCK' and state = 'NO') then 'NO'
							else null
						end as braille_block_state,
						avg(value_number) filter (where feature_type = 'SLOPE' and value_number is not null) as avg_slope_percent,
						percentile_cont(0.5) within group (order by value_number)
							filter (where feature_type = 'WIDTH' and value_number is not null) as width_meter,
						bool_or(feature_type = 'SURFACE' and state = 'UNPAVED') as has_unpaved,
						bool_or(feature_type = 'SURFACE' and state = 'PAVED') as has_paved
					from source_candidates
				)
				update road_segments rs
				set
					walk_access = case
						when updates.walk_access_yes and rs.walk_access <> 'NO' then 'YES'
						else rs.walk_access
					end,
					avg_slope_percent = coalesce(round(updates.avg_slope_percent::numeric, 2), rs.avg_slope_percent),
					width_meter = coalesce(round(updates.width_meter::numeric, 2), rs.width_meter),
					braille_block_state = coalesce(updates.braille_block_state, rs.braille_block_state),
					audio_signal_state = case
						when updates.audio_signal_yes then 'YES'
						else rs.audio_signal_state
					end,
					width_state = case
						when updates.width_meter is null then rs.width_state
						when updates.width_meter >= 1.50 then 'ADEQUATE_150'
						when updates.width_meter >= 1.20 then 'ADEQUATE_120'
						else 'NARROW'
					end,
					surface_state = case
						when updates.has_unpaved then 'UNPAVED'
						when updates.has_paved then 'PAVED'
						else rs.surface_state
					end,
					stairs_state = case
						when updates.stairs_yes then 'YES'
						else rs.stairs_state
					end,
					signal_state = case
						when updates.signal_yes then 'YES'
						else rs.signal_state
					end,
					segment_type = case
						when updates.crosswalk_yes then 'CROSS_WALK'
						else rs.segment_type
					end
				from updates
				where rs.edge_id = ?
					and updates.match_count > 0
				""",
			edgeId,
			edgeId);
	}

	private int removeOrphanNodes(Set<Long> candidateNodeIds) {
		if (candidateNodeIds.isEmpty()) {
			return 0;
		}
		return namedParameterJdbcTemplate.update(
			"""
				delete from road_nodes rn
				where rn.vertex_id in (:nodeIds)
					and not exists (
					select 1
					from road_segments rs
					where rs.from_node_id = rn.vertex_id
						or rs.to_node_id = rn.vertex_id
				)
				""",
			Map.of("nodeIds", candidateNodeIds));
	}

	private Long nextSequenceValue(String sequenceName) {
		return jdbcTemplate.queryForObject("select nextval(cast(? as regclass))", Long.class, sequenceName);
	}

	private LineInput requireLineInput(AdminRoadNetworkEditApplyRequest.Edit edit) {
		AdminRoadNetworkEditApplyRequest.Geometry geom = edit.geom();
		if (geom == null || !"LineString".equals(geom.type()) || geom.coordinates() == null
			|| geom.coordinates().size() < 2) {
			throw invalidRequest("추가할 segment geometry가 올바르지 않습니다.");
		}
		List<CoordinateInput> coordinates = geom.coordinates()
			.stream()
			.map(this::toCoordinateInput)
			.toList();
		return new LineInput(coordinates);
	}

	private CoordinateInput toCoordinateInput(List<Double> coordinate) {
		if (coordinate == null || coordinate.size() < 2) {
			throw invalidRequest("좌표는 [lng, lat] 형식이어야 합니다.");
		}
		Double lng = coordinate.get(0);
		Double lat = coordinate.get(1);
		if (lng == null || lat == null || lng < -180 || lng > 180 || lat < -90 || lat > 90) {
			throw invalidRequest("좌표 범위가 올바르지 않습니다.");
		}
		return new CoordinateInput(lng, lat);
	}

	private Long requirePositiveId(Long id, String message) {
		if (id == null || id <= 0) {
			throw invalidRequest(message);
		}
		return id;
	}

	private SegmentType segmentTypeOrDefault(SegmentType segmentType) {
		return segmentType == null ? SegmentType.SIDE_LINE : segmentType;
	}

	private void recordNodeResult(
		NodeRef nodeRef,
		ApplyCounters counters,
		List<Long> createdNodeIds,
		List<Long> snappedNodeIds) {
		if (nodeRef.snapped()) {
			counters.snappedNodes++;
			snappedNodeIds.add(nodeRef.vertexId());
			return;
		}
		counters.createdNodes++;
		createdNodeIds.add(nodeRef.vertexId());
	}

	private BusinessException invalidRequest(String message) {
		return new BusinessException(CommonErrorCode.INVALID_INPUT, message);
	}

	private record CoordinateInput(
		double lng,
		double lat) {
	}

	private record NodeRef(
		Long vertexId,
		boolean snapped) {
	}

	private record LineInput(
		List<CoordinateInput> coordinates) {

		CoordinateInput first() {
			return coordinates.get(0);
		}

		CoordinateInput last() {
			return coordinates.get(coordinates.size() - 1);
		}

		String toWkt() {
			return "LINESTRING(" + coordinates.stream()
				.map(coordinate -> coordinate.lng() + " " + coordinate.lat())
				.reduce((left, right) -> left + ", " + right)
				.orElseThrow() + ")";
		}
	}

	private static class ApplyCounters {
		private int createdNodes;
		private int snappedNodes;
		private int createdSegmentFeatures;
		private int updatedSegmentAttributes;
	}
}
