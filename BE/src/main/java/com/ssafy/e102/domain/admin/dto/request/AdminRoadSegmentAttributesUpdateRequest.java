package com.ssafy.e102.domain.admin.dto.request;

import com.ssafy.e102.domain.route.type.AccessibilityState;
import com.ssafy.e102.domain.route.type.SurfaceState;
import com.ssafy.e102.domain.route.type.WidthState;

public record AdminRoadSegmentAttributesUpdateRequest(
	AccessibilityState walkAccess,
	AccessibilityState brailleBlockState,
	AccessibilityState audioSignalState,
	WidthState widthState,
	SurfaceState surfaceState,
	AccessibilityState stairsState,
	AccessibilityState signalState,
	Boolean applyRoutingImmediately) {
}
