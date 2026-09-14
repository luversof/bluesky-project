package net.luversof.web.gate.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;

class MonthlyDividendViewSupportTest {

  private final MonthlyDividendViewSupport support = new MonthlyDividendViewSupport();

  @Test
  void resolveRowSort_invalidFallsBackToDisplayOrder() {
    assertThat(support.resolveRowSort(null)).isEqualTo("display-order");
    assertThat(support.resolveRowSort("bogus")).isEqualTo("display-order");
    assertThat(support.resolveRowSort("annual-yield")).isEqualTo("annual-yield");
  }

  /**
   * 화면이 만들어 내보내는 정렬 키는 전부 허용 목록에 있어야 한다.
   *
   * <p>실측 2026-09-12: 표의 마지막 열 머리는 {@code sort=combined-return} 링크를 내보내는데 이 값만 목록에 없어 표시 순서로 되돌아갔다
   * &mdash; {@code sort=combined-return&direction=desc} 의 행 순서가 {@code
   * sort=display-order&direction=desc} 와 완전히 같았다(누르면 아무 일도 일어나지 않는 열). 비교기는 {@code sortRows} 의
   * {@code default} 가지에 이미 있었지만 여기서 걸러져 닿지 못했다.
   *
   * <p>그래서 목록을 손으로 적어 두지 않고 템플릿에서 긁어 대조한다 - 열을 새로 만들 때 같은 일이 또 나지 않게.
   */
  @Test
  void 화면이_만드는_정렬_키는_모두_허용된다() throws IOException {
    Set<String> keys =
        sortKeysInTemplate("src/main/jte/stock/fragments/monthlyDividendSimulator.jte");
    assertThat(keys).as("템플릿에서 정렬 링크를 하나도 못 찾았다 - 스캔이 깨졌다").hasSizeGreaterThan(5);
    for (String key : keys) {
      assertThat(support.resolveRowSort(key))
          .as(key + " 는 화면이 내보내는 정렬 키인데 허용 목록에 없다 - 눌러도 표시 순서로 되돌아간다")
          .isEqualTo(key);
    }
  }

  /** 예상 종합 수익률로 정렬하면 그 값의 순서대로 나온다(표시 순서가 아니라). */
  @Test
  void 종합_수익률_정렬은_그_값을_따른다() {
    var rows =
        List.of(
            row("AAA", "AAA", "1", "3"),
            row("BBB", "BBB", "1", "10"),
            row("CCC", "CCC", "1", "-2"));
    Map<String, Integer> displayOrders = Map.of("AAA", 1, "BBB", 2, "CCC", 3);

    assertThat(support.sortRows(rows, "combined-return", "desc", displayOrders))
        .extracting(MonthlyDividendSnapshotResponse::stockItemSymbol)
        .containsExactly("BBB", "AAA", "CCC");
    assertThat(support.sortRows(rows, "combined-return", "asc", displayOrders))
        .extracting(MonthlyDividendSnapshotResponse::stockItemSymbol)
        .containsExactly("CCC", "AAA", "BBB");
    // 표시 순서와 다른 결과라야 '정렬이 먹었다' 고 말할 수 있다.
    assertThat(support.sortRows(rows, "combined-return", "desc", displayOrders))
        .isNotEqualTo(support.sortRows(rows, "display-order", "desc", displayOrders));
  }

  /** 방향을 안 주면 종합 수익률은 큰 값부터 - 머리 링크의 기본 토글(desc)과 같아야 한다. */
  @Test
  void 종합_수익률의_기본_방향은_내림차순이다() {
    assertThat(support.resolveRowDirection("combined-return", null)).isEqualTo("desc");
  }

  /** 정규식 없이 {@code &sort=} 뒤의 값을 읽는다(빌드 도구가 이스케이프를 먹는 일이 반복돼 단순하게 둔다). */
  private static Set<String> sortKeysInTemplate(String relativePath) throws IOException {
    String template = Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
    Set<String> keys = new LinkedHashSet<>();
    String marker = "&sort=";
    int at = template.indexOf(marker);
    while (at >= 0) {
      int from = at + marker.length();
      int to = from;
      while (to < template.length() && isSortKeyChar(template.charAt(to))) {
        to++;
      }
      if (to > from) {
        keys.add(template.substring(from, to));
      }
      at = template.indexOf(marker, from);
    }
    return new LinkedHashSet<>(new ArrayList<>(keys));
  }

  private static boolean isSortKeyChar(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-';
  }

  @Test
  void resolveRowDirection_defaultsBySortKey() {
    assertThat(support.resolveRowDirection("display-order", null)).isEqualTo("asc");
    assertThat(support.resolveRowDirection("taxable-base", null)).isEqualTo("asc");
    assertThat(support.resolveRowDirection("annual-yield", null)).isEqualTo("desc");
    assertThat(support.resolveRowDirection("annual-yield", "ASC")).isEqualTo("asc");
  }

  @Test
  void filterRows_appliesKeywordMinYieldAndPositiveOnly() {
    var rows =
        List.of(
            row("069500", "KODEX200", "5", "3"), // annualYield 5, combined 3
            row("133690", "TIGER", "1", "-2"), // annualYield 1, combined -2
            row("360750", "TIGERSP", "8", "10"));

    assertThat(support.filterRows(rows, "tiger", null, false))
        .extracting(MonthlyDividendSnapshotResponse::stockItemSymbol)
        .containsExactly("133690", "360750");

    assertThat(support.filterRows(rows, null, new BigDecimal("5"), false))
        .extracting(MonthlyDividendSnapshotResponse::stockItemSymbol)
        .containsExactly("069500", "360750");

    assertThat(support.filterRows(rows, null, null, true))
        .extracting(MonthlyDividendSnapshotResponse::stockItemSymbol)
        .containsExactly("069500", "360750");
  }

  @Test
  void sortRows_byDisplayOrderUsesProfileMapThenSymbol() {
    var rows =
        List.of(
            row("AAA", "AAA", "1", "1"), row("BBB", "BBB", "1", "1"), row("CCC", "CCC", "1", "1"));
    Map<String, Integer> displayOrders = Map.of("BBB", 1, "CCC", 2); // AAA not present -> MAX

    var sorted = support.sortRows(rows, "display-order", "asc", displayOrders);

    assertThat(sorted)
        .extracting(MonthlyDividendSnapshotResponse::stockItemSymbol)
        .containsExactly("BBB", "CCC", "AAA");
  }

  @Test
  void sortProfiles_displayOrderAscWithSymbolTiebreak() {
    var profiles =
        List.of(profile("CCC", 2), profile("AAA", 1), profile("BBB", 1)); // AAA/BBB tie -> symbol

    var sorted = support.sortProfiles(profiles, "display-order", "asc");

    assertThat(sorted)
        .extracting(MonthlyDividendProfileResponse::stockItemSymbol)
        .containsExactly("AAA", "BBB", "CCC");
  }

  @Test
  void buildProfileDisplayOrderMap_normalizesSymbolAndKeepsFirst() {
    var profiles = List.of(profile("aaa", 5), profile("AAA", 9), profile("  bbb ", 3));

    Map<String, Integer> map = support.buildProfileDisplayOrderMap(profiles);

    assertThat(map).containsEntry("AAA", 5).containsEntry("BBB", 3);
  }

  private static MonthlyDividendSnapshotResponse row(
      String symbol, String name, String annualYieldPct, String combinedReturnPct) {
    return new MonthlyDividendSnapshotResponse(
        null, // id
        null, // userId
        null, // stockItemId
        symbol, // stockItemSymbol
        name, // stockItemName
        null, // asOfDate
        null, // latestMonthlyDividendPerShare
        null, // averageMonthlyDividendPerShare1y
        null, // averageTaxableBaseRatio1y
        null, // heldQuantity
        null, // averageBuyPrice
        null, // currentPrice
        null, // currentMarketValue
        null, // expectedMonthlyDividend
        null, // expectedMonthlyYieldPct
        new BigDecimal(annualYieldPct), // expectedAnnualYieldPct
        null, // expectedMonthlyYieldOnCostPct
        null, // expectedAnnualYieldOnCostPct
        null, // expectedTaxableBaseAmount
        null, // totalReturnOnCostPct
        new BigDecimal(combinedReturnPct), // expectedCombinedReturnPct
        null); // updatedDate
  }

  private static MonthlyDividendProfileResponse profile(String symbol, int displayOrder) {
    return new MonthlyDividendProfileResponse(
        null, // id
        null, // stockItemId
        symbol, // stockItemSymbol
        null, // stockItemName
        null, // sourceUrl
        null, // payoutWindow
        displayOrder, // displayOrder
        true, // active
        null, // note
        (LocalDate) null, // lastVerifiedDate
        null); // updatedDate
  }
}
