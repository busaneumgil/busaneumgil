# API Response And Error Convention

## 목적

API 공통 응답, 에러 코드, 예외 처리 방식을 통일한다.

API별 request/response 필드와 status code는 API 명세서를 우선한다.

## 위치

- `ApiResponse`, `ErrorResponse`: `common.response`
- `BusinessException`, `ErrorCode`, `GlobalExceptionHandler`: `common.exception`

## 성공 응답

```json
{
  "success": true,
  "data": {},
  "message": null
}
```

데이터가 없는 성공 응답은 `data = null`을 허용한다.

```java
public record ApiResponse<T>(
        boolean success,
        T data,
        String message
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, null, null);
    }
}
```

## 실패 응답

```json
{
  "success": false,
  "code": "INVALID_INPUT",
  "message": "잘못된 입력입니다."
}
```

```java
public record ErrorResponse(
        boolean success,
        String code,
        String message
) {
    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(false, errorCode.name(), errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(false, errorCode.name(), message);
    }
}
```

## Error Code

| HTTP Status | code | 사용 기준 |
| --- | --- | --- |
| 400 | INVALID_INPUT | 요청값 검증 실패 |
| 401 | UNAUTHORIZED | 인증 필요 |
| 403 | FORBIDDEN | 권한 없음 |
| 404 | RESOURCE_NOT_FOUND | 조회 대상 없음 |
| 409 | CONFLICT | 중복 요청 또는 상태 충돌 |
| 500 | INTERNAL_ERROR | 서버 내부 예외 |
| 502 | EXTERNAL_API_ERROR | 외부 API 호출 실패 |

## 메시지 기준

- 기본 message는 `ErrorCode`의 공통 문구를 사용한다.
- 상황별 문구가 필요하면 override한다.
- 검증 실패처럼 사용자가 고칠 수 있는 오류는 필드명을 포함할 수 있다.
- 내부 클래스명, SQL, stack trace, API key, 내부 URL은 response message에 넣지 않는다.

예시:

```java
throw new BusinessException(ErrorCode.CONFLICT, "이미 북마크한 장소입니다.");
```

## 예외 처리 기준

- Controller에서 try-catch로 에러 응답 생성을 반복하지 않는다.
- Validation 예외는 `INVALID_INPUT`으로 응답한다.
- 외부 API 예외는 `EXTERNAL_API_ERROR`로 응답한다.
- 예상하지 못한 예외는 `INTERNAL_ERROR`로 응답한다.
- 상세 예외는 서버 로그에 남긴다.

## 로그 기준

- 400 validation 오류는 기본적으로 `log.error`를 남기지 않는다.
- 일반적인 404, 409 비즈니스 예외도 `log.error`를 남기지 않는다.
- 추적이 필요한 비즈니스 예외는 `log.warn`으로 남긴다.
- 외부 API 호출 실패는 `log.warn` 또는 `log.error`로 남긴다.
- 예상하지 못한 500 예외는 `log.error("...", exception)`로 남긴다.
- 로그에 API key와 개인정보를 남기지 않는다.

## Slack 콜백 예외

- Slack 요청 확인에는 빠른 `200 OK` 응답이 필요할 수 있다.
- 공통 응답 wrapper 적용 여부는 Slack 연동 방식에 맞춰 판단한다.
- Slack 서명 검증 실패는 `UNAUTHORIZED`로 처리한다.
- 이미 처리된 제보는 `CONFLICT`로 처리한다.
