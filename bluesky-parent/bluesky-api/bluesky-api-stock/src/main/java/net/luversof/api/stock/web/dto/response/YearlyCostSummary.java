package net.luversof.api.stock.web.dto.response;

import java.math.BigDecimal;

/**
 * 연도별 세금·비용 요약 한 줄.
 *
 * <p>이 값들은 원장에 다 있는데 <b>합계를 내는 화면이 없었다</b>. 연말정산·금투세 때 필요한 수(그 해 배당 세전·원천징수·세후, 실현손익, 매매에 든
 * 수수료·증권거래세)를 한 줄로 모은다.
 *
 * <p>실측 2026-09-01: 2026 년 수수료 20,933 · 거래세 616,963 · 배당 세전 26,164,415 · 원천징수 1,412,730.
 *
 * <p>매매는 거래일, 배당은 <b>지급일</b> 기준으로 해에 넣는다 &mdash; 원천징수가 그 때 일어나므로 세금 관점에서는 지급일이 맞다.
 */
public record YearlyCostSummary(
    int year,
    BigDecimal tradeFee,
    BigDecimal tradeTax,
    BigDecimal realizedProfit,
    BigDecimal dividendGross,
    /**
     * 그 해 배당의 <b>과세금액</b> 합. 시트에 적힌 값을 그대로 더한 것이다.
     *
     * <p>세전에 세율을 곱한 값이 아니다 &mdash; 계좌·종목에 따라 분리과세 혜택이 있어 혜택 뒤 남은 몫만 과세된다. 금융소득종합과세 판정은 이 값으로 한다.
     */
    BigDecimal dividendTaxable,
    BigDecimal dividendTax,
    BigDecimal dividendNet) {}
