package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 자산 현황 '종목별 현황' 의 한 종목을 가진 계좌 한 곳.
 *
 * <p>사용자 요청 2026-09-17: 계좌 표의 "보유 종목 보기" 처럼 종목 줄에서도 그 종목을 가진 계좌를 펼쳐 본다. 계좌 표 펼침({@link
 * AssetStatusAccountHoldingView})과 같은 계좌 x 종목 손익 행에서 만든다 &mdash; 두 펼침의 수량 · 금액이 서로 어긋나지 않게.
 *
 * @param stockWeightPct 이 종목 평가액 가운데 이 계좌 몫(%)
 * @param totalWeightPct 전체 평가액 가운데 이 계좌 몫(%)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AssetStatusStockAccountView(
    UUID accountId,
    String accountName,
    int holdingQuantity,
    BigDecimal averageBuyPrice,
    BigDecimal evaluationAmount,
    BigDecimal buyAmount,
    BigDecimal evaluationProfit,
    BigDecimal evaluationProfitRatePct,
    BigDecimal stockWeightPct,
    BigDecimal totalWeightPct) {}
