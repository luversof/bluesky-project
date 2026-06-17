package net.luversof.web.gate.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
