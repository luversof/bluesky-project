@import java.util.List
@import java.math.BigDecimal
@import java.text.DecimalFormat
@import net.luversof.web.gate.stock.dto.response.TradeResponse
@import io.github.luversof.boot.context.support.MessageUtil

@param List<TradeResponse> allTradeList
@param int totalItems
@param BigDecimal totalAllBuyAmount
@param BigDecimal totalAllSellAmount
@param BigDecimal totalAllFee
@param BigDecimal totalAllTax
@param BigDecimal totalAllRealizedProfit
@param DecimalFormat decimalFormat
@param java.util.function.Function<Object, String> countMessage
@param String buyLabel
@param String sellLabel
@param String realizedProfitLabel
@param String feeLabel
@param String tradingTaxLabel

!{
    String periodStatusLabel = MessageUtil.getMessage("stock.trade.label.period.status");
    String tradingCostLabel = MessageUtil.getMessage("stock.trade.label.trading.cost");

    BigDecimal safeBuy = totalAllBuyAmount != null ? totalAllBuyAmount : BigDecimal.ZERO;
    BigDecimal safeSell = totalAllSellAmount != null ? totalAllSellAmount : BigDecimal.ZERO;
    BigDecimal safeFee = totalAllFee != null ? totalAllFee : BigDecimal.ZERO;
    BigDecimal safeTax = totalAllTax != null ? totalAllTax : BigDecimal.ZERO;
    BigDecimal safeRp = totalAllRealizedProfit != null ? totalAllRealizedProfit : BigDecimal.ZERO;
    boolean rpPositive = safeRp.compareTo(BigDecimal.ZERO) >= 0;
}

    <!-- Summary Cards -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-4">

        <!-- Card 1: 매수/매도 금액 -->
        @template._components.ui.card(body = @`
                <div class="text-xs text-base-content/50 mb-1">${periodStatusLabel}</div>
                <div class="flex items-baseline gap-1 mb-2">
                    <span class="text-xl font-bold text-error amount-value">&#8361; ${decimalFormat.format(safeBuy)}</span>
                    <span class="text-xs text-base-content/40">${buyLabel}</span>
                </div>
                <div class="flex gap-4 text-xs text-base-content/50">
                    <span>${sellLabel} <span class="font-medium text-info amount-value">&#8361; ${decimalFormat.format(safeSell)}</span></span>
                    <span class="ml-auto">${countMessage.apply(totalItems)}</span>
                </div>
        `)

        <!-- Card 2: 실현손익 -->
        @template._components.ui.card(body = @`
                <div class="text-xs text-base-content/50 mb-1">${realizedProfitLabel}</div>
                <div class="text-xl font-bold ${rpPositive ? "text-profit" : "text-loss"} amount-value">
                    ${rpPositive ? "+" : ""}&#8361; ${decimalFormat.format(safeRp)}
                </div>
                !{
                    long sellCount = allTradeList != null ? allTradeList.stream()
                        .filter(t -> t.type() == net.luversof.web.gate.stock.constant.TradeType.SELL)
                        .count() : 0L;
                }
                <div class="text-xs text-base-content/40 mt-1">${sellLabel} ${countMessage.apply(sellCount)}</div>
        `)

        <!-- Card 3: 거래 비용 -->
        @template._components.ui.card(body = @`
                <div class="text-xs text-base-content/50 mb-1">${tradingCostLabel}</div>
                <div class="text-xl font-bold amount-value">&#8361; ${decimalFormat.format(safeFee.add(safeTax))}</div>
                <div class="flex gap-4 text-xs text-base-content/50 mt-1">
                    <span>${feeLabel} <span class="font-medium text-base-content/70 amount-value">&#8361; ${decimalFormat.format(safeFee)}</span></span>
                    <span>${tradingTaxLabel} <span class="font-medium text-base-content/70 amount-value">&#8361; ${decimalFormat.format(safeTax)}</span></span>
                </div>
        `)

    </div>
