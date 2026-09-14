package net.luversof.api.stock.domain;

import java.math.BigDecimal;

/**
 * 연도별 매매 비용·실현손익 집계(조회 전용 투영).
 *
 * <p>{@code sellCount} 는 그 해 매도 건수다. 실현손익 0 이 <b>"판 적이 없다"</b> 인지 <b>"팔았는데 0 원"</b> 인지는 금액만으로는 갈리지
 * 않는다 &mdash; 실측 2026-09-12: 14 개 해 중 셋(2024·2017·2009)이 실현손익 0 인데 모두 매도 0 건이었다.
 */
public record YearlyTradeCost(
    int year, BigDecimal fee, BigDecimal tax, BigDecimal realizedProfit, long sellCount) {}
