@import net.luversof.web.gate.stock.util.StockFormatUtil
@import java.math.BigDecimal

<%-- 두 줄짜리 합계 카드. 줄 이름은 쓰는 쪽이 정한다 - 배당은 평균/최근, 과세표준은 월/연이다. --%>
<%-- 두 줄짜리 합계 카드. 줄 이름은 쓰는 쪽이 정한다 - 배당은 평균/최근, 과세표준은 월/연이다. --%>
@param String label
@param String topLabel
@param String bottomLabel
@param BigDecimal topValue = java.math.BigDecimal.ZERO
@param BigDecimal bottomValue = java.math.BigDecimal.ZERO

<div class="bg-base-200 rounded-box p-3">
	<div class="text-xs text-base-content/60">${label}</div>
	<div class="mt-1 flex items-baseline gap-2 tabular-nums">
		<span class="w-7 shrink-0 text-[11px] text-base-content/60">${topLabel}</span>
		<span class="text-lg font-bold font-mono amount-value text-dividend" title="${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(topValue))}">${String.format("%,d", StockFormatUtil.displayWon(topValue))}@template._components.ui.srExact(text = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(topValue)), visible = String.format("%,d", StockFormatUtil.displayWon(topValue)))</span>
	</div>
	<div class="flex items-baseline gap-2 tabular-nums">
		<span class="w-7 shrink-0 text-[11px] text-base-content/60">${bottomLabel}</span>
		<span class="text-lg font-bold font-mono amount-value text-dividend" title="${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(bottomValue))}">${String.format("%,d", StockFormatUtil.displayWon(bottomValue))}@template._components.ui.srExact(text = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(bottomValue)), visible = String.format("%,d", StockFormatUtil.displayWon(bottomValue)))</span>
	</div>
</div>
