package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import net.luversof.api.stock.domain.StockDailyClosePrice;

/**
 * 기간 수익률(사용자 요청 2026-09-21: "연 성장률 기준 수익률 · 배당 수익률 · 합산 수익률과 1 · 3 · 6 · 12 개월 수익률").
 *
 * <p>두 가지를 낸다.
 *
 * <ul>
 *   <li><b>가격 수익률</b> = (기말 종가 - 기초 종가) &divide; 기초 종가. 분배금을 뺀 순수 가격 변화다.
 *   <li><b>합산(총) 수익률</b> = (기말 종가 - 기초 종가 + 그 기간 분배금) &divide; 기초 종가. 커버드콜처럼 분배금이 큰 상품은 이 값으로 견줘야
 *       한다 &mdash; 분배금만 보면 원금이 줄어드는 상품을 좋게 오해한다.
 * </ul>
 *
 * <p>분배금은 <b>지급기준일</b>로 센다. 가격이 떨어지는 날이 기준일이므로, 지급일로 세면 가격 하락과 분배금이 서로 다른 기간에 들어가 합산이 어긋난다.
 *
 * <p><b>이력이 모자라면 아무 값도 내지 않는다</b>(null). 열흘치 이력으로 "1 년 수익률" 을 적으면 거짓이 된다 &mdash; 화면은 대신 이력 시작일을
 * 알린다.
 */
public final class PeriodReturnCalculator {

  private PeriodReturnCalculator() {}

  /** 한 기간의 결과. 기초 날짜를 함께 돌려줘 화면이 "무엇과 견줬는지" 를 말할 수 있게 한다. */
  public record PeriodReturn(
      int months, LocalDate baseDate, BigDecimal priceReturnPct, BigDecimal totalReturnPct) {}

  /** 분배금 한 건(지급기준일 · 주당 금액). 이 계산기는 도메인 객체를 모른다. */
  public record PayoutPoint(LocalDate recordDate, BigDecimal amountPerShare) {}

  /**
   * 기간 수익률.
   *
   * @param pricesAsc 날짜 오름차순 종가(거래량 0 인 날은 이미 빠져 있다)
   * @param payouts 분배금(순서 무관)
   * @param today 기준일
   * @param months 거슬러 갈 개월 수
   * @return 이력이 그 기간을 덮지 못하면 {@code null}
   */
  public static PeriodReturn compute(
      List<StockDailyClosePrice> pricesAsc,
      List<PayoutPoint> payouts,
      LocalDate today,
      int months) {
    if (pricesAsc == null || pricesAsc.size() < 2 || today == null || months <= 0) {
      return null;
    }

    LocalDate target = today.minusMonths(months);
    StockDailyClosePrice base = lastOnOrBefore(pricesAsc, target);
    if (base == null) {
      // 이력이 기간을 못 덮는다 - 지어내지 않는다.
      return null;
    }

    StockDailyClosePrice last = pricesAsc.get(pricesAsc.size() - 1);
    if (last.tradeDate() == null
        || !last.tradeDate().isAfter(base.tradeDate())
        || base.closePrice() == null
        || base.closePrice().signum() <= 0
        || last.closePrice() == null) {
      return null;
    }

    BigDecimal priceChange = last.closePrice().subtract(base.closePrice());
    BigDecimal dividends = sumPayouts(payouts, base.tradeDate(), last.tradeDate());
    return new PeriodReturn(
        months,
        base.tradeDate(),
        percent(priceChange, base.closePrice()),
        percent(priceChange.add(dividends), base.closePrice()));
  }

  /** 기간 안 분배금 합(기초일 <b>다음날</b>부터 기말일까지). 기초일 당일 기준일 분배금은 이미 기초 가격에 반영돼 있다. */
  static BigDecimal sumPayouts(
      List<PayoutPoint> payouts, LocalDate afterDate, LocalDate untilDate) {
    if (payouts == null || payouts.isEmpty()) {
      return BigDecimal.ZERO;
    }

    BigDecimal sum = BigDecimal.ZERO;
    for (PayoutPoint payout : payouts) {
      if (payout == null || payout.recordDate() == null || payout.amountPerShare() == null) {
        continue;
      }
      if (payout.recordDate().isAfter(afterDate) && !payout.recordDate().isAfter(untilDate)) {
        sum = sum.add(payout.amountPerShare());
      }
    }
    return sum;
  }

  /** 그 날 또는 그 전의 마지막 종가(휴장일이면 직전 거래일). 날짜 오름차순 목록을 뒤에서부터 본다. */
  static StockDailyClosePrice lastOnOrBefore(List<StockDailyClosePrice> pricesAsc, LocalDate date) {
    for (int index = pricesAsc.size() - 1; index >= 0; index--) {
      StockDailyClosePrice point = pricesAsc.get(index);
      if (point.tradeDate() != null && !point.tradeDate().isAfter(date)) {
        return point;
      }
    }
    return null;
  }

  private static BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
    return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
  }
}
