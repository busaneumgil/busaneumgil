package com.ssafy.e102.domain.report.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.e102.domain.report.entity.HazardReport;

public interface HazardReportRepository extends JpaRepository<HazardReport, Long> {

	Slice<HazardReport> findAllByUser_UserId(UUID userId, Pageable pageable);

	Slice<HazardReport> findAllByUser_UserIdAndReportIdLessThan(UUID userId, Long reportId, Pageable pageable);

	@EntityGraph(attributePaths = "images")
	Optional<HazardReport> findWithImagesByReportId(Long reportId);
}
