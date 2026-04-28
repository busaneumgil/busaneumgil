# FE/BE Lane Contract

이 레포는 하나의 저장소를 쓰지만 AI 작업은 FE lane과 BE lane을 명령어로 분리한다.

## 기본 규칙

- FE 작업은 `/fe-*` 스킬을 사용한다.
- BE 작업은 `/be-*` 스킬을 사용한다.
- FE와 BE가 동시에 바뀌는 작업만 기존 공통 스킬(`/plan`, `/start`, `/review` 등)을 사용한다.
- lane 전용 스킬은 해당 lane의 코드와 문서를 1차 수정 범위로 삼는다.
- lane 전용 스킬은 `.ai/DOCS.md`의 로컬 문서 제외 규칙을 따른다. `docs-context.sh stale`로 표시된 문서는 수정되거나 `clear`되기 전까지 해당 lane의 source of truth에서 제외한다.
- 반대 lane 수정이 필요하면 직접 섞지 말고 `.ai/LOCAL/PLANS/current-sprint.md`의 `## Cross-Lane Handoff`에 요청, 이유, 필요한 계약을 남긴다.

## FE Lane

FE lane의 1차 기준은 `FE/docs` 전체가 아니라, 명시적으로 지정한 FE 계약 문서와 실제 `FE/app` 코드다.

### 주요 코드 범위

- `FE/app/`
- `FE/docs/`
- `FE/mockup/`

### 주요 문서 범위

- `FE/docs/2026-04-22_부산이음길_FE_화면_인벤토리_및_라우트_맵.md`
- `FE/docs/2026-04-22_부산이음길_FE_디자인_컨벤션.md`
- `FE/docs/2026-04-24_부산이음길_FE_접근성_정보_라벨_가이드.md`
- `FE/docs/2026-04-13_부산이음길_FE_코드_컨벤션.md`
- `FE/docs/2026-04-13_부산이음길_FE_컴포넌트_가이드.md`
- `FE/docs/2026-04-21_S14P31E102-211_공통_UI_접근성_Figma_handoff.md`

### 2차 참고 문서

- `FE/docs/2026-04-27_로그인_필수_전환_FE_정합성_및_구현_영향.md`
- `FE/docs/2026-04-27_온보딩_디자인_화면_구성_분석.md`
- `FE/docs/2026-04-27_FE_Docs_문서_정합성_재검토.md`
- `FE/docs/2026-04-13_FE_프론트엔드_마스터_일정표.md`
- `FE/docs/sprint_backlog/`

### FE 문서 해석 규칙

- 1차 계약 문서는 화면명, IA, 사용자 노출 용어, 디자인 토큰, 접근성 라벨, 공통 구조 기준을 정의한다.
- 실제 `FE/app` 코드는 현재 구현 사실을 확인하는 기준이다.
- `FE/mockup/`과 Figma handoff는 1차 계약 문서가 모호할 때 직접 근거로 쓴다.
- 2차 참고 문서는 1차 계약 문서, 직접 디자인 산출물, 실제 코드를 정리하거나 실행 맥락을 기록한 문서다. 단독 source of truth로 쓰지 않는다.
- 1차 계약 문서를 바꾸지 않은 채 2차 참고 문서만 수정해서 기준을 바꾸지 않는다.

### 허용되는 BE 접점

- API 명세 확인
- 응답 필드, 상태 코드, 에러 코드 확인
- mock 또는 client contract 작성
- BE 변경 요청을 `Cross-Lane Handoff`에 기록

FE lane은 BE 구현 파일을 수정하지 않는다. API 계약 자체가 틀렸다면 `/be-plan` 또는 `/be-start`로 넘긴다.

### FE Design Contract

- `FE/docs/2026-04-22_부산이음길_FE_디자인_컨벤션.md`는 FE 시각 계약의 최상위 기준이다.
- 색상, 타이포, spacing, radius, elevation, CTA hierarchy, card/list/button state, 하단 탭 shell, 화면 밀도, 표현 톤은 디자인 컨벤션을 우선한다.
- `FE/docs/2026-04-22_부산이음길_FE_화면_인벤토리_및_라우트_맵.md`는 화면 구조와 route 계약, `FE/docs/2026-04-13_부산이음길_FE_컴포넌트_가이드.md`는 공통 UI 조합 규칙, `FE/docs/2026-04-24_부산이음길_FE_접근성_정보_라벨_가이드.md`는 접근성 발화/라벨 계약을 맡는다.
- 실제 `FE/app` 코드가 디자인 컨벤션보다 낡았더라도, 디자인 정합성 작업에서는 코드를 정답으로 승격하지 않는다. `현재 구현 사실`과 `목표 시각 계약`을 분리해서 기록한다.

## BE Lane

BE lane의 1차 기준은 `Docs/API`, `Docs/ARD`, `Docs/skills/backend`, 실제 `BE` 코드다.

### 주요 코드 범위

- `BE/`
- backend 실행, 테스트, 설정, migration, infra-adjacent 파일

### 주요 문서 범위

- `Docs/API/`
- `Docs/ARD/ERD_v3.md`
- `Docs/skills/backend/`
- `Docs/컨벤션/2026-04-14_API_응답_코드_컨벤션.md`
- `Docs/인프라/`

### 허용되는 FE 접점

- 화면에서 필요한 API 계약 확인
- FE route 또는 화면 인벤토리 확인
- 응답 shape 변경에 따른 FE 영향 기록
- FE 변경 요청을 `Cross-Lane Handoff`에 기록

BE lane은 FE 구현 파일을 수정하지 않는다. 화면 상태나 ViewModel 수정이 필요하면 `/fe-plan` 또는 `/fe-start`로 넘긴다.

## 문서 작성 분리

- FE 화면, 상태, 디자인, 접근성, 컴포넌트, FE 코드 규칙은 `FE/docs`에 작성한다.
- BE API, ERD, 응답/예외, backend 계층, 설정, 외부 연동, 인프라는 `Docs/API`, `Docs/ARD`, `Docs/skills/backend`, `Docs/인프라`에 작성한다.
- 공통 제품 의도는 `Docs/PRD`와 `Docs/기획`에 남기되, 구현 계약보다 우선하지 않는다.
- 문서 충돌은 계획 파일의 `문서 충돌` 항목에 남기고, 이번 작업에서 따를 임시 기준을 명시한다.

## 계획 파일 필수 항목

lane 전용 계획은 `.ai/LOCAL/PLANS/current-sprint.md`에 아래 항목을 포함한다.

- `## Work Lane`: `FE` 또는 `BE`
- `## Source Documents`: 이번 작업에서 실제로 읽은 문서
- `## Excluded Documents`: 이번 작업에서 의도적으로 제외한 오래된 문서와 이유
- `## Cross-Lane Handoff`: 반대 lane에 넘길 계약, 질문, 수정 요청
- `## 작업 체크리스트`: `/dashboard`가 읽을 체크리스트
