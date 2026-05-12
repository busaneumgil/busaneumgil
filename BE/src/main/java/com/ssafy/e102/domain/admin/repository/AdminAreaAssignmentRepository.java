package com.ssafy.e102.domain.admin.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.e102.domain.admin.entity.AdminAreaAssignment;
import com.ssafy.e102.domain.admin.type.AdminAreaAssignmentType;

public interface AdminAreaAssignmentRepository extends JpaRepository<AdminAreaAssignment, Long> {

	@EntityGraph(attributePaths = "assignee")
	List<AdminAreaAssignment> findAllByOrderByGuAscDongAscAssignmentTypeAsc();

	@EntityGraph(attributePaths = "assignee")
	Optional<AdminAreaAssignment> findByGuAndDongAndAssignmentType(
		String gu,
		String dong,
		AdminAreaAssignmentType assignmentType);

	boolean existsByAssignee_UserIdAndGuAndDongAndAssignmentType(
		UUID assigneeUserId,
		String gu,
		String dong,
		AdminAreaAssignmentType assignmentType);
}
