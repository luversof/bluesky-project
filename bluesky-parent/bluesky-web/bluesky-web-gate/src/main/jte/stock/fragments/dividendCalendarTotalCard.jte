@import net.luversof.web.gate.stock.util.StockFormatUtil
@import java.math.BigDecimal

@param String label
@param String avgLabel
@param String latestLabel
@param BigDecimal avgValue = java.math.BigDecimal.ZERO
@param BigDecimal latestValue = java.math.BigDecimal.ZERO

<div class="bg-base-200 rounded-box p-3">
	<div class="text-xs text-base-content/60">${label}</div>
	<div class="mt-1 flex items-baseline gap-2 tabular-nums">
		<span class="w-7 shrink-0 text-[11px] text-base-content/60">${avgLabel}</span>
		<span class="text-lg font-bold font-mono amount-value text-dividend" title="${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(avgValue))}">${String.format("%,d", StockFormatUtil.displayWon(avgValue))}</span>
	</div>
	<div class="flex items-baseline gap-2 tabular-nums">
		<span class="w-7 shrink-0 text-[11px] text-base-content/60">${latestLabel}</span>
		<span class="text-lg font-bold font-mono amount-value text-dividend" title="${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(latestValue))}">${String.format("%,d", StockFormatUtil.displayWon(latestValue))}</span>
	</div>
</div>
