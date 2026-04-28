# Docs Source Map

`Docs/`는 부산이음길의 제품, 기획, API, 데이터, 인프라, 컨벤션 기준이다. AI 작업은 `.ai/PROJECT.md`만 보고 판단하지 말고, 작업 유형에 맞는 `Docs/` 문서를 함께 읽어야 한다.

## 문서 최신성 원칙

문서가 많고 일부는 오래되었기 때문에, AI는 모든 문서를 같은 무게로 취급하지 않는다.

- 구현 계약은 최신 도메인 상세 문서와 실제 코드 구조를 우선한다.
- 오래된 통합 문서는 배경과 의도 파악용으로만 사용한다.
- 스킬 실행 전 또는 문서 선택 전 `./.ai/scripts/docs-source-report.sh`로 현재 문서 후보와 로컬 제외 목록을 확인한다.
- 사용자가 특정 문서를 아직 최신화되지 않았다고 표시한 경우, 그 문서는 수정되거나 제외가 해제되기 전까지 source of truth로 사용하지 않는다.
- FE 작업은 `Docs/기획`만 보지 말고 반드시 1차 FE 계약 문서와 실제 `FE/app` 라우트/화면 코드를 함께 확인한다.
- `FE/docs/2026-04-27_로그인_필수_전환_FE_정합성_및_구현_영향.md`, `FE/docs/2026-04-27_온보딩_디자인_화면_구성_분석.md`, `FE/docs/2026-04-27_FE_Docs_문서_정합성_재검토.md`, `FE/docs/sprint_backlog/`는 2차 정리 문서다. 1차 FE 계약 문서, 직접 디자인 산출물, 실제 코드를 요약하거나 실행 맥락을 정리할 뿐 단독 기준이 아니다.
- BE 작업은 `Docs/API`, `Docs/ARD`, `Docs/skills/backend`, 실제 `BE` 코드를 함께 확인한다.
- API, 화면, ERD, 코드가 충돌하면 계획 파일에 `문서 충돌` 항목을 만들고, 어떤 기준을 임시 source of truth로 삼았는지 명시한다.
- 확정이 필요한 충돌은 임의 구현하지 말고 `/plan` 결과의 Open Questions 또는 `.ai/DECISIONS/` ADR 후보로 남긴다.

## 로컬 문서 제외 명령

문서가 아직 최신화되지 않았다는 사실을 한 번만 입력해 두고, 해당 로컬 작업 컨텍스트에서만 AI가 그 문서를 기준으로 삼지 않게 할 수 있다.

```bash
./.ai/scripts/docs-context.sh stale "Docs/기획/2026-04-09_화면명세서.md" "FE 라우트 맵보다 오래된 화면 흐름"
./.ai/scripts/docs-context.sh status
./.ai/scripts/docs-context.sh clear "Docs/기획/2026-04-09_화면명세서.md"
```

- 제외 상태는 `.ai/LOCAL/DOCS/context-exclusions.json`에 저장되며 git에 올리지 않는 로컬 컨텍스트다.
- 제외할 때의 파일 해시를 저장하므로, 해당 문서 내용이 수정되면 제외 상태는 자동 만료되고 다시 읽을 수 있다.
- `stale`로 활성 제외된 문서는 구현, 계획, 리뷰에서 계약 근거로 쓰지 않는다. 필요하면 “현재 제외된 오래된 배경 문서”라고만 언급한다.
- 문서가 실제로 최신화되었으면 `clear`를 실행하거나, 파일 수정 후 다음 스킬 실행에서 자동 만료된 상태를 확인한다.
- 만료된 항목을 로컬 상태 파일에서 정리하려면 `./.ai/scripts/docs-context.sh prune`을 실행한다.

## 작업 영역별 Source Of Truth

| 작업 영역 | 1차 기준 | 2차 기준 | 오래된 문서 사용 방식 |
|---|---|---|---|
| FE 화면/라우트 | `FE/docs/2026-04-22_부산이음길_FE_화면_인벤토리_및_라우트_맵.md`, 실제 `FE/app` navigation/screen 코드 | `FE/docs/2026-04-21_S14P31E102-211_공통_UI_접근성_Figma_handoff.md`, `FE/mockup/`, `Docs/기획/*화면명세서*` | 화면 의도 보강용 |
| FE UI/디자인 | `FE/docs/2026-04-22_부산이음길_FE_디자인_컨벤션.md`, `FE/docs/2026-04-13_부산이음길_FE_컴포넌트_가이드.md` | `FE/docs/2026-04-21_S14P31E102-211_공통_UI_접근성_Figma_handoff.md`, `FE/mockup/` | 디자인 컨벤션은 1차 시각 계약, 컴포넌트 가이드는 구현 조합 계약 |
| FE 접근성/라벨 | `FE/docs/2026-04-24_부산이음길_FE_접근성_정보_라벨_가이드.md`, FE 컴포넌트 가이드 | PRD, 인터뷰 결과 | 접근성 의도 참고 |
| FE 코드 구조 | `FE/docs/2026-04-13_부산이음길_FE_코드_컨벤션.md`, 실제 `FE/app` 구조 | FE 화면 인벤토리 | 배경 참고 |
| BE API | `Docs/API/2026-04-12_API_전체_목록.md`, 도메인별 API 명세 | API 응답 코드 컨벤션, 실제 BE 코드 | 요구 배경 참고 |
| BE 데이터/ERD | `Docs/ARD/ERD_v3.md` | API 명세, 실제 Entity/Repository | 이전 ERD는 변경 이력 참고 |
| 공간/경로 | `Docs/PoC/2026-04-21_부산_경사도_추출_정제_OSM_연계_통합_PoC.md`, 보행 네트워크 API | ERD, 인프라 문서 | 가능성/한계 참고 |
| 인프라/배포 | `Docs/인프라/2026-04-20_AWS_인프라_설계안.md`, `Docs/인프라/2026-04-23_Docker_운영_기준.md`, 최신 운영 가이드 | 실제 compose/Jenkins/nginx 설정 | 설계 배경 참고 |
| 팀 컨벤션 | `Docs/컨벤션/`, `Docs/skills/backend/`, `FE/docs/*컨벤션*` | 실제 코드 스타일 | 기본 규칙 참고 |

## 공통 규칙

- 문서 작성 및 수정 규칙: `Docs/2026-04-07_docs_작성_규칙.md`
- Git/Jira/MR 규칙: `Docs/컨벤션/2026-04-09_Git_Jira_컨벤션.md`
- API 응답 코드 규칙: `Docs/컨벤션/2026-04-14_API_응답_코드_컨벤션.md`
- 백엔드 구현 컨벤션: `Docs/skills/backend/backend-convention.md`
- 백엔드 계층/패키지 규칙: `Docs/skills/backend/layer-package-convention.md`
- 백엔드 응답/예외 규칙: `Docs/skills/backend/api-response-error-convention.md`
- 설정/외부 연동 규칙: `Docs/skills/backend/config-external-convention.md`
- FE 코드 컨벤션: `FE/docs/2026-04-13_부산이음길_FE_코드_컨벤션.md`
- FE 컴포넌트 가이드: `FE/docs/2026-04-13_부산이음길_FE_컴포넌트_가이드.md`
- FE 디자인 컨벤션: `FE/docs/2026-04-22_부산이음길_FE_디자인_컨벤션.md`
- FE 디자인 컨벤션은 선택적인 미감 참고 문서가 아니다. 색상, 타이포, spacing, radius, surface 계층, CTA 위계, 카드/리스트 상태, 하단 탭 shell은 이 문서를 우선 기준으로 본다.
- FE 접근성 라벨 가이드: `FE/docs/2026-04-24_부산이음길_FE_접근성_정보_라벨_가이드.md`
- FE 화면 인벤토리/라우트 맵: `FE/docs/2026-04-22_부산이음길_FE_화면_인벤토리_및_라우트_맵.md`

## 단일 레포 작업 원칙

레포가 FE/BE로 분리되어 있지 않기 때문에, AI는 작업 영역을 먼저 분리해서 읽는다.

- lane 전용 작업은 `.ai/LANES.md`를 먼저 읽고 `/fe-*` 또는 `/be-*` 명령어의 범위를 따른다.
- lane 전용 작업에서도 `docs-context.sh status` 또는 `docs-source-report.sh`에 표시된 활성 제외 문서는 기준 문서에서 뺀다.
- FE 단독 작업은 1차 FE 계약 문서와 실제 `FE/app` 코드를 1차 기준으로 삼고, `Docs/기획`은 배경으로만 사용한다.
- FE 단독 작업에서 2차 정리 문서는 직접 산출물과 1차 계약 문서를 다시 찾아가기 위한 색인으로만 사용한다.
- FE 단독 작업에서 디자인 수정, UI 구현, 화면 리팩터링, 디자인 QA는 반드시 `FE 디자인 컨벤션 -> FE 컴포넌트 가이드 -> Figma/mockup -> 실제 FE/app 코드` 순서로 확인한다.
- 실제 `FE/app` 코드가 최신 디자인 컨벤션과 다르면 코드를 곧바로 정답으로 승격하지 않는다. `현재 구현 사실`과 `목표 시각 계약`을 분리해서 해석한다.
- BE 단독 작업은 `Docs/API`, `Docs/ARD`, `Docs/skills/backend`, 실제 `BE` 코드를 1차 기준으로 삼는다.
- 한쪽 작업 중 반대쪽 계약이 필요하면 API 명세, DTO, route, schema처럼 경계에 닿는 문서와 코드만 추가로 읽는다.
- 오래된 화면명세서, 기능명세서, ERD가 현재 코드와 다르면 계획에 `문서 충돌`을 기록하고, 이번 작업에서 따를 기준을 명시한다.
- 문서 최신화가 작업 범위에 포함되면 `/document-release`로 해당 `Docs/` 또는 `FE/docs` 문서를 같이 갱신한다.

## 기획 단계

`/make`, `/plan`, `plan-ceo-review`, `plan-design-review`는 아래 문서를 우선 읽는다.

- 최종 프로젝트 기획서: `Docs/기획/2026-04-10 최종_프로젝트_기획서.md`
- PRD: `Docs/PRD/2026-04-09_부산이음길_PRD.md`
- 기능명세서: `Docs/PRD/2026-04-14_기능명세서.md`
- MON-01 연계 요구사항: `Docs/PRD/2026-04-20_MON-01_연계_요구사항_정리.md`
- 화면명세서: `Docs/기획/2026-04-09_화면명세서.md`
- MVP 화면명세서: `Docs/기획/2026-04-11_MVP_화면명세서.md`
- 확장 기능 화면명세서: `Docs/기획/2026-04-11_확장_기능_화면명세서.md`
- FE 화면 인벤토리/라우트 맵: `FE/docs/2026-04-22_부산이음길_FE_화면_인벤토리_및_라우트_맵.md`
- FE 디자인 컨벤션: `FE/docs/2026-04-22_부산이음길_FE_디자인_컨벤션.md`
- 필드트립/인터뷰 결과: `Docs/기획/2026-04-10_함세상_장애인자립생활센터_인터뷰_결과.md`
- 개발 일정: `Docs/기획/2026-04-23_개발_일정_공유.md`

## 계획 단계

`/plan`, `plan-eng-review`는 기획 문서에 더해 아래 문서를 읽고 구현 단위를 나눈다.

- API 전체 목록: `Docs/API/2026-04-12_API_전체_목록.md`
- 도메인별 API 명세: `Docs/API/*/*_API_명세.md`
- 최신 ERD: `Docs/ARD/ERD_v3.md`
- 경사도/OSM/GraphHopper PoC: `Docs/PoC/2026-04-21_부산_경사도_추출_정제_OSM_연계_통합_PoC.md`
- 인프라 설계: `Docs/인프라/2026-04-20_AWS_인프라_설계안.md`
- Docker 운영 기준: `Docs/인프라/2026-04-23_Docker_운영_기준.md`
- 인프라 스프린트 계획: `Docs/인프라/2026-04-23_인프라_스프린트_작업계획.md`
- FE 화면/상태 계획: `FE/docs/`, `FE/docs/sprint_backlog/`

## 구현 단계

`/start`, `/fix-bug`, `/refactor-module`, `/write-test`, `/investigate`는 변경 영역별로 아래 기준을 적용한다.

- FE 구현은 `/fe-start`, `/fe-fix-bug`, `/fe-refactor-module`, `/fe-write-test`, `/fe-investigate`를 우선 사용한다.
- BE 구현은 `/be-start`, `/be-fix-bug`, `/be-refactor-module`, `/be-write-test`, `/be-investigate`를 우선 사용한다.
- Backend 변경: `Docs/skills/backend/*.md`, `Docs/API/`, `Docs/ARD/ERD_v3.md`
- Frontend 변경: 1차 FE 계약 문서, 실제 `FE/app/src/main/java/...` navigation, screen, ViewModel, contract 코드
- Frontend 시각 변경: 특히 `FE/docs/2026-04-22_부산이음길_FE_디자인_컨벤션.md`의 토큰, CTA 위계, 카드/리스트 패턴, shell 규칙과 대조
- API 응답/예외 변경: `Docs/컨벤션/2026-04-14_API_응답_코드_컨벤션.md`, `Docs/skills/backend/api-response-error-convention.md`
- 장소/경로/공간 데이터 변경: `Docs/API/장소_도메인/`, `Docs/API/보행_네트워크_도메인/`, `Docs/ARD/ERD_v3.md`, `Docs/PoC/`
- 인프라/배포 변경: `Docs/인프라/`
- 문서 변경: `Docs/2026-04-07_docs_작성_규칙.md`

## 검증 단계

`/review`, `/qa`, `/security-review`, `/design-review`, `/ship`은 구현 결과를 아래 기준에 맞춰 확인한다.

- FE 검증은 `/fe-review`, `/fe-qa`, `/fe-design-review`, `/fe-ship`을 우선 사용한다.
- BE 검증은 `/be-review`, `/be-qa`, `/be-security-review`, `/be-ship`을 우선 사용한다.
- 제품/기능 적합성: `Docs/PRD/`, `Docs/기획/`
- API 계약: `Docs/API/`, `Docs/컨벤션/2026-04-14_API_응답_코드_컨벤션.md`
- 데이터 모델: `Docs/ARD/ERD_v3.md`
- 접근성/사용자 흐름: 화면명세서, MVP 화면명세서, 인터뷰 결과
- FE 화면 정합성: `FE/docs/2026-04-22_부산이음길_FE_화면_인벤토리_및_라우트_맵.md`, `FE/docs/2026-04-22_부산이음길_FE_디자인_컨벤션.md`, `FE/docs/2026-04-24_부산이음길_FE_접근성_정보_라벨_가이드.md`
- FE 시각 정합성은 route map의 보조 항목이 아니라 별도 확인 축이다. 화면 구조가 맞아도 디자인 컨벤션의 토큰, 컴포넌트 상태, CTA 위계, tab shell이 어긋나면 미완료로 본다.
- 백엔드 코드 품질: `Docs/skills/backend/`
- 인프라/배포 안정성: `Docs/인프라/`

## 우선순위 규칙

문서가 충돌하면 아래 순서로 판단한다.

1. 실제 코드와 라우트/패키지 구조로 확인한 현재 구현 사실
2. 최신 날짜의 1차 상세 계약 문서
3. Figma handoff, mockup, 도메인 상세 명세 같은 직접 산출물
4. 정리/검토/백로그 문서
5. PRD/기능명세/화면명세처럼 제품 배경 문서
6. `.ai/PROJECT.md`의 요약

FE 예외 해석:

- 화면 구조와 route 판단은 FE 화면 인벤토리/라우트 맵을 우선한다.
- 시각 규칙과 UI 위계 판단은 FE 디자인 컨벤션을 우선한다.
- 접근성 발화와 읽기 순서는 FE 접근성 라벨 가이드를 우선한다.
- 공통 조합과 구현 패턴은 FE 컴포넌트 가이드를 우선한다.

충돌이 구현에 영향을 주면 임의로 결정하지 말고 `.ai/LOCAL/PLANS/current-sprint.md`의 Open Questions에 남기거나, 필요한 경우 `.ai/DECISIONS/`에 ADR을 작성한다.
