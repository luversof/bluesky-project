@import io.github.luversof.boot.context.support.MessageUtil

@param String fromDate = ""
@param String toDate = ""
@param Double periodReturnRatePct = null
@param boolean returnCalculable = false

!{
    String title = MessageUtil.getMessage("stock.asset.growth.summary.period.return");
    String description = MessageUtil.getMessage("stock.asset.growth.summary.period.return.desc");
    String unavailableLabel = MessageUtil.getMessage("stock.asset.growth.summary.period.return.unavailable");
    boolean positive = periodReturnRatePct != null && periodReturnRatePct >= 0.0d;
}

<div id="assetGrowthPeriodReturnSummary" class="rounded-2xl border border-base-300/60 bg-base-100/80 px-4 py-3 mb-3">
    <div class="flex flex-wrap items-center gap-x-2 gap-y-1">
        <span class="text-sm font-semibold text-base-content/70">${title}</span>
        @if(!fromDate.isBlank() && !toDate.isBlank())
            <span class="inline-flex items-center rounded-full bg-base-200/80 px-2 py-0.5 text-[11px] font-medium text-base-content/55">
                ${fromDate} ~ ${toDate}
            </span>
        @endif
    </div>
    <div class="mt-1">
        @if(returnCalculable && periodReturnRatePct != null)
            <span class="text-3xl font-bold leading-none ${positive ? "text-profit" : "text-loss"}">
                ${String.format("%+.2f%%", periodReturnRatePct)}
            </span>
        @else
            <span class="text-lg font-semibold text-base-content/40">${unavailableLabel}</span>
        @endif
    </div>
    <p class="text-xs text-base-content/45 mt-1">${description}</p>
</div>