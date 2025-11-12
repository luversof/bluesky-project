# [BlueskyProject]

## 아키텍처 개선 - Spring Authorization Server 도입

프로젝트에 Spring Authorization Server를 도입하여 표준 OAuth 2.0 / OpenID Connect 인증 구조로 전환했습니다.

### 모듈 구조

- **bluesky-authorization-server**: 중앙 인증 서버 (포트 30140/40140)
  - OAuth 2.0 Authorization Server
  - OpenID Connect Provider
  - JWT 토큰 발급
  - 소셜 로그인 통합 (Google, GitHub)

- **bluesky-web-gate**: OAuth2 Client (포트 30122)
  - Authorization Code Flow
  - JWT 기반 인증

- **bluesky-api-***: Resource Server
  - JWT 토큰 검증
  - Scope 기반 권한 제어

### 실행 방법

1. **데이터베이스 스키마 생성**
   ```sql
   -- oauth2-authorization-server-schema.sql 실행
   psql -U postgres -d bluesky -f oauth2-authorization-server-schema.sql
   ```

2. **Authorization Server 실행**
   ```bash
   run-authorization-server.bat
   ```
   접속: https://auth.bluesky.local:40140

3. **Web Gateway 실행**
   ```bash
   run-bluesky-web-default.bat
   ```

### 인증 흐름

1. 사용자가 bluesky-web-gate 접속
2. 인증 필요 시 Authorization Server로 리다이렉트
3. 로그인 (폼 로그인 or 소셜 로그인)
4. Authorization Code 발급
5. Web Gateway가 Code를 Token으로 교환
6. JWT Access Token 획득
7. API 호출 시 Token 전달
8. Resource Server가 Token 검증

### 주요 변경사항

- ✅ Spring Authorization Server 기반 중앙 인증
- ✅ JWT 기반 stateless 인증
- ✅ OAuth 2.0 / OpenID Connect 표준 준수
- ✅ 소셜 로그인 통합 (Google, GitHub)
- ✅ Scope 기반 권한 제어
- 🔄 Resource Server 전환 (진행 중)
- 🔄 기존 세션 기반 인증 마이그레이션 (진행 중)

자세한 내용은 [ARCHITECTURE_IMPROVEMENT_PLAN.md](ARCHITECTURE_IMPROVEMENT_PLAN.md) 참조

