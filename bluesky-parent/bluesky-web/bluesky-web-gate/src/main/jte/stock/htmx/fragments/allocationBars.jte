@import java.util.List
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.stock.controller.StockSummaryHtmxController.AllocationBarRow

@param List<AllocationBarRow> allocationRows = java.util.List.of()
@param AllocationBarRow othersRow = null

!{
    String titleLabel = MessageUtil.getMessage("stock.dashboard.allocation");
    String assetStatusLabel = MessageUtil.getMessage("layout.menu.stock.analytics");
}
<%-- outerHTML 스왑 대상이므로 데이터 유무와 무관하게 항상 최상위 요소(카드)를 출력한다 --%>
@template._components.ui.card(cssClass = "h-full", body = @`
    <div class="flex justify-between items-center mb-2">
        <h2 class="card-title text-base m-0">${titleLabel}</h2>
        <a href="/stock/analytics" class="link link-hover text-xs">${assetStatusLabel} →</a>
    </div>
    @if(!allocationRows.isEmpty())
    !{String[] allocationBarColors = {"rgba(99,102,241,0.8)", "rgba(34,197,94,0.8)", "rgba(251,191,36,0.8)", "rgba(239,68,68,0.8)", "rgba(59,130,246,0.8)"};}
    <div class="space-y-1">
        @for(int allocationRowIndex = 0; allocationRowIndex < allocationRows.size(); allocationRowIndex++)
            !{AllocationBarRow row = allocationRows.get(allocationRowIndex);}
            !{String barColor = allocationBarColors[allocationRowIndex % allocationBarColors.length];}
            <div class="flex items-center gap-2 text-sm py-1.5 border-b border-base-200 last:border-0">
                @if(row.stockItemId() != null)
                <a href="/stock/item?stockItemId=${row.stockItemId().toString()}" class="link link-hover truncate w-24 sm:w-28 shrink-0" title="${row.stockItemName()}">${row.stockItemName()}</a>
                @else
                <span class="truncate w-24 sm:w-28 shrink-0" title="${row.stockItemName()}">${row.stockItemName()}</span>
                @endif
                <div class="flex-1 min-w-0 h-2 rounded-full bg-base-200">
                    <div class="h-2 rounded-full" style="width: ${String.format("%.1f", row.weightPercent())}%; background: ${barColor}"></div>
                </div>
                <span class="text-xs text-base-content/60 tabular-nums w-11 text-right shrink-0">${String.format("%.1f%%", row.weightPercent())}</span>
                <span class="font-mono font-bold text-xs amount-value whitespace-nowrap text-right shrink-0" title="${String.format("%,d원", row.amount())}">${String.format("%,d", row.amount())}</span>
            </div>
        @endfor
        @if(othersRow != null)
            <div class="flex items-center gap-2 text-sm py-1.5 border-b border-base-200 last:border-0">
                <span class="truncate w-24 sm:w-28 shrink-0 text-base-content/60" title="${othersRow.stockItemName()}">${othersRow.stockItemName()}</span>
                <div class="flex-1 min-w-0 h-2 rounded-full bg-base-200">
                    <div class="h-2 rounded-full" style="width: ${String.format("%.1f", othersRow.weightPercent())}%; background: rgba(156,163,175,0.8)"></div>
                </div>
                <span class="text-xs text-base-content/60 tabular-nums w-11 text-right shrink-0">${String.format("%.1f%%", othersRow.weightPercent())}</span>
                <span class="font-mono font-bold text-xs amount-value whitespace-nowrap text-right shrink-0" title="${String.format("%,d원", othersRow.amount())}">${String.format("%,d", othersRow.amount())}</span>
            </div>
        @endif
    </div>
    @else
    @template._components.ui.emptyState(padding = "py-6")
    @endif
`)
