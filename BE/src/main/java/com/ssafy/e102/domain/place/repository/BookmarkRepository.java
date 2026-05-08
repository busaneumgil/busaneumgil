package com.ssafy.e102.domain.place.repository;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ssafy.e102.domain.place.entity.Bookmark;

public interface BookmarkRepository extends JpaRepository<Bookmark, Integer> {

	boolean existsByUser_UserIdAndPlace_PlaceId(UUID userId, Long placeId);

	@Query("""
		select bookmark.place.placeId
		from Bookmark bookmark
		where bookmark.user.userId = :userId
			and bookmark.place.placeId in :placeIds
		""")
	Set<Long> findBookmarkedPlaceIds(
		@Param("userId")
		UUID userId,
		@Param("placeIds")
		Collection<Long> placeIds);
}
