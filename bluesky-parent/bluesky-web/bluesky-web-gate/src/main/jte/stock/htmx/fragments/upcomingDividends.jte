@import java.math.BigDecimal
@import java.util.List
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse

@param boolean hasSnapshots = false
@param BigDecimal totalAverageMonthlyDividend = java.math.BigDecimal.ZERO
@param BigDecimal totalLatestMonthlyDividend = java.math.BigDecimal.ZERO
@param List<MonthlyDividendSnapshotResponse> topDividendSnapshots = java.util.List.of()

!{
    String titleLabel = MessageUtil.getMessage("stock.dashboard.upcoming.dividend");
    String avgLabel = MessageUtil.getMessage("stock.dividend.calendar.basis.average");
    String latestLabel = MessageUtil.getMessage("stock.dividend.calendar.basis.latest");
    String perMonthLabel = MessageUtil.getMessage("stock.dividend.calendar.per.month");
    String dividendMenuLabel = MessageUtil.getMessage("layout.menu.stock.dividend");
}
<%-- outerHTML 스왑 대상이므로 데이터 유무와 무관하게 항상 최상위 요소(카드)를 출력한다 --%>
@template._components.ui.card(cssClass = "h-full", body = @`
    <div class="flex justify-between items-center mb-2">
        <h2 class="card-title text-base m-0">${titleLabel}</h2>
        <a href="/stock/dividend?tab=calendar" class="link link-hover text-xs">${dividendMenuLabel} →</a>
    </div>
    @if(hasSnapshots)
    <div class="text-right leading-tight tabular-nums mb-2">
        <div class="font-mono font-bold text-dividend amount-value" title="${String.format("%,d원", totalAverageMonthlyDividend != null ? totalAverageMonthlyDividend.longValue() : 0)}"><span class="mr-1 text-[11px] font-normal text-base-content/45">${avgLabel}</span>${String.format("%,d", totalAverageMonthlyDividend != null ? totalAverageMonthlyDividend.longValue() : 0)}<span class="text-xs font-normal text-base-content/40 ml-0.5">${perMonthLabel}</span></div>
        <div class="font-mono font-bold text-dividend amount-value" title="${String.format("%,d원", totalLatestMonthlyDividend != null ? totalLatestMonthlyDividend.longValue() : 0)}"><span class="mr-1 text-[11px] font-normal text-base-content/45">${latestLabel}</span>${String.format("%,d", totalLatestMonthlyDividend != null ? totalLatestMonthlyDividend.longValue() : 0)}<span class="text-xs font-normal text-base-content/40 ml-0.5">${perMonthLabel}</span></div>
    </div>
    <div class="space-y-1">
        !{int upcomingRank = 1;}
        @for(MonthlyDividendSnapshotResponse row : topDividendSnapshots)
            <div class="flex items-center justify-between text-sm py-1.5 border-b border-base-200 last:border-0">
                <span class="flex items-center gap-2 min-w-0">
                    <span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center shrink-0">${upcomingRank++}</span>
                    <span class="truncate">${row.stockItemName() != null ? row.stockItemName() : row.stockItemSymbol()}</span>
                </span>
                <span class="font-mono font-bold text-dividend amount-value shrink-0" title="${String.format("%,d원", row.expectedMonthlyDividend() != null ? row.expectedMonthlyDividend().longValue() : 0)}">${String.format("%,d", row.expectedMonthlyDividend() != null ? row.expectedMonthlyDividend().longValue() : 0)}</span>
            </div>
        @endfor
    </div>
    @else
    @template._components.ui.emptyState(padding = "py-6")
    @endif
`)
