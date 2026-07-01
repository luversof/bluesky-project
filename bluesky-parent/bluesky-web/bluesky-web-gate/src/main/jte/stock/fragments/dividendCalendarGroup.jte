@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.util.List
@import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse

@param String title
@param List<MonthlyDividendSnapshotResponse> rows = java.util.List.of()
@param BigDecimal subtotal = java.math.BigDecimal.ZERO
@param BigDecimal subtotalLatest = java.math.BigDecimal.ZERO

!{
    String perMonthLabel = MessageUtil.getMessage("stock.dividend.calendar.per.month");
    String avgLabel = MessageUtil.getMessage("stock.dividend.calendar.basis.average");
    String latestLabel = MessageUtil.getMessage("stock.dividend.calendar.basis.latest");
    int count = rows != null ? rows.size() : 0;
}

@template._components.ui.card(body = @`
    <div class="flex items-start justify-between mb-2">
        <h2 class="card-title text-base m-0">${title} <span class="text-xs font-normal text-base-content/50">(${count})</span></h2>
        <div class="text-right leading-tight tabular-nums">
            <div class="font-mono font-bold text-dividend amount-value" title="${String.format("%,d원", subtotal != null ? subtotal.longValue() : 0)}"><span class="mr-1 text-[11px] font-normal text-base-content/45">${avgLabel}</span>${String.format("%,d", subtotal != null ? subtotal.longValue() : 0)}<span class="text-xs font-normal text-base-content/40 ml-0.5">${perMonthLabel}</span></div>
            <div class="font-mono font-bold text-dividend amount-value" title="${String.format("%,d원", subtotalLatest != null ? subtotalLatest.longValue() : 0)}"><span class="mr-1 text-[11px] font-normal text-base-content/45">${latestLabel}</span>${String.format("%,d", subtotalLatest != null ? subtotalLatest.longValue() : 0)}<span class="text-xs font-normal text-base-content/40 ml-0.5">${perMonthLabel}</span></div>
        </div>
    </div>
    <div class="space-y-1">
        @if(rows != null)
            !{int rank = 1;}
            @for(MonthlyDividendSnapshotResponse row : rows)
                !{long latestAmount = (row.latestMonthlyDividendPerShare() != null ? row.latestMonthlyDividendPerShare() : BigDecimal.ZERO).multiply(BigDecimal.valueOf(row.heldQuantity() != null ? row.heldQuantity().longValue() : 0L)).longValue();}
                <div class="flex items-center justify-between text-sm py-1.5 border-b border-base-200 last:border-0">
                    <span class="flex items-center gap-2 min-w-0">
                        <span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center shrink-0">${rank++}</span>
                        <span class="truncate">${row.stockItemName() != null ? row.stockItemName() : row.stockItemSymbol()}</span>
                    </span>
                    <span class="text-right leading-tight tabular-nums shrink-0">
                        <span class="block font-mono font-bold text-dividend amount-value" title="${String.format("%,d원", row.expectedMonthlyDividend() != null ? row.expectedMonthlyDividend().longValue() : 0)}"><span class="mr-1 text-[11px] font-normal text-base-content/45">${avgLabel}</span>${String.format("%,d", row.expectedMonthlyDividend() != null ? row.expectedMonthlyDividend().longValue() : 0)}</span>
                        <span class="block font-mono font-bold text-dividend amount-value" title="${String.format("%,d원", latestAmount)}"><span class="mr-1 text-[11px] font-normal text-base-content/45">${latestLabel}</span>${String.format("%,d", latestAmount)}</span>
                    </span>
                </div>
            @endfor
        @endif
    </div>
`)
