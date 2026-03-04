# Coding Guidelines & Standards

## 1. 코딩 스타일

### 들여쓰기 (Indentation)
- **탭(Tab) 사용** (스페이스 아님)
- 모든 Java 파일에서 일관되게 적용

### Spring Bean 설정
- 생략 가능한 기본 설정은 제거 (예: `Customizer.withDefaults()`)
- 필수 설정만 명시적으로 작성

### RequestParam
- 변수명과 파라미터명이 일치하면 속성값 생략
- ❌ `@RequestParam("name") String name`
- ✅ `@RequestParam String name`

### Security 설정
- 불필요한 체이닝 제거
```java
// ✅ 필수 설정만
.logout(logout -> logout.permitAll())
```

### Import 정리
- 순서: Spring Framework -> Third Party -> Project
- 사용하지 않는 import 제거

## 2. 패키지 구조 규칙

### bluesky-web-* 모듈
```
net.luversof.web.{module}/
├── {domain}/                    # 도메인별 패키지 (board, user 등)
│   ├── controller/              # 컨트롤러
│   ├── domain/                  # 도메인 모델 (DTO, VO)
│   └── openfeign/               # Feign Client
│       └── {Domain}Client.java
├── config/
└── Application.java
```

### bluesky-api-* 모듈
```
net.luversof.api.{module}/
├── config/
├── controller/
├── service/
├── repository/
├── domain/
└── Application.java
```

## 3. 자주하는 실수 방지 (Do's and Don'ts)

### ❌ 하지 말아야 할 것
1. 웹 모듈에 Redis 의존성 추가 (`api-user` 독점)
2. Gate 등 웹 모듈에서 직접 토큰 DB 저장
3. 스페이스로 들여쓰기
4. properties 파일에 한글 주석
5. 도메인별 패키지 구조 무시 (client 패키지에 몰아넣기 금지)
6. **Git 관련 작업(commit, push 등) 자동 실행 금지 (사용자가 직접 수행)**

### ✅ 해야 할 것
1. 모든 OAuth2 토큰 저장은 `api-user` 위임
2. **반드시 탭(Tab) 들여쓰기**
3. properties 파일 영문 주석
4. Feign Client는 `{domain}/openfeign/` 패키지 위치

## 4. 모듈간 통신 룰 (Microservice Routing)
- 프론트엔드(luesky-web-*)에서 백엔드(luesky-api-*)를 호출할 때는 직접적인 HTTP 호출 로직 하드코딩을 지양합니다.
- 대신 **Spring HTTP Interfaces (@HttpExchange)**를 사용합니다.
- 프론트엔드 컨트롤러는 클라이언트의 요건을 받고, 주입받은 Client 인터페이스를 호출하여 API를 대신 호출하는 Reverse Proxy 형태를 띕니다.

