@import net.luversof.web.gate.stock.util.StockFormatUtil
@import java.math.BigDecimal
@import java.util.List
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse

@param boolean hasSnapshots = false
@param String nextWindowLabel = ""
@param boolean nextWindowIsMid = false
@param String nextPayDateLabel = ""
@param BigDecimal windowTotal = java.math.BigDecimal.ZERO
@param List<MonthlyDividendSnapshotResponse> topDividendSnapshots = java.util.List.of()
@param java.time.LocalDate dividendAsOfOldest = null
@param java.time.LocalDate dividendAsOfNewest = null
<%-- 스냅샷의 보유 수량이 원장의 현재 수량과 어긋난 종목 수와, 현재 수량으로 다시 낸 합계. --%>
@param long staleQuantityCount = 0L
@param BigDecimal currentQuantityTotal = java.math.BigDecimal.ZERO

!{
    String titleLabel = MessageUtil.getMessage("stock.dashboard.upcoming.dividend");
    String avgLabel = MessageUtil.getMessage("stock.dividend.calendar.basis.average");
    String perMonthLabel = MessageUtil.getMessage("stock.dividend.calendar.per.month");
    String dividendMenuLabel = MessageUtil.getMessage("layout.menu.stock.dividend");
    boolean hasWindow = nextWindowLabel != null && !nextWindowLabel.isEmpty();
}
<%-- outerHTML 스왑 대상이므로 데이터 유무와 무관하게 항상 최상위 요소(카드)를 출력한다 --%>
@template._components.ui.card(cssClass = "h-full", body = @`
    <div class="flex justify-between items-center gap-2 mb-2">
        <div class="flex items-center gap-2 min-w-0">
            <h2 class="card-title text-base m-0 shrink-0">${titleLabel}</h2>
            @if(hasWindow)
            <%-- 아주 좁은 화면에서는 줄바꿈을 허용한다. nowrap + shrink-0 이면 이 배지가 카드의 최소 폭을
                 라벨 길이만큼 밀어 화면 밖으로 나간다(실측: 영어 320px 에서 29px). --%>
            <span class="badge badge-sm ${nextWindowIsMid ? "badge-info" : "badge-secondary"} badge-outline shrink-0 whitespace-nowrap max-sm:shrink max-sm:whitespace-normal max-sm:h-auto">${nextWindowLabel}@if(nextPayDateLabel != null && !nextPayDateLabel.isEmpty()) · ${nextPayDateLabel}@endif</span>
            @endif
        </div>
        <a href="/stock/dividend?tab=calendar" class="link link-hover text-xs shrink-0">${dividendMenuLabel} →</a>
    </div>
<%-- 예상 배당은 저장된 기준 데이터(스냅샷)로 계산된다. 스냅샷은 사람이 갱신해야 하고 종목마다 시점이 달라
     실제로 뒤처진다(실측 2026-08-22: 2026-07-20 ~ 2026-08-04, 최대 33 일 - 그만큼 보유 수량이 옛 값이라
     예상 월배당이 1.66% 낮게 잡혔다). 어느 시점 데이터인지 밝히지 않으면 사용자는 현재값으로 읽는다.
     문구는 월배당 시뮬레이터와 같은 키를 쓴다. --%>
    @if(dividendAsOfOldest != null)
    <div class="text-[11px] text-base-content/60 mb-2">
        ${MessageUtil.getMessage("stock.simulator.monthly.summary.as.of.date")}
        <span class="font-mono">${dividendAsOfOldest.toString()}${dividendAsOfNewest != null && !dividendAsOfNewest.equals(dividendAsOfOldest) ? " ~ " + dividendAsOfNewest.toString() : ""}</span>
    </div>
    @endif
    @if(staleQuantityCount > 0)
    <div data-stale-quantity="${staleQuantityCount}" class="text-[11px] text-warning mb-2">
        ${java.text.MessageFormat.format(
            MessageUtil.getMessage("stock.summary.upcoming.dividend.stale.quantity"),
            String.valueOf(staleQuantityCount),
            String.format("%,d", StockFormatUtil.displayWon(currentQuantityTotal)))}
    </div>
    @endif
    @if(hasSnapshots)
    <div class="text-right leading-tight tabular-nums mb-2">
        <div class="font-mono font-bold text-dividend amount-value" title="${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(windowTotal))}"><span class="mr-1 text-[11px] font-normal text-base-content/60">${avgLabel}</span>${String.format("%,d", StockFormatUtil.displayWon(windowTotal))}<span class="text-xs font-normal text-base-content/60 ml-0.5">${perMonthLabel}</span></div>
    </div>
    <div class="space-y-1">
        !{int upcomingRank = 1;}
        @for(MonthlyDividendSnapshotResponse row : topDividendSnapshots)
            <div class="flex items-center justify-between text-sm py-1.5 border-b border-base-200 last:border-0">
                <span class="flex items-center gap-2 min-w-0">
                    <span class="badge badge-sm badge-ghost w-5 h-5 p-0 flex justify-center shrink-0">${upcomingRank++}</span>
                    <span class="truncate">${row.stockItemName() != null ? row.stockItemName() : row.stockItemSymbol()}</span>
                </span>
                <span class="font-mono font-bold text-dividend amount-value shrink-0" title="${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(row.expectedMonthlyDividend()))}">${String.format("%,d", StockFormatUtil.displayWon(row.expectedMonthlyDividend()))}</span>
            </div>
        @endfor
    </div>
    @else
    @template._components.ui.emptyState(padding = "py-6")
    @endif
`)
