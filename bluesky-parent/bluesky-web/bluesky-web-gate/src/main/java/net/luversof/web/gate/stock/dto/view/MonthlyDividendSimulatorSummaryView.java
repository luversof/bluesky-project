package net.luversof.web.gate.stock.dto.view;

import java.math.BigDecimal;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;

public record MonthlyDividendSimulatorSummaryView(
    int itemCount,
    BigDecimal totalLatestMonthlyDividend,
    BigDecimal totalLatestMonthlyDividendMidMonth,
    BigDecimal totalLatestMonthlyDividendMonthEnd,
    BigDecimal totalExpectedMonthlyDividend,
    BigDecimal totalExpectedAnnualDividend,
    BigDecimal totalExpectedTaxableBaseAmount,
    BigDecimal totalExpectedAnnualTaxableBaseAmount,
    BigDecimal totalBuyAmount,
    BigDecimal totalCurrentMarketValue,
    BigDecimal portfolioExpectedAnnualYieldPct,
    MonthlyDividendSnapshotResponse bestChoice,
    /*
     * 스냅샷 id -> 비교 우위 순위(1 부터). bestChoice 는 1 위다. 화면은 오래도록 1 위 하나만 보여 줬다 - 사용자 요청
     * 2026-09-17: "비교 우위 순위를 확인할 수 있으면".
     */
    java.util.Map<java.util.UUID, Integer> comparisonRanks) {}
