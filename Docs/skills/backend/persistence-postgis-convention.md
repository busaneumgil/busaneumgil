# Persistence And PostGIS Convention

## 목적

JPA, PostgreSQL, PostGIS, Repository 작성 기준을 통일한다.

테이블과 컬럼의 실제 구조는 ERD 문서를 우선한다.

## DB 접근 기준

- Main DB는 PostgreSQL이다.
- 공간 데이터 처리는 PostGIS를 사용한다.
- 일반 CRUD는 Spring Data JPA를 우선 사용한다.
- 공간 질의, 반경 검색, 선형 데이터 처리, 경로 주변 검색은 native SQL 또는 전용 Query Repository로 분리한다.
- QueryDSL은 기본 도입하지 않는다.

## ID와 FK 기준

- PK/FK 타입은 ERD를 우선한다.
- 자동 증가 PK는 DB 명세상 `BIGSERIAL`로 표현할 수 있다.
- FK는 자동 증가 컬럼이 아니므로 `BIGSERIAL`로 작성하지 않는다.
- Entity 연관관계는 처음부터 과하게 양방향으로 만들지 않는다.

## Auditing 기준

- 생성일시/수정일시는 JPA Auditing 기반 `BaseEntity`로 관리한다.
- `BaseEntity`는 `common.entity`에 둔다.
- 테이블별 Entity에 같은 auditing 필드를 반복 선언하지 않는다.
- soft delete가 필요하면 공통 정책으로 분리해서 검토한다.

## 공간 타입 기준

- API 요청/응답 DTO에는 Geometry 타입을 직접 노출하지 않는다.
- API 좌표는 `lat`, `lng` 구조로 받는다.
- JTS Coordinate는 `x = lng`, `y = lat` 순서로 넣는다.
- PostGIS 저장 좌표계는 ERD 기준 SRID를 따른다.
- 거리/반경 검색은 PostGIS 함수를 사용한다.

예시:

```java
public Point toPoint(double lat, double lng) {
    Point point = geometryFactory.createPoint(new Coordinate(lng, lat));
    point.setSRID(4326);
    return point;
}
```

## Repository 분리 기준

단순 CRUD:

```java
public interface PlaceRepository extends JpaRepository<Place, Long> {
}
```

공간 질의:

```text
PlaceSpatialRepository
PlaceSpatialRepositoryImpl
```

## native SQL 사용 기준

- 반경 기반 장소 검색
- 특정 선형 도로 구간 주변 조회
- 좌표 간 거리 계산
- PostGIS 함수를 직접 호출해야 하는 조회
- 성능상 인덱스와 실행 계획 확인이 필요한 공간 질의

## 금지 사항

- ERD에 없는 테이블이나 컬럼을 임의로 추가하지 않는다.
- API 편의를 이유로 DB 구조를 먼저 바꾸지 않는다.
- 외부 API 응답 필드를 Entity에 그대로 붙이지 않는다.
- Repository에서 API Response DTO를 직접 만들지 않는다.
- 대량 적재나 GIS 전처리 파이프라인을 운영 API 코드와 섞지 않는다.
