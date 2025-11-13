# [BlueskyProject]

## 아키텍처 - OAuth2 소셜 로그인

GitHub, Kakao 등의 OAuth2 Provider를 통한 소셜 로그인을 지원합니다.

### 모듈 구조

- **bluesky-api-user**: 사용자 인증 및 토큰 관리
  - OAuth2 Client (GitHub, Kakao 로그인)
  - Token Exchange Grant (GitHub 토큰 → JWT 변환)
  - 사용자 정보 관리

- **bluesky-web-gate**: 공용 Gateway (포트 30122)
  - OAuth2 Login 페이지
  - 정적 리소스 제공

- **bluesky-api-***: API 서버
  - JWT 토큰 검증
  - 권한 기반 접근 제어

### 실행 방법

1. **데이터베이스 스키마 생성**
   ```sql
   -- OAuth2 Client 토큰 저장용 테이블
   psql -U postgres -d bluesky -f bluesky-parent/bluesky-api/bluesky-api-user/src/main/java/net/luversof/api/user/schema-create-postgresql-user.sql
   ```

2. **애플리케이션 실행**
   ```bash
   run-bluesky-web-default.bat
   ```
   접속: http://localhost:30122

### OAuth2 로그인 흐름

1. 사용자가 로그인 페이지 접속
2. GitHub/Kakao 버튼 클릭
3. OAuth2 Provider로 리다이렉트
4. 사용자 인증 및 권한 동의
5. Authorization Code 발급
6. Access Token 교환 및 DB 저장 (oauth2_authorized_client 테이블)
7. 로그인 완료

### 주요 기능

- ✅ OAuth2 소셜 로그인 (GitHub, Kakao)
- ✅ Token Exchange Grant (GitHub 토큰 → JWT)
- ✅ 로그인 토큰 DB 저장 (oauth2_authorized_client)
- ✅ JWT 기반 API 인증
- 🔄 게시판 작성자 정보 연동 (진행 중)

자세한 내용은 [ARCHITECTURE_IMPROVEMENT_PLAN.md](ARCHITECTURE_IMPROVEMENT_PLAN.md) 참조

