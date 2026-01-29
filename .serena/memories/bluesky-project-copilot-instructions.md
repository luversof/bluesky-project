# Bluesky Project - GitHub Copilot Instructions

1.  **Version Standard**: All answers must use current Spring Boot and library versions. Legacy versions are prohibited.
2.  **Language**: All answers must be in Korean.

## Architecture Principles

### Session & Auth (Updated 2025-02-25)
*   **Centralized Session**: Only `bluesky-api-user` manages Redis sessions (`spring-session-data-redis`).
*   **Web Modules (`bluesky-web-*`)**:
    *   Do NOT access Redis directly.
    *   Use `ApiSessionRepository` from `bluesky-client-user` library.
    *   `ApiSessionRepository` serializes session objects (Base64) and talks to `bluesky-api-user` via REST API (`UserInfoApiClient`).
*   **Cookie**: Shared `BLUESKY_SESSION` cookie with domain `bluesky.local`.
*   **OAuth2 Flow**:
    1.  `bluesky-web-gate`: `/login/redirect` -> `LoginRedirectController` saves `redirectUrl` in session.
    2.  `bluesky-web-user`: Delegates auth to Provider (GitHub, etc.).
    3.  `ApiSessionRepository`: Syncs session to `bluesky-api-user` via REST.
    4.  `OAuth2LoginSuccessHandler`: Redirects user to saved `redirectUrl`.
*   **bluesky-api-user (Session Server)**:
    *   Physically stores session in Redis.
    *   API: POST/GET/DELETE `/api/user/session`.

## Module Structure
*   **bluesky-parent/**
    *   **bluesky-api/**: Backend APIs (`user`, `board`, `blog`, `stock`, `bookkeeping`).
    *   **bluesky-web/**: Frontend Web Servers (`gate`, `default`, `dynamiccrud`, `common`).
    *   **bluesky-batch/**: Batch jobs.
    *   **bluesky-app/**: Application logic.
    *   **bluesky-test/**: Test modules.

## Package Structure Rules
### bluesky-web-*
`net.luversof.web.{module}.{domain}.{controller|domain|openfeign}`
*   Example: `net.luversof.web.gate.board.openfeign.BoardClient`

### bluesky-api-*
`net.luversof.api.{module}.{config|controller|service|repository|domain}`

## Coding Style
*   **Indentation**: **Tab** (not spaces) in all Java files.
*   **Spring Bean Config**: Remove defaults (e.g., `Customizer.withDefaults()`, default URLs). Write only explicit setups.
*   **RequestParam**: Use `@RequestParam String name` if variable name matches parameter name.
*   **Security Config**: Minimize config strictly to what is needed.
*   **Imports**: Remove unused. Order: Spring Framework -> Third-party -> Project.

## Database
*   **PostgreSQL**: Default DB. Connection info from Config Server.
*   **Tables**:
    *   `oauth2_authorized_client` (in `bluesky-api-user`): Stores access tokens.
    *   `UserInfo` (in `bluesky-api-user`): User basic info.

## Frontend
*   **Stack**: Tailwind CSS 4.1.17, daisyUI 5.4.7.
*   **Build**: `npm run build` in `bluesky-web-gate/src/main/frontend`.
*   **TypeScript**: `npx tsc`. Compiles `src/` TS files to `../resources/static/js/`.
*   **HTMX**: Used for interactions.
*   **Important**: UI changes in `bluesky-web-gate` require frontend build to take effect.

## Config Server
*   **URL**: `https://raw.githubusercontent.com/luversof/bluesky-config-repo/develop/`
*   **Content**: Database connection info, OAuth2 Client ID/Secrets.

## Development Environment
*   **Profiles**: `localdev` (port 30xxx), `opdev` (port 40xxx).
*   **URLs**: Gate (http://localhost:30122), api-user (https://user.api.bluesky.local:30131 internal).

## Testing
*   **Integration Tests**: Always run with `SPRING_PROFILES_ACTIVE=localdev` for tests using real data sources/config server (e.g., `DividendTest`).
*   **Config**: Do NOT bypass Config Server with local properties files.

## Do's and Don'ts
*   **Don't**: Add Redis dependency to web modules. Save tokens in web DB directly. Use spaces for indentation. Korean comments in properties.
*   **Do**: Delegate token storage to `api-user`. Use **Tabs**. English comments in properties. Place Feign clients correctly.

## Dividend List Changes (2025-11-19)
*   Web UI uses `DividendView` record.
*   Controller `StockHtmxController` handles N+1 by fetching stock names in bulk if missing.
*   Service `DividendService` does LEFT JOIN.
*   Domain `Dividend` has `@Transient stockItemName`.
*   Tests include data integrity checks for `stock_item_id`.
