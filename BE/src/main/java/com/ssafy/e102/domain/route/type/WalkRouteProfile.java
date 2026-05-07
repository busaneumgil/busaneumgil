package com.ssafy.e102.domain.route.type;

public enum WalkRouteProfile {
	PEDESTRIAN_SAFE("pedestrian_safe"),
	PEDESTRIAN_FAST("pedestrian_fast"),
	VISUAL_SAFE("visual_safe"),
	VISUAL_FAST("visual_fast"),
	WHEELCHAIR_MANUAL_SAFE("wheelchair_manual_safe"),
	WHEELCHAIR_MANUAL_FAST("wheelchair_manual_fast"),
	WHEELCHAIR_AUTO_SAFE("wheelchair_auto_safe"),
	WHEELCHAIR_AUTO_FAST("wheelchair_auto_fast");

	private final String profileName;

	WalkRouteProfile(String profileName) {
		this.profileName = profileName;
	}

	public String getProfileName() {
		return profileName;
	}
}
