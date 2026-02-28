# Architecture & Tech Stack

## 1. 프로젝트 개요
Spring Boot 기반의 멀티 모듈 프로젝트로, 게시판, 블로그, 주식 관리 등의 기능을 제공합니다.

## 2. 기술 스택 버전 (2026-02-04 기준)
- **Java**: 25
- **Spring Boot**: 4.0.2
- **Spring Cloud**: 2025.1.1

## 3. 아키텍처 원칙

### 세션 및 인증 아키텍처 (2025-02-25 업데이트)
**핵심 원칙**: `bluesky-api-user`가 세션 저장소(Redis)를 독점 관리하며, 웹 모듈은 API를 통해 세션을 공유합니다.

#### 1) 세션 관리 (Session Management)
- **중앙 집중식 세션**: `bluesky-api-user`만 `spring-session-data-redis` 의존성을 가집니다.
- **웹 모듈 (Client)**:
  - `bluesky-web-*` 모듈은 Redis에 직접 접근하지 않습니다.
  - `bluesky-client-user` 라이브러리의 `ApiSessionRepository`를 사용하여 세션을 관리합니다.
  - **ApiSessionRepository**: 세션 객체를 직렬화(Base64)하여 `bluesky-api-user`의 REST API(`UserInfoApiClient`)로 전송/조회합니다.
- **쿠키 공유**: 모든 모듈은 `BLUESKY_SESSION` 쿠키를 공유합니다. (Domain: `bluesky.local`)

#### 2) OAuth2 로그인 흐름
1. **로그인 시작**: `bluesky-web-gate`에서 `/login/redirect?redirectUrl=...` 호출
2. **리다이렉트 처리**: `LoginRedirectController`가 `redirectUrl`을 세션에 저장하고 `/login`으로 리다이렉트
3. **인증 위임**: `bluesky-web-user`가 OAuth2 Provider(GitHub 등)와 통신
4. **세션 동기화**: 인증 완료 후 `ApiSessionRepository`가 세션을 직렬화하여 `bluesky-api-user`로 전송
5. **로그인 완료**: `OAuth2LoginSuccessHandler`가 세션의 `redirectUrl`로 사용자 이동

#### 3) bluesky-api-user (세션 서버)
- 역할: 세션 데이터 물리적 저장(Redis) 및 조회
- API:
  - `POST /api/user/session`: 세션 생성/저장
  - `GET /api/user/session/{sessionId}`: 세션 조회
  - `DELETE /api/user/session/{sessionId}`: 세션 삭제

## 4. 모듈 구조

### bluesky-parent/
- **bluesky-api/**: 백엔드 API 서버 (user, board, blog, stock, bookkeeping)
- **bluesky-web/**: 프론트엔드 웹 서버 (gate, default, dynamiccrud, common)
- **bluesky-batch/**: 배치 작업
- **bluesky-app/**: 애플리케이션 로직
- **bluesky-test/**: 테스트 모듈

## 5. 데이터베이스
### PostgreSQL
- 기본 데이터베이스 (연결 정보는 Config Server 관리)

### 주요 테이블 Schema
#### oauth2_authorized_client (bluesky-api-user)
- GitHub/Kakao Access Token 저장
- PK: (client_registration_id, principal_name)

#### UserInfo (bluesky-api-user)
- 사용자 기본 정보
- Columns: id(UUID), username, password

## 6. JTE (Java Template Engine) 설정 전략
- **기본 환경 (k8sdev, 프로덕션 등)**: gg.jte.usePrecompiledTemplates=true (사전 컴파일된 클래스 파일 사용)
- **로컬 개발 환경 (localdev 프로파일)**: gg.jte.developmentMode=true 설정으로 실시간 템플릿 컴파일 및 새로고침 반영. gg.jte.usePrecompiledTemplates=false 병행 필수.
- **에러 인지**: developmentMode=true가 누락된 채 usePrecompiledTemplates=false만 설정되면 Spring Boot 시작 시 Failed to instantiate [gg.jte.TemplateEngine] 오류 발생.
