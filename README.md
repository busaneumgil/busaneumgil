<div align="center">

# 부산이음길 (Busan EumGil)

<img src="Docs/img/busan-eumgil-wordmark.png" width="520" alt="부산이음길 워드마크" />

<br/><br/>

**부산의 경사, 계단, 단차, 보도 폭, 장애물 정보를 함께 보고**  
**이동 약자가 실제로 지나갈 수 있는 길을 찾도록 돕는 인클루시브 무장애 길찾기 서비스**

<br/>

![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![Java 21](https://img.shields.io/badge/Java_21-437291?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![PostGIS](https://img.shields.io/badge/PostgreSQL%2FPostGIS-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-D82C20?style=flat-square&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)
![AWS](https://img.shields.io/badge/AWS-232F3E?style=flat-square&logo=amazonaws&logoColor=white)

</div>

---

## | 서비스 소개

부산은 산복도로, 언덕, 계단, 좁은 보도, 보도 단절 구간이 많은 도시입니다. 일반 보행자에게는 돌아가면 되는 길도 휠체어 사용자에게는 막힌 길이 되고, 저시력자에게는 위험한 길이 될 수 있습니다.

**부산이음길**은 최단거리보다 실제 이동 가능성과 안전성을 우선합니다. 저시력자와 보행약자의 정보 접근 방식이 다르다는 점을 전제로, 사용자 유형에 따라 다른 화면 흐름과 안내 방식을 제공합니다.

<br/>

| 핵심 관점 | 설명 |
|----------|------|
| **Inclusive Design** | 사용자가 서비스에 맞추는 것이 아니라, 서비스가 사용자의 이동 조건과 정보 접근 방식에 맞춰집니다. |
| **Barrier-Free Route** | 경사, 계단, 보도 폭, 노면, 접근성 시설을 경로 판단에 반영합니다. |
| **Community Report** | 공사, 장애물, 점자블록 손상 같은 현장 변화를 시민 제보와 관리자 검토로 보강합니다. |
| **Busan Context** | 부산의 지형과 생활권 특성을 고려한 지역 특화 길찾기를 목표로 합니다. |

---

## | 팀 소개

## 이길,지도

<table>
  <tr>
    <td align="center">
      <b>김지윤</b><br/>
      <sub>팀장</sub><br/><br/>
      <img src="https://img.shields.io/badge/총괄-555555?style=flat-square" />
      <img src="https://img.shields.io/badge/FrontEnd-4285F4?style=flat-square" />
            <img src="https://img.shields.io/badge/디자인-E91E63?style=flat-square" />
      <img src="https://img.shields.io/badge/발표-9C27B0?style=flat-square" />
    </td>
    <td align="center">
      <b>김응서</b><br/>
      <sub>팀원</sub><br/><br/>
      <img src="https://img.shields.io/badge/Backend-6DB33F?style=flat-square" />
      <img src="https://img.shields.io/badge/Infra-FF9900?style=flat-square" />
    </td>
    <td align="center">
      <b>박세홍</b><br/>
      <sub>팀원</sub><br/><br/>
      <img src="https://img.shields.io/badge/FrontEnd-4285F4?style=flat-square" />
    </td>
    <td align="center">
      <b>백수연</b><br/>
      <sub>팀원</sub><br/><br/>
      <img src="https://img.shields.io/badge/AI-3776AB?style=flat-square" />
            <img src="https://img.shields.io/badge/FrontEnd-4285F4?style=flat-square" />
    </td>
    <td align="center">
      <b>유준호</b><br/>
      <sub>팀원</sub><br/><br/>
                  <img src="https://img.shields.io/badge/Infra-FF9900?style=flat-square" />
                        <img src="https://img.shields.io/badge/Backend-6DB33F?style=flat-square" />
    </td>
        <td align="center">
      <b>이재호</b><br/>
      <sub>팀원</sub><br/><br/>
      <img src="https://img.shields.io/badge/FrontEnd-4285F4?style=flat-square" />
            <img src="https://img.shields.io/badge/Docs-437291?style=flat-square" />
    </td>
        </td>
        <td align="center">
      <b>장주윤</b><br/>
      <sub>팀원</sub><br/><br/>
            <img src="https://img.shields.io/badge/Backend-6DB33F?style=flat-square" />
      <img src="https://img.shields.io/badge/AI-3776AB?style=flat-square" />
    </td>
  </tr>
</table>

---

## | 인클루시브 디자인

<table>
  <tr>
    <td align="center" width="25%">
      <strong>처음부터 포함</strong><br/><br/>
      <sub>온보딩에서 저시력자와 보행약자를 분리하고, 보행약자 세부 유형을 선택합니다.</sub>
    </td>
    <td align="center" width="25%">
      <strong>같은 목적, 다른 접근</strong><br/><br/>
      <sub>보행약자는 지도 중심, 저시력자는 큰 버튼과 음성 중심의 별도 화면군을 사용합니다.</sub>
    </td>
    <td align="center" width="25%">
      <strong>여러 감각으로 전달</strong><br/><br/>
      <sub>색상만 쓰지 않고 텍스트, 아이콘, 배지, 음성, TalkBack 라벨로 상태를 전달합니다.</sub>
    </td>
    <td align="center" width="25%">
      <strong>안전 우선</strong><br/><br/>
      <sub>최단거리 외에 경사와 장애물을 고려한 안전한 길을 제공합니다.</sub>
    </td>
  </tr>
</table>

---

## | 기술 스택

![부산이음길 아키텍처](<부산이음길.drawio (2).png>)

<table>
  <tr>
    <th align="center">Category</th>
    <th align="center">Stack</th>
  </tr>
  <tr>
    <td align="center"><strong>Android</strong></td>
    <td align="center">
      <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
      <img src="https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
      <img src="https://img.shields.io/badge/Material_3-757575?style=for-the-badge&logo=materialdesign&logoColor=white" alt="Material 3" />
      <img src="https://img.shields.io/badge/Kakao_Map-FFCD00?style=for-the-badge&logo=kakao&logoColor=000000" alt="Kakao Map" />
    </td>
  </tr>
  <tr>
    <td align="center"><strong>Backend</strong></td>
    <td align="center">
      <img src="https://img.shields.io/badge/Java_21-437291?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
      <img src="https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot" />
      <img src="https://img.shields.io/badge/Spring_Security-3A8D3A?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security" />
      <img src="https://img.shields.io/badge/JPA-59666C?style=for-the-badge" alt="JPA" />
    </td>
  </tr>
  <tr>
    <td align="center"><strong>Data / Routing</strong></td>
    <td align="center">
      <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
      <img src="https://img.shields.io/badge/PostGIS-336791?style=for-the-badge" alt="PostGIS" />
      <img src="https://img.shields.io/badge/Redis-D82C20?style=for-the-badge&logo=redis&logoColor=white" alt="Redis" />
      <img src="https://img.shields.io/badge/GraphHopper-77B829?style=for-the-badge" alt="GraphHopper" />
    </td>
  </tr>
  <tr>
    <td align="center"><strong>External</strong></td>
    <td align="center">
      <img src="https://img.shields.io/badge/Kakao_Local-FFCD00?style=for-the-badge&logo=kakao&logoColor=000000" alt="Kakao Local" />
      <img src="https://img.shields.io/badge/ODsay-1E88E5?style=for-the-badge" alt="ODsay" />
      <img src="https://img.shields.io/badge/Busan_BIMS-005BAC?style=for-the-badge" alt="Busan BIMS" />
      <img src="https://img.shields.io/badge/S3%2FMinIO-C72E49?style=for-the-badge&logo=minio&logoColor=white" alt="S3 MinIO" />
    </td>
  </tr>
  <tr>
    <td align="center"><strong>Admin / AI</strong></td>
    <td align="center">
      <img src="https://img.shields.io/badge/React_19-61DAFB?style=for-the-badge&logo=react&logoColor=000000" alt="React" />
      <img src="https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white" alt="Vite" />
      <img src="https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white" alt="Python" />
      <img src="https://img.shields.io/badge/Flask-000000?style=for-the-badge&logo=flask&logoColor=white" alt="Flask" />
    </td>
  </tr>
  <tr>
    <td align="center"><strong>Infra</strong></td>
    <td align="center">
      <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
      <img src="https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white" alt="Nginx" />
      <img src="https://img.shields.io/badge/Jenkins-D24939?style=for-the-badge&logo=jenkins&logoColor=white" alt="Jenkins" />
      <img src="https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white" alt="AWS EC2" />
    </td>
  </tr>
</table>

<br/>

---

## | 시스템 아키텍처

```text
Android App        Admin Web
     |                 |
     +-------> Spring Boot API
                 |
                 +-> PostgreSQL/PostGIS
                 +-> Redis
                 +-> GraphHopper Runtime
                 +-> S3 또는 MinIO
                 +-> Kakao Local / ODsay / Busan BIMS
                 +-> AI Flask Intent Server
```

| 구성 요소 | 설명 |
|-----------|------|
| **Android App** | 사용자 유형별 홈, 접근성 지도, 경로 탐색, 제보, 북마크 흐름을 제공합니다. |
| **Backend** | 인증, 장소, 경로, 제보, 관리자 API를 담당하고 외부 API와 공간 데이터를 조합합니다. |
| **Routing / Data** | PostGIS 공간 데이터와 GraphHopper 라우팅 그래프를 사용해 이동 가능성을 판단합니다. |
| **Admin Web** | 도로/시설 편집, 제보 검토, GraphHopper 반영 흐름을 운영합니다. |
| **Infra** | EC2 2대 구조, RDS, ElastiCache, Jenkins, Docker Compose, Nginx 기반으로 운영합니다. |

---

## | MVP 기능

<table>
  <tr>
    <td align="center" width="33%" valign="top">
      <video src="Docs/media/onboarding.mp4" width="220" height="390" controls muted playsinline></video><br/><br/>
      <strong>사용자 유형 온보딩</strong><br/><br/>
      <sub>저시력자와 보행약자를 구분하고 이동 특성에 맞는 앱 흐름으로 진입합니다.</sub>
    </td>
    <td align="center" width="33%" valign="top">
      <video src="Docs/media/low-vision.mp4" width="220" height="390" controls muted playsinline></video><br/><br/>
      <strong>저시력자 전용 흐름</strong><br/><br/>
      <sub>큰 버튼, 단순한 선택지, 음성 중심 안내로 저시력자에게 맞는 흐름을 제공합니다.</sub>
    </td>
    <td align="center" width="33%" valign="top">
      <video src="Docs/media/font-size.mp4" width="220" height="390" controls muted playsinline></video><br/><br/>
      <strong>글씨 크기 설정</strong><br/><br/>
      <sub>사용자의 시야와 읽기 편의에 맞춰 앱의 텍스트 크기를 조절합니다.</sub>
    </td>
  </tr>
  <tr>
    <td align="center" width="33%" valign="top">
      <video src="Docs/media/route-search.mp4" width="220" height="390" controls muted playsinline></video><br/><br/>
      <strong>무장애 경로 안내</strong><br/><br/>
      <sub>안전한 길과 최단거리를 비교하고 경사, 계단, 방향 안내를 제공합니다.</sub>
    </td>
    <td align="center" width="33%" valign="top">
      <video src="Docs/media/bookmark.mp4" width="220" height="390" controls muted playsinline></video><br/><br/>
      <strong>장소·경로 북마크</strong><br/><br/>
      <sub>자주 가는 장소와 경로를 저장하고 다시 길찾기로 연결합니다.</sub>
    </td>
    <td align="center" width="33%" valign="top">
      <video src="Docs/media/report.mp4" width="220" height="390" controls muted playsinline></video><br/><br/>
      <strong>장애물 제보</strong><br/><br/>
      <sub>공사, 계단, 점자블록 손상 등 현장 정보를 제보하고 검토 후 지도에 반영합니다.</sub>
    </td>
  </tr>
</table>

---

## | 저장소 구조

```text
.
├── FE/                         # Android 앱
│   ├── app/                    # 앱 코드 및 리소스
│   ├── docs/                   # FE 설계, QA, 디버깅 문서
│   └── mockup/                 # 화면 시안 및 목업
├── BE/                         # Spring Boot 백엔드
│   ├── src/main/java/          # 도메인별 API, 서비스, 공통 설정
│   ├── src/main/resources/     # application.yml, profile 설정
│   └── docs/                   # BE 기술 문서
├── ADMIN/                      # React/Vite 관리자 웹
├── AI/                         # Flask intent server와 음성/모델 실험 자산
├── Docs/                       # PRD, 요구사항, API, ERD, 인프라, 기획 문서
├── INF/                        # AWS, Jenkins, monitoring, Terraform 운영 설정
├── exec/                       # 제출/포팅 산출물
├── scripts/                    # 자동화 스크립트
├── docker-compose.*.yml        # local / dev / prod 실행 구성
└── Makefile                    # 실행 진입점
```

---

## | 문서 바로가기

### Overview

<table>
  <tr>
    <th align="left" bgcolor="#f0f0f0">문서</th>
    <th align="left" bgcolor="#f0f0f0">경로</th>
  </tr>
  <tr bgcolor="#f8f8f8">
    <td>Frontend README</td>
    <td><a href="FE/README.md">FE/README.md</a></td>
  </tr>
  <tr>
    <td>Backend README</td>
    <td><a href="BE/README.md">BE/README.md</a></td>
  </tr>
  <tr bgcolor="#f8f8f8">
    <td>Infra README</td>
    <td><a href="INF/README.md">INF/README.md</a></td>
  </tr>
  <tr>
    <td>포팅 매뉴얼</td>
    <td><a href="exec/부산이음길__포팅매뉴얼.md">exec/부산이음길__포팅매뉴얼.md</a></td>
  </tr>
</table>

### Service

| 문서 | 경로 |
|------|------|
| 프로젝트 기획서 | [Docs/기획/2026-04-10 최종_프로젝트_기획서.md](<Docs/기획/2026-04-10 최종_프로젝트_기획서.md>) |
| PRD | [Docs/PRD/2026-04-09_부산이음길_PRD.md](Docs/PRD/2026-04-09_부산이음길_PRD.md) |
| 요구사항명세서 | [Docs/PRD/2026-05-20_요구사항명세서.md](Docs/PRD/2026-05-20_요구사항명세서.md) |

### Architecture / API

| 문서 | 경로 |
|------|------|
| ERD | [Docs/ERD/ERD_v4.md](Docs/ERD/ERD_v4.md) |
| API 전체 목록 | [Docs/API/2026-04-12_API_전체_목록.md](Docs/API/2026-04-12_API_전체_목록.md) |
| 경로 API 명세 | [Docs/API/길안내_도메인/2026-05-06_경로_API_명세.md](Docs/API/길안내_도메인/2026-05-06_경로_API_명세.md) |

---

## | 실행 및 상세 안내

| 구분 | 안내 | 경로 |
|------|------|------|
| Frontend | Android 앱 실행 및 빌드 가이드 | [FE/README.md](FE/README.md) |
| Backend | 백엔드 실행 및 환경 변수 가이드 | [BE/README.md](BE/README.md) |
| Infra | 운영 설정 자산 기준 | [INF/README.md](INF/README.md) |
| Porting | 제출/포팅 운영 매뉴얼 | [exec/부산이음길__포팅매뉴얼.md](exec/부산이음길__포팅매뉴얼.md) |
