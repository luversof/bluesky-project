# Development Environment

## 1. 프론트엔드 (bluesky-web-gate)
- **Stack**: Tailwind CSS 4.1.17 + daisyUI 5.4.7 + TypeScript + HTMX
- **빌드 경로**: `bluesky-project/bluesky-parent/bluesky-web/bluesky-web-gate/src/main/frontend`
- **빌드 명령**: `npm run build`
- **주의**: UI(HTML, CSS, JS/TS) 수정 시 반드시 빌드를 수행해야 `resources/static`에 반영됨.

## 2. Config Server
- **저장소**: [bluesky-config-repo](https://raw.githubusercontent.com/luversof/bluesky-config-repo/develop/)
- **역할**: DB 연결 정보, OAuth2 Client ID/Secret 관리

## 3. 개발 프로파일 (Profile)
- `localdev`: 로컬 개발 (Port 30xxx)
- `opdev`: 개발 서버 (Port 40xxx)
- **주요 URL**:
  - Gate: http://localhost:30122
  - api-user: https://user.api.bluesky.local:30131

## 4. 테스트 실행 전략
- 통합 테스트 시 실제 데이터소스/Config Server 사용을 위해 **`SPRING_PROFILES_ACTIVE=localdev`** 필수.
- 테스트 리소스의 properties 파일로 Config Server를 우회하지 말 것(프로덕션 환경 모사).

### Powershell 실행 예시
```powershell
$env:SPRING_PROFILES_ACTIVE='localdev'
mvn -q -pl bluesky-api/bluesky-api-stock -DskipITs test -Dtest=DividendTest
Remove-Item Env:SPRING_PROFILES_ACTIVE
```

## 5. Token Exchange Grant
GitHub Access Token을 자체 JWT로 변환.
- **Request**:
```http
POST /oauth2/token
grant_type=urn:ietf:params:oauth:grant-type:token-exchange
&subject_token={github_access_token}
...
```
