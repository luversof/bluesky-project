# Bluesky Project - Environment & Development Instructions

## 데이터베이스

### PostgreSQL
- 기본 데이터베이스, 연결 정보는 Config Server에서 관리

### 주요 테이블
- `oauth2_authorized_client` (bluesky-api-user): GitHub/Kakao Access Token 저장
- `UserInfo` (bluesky-api-user): 사용자 기본 정보

## Config Server
- GitHub 기반 설정 저장소: https://raw.githubusercontent.com/luversof/bluesky-config-repo/develop/
- OAuth2 Client 정보 관리

## 개발 환경

### Profile
- `localdev`: 로컬 개발 (포트 30xxx)
- `opdev`: 개발 서버 (포트 40xxx)

### 주요 URL
- Gate: http://localhost:30122
- api-user: https://user.api.bluesky.local:30131

## 테스트 실행 메모
- 통합 테스트는 **항상 `SPRING_PROFILES_ACTIVE=localdev`** 로 구동
- `src/test/resources`에 `application.properties` 등을 통해 Config Server 호출을 우회하지 말 것.

## 자주하는 실수 방지

### ❌ 하지 말아야 할 것
1. 웹 모듈에 Redis 의존성 추가
2. Gate 등에서 OAuth2 토큰 직접 DB 저장
3. 스페이스 들여쓰기 (탭 사용 필수)
4. properties 파일 한글 주석
5. 불필요한 기본 설정 추가
6. api-user 외부 노출
7. 도메인 패키지 구조 무시

### ✅ 해야 할 것
1. 모든 OAuth2 토큰 저장은 api-user에서 처리
2. 반드시 탭(Tab)으로 들여쓰기
3. properties 파일 영어 주석
4. Feign Client는 `{domain}/openfeign/` 패키지에 위치
5. Gate 로그인 성공 시 UserApiClient 통해 토큰 저장

## 최근 변경 사항 (2025-11-19 Dividend 리스트)
- UI: `DividendView` 레코드 추가 (`stockItemId`, `stockItemName`)
- Controller: N+1 방지를 위해 `stockItemId` 집합으로 이름 일괄 조회
- API: `DividendService.findDividends()`에 `stockItemName` 조인 추가
- 도메인: `Dividend`에 `@Transient String stockItemName` 추가
- 테스트: `DividendTest` 무결성 검증 추가
