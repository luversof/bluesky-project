@import net.luversof.web.gate.stock.util.StockFormatUtil
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
	String tradeCountLabel = MessageUtil.getMessage("stock.activity.label.trade");
}

	<!-- Summary Cards -->
	<%-- 세 칸은 화면 폭이 아니라 자리 폭(@3xl = 48rem, 글꼴을 키우면 같이 커진다)으로 정한다(2026-09-30 실측: 1280px · 글꼴 200% 에서
     sm:grid-cols-3 가 세 칸으로 눌러 "1,815,470|,942" 처럼 금액이 갈렸다). 카드 금액은 15 글자까지 들어간다. --%>
	<div class="@container mb-4"><div class="grid grid-cols-1 @3xl:grid-cols-3 gap-3">

		<!-- Card 1: 매수/매도 금액 -->
		@template._components.ui.card(body = @`
				<div class="text-xs text-base-content/60 mb-1">${periodStatusLabel}</div>
				<div class="flex items-baseline gap-1 mb-2">
					<span class="text-xl font-bold amount-value">&#8361; ${decimalFormat.format(safeBuy)}</span>
					<span class="text-xs text-base-content/60">${buyLabel}</span>
				</div>
				<div class="flex gap-4 text-xs text-base-content/60">
					<span>${sellLabel} <span class="font-medium amount-value">&#8361; ${decimalFormat.format(safeSell)}</span></span>
					<%-- 라벨을 붙인다. 이 수는 매수+매도 전체 건수인데 같은 줄 왼쪽이 "매도" 라, 라벨이 없으면
						 매도 건수로 읽힌다 - 실측 2026-09-12(올해): 이 자리가 98건(매수 91 + 매도 7)인데
						 바로 옆 실현손익 카드는 "매도 7건" 이라, 한 화면이 매도를 98건과 7건 둘로 말했다.
						 활동 화면은 같은 수량에 이미 이 라벨을 쓴다. --%>
					<span class="ml-auto">${tradeCountLabel} ${countMessage.apply(totalItems)}</span>
				</div>
		`)

		<!-- Card 2: 실현손익 -->
		@template._components.ui.card(body = @`
				<%-- 이 카드의 값은 거래 기록에 저장된 실현손익의 합이고, 대시보드 누적치는 수수료·세금을
					 반영해 계산한 값이라 서로 다르다(실측 차 0.11%). 어느 쪽이 틀린 게 아니라
					 근거가 달라서이므로 무엇을 보고 있는지 알려준다. --%>
				<div class="text-xs text-base-content/60 mb-1 flex items-center gap-1">
					<span>${realizedProfitLabel}</span>
					<span class="tooltip tooltip-bottom" role="note" tabindex="0" aria-label="${MessageUtil.getMessage("stock.realized.tooltip.source")}" data-tip="${MessageUtil.getMessage("stock.realized.tooltip.source")}">
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="w-3.5 h-3.5 opacity-60">
							<path fill-rule="evenodd" d="M2.25 12c0-5.385 4.365-9.75 9.75-9.75s9.75 4.365 9.75 9.75-4.365 9.75-9.75 9.75S2.25 17.385 2.25 12zm8.706-1.442c1.146-.573 2.437.463 2.126 1.706l-.709 2.836.042-.02a.75.75 0 01.67 1.34l-.04.022c-1.147.573-2.438-.463-2.127-1.706l.71-2.836-.042.02a.75.75 0 11-.671-1.34l.041-.022zM12 9a.75.75 0 100-1.5.75.75 0 000 1.5z" clip-rule="evenodd" />
						</svg>
					</span>
				</div>
				<div class="text-xl font-bold ${rpPositive ? "text-profit" : "text-loss"} amount-value">
					${StockFormatUtil.displayWon(safeRp) > 0 ? "+" : ""}&#8361; ${decimalFormat.format(safeRp)}
				</div>
				!{
					long sellCount = allTradeList != null ? allTradeList.stream()
						.filter(t -> t.type() == net.luversof.web.gate.stock.constant.TradeType.SELL)
						.count() : 0L;
				}
				<div class="text-xs text-base-content/60 mt-1">${sellLabel} ${countMessage.apply(sellCount)}</div>
		`)

		<!-- Card 3: 거래 비용 -->
		@template._components.ui.card(body = @`
				<div class="text-xs text-base-content/60 mb-1">${tradingCostLabel}</div>
				<div class="text-xl font-bold amount-value">&#8361; ${decimalFormat.format(safeFee.add(safeTax))}</div>
				<div class="flex gap-4 text-xs text-base-content/60 mt-1">
					<span>${feeLabel} <span class="font-medium text-base-content/70 amount-value">&#8361; ${decimalFormat.format(safeFee)}</span></span>
					<span>${tradingTaxLabel} <span class="font-medium text-base-content/70 amount-value">&#8361; ${decimalFormat.format(safeTax)}</span></span>
				</div>
		`)

	</div></div>
