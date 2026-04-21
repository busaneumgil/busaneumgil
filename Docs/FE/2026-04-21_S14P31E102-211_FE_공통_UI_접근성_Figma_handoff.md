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

## 6. 접근성 문안 원칙

| 기준 | 적용 규칙 | 예시 |
| --- | --- | --- |
| 버튼 | 버튼 label만으로 행동이 이해되면 별도 contentDescription을 반복하지 않는다. 아이콘 단독 버튼은 반드시 기능을 문장으로 제공한다. | `검색 화면 열기`, `현재 위치로 이동`, `시설 상세 닫기` |
| 선택 상태 | chip, option card, list item은 선택 여부를 TalkBack에서 알 수 있어야 한다. | `접근성 우선, 선택됨`, `전체, 선택되지 않음` |
| 입력 필드 | `label`은 항상 제공하고 placeholder는 예시 입력으로만 사용한다. | label: `주소`, placeholder: `예: 부산광역시 부산진구 중앙대로 인근` |
| 오류 | 오류는 색상과 문구를 함께 제공한다. 오류 문구는 사용자가 다음 행동을 알 수 있게 작성한다. | `제보 위치를 선택해주세요.`, `위치 좌표를 다시 확인해주세요.` |
| 상태 카드 | loading, empty, failure는 상태명과 해결 행동을 함께 말한다. | `경로 정보를 불러오지 못했습니다. 다시 시도해 주세요.` |
| 지도/마커 | 실제 지도 영역은 현재 중심, 선택 마커, fallback 여부를 문구로 전달한다. | `선택한 목적지로 지도 중심을 이동했습니다.` |
| 거리/좌표 | 좌표만 단독 노출하지 않고 의미를 붙인다. | `위도 35.1796, 경도 129.0756` |
| 공백/placeholder | mock 또는 준비 중 상태는 사용자에게 다음 단계가 막힌 것인지, 단순 placeholder인지 구분해 말한다. | `지도 SDK 연결 전 fallback surface입니다.` |

## 7. 화면별 접근성 문안 기준

| 화면 | 대상 | 기준 문안 | 구현 연결 |
| --- | --- | --- | --- |
| 지도 메인 | 검색 진입 영역 | `검색 화면 열기`, 목적지 선택 후에는 `선택한 목적지 {이름}, {주소}. 검색 수정` | `map_shell_search_a11y_label*` |
| 지도 메인 | 현재 위치 액션 | 권한 요청, 로딩, 재시도, 내 위치 이동, 사용 불가를 action label로 구분한다. | `MapRecenterButtonState` |
| 지도 메인 | 시설 필터 chip | `전체`, `{카테고리명}`과 선택 상태를 함께 전달한다. 결과 없음은 별도 텍스트로 표시한다. | `MapCategoryFilterBar` |
| 지도 메인 | 마커 | 마커명만 읽히는 상태에서, 후속 구현 시 카테고리와 선택 가능 상태를 함께 전달한다. | `MapViewport` marker semantics |
| 시설 상세 | 닫기 | `시설 상세 닫기`로 기능을 명확히 한다. 현재 `닫기` label은 화면상 텍스트로는 허용한다. | `FacilityDetailBottomSheetShell` |
| 시설 상세 | 길안내 | `이 시설로 길안내 시작`을 primary action으로 유지한다. | `map_facility_detail_route_entry_action` |
| 경로 설정 | 뒤로가기 | `뒤로가기`를 유지한다. 아이콘 전환 시에도 동일 문구를 contentDescription으로 사용한다. | `route_setting_back` |
| 경로 설정 | 경로 옵션 | 옵션명, 요약, 위험도, 선택 여부를 한 카드 안에서 읽을 수 있게 한다. | `RouteOptionCardUiState` |
| 경로 설정 | CTA | 활성 상태는 `선택한 경로로 안내 시작`, 비활성 상태는 supporting text로 사유를 제공한다. | `RouteSettingCtaUiState` |
| 내비게이션 | 상태 | `준비 중`, `안내 중`, `재탐색 필요`, `안내 완료`, `안내 실패`를 상태 문구로 노출한다. | `NavigationScreenState` |
| 내비게이션 | 음성 안내 | switch label은 `음성 안내`, 상태는 `켜짐` 또는 `꺼짐`을 함께 읽게 한다. | `NavigationUiAction.VoiceGuidanceToggled` |
| 내비게이션 | 재탐색 | `재탐색 mock`은 실제 연동 전 문구다. 실제 구현 시 `경로 다시 탐색`으로 교체한다. | `NavigationUiAction.RerouteClicked` |
| 제보 작성 | 제보 유형 | 유형 버튼은 `도로 공사`, `장애물`, `점자블록 손상`, `기타`와 선택 여부를 함께 전달한다. | `ReportTypeSection` |
| 제보 작성 | 위치 | 주소 label과 현재 위치 설정, 지도 선택 액션을 별도 버튼으로 유지한다. | `ReportLocationSection` |
| 제보 작성 | 사진 | 미첨부는 오류가 아니므로 `첨부된 사진 없음`과 `사진은 선택 사항입니다`를 함께 둔다. | `ReportPhotoSection` |
| 제보 작성 | 설명 | label은 `설명`, helper text는 글자 수와 선택 입력임을 함께 제공한다. | `ReportDescriptionSection` |

## 8. 오류/상태 문구 기준

| 유형 | 문구 기준 | 사용 예시 |
| --- | --- | --- |
| 필수값 누락 | 무엇을 해야 하는지 직접 말한다. | `제보 유형을 선택해주세요.`, `제보 위치를 선택해주세요.` |
| 좌표 오류 | 사용자가 다시 선택해야 함을 말한다. | `위치 좌표를 다시 확인해주세요.` |
| 권한 제한 | 제한되는 기능을 함께 말한다. | `현재 위치 표시와 길안내 시작 기능은 사용할 수 없습니다.` |
| 로딩 | 현재 진행 중인 작업을 말한다. | `시설 마커와 필터 상태를 준비하는 중입니다.` |
| empty | 데이터가 없는 이유와 다음 행동을 말한다. | `선택한 카테고리에 해당하는 마커가 없습니다.` |
| 실패 | 실패 사실과 재시도 가능성을 함께 말한다. | `경로 정보를 불러오지 못했습니다. 다시 시도해 주세요.` |
| 완료 | 완료된 결과를 짧게 말한다. | `제출 완료`, `안내 완료` |

## 9. TalkBack 점검 기준

| 점검 항목 | 통과 기준 |
| --- | --- |
| 읽기 순서 | 화면 상단 제목, 상태 설명, 주요 입력/정보, 하단 CTA 순서로 읽힌다. |
| 중복 낭독 | 버튼 label과 contentDescription이 같은 말을 반복하지 않는다. |
| 선택 상태 | chip, card, switch, radio 역할을 하는 요소는 선택 여부가 전달된다. |
| 오류 전달 | 오류는 시각적 색상 외에 텍스트로 노출된다. submit 실패 시 첫 오류 영역으로 이동할 수 있는 구조를 유지한다. |
| 터치 대상 | 주요 CTA와 icon action은 손가락 조작 가능한 충분한 영역을 가진다. |
| mock 문구 | `mock`, `placeholder`, `fixture` 문구는 개발 중 시안에서만 허용하고, 실제 배포 전 사용자 문구로 치환한다. |
