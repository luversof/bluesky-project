# Bluesky Project - GitHub Copilot Instructions

1. 모든 답변은 현재 프로젝트의 스프링부트, 라이브러리 버전을 기준으로 합니다. (이전 버전 기준 금지)
2. 모든 답변은 한국어로 합니다.

이 문서는 GitHub Copilot이 프로젝트의 아키텍처와 코딩 규칙을 이해하고 일관된 코드를 생성하도록 돕습니다.

## 프로젝트 개요

Spring Boot 기반의 멀티 모듈 프로젝트로, 게시판, 블로그, 주식 관리 등의 기능을 제공합니다.

## 아키텍처 원칙

### 세션 및 인증 아키텍처 (2025-02-25 업데이트)

**핵심 원칙**: `bluesky-api-user`가 세션 저장소(Redis)를 독점 관리하며, 웹 모듈은 API를 통해 세션을 공유합니다.

#### 1. 세션 관리 (Session Management)

- **중앙 집중식 세션**: `bluesky-api-user`만 `spring-session-data-redis` 의존성을 가집니다.
- **웹 모듈 (Client)**:
  - `bluesky-web-*` 모듈은 Redis에 직접 접근하지 않습니다.
  - `bluesky-client-user` 라이브러리의 `ApiSessionRepository`를 사용하여 세션을 관리합니다.
  - **ApiSessionRepository**: 세션 객체를 직렬화(Base64)하여 `bluesky-api-user`의 REST API(`UserInfoApiClient`)로 전송/조회합니다.
- **쿠키 공유**:
  - 모든 모듈은 `BLUESKY_SESSION` 쿠키를 공유합니다.
  - Domain: `bluesky.local` (로컬 개발 기준)

#### 2. OAuth2 로그인 흐름

1. **로그인 시작**: `bluesky-web-gate`에서 `/login/redirect?redirectUrl=...` 호출
2. **리다이렉트 처리**: `LoginRedirectController`가 `redirectUrl`을 세션에 저장하고 `/login`으로 리다이렉트
3. **인증 위임**: `bluesky-web-user`가 OAuth2 Provider(GitHub 등)와 통신
4. **세션 동기화**: 인증 완료 후 `OAuth2AuthorizedClient` 등이 세션에 저장되면, `ApiSessionRepository`가 이를 직렬화하여 `bluesky-api-user`로 전송
5. **로그인 완료**: `OAuth2LoginSuccessHandler`가 세션의 `redirectUrl`로 사용자 이동

#### 3. bluesky-api-user (세션 서버)

- **역할**: 세션 데이터의 물리적 저장(Redis) 및 조회 담당
- **API**:
  - `POST /api/user/session`: 세션 생성/저장 (직렬화된 속성 포함)
  - `GET /api/user/session/{sessionId}`: 세션 조회
  - `DELETE /api/user/session/{sessionId}`: 세션 삭제

## 모듈 구조

### bluesky-parent/

- **bluesky-api/**: 백엔드 API 서버

  - bluesky-api-user: 사용자 인증 (OAuth2 Authorization Server)
  - bluesky-api-board: 게시판
  - bluesky-api-blog: 블로그
  - bluesky-api-stock: 주식
  - bluesky-api-bookkeeping: 가계부

- **bluesky-web/**: 프론트엔드 웹 서버

  - bluesky-web-gate: 공용 Gateway
  - bluesky-web-default: 기본 웹
  - bluesky-web-dynamiccrud: 동적 CRUD
  - bluesky-web-common: 공통 모듈

- **bluesky-batch/**: 배치 작업
- **bluesky-app/**: 애플리케이션 로직
- **bluesky-test/**: 테스트 모듈

## 패키지 구조 규칙

### bluesky-web-\* 모듈

```
net.luversof.web.{module}/
├── {domain}/                    # 도메인별 패키지 (board, user, blog, stock 등)
│   ├── controller/              # 컨트롤러
│   ├── domain/                  # 도메인 모델 (DTO, VO)
│   └── openfeign/               # Feign Client (백엔드 API 호출)
│       └── {Domain}Client.java
├── config/                      # 설정 클래스
└── Application.java
```

**예시**:

- `net.luversof.web.gate.board.openfeign.BoardClient`
- `net.luversof.web.gate.user.openfeign.UserApiClient`
- `net.luversof.web.gate.stock.controller.StockController`

### bluesky-api-\* 모듈

```
net.luversof.api.{module}/
├── config/                      # Security, JPA 등 설정
├── controller/                  # REST API 컨트롤러
├── service/                     # 비즈니스 로직
├── repository/                  # JPA Repository
├── domain/                      # Entity, DTO
└── Application.java
```

## 코딩 스타일

### 들여쓰기

- **탭 사용** (스페이스 아님)
- 모든 Java 파일에서 일관되게 탭 사용

### Spring Bean 설정

- 생략 가능한 기본 설정은 제거
- 예: `Customizer.withDefaults()`, 기본 URL 등
- 필수 설정만 명시적으로 작성

### RequestParam

- `@RequestParam` 사용 시 변수명과 파라미터명이 일치하면 속성값 생략
- ❌ `@RequestParam("name") String name`
- ✅ `@RequestParam String name`

### Security 설정 예시

```java
// ❌ 불필요한 설정
.logout(logout -> logout
    .logoutSuccessUrl("/")  // 기본값
    .permitAll())           // 기본값
.csrf(Customizer.withDefaults())  // 기본값

// ✅ 필수 설정만
.logout(logout -> logout.permitAll())
```

### Import 정리

- 사용하지 않는 import 제거
- 스프링 프레임워크 import 먼저, 서드파티, 프로젝트 순서

## 데이터베이스

### PostgreSQL

- 기본 데이터베이스
- 연결 정보는 Config Server에서 관리

### 주요 테이블

#### oauth2_authorized_client (bluesky-api-user)

```sql
CREATE TABLE oauth2_authorized_client (
  client_registration_id varchar(100) NOT NULL,
  principal_name varchar(200) NOT NULL,
  access_token_type varchar(100) NOT NULL,
  access_token_value bytea NOT NULL,
  access_token_issued_at timestamp NOT NULL,
  access_token_expires_at timestamp NOT NULL,
  access_token_scopes varchar(1000) DEFAULT NULL,
  refresh_token_value bytea DEFAULT NULL,
  refresh_token_issued_at timestamp DEFAULT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (client_registration_id, principal_name)
);
```

- GitHub/Kakao 로그인으로 받은 Access Token 저장
- api-user의 JdbcOAuth2AuthorizedClientService가 관리

#### UserInfo (bluesky-api-user)

```sql
CREATE TABLE "UserInfo" (
  "id" UUID NOT NULL PRIMARY KEY,
  "username" VARCHAR(50) NOT NULL,
  "password" VARCHAR(200)
);
```

- 사용자 기본 정보
- OAuth2 로그인 사용자도 여기 저장

## 프론트엔드

### Tailwind CSS + daisyUI

- Tailwind CSS 4.1.17
- daisyUI 5.4.7
- CSS 빌드: `npm run build` (bluesky-web-gate/src/main/frontend)

### TypeScript

- 빌드: `npx tsc` (bluesky-web-gate/src/main/frontend)
- `tsconfig.json`에 설정된 대로 `src/`의 TS 파일을 `../resources/static/js/`로 컴파일합니다.

### HTMX

- HTMX 기반 인터랙션
- Server-Side Rendering

### UI 수정 시 빌드 필수

- `bluesky-web-gate`의 UI(HTML, CSS, JS/TS)를 수정한 경우, 반드시 프론트엔드 빌드를 수행해야 변경사항이 반영됩니다.
- 터미널 경로: `bluesky-project/bluesky-parent/bluesky-web/bluesky-web-gate/src/main/frontend`
- 명령어: `npm run build`

## Config Server

- GitHub 기반 설정 저장소
- URL: https://raw.githubusercontent.com/luversof/bluesky-config-repo/develop/
- OAuth2 Client 정보 (GitHub/Kakao Client ID/Secret) 관리

## 개발 환경

### Profile

- `localdev`: 로컬 개발 (포트 30xxx)
- `opdev`: 개발 서버 (포트 40xxx)

### 주요 URL

- Gate: http://localhost:30122
- api-user: https://user.api.bluesky.local:30131 (내부 API)

## 테스트 실행 메모

- `bluesky-api-stock`의 `DividendTest`처럼 실제 데이터소스/Config Server를 그대로 쓰는 통합 테스트는 **항상 `SPRING_PROFILES_ACTIVE=localdev`** 로 구동한다.
- Powershell 예시:

```powershell
$env:SPRING_PROFILES_ACTIVE='localdev'
mvn -q -pl bluesky-api/bluesky-api-stock -DskipITs test -Dtest=DividendTest
Remove-Item Env:SPRING_PROFILES_ACTIVE
```

- 테스트 리소스(`src/test/resources`)에 `application.properties`나 `application.yml`을 추가해 Config Server 호출을 우회하지 않는다. 프로덕션과 동일하게 Config Server에서 연결 정보를 내려받는 것을 기본 원칙으로 한다.

## 자주하는 실수 방지

### ❌ 하지 말아야 할 것

1. **웹 모듈에 Redis 의존성 추가** (`spring-session-data-redis`는 `bluesky-api-user`에만 존재해야 함)
2. Gate나 다른 웹 모듈에서 직접 OAuth2 토큰을 DB에 저장
3. **스페이스로 들여쓰기** (반드시 탭 사용)
4. **properties 파일에 한글 주석** (인코딩 깨짐)
5. 불필요한 기본 설정 추가
6. api-user를 외부에 노출
7. 도메인별 패키지 구조를 무시하고 client 패키지에 모든 Feign Client 모음

### ✅ 해야 할 것

1. 모든 OAuth2 토큰 저장은 api-user에서 처리
2. **반드시 탭(Tab)으로 들여쓰기**
3. **properties 파일은 영어 주석만 사용**
4. 필수 설정만 명시
5. Feign Client는 `{domain}/openfeign/` 패키지에 위치
6. Gate에서 로그인 성공 시 UserApiClient를 통해 api-user에 토큰 저장 요청

## Token Exchange Grant

GitHub Access Token을 자체 JWT로 변환하는 기능입니다.

### 요청 예시

```http
POST /oauth2/token
Authorization: Basic {client_credentials}
Content-Type: application/x-www-form-urlencoded

grant_type=urn:ietf:params:oauth:grant-type:token-exchange
&subject_token={github_access_token}
&subject_token_type=urn:ietf:params:oauth:token-type:access_token
```

### 응답

```json
{
  "access_token": "eyJhbGc...",
  "token_type": "Bearer",
  "expires_in": 7200
}
```

## 추가 참고 문서

- ARCHITECTURE_IMPROVEMENT_PLAN.md: 상세 아키텍처 개선 계획
- README.md: 프로젝트 개요 및 실행 방법

## Dividend 리스트 변경사항 및 Copilot 지침 (2025-11-19)

- 목적: `bluesky-web-gate`의 Dividend 목록을 `tradeProfit`와 유사하게 UI에 노출, UI/컨트롤러/API/도메인/테스트를 모두 정리합니다.
- 주요 변경점:
  - Web UI: `DividendView` 레코드를 `bluesky-web-gate`의 `stock.view` 패키지에 추가하여 `stockItemId`, `stockItemName` 등을 포함합니다.
  - Controller: `StockHtmxController.dividendList()`에서 반환된 Dividend 목록을 보여주며, `stockItemName`이 누락된 경우 `stockItemId`의 집합을 모아 한 번만 `StockItemClient`로 이름을 조회합니다.
  - API: `bluesky-api-stock`의 `DividendService.findDividends()`를 LEFT JOIN `StockItem` 하여 `si.name AS "stockItemName"`을 반환하도록 수정했습니다. SQL 컬럼 별칭을 도메인 필드에 맞도록 조정해야 합니다.
  - 도메인: `Dividend` 도메인에 `stockItemName`을 추가하되, Spring Data JDBC 매핑 문제를 피하기 위해 `@Transient`를 적용했습니다.
  - 테스트: `DividendTest`에 `selectAllDividends()`와 CSV import/데이터 무결성 테스트를 추가해 `stock_item_id`가 NULL인 행이 있는지 점검합니다.
- Copilot/개발자 지침:
  1. UI는 `dividend.stockItemName`을 우선 사용하고, 없으면 `stockItemId`로 조회한 값으로 대체하세요.
  2. 도메인에 로컬(비영속) 필드를 추가할 때는 `@Transient`를 사용해 DB 매핑 오류를 방지하세요.
  3. SQL을 변경할 때는 컬럼별칭(`AS`)을 도메인 필드에 맞도록 유지하세요.
  4. `StockHtmxController`는 N+1 호출을 피하도록 `stockItemId`의 유니크 집합을 한 번만 요청하는 방식을 사용하세요.
  5. 데이터 무결성을 확보하세요: `stock_item_id`가 NULL인 기존 row에 대한 마이그레이션 계획을 세우고, import 파이프라인에서 `stockItemId`가 반드시 채워지도록 검증 로직을 추가하세요.
  6. 통합 테스트는 `localdev` 프로파일로 실행하세요. 예: `mvn -Dspring.profiles.active=localdev -Dtest=net.luversof.api.stock.DividendTest#selectAllDividends test`.
