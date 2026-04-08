# Git / Jira 컨벤션

최초 1회만 아래 명령을 실행한다.

```bash
make init
```

정상 설치되면 `세팅 완료`가 출력된다.

## 커밋 컨벤션

### 커밋 메시지

```text
{gitmoji} {type}[#{이슈번호}]: {내용}
```

```bash
✨ Feat[#31]: docker-compose.dev.yml 생성
🐛 Fix[#32]: RDS 커넥션 타임아웃 수정
♻️ Refactor[#33]: Celery 태스크 구조 개선
🔧 Chore[#41]: .gitignore 업데이트
📝 Docs[#42]: API 명세서 작성
```

`make init` 실행 후 `[#31]`은 자동으로 `[S14P31E102-31]`로 변환되어 Jira에 연결된다.

브랜치에 Jira ticket이 이미 포함되어 있으면 아래 형식도 자동 보정된다.

```bash
git commit -m "✨ Feat: 로그인 API 추가"
```

예:

```text
✨ Feat[S14P31E102-31]: 로그인 API 추가
```

### 타입

| gitmoji | type | 용도 |
|---|---|---|
| `✨` | `Feat` | 새 기능 |
| `🐛` | `Fix` | 버그 수정 |
| `🚑` | `Hotfix` | 긴급 수정 |
| `♻️` | `Refactor` | 리팩토링 |
| `🔧` | `Chore` | 설정, 기타 |
| `✅` | `Test` | 테스트 |
| `📝` | `Docs` | 문서 |

## 브랜치 컨벤션

브랜치는 `git br`로 생성한다.

```text
feat/{설명}-{이슈번호}      예: feat/server-init-31
fix/{설명}-{이슈번호}       예: fix/rds-timeout-32
be/feat/{설명}-{이슈번호}   예: be/feat/login-31
fe/fix/{설명}-{이슈번호}    예: fe/fix/header-32
```

예:

```bash
git br be/feat/login-31
```

실제 생성 결과:

```text
be/feat/login-S14P31E102-31
```

브랜치 규칙은 마지막이 반드시 `-숫자`여야 한다.

비허용 예:

```bash
git br be/feat/login-31-ryuwon
git br be/feat/login-S14P31E102-31-ryuwon
```

## MR 컨벤션

- 제목에 Jira 이슈 키 포함: `S14P31E102-32 Prod AWS 서비스 연결`
- 본문에 `Closes S14P31E102-32` 포함 시 머지 후 Jira 이슈 자동 완료

## 자주 쓰는 명령

```bash
make init
git br be/feat/login-31
git add .
git commit -m "✨ Feat[#31]: 로그인 API 추가"
make test-git-jira
```
