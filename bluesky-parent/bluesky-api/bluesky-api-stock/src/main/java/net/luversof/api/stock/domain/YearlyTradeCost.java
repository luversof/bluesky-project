package net.luversof.api.stock.domain;

import java.math.BigDecimal;

/** 연도별 매매 비용·실현손익 집계(조회 전용 투영). */
public record YearlyTradeCost(
    int year, BigDecimal fee, BigDecimal tax, BigDecimal realizedProfit) {}
