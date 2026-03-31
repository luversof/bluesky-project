package net.luversof.web.gate.stock.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record TradeProfit(
        UUID stockItemId,
        String stockItemName,
        UUID accountId,
        String accountName,

        // 매수 관련 정보 (수수료 제외: 기존 호환 유지)
        BigDecimal totalBuyAmount,
        BigDecimal averageBuyPrice,

        // 매도 관련 정보 (실현 손익 - 수수료/거래세 제외: 기존 호환 유지)
        int totalSellQuantity,
        BigDecimal averageSellPrice,
        BigDecimal totalSellAmount,
        BigDecimal realizedProfit,

        // 보유 관련 정보 (미실현 손익 - 수수료 제외: 기존 호환 유지)
        int holdingQuantity,
        BigDecimal currentPrice,
        BigDecimal evaluationAmount,
        BigDecimal evaluationProfit,

        // 총 손익 (실현 + 미실현) - 수수료 제외: 기존 호환 유지
        BigDecimal totalProfit,

        // -----------------------------
        // 수수료/거래세 포함한 실제 손익 계산을 위한 추가 필드
        // -----------------------------
        // 합계 수수료/거래세
        BigDecimal totalBuyFee, // BUY 거래 수수료 합계
        BigDecimal totalSellFee, // SELL 거래 수수료 합계
        BigDecimal totalSellTax, // SELL 거래세 합계

        // 순수 매수/매도 금액 (수수료/세금 반영)
        BigDecimal totalBuyCost, // 매수 총원가 = 총매수액 + 매수수수료
        BigDecimal totalSellProceeds, // 매도 총수령액 = 총매도액 - 매도수수료 - 거래세

        // 수수료/세금 반영 평단
        BigDecimal averageBuyPriceNet, // (총매수액+수수료)/수량
        BigDecimal averageSellPriceNet, // (총매도액-수수료-세금)/수량

        // 실현/미실현/총 손익 (수수료/세금 반영)
        BigDecimal realizedProfitNet, // 총수령액 - 총원가
        BigDecimal evaluationProfitNet, // 평가액 - (평단NET*보유수량)
        BigDecimal totalProfitNet // realizedNet + evaluationNet
        ) {}
