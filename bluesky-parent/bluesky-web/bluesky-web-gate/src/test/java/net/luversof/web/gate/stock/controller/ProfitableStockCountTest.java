package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * 요약 카드의 "수익권 종목 비율"이 배당을 얹어 센다는 것을 못박는다.
 *
 * <p>이 카드가 세는 손익({@code totalProfitNet})은 <b>실현 + 평가</b>일 뿐 배당이 없다. 그런데 같은 카드 바로 위에 누적 배당이 있고, 같은
 * 카드의 '합산 손익'은 {@code 평가 + 실현 + 배당} 이다. 승패만 배당을 빼고 세면 배당이 큰 종목이 실제로는 이익인데 패로 잡힌다.
 *
 * <p>실측 2026-08-24(실데이터):
 *
 * <pre>
 *   TIGER 리츠부동산인프라  실현+평가 -2,929,544 · 세후 배당 +5,132,889  ->  실제 +2,203,345
 *   42 종목 중 이 1 종목이 뒤집힌다: 32/42 = 76.19%  ->  33/42 = 78.57%
 * </pre>
 *
 * <p>분모는 손익 쪽 종목 수 그대로다 &mdash; 배당만 있고 손익 행이 없는 종목이 분모를 늘리면 비율이 조용히 낮아진다.
 */
class ProfitableStockCountTest {

  private static final UUID REIT = UUID.fromString("019d271d-ca46-7536-8872-40eefc85b5fa");
  private static final UUID WINNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID LOSER = UUID.fromString("00000000-0000-0000-0000-000000000002");

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }

  @Test
  void 배당으로_이익이_되는_종목을_수익권으로_센다() {
    Map<UUID, BigDecimal> profit = new LinkedHashMap<>();
    profit.put(REIT, bd("-2929544"));
    Map<UUID, BigDecimal> dividend = new HashMap<>();
    dividend.put(REIT, bd("5132889"));

    assertThat(StockSummaryHtmxController.countProfitableStocks(profit, dividend))
        .as("실현+평가 -2,929,544 인데 배당 5,132,889 이면 실제로는 +2,203,345 이다")
        .isEqualTo(1L);
  }

  @Test
  void 배당을_얹어도_손실인_종목은_세지_않는다() {
    // KODEX 한국부동산리츠인프라 실측: 실현+평가 -11,720,233 · 배당 5,385,714 -> 여전히 -6,334,519
    Map<UUID, BigDecimal> profit = Map.of(LOSER, bd("-11720233"));
    Map<UUID, BigDecimal> dividend = Map.of(LOSER, bd("5385714"));

    assertThat(StockSummaryHtmxController.countProfitableStocks(profit, dividend)).isEqualTo(0L);
  }

  @Test
  void 배당이_분모를_늘리지_않는다() {
    Map<UUID, BigDecimal> profit = new LinkedHashMap<>();
    profit.put(WINNER, bd("100"));
    profit.put(LOSER, bd("-100"));
    // 손익 행이 없는 종목의 배당 - 분자에도 분모에도 들어오면 안 된다.
    Map<UUID, BigDecimal> dividend = Map.of(REIT, bd("999999"));

    assertThat(StockSummaryHtmxController.countProfitableStocks(profit, dividend))
        .as("손익 행이 없는 종목의 배당이 수익권 수를 늘렸다")
        .isEqualTo(1L);
  }

  @Test
  void 정확히_0_은_수익권이_아니다() {
    Map<UUID, BigDecimal> profit = Map.of(WINNER, bd("-500"));
    Map<UUID, BigDecimal> dividend = Map.of(WINNER, bd("500"));

    assertThat(StockSummaryHtmxController.countProfitableStocks(profit, dividend))
        .as("본전(0)을 수익으로 세면 배당 제외 기준과 경계가 달라진다")
        .isEqualTo(0L);
  }

  @Test
  void 배당이_아예_없어도_동작한다() {
    Map<UUID, BigDecimal> profit = new LinkedHashMap<>();
    profit.put(WINNER, bd("100"));
    profit.put(LOSER, bd("-100"));

    assertThat(StockSummaryHtmxController.countProfitableStocks(profit, Map.of())).isEqualTo(1L);
    assertThat(StockSummaryHtmxController.countProfitableStocks(profit, null)).isEqualTo(1L);
  }
}
