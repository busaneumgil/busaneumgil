# S14P31E102-211 FE 공통 UI, 접근성 문안, Figma handoff

> **작성일** 2026-04-21  
> **작성자** Android FE  
> **최종 수정일** 2026-04-21

## 1. 문서 목적

이 문서는 `204~210` FE 작업에서 만든 지도, 시설 상세, 경로 설정, 내비게이션, 제보 작성 화면을 기준으로 공통 UI 기준과 handoff 확인 항목을 정리한다.

이번 범위는 기준 문서화다. Compose 디자인 시스템을 새로 만들거나 기존 feature 화면을 대규모로 공통화하지 않는다.

## 2. 연결된 구현 범위

| Jira | 범위 | 연결 기준 |
| --- | --- | --- |
| `S14P31E102-204` | 제보 폼 상태 모델 | 필드별 helper/error text, touched/dirty/error 상태 기준 |
| `S14P31E102-205` | 제보 작성 shell | 유형, 위치, 사진, 설명, 제출 순서 |
| `S14P31E102-206` | 제보 validation/state | submit 가능 조건과 오류 노출 시점 |
| `S14P31E102-209` | 지도 메인/시설 상세 mock | 검색 진입, 필터, 마커, 시설 상세, 길안내 진입 |
| `S14P31E102-210` | 경로 설정/내비게이션/제보 mock | 경로 옵션, route summary, navigation 상태 시안 |

## 3. 공통 UI 원칙

| 기준 | 적용 방식 | 현재 코드 기준 |
| --- | --- | --- |
| 화면 책임 분리 | `Route`는 ViewModel 연결과 event 처리, `Screen`은 `uiState`와 `onAction` 기반 UI만 담당한다. | `MapRoute`, `RouteSettingEntryRoute`, `NavigationRoute`, `ReportRoute` |
| 상태 기반 렌더링 | loading, empty, error, success는 화면에서 텍스트와 상태 카드로 구분한다. | `MapLocationStatus`, `RouteSettingUiState`, `NavigationScreenState`, `ReportScreenState` |
| 하단 주요 액션 | 화면의 핵심 CTA는 하단 또는 주요 흐름 말미에 고정한다. 비활성 사유는 supporting text로 설명한다. | `ReportSubmitBar`, `RouteSettingCtaUiState` |
| 입력 섹션 | label, 입력 영역, helper/error text를 함께 둔다. placeholder만 label 역할을 맡기지 않는다. | `ReportFormSection`, `OutlinedTextField(label, supportingText)` |
| 선택형 UI | 유형, 필터, 옵션은 radio/dropdown보다 chip, button group, card 선택을 우선한다. | `MapCategoryFilterBar`, `RouteOptionCard`, `ReportTypeSection` |
| 색상 의존 방지 | 오류와 제한 상태는 색상 변경만으로 표현하지 않고 설명 문구를 함께 둔다. | route summary error card, report helper/error text |
| 지도 overlay | 지도 위 정보는 검색 진입, 위치 상태, 필터, 상세 영역 순서로 역할을 분리한다. | `MapShellScaffold`, `MapTopSearchBar`, `MapViewport`, facility detail sheet |
| 코드 범위 | 공통화는 반복 구조가 명확할 때만 한다. feature별 문맥이 강한 UI는 private composable로 유지한다. | 현재 feature 내부 private composable 유지 |

## 4. 공통 컴포넌트 기준

| 컴포넌트/패턴 | 기준안 | 사용 화면 |
| --- | --- | --- |
| Top title row | 뒤로가기, 화면명, 보조 설명은 같은 위계를 유지한다. 화면명은 `titleLarge` 이상을 기본으로 한다. | 제보, 경로 설정, 내비게이션 |
| Bottom CTA | 버튼 label은 행동을 직접 표현한다. 비활성 상태는 supporting text로 이유를 말한다. | 제보 제출, 경로 안내 시작 |
| Status card | 상태명과 다음 행동을 함께 제공한다. loading/error/empty를 색상만으로 구분하지 않는다. | 지도 위치 상태, 경로 요약, 내비게이션 상태 |
| Filter/option chip | 선택 상태를 label과 selected state로 함께 전달한다. 전체 선택은 별도 chip으로 둔다. | 지도 시설 필터, 경로 옵션 |
| Detail bottom area | 제목, 카테고리, 주소, 접근성 요약, 주요 액션 순서로 배치한다. | 시설 상세 |
| Form section | 섹션 제목, 입력 영역, helper/error text를 한 묶음으로 유지한다. | 제보 작성 |
| Map placeholder | 실제 지도 SDK 전까지 지도 중심, 좌표, 선택 마커 상태를 텍스트로 노출한다. | 지도 메인, 경로 preview, 내비게이션 mock |

## 5. 화면별 UI 기준

| 화면 | 핵심 흐름 | 공통 UI 적용 기준 |
| --- | --- | --- |
| 지도 메인 | 지도, 검색, 필터, 마커, 시설 상세 | 검색 진입은 상단 overlay, 현재 위치 상태는 별도 card, 필터는 chip row, 상세는 하단 영역으로 분리한다. |
| 시설 상세 | 시설명, 카테고리, 주소, 접근성 요약, 길안내 | 상세 정보와 CTA를 같은 카드 안에 과하게 중첩하지 않는다. 길안내 진입은 primary action으로 둔다. |
| 경로 설정 | 출발지/도착지, 옵션, 요약, 안내 시작 | 목적지 없거나 경로 로딩 중이면 CTA를 비활성화하고 supporting text로 이유를 표시한다. |
| 내비게이션 | 상태, 지도/경로, 다음 안내, 주요 액션 | 준비 중, 안내 중, 재탐색, 완료, 실패 상태를 같은 화면 구조에서 표현한다. |
| 제보 작성 | 유형, 위치, 사진, 설명, 제출 | 입력 순서를 유지하고 각 섹션에 helper/error text 자리를 항상 확보한다. |
