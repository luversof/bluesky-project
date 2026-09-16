package net.luversof.web.gate.stock.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 월배당 기준 데이터 요약.
 *
 * <p>평균 둘은 <b>같은 행을 쓰지 않는다</b> &mdash; 평균 분배금은 최근 12 건(있는 만큼), 과세표준 비중은 그중 분배금이 0 보다 큰 행만 쓴다. 그래서
 * 화면 문구가 쓸 기준 건수를 각자 들고 간다. 실측 2026-09-16: 이력이 11 건뿐인 종목에도 "최근 12건 기준" 이 붙어 있었다.
 */
public record MonthlyDividendReferenceSummaryView(
    String stockItemSymbol,
    int payoutCount,
    BigDecimal latestDividendAmountPerShare,
    BigDecimal averageDividendAmountPerShare1y,
    BigDecimal averageTaxableBaseRatio1y,
    int averageBasisRowCount,
    int taxableRatioBasisRowCount,
    LocalDate latestRecordDate,
    LocalDate latestPayDate) {}
