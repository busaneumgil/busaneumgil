package com.ssafy.e102.domain.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.e102.domain.user.entity.User;
import com.ssafy.e102.domain.user.type.SocialProvider;
import com.ssafy.e102.domain.user.type.UserRole;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findBySocialProviderAndSocialProviderUserId(
		SocialProvider socialProvider,
		String socialProviderUserId);

	boolean existsBySocialProviderAndSocialProviderUserId(
		SocialProvider socialProvider,
		String socialProviderUserId);

	boolean existsByUserIdAndRole(UUID userId, UserRole role);
}
