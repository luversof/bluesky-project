@import io.github.luversof.boot.context.support.MessageUtil
@import java.time.DayOfWeek
@import java.time.format.TextStyle
@import net.luversof.web.gate.stock.dto.view.DividendCalendarView
@import net.luversof.web.gate.stock.util.StockFormatUtil

<%-- 지급일마다 한 장. 왼쪽 큰 숫자가 '며칠' 을, 오른쪽 큰 숫자가 '얼마' 를 먼저 말하고
     종목별 내역이 그 아래 붙는다(사용자 선택 2026-09-15). 한 달에 한두 장뿐이라 카드가 과하지 않다.
     실수령 배당의 달력(그 달 실지급)과 화면 위 '다가올 배당'(예정)이 같은 카드를 쓴다 - 2026-09-17 캘린더 탭 통합. --%>
@param DividendCalendarView.Day day
@param String avgLabel = ""
@param String latestLabel = ""
<%-- 지나간 달은 금액도 추정이 아니다 - 그때는 '최근/평균' 이 아니라 '세전/실수령' 이다. --%>
@param String actualGrossLabel = ""
@param String actualNetLabel = ""
<%-- 두 달에 걸친 목록(다가올 배당)에서는 날짜 숫자만으로 어느 달인지 모른다 - 그때 달을 함께 적는다. --%>
@param boolean showMonth = false

!{
	java.util.Locale locale = org.springframework.context.i18n.LocaleContextHolder.getLocale();
	String spreadPattern = MessageUtil.getMessage("stock.dividend.calendar.spread");
	String samplePattern = MessageUtil.getMessage("stock.dividend.calendar.sample");
	String dayTotalLabel = MessageUtil.getMessage("stock.dividend.calendar.day.total");
	String dayTitlePattern = MessageUtil.getMessage("stock.dividend.calendar.day.title");
	String taxableShortLabel = MessageUtil.getMessage("stock.dividend.calendar.taxable.short");
	String taxableUnregisteredPattern = MessageUtil.getMessage("stock.dividend.calendar.taxable.unregistered");
	String dayHoliday = net.luversof.web.gate.util.KoreanHolidays.holidayName(day.date());
	String monthName = day.date().getMonth().getDisplayName(TextStyle.SHORT, locale);
	String dayTitle = (showMonth ? monthName + " " : "") + java.text.MessageFormat.format(dayTitlePattern, String.valueOf(day.date().getDayOfMonth()), day.date().getDayOfWeek().getDisplayName(TextStyle.SHORT, locale));
	boolean dayRed = day.date().getDayOfWeek() == DayOfWeek.SUNDAY || day.date().getDayOfWeek() == DayOfWeek.SATURDAY || dayHoliday != null;
	boolean estimatedDay = day.entries() != null && !day.entries().isEmpty() && day.entries().stream().noneMatch(DividendCalendarView.Entry::actualAmount);
}
<%-- 예상(실수령이 아닌) 칸은 최근을 앞에, 평균을 뒤에 함께 적는다(2026-10-13 사용자 선택 - 처음엔 평균을 앞에 뒀다가 "최근을 우선으로"). 이 칸 합계가 최근만 더해 대시보드의
     다가올 배당(평균)과 같은 날 금액이 달랐다 - 10월 17일분 1,601,445 vs 997,459. 실수령 칸은 세전 · 실수령 그대로. --%>
<li class="rounded-box border border-base-200 px-3 py-2" data-calendar-daylist-day="${day.date().toString()}">
	<%-- 날짜 덩어리는 눈으로만 읽는다 - 낭독기에는 '4일 (화)' 를 한 번만 준다(숫자와 요일이 따로 읽히면 어수선하다). --%>
	<span class="sr-only">${dayTitle}@if(dayHoliday != null), ${dayHoliday}@endif</span>
	<div class="flex items-start gap-3">
		<div class="w-12 shrink-0 text-center leading-tight" aria-hidden="true">
			@if(showMonth)<div class="text-[11px] text-base-content/60" data-daycard-month>${monthName}</div>@endif
			<div class="text-2xl font-medium ${dayRed ? "text-error" : ""}">${day.date().getDayOfMonth()}</div>
			<div class="text-[11px] ${dayRed ? "text-error/80" : "text-base-content/60"}">${day.date().getDayOfWeek().getDisplayName(TextStyle.SHORT, locale)}</div>
			@if(dayHoliday != null)<div class="truncate text-[10px] leading-tight text-error/80" title="${dayHoliday}">${dayHoliday}</div>@endif
		</div>
		<div class="min-w-0 flex-1">
			@if(estimatedDay)
			<div class="mb-1 flex flex-wrap items-baseline gap-x-2 font-mono" data-daycard-totals>
				<span class="text-base font-medium text-dividend" data-daycard-latest-total><span class="sr-only">${dayTotalLabel} </span><span class="mr-1 text-[11px] font-normal text-base-content/60">${latestLabel}</span><span class="amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(day.latestTotal()))}</span></span>
				<span class="text-xs text-base-content/60" data-daycard-average-total>${avgLabel} <span class="amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(day.averageTotal()))}</span></span>
			</div>
			@else
			<div class="mb-1 font-mono text-base font-medium text-dividend"><span class="sr-only">${dayTotalLabel} </span><span class="amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(day.latestTotal()))}</span></div>
			@endif
			<ul class="space-y-1">
				@for(DividendCalendarView.Entry entry : day.entries())
					<%-- 폭 안내와 표본 수는 추정일 때만 할 말이다. 실제 수령액에는 붙이지 않는다. --%>
					<%-- 식별자는 비워 두지 않는다 - JTE 는 값이 null 인 속성을 통째로 지우고,
					     원장으로만 놓은 종목은 심볼이 없다(실측 2024-11: 두 종목 전부 속성이 사라져 QA 탐침이 0 종목으로 읽었다). --%>
					!{String entryNote = entry.actualAmount() ? "" : ((entry.spread()
						? java.text.MessageFormat.format(spreadPattern, String.valueOf(entry.earliestDay()), String.valueOf(entry.latestDay())) + " "
						: "") + java.text.MessageFormat.format(samplePattern, entry.sampleCount()));}
					!{boolean taxableUnknown = entry.taxableLooksUnregistered();}
					!{String taxableNote = taxableUnknown ? java.text.MessageFormat.format(taxableUnregisteredPattern, StockFormatUtil.pct(entry.historyTaxableRatioPct().doubleValue(), 1)) : "";}
					<li class="flex flex-col gap-y-0.5 sm:flex-row sm:flex-wrap sm:items-baseline sm:justify-between sm:gap-x-3" data-calendar-entry="${entry.symbol() != null ? entry.symbol() : ""}" data-calendar-entry-name="${entry.name()}" title="${entryNote}">
						<span class="min-w-0 truncate text-xs" title="${entry.name()}">${entry.name()}</span>
						<%-- 넓은 화면에서 이름(왼쪽)과 금액(오른쪽) 사이가 수백 px 라 줄을 눈으로 잇기 어려웠다 - 점선 길잡이(2026-10-28 12차 자율). --%>
						<span class="hidden sm:block min-w-4 flex-1 self-center border-b border-dotted border-base-content/20" aria-hidden="true" data-calendar-entry-leader></span>
						<span class="flex flex-wrap items-baseline gap-x-2 font-mono text-[11px] sm:shrink-0">
							@if(entry.actualAmount())
							<span class="text-xs font-medium text-dividend"><span class="sr-only">${actualGrossLabel} </span><span class="amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(entry.latest()))}</span></span>
							<span class="text-base-content/60">${actualNetLabel} <span class="amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(entry.average()))}</span></span>
							@else
							<span class="text-xs font-medium text-dividend" data-calendar-entry-latest><span class="font-normal text-base-content/60">${latestLabel}</span> <span class="amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(entry.latest()))}</span></span>
							<span class="text-base-content/60" data-calendar-entry-average>${avgLabel} <span class="amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(entry.average()))}</span></span>
							@endif
							<span class="${taxableUnknown ? "text-warning" : "text-base-content/60"}" data-calendar-taxable data-taxable-unknown="${String.valueOf(taxableUnknown)}" title="${taxableNote}">${taxableShortLabel} <span class="amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(entry.taxable()))}</span>@if(taxableUnknown)<span aria-hidden="true">&nbsp;?</span><span class="sr-only">, ${taxableNote}</span>@endif</span>
							@if(!entryNote.isEmpty())<span class="sr-only">, ${entryNote}</span>@endif
						</span>
					</li>
				@endfor
			</ul>
		</div>
	</div>
</li>
