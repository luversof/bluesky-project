package net.luversof.web.gate.stock.dto.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 기간을 달 또는 해로 쪼갠 성과 한 줄.
 *
 * <p>{@code complete} 는 그 달/해를 온전히 덮었는지다. 조회 기간이 경계를 가로지르면 일부만 담기므로, 밝히지 않으면 "8월 -3%" 를 그 달 전체 성과로
 * 오해한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TradeProfitPeriodSummary(
    String unit,
    String label,
    LocalDate fromDate,
    LocalDate toDate,
    boolean complete,
    TradeProfitTimeSeriesSummary summary) {}
