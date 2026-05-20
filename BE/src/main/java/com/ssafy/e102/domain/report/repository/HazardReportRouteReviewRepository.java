package com.ssafy.e102.domain.report.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.e102.domain.report.entity.HazardReportRouteReview;
import com.ssafy.e102.domain.report.type.HazardRouteReviewStage;

public interface HazardReportRouteReviewRepository extends JpaRepository<HazardReportRouteReview, Long> {

	Optional<HazardReportRouteReview> findTopByHazardReport_ReportIdOrderByReviewIdDesc(Long reportId);

	Optional<HazardReportRouteReview> findTopByHazardReport_ReportIdAndStageOrderByReviewIdDesc(
		Long reportId,
		HazardRouteReviewStage stage);
}
