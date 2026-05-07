package com.ssafy.e102.domain.place.entity;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Point;

import com.ssafy.e102.domain.place.type.PlaceCategory;
import com.ssafy.e102.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "places")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false, updatable = false)
	private Long placeId;

	@Column(nullable = false, length = 255)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private PlaceCategory category;

	@Column(length = 255)
	private String address;

	@Column(nullable = false, columnDefinition = "geometry(Point, 4326)")
	private Point point;

	@Column(length = 100)
	private String providerPlaceId;

	@OneToMany(mappedBy = "place", fetch = FetchType.LAZY)
	private List<PlaceAccessibilityFeature> accessibilityFeatures = new ArrayList<>();
}
