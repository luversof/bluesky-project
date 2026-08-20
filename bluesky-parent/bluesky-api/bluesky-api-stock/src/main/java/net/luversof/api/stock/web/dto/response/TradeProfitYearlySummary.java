package net.luversof.api.stock.web.dto.response;

/** 연도별 성과. 해당 연도 구간의 일별 시리즈로 계산하며, 기초값은 전년도 마지막 지점을 쓴다(연초 첫날의 수익률이 누락되지 않도록). */
public record TradeProfitYearlySummary(int year, TradeProfitTimeSeriesSummary summary) {}
