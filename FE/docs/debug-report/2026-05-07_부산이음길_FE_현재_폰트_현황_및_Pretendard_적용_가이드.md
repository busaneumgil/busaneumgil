# 2026-05-07_부산이음길_FE_현재_폰트_현황_및_Pretendard_적용_가이드

## 1. 문서 목적

- 이 문서는 부산이음길 FE 코드와 FE 문서를 기준으로 현재 구현된 화면별 폰트 크기와 폰트 굵기 사용 현황을 정리한다.
- 이 문서는 `current implementation fact`와 `목표 적용 guideline`을 명시적으로 분리한다.
- 일반 사용자(보행약자) 대상 일반 모드에 Pretendard를 적용할 때 필요한 전역 토큰 정리, 화면군별 적용 기준, 예외 분기 기준을 FE 구현 관점에서 제안한다.
- 저시력/시각보조 전용 화면은 일반 모드와 분리해서 다룬다.

## 2. 분석 범위

- 코드 범위
  - `FE/app/src/main/java/com/ssafy/e102/eumgil/core/designsystem/theme/Type.kt`
  - `FE/app/src/main/java/com/ssafy/e102/eumgil/core/designsystem/theme/Theme.kt`
  - `FE/app/src/main/java/com/ssafy/e102/eumgil/app/MainActivity.kt`
  - `FE/app/src/main/java/com/ssafy/e102/eumgil/app/navigation/AppStartDestination.kt`
  - `FE/app/src/main/java/com/ssafy/e102/eumgil/app/navigation/MainNavGraph.kt`
  - `FE/app/src/main/java/com/ssafy/e102/eumgil/app/navigation/LowVisionNavGraph.kt`
  - `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/**`
  - `FE/app/src/main/java/com/ssafy/e102/eumgil/core/designsystem/component/**`
- 문서 범위
  - `FE/docs/2026-04-13_부산이음길_FE_컴포넌트_가이드.md`
  - `FE/docs/2026-04-22_부산이음길_FE_디자인_컨벤션.md`
  - `FE/docs/2026-05-04_부산이음길_FE_일반모드_UI_라운딩_정합성_정리.md`
- 정리 방식
  - 동일 화면 안에서 반복되는 리스트/버튼/카드 텍스트는 대표 요소 기준으로 묶어 기록했다.
  - `MaterialTheme.typography.*` 직접 사용, `copy(fontSize = ...)` override, raw `fontSize`/`fontWeight` 사용, 암묵적 기본 스타일 사용을 구분했다.
  - `Type.kt`에 정의되지 않은 role은 코드상 `MaterialTheme.typography.*` 호출만 존재하므로, 최종 해석값은 Compose Material 3 기본 scale 기준으로 해석했다.

## 3. 기준 문서 및 기준 코드

| 구분 | 경로 | 확인 목적 |
| --- | --- | --- |
| 기준 코드 | `FE/app/src/main/java/com/ssafy/e102/eumgil/core/designsystem/theme/Type.kt` | 전역 typography role, font family, size, line height, weight 확인 |
| 기준 코드 | `FE/app/src/main/java/com/ssafy/e102/eumgil/core/designsystem/theme/Theme.kt` | 전역 `MaterialTheme.typography` 적용 방식 확인 |
| 기준 코드 | `FE/app/src/main/java/com/ssafy/e102/eumgil/app/MainActivity.kt` | 앱 전체 theme wrapping 여부 확인 |
| 기준 코드 | `FE/app/src/main/java/com/ssafy/e102/eumgil/app/navigation/AppStartDestination.kt` | 일반 모드 / 저시력 시작 분기 확인 |
| 기준 코드 | `FE/app/src/main/java/com/ssafy/e102/eumgil/app/navigation/LowVisionNavGraph.kt` | 저시력 전용 화면군 범위 확인 |
| 기준 코드 | `FE/app/src/main/java/com/ssafy/e102/eumgil/app/navigation/OnboardingNavGraph.kt` | `TermsGuideScreen` 진입 경로 확인 |
| 기준 문서 | `FE/docs/2026-04-13_부산이음길_FE_컴포넌트_가이드.md` | Pretendard 선언, 본문 최소 크기, 토큰 초안 확인 |
| 기준 문서 | `FE/docs/2026-04-22_부산이음길_FE_디자인_컨벤션.md` | H1/H2/H3, Body, Caption 기준 확인 |
| 기준 문서 | `FE/docs/2026-05-04_부산이음길_FE_일반모드_UI_라운딩_정합성_정리.md` | 일반 모드와 저시력/고대비 화면 분리 원칙 확인 |
| 보조 기준 | Android Developers Material 3 typography default scale | `Type.kt` 미정의 role의 최종 해석값 확인 |

## 4. 현재 전역 Typography 토큰 현황

### 4.1 current implementation fact

#### 4.1.1 전역 font family

| 항목 | 현재 구현 값 | 근거 |
| --- | --- | --- |
| 전역 font family 이름 | `BusanEumgilFontFamily` | `FE/app/src/main/java/com/ssafy/e102/eumgil/core/designsystem/theme/Type.kt` |
| Normal | `R.font.koddi_ud_on_gothic_regular` | 동일 |
| Medium | `R.font.koddi_ud_on_gothic_regular` | 동일 |
| SemiBold | `R.font.koddi_ud_on_gothic_bold` | 동일 |
| Bold | `R.font.koddi_ud_on_gothic_bold` | 동일 |
| ExtraBold | `R.font.koddi_ud_on_gothic_extra_bold` | 동일 |

#### 4.1.2 전역 custom typography role

| role name | font family | font size | line height | font weight | 현재 전역 적용 범위 |
| --- | --- | --- | --- | --- | --- |
| `displayLarge` | `BusanEumgilFontFamily` | `32.sp` | `40.sp` | `FontWeight.SemiBold` | `MainActivity.kt`의 `BusanEumgilTheme`를 통해 일반 모드/저시력 모드 공통 적용 |
| `headlineMedium` | `BusanEumgilFontFamily` | `28.sp` | `36.sp` | `FontWeight.SemiBold` | 동일 |
| `titleLarge` | `BusanEumgilFontFamily` | `22.sp` | `30.sp` | `FontWeight.SemiBold` | 동일 |
| `titleMedium` | `BusanEumgilFontFamily` | `18.sp` | `26.sp` | `FontWeight.Medium` | 동일 |
| `bodyLarge` | `BusanEumgilFontFamily` | `16.sp` | `24.sp` | `FontWeight.Normal` | 동일 |
| `bodyMedium` | `BusanEumgilFontFamily` | `14.sp` | `22.sp` | `FontWeight.Normal` | 동일 |
| `labelLarge` | `BusanEumgilFontFamily` | `14.sp` | `20.sp` | `FontWeight.Medium` | 동일 |

#### 4.1.3 코드에서 사용되지만 `Type.kt`에 정의되지 않은 role

| role name | 최종 해석 scale | 최종 해석 weight | 현재 사용 예시 |
| --- | --- | --- | --- |
| `headlineLarge` | `32.sp / 40.sp` | Material 3 기본 `Regular(400)` | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/routesetting/RouteSettingScreen.kt` |
| `headlineSmall` | `24.sp / 32.sp` | Material 3 기본 `Regular(400)` | `SearchScreen.kt`, `NavigationStepCard.kt`, `ArrivalScreen.kt`, `MyPageAppInfoScreen.kt` |
| `titleSmall` | `14.sp / 20.sp` | Material 3 기본 `Medium(500)` | `MapCategoryFilterBar.kt`, `SavedRouteScreen.kt`, `ReportScreen.kt`, `MyPageScreen.kt` |
| `bodySmall` | `12.sp / 16.sp` | Material 3 기본 `Regular(400)` | `MapScreen.kt`, `RouteSettingScreen.kt`, `ReportScreen.kt`, `MyPageReportHistoryScreen.kt` |
| `labelMedium` | `12.sp / 16.sp` | Material 3 기본 `Medium(500)` | `SearchScreen.kt`, `SavedRouteScreen.kt`, `RouteSettingScreen.kt`, `ArrivalScreen.kt`, `ReportScreen.kt` |
| `labelSmall` | `11.sp / 16.sp` | Material 3 기본 `Medium(500)` | `RecentDestinationBottomSheetShell.kt`, `SavedRouteScreen.kt`, `RouteSettingScreen.kt`, `ReportScreen.kt` |

### 4.2 current implementation fact 정리

| 항목 | 현재 상태 |
| --- | --- |
| theme 분기 | `Theme.kt`에는 일반 모드용 typography와 저시력 전용 typography 분기가 없다. |
| 전역 적용 방식 | `MainActivity.kt`에서 앱 전체를 `BusanEumgilTheme`로 감싼다. |
| 저시력 화면 영향 | 저시력 화면도 동일한 전역 font family를 상속한다. 저시력 화면이 raw `fontSize`를 쓰더라도 `fontFamily`를 별도 지정하지 않았기 때문에 전역 font family 변경 시 함께 영향받는다. |

## 5. 화면별 현재 폰트 적용 현황

### 5.1 current implementation fact 기록 원칙

- 아래 표는 화면별 대표 텍스트를 기준으로 정리했다.
- `공통 token 사용 여부`는 `예`, `부분`, `아니오`, `추가 확인 필요`로 표기했다.
- `최종 해석된 폰트 크기`는 line height를 함께 병기했다.

### 5.2 인증/로그인

| 화면명 | 파일 경로 | 텍스트가 쓰인 UI 위치 | 현재 적용 방식 | 최종 해석된 폰트 크기 | 최종 해석된 폰트 굵기 | 공통 token 사용 여부 | 모드 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 로그인 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/auth/AuthScreen.kt` | 히어로 서비스명 | `MaterialTheme.typography.headlineMedium.copy(fontSize = 36.sp, lineHeight = 44.sp)` + `FontWeight.ExtraBold` | `36.sp / 44.sp` | `ExtraBold` | 부분 | 일반 모드 | 전역 role를 쓰지만 size/weight를 override한다. |
| 로그인 | 동일 | 히어로 영문 부제 | `MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, lineHeight = 28.sp)` + `FontWeight.SemiBold` | `20.sp / 28.sp` | `SemiBold` | 부분 | 일반 모드 | `titleMedium` 기본값 `18/26`에서 확대했다. |
| 로그인 | 동일 | 브랜드 태그라인 | `MaterialTheme.typography.titleLarge` + `FontWeight.SemiBold` | `22.sp / 30.sp` | `SemiBold` | 예 | 일반 모드 | 전역 토큰과 동일하다. |
| 로그인 | 동일 | 소셜 로그인 버튼 라벨 | `MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp)` + `FontWeight.Bold` | `17.sp / 20.sp` | `Bold` | 부분 | 일반 모드 | 버튼 라벨이 토큰보다 커진다. |
| 로그인/Auth Gate | 동일 | 설명/상태 문구 | `MaterialTheme.typography.bodyMedium`, `bodyLarge`, `headlineMedium`, `labelLarge` | `14.sp / 22.sp`, `16.sp / 24.sp`, `28.sp / 36.sp`, `14.sp / 20.sp` | 각 role 기본 weight 또는 override weight | 예 | 일반 모드 | gate 화면은 전역 role 사용 비중이 높다. |

### 5.3 온보딩

| 화면명 | 파일 경로 | 텍스트가 쓰인 UI 위치 | 현재 적용 방식 | 최종 해석된 폰트 크기 | 최종 해석된 폰트 굵기 | 공통 token 사용 여부 | 모드 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 사용자 유형 선택 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/onboarding/OnboardingScreen.kt` | 1차 사용자 유형 카드 제목 | `MaterialTheme.typography.headlineMedium.copy(fontSize = 38.sp, lineHeight = 42.sp)` + `FontWeight.Black` | `38.sp / 42.sp` | `Black` | 부분 | 일반 모드 | 전역 role를 크게 override한다. |
| 사용자 유형 선택 | 동일 | 1차 사용자 유형 카드 설명 | `MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp)` | `18.sp / 26.sp` | `Normal` | 부분 | 일반 모드 | 본문이 전역 `bodyLarge`보다 크다. |
| 사용자 유형 선택 | 동일 | 이동 유형 세부 카드 제목 | `MaterialTheme.typography.titleMedium.copy(fontSize = 26.sp, lineHeight = 32.sp)` + `FontWeight.Black` | `26.sp / 32.sp` | `Black` | 부분 | 일반 모드 | raw heading 수준까지 키웠다. |
| 온보딩 공통 scaffold | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/onboarding/component/OnboardingStepScaffold.kt` | 중앙 헤더 제목 | `MaterialTheme.typography.displayLarge` + `FontWeight.SemiBold` | `32.sp / 40.sp` | `SemiBold` | 예 | 일반 모드 | 현재 전역 토큰과 일치한다. |
| 위치 약관 동의 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/onboarding/OnboardingScreen.kt` | 전체 동의 row 라벨 | `MaterialTheme.typography.bodyLarge` + `FontWeight.SemiBold` | `16.sp / 24.sp` | `SemiBold` | 부분 | 일반 모드 | role은 body지만 weight만 강조한다. |

### 5.4 지도 메인

| 화면명 | 파일 경로 | 텍스트가 쓰인 UI 위치 | 현재 적용 방식 | 최종 해석된 폰트 크기 | 최종 해석된 폰트 굵기 | 공통 token 사용 여부 | 모드 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 지도 메인 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/map/MapScreen.kt` | 위치 상태 카드 제목/설명 | `titleMedium`, `bodyLarge`, `bodyMedium`, `labelLarge` | `18/26`, `16/24`, `14/22`, `14/20` | 각 role 기본 weight | 예 | 일반 모드 | role 조합 자체는 안정적이다. |
| 지도 검색바 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapTopSearchBar.kt` | 검색바 대표 텍스트/보조 문구 | `bodyLarge`, `bodySmall` | `16.sp / 24.sp`, `12.sp / 16.sp` | `Normal` | 부분 | 일반 모드 | `bodySmall`은 전역 미정의 role이다. |
| 지도 카테고리 필터 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapCategoryFilterBar.kt` | 섹션 제목/선택 요약 | `titleSmall`, `labelLarge`, `bodyMedium` | `14.sp / 20.sp`, `14.sp / 20.sp`, `14.sp / 22.sp` | `Medium(500)`, `Medium(500)`, `Normal` | 부분 | 일반 모드 | `titleSmall`이 전역 미정의 role이다. |
| 시설 상세 바텀시트 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/map/component/FacilityDetailBottomSheetShell.kt` | 시설명/메타/address | `titleLarge`, `bodySmall`, `bodyMedium` | `22.sp / 30.sp`, `12.sp / 16.sp`, `14.sp / 22.sp` | `SemiBold`, `Regular`, `Regular` | 부분 | 일반 모드 | 타이틀만 custom token, 메타는 default role 섞임. |
| 최근 목적지 바텀시트 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/map/component/RecentDestinationBottomSheetShell.kt` | 시트 제목/행 제목/주소/태그 | `titleLarge`, `titleSmall`, `bodySmall`, `labelLarge`, `labelSmall` | `22/30`, `14/20`, `12/16`, `14/20`, `11/16` | `SemiBold`, `Medium`, `Regular`, `Medium`, `Medium` | 부분 | 일반 모드 | `titleSmall`, `labelSmall`이 미정의 role이다. |
| 지도 fallback/overlay | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapViewport.kt` | fallback 제목/설명/상태 배지 | `titleLarge`, `bodyLarge`, `bodyMedium`, `labelLarge` | `22/30`, `16/24`, `14/22`, `14/20` | role 기본 weight | 예 | 공통 | 일반/저시력 공용 인프라성 텍스트다. |
| 장소 목록 공통 카드 | `FE/app/src/main/java/com/ssafy/e102/eumgil/core/designsystem/component/place/PlaceListCard.kt` | 순번 배지/장소명/주소/액션 버튼/하단 탭 | raw `42.sp Black`, raw `44.sp Black`, raw `26.sp Bold`, raw `36.sp Black`, raw `10.sp Normal/Bold` | `42/48`, `44/52`, `26/32`, `36/42`, `10/기본` | `Black`, `Black`, `Bold`, `Black`, `Normal/Bold` | 아니오 | 추가 확인 필요 | 일반 모드 토큰 체계와 동떨어진 특수 컴포넌트다. 저시력 계열 소속 여부 재분류가 필요하다. |

### 5.5 검색/검색 결과

| 화면명 | 파일 경로 | 텍스트가 쓰인 UI 위치 | 현재 적용 방식 | 최종 해석된 폰트 크기 | 최종 해석된 폰트 굵기 | 공통 token 사용 여부 | 모드 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 검색 진입/음성 검색 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/search/SearchScreen.kt` | 진입 headline | `MaterialTheme.typography.headlineSmall` | `24.sp / 32.sp` | `Regular(400)` | 부분 | 일반 모드 | `headlineSmall`이 미정의 role이다. |
| 검색 결과 | 동일 | 결과 요약/상태 카드 제목 | `titleMedium` | `18.sp / 26.sp` | `Medium(500)` | 예 | 일반 모드 | 현재 전역 token 사용이다. |
| 검색 결과 | 동일 | 최근 검색어/결과 부제 | `bodyLarge`, `bodyMedium` | `16.sp / 24.sp`, `14.sp / 22.sp` | `Regular` | 예 | 일반 모드 | body 계열은 전역 token과 일치한다. |
| 검색 결과 | 동일 | 메타 정보(place id, 보조 라벨) | `labelMedium` | `12.sp / 16.sp` | `Medium(500)` | 부분 | 일반 모드 | 문서 기준의 `label/md 12/18`과 다르다. |
| 검색 입력/placeholder | 동일 | `OutlinedTextField` placeholder 및 일부 기본 Text | 명시적 style 없음 | 추가 확인 필요 | 추가 확인 필요 | 추가 확인 필요 | 일반 모드 | `LocalTextStyle`과 Material component default merge 확인이 필요하다. |

### 5.6 경로 설정/경로 상세

| 화면명 | 파일 경로 | 텍스트가 쓰인 UI 위치 | 현재 적용 방식 | 최종 해석된 폰트 크기 | 최종 해석된 폰트 굵기 | 공통 token 사용 여부 | 모드 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 경로 설정 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/routesetting/RouteSettingScreen.kt` | 요약 metric 카드 숫자 | `MaterialTheme.typography.headlineLarge` + `FontWeight.ExtraBold` | `32.sp / 40.sp` | `ExtraBold` | 부분 | 일반 모드 | size는 default role, weight는 강하게 override한다. |
| 경로 설정 | 동일 | 섹션 제목 | `titleMedium` + `FontWeight.SemiBold` | `18.sp / 26.sp` | `SemiBold` | 부분 | 일반 모드 | 전역 token의 기본 `Medium`보다 굵다. |
| 경로 설정 | 동일 | 경유지/상세 row 제목 | `titleSmall` + `FontWeight.SemiBold` | `14.sp / 20.sp` | `SemiBold` | 부분 | 일반 모드 | 전역 미정의 role 사용 빈도가 높다. |
| 경로 설정 | 동일 | 보조 메타/설명 | `bodySmall`, `labelSmall`, `labelMedium` | `12/16`, `11/16`, `12/16` | `Regular`, `Medium`, `Medium` | 부분 | 일반 모드 | 세부 정보가 대부분 default role에 의존한다. |
| 경로 설정 | 동일 | 옵션 카드 예상 시간 | `titleLarge` + `FontWeight.ExtraBold` | `22.sp / 30.sp` | `ExtraBold` | 부분 | 일반 모드 | 숫자 강조용 별도 token 부재가 드러난다. |

### 5.7 길안내/도착

| 화면명 | 파일 경로 | 텍스트가 쓰인 UI 위치 | 현재 적용 방식 | 최종 해석된 폰트 크기 | 최종 해석된 폰트 굵기 | 공통 token 사용 여부 | 모드 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 길안내 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/navigation/NavigationScreen.kt` | 길안내 hero action 라벨 | `titleLarge` + `FontWeight.Bold` | `22.sp / 30.sp` | `Bold` | 부분 | 일반 모드 | title 계열로 행동을 강조한다. |
| 길안내 step 카드 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/navigation/component/NavigationStepCard.kt` | 메인 방향 지시문 | `headlineSmall` + `FontWeight.SemiBold` | `24.sp / 32.sp` | `SemiBold` | 부분 | 일반 모드 | direction headline이 전역 미정의 role이다. |
| 길안내 step 카드 | 동일 | 거리/시간 메타 값 | `titleSmall` + `FontWeight.SemiBold`, `labelMedium` | `14/20`, `12/16` | `SemiBold`, `Medium` | 부분 | 일반 모드 | 메타용 role이 일관되지 않다. |
| 도착 화면 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/arrival/ArrivalScreen.kt` | 도착 headline/설명 | `headlineSmall` + `FontWeight.SemiBold`, `bodyLarge` | `24/32`, `16/24` | `SemiBold`, `Regular` | 부분 | 일반 모드 | 도착 핵심 메시지도 default role에 의존한다. |
| 도착 평가/저장 dialog | 동일 | 시트 제목/질문/저장 메타 | `titleMedium` + `FontWeight.SemiBold`, `bodyLarge` + `FontWeight.SemiBold`, `labelMedium` | `18/26`, `16/24`, `12/16` | `SemiBold`, `SemiBold`, `Medium` | 부분 | 일반 모드 | labelMedium default 의존이 있다. |

### 5.8 저장 경로/북마크

| 화면명 | 파일 경로 | 텍스트가 쓰인 UI 위치 | 현재 적용 방식 | 최종 해석된 폰트 크기 | 최종 해석된 폰트 굵기 | 공통 token 사용 여부 | 모드 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 저장 경로 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/savedroute/SavedRouteScreen.kt` | 화면 상단 타이틀 | `titleLarge` + `FontWeight.SemiBold` | `22.sp / 30.sp` | `SemiBold` | 예 | 일반 모드 | 현재 토큰과 일치한다. |
| 저장 경로 | 동일 | 탭 라벨/행 액션 | `labelLarge` | `14.sp / 20.sp` | `Medium(500)` | 예 | 일반 모드 | labelLarge 사용은 비교적 안정적이다. |
| 저장 경로 | 동일 | 저장 장소명/경로명 | `titleMedium` | `18.sp / 26.sp` | `Medium(500)` | 예 | 일반 모드 | 리스트 제목은 titleMedium 중심이다. |
| 저장 경로 | 동일 | 주소/요약/옵션 메타 | `bodyMedium`, `bodySmall`, `labelMedium`, `labelSmall` | `14/22`, `12/16`, `12/16`, `11/16` | `Regular`, `Regular`, `Medium`, `Medium` | 부분 | 일반 모드 | 작은 메타가 모두 default role에 의존한다. |

### 5.9 제보

| 화면명 | 파일 경로 | 텍스트가 쓰인 UI 위치 | 현재 적용 방식 | 최종 해석된 폰트 크기 | 최종 해석된 폰트 굵기 | 공통 token 사용 여부 | 모드 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 제보 draft 배너 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/report/ReportScreen.kt` | 임시 저장 배너 제목 | `titleSmall` + `FontWeight.SemiBold` | `14.sp / 20.sp` | `SemiBold` | 부분 | 일반 모드 | 작지만 강조가 필요한 텍스트가 `titleSmall`에 몰린다. |
| 제보 유형 선택 | 동일 | 단계 대표 질문/보조 문구 | `titleLarge` + `FontWeight.SemiBold`, `bodyLarge` | `22/30`, `16/24` | `SemiBold`, `Regular` | 예 | 일반 모드 | 메인 헤더는 전역 token 사용이다. |
| 제보 유형 카드 | 동일 | 유형명/설명 | `titleSmall` + `FontWeight.SemiBold`, `bodySmall` | `14/20`, `12/16` | `SemiBold`, `Regular` | 부분 | 일반 모드 | 카드 내부 텍스트가 default role 기반이다. |
| 위치 카드 | 동일 | 선택 위치 라벨/주소/좌표 | `labelLarge`, `bodyLarge` + `FontWeight.SemiBold`, `bodySmall` | `14/20`, `16/24`, `12/16` | `Medium`, `SemiBold`, `Regular` | 부분 | 일반 모드 | 주소를 본문보다 무겁게 처리한다. |
| 사진 섹션 | 동일 | 섹션 제목/카운트/helper | `titleMedium` + `FontWeight.SemiBold`, `labelMedium`, `bodyMedium` | `18/26`, `12/16`, `14/22` | `SemiBold`, `Medium`, `Regular` | 부분 | 일반 모드 | 카운트가 `labelMedium` default에 의존한다. |
| 상세 설명 섹션 | 동일 | 섹션 제목/helper/글자수 | `titleMedium` + `FontWeight.SemiBold`, `bodyMedium`, `labelMedium` | `18/26`, `14/22`, `12/16` | `SemiBold`, `Regular`, `Medium` | 부분 | 일반 모드 | 입력 폼의 보조 정보 규칙이 고정돼 있지 않다. |

### 5.10 마이페이지

| 화면명 | 파일 경로 | 텍스트가 쓰인 UI 위치 | 현재 적용 방식 | 최종 해석된 폰트 크기 | 최종 해석된 폰트 굵기 | 공통 token 사용 여부 | 모드 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 마이페이지 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/mypage/MyPageScreen.kt` | 로그아웃/주요 section 제목 | `titleMedium` + `FontWeight.Bold` | `18.sp / 26.sp` | `Bold` | 부분 | 일반 모드 | section title이 전역 기본보다 무겁다. |
| 마이페이지 hero | 동일 | 사용자 유형 headline/subtype | `titleMedium` + `FontWeight.Bold`, `bodySmall` | `18/26`, `12/16` | `Bold`, `Regular` | 부분 | 일반 모드 | subtype은 default `bodySmall` 사용이다. |
| 마이페이지 메뉴 row | 동일 | 메뉴명 | `bodyLarge` + `FontWeight.SemiBold` | `16.sp / 24.sp` | `SemiBold` | 부분 | 일반 모드 | 본문을 사실상 버튼 라벨처럼 쓰고 있다. |
| 앱 정보 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/mypage/MyPageAppInfoScreen.kt` | 앱명/버전/설명 | `headlineSmall` + `FontWeight.Bold`, `titleSmall`, `bodyMedium` | `24/32`, `14/20`, `14/22` | `Bold`, `Medium`, `Regular` | 부분 | 일반 모드 | 미정의 role 의존이 있다. |
| 제보 이력 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/mypage/MyPageReportHistoryScreen.kt` | 카드 제목/주소/상태 문구 | `titleMedium` + `FontWeight.SemiBold`, `bodyMedium`, `bodySmall` | `18/26`, `14/22`, `12/16` | `SemiBold`, `Regular`, `Regular` | 부분 | 일반 모드 | 상태 카드 CTA도 `titleSmall`을 사용한다. |

### 5.11 저시력/시각보조 전용 화면

| 화면명 | 파일 경로 | 텍스트가 쓰인 UI 위치 | 현재 적용 방식 | 최종 해석된 폰트 크기 | 최종 해석된 폰트 굵기 | 공통 token 사용 여부 | 모드 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 저시력 공통 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/lowvision/LowVisionScreenDefaults.kt` | 기본 헤더 | raw `40.sp`, `48.sp` lineHeight | `40.sp / 48.sp` | 코드상 각 화면에서 주로 `Black` 또는 `ExtraBold` | 아니오 | 저시력 모드 | typography token이 아니라 layout default 상수다. |
| 저시력 홈 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/lowvision/LowVisionHomeScreen.kt` | 메인 액션 카드 라벨 | raw `48.sp` + `FontWeight.Black` | `48.sp / 추가 확인 필요` | `Black` | 아니오 | 저시력 모드 | 일반 모드 token 체계와 완전히 분리되어 있다. |
| 저시력 카테고리 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/lowvision/LowVisionCategoryScreen.kt` | 헤더/카드 라벨 | raw `52.sp / 60.sp`, raw card label | `52.sp / 60.sp`, `38.sp / 46.sp` | `Black` | 아니오 | 저시력 모드 | 고대비 대형 글자 체계다. |
| 저시력 검색 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/lowvision/LowVisionSearchScreen.kt` | 헤더/결과 카드 제목/주소/버튼 | raw `40`, `36`, `30`, `20`, `28`, `24.sp` | 각 구간 raw 값 | `ExtraBold`, `Black`, `Bold` 혼용 | 아니오 | 저시력 모드 | 결과 카드 전체가 raw scale 기반이다. |
| 저시력 길안내 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/lowvision/LowVisionNavigationScreen.kt` | metric label/number/unit, 현재 위치, 종료 액션 | raw `28/34`, `76/84`, `34/40`, `44/50`, `60/68` | 각 구간 raw 값 | `ExtraBold` 또는 `Black` | 아니오 | 저시력 모드 | 거리/시간 숫자 강조 체계가 일반 모드와 별개다. |
| 저시력 경로 브리핑 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/lowvision/LowVisionRouteBriefingScreen.kt` | 헤더/안내 제목/강조 숫자 | raw `56`, `31`, `38`, `82`, `72.sp` | 각 구간 raw 값 | 대부분 `Black` | 아니오 | 저시력 모드 | 가장 강한 oversized text를 사용한다. |
| 저시력 북마크 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/lowvision/LowVisionBookmarkScreen.kt` | 헤더/장소명/주소/버튼 | raw `40`, `30`, `22`, `28.sp` | 각 구간 raw 값 | `Black`, `Bold` | 아니오 | 저시력 모드 | 저장 경로 일반 모드와 별도 스타일 체계다. |
| 저시력 마이페이지 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/lowvision/LowVisionMyPageScreen.kt` | 헤더/메뉴명/설명 | raw `40`, `30`, `22.sp` | 각 구간 raw 값 | `Black`, `Bold` | 아니오 | 저시력 모드 | 일반 모드 MyPage와 공용 token이 없다. |
| 저시력 도착 완료 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/lowvision/LowVisionNavigationCompleteScreen.kt` | 완료 제목/CTA | raw `52.sp / 60.sp`, raw `54.sp / 62.sp` | 각 구간 raw 값 | `Black` | 아니오 | 저시력 모드 | 화면 전반이 large-type 전용이다. |
| 저시력 음성 입력 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/lowvision/LowVisionVoiceInputScreen.kt` | 화면 대표 문구 | raw `44.sp` + `FontWeight.Black` | `44.sp / 추가 확인 필요` | `Black` | 아니오 | 저시력 모드 | 안내/대기 텍스트가 모두 oversized다. |
| 약관 고대비 가이드 후보 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/terms/TermsGuideScreen.kt` | 헤더/메인 카드/힌트/더보기 버튼 | raw `24`, `48`, `88`, `28`, `34.sp` + `ExtraBold/Black` | `24`, `48`, `88`, `28`, `34.sp` | `ExtraBold`, `Black` | 아니오 | 저시력 후보 | `OnboardingNavGraph`에 연결되어 있지만 화면 언어와 수치가 저시력/고대비 계열이다. 일반 모드 소속 여부 추가 확인이 필요하다. |

## 6. 현재 구현 문제점 및 불일치

### 6.1 current implementation fact

| 문제 구분 | 현재 구현 fact | 영향 |
| --- | --- | --- |
| 전역 font family와 문서 불일치 | 코드 전역 font family는 `Koddi UD On Gothic`, FE 문서는 `Pretendard`를 기본 폰트로 규정한다. | 문서 기준으로 Pretendard를 전제로 논의해도 현재 코드에는 그대로 반영되지 않는다. |
| 일반/저시력 theme 미분리 | `Theme.kt`에 단일 typography만 존재한다. 저시력 화면도 같은 font family를 상속한다. | Pretendard를 전역 일괄 교체하면 저시력/시각보조 화면까지 동시에 바뀐다. |
| 미정의 role fallback | `headlineLarge`, `headlineSmall`, `titleSmall`, `bodySmall`, `labelMedium`, `labelSmall`를 많이 쓰지만 `Type.kt`에 정의가 없다. | 현재 화면마다 일부는 custom token, 일부는 Material 3 default scale에 의존한다. |
| raw override 다발 | 로그인, 온보딩, 저시력 화면, `PlaceListCard`, `TermsGuideScreen`이 raw `fontSize`/`fontWeight`를 직접 사용한다. | 토큰 단위 일괄 조정이 어렵고 Pretendard 교체 시 크기/weight 조합 검증 비용이 커진다. |
| 문서 간 line height 불일치 | 컴포넌트 가이드는 `body/md 14/22`, 디자인 컨벤션은 `Body 2 14/20`을 제시한다. | 일반 모드 보조 본문과 메타 텍스트 기준이 하나로 확정돼 있지 않다. |
| `label/md` 부재 | 문서는 `label/md 12/18` 또는 `Caption 12/18`를 가정하지만 코드 custom token에는 정의가 없다. | 실제 코드의 `labelMedium`은 Material 3 default `12/16`으로 동작한다. |
| 수치 강조 token 부재 | 경로 설정/길안내/저시력 화면에서 숫자 강조를 `ExtraBold` override 또는 raw 60~82sp로 처리한다. | 거리/시간/순번용 token 체계를 분리하지 않으면 화면마다 강조 기준이 흔들린다. |
| 암묵적 기본 스타일 존재 | 일부 `Button` 내부 `Text`, `OutlinedTextField` placeholder 등은 명시적 style이 없다. | 정확한 최종 해석값 확인과 토큰 치환 대상 식별에 추가 점검이 필요하다. |

### 6.2 문서 기준과 코드 기준 비교

| 항목 | FE 문서 기준 | 코드 기준 | 판단 |
| --- | --- | --- | --- |
| 기본 font family | `Pretendard` | `Koddi UD On Gothic` | 불일치 |
| 본문 최소 기준 | 일반 모드 `16sp`, 시각보조 `18sp` | 일반 모드 일부 `12sp`, `14sp` 사용 | 일반 모드 일부 불일치 |
| `title/md` weight | 문서상 H3/중간 제목은 600 계열 의미 | 코드 `titleMedium`은 `FontWeight.Medium(500)` 정의 후 화면별로 Bold/SemiBold override | 불안정 |
| `body/md` | 문서 간 `14/22` vs `14/20` 충돌 | 코드 `14/22` | 문서 기준 정합 필요 |
| `label/md` 또는 caption | 문서 `12/18` | 코드 custom token 없음, 실제 `12/16` default 사용 | 불일치 |

## 7. Pretendard 적용 원칙

### 7.1 목표 적용 guideline

- 일반 사용자(보행약자) 대상 일반 모드는 Pretendard를 기본 font family로 사용한다.
- Pretendard 적용은 `font family 교체`만으로 끝내지 말고, `미정의 role 보강`과 `raw override 정리`를 함께 진행한다.
- 일반 모드에서는 `title`, `body`, `label` 역할을 명확히 나누고, 숫자 강조는 별도 강조 규칙으로 관리한다.
- 저시력/시각보조 화면은 일반 모드 Pretendard rollout에 포함하지 않는다.
- 저시력/시각보조 화면은 현재 large-type/high-contrast interaction model을 유지하고, 별도 theme 분기 또는 route-scoped theme를 통해 분리 적용한다.

### 7.2 저시력/시각보조 분리 원칙

| 항목 | 목표 적용 guideline |
| --- | --- |
| 적용 대상 | `feature/lowvision/**` 전체와 고대비/oversized text 기반 화면 |
| 기본 font family | 현재 접근성용 글꼴 체계 유지 권장 |
| Pretendard 적용 여부 | 일반 모드와 같은 시점의 전역 일괄 교체 비권장 |
| 선행 조건 | 저시력용 별도 typography/theme 분기 도입, `TermsGuideScreen` 소속 확정, 공용 컴포넌트 재분류 |

## 8. Pretendard Typography 토큰 제안

### 8.1 목표 적용 guideline

| token name | font family | font size | line height | font weight | 용도 | 적용 대상 화면군 | 현재 토큰 유지 가능 여부 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `displayLarge` | `Pretendard` | `32.sp` | `40.sp` | `SemiBold(600)` | 온보딩 핵심 문구, 브랜드성 hero | 온보딩 | 유지 가능, font family만 교체 |
| `headlineLarge` | `Pretendard` | `32.sp` | `40.sp` | `Bold(700)` | 숫자 강조 카드, 요약 metric hero | 경로 설정, 길안내 요약 | 신규 정의 필요 |
| `headlineMedium` | `Pretendard` | `28.sp` | `36.sp` | `Bold(700)` | 화면 대표 제목 | 로그인, 검색 진입, 제보, 고정 화면 제목 | weight 조정 필요 |
| `headlineSmall` | `Pretendard` | `24.sp` | `32.sp` | `SemiBold(600)` | 길안내 지시문, 도착 headline, 검색 중간 대표 문구 | 검색, 길안내, 도착, 앱 정보 | 신규 정의 필요 |
| `titleLarge` | `Pretendard` | `22.sp` | `30.sp` | `SemiBold(600)` | 카드/시트 제목, 주요 CTA 근처 제목 | 지도 시트, 저장 경로, 제보, 길안내 hero | 유지 가능 |
| `titleMedium` | `Pretendard` | `18.sp` | `26.sp` | `SemiBold(600)` | 섹션 제목, 리스트 제목 | 지도, 검색, 경로 설정, 마이페이지 | weight 조정 필요 |
| `titleSmall` | `Pretendard` | `14.sp` | `20.sp` | `SemiBold(600)` | 소형 카드 제목, inline CTA 텍스트 | 지도 필터, 제보 카드, 제보 이력 CTA | 신규 정의 필요 |
| `bodyLarge` | `Pretendard` | `16.sp` | `24.sp` | `Regular(400)` | 일반 본문, 핵심 설명 | 대부분 일반 모드 공통 | 유지 가능 |
| `bodyMedium` | `Pretendard` | `14.sp` | `22.sp` | `Regular(400)` | 보조 본문, 리스트 부연, 폼 helper | 검색, 제보, 마이페이지, 저장 경로 | 유지 가능, 문서 기준 통합 필요 |
| `bodySmall` | `Pretendard` | `12.sp` | `18.sp` | `Regular(400)` | 주소, 좌표, 상태 부연 | 지도 메타, 제보 메타, 이력 부가 정보 | 신규 정의 필요 |
| `labelLarge` | `Pretendard` | `14.sp` | `20.sp` | `SemiBold(600)` | 버튼, 탭, 주요 칩 | 로그인 버튼, 저장/탐색 CTA, 도착 CTA | weight 조정 필요 |
| `labelMedium` | `Pretendard` | `12.sp` | `18.sp` | `Medium(500)` | 상태, 배지, count, secondary chip | 검색 메타, 제보 카운트, 저장 경로 배지 | 신규 정의 필요 |
| `labelSmall` | `Pretendard` | `11.sp` | `16.sp` | `Medium(500)` | compact tag, 미세 부연 | 저장 경로 태그, 경로 설정 step badge | 신규 정의 필요, 사용 최소화 권장 |

### 8.2 token 제안 보충

| 항목 | 제안 |
| --- | --- |
| `bodyMedium` line height | 현재 코드와 접근성 측면을 고려하면 `14/22` 유지가 더 안전하다. 단, 문서 정합성 차원에서 컴포넌트 가이드와 디자인 컨벤션 중 하나를 canonical source로 확정해야 한다. |
| `labelMedium` / `bodySmall` 분리 | `12sp` 계열을 모두 `labelMedium`에 몰지 말고, 상태/배지용은 `labelMedium 12/18`, 주소/부연용은 `bodySmall 12/18`으로 분리한다. |
| 숫자 강조 | 일반 모드에서 거리/시간 등 강한 metric은 `headlineLarge` 중심으로 정리하고, 저시력 화면의 60sp 이상 raw scale은 일반 모드에 재사용하지 않는다. |

## 9. 화면군별 Pretendard 적용 가이드

### 9.1 목표 적용 guideline: 일반 모드

| 화면군 | 대표 제목 | 섹션 제목 | 본문 | 보조 설명 | 버튼 텍스트 | 상태/배지/탭 라벨 | 숫자 강조 정보 | 적용 이유 | 예외 규칙 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 인증/로그인 | `headlineMedium` | `titleLarge` | `bodyLarge` | `bodyMedium` | `labelLarge` | `labelMedium` | 사용 안 함 | 첫 진입 화면이라 명확한 위계가 필요하다. | 브랜드 hero만 `displayLarge` 허용 |
| 온보딩 | `displayLarge` | `titleMedium` | `bodyLarge` | `bodyMedium` | `labelLarge` | `labelMedium` | 사용 안 함 | 선택 흐름과 설명 흐름을 크게 구분해야 한다. | 카드형 CTA가 크더라도 raw size 사용 대신 `displayLarge`/`headlineMedium` 조합으로 처리 |
| 지도 메인 | `titleLarge` | `titleSmall` | `bodyLarge` | `bodyMedium`, `bodySmall` | `labelLarge` | `labelMedium`, `labelSmall` | 순번 등은 `headlineLarge` 대신 별도 badge 규칙 우선 | 지도 위에는 과도한 text hierarchy가 화면 집중을 방해한다. | `PlaceListCard`는 일반 모드 소속 확정 전까지 별도 관리 |
| 검색/검색 결과 | `headlineSmall` | `titleMedium` | `bodyLarge` | `bodyMedium` | `labelLarge` | `labelMedium` | 사용 안 함 | 검색 결과는 제목-주소-메타 3단 위계가 가장 중요하다. | placeholder는 `bodyMedium` 기준으로 통일 |
| 경로 설정/경로 상세 | `titleLarge` | `titleMedium` | `bodyLarge` | `bodyMedium`, `bodySmall` | `labelLarge` | `labelMedium`, `labelSmall` | `headlineLarge` | 거리/시간/경유지 정보가 함께 보이므로 정보 역할 분리가 필요하다. | 경유지 badge는 `labelSmall` 이하로만 사용 |
| 길안내/도착 | `headlineSmall` | `titleMedium` | `bodyLarge` | `bodyMedium` | `labelLarge` | `labelMedium` | `headlineLarge` 또는 `titleLarge` | 길안내는 지시문 가독성과 CTA 인지성이 가장 중요하다. | turn-by-turn 본문은 `bodyLarge`로 낮추지 말고 `headlineSmall` 유지 |
| 저장 경로/북마크 | `titleLarge` | `titleMedium` | `bodyMedium` | `bodySmall` | `labelLarge` | `labelMedium`, `labelSmall` | 사용 안 함 | 목록 밀도와 재사용성이 중요하다. | 태그는 `labelSmall`, 주소는 `bodySmall`로 고정 |
| 제보 | `titleLarge` | `titleMedium` | `bodyLarge` | `bodyMedium` | `labelLarge` | `labelMedium`, `labelSmall` | 사용 안 함 | 폼 단계가 많아 제목/설명/입력 helper의 일관성이 중요하다. | error/helper는 `bodyMedium` 또는 `labelMedium`으로만 제한 |
| 마이페이지 | `titleMedium` | `titleSmall` | `bodyLarge` | `bodyMedium`, `bodySmall` | `labelLarge` | `labelMedium` | 사용 안 함 | 설정/목록/상태가 섞여 있어 과한 굵기 남용을 줄여야 한다. | 메뉴 row는 `bodyLarge` 대신 `titleSmall`로 올리지 않는다. |

### 9.2 목표 적용 guideline: 저시력/시각보조 전용 화면

| 화면군 | 대표 제목 | 섹션 제목 | 본문 | 보조 설명 | 버튼 텍스트 | 상태/배지/탭 라벨 | 숫자 강조 정보 | 적용 이유 | 예외 규칙 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 저시력/시각보조 전용 화면 | 현행 oversized 접근성 scale 유지 | 현행 raw scale 유지 | 현행 raw scale 유지 | 현행 raw scale 유지 | 현행 raw scale 유지 | 현행 raw scale 유지 | 현행 raw scale 유지 | high-contrast, large-type, gesture 안내 모델이 일반 모드와 다르다. | Pretendard 일반 모드 rollout과 분리하고 별도 theme wrapper를 둔다. |
| `TermsGuideScreen` 후보 | 저시력/고대비 계열로 우선 분류 | 동일 | 동일 | 동일 | 동일 | 동일 | 동일 | `24sp/48sp/88sp/28sp/34sp`, black/yellow high-contrast 구조가 일반 약관 화면보다 저시력 가이드에 가깝다. | 실제 제품 기획상 일반 약관 화면으로 확정되면 별도 일반 모드 재디자인이 선행돼야 한다. |

## 10. 구현 우선순위

| 우선순위 | 작업 범위 | 핵심 작업 | 영향 파일 |
| --- | --- | --- | --- |
| `P0` | 전역 theme/type 정리 | Pretendard 일반 모드 토큰 정의, 미정의 role 추가, 일반/저시력 typography 분기 설계 | `FE/app/src/main/java/com/ssafy/e102/eumgil/core/designsystem/theme/Type.kt`, `Theme.kt`, `MainActivity.kt`, 필요 시 `AppNavHost.kt` 또는 mode별 route wrapper |
| `P1` | 일반 모드 공통 화면 정리 | auth/onboarding/map/search/routesetting/navigation/savedroute/report/mypage에서 raw override와 default fallback 제거 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/auth/**`, `feature/onboarding/**`, `feature/map/**`, `feature/search/**`, `feature/routesetting/**`, `feature/navigation/**`, `feature/arrival/**`, `feature/savedroute/**`, `feature/report/**`, `feature/mypage/**` |
| `P2` | 예외 화면 및 저시력 분기 검토 | `feature/lowvision/**`, `TermsGuideScreen`, `PlaceListCard` 분리 여부 결정 및 별도 접근성 typography 유지 | `FE/app/src/main/java/com/ssafy/e102/eumgil/feature/lowvision/**`, `feature/terms/TermsGuideScreen.kt`, `core/designsystem/component/place/PlaceListCard.kt`, 관련 navigation 파일 |

## 11. 추가 확인 필요 항목

| 항목 | 이유 |
| --- | --- |
| `TermsGuideScreen`의 제품 소속 | 현재 라우트는 온보딩이지만 시각 언어와 폰트 스케일은 저시력/고대비 계열에 가깝다. |
| `PlaceListCard`의 사용 화면/모드 | 공통 컴포넌트 경로에 있지만 일반 모드 token 체계와 동떨어진 raw scale을 사용한다. |
| 일반 모드 canonical line height | `bodyMedium 14/22`와 `14/20` 중 어느 문서를 기준으로 확정할지 합의가 필요하다. |
| 저시력 전용 font family 정책 | 현재는 전역 font family를 상속하므로 Pretendard 일반 모드 적용 시 저시력 화면 보호 장치가 필요하다. |
| 암묵적 기본 스타일 구간 | 일부 버튼 라벨, placeholder, component 내부 기본 style merge 결과는 추가 코드 확인이 필요하다. |

## 12. 결론

### 12.1 current implementation fact

- 현재 FE 코드는 `Koddi UD On Gothic` 기반 단일 typography theme를 앱 전체에 적용하고 있다.
- 일반 모드 화면은 custom role과 Material 3 default role, raw `fontSize`/`fontWeight` 사용이 함께 섞여 있다.
- 저시력/시각보조 화면은 전역 font family를 상속하면서도 raw large-type scale을 광범위하게 사용한다.

### 12.2 목표 적용 guideline

- `Pretendard를 전역 일괄 교체`하는 방식은 현재 구조에서 권장하지 않는다.
- 권장안은 `일반 모드만 분기 적용`이다.
- 구체적으로는 `P0`에서 일반 모드용 Pretendard typography token을 완성하고, 동시에 저시력/시각보조 화면이 별도 typography를 쓰도록 분기한 뒤, `P1`에서 일반 모드 화면의 raw override와 default fallback을 제거하는 순서가 안전하다.
- 저시력/시각보조 화면은 현재 접근성용 large-type 체계를 유지하되, 전역 font family 변경의 영향에서 분리되도록 별도 theme 또는 route-scoped theme를 두는 것이 가장 현실적이다.

## 후속 작업 TODO

- `Type.kt`에 `headlineLarge`, `headlineSmall`, `titleSmall`, `bodySmall`, `labelMedium`, `labelSmall`를 명시적으로 정의한다.
- 일반 모드와 저시력 모드를 분리하는 typography/theme 진입점을 설계한다.
- 로그인/온보딩/경로 설정 화면의 `copy(fontSize = ...)` override를 토큰 기반으로 교체한다.
- `TermsGuideScreen`과 `PlaceListCard`의 소속 모드를 확정한다.
- FE 문서 기준에서 `bodyMedium`과 `label/caption` canonical scale을 하나로 통합한다.
