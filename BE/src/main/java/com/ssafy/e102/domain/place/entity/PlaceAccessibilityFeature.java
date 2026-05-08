package com.ssafy.e102.domain.place.entity;

import com.ssafy.e102.domain.place.type.AccessibilityFeatureType;

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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "place_accessibility_features")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceAccessibilityFeature {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false, updatable = false)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "placeId", nullable = false)
	private Place place;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private AccessibilityFeatureType featureType;

	@Column(nullable = false)
	private boolean isAvailable;
}
