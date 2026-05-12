package com.ssafy.e102.domain.admin.dto.request;

import java.util.UUID;

import com.ssafy.e102.domain.admin.type.AdminAreaWorkStatus;

import jakarta.validation.constraints.NotBlank;

public record AdminAreaAssignmentUpsertRequest(
	@NotBlank
	String gu,
	@NotBlank
	String dong,
	UUID assigneeUserId,
	AdminAreaWorkStatus status) {
}
