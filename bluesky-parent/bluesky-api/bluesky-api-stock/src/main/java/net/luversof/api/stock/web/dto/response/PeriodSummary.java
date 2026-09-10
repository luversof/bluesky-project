package net.luversof.api.stock.web.dto.response;

import java.math.BigDecimal;

/**
 * 한 기간의 매매·배당 합계 한 줄.
 *
 * <p>대시보드는 전기간 합계만 보여 주고, "올해 얼마 벌었나" 는 화면 네 곳에 흩어져 있었다(실측 2026-09-10: 기간 매매· 실현손익·거래비용은 매매 화면, 기간
 * 배당·세금·월평균은 배당 화면). 그 값을 얻자고 원장을 통째로 내려받으면 응답이 원장 크기를 따라가므로(올해 매매 258 행·배당 202 행) 합계만 DB 에서 내어 한
 * 줄로 돌려준다.
 *
 * <p>배당은 <b>지급일</b> 기준이다(원천징수가 그때 일어난다). 매매는 거래일 기준이다.
 */
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
