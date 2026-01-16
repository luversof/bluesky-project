# Bluesky Project - Copilot Instructions and Architecture

This memory summarizes the core principles, architecture, and coding rules for the Bluesky project, as defined in `.github/copilot-instructions.md`.

## 1. Project Overview & Architecture
- **Type**: Multi-module Spring Boot project (Board, Blog, Stock, Bookkeeping).
- **Session Architecture** (Updated 2025-02-25):
  - **`bluesky-api-user`** owns the Session Redis and manages `spring-session-data-redis`.
  - **Web Modules (`bluesky-web-*`)**: Do NOT access Redis directly. They use `ApiSessionRepository` from `bluesky-client-user` to send serialized session data to `bluesky-api-user` via REST API.
  - **Cookie**: `BLUESKY_SESSION` shared across `bluesky.local` domain.
  - **OAuth2 Flow**: `bluesky-web-gate` redirects -> `bluesky-web-user` handles OAuth2 with Provider -> `ApiSessionRepository` syncs session to `bluesky-api-user`.
- **Infrastructure**: Spring Cloud Config, Eureka, Gateway, Admin Server, MariaDB, Prometheus, Kubernetes.

## 2. Module Structure
- **bluesky-parent/bluesky-api/**: Backend API Servers (`api-user`, `api-board`, `api-stock`, etc.)
- **bluesky-parent/bluesky-web/**: Frontend Web Servers (`web-gate`, `web-default`, `web-dynamiccrud`)
- **bluesky-cloud/**: Infrastructure (Config Server, Eureka, Gateway, Docker/K8s).

## 3. Package Structure Rules
- **Web Modules** (`net.luversof.web.{module}`):
  - `{domain}/controller`, `{domain}/domain`, `{domain}/openfeign/{Domain}Client.java`
  - Example: `net.luversof.web.gate.board.openfeign.BoardClient`
- **API Modules** (`net.luversof.api.{module}`):
  - `config`, `controller`, `service`, `repository`, `domain`

## 4. Coding Style & Conventions
- **Indentation**: **MUST USE TABS**, not spaces.
- **Spring Beans**: Remove unnecessary defaults (e.g., `Customizer.withDefaults()`). Use clear, explicit config.
- **`@RequestParam`**: Omit name if it matches the variable (e.g., `@RequestParam String name`).
- **Imports**: Remove unused. Order: Spring Framework -> Third Party -> Project.
- **Properties Files**: **English comments only**. No Korean comments to avoid encoding issues.

## 5. Frontend Stack
- **Tech**: Tailwind CSS 4.1.17, daisyUI 5.4.7, HTMX, TypeScript.
- **Build**: `npm run build` for CSS, `npx tsc` for TS. TS files in `src` compile to `../resources/static/js/`.

## 6. Database & Config
- **DB**: PostgreSQL (primary), MariaDB (infra). Connection info from Config Server.
- **Config Server**: GitHub-based, URL `https://raw.githubusercontent.com/luversof/bluesky-config-repo/develop/`.
- **Profiles**: `localdev` (local), `opdev` (dev server).
- **Key Tables**: `oauth2_authorized_client`, `UserInfo` (both in `bluesky-api-user`).

## 7. Development & Testing
- **Local Dev URLs**: Gate `http://localhost:30122`, Api-User `https://user.api.bluesky.local:30131`.
- **Integration Tests**: RUN WITH `SPRING_PROFILES_ACTIVE=localdev`. DO NOT mock Config Server in `src/test/resources` properties; use the real Config Server.
- **Powershell Test Command**:
  ```powershell
  $env:SPRING_PROFILES_ACTIVE='localdev'
  mvn -q -pl bluesky-api/bluesky-api-stock -DskipITs test -Dtest=DividendTest
  Remove-Item Env:SPRING_PROFILES_ACTIVE
  ```

## 8. Specific Feature Instructions
- **Dividend List (Stock)**:
  - UI uses `dividend.stockItemName`. Fallback to `StockItemClient` lookup only if name missing.
  - Domain `Dividend` has `@Transient` `stockItemName`.
  - Avoid N+1 in controllers by fetching unique sets of IDs.
  - `Token Exchange`: Custom endpoint `POST /oauth2/token` exchanges GitHub token for internal JWT.

## 9. Common Mistakes to Avoid (❌)
- Adding Redis dependency to Web modules.
- Saving OAuth2 tokens in Gate/Web DBs (Must be `api-user`).
- Using Spaces for indentation.
- Korean comments in properties.
- Exposing `api-user` publicly.
