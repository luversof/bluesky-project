package net.luversof.api.stock.domain;

import java.math.BigDecimal;

/**
 * 기간 배당 집계(조회 전용 투영).
 *
 * <p>연도별 집계(YearlyDividendIncome)와 같은 기준이다 - <b>지급일</b>로 기간에 넣는다(원천징수가 그때 일어난다).
 */
public record PeriodDividendSummary(
    BigDecimal grossAmount, BigDecimal taxableAmount, BigDecimal tax, BigDecimal fee, long count) {}
