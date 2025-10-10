package net.luversof.web.gate.stock.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record StockProfit(
	UUID stockItemId,
	UUID accountId,
	
	// 매수 관련 정보
	BigDecimal totalBuyAmount,
	BigDecimal averageBuyPrice,
	
	// 매도 관련 정보 (실현 손익)
	int totalSellQuantity,
	BigDecimal averageSellPrice,
	BigDecimal totalSellAmount,
	BigDecimal realizedProfit,
	
	// 보유 관련 정보 (미실현 손익)
	int holdingQuantity,
	BigDecimal currentPrice,
	BigDecimal evaluationAmount,
	BigDecimal evaluationProfit,
	
	// 총 손익 (실현 + 미실현)
	BigDecimal totalProfit
	) {

}
