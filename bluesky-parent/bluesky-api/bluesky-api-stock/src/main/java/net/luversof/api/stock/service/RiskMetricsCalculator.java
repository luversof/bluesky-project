package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import net.luversof.api.stock.domain.StockDailyClosePrice;

/**
 * 최대낙폭과 변동성(사용자 요청 2026-09-22: "2년치 시세가 있으니 낙폭·변동성도").
 *
 * <p>합산 수익률만 보면 <b>어떻게 벌었는지</b>를 못 본다. 실측 2026-09-22(1 년): TIGER 배당커버드콜액티브는 합산 +165% 인데 그 사이 전고점에서
 * 43.1% 빠졌고 변동성이 60.4% 였다. 같은 자리의 RISE 200위클리커버드콜은 +89.8% 에 낙폭 34.2% · 변동성 45.7% 다.
 *
 * <ul>
 *   <li><b>최대낙폭</b>: 그 기간 안에서 전고점 대비 가장 많이 빠진 폭(음수 %). 한 번에 얼마나 잃을 수 있었는지.
 *   <li><b>변동성</b>: 일간 수익률의 표준편차를 연환산(&radic;252)한 값(%). 얼마나 출렁였는지.
 * </ul>
 *
 * <p><b>분배금은 빼고 가격만 본다.</b> 낙폭은 "내 잔고가 얼마나 줄었나" 가 아니라 "가격이 얼마나 빠졌나" 를 묻는 값이고, 커버드콜은 분배금이 커서 섞으면 낙폭이
 * 실제보다 얕게 보인다.
 *
 * <p><b>이력이 그 기간을 못 덮으면 있는 만큼으로 낸다.</b> 대신 <b>언제부터</b> 센 것인지 함께 돌려준다 &mdash; 실측 2026-09-22: 보유 8 종목
 * 중 둘은 이력이 258 일 · 243 일뿐이라 "1 년 기준" 이라 적으면 거짓이 된다.
 */
public final class RiskMetricsCalculator {

  private RiskMetricsCalculator() {}

  /** 연환산에 쓰는 거래일 수. */
  public static final int TRADING_DAYS_PER_YEAR = 252;

  /** 변동성을 내려면 최소 이만큼의 수익률이 있어야 한다. 둘로는 표준편차가 뜻을 갖지 못한다. */
  public static final int MINIMUM_RETURNS = 20;

  /**
   * 위험 지표.
   *
   * @param fromDate 실제로 센 첫 거래일. 이력이 기간을 못 덮으면 요청한 날보다 늦다
   * @param tradingDays 센 거래일 수
   * @param maxDrawdownPct 전고점 대비 최대 낙폭(%). 늘 0 이하다
   * @param volatilityPct 연환산 변동성(%). 표본이 모자라면 {@code null}
   */
  public record RiskMetrics(
      LocalDate fromDate, int tradingDays, BigDecimal maxDrawdownPct, BigDecimal volatilityPct) {}

  /**
   * 기간 위험 지표.
   *
   * @param pricesAsc 날짜 오름차순 종가(거래량 0 인 날은 이미 빠져 있다)
   * @param today 기준일
   * @param months 거슬러 갈 개월 수
   * @return 셀 것이 없으면 {@code null}
   */
  public static RiskMetrics compute(
      List<StockDailyClosePrice> pricesAsc, LocalDate today, int months) {
    if (pricesAsc == null || pricesAsc.isEmpty() || today == null || months <= 0) {
      return null;
    }

    LocalDate from = today.minusMonths(months);
    List<StockDailyClosePrice> window = new ArrayList<>();
    for (StockDailyClosePrice point : pricesAsc) {
      if (point == null
          || point.tradeDate() == null
          || point.closePrice() == null
          || point.closePrice().signum() <= 0) {
        continue;
      }
      if (!point.tradeDate().isBefore(from)) {
        window.add(point);
      }
    }
    if (window.size() < 2) {
      return null;
    }

    return new RiskMetrics(
        window.get(0).tradeDate(), window.size(), maxDrawdown(window), volatility(window));
  }

  /** 전고점 대비 최대 낙폭(%). 오르기만 했으면 0 이다. */
  static BigDecimal maxDrawdown(List<StockDailyClosePrice> window) {
    BigDecimal peak = window.get(0).closePrice();
    BigDecimal worst = BigDecimal.ZERO;
    for (StockDailyClosePrice point : window) {
      BigDecimal close = point.closePrice();
      if (close.compareTo(peak) > 0) {
        peak = close;
      }
      BigDecimal drop =
          close
              .subtract(peak)
              .multiply(BigDecimal.valueOf(100))
              .divide(peak, 2, RoundingMode.HALF_UP);
      if (drop.compareTo(worst) < 0) {
        worst = drop;
      }
    }
    return worst;
  }

  /**
   * 일간 수익률 표준편차를 연환산한 변동성(%).
   *
   * <p>표본 표준편차({@code n-1})를 쓴다. 이 종가들은 "있을 수 있는 모든 날" 이 아니라 그중 우리가 받은 날들이다.
   */
  static BigDecimal volatility(List<StockDailyClosePrice> window) {
    List<BigDecimal> returns = new ArrayList<>();
    for (int index = 1; index < window.size(); index++) {
      BigDecimal before = window.get(index - 1).closePrice();
      if (before.signum() <= 0) {
        continue;
      }
      returns.add(
          window.get(index).closePrice().subtract(before).divide(before, MathContext.DECIMAL64));
    }
    if (returns.size() < MINIMUM_RETURNS) {
      // 표본이 모자라면 지어내지 않는다 - 며칠치로 낸 변동성은 숫자일 뿐이다.
      return null;
    }

    BigDecimal sum = BigDecimal.ZERO;
    for (BigDecimal value : returns) {
      sum = sum.add(value);
    }
    BigDecimal mean = sum.divide(BigDecimal.valueOf(returns.size()), MathContext.DECIMAL64);

    BigDecimal squares = BigDecimal.ZERO;
    for (BigDecimal value : returns) {
      BigDecimal diff = value.subtract(mean);
      squares = squares.add(diff.multiply(diff));
    }
    BigDecimal variance =
        squares.divide(BigDecimal.valueOf(returns.size() - 1L), MathContext.DECIMAL64);
    double annualized = Math.sqrt(variance.doubleValue()) * Math.sqrt(TRADING_DAYS_PER_YEAR) * 100;
    return BigDecimal.valueOf(annualized).setScale(2, RoundingMode.HALF_UP);
  }
}
