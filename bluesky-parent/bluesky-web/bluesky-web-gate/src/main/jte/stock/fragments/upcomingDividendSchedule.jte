@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import net.luversof.web.gate.stock.dto.view.DividendCalendarView
@import net.luversof.web.gate.stock.dto.view.UpcomingDividendScheduleView
@import net.luversof.web.gate.stock.util.StockFormatUtil

<%-- 다가올 배당 - 이번 달 남은 날과 다음 달에 받을 것으로 예상되는 월배당(사용자 결정 2026-09-17: 캘린더 탭을 실수령 배당에 통합하고
     '앞으로' 는 화면 위 이 구역으로, 범위는 이번 달 남은 날 + 다음 달).
     금액은 월배당 기준 데이터(스냅샷) x 지금 스냅샷 수량이라, 옛 캘린더 탭 위에 있던 예상치 안내(기준일 · 수량 어긋남 · 원장 조회 실패 ·
     짧은 이력)가 이 구역으로 따라왔다 - 같은 숫자가 화면에 따라 밝혀지기도 하고 조용히 나가기도 하면 안 된다(실측 2026-08-23). --%>
@param boolean hasRows = false
@param UpcomingDividendScheduleView schedule = null
@param java.time.LocalDate asOfOldest = null
@param java.time.LocalDate asOfNewest = null
@param long staleQuantityCount = 0L
@param boolean currentQuantityUnavailable = false
@param BigDecimal currentQuantityTotal = java.math.BigDecimal.ZERO
@param java.util.List<String> shortHistorySymbols = java.util.List.of()

!{
	String avgLabel = MessageUtil.getMessage("stock.dividend.calendar.basis.average");
	String latestLabel = MessageUtil.getMessage("stock.dividend.calendar.basis.latest");
}
<section class="rounded-2xl border border-base-300 bg-base-100 p-4 shadow-sm" data-upcoming-dividends>
	<h2 class="text-base font-bold" data-page-section>${MessageUtil.getMessage("stock.dashboard.upcoming.dividend")}</h2>
	@if(!hasRows)
		<%-- 빈 상태엔 다음 행동을 준다(공용 emptyState 의 규칙) - 문구가 '월배당 기준 데이터에서 등록하라' 고 말하므로 그리로 가는 링크를 그 자리에 둔다. --%>
		<div class="mt-2">
			@template._components.ui.emptyState(
				message = MessageUtil.getMessage("stock.dividend.calendar.empty"),
				boxed = true,
				actionLabel = MessageUtil.getMessage("stock.page.dividend.tab.monthly.reference"),
				actionHref = "/stock/admin?tab=monthly-reference")
		</div>
	@else
		<p class="mt-1 text-xs text-base-content/60" data-upcoming-desc>${MessageUtil.getMessage("stock.dividend.upcoming.desc")}</p>
		<%-- 스냅샷은 사람이 갱신해야 하고 종목마다 시점이 달라 실제로 뒤처진다(실측 2026-08-22: 최대 33 일). --%>
		@if(asOfOldest != null)
			<div class="mt-1 text-xs text-base-content/60">
				${MessageUtil.getMessage("stock.simulator.monthly.summary.as.of.date")}
				<span class="font-mono">${asOfOldest.toString()}${asOfNewest != null && !asOfNewest.equals(asOfOldest) ? " ~ " + asOfNewest.toString() : ""}</span>
			</div>
		@endif
		<%-- 조회가 실패하면 어긋난 줄이 0 이라 아래 안내가 사라진다 - 안내 없는 화면은 "원장과 같다" 로 읽힌다. --%>
		@if(currentQuantityUnavailable)
			<div class="mt-1 text-[11px] text-warning" data-ledger-unavailable="true">${MessageUtil.getMessage("stock.holdings.ledger.unavailable")}</div>
		@endif
		@if(staleQuantityCount > 0)
			<div class="mt-1 text-[11px] text-warning" data-upcoming-stale-quantity="${staleQuantityCount}">
				${java.text.MessageFormat.format(
					MessageUtil.getMessage("stock.summary.upcoming.dividend.stale.quantity"),
					String.valueOf(staleQuantityCount),
					StockFormatUtil.fullKrw(StockFormatUtil.displayWon(currentQuantityTotal)))}
			</div>
		@endif
		<%-- "평균" 기준은 최근 12 건을 평균한다. 이력이 그보다 짧은 종목은 그만큼 짧은 평균이다(실측 2026-08-23: 8 종목 중 2 종목이 10 건). --%>
		@if(shortHistorySymbols != null && !shortHistorySymbols.isEmpty())
			<div class="mt-1 text-xs text-base-content/60" data-short-history-count="${shortHistorySymbols.size()}">
				${java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.calendar.short.history"), String.join(", ", shortHistorySymbols))}
			</div>
		@endif
		@if(schedule != null && schedule.hasDays())
			<%-- 이번 달 남은 예정과 다음 달 예정을 따로 더한다 - 한 줄로 합치면 "한 달 예상" 으로 읽힌다. 금액은 최근 기준, 칸마다 보이는 값의 합. --%>
			<div class="mt-2 flex flex-wrap items-baseline gap-x-3 gap-y-1 text-xs" data-upcoming-totals>
				<span><span class="text-base-content/60">${MessageUtil.getMessage("stock.dividend.upcoming.this.month")}</span> <span class="font-mono font-medium text-dividend amount-value" data-upcoming-this-month-total>${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(schedule.thisMonthTotal()))}</span></span>
				<span><span class="text-base-content/60">${MessageUtil.getMessage("stock.dividend.upcoming.next.month")}</span> <span class="font-mono font-medium text-dividend amount-value" data-upcoming-next-month-total>${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(schedule.nextMonthTotal()))}</span></span>
			</div>
			<ul class="mt-3 space-y-2" data-upcoming-daylist="${schedule.days().size()}">
				@for(DividendCalendarView.Day day : schedule.days())
					@template.stock.fragments.dividendCalendarDayCard(day = day, avgLabel = avgLabel, latestLabel = latestLabel, showMonth = true)
				@endfor
			</ul>
		@else
			<p class="mt-2 text-sm text-base-content/60" data-upcoming-none>${MessageUtil.getMessage("stock.dividend.upcoming.none")}</p>
		@endif
		<%-- 이번 달 예정일이 지났는데 원장에 받은 기록이 없다 - 늦는 지급이거나 원장 반영 전이다. 조용히 빼면 사라진 돈처럼 보인다. --%>
		@if(schedule != null && schedule.hasOverdue())
			<div class="mt-3 rounded-box bg-warning/10 px-3 py-2 text-xs" data-upcoming-overdue="${schedule.overdue().size()}">
				<div class="mb-1 font-medium text-base-content/70">${MessageUtil.getMessage("stock.dividend.upcoming.overdue.title")}</div>
				<ul class="space-y-0.5">
					@for(DividendCalendarView.Entry entry : schedule.overdue())
						<li class="flex items-center justify-between gap-2">
							<span class="truncate" title="${entry.name()}">${entry.name()}</span>
							<span class="shrink-0 font-mono"><span class="text-base-content/60">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.upcoming.overdue.day"), String.valueOf(entry.payDay()))}</span> <span class="text-dividend amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(entry.latest()))}</span></span>
						</li>
					@endfor
				</ul>
			</div>
		@endif
		<%-- 지급 이력이 없으면 날짜를 지어낼 수 없다. 목록에서 빼되 어느 종목인지는 밝힌다. --%>
		@if(schedule != null && schedule.hasUndated())
			<div class="mt-3 rounded-box bg-base-200/60 px-3 py-2 text-xs" data-upcoming-undated="${schedule.undated().size()}">
				<div class="mb-1 font-medium text-base-content/70">${MessageUtil.getMessage("stock.dividend.calendar.undated")}</div>
				<ul class="space-y-0.5">
					@for(DividendCalendarView.Entry entry : schedule.undated())
						<li class="flex items-center justify-between gap-2">
							<span class="truncate" title="${entry.name()}">${entry.name()}</span>
							<span class="shrink-0 font-mono text-dividend amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(entry.latest()))}</span>
						</li>
					@endfor
				</ul>
			</div>
		@endif
	@endif
</section>
