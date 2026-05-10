package com.ssafy.e102.domain.place.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ssafy.e102.domain.place.entity.Place;

public interface PlaceRepository extends JpaRepository<Place, Long> {

	@EntityGraph(attributePaths = "accessibilityFeatures")
	Optional<Place> findWithAccessibilityFeaturesByPlaceId(Long placeId);

	@EntityGraph(attributePaths = "accessibilityFeatures")
	List<Place> findAllByProviderPlaceIdIn(Collection<String> providerPlaceIds);

	@EntityGraph(attributePaths = "accessibilityFeatures")
	List<Place> findAllByPlaceIdIn(Collection<Long> placeIds);

	Optional<Place> findByProviderPlaceId(String providerPlaceId);

	@Query(value = """
		select p.place_id
		from places p
		where (:categoriesEmpty = true or p.category in (:categories))
			and (:featureTypesEmpty = true or exists (
				select 1
				from place_accessibility_features ef
				where ef.place_id = p.place_id
					and ef.is_available = true
					and ef.feature_type in (:featureTypes)
			))
			and ST_DWithin(
				CAST(p.point AS geography),
				CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography),
				:radius
			)
		order by ST_DistanceSphere(p.point, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326))
		limit :limit
		""", nativeQuery = true)
	List<Long> findPlaceMarkerIds(
		@Param("lat")
		double lat,
		@Param("lng")
		double lng,
		@Param("radius")
		int radius,
		@Param("categories")
		Collection<String> categories,
		@Param("categoriesEmpty")
		boolean categoriesEmpty,
		@Param("featureTypes")
		Collection<String> featureTypes,
		@Param("featureTypesEmpty")
		boolean featureTypesEmpty,
		@Param("limit")
		int limit);
}
