<div align="center">

# 부산이음길

### Busan EumGil

**부산 지역 이동 약자를 위한 무장애 길찾기 서비스**

<br>

[![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3776AB?style=flat-square&logo=python&logoColor=white)](https://www.python.org)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com)
[![Jenkins](https://img.shields.io/badge/Jenkins-D24939?style=flat-square&logo=jenkins&logoColor=white)](https://www.jenkins.io)
[![AWS](https://img.shields.io/badge/AWS-232F3E?style=flat-square&logo=amazonaws&logoColor=white)](https://aws.amazon.com)

</div>

---

## 프로젝트 소개

**부산이음길**은 부산 지역 이동 약자를 위한 **무장애 길찾기 모바일 서비스**입니다.

부산은 언덕, 계단, 급경사, 단차가 많은 도시 구조를 가지고 있어 휠체어 이용자, 고령자, 유아차 동반 보호자, 시각장애인에게 일반 보행자용 길찾기 서비스가 그대로 통하지 않습니다.

부산이음길은 이런 문제를 해결하기 위해 다음을 목표로 합니다.

- 이동 약자 맞춤 경로 탐색
- 경사, 계단, 엘리베이터, 점자블록 같은 접근성 정보 제공
- 시민 참여형 장애물 제보
- 부산 지역 중심의 무장애 이동 데이터 축적

### 핵심 사용자

- 시각장애인
- 휠체어 이용자
- 고령자
- 유아차 동반 보호자
- 일시적 이동 불편자

### 핵심 기능

- **무장애 경로 탐색**: 경사로, 엘리베이터, 자동문 중심 경로 우선
- **접근성 지도 정보 제공**: 장애인 화장실, 충전소, 점자블록, 베리어프리 시설 표시
- **인증 기반 사용 흐름**: 소셜 로그인, 사용자 유형, 온보딩 상태에 따라 앱 진입 분기
- **TTS 기반 길안내**: 시각장애인 사용자를 고려한 음성 안내
- **장애물 제보**: 공사, 점자블록 손상, 보행 장애물 제보

### 핵심 흐름

```text
현재 위치 / 목적지 입력
  -> 접근성 데이터 조회
  -> 무장애 경로 계산
  -> 지도 표시 + 음성 안내
  -> 사용자 이동 / 제보 데이터 축적
```

---

## 주요 문서

- PRD: [Docs/PRD/2026-04-09_부산이음길_PRD.md](Docs/PRD/2026-04-09_부산이음길_PRD.md)
- 프로젝트 기획서: [Docs/기획/2026-04-10 최종_프로젝트_기획서.md](<Docs/기획/2026-04-10 최종_프로젝트_기획서.md>)
- Frontend README: [FE/README.md](FE/README.md)
- 인프라 설계안: [Docs/인프라/2026-04-20_AWS_인프라_설계안.md](<Docs/인프라/2026-04-20_AWS_인프라_설계안.md>)
- 인프라 현재 상태: [Docs/인프라/2026-05-20_인프라_현재상태_및_운영_기준.md](Docs/인프라/2026-05-20_인프라_현재상태_및_운영_기준.md)
- INF 기준: [INF/README.md](INF/README.md)


## 기술 스택

| 분류 | 기술 |
|---|---|
| Mobile App | Kotlin, Android, Jetpack Compose |
| Backend API | Spring Boot 3, Spring Data JPA |
| Data / AI | Python |
| Route Engine | GraphHopper |
| DB | PostgreSQL/PostGIS |
| Cache | Redis / ElastiCache |
| Infra | AWS, Docker, Docker Compose |
| CI/CD | Jenkins |
| Monitoring | PLG, CloudWatch |

[E102_기술스택](Docs/img/E102_기술스택.jpg)
---

## 저장소 구조

```text
S14P31E102/
├── FE/                    # Android 앱, 상세는 FE/README.md 참고
├── BE/                    # Spring Boot API
├── AI/                    # Python 실험 코드, 데이터 가공, 인식 관련 작업
├── Docs/                  # 기획, PRD, API, 인프라, 회의록 등 설명 문서
├── INF/                   # 운영 설정 자산
├── scripts/               # make에서 호출하는 자동화 스크립트
├── Makefile
└── README.md
```

### 디렉토리 책임

- `FE/`
  - Android 클라이언트 코드
- `BE/`
  - Spring Boot 서버 코드
- `AI/`
  - Python 기반 실험/검증/데이터 처리 코드
- `Docs/`
  - 설명 문서, 설계안, runbook, 운영 가이드
- `INF/`
  - 운영 설정 자산
  - AWS, Jenkins, monitoring 관련 기준 파일
- `scripts/`
  - `make`가 호출하는 스크립트
  - 배포, 초기화, QA, GIS 작업 자동화

### 운영 파일 배치 원칙

- 루트 디렉토리는 실행 진입점으로 사용한다.
- 앞으로 환경별 compose 파일은 루트에서 관리한다.
  - `docker-compose.local.yml`
  - `docker-compose.dev.yml`
  - `docker-compose.prod.yml`
- 환경 변수 파일도 루트에서 관리한다.
  - `.env.local`
  - `.env.dev`
  - `.env.prod`
- 환경 변수 key 목록과 템플릿은 노션에서 관리하며, 저장소에는 env example 파일을 두지 않는다.
- `INF/`에는 설명 문서보다 **운영 설정 자산**을 둔다.
- 정식 설계안, 장애 대응 절차, 운영 설명은 `Docs/인프라`에서 관리한다.


---

## 현재 아키텍처 결정

이 저장소는 현재 **EC2 2대 운영 구조**를 기준으로 인프라 계약을 맞춰가는 단계입니다.

2026-05-20 실서버 확인 기준 최신 런타임 스냅샷은 [Docs/인프라/2026-05-20_인프라_현재상태_및_운영_기준.md](Docs/인프라/2026-05-20_인프라_현재상태_및_운영_기준.md)를 기준으로 합니다.

### 현재 운영 기준

- `S1 = dev + Jenkins + build runner + 운영도구`
- `S2 = primary prod runtime`
- `RDS = PostgreSQL managed service`
- `ElastiCache = 필요 시 운영`
- EC2 shell 접속은 `SSH`를 기본으로 사용하되, 관리자 고정 IP에서만 허용
- `RDS`, `ElastiCache` 같은 private managed resource 접근은 `SSM Session Manager` 포트 포워딩 기준
- `SSM Session Manager`는 SSH 장애 시 복구 채널로도 유지
- 1차 알람은 AWS 관리 평면 기준으로 운영
- 보조 모니터링은 `PLG`를 사용
- 서비스 API와 필요한 관리자 UI는 각 서버의 `Nginx` `80/443` host-based routing으로 접근한다.
- EC2에서 외부에 직접 여는 포트는 `80/443`과 `22`만 두며, `22`는 관리자 고정 IP에서만 허용한다.

### 서버 역할

- `S1`
  - `dev` 상시 실행
  - `Jenkins` 상시 실행
  - `GraphHopper runtime`은 dev serve 용도로 실행
  - `Grafana`, `Portainer`, `SonarQube`, `PLG` 운영도구 실행
- `S2`
  - `primary prod` 실행
  - 운영 WAS, AI Flask intent server, 관리자 웹 실행
  - prod GraphHopper runtime은 S2 내부 `blue/green` slot으로 실행

### 운영 원칙

- 현재 운영은 **2대 기반 현실형 운영안**이다.
- 현재는 `S2 primary prod + S1 dev/Jenkins/운영도구` 구조이며, ALB 기반 Blue/Green은 같은 VPC 또는 private routing 정리 이후 확장 옵션으로 둔다.
- 현재 prod blue/green은 EC2 서버 단위가 아니라 S2 내부 GraphHopper `blue/green` runtime slot 전환을 의미한다.
- prod와 dev의 역할 경계는 문서와 설정에서 명확해야 한다.
- GraphHopper의 무거운 build/import 작업은 prod 서버에서 직접 돌리지 않는다.
- Jenkins, Grafana, SonarQube, PLG 같은 관리자 UI는 원 포트를 공개하지 않고 `Nginx`의 `443` host routing으로만 접근한다.
- Portainer는 외부 공개하지 않고 `make portainer-tunnel` 기반 SSH 터널로만 접근한다.
- 관리자 UI 라우팅은 OAuth, source IP 제한, VPN, SSH 터널 같은 접근 제한을 전제로 한다.
- 관리자 UI의 웹 접근은 초기 편의 운영안이며, 운영 안정화 후에는 VPN 또는 SSM 터널 기반 접근으로 축소할 수 있다.

### 현재 저장소 상태

- 현재 저장소는 최종 운영 구조를 향해 정리 중이다.
- 일부 설정 자산과 디렉토리 구조는 먼저 잡고, 실제 런타임 파일은 이후 맞춰갈 수 있다.
- 따라서 이 README는 "완성된 배포 가이드"보다 **현재 기준 운영 계약과 저장소 기준**을 설명하는 문서로 읽는 것이 맞다.

---

## 시작하기

### 1. 클론

```bash
git clone <repository-url>
cd S14P31E102
```

### 2. 초기 세팅

```bash
make init
```

정상 설치되면 `세팅 완료`가 출력된다.

### 3. 환경 변수 준비

현재 기준 환경 변수 파일은 루트에서 관리한다.

- 개발용: `.env.dev`
- 운영용: `.env.prod`

필요 시 노션의 env 기준을 확인해 환경별 파일을 준비한다.

### 4. 문서 먼저 확인

구조를 빠르게 이해하려면 아래 순서가 좋다.

1. `README.md`
2. `Docs/PRD`
3. `Docs/인프라`
4. `INF/README.md`

---

## Git / Jira 컨벤션

### 커밋 메시지

```text
{gitmoji} {type}[#{이슈번호}]: {내용}
```

예시:

```bash
✨ Feat[#31]: 로그인 API 추가
🐛 Fix[#32]: RDS 커넥션 타임아웃 수정
♻️ Refactor[#33]: 경로 탐색 서비스 구조 정리
🔧 Chore[#41]: docker-compose 설정 정리
📝 Docs[#42]: 인프라 설계안 갱신
```

`make init` 실행 후 `[#31]`은 자동으로 `[S14P31E102-31]`로 변환되어 Jira에 연결된다.

브랜치에 Jira ticket이 이미 포함되어 있으면 아래 형식도 자동 보정된다.

```bash
git commit -m "✨ Feat: 로그인 API 추가"
```

예시:

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

### 브랜치 컨벤션

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

### MR 컨벤션

- 제목에 Jira 이슈 키 포함: `S14P31E102-32 Prod AWS 서비스 연결`
- 본문에 `Closes S14P31E102-32` 포함 시 머지 후 Jira 이슈 자동 완료

### 자주 쓰는 명령

```bash
make init
git br be/feat/login-31
git add .
git commit -m "✨ Feat[#31]: 로그인 API 추가"
make test-git-jira
```
