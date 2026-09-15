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
@param String dividendHistoryTabHref = "/stock/dividend?tab=history"
@param String dividendCalendarTabHref = "/stock/dividend?tab=calendar"
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
@param boolean calendarCurrentQuantityUnavailable = false
@param BigDecimal calendarCurrentQuantityTotal = java.math.BigDecimal.ZERO
<%-- 달력 한 달치. 컨트롤러가 지급이력의 종목별 최빈 지급일로 종목을 날짜에 놓아 둔다. --%>
@param net.luversof.web.gate.stock.dto.view.DividendCalendarView dividendCalendar = null
@param String calendarPrevHref = ""
@param String calendarNextHref = ""
@param String calendarThisHref = ""
@param boolean calendarIsThisMonth = true
@param BigDecimal calendarTaxableMonthly = java.math.BigDecimal.ZERO
@param BigDecimal calendarTaxableAnnual = java.math.BigDecimal.ZERO

!{
	boolean isCalendar = "calendar".equals(dividendTab);
	String activeTabClass = "tab tab-active flex-1 font-bold !bg-primary !text-primary-content shadow-sm";
	String inactiveTabClass = "tab flex-1 text-base-content/60 hover:text-base-content";

	String midLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.mid.month");
	String endLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.month.end");
	String otherLabel = MessageUtil.getMessage("stock.dividend.calendar.window.other");
	String windowBasisLabel = MessageUtil.getMessage("stock.dividend.calendar.window.basis");
	String monthlyTotalLabel = MessageUtil.getMessage("stock.dividend.calendar.monthly.total");
	String annualTotalLabel = MessageUtil.getMessage("stock.dividend.calendar.annual.total");
	String avgBasisLabel = MessageUtil.getMessage("stock.dividend.calendar.basis.average");
	String latestBasisLabel = MessageUtil.getMessage("stock.dividend.calendar.basis.latest");
	// 지나간 달은 금액도 추정이 아니다 - 그때 두 줄은 세전/실수령이다.
	String actualGrossBasisLabel = MessageUtil.getMessage("stock.dividend.calendar.basis.actual.gross");
	String actualNetBasisLabel = MessageUtil.getMessage("stock.dividend.calendar.basis.actual.net");
	String taxableTotalLabel = MessageUtil.getMessage("stock.dividend.calendar.taxable.total");
	String monthlyBasisLabel = MessageUtil.getMessage("stock.dividend.calendar.basis.monthly");
	String annualBasisLabel = MessageUtil.getMessage("stock.dividend.calendar.basis.annual");
	String calendarDesc = MessageUtil.getMessage("stock.dividend.calendar.desc");
	String emptyLabel = MessageUtil.getMessage("stock.dividend.calendar.empty");
	boolean calendarEmpty = (midRows == null || midRows.isEmpty())
		&& (endRows == null || endRows.isEmpty())
		&& (otherRows == null || otherRows.isEmpty());
	// 탭마다 문서 제목을 달리한다. 실측 2026-09-12: 이 화면의 두 탭이 제목 한 종류("배당 내역")를 써서
	// 탭 두 개를 함께 열어 두면 브라우저 탭·기록·즐겨찾기에서 구분되지 않았다. 시뮬레이터(3탭 3종)와
	// 관리(2탭 2종)는 이미 탭 이름을 붙이고 있다 - 같은 규칙을 쓴다.
	String dividendTabLabel = isCalendar
		? MessageUtil.getMessage("stock.page.dividend.tab.calendar")
		: MessageUtil.getMessage("stock.page.dividend.tab.history");
	String dividendPageTitle = MessageUtil.getMessage("stock.page.dividend.title") + " · " + dividendTabLabel;
}

@template._layout.stockLayout(
	pageTitle = dividendPageTitle,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentStock = @`
		<div class="space-y-6" data-sync-url="/stock/htmx/dividend/list">
			@template._components.ui.pageHeader(title = MessageUtil.getMessage("stock.page.dividend.title"))

			<!-- 같은 페이지 탭: 클릭 시 이 페이지에서 내용만 교체된다 -->
			<div role="tablist" class="tabs tabs-box w-full bg-base-200/70 p-1">
				<a role="tab" href="${dividendHistoryTabHref}" class="${!isCalendar ? activeTabClass : inactiveTabClass}">${MessageUtil.getMessage("stock.page.dividend.tab.history")}</a>
				<a role="tab" href="${dividendCalendarTabHref}" class="${isCalendar ? activeTabClass : inactiveTabClass}">${MessageUtil.getMessage("stock.page.dividend.tab.calendar")}</a>
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
				<%-- 지나간 달은 달력이 예상이 아니라 그 달 실제 지급을 그린다 - 그때 '예상 지급 일정입니다' 는
				     화면 맨 위에서 아래 달력과 반대되는 말을 하는 셈이다. 그 달 안내는 달력 상자가 이미 하므로
				     여기서는 접는다(같은 말을 두 번 하지 않는다). --%>
				!{boolean calendarShowsActual = dividendCalendar != null && dividendCalendar.hasActualAmount();}
				@if(!calendarShowsActual)
					<p class="text-sm text-base-content/60" data-calendar-desc>${calendarDesc}</p>
				@endif
				@if(calendarEmpty)
					<%-- 빈 상태엔 다음 행동을 준다(공용 emptyState 의 규칙). 이 상자만 손으로 만들어 그 규칙 밖에 있었다 -
					     문구는 '월배당 기준 데이터에서 종목을 등록하라' 고 말하면서 정작 그리로 가는 링크가 없었다.
					     화면 위에 같은 링크가 있긴 하지만, 빈 상태에서 다음 걸음을 그 자리에 두는 것이 이 저장소의 규칙이다
					     (기간 넓히기 CTA 를 네 화면에 붙인 것과 같은 이유). --%>
					@template._components.ui.emptyState(
						message = emptyLabel,
						boxed = true,
						actionLabel = MessageUtil.getMessage("stock.page.dividend.tab.monthly.reference"),
						actionHref = "/stock/admin?tab=monthly-reference")
				@else
					<div class="grid grid-cols-2 gap-3 lg:grid-cols-3">
						@template.stock.fragments.dividendCalendarTotalCard(
							label = monthlyTotalLabel,
							topLabel = avgBasisLabel,
							bottomLabel = latestBasisLabel,
							topValue = monthlyTotal,
							bottomValue = monthlyTotalLatest)
						@template.stock.fragments.dividendCalendarTotalCard(
							label = annualTotalLabel,
							topLabel = avgBasisLabel,
							bottomLabel = latestBasisLabel,
							topValue = annualTotal,
							bottomValue = annualTotalLatest)
						<%-- 과세표준은 '최근' 기준이 스냅샷에 없다. 배당 카드가 평균/최근을 쌓는 자리에 월/연을 쌓는다. --%>
						@template.stock.fragments.dividendCalendarTotalCard(
							label = taxableTotalLabel,
							topLabel = monthlyBasisLabel,
							bottomLabel = annualBasisLabel,
							topValue = calendarTaxableMonthly,
							bottomValue = calendarTaxableAnnual)
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
					     하고 아무 말 없이 나가기도 했다(실측 2026-08-23: 8종목 중 7종목이 어긋나
					     1.66% 낮다). 문구·계산은 다른 두 화면과 같은 것을 쓴다. --%>
					<%-- 조회가 실패하면 어긋난 줄이 0 이라 위 안내가 사라진다 - 안내 없는 화면은 "원장과 같다" 로 읽힌다. --%>
					@if(calendarCurrentQuantityUnavailable)
						<div class="text-[11px] text-warning mb-2" data-ledger-unavailable="true">${MessageUtil.getMessage("stock.holdings.ledger.unavailable")}</div>
					@endif
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
					<%-- 월중/월말은 소계 한 줄로만 남긴다. 달력이 종목별 날짜를 말하므로 덩어리 목록은 더 필요 없고,
					     같은 목록은 월배당 시뮬레이터에 정렬 가능한 표로 이미 있다.
					     금액은 amount-value 로 감싼다 - MessageFormat 안에 넣으면 '금액 숨김' 이 못 가린다. --%>
					<%-- 라벨 없는 숫자 셋이었다 - 이 값들은 달과 무관한 '지금 보유 수량 기준 예상' 인데
					     그 사실이 화면 어디에도 없어, 지나간 달을 보면 그 달 실적으로 읽혔다
					     (실측 2026-09-15: 어느 달을 봐도 3,956,660 원 고정, 2026-08 실제와 -20.6% 차이). --%>
					<div class="mb-3 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-base-content/60" data-calendar-window-subtotal>
						<span class="font-medium text-base-content/70" data-calendar-subtotal-basis>${windowBasisLabel}</span>
						<span><span>${midLabel}</span> <span class="font-mono amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(midTotalLatest))}</span></span>
						<span><span>${endLabel}</span> <span class="font-mono amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(endTotalLatest))}</span></span>
						@if(otherRows != null && !otherRows.isEmpty())
							<span><span>${otherLabel}</span> <span class="font-mono amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(otherTotalLatest))}</span></span>
						@endif
					</div>
					@if(dividendCalendar != null)
						@template.stock.fragments.dividendCalendarMonth(
							calendar = dividendCalendar,
							prevHref = calendarPrevHref,
							nextHref = calendarNextHref,
							thisHref = calendarThisHref,
							isThisMonth = calendarIsThisMonth,
							avgLabel = avgBasisLabel,
							latestLabel = latestBasisLabel,
							actualGrossLabel = actualGrossBasisLabel,
							actualNetLabel = actualNetBasisLabel)
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
