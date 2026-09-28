package net.luversof.web.gate.stock.dto.view;

import java.math.BigDecimal;
import java.time.Instant;

public record DividendYieldGroupView(
    /** 상세 링크용 id(종목·계좌 행에만 존재). 이름으로 되찾던 매핑을 대체한다. */
    java.util.UUID groupId,
    String label,
    BigDecimal totalGrossAmount,
    BigDecimal totalNetAmount,
    BigDecimal totalTaxableAmount,
    /**
     * 기준일 원금/시가가 있는 배당만 모은 세후 합계. 합계행이 행과 같은 규칙으로 수익률을 내도록 실어 보낸다 — 분모(평균원금)에 기여하지 않은 배당을 분자에만 넣으면
     * 수익률이 과대 계상된다.
     */
    BigDecimal netAmountWithPrincipalCost,
    BigDecimal netAmountWithPrincipalMarket,
    BigDecimal averageDailyPrincipalCost,
    BigDecimal averagePrincipalCost,
    BigDecimal averagePrincipalMarketValue,
    BigDecimal yieldOnDailyAverageCostPct,
    BigDecimal yieldOnCostPct,
    BigDecimal yieldOnMarketPct,
    long dividendCount,
    Instant lastDividendDate,
    /**
     * 기간 안에서 날마다 들고 있던 매입원금의 합(원 x 일). 연 수익률의 분모다 - 합계행은 이 값을 더해 행과 같은 규칙으로 다시 계산한다.
     *
     * <p>아래 네 값은 사용자 요청 2026-09-28("수익률이 잘 눈에 안 들어온다")로 더했다. 예전 기본 지표(기간 일평균 투입원금 수익률)는 들고 있지 않은 날도
     * 0 원으로 세어 기간 전체로 나눠, 최근에 산 종목이 284% 처럼 부풀었다(실측: 전체 기간 2,354 일 중 약 115 일 보유).
     */
    BigDecimal principalCostDaySum,
    /** 기간 안에서 하나라도 들고 있던 날 수(계좌 · 종목을 합친 행은 겹치는 날을 한 번만 센다). */
    long heldDayCount,
    /** 들고 있던 날의 평균 매입원금 = principalCostDaySum / heldDayCount. */
    BigDecimal heldAverageDailyPrincipalCost,
    /**
     * 연 수익률 = 세후 배당 x 365 / principalCostDaySum = (세후 배당 / 들고 있던 날 평균 원금) x (365 / 들고 있던 날 수). 오래
     * 들고 있던 것과 최근 산 것을 같은 잣대로 본다.
     */
    BigDecimal annualizedYieldPct,
    /** 들고 있던 날이 짧아(90 일 또는 기간 전체 중 짧은 쪽 미만) 연 수익률이 크게 흔들리는 행. 순위에서 뒤로 보내고 표시를 단다. */
    boolean shortHeld,
    /** 이 행이 덮는 기간 일수(연도 행은 그 해 안의 일수) - 올해처럼 1 년이 안 차는 해를 표시한다. */
    long periodDayCount) {

  /** 연 수익률이 크게 흔들리는 보유 일수의 문턱. */
  public static final long SHORT_HELD_DAYS = 90;

  /** 예전 모양(연 수익률 이전) - 시험과 새 값을 모르는 곳이 쓴다. */
  public DividendYieldGroupView(
      java.util.UUID groupId,
      String label,
      BigDecimal totalGrossAmount,
      BigDecimal totalNetAmount,
      BigDecimal totalTaxableAmount,
      BigDecimal netAmountWithPrincipalCost,
      BigDecimal netAmountWithPrincipalMarket,
      BigDecimal averageDailyPrincipalCost,
      BigDecimal averagePrincipalCost,
      BigDecimal averagePrincipalMarketValue,
      BigDecimal yieldOnDailyAverageCostPct,
      BigDecimal yieldOnCostPct,
      BigDecimal yieldOnMarketPct,
      long dividendCount,
      Instant lastDividendDate) {
    this(
        groupId,
        label,
        totalGrossAmount,
        totalNetAmount,
        totalTaxableAmount,
        netAmountWithPrincipalCost,
        netAmountWithPrincipalMarket,
        averageDailyPrincipalCost,
        averagePrincipalCost,
        averagePrincipalMarketValue,
        yieldOnDailyAverageCostPct,
        yieldOnCostPct,
        yieldOnMarketPct,
        dividendCount,
        lastDividendDate,
        null,
        0L,
        null,
        null,
        false,
        0L);
  }

  /** 들고 있던 날이 짧은가. 기간 자체가 90 일보다 짧으면 기간 전체를 들고 있었을 때는 짧다고 하지 않는다. */
  public static boolean isShortHeld(long heldDayCount, long periodDayCount) {
    long threshold = Math.min(SHORT_HELD_DAYS, periodDayCount);
    return heldDayCount > 0 && heldDayCount < threshold;
  }
}
