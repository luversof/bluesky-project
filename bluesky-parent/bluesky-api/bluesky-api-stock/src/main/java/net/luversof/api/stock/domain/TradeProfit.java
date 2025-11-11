package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class TradeProfit {
	
	private UUID stockItemId;
	private UUID accountId;

	// 매수 관련 정보 (총액/평단 - 수수료 제외: 기존 호환 유지)
	private BigDecimal totalBuyAmount;
	private BigDecimal averageBuyPrice;

	// 매도 관련 정보 (실현 손익 - 수수료/거래세 제외: 기존 호환 유지)
	private int totalSellQuantity;
	private BigDecimal averageSellPrice;
	private BigDecimal totalSellAmount;
	private BigDecimal realizedProfit;

	// 보유 관련 정보 (미실현 손익 - 수수료 제외: 기존 호환 유지)
	private int holdingQuantity;
	private BigDecimal currentPrice;
	private BigDecimal evaluationAmount;
	private BigDecimal evaluationProfit;

	// 총 손익 (실현 + 미실현) - 수수료 제외: 기존 호환 유지
	private BigDecimal totalProfit;

	// -----------------------------
	// 수수료/거래세 포함한 실제 손익 계산을 위한 추가 필드
	// -----------------------------
	// 합계 수수료/거래세
	private BigDecimal totalBuyFee;       // BUY 거래 수수료 합계
	private BigDecimal totalSellFee;      // SELL 거래 수수료 합계
	private BigDecimal totalSellTax;      // SELL 거래세 합계

	// 순수 매수/매도 금액 (수수료/세금 반영)
	// 매수 총원가 = 총매수액 + 매수수수료
	private BigDecimal totalBuyCost;
	// 매도 총수령액 = 총매도액 - 매도수수료 - 거래세
	private BigDecimal totalSellProceeds;

	// 수수료/세금 반영 평단
	private BigDecimal averageBuyPriceNet;   // (총매수액+수수료)/수량
	private BigDecimal averageSellPriceNet;  // (총매도액-수수료-세금)/수량

	// 실현/미실현/총 손익 (수수료/세금 반영)
	private BigDecimal realizedProfitNet;    // 총수령액 - 총원가 (간이 방식)
	private BigDecimal evaluationProfitNet;  // 평가액 - (평단NET*보유수량)
	private BigDecimal totalProfitNet;       // realizedNet + evaluationNet

}