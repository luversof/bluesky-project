# Active Tasks & History

## Dividend 리스트 변경사항 및 지침 (2025-11-19)

### 1. 목적
`bluesky-web-gate`의 Dividend 목록을 개선하여 `stockItemName` 등을 포함하고 UI/API/도메인을 정리함.

### 2. 주요 변경점
- **Web UI**: `DividendView` 레코드 추가, `stockItemName` 필드 포함.
- **Controller**: `StockHtmxController.dividendList()`에서 N+1 방지 로직 적용 (ID 집합으로 이름 조회).
- **API**: `bluesky-api-stock`의 `DividendService`가 LEFT JOIN을 통해 `stockItemName` 반환.
- **Domain**: `Dividend`에 `@Transient` 필드로 `stockItemName` 추가.

### 3. Copilot/개발자 필독 지침
1. **UI**: `dividend.stockItemName` 우선 사용.
2. **Domain**: 로컬 필드는 반드시 `@Transient` 사용 (DB 매핑 오류 방지).
3. **SQL**: Alias(`AS`)를 도메인 필드명과 일치시킬 것.
4. **Performance**: Controller에서 Loop 내 API 호출 금지 (Batch 조회 사용).
5. **Data Integrity**: `stock_item_id` NULL 데이터 마이그레이션 및 검증 로직 필수.
6. **Test**: `localdev` 프로파일로 통합 테스트 수행.
