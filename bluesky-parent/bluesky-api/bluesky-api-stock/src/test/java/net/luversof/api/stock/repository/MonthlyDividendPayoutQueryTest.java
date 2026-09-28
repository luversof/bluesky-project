package net.luversof.api.stock.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.domain.MonthlyDividendPayout;

/**
 * 월배당 카탈로그의 지급 이력 일괄 조회(2026-09-23 파생 쿼리 -&gt; 명시 RowMapper).
 *
 * <p>파생 쿼리는 이름이 곧 조건 · 정렬이었다. 손으로 쓴 SQL 은 조건 · 정렬 · 컬럼 위치가 전부 글자라, 하나가 빠지거나 어긋나도 컴파일은 통과하고 값만 조용히
 * 바뀐다(기준일과 지급일이 뒤바뀌면 통계의 "최근 지급" 이 다른 날을 가리킨다). 그래서 SQL 모양과 위치 매핑을 함께 본다.
 */
class MonthlyDividendPayoutQueryTest {

  private static final Path QUERY =
      Path.of("src/main/java/net/luversof/api/stock/repository/MonthlyDividendPayoutQuery.java");

  private static final Path REPOSITORY =
      Path.of(
          "src/main/java/net/luversof/api/stock/repository/MonthlyDividendPayoutRepository.java");

  private static String squash(String text) {
    return text.replaceAll("[\\s]+", "");
  }

  @Test
  void 파생_쿼리와_같은_조건과_정렬이다() throws IOException {
    String source = squash(Files.readString(QUERY, StandardCharsets.UTF_8));
    assertThat(source)
        .as("컬럼 순서 - mapRow 가 위치로 읽는다")
        .contains(
            squash(
                "SELECT \"id\", \"stockItem_id\", \"recordDate\", \"payDate\", \"distributionRatePct\","
                    + " \"dividendAmountPerShare\", \"taxableBasePerShare\", \"createdDate\", \"updatedDate\""
                    + " FROM \"MonthlyDividendPayout\""))
        .as("고른 종목만")
        .contains(squash("WHERE \"stockItem_id\" IN (:ids)"))
        .as("지급일 · 기준일 내림차순(파생 쿼리 이름 그대로)")
        .contains(squash("ORDER BY \"payDate\" DESC, \"recordDate\" DESC"));
    assertThat(Files.readString(REPOSITORY, StandardCharsets.UTF_8))
        .as("같은 조회의 파생 쿼리 사본 - 행 변환이 카탈로그 응답의 절반을 넘었다")
        .doesNotContain("findByStockItemIdInOrderByPayDateDescRecordDateDesc")
        .as("전체 조회의 파생 쿼리 사본 - dataStatus 의 45% 였다(2026-09-23)")
        .doesNotContain("findAllByOrderByPayDateDescRecordDateDesc");
    assertThat(source)
        .as("전체 조회도 같은 컬럼 · 같은 정렬, 조건 없이")
        .contains(
            squash(
                "\"taxableBasePerShare\", \"createdDate\", \"updatedDate\" FROM \"MonthlyDividendPayout\""
                    + " ORDER BY \"payDate\" DESC, \"recordDate\" DESC"));
  }

  /** 메서드마다 제 SQL 을 쓴다 - 상수 이름만 바뀌어도 전체 조회가 종목 조건을 달거나 그 반대가 된다. */
  @Test
  void 메서드마다_제_SQL_을_쓴다() throws IOException {
    String source = squash(Files.readString(QUERY, StandardCharsets.UTF_8));
    int all =
        source.indexOf(
            "publicList<MonthlyDividendPayout>findAllByOrderByPayDateDescRecordDateDesc(){");
    int byItems =
        source.indexOf(
            "publicList<MonthlyDividendPayout>findByStockItemIdInOrderByPayDateDescRecordDateDesc(");
    assertThat(all).as("전체 조회 메서드").isGreaterThan(0);
    assertThat(byItems).as("종목별 조회 메서드").isGreaterThan(0);
    String allBody = source.substring(all, source.indexOf("}", all) + 1);
    String byItemsBody = source.substring(byItems, source.indexOf("MAPPER)", byItems) + 7);
    assertThat(allBody).as("전체 조회는 ALL_SQL").contains("query(ALL_SQL,Map.of(),MAPPER)");
    assertThat(byItemsBody)
        .as("종목별 조회는 BY_STOCK_ITEMS_SQL 과 ids")
        .contains("query(BY_STOCK_ITEMS_SQL,Map.of(\"ids\",stockItemIds),MAPPER)");
  }

  @Test
  void 컬럼마다_제_필드에_들어간다() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID stockItemId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    when(rs.getObject(1, UUID.class)).thenReturn(id);
    when(rs.getObject(2, UUID.class)).thenReturn(stockItemId);
    when(rs.getObject(3, LocalDate.class)).thenReturn(LocalDate.of(2026, 9, 1));
    when(rs.getObject(4, LocalDate.class)).thenReturn(LocalDate.of(2026, 9, 3));
    when(rs.getBigDecimal(5)).thenReturn(new BigDecimal("5.5"));
    when(rs.getBigDecimal(6)).thenReturn(new BigDecimal("66"));
    when(rs.getBigDecimal(7)).thenReturn(new BigDecimal("7.7"));
    when(rs.getObject(8, OffsetDateTime.class))
        .thenReturn(OffsetDateTime.of(2026, 9, 8, 0, 0, 0, 0, ZoneOffset.UTC));
    when(rs.getObject(9, OffsetDateTime.class))
        .thenReturn(OffsetDateTime.of(2026, 9, 9, 0, 0, 0, 0, ZoneOffset.ofHours(9)));

    MonthlyDividendPayout payout = MonthlyDividendPayoutQuery.mapRow(rs, 0);

    assertThat(payout.getId()).isEqualTo(id);
    assertThat(payout.getStockItemId()).isEqualTo(stockItemId);
    assertThat(payout.getRecordDate()).as("기준일").isEqualTo(LocalDate.of(2026, 9, 1));
    assertThat(payout.getPayDate()).as("지급일").isEqualTo(LocalDate.of(2026, 9, 3));
    assertThat(payout.getDistributionRatePct()).isEqualByComparingTo("5.5");
    assertThat(payout.getDividendAmountPerShare()).as("주당 분배금").isEqualByComparingTo("66");
    assertThat(payout.getTaxableBasePerShare()).as("주당 과세표준").isEqualByComparingTo("7.7");
    assertThat(payout.getCreatedDate())
        .isEqualTo(OffsetDateTime.of(2026, 9, 8, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());
    assertThat(payout.getUpdatedDate())
        .as("시간대가 붙은 값도 같은 순간으로")
        .isEqualTo(OffsetDateTime.of(2026, 9, 9, 0, 0, 0, 0, ZoneOffset.ofHours(9)).toInstant());
  }

  @Test
  void 빈_목록은_조회하지_않는다() {
    assertThat(
            new MonthlyDividendPayoutQuery()
                .findByStockItemIdInOrderByPayDateDescRecordDateDesc(List.of()))
        .isEmpty();
    assertThat(
            new MonthlyDividendPayoutQuery()
                .findByStockItemIdInOrderByPayDateDescRecordDateDesc(null))
        .isEmpty();
  }
}
