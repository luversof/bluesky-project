package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AssetStatusAccountHoldingView(
    /** 상세 링크용. 이름으로 id 를 되찾으면 같은 이름의 종목이 둘일 때 엉뚱한 종목으로 연결된다. */
    java.util.UUID stockItemId,
    String stockItemName,
    int holdingQuantity,
    BigDecimal averageBuyPrice,
    BigDecimal currentPrice,
    BigDecimal evaluationAmount,
    BigDecimal buyAmount,
    BigDecimal evaluationProfit,
    BigDecimal evaluationProfitRatePct,
    BigDecimal accountWeightPct,
    BigDecimal totalWeightPct) {}
