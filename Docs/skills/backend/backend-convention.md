# Backend Convention

## 목적

백엔드 코드를 작성할 때 AI와 팀원이 같은 기준으로 판단하기 위한 상위 문서다.

이 문서는 ERD/API 필드 값을 반복해서 적지 않는다. 도메인 필드, enum, API path, 요청/응답 필드는 항상 아래 문서를 우선한다.

- `Docs/API/2026-04-12_API_전체_목록.md`
- `Docs/API/{도메인}/..._API_명세.md`
- `Docs/ARD/2026-04-12_ERD_초안.md`

## 문서 사용 순서

백엔드 코드를 작성할 때는 아래 순서로 확인한다.

1. API 명세에서 path, method, request, response, error를 확인한다.
2. ERD에서 테이블, 컬럼, enum, 관계를 확인한다.
3. 이 skills 문서에서 패키지 구조, 응답 규격, 예외 처리, DB 접근 방식을 확인한다.

## 세부 컨벤션 문서

- `layer-package-convention.md`: 패키지 구조와 Controller, Service, DTO, Entity 작성 기준
- `api-response-error-convention.md`: 공통 응답, 에러 코드, 예외 처리 기준
- `persistence-postgis-convention.md`: JPA, PostgreSQL, PostGIS, Repository 작성 기준
- `config-external-convention.md`: profile, config, 외부 API client 작성 기준

## 기술 기준

- Java 21 기준으로 작성한다.
- Spring Boot와 Gradle을 기준으로 작성한다.
- 기본 데이터 접근은 Spring Data JPA를 사용한다.
- 공간 데이터 처리는 PostgreSQL + PostGIS를 기준으로 한다.
- API 문서화는 Springdoc OpenAPI를 기준으로 한다.
- 포맷팅과 Checkstyle 규칙은 별도 컨벤션/설정 작업을 따른다.

## 판단 원칙

- 명세에 없는 필드나 API를 코드 작성 중 임의로 추가하지 않는다.
- API 기본 prefix는 `/api`를 사용한다.
- 설계가 애매하면 DB 구조보다 API 명세와 기능 흐름을 먼저 확인한다.
- 단순 CRUD와 공간 질의는 구현 방식을 분리한다.
- 인증/OAuth, Redis, RabbitMQ, GraphHopper 설정은 실제 기능 착수 또는 PoC 결과가 나온 뒤 붙인다.
- PoC 코드는 운영 도메인 패키지에 섞지 않는다.
