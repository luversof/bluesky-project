# Bluesky Project - Architecture Instructions

## 아키텍처 원칙 (2025-02-25 업데이트)

**핵심 원칙**: `bluesky-api-user`가 세션 저장소(Redis)를 독점 관리하며, 웹 모듈은 API를 통해 세션을 공유합니다.

### 1. 세션 관리 (Session Management)

- **중앙 집중식 세션**: `bluesky-api-user`만 `spring-session-data-redis` 의존성을 가집니다.
- **웹 모듈 (Client)**:
  - `bluesky-web-*` 모듈은 Redis에 직접 접근하지 않습니다.
  - `bluesky-client-user` 라이브러리의 `ApiSessionRepository`를 사용하여 세션을 관리합니다.
  - **ApiSessionRepository**: 세션 객체를 직렬화(Base64)하여 `bluesky-api-user`의 REST API(`UserInfoApiClient`)로 전송/조회합니다.
- **쿠키 공유**:
  - 모든 모듈은 `BLUESKY_SESSION` 쿠키를 공유합니다.
  - Domain: `bluesky.local` (로컬 개발 기준)

### 2. OAuth2 로그인 흐름

1. **로그인 시작**: `bluesky-web-gate`에서 `/login/redirect?redirectUrl=...` 호출
2. **리다이렉트 처리**: `LoginRedirectController`가 `redirectUrl`을 세션에 저장하고 `/login`으로 리다이렉트
3. **인증 위임**: `bluesky-web-user`가 OAuth2 Provider(GitHub 등)와 통신
4. **세션 동기화**: 인증 완료 후 `OAuth2AuthorizedClient` 등이 세션에 저장되면, `ApiSessionRepository`가 이를 직렬화하여 `bluesky-api-user`로 전송
5. **로그인 완료**: `OAuth2LoginSuccessHandler`가 세션의 `redirectUrl`로 사용자 이동

### 3. bluesky-api-user (세션 서버)

- **역할**: 세션 데이터의 물리적 저장(Redis) 및 조회 담당
- **API**:
  - `POST /api/user/session`: 세션 생성/저장
  - `GET /api/user/session/{sessionId}`: 세션 조회
  - `DELETE /api/user/session/{sessionId}`: 세션 삭제

### Token Exchange Grant

GitHub Access Token을 자체 JWT로 변환하는 기능입니다.

#### 요청 예시
```http
POST /oauth2/token
Authorization: Basic {client_credentials}
Content-Type: application/x-www-form-urlencoded

grant_type=urn:ietf:params:oauth:grant-type:token-exchange
&subject_token={github_access_token}
&subject_token_type=urn:ietf:params:oauth:token-type:access_token
```
