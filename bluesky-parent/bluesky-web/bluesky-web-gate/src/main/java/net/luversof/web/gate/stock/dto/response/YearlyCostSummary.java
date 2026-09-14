package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 연도별 세금·비용 요약 한 줄.
 *
 * <p>원장에는 다 있는데 합계를 내는 화면이 없던 값들이다. 매매는 거래일, 배당은 <b>지급일</b> 기준으로 해에 들어간다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record YearlyCostSummary(
    int year,
    BigDecimal tradeFee,
    BigDecimal tradeTax,
    BigDecimal realizedProfit,
    BigDecimal dividendGross,
    /** 시트에 적힌 과세금액 합. 세전에 세율을 곱한 값이 아니다 - 계좌별 분리과세 혜택 뒤 남은 몫이다. */
    BigDecimal dividendTaxable,
    BigDecimal dividendTax,
    BigDecimal dividendNet,
    /**
     * 그 해 매도 건수.
     *
     * <p>실현 손익 0 이 "판 적이 없다" 인지 "팔았는데 0 원" 인지는 금액만으로 갈리지 않는다 - 실측 2026-09-12: 14 개 해 중
     * 2024·2017·2009 가 실현손익 0 이었고 셋 다 매도 0 건이었는데, 표는 셋 다 까닭 없는 "-" 한 글자였다.
     */
    long sellCount) {}
