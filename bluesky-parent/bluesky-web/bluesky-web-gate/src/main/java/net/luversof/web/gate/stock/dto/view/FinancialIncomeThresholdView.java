package net.luversof.web.gate.stock.dto.view;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 배당 내역 위의 '올해 금융소득 종합과세 기준' &mdash; 올해 받은 과세 대상 배당이 2,000 만 원에 얼마나 다가갔는지(2026-10-20 자율 개선).
 *
 * <p>합은 과세 계좌(ISA · 연금 같은 과세이연 계좌 제외)의 <b>과세표준</b>이다 &mdash; 세전 금액이 아니다. 리츠 · 인프라 분배금처럼 9.9% 분리과세로
 * 끝나는 부분과 비과세 분배금은 과세표준에 들지 않아 종합과세 합산에서도 빠진다(실측 2026-10-20: KODEX 한국부동산리츠인프라 세전 297,424 원 중 과세표준
 * 2,233 원, 세금 29,210 원 = 세전의 9.8%).
 *
 * <p>연말 예상 = 지금까지 + 앞으로 더 받을 것. 더 받을 것은 종목마다 나눠 셈한다(2026-10-20 검토 반영) &mdash; 과세 계좌의 과세표준은 덩어리다(실측:
 * 삼성전자 분기 배당이 4 · 5 · 8 · 11 월에 한 번에 190 ~ 340 만 원, 나머지 월배당 ETF 는 달마다 수천 ~ 수십만 원). 최근 90 일 하루 평균
 * 하나로 늘리면 8 월 덩어리를 날마다 받는 돈처럼 키우고 11 월 덩어리는 놓친다.
 *
 * <ul>
 *   <li><b>달마다 나오는 종목</b>(지급 간격 중앙값 45 일 이하): 최근 {@code recentWindowDays} 일 그 종목들 합의 하루 평균 &times;
 *       남은 날. 그만둔 종목은 최근 합이 0 이라 저절로 빠진다.
 *   <li><b>분기 · 연 배당 종목</b>: 작년 오늘 뒤 ~ 12 월 31 일에 받은 횟수 &times; 가장 최근 1 회 금액(보유 수량이 바뀐 것을 따른다).
 * </ul>
 *
 * <p>둘 다 과세 계좌에 지금 들고 있는 종목만 센다(판 종목은 앞으로 안 나온다). 보유를 못 읽으면 빼지 않는다.
 *
 * @param year 올해(서버 존)
 * @param asOf 기준일(오늘)
 * @param ytdTaxable 올해 1 월 1 일 ~ 오늘 지급분의 과세표준 합(과세 계좌)
 * @param unknownTaxableCount 그 기간 과세 계좌 배당 중 과세표준이 비어 합에 못 넣은 건수 &mdash; 0 이 아니면 합이 모자랄 수 있다
 * @param lastYearTaxable 작년 한 해의 같은 합(비교용, 과세표준이 빈 작년 행은 빠진다 - 0 이면 화면에 안 적는다)
 * @param threshold 기준 금액(2,000 만 원)
 * @param recentWindowDays 연말 예상에 쓴 최근 기간(일)
 * @param recentTaxable 최근 기간의 과세표준 합 &mdash; <b>달마다 나오는 종목만</b>
 * @param remainingDays 오늘 다음 날부터 12 월 31 일까지 남은 날
 * @param periodicRest 분기 · 연 배당 종목의 올해 남은 예상(원)
 * @param periodicCount 그 예상에 든 지급 횟수(작년 같은 때 기준)
 */
public record FinancialIncomeThresholdView(
    int year,
    LocalDate asOf,
    BigDecimal ytdTaxable,
    long unknownTaxableCount,
    BigDecimal lastYearTaxable,
    BigDecimal threshold,
    int recentWindowDays,
    BigDecimal recentTaxable,
    long remainingDays,
    BigDecimal periodicRest,
    int periodicCount) {

  /** 금융소득 종합과세 기준 금액(이자 · 배당 합 2,000 만 원 초과분이 다른 소득과 합산된다). */
  public static final BigDecimal THRESHOLD = new BigDecimal("20000000");

  /** 기준까지 남은 금액(넘었으면 0). */
  public BigDecimal remaining() {
    BigDecimal left = threshold.subtract(ytdTaxable);
    return left.signum() > 0 ? left : BigDecimal.ZERO;
  }

  /** 기준을 넘은 금액(넘지 않았으면 0). */
  public BigDecimal over() {
    BigDecimal over = ytdTaxable.subtract(threshold);
    return over.signum() > 0 ? over : BigDecimal.ZERO;
  }

  /** 기준을 넘었는가(같으면 넘지 않았다 - 초과분이 합산된다). */
  public boolean exceeded() {
    return ytdTaxable.compareTo(threshold) > 0;
  }

  /** 기준 대비 비율(%), 소수 첫째 자리. */
  public BigDecimal usedPct() {
    return pctOf(ytdTaxable);
  }

  /** 막대 너비(0~100). */
  public int usedBarPct() {
    return Math.max(0, Math.min(100, usedPct().setScale(0, RoundingMode.DOWN).intValue()));
  }

  /** 최근 기간의 하루 평균 과세표준(원, 반올림 전 소수 둘째 자리). */
  public BigDecimal recentDailyAverage() {
    if (recentWindowDays <= 0 || recentTaxable == null) {
      return BigDecimal.ZERO;
    }
    return recentTaxable.divide(BigDecimal.valueOf(recentWindowDays), 2, RoundingMode.HALF_UP);
  }

  /** 달마다 나오는 종목의 올해 남은 예상(하루 평균 x 남은 날, 원 단위 반올림). */
  public BigDecimal monthlyRest() {
    if (remainingDays <= 0) {
      return BigDecimal.ZERO;
    }
    return recentDailyAverage()
        .multiply(BigDecimal.valueOf(remainingDays))
        .setScale(0, RoundingMode.HALF_UP);
  }

  /** 연말까지 더 받을 것으로 보는 과세표준 = 달마다 나오는 종목 + 분기 · 연 배당 종목. */
  public BigDecimal projectedRest() {
    if (remainingDays <= 0) {
      return BigDecimal.ZERO;
    }
    return monthlyRest().add(periodicRest == null ? BigDecimal.ZERO : periodicRest);
  }

  /** 연말 예상 합 = 지금까지 + 더 받을 예상. */
  public BigDecimal projectedYearEnd() {
    return ytdTaxable.add(projectedRest());
  }

  /** 연말 예상의 기준 대비 비율(%). */
  public BigDecimal projectedPct() {
    return pctOf(projectedYearEnd());
  }

  /** 지금은 아래인데 연말 예상이 기준을 넘는가. */
  public boolean projectedToExceed() {
    return !exceeded() && projectedYearEnd().compareTo(threshold) > 0;
  }

  private BigDecimal pctOf(BigDecimal value) {
    if (threshold == null || threshold.signum() <= 0 || value == null) {
      return BigDecimal.ZERO;
    }
    return value.multiply(BigDecimal.valueOf(100)).divide(threshold, 1, RoundingMode.HALF_UP);
  }
}
