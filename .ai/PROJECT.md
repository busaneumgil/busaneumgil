# Project

## Identity

- Name: 부산이음길
- English name: Busan EumGil
- Repository: S14P31E102
- Repository role: SSAFY team project repository with a shared AI harness
- Product definition: 부산 지역 이동 약자를 위한 무장애(Barrier-Free) 길찾기 모바일 서비스
- Primary platform: Android app MVP with backend APIs and spatial data infrastructure
- Primary users: 시각장애인과 보행약자
- Primary region: 부산

## Problem

부산은 언덕, 계단, 단차, 복잡한 지형이 많아 휠체어 이용자, 고령자, 유아차 동반 보호자, 일시적 이동 불편자, 시각장애인이 안전하게 이동하기 어렵다. 기존 범용 길찾기 서비스는 일반 보행자 기준 최단 경로를 제공하며, 경사도, 계단, 엘리베이터, 점자블록, 장애물 제보 같은 접근성 정보를 충분히 반영하지 못한다.

부산이음길은 이동 약자가 처음 가는 장소의 장벽 정보를 사전에 확인하고, 계단과 급경사를 피하며, 접근성 시설과 음성 안내를 활용해 더 독립적으로 이동할 수 있게 하는 것을 목표로 한다.

## Target Users

- 시각장애인: TTS 안내, 점자블록 정보, 큰 버튼, 간결한 화면, 향후 TalkBack/음성 기반 목적지 설정이 중요하다.
- 보행약자: 휠체어 이용자, 고령자, 유아차 동반 보호자, 일시적 이동 불편자를 포함한다.
- 일반 보행자는 핵심 대상이 아니다. 범용 길찾기보다 교통약자 전용 접근성 길찾기에 초점을 둔다.

## Product Scope

MVP 필수 범위:

- 온보딩: 장애 유형과 지원 수준 선택
- 위치정보 이용약관 동의
- 현재 위치 기반 부산 지도 표시
- 베리어프리 시설 마커 표시: 장애인 화장실, 엘리베이터, 전동휠체어 충전소, 무장애 관광지, 접근성 시설 보유 장소
- 시설 필터와 상세 정보 조회
- 텍스트 기반 목적지 검색
- 무장애 경로 탐색: 계단, 급경사, 단차, 좁은 보행 폭 회피와 경사로, 엘리베이터 등 우선 고려
- 경로 결과 지도 표시와 TTS/자막 안내
- 북마크와 자주 가는 길 저장
- 도로 상태 제보: 공사, 장애물, 점자블록 손상 등
- 사용자 동의 기반 경로 로그 수집

후순위 또는 확장 범위:

- 소셜 로그인과 계정 기반 동기화
- 음성 검색, STT/LLM/TTS 기반 목적지 설정
- 시각장애인 전용 고도화 UI와 TalkBack 최적화
- 경로 이탈 재탐색
- 저상버스, 지하철 엘리베이터, 두리발 등 대중교통/이동지원 연계
- Gemini API 기반 점자블록 판별
- 웨어러블, 커뮤니티, 추천, 다국어, iOS

## Product Principles

- Local-first: MVP 기본 기능은 로그인 없이 사용할 수 있어야 한다.
- Accessibility-first: 최단 경로보다 안전하고 이동 가능한 경로를 우선한다.
- Busan-specific: 부산의 경사, 산복도로, 보도 부재 생활권, 골목길 특성을 데이터와 경로 비용에 반영한다.
- Public-data-driven: 공공데이터, OSM, 지형 데이터, 사용자 제보를 정제해 접근성 정보를 보강한다.
- Privacy-conscious: 위치, 경로 로그, 장애 유형 등 민감한 정보는 최소 수집, 동의, 익명화 기준을 전제로 다룬다.
- Human-reviewable: 사용자 제보는 즉시 최종 반영하지 않고 검토 상태를 거쳐 지도 정보나 위험도에 반영한다.

## Core Domains

- 사용자 도메인: 사용자 설정, 위치 약관, TTS/수집/푸시 설정, 북마크, 자주 가는 길
- 장소 도메인: 장소 검색, 주변 시설 마커, 접근성 시설 정보
- 보행 네트워크 도메인: road nodes, road segments, segment features, route logs
- 제보 도메인: hazard reports, report images, Slack 기반 검토 흐름
- 대중교통 도메인: 저상버스, 정류장/노선/도착 정보, 지하철 엘리베이터, 외부 길찾기 후보
- 운영/인프라 도메인: dev/prod 분리, CI/CD, 모니터링, Blue/Green 운영

## Data And Routing Context

- 공간 데이터는 PostGIS를 기준으로 관리한다.
- 장소 검색은 MVP 기준 카카오 Local API와 내부 장소/접근성 DB 매칭을 전제로 한다.
- 보행 네트워크는 OSM node/edge 구조를 정제해 GraphHopper 라우팅 그래프 원천으로 활용한다.
- 경사도 데이터는 부산 5m DEM 기반 PoC 결과를 토대로 road segment 단위에 slope를 부착하는 방향이다.
- 현재 경사도 PoC는 접근성 경로 추천 가능성을 확인했지만, 계단 데이터는 별도 확보와 결합이 필요하다.
- GraphHopper import/build 작업은 운영 서버에서 직접 수행하지 않고 Jenkins 또는 별도 배치에서 graph-cache 아티팩트로 생성해 배포한다.

## Architecture Context

- Backend API는 Spring Boot 기반으로 운영한다.
- Dev stack은 Docker Compose 기반이며 PostGIS, Redis, MinIO, backend를 포함한다.
- 운영 DB는 AWS RDS PostgreSQL/PostGIS를 전제로 한다.
- 캐시는 ElastiCache/Redis를 전제로 한다.
- 핵심 런타임은 WAS와 GraphHopper runtime이다.
- AWS 운영은 현재 EC2 2대 기준으로 설계되어 있으며, S1은 blue(prod) + dev/Jenkins, S2는 green(prod standby) + 운영도구 후보 역할을 가진다.
- ALB host-based routing과 target group health check를 사용하고, Blue/Green 전환은 target group 또는 listener rule 전환을 기준으로 한다.
- Jenkins는 develop 배포와 smoke test 자동화를 담당한다.
- 모니터링은 CloudWatch를 1차 기준으로 두고, Grafana/Portainer/SonarQube/PLG는 보조 운영도구로 둔다.

## Source Documents

- `Docs/기획/2026-04-10 최종_프로젝트_기획서.md`
- `Docs/PRD/2026-04-09_부산이음길_PRD.md`
- `Docs/PRD/2026-04-14_기능명세서.md`
- `Docs/PRD/2026-04-20_MON-01_연계_요구사항_정리.md`
- `Docs/API/2026-04-12_API_전체_목록.md`
- `Docs/ARD/ERD_v3.md`
- `Docs/PoC/2026-04-21_부산_경사도_추출_정제_OSM_연계_통합_PoC.md`
- `Docs/인프라/2026-04-20_AWS_인프라_설계안.md`

## Shared AI Harness Boundary

This file is shared team context. Keep project facts and team-wide assumptions here.

- Personal task notes and local sprint progress belong under `.ai/LOCAL/`.
- Detailed harness structure belongs in `.ai/ARCHITECTURE.md`.
- Team workflow rules belong in `.ai/WORKFLOW.md`.
- Setup, release, rollback, and operational commands belong in `.ai/RUNBOOKS/`.
- Repeated debugging lessons and conventions belong in `.ai/MEMORY/` or `.ai/EVALS/`.
