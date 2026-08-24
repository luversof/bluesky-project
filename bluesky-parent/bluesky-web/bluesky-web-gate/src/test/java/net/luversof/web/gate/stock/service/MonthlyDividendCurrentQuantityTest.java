package net.luversof.web.gate.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;
import net.luversof.web.gate.stock.service.MonthlyDividendCalculator.CurrentQuantitySummary;

/**
 * 스냅샷 수량이 원장의 현재 보유와 얼마나 어긋났는지 세는 규칙을 고정한다.
 *
 * <p>월배당 화면의 합계는 사람이 갱신하는 스냅샷 수량으로 계산된다. 실측 2026-08-23 기준 8 종목 중 7 종목이 달랐고, 그만큼 예상 월배당이 46,123 원
 * (1.66%) 낮게 잡혀 있었다. 이 규칙이 요약 화면 안에만 있어서 시뮬레이터 합계 카드는 아무 안내 없이 옛 수량 기준 값을 헤드라인으로 내보냈다.
 */
class MonthlyDividendCurrentQuantityTest {

  private static final UUID ITEM_A = UUID.randomUUID();
  private static final UUID ITEM_B = UUID.randomUUID();

  /** 1주당 배당(1년 평균)과 보유 수량만 있으면 되는 최소 행. */
  private MonthlyDividendSnapshotResponse row(
      UUID stockItemId, int heldQuantity, String perShare1y, String expectedMonthlyDividend) {
    return new MonthlyDividendSnapshotResponse(
        null,
        null,
        stockItemId,
        "SYM",
        "이름",
        null,
        null,
        perShare1y == null ? null : new BigDecimal(perShare1y),
        null,
        heldQuantity,
        null,
        null,
        null,
        expectedMonthlyDividend == null ? null : new BigDecimal(expectedMonthlyDividend),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  @Test
  void 수량이_같으면_어긋난_종목이_없고_합계도_그대로다() {
    CurrentQuantitySummary summary =
        MonthlyDividendCalculator.currentQuantitySummary(
            List.of(row(ITEM_A, 100, "30", "3000")), Map.of(ITEM_A, 100));

    assertThat(summary.staleCount()).isZero();
    assertThat(summary.totalAtCurrentQuantity()).isEqualByComparingTo("3000");
  }

  /** 실데이터 모양: 스냅샷 857 주인데 실제 879 주. 1주당 240 원이면 합계가 205,680 -> 210,960 이 된다. */
  @Test
  void 수량이_늘었으면_현재_수량으로_다시_센다() {
    CurrentQuantitySummary summary =
        MonthlyDividendCalculator.currentQuantitySummary(
            List.of(row(ITEM_A, 857, "240", "205680")), Map.of(ITEM_A, 879));

    assertThat(summary.staleCount()).isEqualTo(1);
    assertThat(summary.totalAtCurrentQuantity()).isEqualByComparingTo("210960");
  }

  @Test
  void 여러_종목이면_어긋난_것만_센다() {
    CurrentQuantitySummary summary =
        MonthlyDividendCalculator.currentQuantitySummary(
            List.of(row(ITEM_A, 100, "10", "1000"), row(ITEM_B, 50, "20", "1000")),
            Map.of(ITEM_A, 100, ITEM_B, 70));

    assertThat(summary.staleCount()).isEqualTo(1);
    assertThat(summary.totalAtCurrentQuantity()).isEqualByComparingTo("2400"); // 1000 + 20x70
  }

  /** 현재 수량을 알 수 없으면(보유 목록에 없는 종목) 스냅샷 값을 그대로 쓰고 어긋난 것으로 세지 않는다. */
  @Test
  void 현재_수량을_모르면_스냅샷_값을_그대로_쓴다() {
    CurrentQuantitySummary summary =
        MonthlyDividendCalculator.currentQuantitySummary(
            List.of(row(ITEM_A, 100, "30", "3000")), Map.of());

    assertThat(summary.staleCount()).isZero();
    assertThat(summary.totalAtCurrentQuantity()).isEqualByComparingTo("3000");
  }

  /** 1주당 배당이 없으면 다시 셀 수 없으므로 스냅샷 값을 그대로 쓴다(0 으로 떨어뜨리지 않는다). */
  @Test
  void 주당_배당이_없으면_스냅샷_값을_그대로_쓴다() {
    CurrentQuantitySummary summary =
        MonthlyDividendCalculator.currentQuantitySummary(
            List.of(row(ITEM_A, 100, null, "3000")), Map.of(ITEM_A, 130));

    assertThat(summary.staleCount()).isZero();
    assertThat(summary.totalAtCurrentQuantity()).isEqualByComparingTo("3000");
  }

  @Test
  void 행이_없거나_null_이어도_터지지_않는다() {
    assertThat(MonthlyDividendCalculator.currentQuantitySummary(null, Map.of()).staleCount())
        .isZero();
    assertThat(
            MonthlyDividendCalculator.currentQuantitySummary(
                    List.of(row(ITEM_A, 10, "1", "10")), null)
                .totalAtCurrentQuantity())
        .isEqualByComparingTo("10");
  }
}
