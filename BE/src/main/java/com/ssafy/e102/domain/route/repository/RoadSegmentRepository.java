package com.ssafy.e102.domain.route.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ssafy.e102.domain.route.entity.RoadSegment;

public interface RoadSegmentRepository extends JpaRepository<RoadSegment, Long> {

	@Query(value = """
		select distinct rs.*
		from road_segments rs
		join admin_areas aa
			on ST_Intersects(rs.geom, ST_Buffer(aa.geom::geography, 100)::geometry)
		where aa.gu = :gu
			and (
				aa.dong = :dong
				or replace(replace(replace(replace(aa.dong, '1동', '동'), '2동', '동'), '3동', '동'), '4동', '동') = :dong
			)
		order by rs.edge_id asc
		limit :limit
		""", nativeQuery = true)
	List<RoadSegment> findAllIntersectingArea(
		@Param("gu")
		String gu,
		@Param("dong")
		String dong,
		@Param("limit")
		int limit);

	@Query(value = """
		select count(distinct rs.edge_id)
		from road_segments rs
		join admin_areas aa
			on ST_Intersects(rs.geom, ST_Buffer(aa.geom::geography, 100)::geometry)
		where aa.gu = :gu
			and (
				aa.dong = :dong
				or replace(replace(replace(replace(aa.dong, '1동', '동'), '2동', '동'), '3동', '동'), '4동', '동') = :dong
			)
		""", nativeQuery = true)
	long countIntersectingArea(
		@Param("gu")
		String gu,
		@Param("dong")
		String dong);

	@Query(value = """
		select exists (
			select 1
			from road_segments rs
			join admin_areas aa
				on ST_Intersects(rs.geom, ST_Buffer(aa.geom::geography, 100)::geometry)
			where rs.edge_id = :edgeId
				and aa.gu = :gu
				and (
					aa.dong = :dong
					or replace(replace(replace(replace(aa.dong, '1동', '동'), '2동', '동'), '3동', '동'), '4동', '동') = :dong
				)
		)
		""", nativeQuery = true)
	boolean existsIntersectingAreaByEdgeId(
		@Param("edgeId")
		Long edgeId,
		@Param("gu")
		String gu,
		@Param("dong")
		String dong);
}
