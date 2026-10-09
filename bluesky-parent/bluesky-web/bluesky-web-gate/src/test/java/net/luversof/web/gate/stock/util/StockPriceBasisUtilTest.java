package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.TradeProfit;

/**
 * 화면에 적는 "평가 기준 종가일"을 고르는 규칙을 고정한다.
 *
 * <p>이 값은 사용자에게 "이 평가액이 언제 시세로 계산됐는지"를 알리는 유일한 단서다. 시세는 자동 수집되지 않으므로(스케줄러 없음) 실제로 며칠 전 값일 수 있고, 날짜를
 * 잘못 고르면 오히려 최신인 것처럼 오해시킨다.
 */
class StockPriceBasisUtilTest {

  private TradeProfit holding(int holdingQuantity, LocalDate priceDate) {
    return new TradeProfit(
        null,
        "삼성전자",
        null,
        "한국투자증권 위탁",
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        0,
        null,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        holdingQuantity,
        null,
        null,
        null,
        null,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        priceDate);
  }

  @Test
  void 보유_종목_중_가장_늦은_종가일을_고른다() {
    LocalDate result =
        StockPriceBasisUtil.latestPriceBasisDate(
            List.of(
                holding(10, LocalDate.parse("2026-08-18")),
                holding(5, LocalDate.parse("2026-08-20")),
                holding(3, LocalDate.parse("2026-08-19"))));

    assertThat(result).isEqualTo(LocalDate.parse("2026-08-20"));
  }

  /** 이미 판 종목의 종가일이 섞이면 화면 숫자와 무관한 날짜가 표시된다. */
  @Test
  void 보유_수량이_없는_종목은_무시한다() {
    LocalDate result =
        StockPriceBasisUtil.latestPriceBasisDate(
            List.of(
                holding(10, LocalDate.parse("2026-08-18")),
                holding(0, LocalDate.parse("2026-08-21"))));

    assertThat(result).isEqualTo(LocalDate.parse("2026-08-18"));
  }

  @Test
  void 종가일이_없는_종목은_건너뛴다() {
    LocalDate result =
        StockPriceBasisUtil.latestPriceBasisDate(
            Arrays.asList(holding(10, null), holding(4, LocalDate.parse("2026-08-20"))));

    assertThat(result).isEqualTo(LocalDate.parse("2026-08-20"));
  }

  /** 하나도 없으면 화면은 안내를 감춰야 한다. 빈 문자열이나 오늘 날짜로 때우면 없는 근거를 지어내는 셈이다. */
  @Test
  void 근거가_없으면_null_이다() {
    assertThat(StockPriceBasisUtil.latestPriceBasisDate(null)).isNull();
    assertThat(StockPriceBasisUtil.latestPriceBasisDate(List.of())).isNull();
    assertThat(
            StockPriceBasisUtil.latestPriceBasisDate(
                List.of(holding(0, LocalDate.parse("2026-08-20")))))
        .isNull();
    assertThat(StockPriceBasisUtil.latestPriceBasisDate(Arrays.asList(holding(10, null)))).isNull();
  }

  /**
   * 전량 매도한 종목은 보유 기준으로는 날짜가 없다. 그러면 화면에서 안내 줄만 사라지고 멈춰 있는 현재가가 그대로 남아 오늘 값처럼 보인다.
   *
   * <p>시세 수집은 보유 중인 종목만 따라가므로 이런 종목이 실제로 33 개 있었다(NAVER 210,000 원 / 2026-04-01).
   */
  @Test
  void 전량_매도한_종목은_마지막_종가일로_되돌린다() {
    List<TradeProfit> soldOut =
        List.of(holding(0, LocalDate.of(2026, 4, 1)), holding(0, LocalDate.of(2025, 11, 27)));

    assertThat(StockPriceBasisUtil.latestPriceBasisDate(soldOut))
        .as("보유 기준으로는 날짜가 없다 - 그래서 되돌림이 필요하다")
        .isNull();
    assertThat(StockPriceBasisUtil.priceBasisDateWithFallback(soldOut))
        .isEqualTo(LocalDate.of(2026, 4, 1));
  }

  /** 보유가 남아 있으면 보유 기준이 이긴다. 판 종목의 오래된 날짜에 끌려가면 안 된다. */
  @Test
  void 보유가_남아있으면_보유_기준일을_쓴다() {
    List<TradeProfit> mixed =
        List.of(holding(0, LocalDate.of(2026, 4, 1)), holding(10, LocalDate.of(2026, 8, 19)));

    assertThat(StockPriceBasisUtil.priceBasisDateWithFallback(mixed))
        .isEqualTo(LocalDate.of(2026, 8, 19));
  }

  /** 판 종목의 날짜가 더 늦어도 보유 종목 기준이 우선이다(화면이 반영한 날은 보유분 기준이다). */
  @Test
  void 판_종목의_날짜가_더_늦어도_보유_기준이_우선이다() {
    List<TradeProfit> mixed =
        List.of(holding(0, LocalDate.of(2026, 8, 19)), holding(10, LocalDate.of(2026, 4, 1)));

    assertThat(StockPriceBasisUtil.priceBasisDateWithFallback(mixed))
        .isEqualTo(LocalDate.of(2026, 4, 1));
  }

  @Test
  void 행이_없거나_날짜가_전혀_없으면_null_이다() {
    assertThat(StockPriceBasisUtil.priceBasisDateWithFallback(null)).isNull();
    assertThat(StockPriceBasisUtil.priceBasisDateWithFallback(List.of())).isNull();
    assertThat(StockPriceBasisUtil.priceBasisDateWithFallback(List.of(holding(0, null)))).isNull();
  }

  private TradeProfit pricedAt(LocalDate priceDate, String updatedAtUtc) {
    TradeProfit base = holding(1, priceDate);
    return new TradeProfit(
        base.stockItemId(),
        base.stockItemName(),
        base.accountId(),
        base.accountName(),
        base.totalBuyAmount(),
        base.averageBuyPrice(),
        base.totalSellQuantity(),
        base.averageSellPrice(),
        base.totalSellAmount(),
        base.realizedProfit(),
        base.holdingQuantity(),
        base.currentPrice(),
        base.evaluationAmount(),
        base.evaluationProfit(),
        base.totalProfit(),
        base.totalBuyFee(),
        base.totalSellFee(),
        base.totalSellTax(),
        base.totalBuyCost(),
        base.totalSellProceeds(),
        base.averageBuyPriceNet(),
        base.averageSellPriceNet(),
        base.realizedProfitNet(),
        base.evaluationProfitNet(),
        base.totalProfitNet(),
        base.currentPriceDate(),
        updatedAtUtc == null ? null : java.time.Instant.parse(updatedAtUtc));
  }

  /**
   * 장중에 받은 시세는 "종가" 가 아니다(2026-10-02 실측: 10:16 에 받은 행이 장 마감 뒤까지 "평가 기준 2026-10-02 종가" 로 보였다). 마감
   * 15:30 KST 경계 양쪽, 다음 날 받은 경우, 값이 없는 경우를 본다.
   */
  @Test
  void 그_거래일_마감_전에_받은_시세만_장중_시각을_낸다() {
    LocalDate day = LocalDate.parse("2026-10-02");
    // 10:16 KST = 01:16 UTC
    assertThat(
            StockPriceBasisUtil.intradayTime(java.time.Instant.parse("2026-10-02T01:16:02Z"), day))
        .isEqualTo("10:16");
    // 15:29 KST 는 아직 장중, 15:30 KST 부터는 종가
    assertThat(
            StockPriceBasisUtil.intradayTime(java.time.Instant.parse("2026-10-02T06:29:59Z"), day))
        .isEqualTo("15:29");
    assertThat(
            StockPriceBasisUtil.intradayTime(java.time.Instant.parse("2026-10-02T06:30:00Z"), day))
        .isNull();
    // 다음 날 아침에 받았으면 전날 행은 확정 종가
    assertThat(
            StockPriceBasisUtil.intradayTime(java.time.Instant.parse("2026-10-03T00:30:00Z"), day))
        .isNull();
    // UTC 로는 전날이지만 KST 로는 그 날 08:59 - 날짜 비교는 KST 로 한다
    assertThat(
            StockPriceBasisUtil.intradayTime(java.time.Instant.parse("2026-10-01T23:59:00Z"), day))
        .isEqualTo("08:59");
    assertThat(StockPriceBasisUtil.intradayTime((java.time.Instant) null, day)).isNull();
    assertThat(
            StockPriceBasisUtil.intradayTime(java.time.Instant.parse("2026-10-02T01:16:02Z"), null))
        .isNull();
  }

  @Test
  void 기준일_행_중_하나라도_장중이면_가장_이른_시각을_낸다() {
    LocalDate day = LocalDate.parse("2026-10-02");
    List<TradeProfit> rows =
        List.of(
            pricedAt(day, "2026-10-02T09:19:37Z"), // 18:19 KST - 종가
            pricedAt(day, "2026-10-02T01:16:02Z"), // 10:16 KST - 장중
            pricedAt(LocalDate.parse("2026-10-01"), "2026-10-01T00:10:00Z"), // 다른 날 행은 안 본다
            pricedAt(day, null));
    assertThat(StockPriceBasisUtil.intradayTime(rows, day)).isEqualTo("10:16");
    assertThat(StockPriceBasisUtil.intradayTime(List.of(rows.get(0), rows.get(3)), day)).isNull();
    assertThat(StockPriceBasisUtil.intradayTime(List.of(rows.get(2)), day)).isNull();
    assertThat(StockPriceBasisUtil.intradayTime((List<TradeProfit>) null, day)).isNull();
  }

  @Test
  void 보유_행이_있으면_다_판_종목의_같은_날_행은_시각을_정하지_않는다() {
    LocalDate day = LocalDate.parse("2026-10-02");
    TradeProfit held = pricedAt(day, "2026-10-02T09:19:37Z"); // 보유 1 주, 18:19 KST - 종가
    TradeProfit soldBase = holding(0, day);
    TradeProfit sold =
        new TradeProfit(
            soldBase.stockItemId(),
            soldBase.stockItemName(),
            soldBase.accountId(),
            soldBase.accountName(),
            soldBase.totalBuyAmount(),
            soldBase.averageBuyPrice(),
            soldBase.totalSellQuantity(),
            soldBase.averageSellPrice(),
            soldBase.totalSellAmount(),
            soldBase.realizedProfit(),
            0,
            soldBase.currentPrice(),
            soldBase.evaluationAmount(),
            soldBase.evaluationProfit(),
            soldBase.totalProfit(),
            soldBase.totalBuyFee(),
            soldBase.totalSellFee(),
            soldBase.totalSellTax(),
            soldBase.totalBuyCost(),
            soldBase.totalSellProceeds(),
            soldBase.averageBuyPriceNet(),
            soldBase.averageSellPriceNet(),
            soldBase.realizedProfitNet(),
            soldBase.evaluationProfitNet(),
            soldBase.totalProfitNet(),
            day,
            java.time.Instant.parse("2026-10-02T01:16:02Z")); // 다 판 종목, 10:16 KST
    assertThat(StockPriceBasisUtil.intradayTime(List.of(held, sold), day)).isNull();
    // 보유가 하나도 없으면(전량 매도 화면) 그 행들로 판정한다
    assertThat(StockPriceBasisUtil.intradayTime(List.of(sold), day)).isEqualTo("10:16");
  }
}
