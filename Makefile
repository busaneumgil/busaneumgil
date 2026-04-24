# =============================
# make 진입점
# =============================
#
# bash가 PATH에 있으면 그대로 쓰고,
# Windows에서는 Git 설치 경로에서 bash.exe를 찾아 쓴다.
BASH ?= bash
JIRA_PREFIX ?=
GIT_EXEC_PATH := $(subst \,/,$(shell git --exec-path))
MAKE_DOCKER_SCRIPT_DIR := scripts/make/docker

ifeq ($(OS),Windows_NT)
BASH := $(patsubst %/mingw64/libexec/git-core,%/bin/bash.exe,$(GIT_EXEC_PATH))
endif

.PHONY: init test-git-jira local-config local-up local-down local-logs dev-config dev-up dev-down dev-logs be-local-up ai-local-up be-dev-config be-dev-up be-dev-down be-dev-logs

# Git/Jira 보조 스크립트
# 로컬 Git 설정과 hook을 한 번에 맞춘다.
init:
	@"$(BASH)" scripts/init/init-git-jira.sh "$(JIRA_PREFIX)"

# 자동화 규칙이 안 깨졌는지 빠르게 본다.
test-git-jira:
	@"$(BASH)" scripts/test/test-git-jira.sh

# Docker Compose: local
local-config local-up local-down local-logs:
	@"$(BASH)" $(MAKE_DOCKER_SCRIPT_DIR)/$@.sh

# Docker Compose: dev server
dev-config dev-up dev-down dev-logs:
	@"$(BASH)" $(MAKE_DOCKER_SCRIPT_DIR)/$@.sh

# Docker host scripts
be-dev-config be-dev-up be-dev-down be-dev-logs:
	@"$(BASH)" $(MAKE_DOCKER_SCRIPT_DIR)/$@.sh

# Partial local stacks
be-local-up ai-local-up:
	@"$(BASH)" $(MAKE_DOCKER_SCRIPT_DIR)/$@.sh
