package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;

/** 월배당 시뮬레이터의 지급 이력 기준 과세표준(2026-10-22 populateMonthlyDividendModel 에서 정적 도우미로 옮기며 처음 둔 시험). */
class StockViewControllerReferenceTaxableTest {

  private static MonthlyDividendSnapshotResponse row(
      UUID id, String symbol, String expectedDividend, String savedTaxable, String savedRatio) {
    try {
      RecordComponent[] components = MonthlyDividendSnapshotResponse.class.getRecordComponents();
      Class<?>[] types = new Class<?>[components.length];
      Object[] values = new Object[components.length];
      for (int i = 0; i < components.length; i++) {
        types[i] = components[i].getType();
        switch (components[i].getName()) {
          case "stockItemId" -> values[i] = id;
          case "stockItemSymbol" -> values[i] = symbol;
          case "expectedMonthlyDividend" -> values[i] = bd(expectedDividend);
          case "expectedTaxableBaseAmount" -> values[i] = bd(savedTaxable);
          case "averageTaxableBaseRatio1y" -> values[i] = bd(savedRatio);
          default -> values[i] = defaultOf(types[i]);
        }
      }
      return MonthlyDividendSnapshotResponse.class
          .getDeclaredConstructor(types)
          .newInstance(values);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static Object defaultOf(Class<?> type) {
    if (type == boolean.class) {
      return false;
    }
    if (type == int.class) {
      return 0;
    }
    if (type == long.class) {
      return 0L;
    }
    if (type == double.class) {
      return 0d;
    }
    return null;
  }

  private static BigDecimal bd(String value) {
    return value == null ? null : new BigDecimal(value);
  }

  @Test
  void 비중은_심볼을_다듬어_찾는다() {
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    var ratios =
        StockViewController.referenceTaxableRatios(
            List.of(
                row(a, " 0018c0 ", "1000", null, null),
                row(b, "XYZ", "1000", null, null),
                row(null, "0018C0", "1", null, null)),
            Map.of("0018C0", new BigDecimal("12.5")));

    assertThat(ratios).containsOnlyKeys(a);
    assertThat(ratios.get(a)).isEqualByComparingTo("12.5");
  }

  @Test
  void 합계는_이력_비중이_있으면_그것으로_없으면_저장값() {
    UUID withRef = UUID.randomUUID();
    UUID withoutRef = UUID.randomUUID();
    UUID sameAsSaved = UUID.randomUUID();
    var summary =
        StockViewController.referenceTaxableSummary(
            List.of(
                row(withRef, "A", "10001", "500", "5.0"),
                row(withoutRef, "B", "20000", "777", "3.0"),
                row(sameAsSaved, "C", "4000", "999", "39.5")),
            Map.of(withRef, new BigDecimal("40"), sameAsSaved, new BigDecimal("40")));

    // 10001 x 40% = 4000.4 -> 4000 · 777(저장값) · 4000 x 40% = 1600
    assertThat(summary.total()).isEqualByComparingTo("6377");
    assertThat(summary.staleCount()).as("5.0 -> 40 은 낡음, 39.5 -> 40 은 1%p 미만").isEqualTo(1);
  }

  @Test
  void 경계_1퍼센트포인트는_낡음() {
    UUID id = UUID.randomUUID();
    var summary =
        StockViewController.referenceTaxableSummary(
            List.of(row(id, "A", "100", null, "39")), Map.of(id, new BigDecimal("40")));

    assertThat(summary.staleCount()).isEqualTo(1);
  }
}
