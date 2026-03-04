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

## KIS 종목 히스토리 자동 갱신 기능 (2026-03-05)

### 1. 목적
Active 상태인(Trade나 Dividend에 존재하는) 종목의 날짜 구간(min, max)을 분석하여 누락된 StockPriceHistory 데이터를 KIS API로부터 조회하고 동기화함.

### 2. 주요 변경점
- **Repository**: bluesky-api-stock에 StockPriceHistoryRepository 연동 (findByStockItemIdAndTradeDate).
- **Service**: 100일 단위 데이터 조회 및 적제를 위한 KisStockPriceUpdateService 추가.
- **API 및 라우팅**:
  - API 영역: StockAdminController에 /price-histories 엔드포인트 추가.
  - Web 영역: 프론트호출 프록시를 위해 StockAdminClient(@HttpExchange) / StockAdminApiController 추가.
- **UI**: adminActions.jte에 종목 히스토리 갱신 HTMX 버튼 추가.

### 3. 알아야할 개발 지침
- **에디터 포맷터 충돌**: VS Code에서 저장 시 글로벌 설정과 내부 JDT(4칸 기반) 설정이 탭 들여쓰기를 무시하는 이슈. 필요한 경우 ".vscode/settings.json"에서 editor.formatOnSave를 끄거나 조정.
- **API 프록시**: bluesky-web 모듈에서 bluesky-api 모듈의 신규 API를 노출하려면, 반드시 @HttpExchange 가 선언된 외부 인터페이스를 활용하여 중계해야 함. 단순 html 매핑으로 직접 호출할 수 없음.
