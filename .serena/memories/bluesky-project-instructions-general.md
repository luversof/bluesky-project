# Bluesky Project - General Instructions

1. 모든 답변은 현재 프로젝트의 스프링부트, 라이브러리 버전을 기준으로 합니다. (이전 버전 기준 금지)
2. 모든 답변은 한국어로 합니다.

## 프로젝트 개요

Spring Boot 기반의 멀티 모듈 프로젝트로, 게시판, 블로그, 주식 관리 등의 기능을 제공합니다.

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
├── {domain}/                    # 도메인별 패키지
│   ├── controller/              # 컨트롤러
│   ├── domain/                  # 도메인 모델
│   └── openfeign/               # Feign Client
│       └── {Domain}Client.java
├── config/                      # 설정 클래스
└── Application.java
```

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
- 생략 가능한 기본 설정은 제거 (예: `Customizer.withDefaults()`)
- 필수 설정만 명시적으로 작성

### RequestParam
- `@RequestParam` 사용 시 변수명과 파라미터명이 일치하면 속성값 생략
- ✅ `@RequestParam String name`

### Import 정리
- 사용하지 않는 import 제거
- 스프링 프레임워크 import 먼저, 서드파티, 프로젝트 순서

## 프론트엔드

### Tailwind CSS + daisyUI
- Tailwind CSS 4.1.17, daisyUI 5.4.7
- CSS 빌드: `npm run build` (bluesky-web-gate/src/main/frontend)

### TypeScript
- 빌드: `npx tsc` (bluesky-web-gate/src/main/frontend)
- `tsconfig.json`에 설정된 대로 `src/`의 TS 파일을 `../resources/static/js/`로 컴파일합니다.

### UI 수정 시 빌드 필수
- `bluesky-web-gate` UI 수정 시 `npm run build` 필수
