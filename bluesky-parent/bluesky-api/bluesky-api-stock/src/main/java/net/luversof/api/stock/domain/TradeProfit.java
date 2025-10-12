package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class TradeProfit {
	
	private UUID stockItemId;
	private UUID accountId;

	// 매수 관련 정보
	private BigDecimal totalBuyAmount;
	private BigDecimal averageBuyPrice;

	// 매도 관련 정보 (실현 손익)
	private int totalSellQuantity;
	private BigDecimal averageSellPrice;
	private BigDecimal totalSellAmount;
	private BigDecimal realizedProfit;

	// 보유 관련 정보 (미실현 손익)
	private int holdingQuantity;
	private BigDecimal currentPrice;
	private BigDecimal evaluationAmount;
	private BigDecimal evaluationProfit;

	// 총 손익 (실현 + 미실현)
	private BigDecimal totalProfit;

}