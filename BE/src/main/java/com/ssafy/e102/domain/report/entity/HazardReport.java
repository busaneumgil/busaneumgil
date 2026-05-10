package com.ssafy.e102.domain.report.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.locationtech.jts.geom.Point;

import com.ssafy.e102.domain.report.exception.HazardReportErrorCode;
import com.ssafy.e102.domain.report.exception.HazardReportException;
import com.ssafy.e102.domain.report.type.ReportStatus;
import com.ssafy.e102.domain.report.type.ReportType;
import com.ssafy.e102.domain.user.entity.User;
import com.ssafy.e102.global.entity.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "hazard_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HazardReport extends BaseEntity {

	private static final int MAX_IMAGE_COUNT = 5;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "report_id", nullable = false, updatable = false)
	private Long reportId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "report_type", nullable = false, length = 30)
	private ReportType reportType;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "report_point", nullable = false, columnDefinition = "geometry(Point, 4326)")
	private Point reportPoint;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ReportStatus status;

	@OrderBy("displayOrder ASC")
	@OneToMany(mappedBy = "hazardReport", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<HazardReportImage> images = new ArrayList<>();

	public static HazardReport create(
		User user,
		ReportType reportType,
		String description,
		Point reportPoint,
		List<String> imageUrls) {
		HazardReport hazardReport = new HazardReport();
		hazardReport.user = requireUser(user);
		hazardReport.reportType = requireReportType(reportType);
		hazardReport.description = normalizeDescription(description);
		hazardReport.reportPoint = requirePoint(reportPoint);
		hazardReport.status = ReportStatus.PENDING;
		hazardReport.addImages(valueOrEmpty(imageUrls));
		return hazardReport;
	}

	public boolean isOwner(UUID userId) {
		return user != null && Objects.equals(user.getUserId(), userId);
	}

	public void approve() {
		validatePendingStatus();
		status = ReportStatus.APPROVED;
	}

	public void reject() {
		validatePendingStatus();
		status = ReportStatus.REJECTED;
	}

	private void validatePendingStatus() {
		if (status != ReportStatus.PENDING) {
			throw new HazardReportException(HazardReportErrorCode.HAZARD_REPORT_ALREADY_PROCESSED);
		}
	}

	private void addImages(List<String> imageUrls) {
		if (imageUrls.size() > MAX_IMAGE_COUNT) {
			throw invalidRequest("제보 이미지는 최대 5장까지 등록할 수 있습니다.");
		}
		for (int index = 0; index < imageUrls.size(); index++) {
			images.add(HazardReportImage.create(this, imageUrls.get(index), index));
		}
	}

	private static User requireUser(User user) {
		if (user == null) {
			throw invalidRequest("사용자는 필수입니다.");
		}
		return user;
	}

	private static ReportType requireReportType(ReportType reportType) {
		if (reportType == null) {
			throw invalidRequest("제보 유형은 필수입니다.");
		}
		return reportType;
	}

	private static Point requirePoint(Point reportPoint) {
		if (reportPoint == null) {
			throw invalidRequest("제보 위치는 필수입니다.");
		}
		return reportPoint;
	}

	private static String normalizeDescription(String description) {
		if (description == null || description.isBlank()) {
			return null;
		}
		return description.trim();
	}

	private static List<String> valueOrEmpty(List<String> imageUrls) {
		if (imageUrls == null) {
			return List.of();
		}
		return imageUrls;
	}

	private static HazardReportException invalidRequest(String message) {
		return new HazardReportException(HazardReportErrorCode.INVALID_HAZARD_REPORT_REQUEST, message);
	}
}
