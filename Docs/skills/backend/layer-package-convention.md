# Layer And Package Convention

## 목적

백엔드 패키지 구조와 계층별 작성 기준을 맞춘다.

API path, request/response 필드, enum 값은 API 명세와 ERD를 우선한다.

## 패키지 구조

`com.ssafy.e102` 바로 아래 패키지는 모두 동일 레벨로 둔다.

```text
com.ssafy.e102
├─ common
│  ├─ response
│  ├─ exception
│  ├─ entity
│  └─ util
├─ config
├─ external
├─ user
├─ place
├─ route
├─ report
└─ publictransit
```

도메인 패키지 내부는 아래 구조를 기본으로 둔다.

```text
{domain}
├─ controller
├─ dto
│  ├─ request
│  └─ response
├─ entity
├─ repository
└─ service
```

예시:

```text
place
├─ controller
├─ dto
│  ├─ request
│  └─ response
├─ entity
├─ repository
└─ service
```

## common 기준

- `common.response`: `ApiResponse`, `ErrorResponse`
- `common.exception`: `BusinessException`, `ErrorCode`, `GlobalExceptionHandler`
- `common.entity`: `BaseEntity`
- `common.util`: 여러 도메인에서 반복해서 쓰는 순수 변환/계산 함수

## Controller 기준

- API 기본 prefix는 `/api`를 사용한다.
- API path, method, status code는 API 명세서를 따른다.
- Request Body가 있는 API에는 `@Valid`를 붙인다.
- Entity를 API 응답으로 직접 반환하지 않는다.
- Controller에서 Repository나 외부 API client를 직접 호출하지 않는다.
- 성공 응답은 공통 응답 규격으로 감싼다.

예시:

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PlaceSearchResponse>>> searchPlaces(
            @Valid PlaceSearchRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(placeService.searchPlaces(request)));
    }
}
```

## DTO 기준

- Request DTO와 Response DTO는 분리한다.
- DTO 필드명은 API 명세의 camelCase를 따른다.
- 좌표 요청/응답은 `lat`, `lng` 구조를 사용한다.
- Geometry 타입을 API DTO에 직접 노출하지 않는다.
- DTO는 Java record 사용을 우선 검토한다.
- Validation annotation은 Request DTO에 둔다.

## Service 기준

- Service 인터페이스는 기본 생성하지 않는다.
- 구현체가 2개 이상 필요하거나 대체 구현이 필요한 경우에만 인터페이스를 둔다.
- 조회 메서드는 `@Transactional(readOnly = true)`를 사용한다.
- 생성/수정/삭제 메서드는 `@Transactional`을 사용한다.
- 외부 API 호출은 `external` 하위 client/service로 분리한다.

## Entity 기준

- Entity와 enum은 도메인 내부 `entity` 패키지에 둔다.
- Entity에는 무분별한 setter를 만들지 않는다.
- 값 변경은 의미 있는 메서드로 작성한다.
- enum은 `@Enumerated(EnumType.STRING)`을 사용한다.
- 공통 생성/수정 시각은 `BaseEntity`와 JPA Auditing으로 관리한다.
