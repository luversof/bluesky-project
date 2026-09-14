package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

/**
 * 보유 기간과 연평균 수익률, 그리고 배당이 평가손실을 덮은 비율.
 *
 * <p>2026-09-14 까지 자산 현황에는 "배당이 평가손실을 얼마나 메웠나" 카드가 따로 있었다. 그 카드가 찍던 평가손익 · 누적배당 · 합산손익은 바로 아래 표에 이미
 * 열로 다 있었고, 막대는 줄 안에서만 견주는 정규화(큰 쪽이 100)라 <b>둘 중 하나가 항상 100%</b> 였다 &mdash; 막대 쌍이 나르는 정보는 비율 하나뿐이고
 * 그 비율은 옆 배지에 숫자로 적혀 있었다. 그래서 카드를 지우고, 고유했던 값(상쇄율)과 새 값(보유 기간 · 연평균)을 각각 자기 값이 있는 칸 안으로 옮겼다.
 *
 * <p><b>연평균은 복리(CAGR)</b>다 &mdash; {@code (1 + 수익률)^(365/보유일) - 1}. 단순 환산({@link StockYieldUtil})은
 * 기간 1 회 수익인 배당수익률용이고, 여러 해를 들고 있는 종목에 쓰면 과대평가된다(실측 2026-09-14: 삼성전자 합산 240.2% · 2,385 일 &rarr; 단순
 * 36.8% vs 복리 <b>20.6%</b>, 16%p 차이).
 *
 * <p>보유 9 종목 중 7 종목이 1 년 미만(117~361 일)이라 연환산은 대부분 <b>확대된 값</b>이다(실측: 117 일 -6.0% &rarr; 연 -17.5%).
 * 그래서 1 년 미만인지를 {@link Row#shortTerm()} 로 알려 주고, 화면은 그 줄을 흐리게 + 근거 문구를 붙인다.
 */
public final class StockHoldingReturnUtil {

  private StockHoldingReturnUtil() {}

  /** 연환산이 확대로 읽히는 경계. 이 미만이면 화면이 약하게 표시한다. */
  public static final long SHORT_TERM_DAY_COUNT = 365L;

  /**
   * 한 종목.
   *
   * @param holdingDays 최초 매수일부터 오늘까지 일수. 최초 매수일을 모르면 0
   * @param years 보유 연수(표시용)
   * @param months 연수를 뺀 나머지 개월(표시용)
   * @param annualizedPct 연평균 수익률(복리, %). 낼 수 없으면 널
   * @param coveragePct 누적 배당 ÷ 평가손실 (%). 평가손실이 없으면 널
   * @param shortTerm 보유 1 년 미만
   */
  public record Row(
      long holdingDays,
      int years,
      int months,
      BigDecimal annualizedPct,
      Integer coveragePct,
      boolean shortTerm) {

    /** 보유 기간을 적을 수 있는지. 최초 매수일을 모르면 화면은 이 줄을 비운다. */
    public boolean hasPeriod() {
      return holdingDays > 0;
    }

    /** 1 년이 안 돼 연수/개월로는 못 적는 경우(표시는 일수로). */
    public boolean daysOnly() {
      return years == 0 && months == 0;
    }
  }

  /**
   * @param firstBuyDate 종목의 최초 매수일(널 허용 &mdash; 모르면 기간을 안 적는다)
   * @param today 기준일(요청 존의 오늘)
   * @param buyCost 매수 총원가. 연평균의 분모다
   * @param combinedProfit 합산 손익(평가 + 실현 + 배당). 연평균의 분자다
   * @param evaluationProfit 평가 손익. 음수일 때만 상쇄율을 낸다
   * @param dividendTotal 누적 배당(세후)
   */
  public static Row of(
      LocalDate firstBuyDate,
      LocalDate today,
      BigDecimal buyCost,
      BigDecimal combinedProfit,
      BigDecimal evaluationProfit,
      BigDecimal dividendTotal) {
    long days = 0L;
    int years = 0;
    int months = 0;
    if (firstBuyDate != null && today != null && !firstBuyDate.isAfter(today)) {
      days = ChronoUnit.DAYS.between(firstBuyDate, today);
      Period period = Period.between(firstBuyDate, today);
      years = period.getYears();
      months = period.getMonths();
    }
    return new Row(
        days,
        years,
        months,
        annualizedPct(buyCost, combinedProfit, days),
        coveragePct(evaluationProfit, dividendTotal),
        days > 0 && days < SHORT_TERM_DAY_COUNT);
  }

  /**
   * 연평균 수익률(복리, 소수 한 자리).
   *
   * <p>원금을 다 잃고 더 잃은 경우(1 + 수익률 &le; 0)는 복리로 환산할 수 없다 &mdash; 음수의 분수 거듭제곱이다. 그때는 널을 돌려주고 화면은 연평균을
   * 비운다. 억지로 -100% 를 적으면 "딱 전액 손실" 로 읽힌다.
   */
  static BigDecimal annualizedPct(BigDecimal buyCost, BigDecimal combinedProfit, long days) {
    if (buyCost == null || buyCost.signum() <= 0 || combinedProfit == null || days <= 0) {
      return null;
    }
    double growth = 1d + combinedProfit.doubleValue() / buyCost.doubleValue();
    if (growth <= 0d) {
      return null;
    }
    double annualized = Math.pow(growth, 365d / days) - 1d;
    if (!Double.isFinite(annualized)) {
      return null;
    }
    return BigDecimal.valueOf(annualized * 100d).setScale(1, RoundingMode.HALF_UP);
  }

  /** 누적 배당이 평가손실의 몇 %를 덮었는지. 평가손실이 없으면 널(덮을 것이 없다). */
  static Integer coveragePct(BigDecimal evaluationProfit, BigDecimal dividendTotal) {
    if (evaluationProfit == null || evaluationProfit.signum() >= 0) {
      return null;
    }
    BigDecimal dividend = dividendTotal == null ? BigDecimal.ZERO : dividendTotal;
    if (dividend.signum() <= 0) {
      return 0;
    }
    return dividend
        .multiply(BigDecimal.valueOf(100))
        .divide(evaluationProfit.abs(), 0, RoundingMode.HALF_UP)
        .intValue();
  }
}
