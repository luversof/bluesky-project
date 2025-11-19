# bluesky-api-dto

This module contains shared response DTOs used across modules (API and web-gate).

Currently included DTOs:
- `net.luversof.api.stock.web.dto.response.DividendResponse` — raw API response for dividend data.
- `net.luversof.api.stock.web.dto.response.DividendView` — DTO for UI view displaying dividend rows (contains accountName, netAmount, etc.).

Usage:
- Add dependency on `bluesky-api-dto` in your module's `pom.xml`. (Parent POM should include this module.)
- Prefer `DividendResponse` as API contract; build UI presentation DTOs from it (`DividendView`).

Notes:
- Existing `web-gate` local `DividendView` remains as a wrapper but is marked deprecated; use shared DTO instead.
- Ensure `DividendController` maps domain `Dividend` to `DividendResponse` before returning API responses.
