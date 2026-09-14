@import io.github.luversof.boot.context.support.MessageUtil
@import java.time.DayOfWeek
@import java.time.format.TextStyle
@import net.luversof.web.gate.stock.dto.view.DividendCalendarView
@import net.luversof.web.gate.stock.util.StockFormatUtil

<%-- 배당 캘린더의 달력 한 달치.
     2026-09-14 까지 이 화면은 이름만 캘린더였다 - 날짜가 하나도 없고 월중/월말 두 덩어리로만 나뉘었다.
     그 덩어리 정보는 월배당 시뮬레이터 요약 카드에도 이미 있어(총 최근 월 배당금 옆 월중/월말 분해),
     이 화면이 홀로 주던 것이 "어느 종목이 어느 덩어리인가" 하나뿐이었다.
     종목을 제 지급일에 놓아 "언제 얼마가 들어오나" 에 답하게 한다. --%>
<%-- 자료가 없으면 아무것도 그리지 않는다. 조각을 기본값으로 훑는 렌더 검사가 널을 넣고 부른다. --%>
@param DividendCalendarView calendar = null
@param String prevMonth = ""
@param String nextMonth = ""
@param String thisMonth = ""
@param boolean isThisMonth = true
@param String avgLabel = ""
@param String latestLabel = ""

@if(calendar != null && calendar.month() != null)
!{
	java.util.Locale locale = org.springframework.context.i18n.LocaleContextHolder.getLocale();
	String monthLabel = java.text.MessageFormat.format(
		MessageUtil.getMessage("stock.dividend.calendar.month.label"),
		String.valueOf(calendar.month().getYear()),
		calendar.month().getMonth().getDisplayName(TextStyle.FULL, locale));
	String prevLabel = MessageUtil.getMessage("stock.dividend.calendar.nav.prev");
	String nextLabel = MessageUtil.getMessage("stock.dividend.calendar.nav.next");
	String thisLabel = MessageUtil.getMessage("stock.dividend.calendar.nav.this");
	String estimatedNote = MessageUtil.getMessage("stock.dividend.calendar.estimated");
	String spreadPattern = MessageUtil.getMessage("stock.dividend.calendar.spread");
	String samplePattern = MessageUtil.getMessage("stock.dividend.calendar.sample");
	String dayTotalLabel = MessageUtil.getMessage("stock.dividend.calendar.day.total");
	String undatedLabel = MessageUtil.getMessage("stock.dividend.calendar.undated");
	String taxableShortLabel = MessageUtil.getMessage("stock.dividend.calendar.taxable.short");
	String taxableUnregisteredPattern = MessageUtil.getMessage("stock.dividend.calendar.taxable.unregistered");
	String base = "/stock/dividend?tab=calendar&month=";
	// 공휴일 표는 손으로 채운 것이라 수록 범위가 있다. 범위 밖이면 이름이 전부 null 이라
	// '그 해엔 공휴일이 없다' 처럼 보인다 - 자료가 없는 것과 공휴일이 없는 것은 다르다.
	boolean holidayDataMissing = !net.luversof.web.gate.util.KoreanHolidays.covered(calendar.month().getYear());
	String holidayMissingNote = holidayDataMissing
		? java.text.MessageFormat.format(
			MessageUtil.getMessage("stock.dividend.calendar.holiday.unavailable"),
			String.valueOf(calendar.month().getYear()))
		: "";
}

<div class="rounded-2xl border border-base-300 bg-base-100 p-4 shadow-sm" data-dividend-calendar data-calendar-month="${calendar.month().toString()}">
	<div class="mb-3 flex flex-wrap items-center justify-between gap-2">
		<h2 class="text-base font-bold" data-page-section>${monthLabel}</h2>
		<div class="join">
			<a class="btn btn-sm join-item" href="${base + prevMonth}" aria-label="${prevLabel}" data-calendar-prev><span aria-hidden="true">‹</span></a>
			<a class="btn btn-sm join-item ${isThisMonth ? "btn-active" : ""}" href="${base + thisMonth}" data-calendar-this>${thisLabel}</a>
			<a class="btn btn-sm join-item" href="${base + nextMonth}" aria-label="${nextLabel}" data-calendar-next><span aria-hidden="true">›</span></a>
		</div>
	</div>
	<%-- 달력이 놓는 날짜는 지급이력의 최빈일이다. 실제 지급일은 그 주변에 퍼져 있어(실측: 월중 17x39 · 19x20 ·
	     18x10 · 20x9), 날짜 하나만 적으면 그 날 들어오는 것으로 읽힌다. 그래서 예상임을 먼저 밝힌다. --%>
	<p class="mb-3 text-xs text-base-content/60" data-calendar-estimate-note>${estimatedNote}</p>
	@if(holidayDataMissing)
		<p class="mb-3 text-xs text-warning" data-calendar-holiday-missing>${holidayMissingNote}</p>
	@endif
	<div class="overflow-x-auto">
		<table class="w-full table-fixed border-collapse text-xs" aria-label="${monthLabel}" data-calendar-table>
			<thead>
				<tr>
					@for(DayOfWeek dow : new DayOfWeek[]{DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY})
						<th scope="col" class="border border-base-200 px-1 py-1 text-center font-medium ${dow == DayOfWeek.SUNDAY || dow == DayOfWeek.SATURDAY ? "text-error" : "text-base-content/70"}">${dow.getDisplayName(TextStyle.SHORT, locale)}</th>
					@endfor
				</tr>
			</thead>
			<tbody>
				@for(DividendCalendarView.Week week : calendar.weeks())
					<tr>
						@for(DividendCalendarView.Day day : week.days())
							<%-- 공휴일은 표시만 하고 종목은 옮기지 않는다. 지급일을 영업일로 보정해 봤더니 최빈일 그대로 두는 것보다
							     오히려 빗나갔다(기록: 다가올 배당). 달력은 '그 날이 쉬는 날' 이라는 사실만 말한다. --%>
							!{String holidayName = day.inMonth() ? net.luversof.web.gate.util.KoreanHolidays.holidayName(day.date()) : null;}
							!{boolean weekend = day.date().getDayOfWeek() == DayOfWeek.SUNDAY || day.date().getDayOfWeek() == DayOfWeek.SATURDAY;}
							!{boolean redDay = day.inMonth() && (weekend || holidayName != null);}
							<td class="h-24 border border-base-200 align-top ${day.inMonth() ? "" : "bg-base-200/40"} ${day.today() ? "ring-2 ring-primary ring-inset" : ""}" data-calendar-day="${day.date().toString()}" data-in-month="${String.valueOf(day.inMonth())}" data-holiday="${holidayName}" >
								<%-- 앞뒤 달 날짜도 날짜다 - 흐리게 해도 읽혀야 한다. /30 은 흰 배경에서 대비 2.01:1 로 소형 텍스트 기준(4.5)에
								     한참 못 미쳤다(실측 2026-09-14). /60 이 그 기준을 넘는 가장 흐린 단계다(4.90:1).
								     '이번 달이 아니다' 는 칸 배경(bg-base-200/40)과 이번 달 날짜(/70)와의 차이가 말한다. --%>
								<div class="px-1 pt-0.5 text-[11px] ${day.today() ? "font-bold text-primary" : (day.inMonth() ? (redDay ? "text-error" : "text-base-content/70") : "text-base-content/60")}">${day.date().getDayOfMonth()}</div>
								@if(holidayName != null)
									<div class="truncate px-1 text-[9px] leading-tight text-error/80" title="${holidayName}">${holidayName}</div>
								@endif
								@if(day.hasEntries())
									<ul class="space-y-0.5 px-1 pb-1">
										@for(DividendCalendarView.Entry entry : day.entries())
											!{String entryNote = (entry.spread()
												? java.text.MessageFormat.format(spreadPattern, String.valueOf(entry.earliestDay()), String.valueOf(entry.latestDay())) + " "
												: "") + java.text.MessageFormat.format(samplePattern, String.valueOf(entry.sampleCount()));}
											<li class="leading-tight" data-calendar-entry="${entry.symbol()}" title="${entryNote}">
												<div class="truncate text-[11px] font-medium" title="${entry.name()}">${entry.name()}</div>
												<div class="font-mono text-[11px] text-dividend"><span class="text-base-content/60">${latestLabel}</span> <span class="amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(entry.latest()))}</span></div>
												<div class="font-mono text-[10px] text-base-content/60"><span>${avgLabel}</span> <span class="amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(entry.average()))}</span><span class="sr-only">, ${entryNote}</span></div>
												<%-- 과세표준은 0 이어도 적는다 - 비과세인지 아닌지가 세후 수령액을 가른다.
												     다만 저장값 0 인데 지급이력은 과세가 있었다고 말하는 종목이 있다(실측 2026-09-14: 8 중 2 종목, 31.3% · 25.1%).
												     그 0 은 비과세가 아니라 기준 데이터 미등록이므로, 0 만 적고 말면 없는 비과세를 알리는 셈이다. --%>
												!{boolean taxableUnknown = entry.taxableLooksUnregistered();}
												!{String taxableNote = taxableUnknown ? java.text.MessageFormat.format(taxableUnregisteredPattern, StockFormatUtil.pct(entry.historyTaxableRatioPct().doubleValue(), 1)) : "";}
												<div class="font-mono text-[10px] ${taxableUnknown ? "text-warning" : "text-base-content/60"}" data-calendar-taxable data-taxable-unknown="${String.valueOf(taxableUnknown)}" title="${taxableNote}"><span>${taxableShortLabel}</span> <span class="amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(entry.taxable()))}</span>@if(taxableUnknown)<span aria-hidden="true">&nbsp;?</span><span class="sr-only">, ${taxableNote}</span>@endif</div>
											</li>
										@endfor
									</ul>
									@if(day.entries().size() > 1)
										<div class="border-t border-base-200 px-1 py-0.5 text-right font-mono text-[10px] text-base-content/60" data-calendar-day-total>
											<span class="sr-only">${dayTotalLabel} </span><span class="amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(day.latestTotal()))}</span>
										</div>
									@endif
								@endif
							</td>
						@endfor
					</tr>
				@endfor
			</tbody>
		</table>
	</div>
	<%-- 지급이력이 없으면 날짜를 지어낼 수 없다. 달력에서 빼되 어느 종목이 빠졌는지는 밝힌다 -
	     안 밝히면 달력 합계가 월 합계와 안 맞는 이유를 알 길이 없다. --%>
	@if(calendar.hasUndated())
		<div class="mt-3 rounded-box bg-base-200/60 px-3 py-2 text-xs" data-calendar-undated="${calendar.undated().size()}">
			<div class="mb-1 font-medium text-base-content/70">${undatedLabel}</div>
			<ul class="space-y-0.5">
				@for(DividendCalendarView.Entry entry : calendar.undated())
					<li class="flex items-center justify-between gap-2">
						<span class="truncate" title="${entry.name()}">${entry.name()}</span>
						<span class="shrink-0 font-mono text-dividend amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(entry.latest()))}</span>
					</li>
				@endfor
			</ul>
		</div>
	@endif
</div>
@endif
