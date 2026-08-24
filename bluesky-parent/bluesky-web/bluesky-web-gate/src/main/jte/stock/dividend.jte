@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.util.List
@import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse
@import net.luversof.web.gate.stock.util.StockFormatUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String dividendTab = "history"
@param List<MonthlyDividendSnapshotResponse> midRows = java.util.List.of()
@param List<MonthlyDividendSnapshotResponse> endRows = java.util.List.of()
@param List<MonthlyDividendSnapshotResponse> otherRows = java.util.List.of()
@param BigDecimal midTotal = java.math.BigDecimal.ZERO
@param BigDecimal endTotal = java.math.BigDecimal.ZERO
@param BigDecimal otherTotal = java.math.BigDecimal.ZERO
@param BigDecimal monthlyTotal = java.math.BigDecimal.ZERO
@param BigDecimal annualTotal = java.math.BigDecimal.ZERO
@param BigDecimal midTotalLatest = java.math.BigDecimal.ZERO
@param BigDecimal endTotalLatest = java.math.BigDecimal.ZERO
@param BigDecimal otherTotalLatest = java.math.BigDecimal.ZERO
@param BigDecimal monthlyTotalLatest = java.math.BigDecimal.ZERO
@param BigDecimal annualTotalLatest = java.math.BigDecimal.ZERO
@param java.util.List<String> shortHistorySymbols = java.util.List.of()
@param long calendarStaleQuantityCount = 0L
@param BigDecimal calendarCurrentQuantityTotal = java.math.BigDecimal.ZERO

!{
    boolean isCalendar = "calendar".equals(dividendTab);
    String activeTabClass = "tab tab-active flex-1 font-bold !bg-primary !text-primary-content shadow-sm";
    String inactiveTabClass = "tab flex-1 text-base-content/60 hover:text-base-content";

    String midLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.mid.month");
    String endLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.month.end");
    String otherLabel = MessageUtil.getMessage("stock.dividend.calendar.window.other");
    String monthlyTotalLabel = MessageUtil.getMessage("stock.dividend.calendar.monthly.total");
    String annualTotalLabel = MessageUtil.getMessage("stock.dividend.calendar.annual.total");
    String avgBasisLabel = MessageUtil.getMessage("stock.dividend.calendar.basis.average");
    String latestBasisLabel = MessageUtil.getMessage("stock.dividend.calendar.basis.latest");
    String calendarDesc = MessageUtil.getMessage("stock.dividend.calendar.desc");
    String emptyLabel = MessageUtil.getMessage("stock.dividend.calendar.empty");
    boolean calendarEmpty = (midRows == null || midRows.isEmpty())
        && (endRows == null || endRows.isEmpty())
        && (otherRows == null || otherRows.isEmpty());
}

@template._layout.stockLayout(
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentStock = @`
		<div class="space-y-6" data-sync-url="/stock/htmx/dividend/list">
			@template._components.ui.pageHeader(title = MessageUtil.getMessage("stock.page.dividend.title"))

			<!-- 같은 페이지 탭: 클릭 시 이 페이지에서 내용만 교체된다 -->
			<div role="tablist" class="tabs tabs-box w-full bg-base-200/70 p-1">
				<a role="tab" href="/stock/dividend?tab=history" class="${!isCalendar ? activeTabClass : inactiveTabClass}">${MessageUtil.getMessage("stock.page.dividend.tab.history")}</a>
				<a role="tab" href="/stock/dividend?tab=calendar" class="${isCalendar ? activeTabClass : inactiveTabClass}">${MessageUtil.getMessage("stock.page.dividend.tab.calendar")}</a>
			</div>

			<!-- 월배당 도구: 별도 페이지로 이동(링크). 탭과 구분되도록 버튼 형태로 표기 -->
			<div class="flex flex-wrap items-center gap-2 text-sm">
				<span class="text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.tools")}</span>
				<a href="/stock/admin?tab=monthly-reference" class="btn btn-sm btn-outline gap-1">
					${MessageUtil.getMessage("stock.page.dividend.tab.monthly.reference")}
					<svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M14 5l7 7m0 0l-7 7m7-7H3" /></svg>
				</a>
				<a href="/stock/simulator?tab=monthly-dividend" class="btn btn-sm btn-outline gap-1">
					${MessageUtil.getMessage("stock.page.dividend.tab.simulator")}
					<svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M14 5l7 7m0 0l-7 7m7-7H3" /></svg>
				</a>
			</div>

			@if(isCalendar)
				<p class="text-sm text-base-content/60">${calendarDesc}</p>
				@if(calendarEmpty)
					<div class="bg-base-200 rounded-box p-10 text-center text-base-content/60">${emptyLabel}</div>
				@else
					<div class="grid grid-cols-2 gap-3">
						@template.stock.fragments.dividendCalendarTotalCard(
							label = monthlyTotalLabel,
							avgLabel = avgBasisLabel,
							latestLabel = latestBasisLabel,
							avgValue = monthlyTotal,
							latestValue = monthlyTotalLatest)
						@template.stock.fragments.dividendCalendarTotalCard(
							label = annualTotalLabel,
							avgLabel = avgBasisLabel,
							latestLabel = latestBasisLabel,
							avgValue = annualTotal,
							latestValue = annualTotalLatest)
					</div>
					<%-- 이 달력의 예상 배당은 저장된 기준 데이터(스냅샷)로 계산된다. 스냅샷은 사람이 갱신해야 하고
					     종목마다 시점이 달라 실제로 뒤처진다(실측 2026-08-22: 2026-07-20 ~ 2026-08-04, 최대 33 일 -
					     그만큼 보유 수량이 옛 값이라 예상 월배당이 1.66% 낮게 잡혔다). 월배당 시뮬레이터에는 이미
					     같은 표기가 있는데 이 화면과 대시보드 카드에만 없었다. --%>
					!{
						java.time.LocalDate calendarAsOfOldest = java.util.stream.Stream.of(midRows, endRows, otherRows)
							.filter(java.util.Objects::nonNull).flatMap(java.util.List::stream)
							.map(net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse::asOfDate)
							.filter(java.util.Objects::nonNull).min(java.time.LocalDate::compareTo).orElse(null);
						java.time.LocalDate calendarAsOfNewest = java.util.stream.Stream.of(midRows, endRows, otherRows)
							.filter(java.util.Objects::nonNull).flatMap(java.util.List::stream)
							.map(net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse::asOfDate)
							.filter(java.util.Objects::nonNull).max(java.time.LocalDate::compareTo).orElse(null);
					}
					@if(calendarAsOfOldest != null)
						<div class="text-xs text-base-content/60 mb-2">
							${MessageUtil.getMessage("stock.simulator.monthly.summary.as.of.date")}
							<span class="font-mono">${calendarAsOfOldest.toString()}${calendarAsOfNewest != null && !calendarAsOfNewest.equals(calendarAsOfOldest) ? " ~ " + calendarAsOfNewest.toString() : ""}</span>
						</div>
					@endif
					<%-- 스냅샷 수량은 사람이 갱신한 시점의 값이라 원장과 어긋난다. 같은 안내가 요약 카드와
					     월배당 시뮬레이터에는 있는데 이 달력에만 없어서, 같은 숫자가 화면에 따라 밝혀지기도
					     하고 아무 말 없이 나가기도 했다(실측 2026-08-23: 8종목 중 7종목이 어긋나 46,124원 /
					     1.66% 낮다). 문구·계산은 다른 두 화면과 같은 것을 쓴다. --%>
					@if(calendarStaleQuantityCount > 0)
						<div class="text-[11px] text-warning mb-2" data-calendar-stale-quantity="${calendarStaleQuantityCount}">
							${java.text.MessageFormat.format(
								MessageUtil.getMessage("stock.summary.upcoming.dividend.stale.quantity"),
								String.valueOf(calendarStaleQuantityCount),
								StockFormatUtil.fullKrw(StockFormatUtil.displayWon(calendarCurrentQuantityTotal)))}
						</div>
					@endif
					<%-- "평균" 기준은 최근 12건의 주당 배당을 평균한다(건수 기준이지 기간 기준이 아니다).
					     상장이 얼마 안 된 종목은 이력이 12건에 못 미쳐 그만큼 짧은 평균이 되므로 그 사실을 밝힌다.
					     실측 2026-08-23: 8종목 중 2종목이 10건(각 9개월 구간). --%>
					@if(shortHistorySymbols != null && !shortHistorySymbols.isEmpty())
						<div class="text-xs text-base-content/60 mb-2" data-short-history-count="${shortHistorySymbols.size()}">
							${java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.calendar.short.history"), String.join(", ", shortHistorySymbols))}
						</div>
					@endif
					<div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
						@template.stock.fragments.dividendCalendarGroup(title = midLabel, rows = midRows, subtotal = midTotal, subtotalLatest = midTotalLatest)
						@template.stock.fragments.dividendCalendarGroup(title = endLabel, rows = endRows, subtotal = endTotal, subtotalLatest = endTotalLatest)
					</div>
					@if(otherRows != null && !otherRows.isEmpty())
						<div class="grid grid-cols-1">
							@template.stock.fragments.dividendCalendarGroup(title = otherLabel, rows = otherRows, subtotal = otherTotal, subtotalLatest = otherTotalLatest)
						</div>
					@endif
				@endif
			@else
				<div class="min-h-[200px]" data-params-from-query="true" hx-get="/stock/htmx/dividend/list" hx-trigger="load" hx-target="this" hx-swap="outerHTML" hx-include="#globalDateRangeInputs input">
					<div class="flex items-center gap-2 text-base-content/60">
						<span class="loading loading-spinner loading-sm text-primary"></span>
						<span>${MessageUtil.getMessage("common.message.loading")}</span>
					</div>
				</div>
			@endif
		</div>
	`
)
