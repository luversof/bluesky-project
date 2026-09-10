package net.luversof.api.stock.domain;

import java.math.BigDecimal;

/**
 * 기간 매매 집계(조회 전용 투영).
 *
 * <p>대시보드가 "올해 얼마 사고 팔았나"를 보여 주려면 지금은 매매 원장을 통째로 받아 더해야 한다(실측 2026-09-10: 올해 258 행). 합계는 DB 에서 한 줄로
 * 낸다 - 연도별 집계(YearlyTradeCost)와 같은 이유다.
 */
public record PeriodTradeSummary(
    BigDecimal buyAmount,
    BigDecimal sellAmount,
    BigDecimal fee,
    BigDecimal tax,
    BigDecimal realizedProfit,
    long buyCount,
    long sellCount) {}
