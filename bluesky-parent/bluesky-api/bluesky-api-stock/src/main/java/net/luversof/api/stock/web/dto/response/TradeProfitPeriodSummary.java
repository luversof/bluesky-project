package net.luversof.api.stock.web.dto.response;

import java.time.LocalDate;

/**
 * 기간을 <b>달 또는 해</b>로 쪼갠 성과 한 줄.
 *
 * <p>{@link TradeProfitYearlySummary} 와 같은 계산인데 쪼개는 단위만 다르다. 연 단위만 있으면 "올해" 처럼 짧은 구간을 볼 때 표가 한 줄뿐이라
 * 아무것도 말해 주지 않는다(실측 2026-08-31 삼성전자 '올해': 연도별 1 행).
 *
 * <p>{@code complete} 는 그 단위를 <b>온전히</b> 덮었는지다. 조회 기간이 달/해를 가로지르면 일부만 담기므로, 그 사실을 밝히지 않으면 "8월 -3%"
 * 를 그 달 전체 성과로 오해한다.
 */
public record TradeProfitPeriodSummary(
    /** {@code YEAR} 또는 {@code MONTH}. */
    String unit,
    /** 화면에 그대로 쓰는 이름 &mdash; {@code 2026} 또는 {@code 2026-08}. */
    String label,
    LocalDate fromDate,
    LocalDate toDate,
    boolean complete,
    TradeProfitTimeSeriesSummary summary) {}
