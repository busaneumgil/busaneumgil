package com.ssafy.e102.domain.admin.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.e102.domain.admin.entity.AdminAreaAssignment;

public interface AdminAreaAssignmentRepository extends JpaRepository<AdminAreaAssignment, Long> {

	@EntityGraph(attributePaths = "assignee")
	List<AdminAreaAssignment> findAllByOrderByGuAscDongAsc();

	@EntityGraph(attributePaths = "assignee")
	Optional<AdminAreaAssignment> findByGuAndDong(String gu, String dong);

	boolean existsByAssignee_UserIdAndGuAndDong(UUID assigneeUserId, String gu, String dong);
}
