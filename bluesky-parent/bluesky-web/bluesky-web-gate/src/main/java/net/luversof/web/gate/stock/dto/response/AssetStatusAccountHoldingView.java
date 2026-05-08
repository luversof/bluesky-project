package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;

public record AssetStatusAccountHoldingView(
		String stockItemName,
		int holdingQuantity,
		BigDecimal averageBuyPrice,
		BigDecimal currentPrice,
		BigDecimal evaluationAmount,
		BigDecimal buyAmount,
		BigDecimal evaluationProfit,
		BigDecimal evaluationProfitRatePct,
		BigDecimal accountWeightPct,
		BigDecimal totalWeightPct) {
}