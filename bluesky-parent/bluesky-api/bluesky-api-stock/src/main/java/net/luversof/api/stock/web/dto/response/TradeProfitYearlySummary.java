package net.luversof.api.stock.web.dto.response;

import java.time.LocalDate;

/**
 * 연도별 성과. 해당 연도 구간의 일별 시리즈로 계산하며, 기초값은 전년도 마지막 지점을 쓴다(연초 첫날의 수익률이 누락되지 않도록).
 *
 * <p>조회 기간이 연도를 가로지르면 그 해의 일부만 담기므로, 실제로 덮은 구간(fromDate~toDate)과 온전한 한 해인지 여부(fullYear)를 함께 준다. 이게
 * 없으면 "3개월"을 보는 중에 표에 뜬 "2026 -9.7%"를 그 해 전체 성과로 오해한다.
 */
public record TradeProfitYearlySummary(
    int year,
    LocalDate fromDate,
    LocalDate toDate,
    boolean fullYear,
    TradeProfitTimeSeriesSummary summary) {}
