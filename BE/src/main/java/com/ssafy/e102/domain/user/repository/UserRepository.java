package com.ssafy.e102.domain.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ssafy.e102.domain.user.entity.User;
import com.ssafy.e102.domain.user.type.SocialProvider;
import com.ssafy.e102.domain.user.type.UserRole;

import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, UUID> {

	default List<User> findAllOrderByCreatedAtDesc() {
		return findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
	}

	List<User> findAllByRoleOrderByCreatedAtDesc(UserRole role);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select userEntity from User userEntity where userEntity.userId = :userId")
	Optional<User> findByIdForUpdate(
		@Param("userId")
		UUID userId);

	Optional<User> findBySocialProviderAndSocialProviderUserId(
		SocialProvider socialProvider,
		String socialProviderUserId);

	boolean existsBySocialProviderAndSocialProviderUserId(
		SocialProvider socialProvider,
		String socialProviderUserId);

	boolean existsByUserIdAndRole(UUID userId, UserRole role);
}
