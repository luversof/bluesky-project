@import java.math.BigDecimal

@param String label
@param String avgLabel
@param String latestLabel
@param BigDecimal avgValue = java.math.BigDecimal.ZERO
@param BigDecimal latestValue = java.math.BigDecimal.ZERO

<div class="bg-base-200 rounded-box p-3">
	<div class="text-xs text-base-content/60">${label}</div>
	<div class="mt-1 flex items-baseline gap-2 tabular-nums">
		<span class="w-7 shrink-0 text-[11px] text-base-content/45">${avgLabel}</span>
		<span class="text-lg font-bold font-mono amount-value text-dividend" title="${String.format("%,d원", avgValue != null ? avgValue.longValue() : 0)}">${String.format("%,d", avgValue != null ? avgValue.longValue() : 0)}</span>
	</div>
	<div class="flex items-baseline gap-2 tabular-nums">
		<span class="w-7 shrink-0 text-[11px] text-base-content/45">${latestLabel}</span>
		<span class="text-lg font-bold font-mono amount-value text-dividend" title="${String.format("%,d원", latestValue != null ? latestValue.longValue() : 0)}">${String.format("%,d", latestValue != null ? latestValue.longValue() : 0)}</span>
	</div>
</div>
