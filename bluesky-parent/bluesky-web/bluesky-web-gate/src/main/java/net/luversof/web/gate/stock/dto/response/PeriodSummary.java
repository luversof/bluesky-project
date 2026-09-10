package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 한 기간의 매매·배당 합계(api-stock 의 /api/periodSummary 응답).
 *
 * <p>대시보드는 전기간 합계만 보여 주고 "올해 얼마 벌었나" 는 화면 네 곳에 흩어져 있었다(실측 2026-09-10). 같은 값을 얻자고 원장을 받으면 응답이 원장 크기를
 * 따라간다 - 실측: 이 집계 268 바이트, 매매 목록 33,160 · 배당 목록 48,577.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PeriodSummary(
    BigDecimal buyAmount,
    BigDecimal sellAmount,
    BigDecimal tradeFee,
    BigDecimal tradeTax,
    BigDecimal realizedProfit,
    long buyCount,
    long sellCount,
    BigDecimal dividendGross,
    BigDecimal dividendTaxable,
    BigDecimal dividendTax,
    BigDecimal dividendFee,
    BigDecimal dividendNet,
    long dividendCount) {}
