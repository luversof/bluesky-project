package net.luversof.web.gate.stock.dto.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 연도별 성과. 조회 기간이 연도를 가로지르면 그 해의 일부만 담기므로 실제 구간과 온전한 해 여부를 함께 받는다. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TradeProfitYearlySummary(
    int year,
    LocalDate fromDate,
    LocalDate toDate,
    boolean fullYear,
    TradeProfitTimeSeriesSummary summary) {}
