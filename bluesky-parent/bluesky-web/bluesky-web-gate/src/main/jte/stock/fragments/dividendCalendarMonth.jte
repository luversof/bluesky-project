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
<%-- 달 이동은 이 상자가 하지 않는다 - 실수령 배당의 기간 선택기(이번달 · ‹ ›)가 한다(2026-09-17 캘린더 탭을 실수령 배당에 통합,
     사용자 결정). 상자에 따로 버튼이 있으면 기간이 두 곳에서 바뀐다. --%>
@param String avgLabel = ""
@param String latestLabel = ""
<%-- 지나간 달은 금액도 추정이 아니다 - 그때는 '최근/평균' 이 아니라 '세전/실수령' 이다. --%>
@param String actualGrossLabel = ""
@param String actualNetLabel = ""

@if(calendar != null && calendar.month() != null)
!{
	java.util.Locale locale = org.springframework.context.i18n.LocaleContextHolder.getLocale();
	String monthLabel = java.text.MessageFormat.format(
		MessageUtil.getMessage("stock.dividend.calendar.month.label"),
		String.valueOf(calendar.month().getYear()),
		calendar.month().getMonth().getDisplayName(TextStyle.FULL, locale));
	String estimatedNote = MessageUtil.getMessage("stock.dividend.calendar.estimated");
	String settledNote = MessageUtil.getMessage("stock.dividend.calendar.settled");
	String actualNote = MessageUtil.getMessage("stock.dividend.calendar.actual");
	String settledEmptyNote = MessageUtil.getMessage("stock.dividend.calendar.settled.empty");
	String allOutsideNote = MessageUtil.getMessage("stock.dividend.calendar.all.outside");
	// 그 달 배당이 전부 월배당 기준 데이터 없는 종목이라 한 칸도 못 채운 달이 있다
	// (실측 2026-11: 삼성전자+SK텔레콤 3,481,886 원). 그때 '금액은 예상치' 라고 하면 가리킬 칸이 없다.
	// 날짜도 금액도 전부 실제일 때만 '실제' 라고 말한다. 한 칸이라도 추정이 섞여 있으면 화면은 추정이라고 말해야 한다.
	boolean allActual = !calendar.hasEstimatedDate() && calendar.hasActualAmount() && !calendar.hasEstimatedAmount();
	String missingLabel = MessageUtil.getMessage("stock.dividend.calendar.missing");
	String actualMonthTotalLabel = MessageUtil.getMessage("stock.dividend.calendar.actual.month.total");
	String dayTotalLabel = MessageUtil.getMessage("stock.dividend.calendar.day.total");
	String undatedLabel = MessageUtil.getMessage("stock.dividend.calendar.undated");
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
	</div>
	<%-- 달력이 놓는 날짜는 지급이력의 최빈일이다. 실제 지급일은 그 주변에 퍼져 있어(실측: 월중 17x39 · 19x20 ·
	     18x10 · 20x9), 날짜 하나만 적으면 그 날 들어오는 것으로 읽힌다. 그래서 예상임을 먼저 밝힌다. --%>
	<%-- 지나간 달은 그 달 지급이력이 있어 전부 확정이다 - 그때도 '추정' 이라고 적으면 화면이 거짓말을 한다.
	     실측 2026-09-15: 2026-08 은 최빈일 2·17 일에 놓였는데 실제 지급은 4·19 일이었고,
	     2 일은 일요일 · 17 일은 대체공휴일(광복절)이라 지급이 있을 수 없는 날에 찍혀 있었다. --%>
	<%-- 금액까지 실제인 달은 그렇다고 말한다 - 실측 2026-09-15: 지난 달력은 '지금 보유 수량 x 최근 주당 배당' 이라
	     2026-08 합계가 실제보다 +27.8%(종목별 최대 +132.5%) 였고 달을 넘겨도 값이 똑같았다. --%>
	<%-- 배당이 한 건도 없는 지난 달도 원장의 답이다 - 빈 격자만 두면 화면이 고장 난 것처럼 보이므로
	     그 달엔 받은 게 없다고 말한다. 실측 2026-09-15: 배당이 있는 첫 달(2020-04)부터 지난 달까지
	     사이에만 그런 달이 41 개 있고, 그 전 달은 전부 그렇다. --%>
	<p class="mb-3 text-xs text-base-content/60" data-calendar-estimate-note data-estimated="${String.valueOf(calendar.hasEstimatedDate())}" data-actual-amount="${String.valueOf(allActual)}" data-settled-empty="${String.valueOf(calendar.isSettledEmpty())}">${calendar.isSettledEmpty() ? settledEmptyNote : (!calendar.hasPlacedEntries() && calendar.hasMissing() ? allOutsideNote : (calendar.hasEstimatedDate() ? estimatedNote : (allActual ? actualNote : settledNote)))}</p>
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
							<td class="h-12 border border-base-200 align-top ${day.inMonth() ? "" : "bg-base-200/40"} ${day.today() ? "ring-2 ring-primary ring-inset" : ""}" data-calendar-day="${day.date().toString()}" data-in-month="${String.valueOf(day.inMonth())}" data-holiday="${holidayName}" >
								<%-- 앞뒤 달 날짜도 날짜다 - 흐리게 해도 읽혀야 한다. /30 은 흰 배경에서 대비 2.01:1 로 소형 텍스트 기준(4.5)에
								     한참 못 미쳤다(실측 2026-09-14). /60 이 그 기준을 넘는 가장 흐린 단계다(4.90:1).
								     '이번 달이 아니다' 는 칸 배경(bg-base-200/40)과 이번 달 날짜(/70)와의 차이가 말한다. --%>
								<div class="px-1 pt-0.5 text-[11px] ${day.today() ? "font-bold text-primary" : (day.inMonth() ? (redDay ? "text-error" : "text-base-content/70") : "text-base-content/60")}">${day.date().getDayOfMonth()}</div>
								@if(holidayName != null)
									<div class="truncate px-1 text-[9px] leading-tight text-error/80" title="${holidayName}">${holidayName}</div>
								@endif
								<%-- 42 칸 중 실제로 차는 칸은 평균 2 개다(실측 2026-09-15, 최근 24 개월: 지급일 1~3 일,
								     늘 월초 2~6 일과 월중 17~20 일 두 덩어리). 격자의 95% 가 구조적으로 비는데 그 면적 때문에
								     정작 금액이 11px 로 눌려 있었다 - 칸은 '며칠에 얼마' 만 말하고 종목별 금액은 아래 목록이 제 크기로 말한다. --%>
								@if(day.hasEntries())
									<div class="px-1 pb-1 text-center font-mono text-[12px] font-medium text-dividend" data-calendar-day-total data-calendar-entries="${day.entries().size()}">
										<span class="amount-value" aria-hidden="true">${StockFormatUtil.compactKrw(StockFormatUtil.displayWon(day.latestTotal()))}</span>
										<span class="sr-only">${dayTotalLabel} <span class="amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(day.latestTotal()))}</span></span>
									</div>
								@endif
							</td>
						@endfor
					</tr>
				@endfor
			</tbody>
		</table>
	</div>
	<%-- 달력은 '언제' 만 말한다. 종목과 금액은 여기서 제 크기로 읽힌다 - 칸 안 11px 로는
	     긴 종목명과 일곱 자리 금액이 함께 들어가지 않았다(사용자 지적 2026-09-15). --%>
	@if(!calendar.payDays().isEmpty())
		<ul class="mt-3 space-y-2" data-calendar-daylist="${calendar.payDays().size()}">
			@for(DividendCalendarView.Day day : calendar.payDays())
				@template.stock.fragments.dividendCalendarDayCard(day = day, avgLabel = avgLabel, latestLabel = latestLabel, actualGrossLabel = actualGrossLabel, actualNetLabel = actualNetLabel)
			@endfor
		</ul>
	@endif
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
	<%-- 달력은 월배당 기준 데이터가 있는 종목만 그린다. 그 달 원장에 있는 나머지는 통째로 안 보이므로
	     금액과 함께 밝힌다 - 실측 2026-09-15: 2026-08 은 분기배당인 삼성전자 1,886,082 원이 빠져
	     그 달 배당(세전 4,982,406 원)의 37.9% 가 달력 밖에 있었다. 안 밝히면 합이 안 맞는 까닭을 알 길이 없다.
	     금액은 amount-value 로 감싼다 - MessageFormat 안에 넣으면 '금액 숨김' 이 못 가린다. --%>
	@if(calendar.hasMissing())
		<div class="mt-3 rounded-box bg-warning/10 px-3 py-2 text-xs" data-calendar-missing="${calendar.missing().size()}">
			<div class="mb-1 font-medium text-base-content/70">${missingLabel}</div>
			<ul class="space-y-0.5">
				@for(DividendCalendarView.Missing item : calendar.missing())
					<li class="flex items-center justify-between gap-2">
						<span class="truncate" title="${item.name()}">${item.name()}</span>
						<span class="shrink-0 font-mono text-dividend amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(item.amount()))}</span>
					</li>
				@endfor
			</ul>
		</div>
	@endif
	<%-- 화면 위 요약 카드와 월중/월말 소계는 <b>지금 보유 수량</b> 기준 예상이라 달을 넘겨도 값이 같다
	     (실측 2026-09-15: 어느 달을 봐도 3,956,660 원). 그 옆에 그 달 실제 합계가 없으면 위 숫자가
	     그 달 얘기로 읽힌다 - 2026-08 은 실제 4,982,406 원으로 요약과 -20.6%, 2026-07 은 +16.9% 어긋난다.
	     달력에 놓인 것 + 놓지 못한 것을 모두 더한 값이라 위 두 상자 뒤에 둔다. --%>
	@if(calendar.hasActualAmount())
		<div class="mt-2 border-t border-base-200 pt-2 text-right text-xs text-base-content/70" data-calendar-actual-month-total>
			<span>${actualMonthTotalLabel}</span> <span class="font-mono font-medium text-dividend amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(calendar.actualMonthTotal()))}</span>
		</div>
	@endif
</div>
@endif
