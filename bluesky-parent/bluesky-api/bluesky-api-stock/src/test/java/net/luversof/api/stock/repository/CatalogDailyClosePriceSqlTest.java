package net.luversof.api.stock.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 월배당 카탈로그가 읽는 종목별 일별 종가 SQL 의 조건과 순서.
 *
 * <p>2026-09-23 에 이 조회를 StockPriceHistoryRepository 의 {@code @Query} 에서 StockDailyClosePriceQuery 로
 * 옮겼다(Spring Data 행 변환이 카탈로그 응답의 71% 였다, 80 &rarr; 32ms). 거래량 조건은 ZeroVolumePriceSelectionTest 가
 * 세지만, 계산 창(fromDate)과 정렬은 옮기기 전에도 아무도 보지 않았다. 창이 빠지면 15 년 치를 읽어 다시 느려지고, 정렬이 빠지면 종목별 시계열을 순서대로
 * 받는다고 믿는 위험 지표 계산이 조용히 틀린다.
 */
class CatalogDailyClosePriceSqlTest {

  private static final Path QUERY =
      Path.of("src/main/java/net/luversof/api/stock/repository/StockDailyClosePriceQuery.java");

  private static final Path REPOSITORY =
      Path.of("src/main/java/net/luversof/api/stock/repository/StockPriceHistoryRepository.java");

  private static String squash(String text) {
    return text.replaceAll("\\s+", "");
  }

  private static String itemsSql() throws IOException {
    String source = squash(Files.readString(QUERY, StandardCharsets.UTF_8));
    String head = "ITEMS_FROM_SQL=\"\"\"";
    int start = source.indexOf(head);
    assertThat(start).as("ITEMS_FROM_SQL 이 사라졌다").isGreaterThanOrEqualTo(0);
    // SQL 이 따옴표로 끝나면(h."tradeDate") 닫는 텍스트 블록과 붙어 따옴표가 넷 연달아 온다 - 세미콜론까지 붙여 찾는다.
    int end = source.indexOf("\"\"\";", start + head.length());
    return source.substring(start + head.length(), end);
  }

  @Test
  void 계산_창_이후만_종목_거래일_순으로_읽는다() throws IOException {
    String sql = itemsSql();
    assertThat(sql)
        .as("세 컬럼을 이 순서로 - MAPPER 가 위치로 읽는다")
        .startsWith(
            "SELECTh.\"stockItem_id\"ASstock_item_id,h.\"tradeDate\"AStrade_date,h.\"closePrice\"ASclose_price");
    assertThat(sql)
        .as("종목 목록으로 거른다")
        .contains("h.\"stockItem_id\"=ANY(string_to_array(:ids,',')::uuid[])");
    assertThat(sql).as("거래가 있던 날만").contains("ANDh.\"volume\">0");
    assertThat(sql).as("계산 창 이후만").contains("ANDh.\"tradeDate\">=CAST(:fromDateASdate)");
    assertThat(sql).as("종목 -> 거래일 순").endsWith("ORDERBYh.\"stockItem_id\",h.\"tradeDate\"");
  }

  @Test
  void 행_변환이_느린_저장소_사본은_되살리지_않는다() throws IOException {
    assertThat(Files.readString(REPOSITORY, StandardCharsets.UTF_8))
        .as("같은 SQL 의 @Query 사본 - Spring Data 행 변환이 카탈로그 응답의 71% 였다")
        .doesNotContain("findDailyClosePricesForItems");
  }
}
