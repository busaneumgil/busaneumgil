package com.ssafy.e102.domain.route.service;

import org.springframework.stereotype.Component;

import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.route.type.WalkRouteProfile;
import com.ssafy.e102.domain.user.type.MobilitySubtype;
import com.ssafy.e102.domain.user.type.PrimaryUserType;

@Component
public class WalkRouteProfileResolver {

	public WalkRouteProfile resolve(
		PrimaryUserType primaryUserType,
		MobilitySubtype mobilitySubtype,
		RouteOption routeOption) {
		if (routeOption == null) {
			throw invalidRequest("경로 옵션은 필수입니다.");
		}
		return switch (requirePrimaryUserType(primaryUserType)) {
			case LOW_VISION -> resolveLowVision(mobilitySubtype, routeOption);
			case MOBILITY_IMPAIRED -> resolveMobilityImpaired(mobilitySubtype, routeOption);
		};
	}

	public String resolveProfileName(
		PrimaryUserType primaryUserType,
		MobilitySubtype mobilitySubtype,
		RouteOption routeOption) {
		return resolve(primaryUserType, mobilitySubtype, routeOption).getProfileName();
	}

	private PrimaryUserType requirePrimaryUserType(PrimaryUserType primaryUserType) {
		if (primaryUserType == null) {
			throw invalidRequest("사용자 유형은 필수입니다.");
		}
		return primaryUserType;
	}

	private WalkRouteProfile resolveLowVision(MobilitySubtype mobilitySubtype, RouteOption routeOption) {
		if (mobilitySubtype != null) {
			throw invalidRequest("저시력자는 보행약자 세부 유형을 가질 수 없습니다.");
		}
		return switch (routeOption) {
			case SAFE -> WalkRouteProfile.VISUAL_SAFE;
			case SHORTEST -> WalkRouteProfile.VISUAL_FAST;
		};
	}

	private WalkRouteProfile resolveMobilityImpaired(MobilitySubtype mobilitySubtype, RouteOption routeOption) {
		if (mobilitySubtype == null) {
			throw invalidRequest("보행약자는 보행약자 세부 유형이 필요합니다.");
		}
		return switch (mobilitySubtype) {
			case POWER_WHEELCHAIR -> resolvePowerWheelchair(routeOption);
			case MANUAL_WHEELCHAIR -> resolveManualWheelchair(routeOption);
			case OTHER_MOBILITY -> resolveOtherMobility(routeOption);
		};
	}

	private WalkRouteProfile resolvePowerWheelchair(RouteOption routeOption) {
		return switch (routeOption) {
			case SAFE -> WalkRouteProfile.WHEELCHAIR_AUTO_SAFE;
			case SHORTEST -> WalkRouteProfile.WHEELCHAIR_AUTO_FAST;
		};
	}

	private WalkRouteProfile resolveManualWheelchair(RouteOption routeOption) {
		return switch (routeOption) {
			case SAFE -> WalkRouteProfile.WHEELCHAIR_MANUAL_SAFE;
			case SHORTEST -> WalkRouteProfile.WHEELCHAIR_MANUAL_FAST;
		};
	}

	private WalkRouteProfile resolveOtherMobility(RouteOption routeOption) {
		return switch (routeOption) {
			case SAFE -> WalkRouteProfile.PEDESTRIAN_SAFE;
			case SHORTEST -> WalkRouteProfile.PEDESTRIAN_FAST;
		};
	}

	private RouteException invalidRequest(String message) {
		return new RouteException(RouteErrorCode.INVALID_ROUTE_REQUEST, message);
	}
}
