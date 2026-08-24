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
}
