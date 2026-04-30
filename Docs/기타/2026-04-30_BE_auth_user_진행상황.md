# BE Auth/User 진행상황

> 작성일: 2026-04-30
> 브랜치: `be/feat/auth-S14P31E102-368`

## 현재 브랜치 상태

- 이 브랜치는 `be/feat/auth-S14P31E102-362`의 Auth/User foundation 작업 위에 쌓인 stacked branch이다.
- MR1이 develop에 먼저 머지되면, 이 브랜치는 develop 기준으로 rebase 또는 retarget 정리가 필요하다.
- DTO 일부는 API 구현 단계에서 다시 반영할 예정이며, 현재 MR2 범위에서는 보안/토큰/소셜 verifier 기반을 우선 구현했다.

## 완료한 작업

### MR1: Auth/User Foundation

- `S14P31E102-371`: Auth/User 도메인 예외 구조 추가
  - `AuthException`, `AuthErrorCode`
  - `UserException`, `UserErrorCode`
- `S14P31E102-367`: User 도메인 기반 추가
  - `User` 엔티티
  - `SocialProvider`, `PrimaryUserType`, `MobilitySubtype`
  - `UserRepository`
  - User 엔티티 검증 테스트

### MR2: Token/Security/Social Login

- `S14P31E102-368`: Spring Security 기본 구조 구현
  - stateless security filter chain
  - 인증 실패/인가 실패 JSON 응답
  - 인증 필요 endpoint와 public endpoint 분리
- `S14P31E102-369`: JWT 발급/검증 및 UUID subject 처리 구현
  - access token, refresh token, signup token 발급/검증
  - service token은 UUID subject 사용
  - signup token은 social provider identity를 claim으로 보관
  - bearer token 기반 `AuthPrincipal` 주입
- `S14P31E102-370`: refresh/signup token 저장 전략 구현
  - Redis 기반 refresh token store
  - Redis 기반 signup token store
  - raw token을 Redis key로 쓰지 않고 SHA-256 hash를 key suffix로 사용
  - refresh token rotation 지원
- `S14P31E102-372`: Kakao/Naver/Google social token verifier 구현
  - FE가 전달한 provider access token으로 user-info API 호출
  - provider 응답을 `SocialUserInfo`로 정규화
  - provider 4xx는 `INVALID_SOCIAL_TOKEN`
  - provider 5xx/호출 실패/응답 이상은 `SOCIAL_PROVIDER_API_FAILED`

## 검증 내역

- `cd BE && .\gradlew.bat test --tests com.ssafy.e102.global.security.SecurityConfigTest`
- `cd BE && .\gradlew.bat test --tests com.ssafy.e102.global.security.JwtTokenProviderTest --tests com.ssafy.e102.global.security.SecurityConfigTest`
- `cd BE && .\gradlew.bat test --tests com.ssafy.e102.domain.auth.token.RedisRefreshTokenStoreTest --tests com.ssafy.e102.domain.auth.token.RedisSignupTokenStoreTest`
- `cd BE && .\gradlew.bat test --tests com.ssafy.e102.domain.auth.client.SocialTokenVerifierTest --tests com.ssafy.e102.domain.auth.client.CompositeSocialTokenVerifierTest`
- `cd BE && .\gradlew.bat spotlessJavaCheck`
- `cd BE && .\gradlew.bat checkstyleMain checkstyleTest`
- `cd BE && .\gradlew.bat test`
- `git diff --check`

## MR 계획

### MR1: Auth/User Foundation

- 대상 브랜치: `be/feat/auth-S14P31E102-362`
- 대상 Jira:
  - `S14P31E102-367`
  - `S14P31E102-371`
- MR 제목:
  - `S14P31E102-362 [BE] Auth/User 도메인 기반 구조 추가`
- 리뷰 포인트:
  - User 엔티티가 최신 ERD/API 계약과 맞는지
  - `profileCompleted` 같은 불필요한 persisted 상태를 추가하지 않았는지
  - domain별 exception/error code 구조가 팀 컨벤션으로 적절한지

### MR2: Token, Security, Social Login Flow

- 대상 브랜치: `be/feat/auth-S14P31E102-368`
- 대상 Jira:
  - `S14P31E102-368`
  - `S14P31E102-369`
  - `S14P31E102-370`
  - `S14P31E102-372`
  - `S14P31E102-373`
  - `S14P31E102-374`
- 현재 완료:
  - `S14P31E102-368`
  - `S14P31E102-369`
  - `S14P31E102-370`
  - `S14P31E102-372`
- 남은 작업:
  - `S14P31E102-373`: `POST /api/auth/social-login` 기존/신규 사용자 흐름 구현
  - `S14P31E102-374`: 신규 사용자 signup token 기반 회원가입 완료 흐름 구현
- MR 제목:
  - `S14P31E102-362 [BE] 소셜 로그인 및 회원가입 토큰 흐름 구현`
- 리뷰 포인트:
  - service token subject가 UUID인지
  - 신규 소셜 사용자가 signup 전 `users` row를 만들지 않는지
  - provider 응답이 service layer로 직접 새지 않고 `SocialUserInfo`로 정규화되는지
  - invalid provider token과 provider 장애가 다른 error code로 내려가는지

### MR3: Session/User API/Test/Swagger

- 대상 Jira:
  - `S14P31E102-364`
  - `S14P31E102-375`
  - `S14P31E102-376`
  - `S14P31E102-377`
  - `S14P31E102-378`
  - `S14P31E102-379`
  - `S14P31E102-380`
- 남은 작업:
  - `POST /api/auth/reissue`
  - `POST /api/auth/logout`
  - `GET /api/users/me`
  - `PATCH /api/users/me/user-type`
  - `DELETE /api/users/me`는 회원탈퇴 정책 확정 후 포함
  - controller/service/failure path 테스트
  - Swagger 문서 정합성 확인
- MR 제목:
  - `S14P31E102-364 [BE] 토큰 재발급 로그아웃 사용자 API 및 테스트 구현`

## 남은 결정 사항

- 회원탈퇴 정책
  - soft delete인지, 물리 삭제/익명화인지 확정 필요
  - bookmark/favorite route/rating/report 보존 정책 확정 필요
- signup token TTL
  - 현재 기본값은 10분
  - FE 온보딩 흐름 기준으로 충분한지 확인 필요
- provider 운영 범위
  - Kakao/Naver/Google 모두 production-ready로 열지, MVP에서 일부만 우선 운영할지 확인 필요
- MR2 병합 순서
  - MR1이 먼저 머지되어야 MR2 diff가 리뷰 가능하다.
  - MR1 머지 후 MR2는 develop 기준 rebase가 필요할 수 있다.
