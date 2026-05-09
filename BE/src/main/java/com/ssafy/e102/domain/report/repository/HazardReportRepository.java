package com.ssafy.e102.domain.report.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ssafy.e102.domain.report.entity.HazardReport;
import com.ssafy.e102.domain.report.type.ReportStatus;

public interface HazardReportRepository extends JpaRepository<HazardReport, Long> {

	Slice<HazardReport> findAllByUser_UserId(UUID userId, Pageable pageable);

	Slice<HazardReport> findAllByUser_UserIdAndReportIdLessThan(UUID userId, Long reportId, Pageable pageable);

	@EntityGraph(attributePaths = "images")
	Optional<HazardReport> findWithImagesByReportId(Long reportId);

	@EntityGraph(attributePaths = "user")
	@Query("select hazardReport from HazardReport hazardReport")
	Slice<HazardReport> findAllForAdmin(Pageable pageable);

	@EntityGraph(attributePaths = "user")
	@Query("select hazardReport from HazardReport hazardReport where hazardReport.reportId < :reportId")
	Slice<HazardReport> findAllByReportIdLessThanForAdmin(
		@Param("reportId")
		Long reportId,
		Pageable pageable);

	@EntityGraph(attributePaths = "user")
	@Query("select hazardReport from HazardReport hazardReport where hazardReport.status = :status")
	Slice<HazardReport> findAllByStatusForAdmin(
		@Param("status")
		ReportStatus status,
		Pageable pageable);

	@EntityGraph(attributePaths = "user")
	@Query("""
		select hazardReport
		from HazardReport hazardReport
		where hazardReport.status = :status
			and hazardReport.reportId < :reportId
		""")
	Slice<HazardReport> findAllByStatusAndReportIdLessThanForAdmin(
		@Param("status")
		ReportStatus status,
		@Param("reportId")
		Long reportId,
		Pageable pageable);

	@EntityGraph(attributePaths = {"images", "user"})
	Optional<HazardReport> findWithImagesAndUserByReportId(Long reportId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update HazardReport hazardReport
			set hazardReport.status = :nextStatus,
				hazardReport.updatedAt = current_timestamp
			where hazardReport.reportId = :reportId
				and hazardReport.status = :currentStatus
		""")
	int updateStatusIfCurrentStatus(
		@Param("reportId")
		Long reportId,
		@Param("currentStatus")
		ReportStatus currentStatus,
		@Param("nextStatus")
		ReportStatus nextStatus);
}
