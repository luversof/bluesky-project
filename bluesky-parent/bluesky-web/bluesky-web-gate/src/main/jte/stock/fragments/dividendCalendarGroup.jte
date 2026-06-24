@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.util.List
@import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse

@param String title
@param List<MonthlyDividendSnapshotResponse> rows = java.util.List.of()
@param BigDecimal subtotal = java.math.BigDecimal.ZERO

!{
    String perMonthLabel = MessageUtil.getMessage("stock.dividend.calendar.per.month");
    int count = rows != null ? rows.size() : 0;
}

@template._components.ui.card(body = @`
    <div class="flex items-center justify-between mb-2">
        <h2 class="card-title text-base m-0">${title} <span class="text-xs font-normal text-base-content/50">(${count})</span></h2>
        <span class="font-mono font-bold text-dividend amount-value" title="${String.format("%,d원", subtotal != null ? subtotal.longValue() : 0)}">${String.format("%,d", subtotal != null ? subtotal.longValue() : 0)}<span class="text-xs font-normal text-base-content/40 ml-0.5">${perMonthLabel}</span></span>
    </div>
    <div class="space-y-1">
        @if(rows != null)
            !{int rank = 1;}
            @for(MonthlyDividendSnapshotResponse row : rows)
                <div class="flex items-center justify-between text-sm py-1.5 border-b border-base-200 last:border-0">
                    <span class="flex items-center gap-2 min-w-0">
                        <span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center shrink-0">${rank++}</span>
                        <span class="truncate">${row.stockItemName() != null ? row.stockItemName() : row.stockItemSymbol()}</span>
                    </span>
                    <span class="font-mono font-bold text-dividend amount-value shrink-0" title="${String.format("%,d원", row.expectedMonthlyDividend() != null ? row.expectedMonthlyDividend().longValue() : 0)}">${String.format("%,d", row.expectedMonthlyDividend() != null ? row.expectedMonthlyDividend().longValue() : 0)}</span>
                </div>
            @endfor
        @endif
    </div>
`)
