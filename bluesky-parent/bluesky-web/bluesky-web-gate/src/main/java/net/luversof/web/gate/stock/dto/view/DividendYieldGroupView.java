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
    Instant lastDividendDate) {}
