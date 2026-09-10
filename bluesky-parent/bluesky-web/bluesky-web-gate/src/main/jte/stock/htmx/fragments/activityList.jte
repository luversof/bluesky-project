@import net.luversof.web.gate.util.CspNonceHolder
@import java.util.List
@import net.luversof.web.gate.stock.controller.StockTradeHtmxController.Activity
@import net.luversof.web.gate.stock.domain.Account
@import net.luversof.web.gate.stock.domain.StockItem
@import java.util.UUID
@import java.time.ZoneId
@import java.time.format.DateTimeFormatter
@import java.time.Instant
@import java.math.BigDecimal
@import java.text.DecimalFormat
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.stock.util.StockFormatUtil

@param List<Activity> activities
@param String activityView = "all"
@param java.time.Instant startDate = null
@param java.time.Instant endDate = null
@param String timeZone = null
@param String rangeMode = null
@param String dataFirstDate = null
@param List<Account> accountList = null
@param List<StockItem> stockItemList = null
@param List<String> stockTagList = java.util.List.of()
@param UUID selectedAccountId = null
@param UUID selectedStockItemId = null
@param List<UUID> selectedAccountIds = java.util.List.of()
@param List<UUID> selectedStockItemIds = java.util.List.of()
@param List<String> selectedStockTags = java.util.List.of()
@param long buyCount = 0L
@param long sellCount = 0L
@param BigDecimal buyAmount = BigDecimal.ZERO
@param BigDecimal sellAmount = BigDecimal.ZERO
@param long dividendCount = 0L
@param BigDecimal dividendAmount = BigDecimal.ZERO
@param java.util.Map<UUID, String> accountNames = java.util.Map.of()

!{
	DecimalFormat df = new DecimalFormat("#,##0");
	BigDecimal safeBuy = buyAmount != null ? buyAmount : BigDecimal.ZERO;
	BigDecimal safeSell = sellAmount != null ? sellAmount : BigDecimal.ZERO;
	BigDecimal safeDividend = dividendAmount != null ? dividendAmount : BigDecimal.ZERO;
	java.util.function.Function<Object, String> countMessage = value -> java.text.MessageFormat.format(MessageUtil.getMessage("common.count"), value);
	String advancedSearchLabel = MessageUtil.getMessage("stock.activity.label.advanced.search");
	String periodLabel = MessageUtil.getMessage("common.label.period");
	String accountLabel = MessageUtil.getMessage("common.label.account");
	String stockLabel = MessageUtil.getMessage("common.label.stock");
	String tagLabel = MessageUtil.getMessage("common.label.tag");
	String tagHelperLabel = MessageUtil.getMessage("stock.filter.tag.helper");
	String tagSummaryLabel = MessageUtil.getMessage("stock.filter.tag.summary.selected");
	String tagStockSummaryPattern = MessageUtil.getMessage("stock.filter.tag.summary.stocks");
	String tagMorePattern = MessageUtil.getMessage("stock.filter.tag.summary.more");
	String allLabel = MessageUtil.getMessage("stock.label.period.all");
	String searchLabel = MessageUtil.getMessage("common.button.search");
	String resetLabel = MessageUtil.getMessage("common.button.reset");
	String buyLabel = MessageUtil.getMessage("stock.trade.label.buy");
	String sellLabel = MessageUtil.getMessage("stock.trade.label.sell");
	String tradeLabel = MessageUtil.getMessage("stock.activity.label.trade");
	String dividendLabel = MessageUtil.getMessage("stock.activity.label.dividend");
	String realizedProfitLabel = MessageUtil.getMessage("stock.profit.realized");
	String dividendChartTitle = MessageUtil.getMessage("stock.activity.chart.dividend.title");
	String realizedChartTitle = MessageUtil.getMessage("stock.activity.chart.realized.title");
	String tradeStatusLabel = MessageUtil.getMessage("stock.activity.card.trade.status");
	String dividendReceivedLabel = MessageUtil.getMessage("stock.activity.card.dividend.received");
	String totalActivityLabel = MessageUtil.getMessage("stock.activity.card.total.activity");
	String noActivityDataLabel = MessageUtil.getMessage("stock.activity.message.no.data");
	String dateLabel = MessageUtil.getMessage("common.label.date");
	String typeLabel = MessageUtil.getMessage("common.label.type");
	String stockNameLabel = MessageUtil.getMessage("stock.activity.label.stock.name");
	String detailLabel = MessageUtil.getMessage("common.label.detail");
	String amountLabel = MessageUtil.getMessage("common.label.amount");
	String sharesLabel = MessageUtil.getMessage("stock.activity.label.shares");
}

<%-- 보이는 뷰 하나만 그린다. activityView 가 "all" 이면(스크립트 없는 환경/직접 호출) 예전처럼 3종 모두. --%>
!{boolean showCalendarView = "all".equals(activityView) || "calendar".equals(activityView);}
!{boolean showTimelineView = "all".equals(activityView) || "timeline".equals(activityView);}
!{boolean showListView = "all".equals(activityView) || "list".equals(activityView);}
<div id="activityListFragment" data-activity-view="${activityView}">

	!{
		boolean isAllMode = "all".equals(rangeMode);
		java.time.ZoneId zone = net.luversof.web.gate.stock.util.StockZoneUtil.resolve(timeZone);
		java.time.LocalDate today = java.time.LocalDate.now(zone);
		java.time.LocalDate startLocal = startDate != null ? startDate.atZone(zone).toLocalDate() : null;
		java.time.LocalDate endLocal = endDate != null ? endDate.atZone(zone).toLocalDate() : null;
		boolean canNext = !isAllMode && endLocal != null && endLocal.isBefore(today);
		boolean canPrev = !isAllMode && startLocal != null && dataFirstDate != null && !dataFirstDate.isEmpty()
			&& startLocal.isAfter(java.time.LocalDate.parse(dataFirstDate));
		// '전체' 는 날짜를 안 보내므로 기간 배지가 빌 수밖에 없다. 그때는 이 화면이 실제로 그린 자료의
		// 구간을 대신 적는다 - 화면마다 덮는 범위가 다르므로 사용자 전체의 최초 일자를 쓰면 틀린 날짜가 나온다.
		var coveredRange = net.luversof.web.gate.stock.util.StockCoveredRangeUtil.covered(activities, net.luversof.web.gate.stock.controller.StockTradeHtmxController.Activity::date, zone);
	}
	@template.stock.htmx.fragments.dateRangeNavBar(
		pickerName = "activityPicker",
		btnClass = "date-range-btn",
		canPrev = canPrev,
		canNext = canNext,
		rangeMode = rangeMode,
		startDate = startLocal,
		endDate = endLocal,
		coveredStartDate = coveredRange.startDate(),
		coveredEndDate = coveredRange.endDate()
	)

	<!-- Filter Form (collapsible) -->
	<details class="mb-4" data-detail-filter data-filter-active="${(selectedAccountIds != null && !selectedAccountIds.isEmpty()) || (selectedStockItemIds != null && !selectedStockItemIds.isEmpty()) || (selectedStockTags != null && !selectedStockTags.isEmpty()) ? "true" : "false"}">
		<summary class="flex items-center gap-2 cursor-pointer select-none text-sm font-medium text-base-content/60 hover:text-base-content py-1 px-2 rounded hover:bg-base-200 w-fit transition-colors">
			<svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2a1 1 0 01-.293.707L13 13.414V19a1 1 0 01-.553.894l-4 2A1 1 0 017 21v-7.586L3.293 6.707A1 1 0 013 6V4z" /></svg>
			${advancedSearchLabel}
			@template.stock.htmx.fragments.components.filterBadge(
				selectedAccountId = selectedAccountId,
				selectedStockItemId = selectedStockItemId,
				selectedAccountIds = selectedAccountIds,
				selectedStockItemIds = selectedStockItemIds,
				selectedStockTags = selectedStockTags
			)
		</summary>
	<form id="activitySearchForm" class="mt-2 mb-2 rounded-2xl bg-base-100 p-3 shadow-sm"
		  hx-get="/stock/htmx/activity-list"
		  hx-target="#activityListFragment"
		  hx-select="#activityListFragment"
		  hx-swap="outerHTML"
		  hx-trigger="submit">
		<div class="stock-filter-grid">
			<div class="form-control w-full">
				<label class="label p-1" id="filterPeriodLabel"><span class="label-text text-xs">${periodLabel}</span></label>
				<div class="join w-full" role="group" aria-labelledby="filterPeriodLabel">
					<input type="date" name="activityUiStartDate" aria-label="${MessageUtil.getMessage("common.label.date.start")}" id="activityStartDateInput" class="input input-sm input-bordered join-item w-1/2" value="${startDate != null ? startDate.atZone(zone).toLocalDate().toString() : ""}">
					<input type="date" name="activityUiEndDate" aria-label="${MessageUtil.getMessage("common.label.date.end")}" id="activityEndDateInput" class="input input-sm input-bordered join-item w-1/2" value="${endDate != null ? endDate.atZone(zone).toLocalDate().minusDays(1).toString() : ""}">
				</div>
			</div>
			<div class="form-control w-full">
				<label class="label p-1" for="filterAccountSelect"><span class="label-text text-xs">${accountLabel}</span></label>
				<select id="filterAccountSelect" name="accountIdList" class="select select-bordered w-full max-h-40 overflow-auto" multiple size="${Math.min(accountList != null ? accountList.size() + 1 : 1, 7)}" data-max-visible="7">
					<option value="">${allLabel}</option>
					@if(accountList != null)
						@for(Account account : accountList)
							<option value="${account.id().toString()}" selected="${selectedAccountIds != null && selectedAccountIds.contains(account.id())}">${account.name()}</option>
						@endfor
					@endif
				</select>
			</div>

			<div class="form-control w-full">
				<label class="label p-1" for="filterStockSelect"><span class="label-text text-xs">${stockLabel}</span></label>
				<select id="filterStockSelect" name="stockItemIdList" class="select select-bordered w-full max-h-40 overflow-auto bg-base-100" multiple size="${Math.min(stockItemList != null ? stockItemList.size() + 1 : 1, 7)}" data-max-visible="7">
					<option value="">${allLabel}</option>
					@if(stockItemList != null)
						@for(StockItem stockItem : stockItemList)
							!{String stockItemTags = stockItem.tags() != null ? String.join("|", stockItem.tags()) : "";}
							<option value="${stockItem.id().toString()}"
									selected="${selectedStockItemIds != null && selectedStockItemIds.contains(stockItem.id())}"
									data-stock-tags="${stockItemTags}">${stockItem.name()}</option>
						@endfor
					@endif
				</select>
			</div>

			<div class="contents">
				@template.stock.htmx.fragments.components.stockSelectionPanel(
					tagLabel = tagLabel,
					stockLabel = stockLabel,
					allLabel = allLabel,
					helperLabel = tagHelperLabel,
					selectedTagSummaryLabel = tagSummaryLabel,
					selectedStockSummaryPattern = tagStockSummaryPattern,
					stockPreviewMorePattern = tagMorePattern,
					stockTagList = stockTagList,
					selectedStockTags = selectedStockTags,
					stockItemList = stockItemList
				)

				<div class="flex justify-end gap-2 xl:self-end">
					<%-- data-activity-refresh: 아직 렌더되지 않은 뷰로 전환할 때 common.js 가 이 버튼을 눌러 재조회한다. --%>
					<button type="button" class="btn btn-sm btn-primary" data-selection-save="true" data-activity-refresh="true"
						hx-get="/stock/htmx/activity-list"
						hx-target="#activityListFragment"
						hx-select="#activityListFragment"
						hx-swap="outerHTML"
						hx-include="#activitySearchForm">${searchLabel}</button>
					<button type="button" class="btn btn-sm text-xs"
						hx-get="/stock/htmx/activity-list"
						hx-target="#activityListFragment"
						hx-select="#activityListFragment"
							hx-swap="outerHTML"
							hx-include="#activitySearchForm"
						data-selection-reset="true">${resetLabel}</button>
				</div>
			</div>
		</div>
		<input type="hidden" name="rangeMode" id="activityRangeModeInput" value="${rangeMode != null ? rangeMode : ""}">
		<input type="hidden" name="startDate" id="activityStartInstantInput" value="${startDate != null ? startDate.toString() : ""}">
		<input type="hidden" name="endDate" id="activityEndInstantInput" value="${endDate != null ? endDate.toString() : ""}">
		<input type="hidden" name="timeZone" id="activityTimeZoneInput" value="${timeZone != null ? timeZone : ""}">
	</form>
	</details>

	<script type="module" src="/js/stock/selectionStorage.js"></script>
	<script type="module" src="/js/stock/tagFilterChips.js"></script>

	<!-- Summary Cards -->
	<div class="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-4">

		<!-- Card 1: 매매 현황 -->
		@template._components.ui.card(body = @`
				<div class="text-xs text-base-content/60 mb-1">${tradeStatusLabel}</div>
				<div class="flex items-baseline gap-1 mb-2">
					<span class="text-xl font-bold text-error amount-value">₩ ${df.format(safeBuy)}</span>
					<span class="text-xs text-base-content/60">${buyLabel}</span>
				</div>
				<div class="flex gap-4 text-xs text-base-content/60">
					<span>${buyLabel} <span class="font-medium text-base-content/70">${countMessage.apply(buyCount)}</span></span>
					<span>${sellLabel} <span class="font-medium text-info amount-value">₩ ${df.format(safeSell)}</span> <span class="text-base-content/70">${countMessage.apply(sellCount)}</span></span>
				</div>
		`)

		<!-- Card 2: 배당 수령 -->
		@template._components.ui.card(body = @`
				<div class="text-xs text-base-content/60 mb-1">${dividendReceivedLabel}</div>
				<div class="flex items-baseline gap-1 mb-2">
					<span class="text-xl font-bold text-dividend amount-value">₩ ${df.format(safeDividend)}</span>
				</div>
				<div class="text-xs text-base-content/60">${countMessage.apply(dividendCount)}</div>
		`)

		<!-- Card 3: 전체 활동 -->
		@template._components.ui.card(body = @`
				<div class="text-xs text-base-content/60 mb-1">${totalActivityLabel}</div>
				<div class="text-xl font-bold">${countMessage.apply(activities != null ? activities.size() : 0)}</div>
				<div class="flex gap-4 text-xs text-base-content/60 mt-1">
					<span>${tradeLabel} <span class="font-medium text-base-content/70">${countMessage.apply(buyCount + sellCount)}</span></span>
					<span>${dividendLabel} <span class="font-medium text-base-content/70">${countMessage.apply(dividendCount)}</span></span>
				</div>
		`)
	</div>

	!{
		// 캘린더 뷰: timeZone 적용 LocalDate 기준 일별 그룹핑 → 활동이 있는 월만 그리드 생성
		java.util.TreeMap<java.time.LocalDate, java.util.List<Activity>> activityByDate = new java.util.TreeMap<>();
		if (activities != null) {
			for (Activity calActivity : activities) {
				if (calActivity.date() == null) continue;
				activityByDate.computeIfAbsent(calActivity.date().atZone(zone).toLocalDate(), k -> new java.util.ArrayList<Activity>()).add(calActivity);
			}
		}
		java.util.List<java.time.YearMonth> activityMonths = activityByDate.keySet().stream().map(java.time.YearMonth::from).distinct().sorted().toList();
		String[] weekdayLabels = MessageUtil.getMessage("common.calendar.weekdays").split(",");
		String calendarTabLabel = MessageUtil.getMessage("stock.activity.view.calendar");
		String timelineTabLabel = MessageUtil.getMessage("stock.activity.view.timeline");
		String listTabLabel = MessageUtil.getMessage("stock.activity.view.list");

		// 월별 집계: 월(YearMonth) → [배당(세후), 실현손익, 매수, 매도] 합 (활동 있는 월만, 기간 순)
		// [2] 매수 · [3] 매도는 월 카드 헤더의 요약에만 쓴다(2026-09-08). 접힌 달 카드가 건수만 보여 줘
		// 한 화면이 빈 막대 여덟 줄로 채워졌는데, 이 합은 이미 여기서 돌고 있었다.
		// [0] 배당(세후) · [1] 실현손익. 예전에는 매수/매도/배당 금액을 한 축에 올렸는데,
		// 성격이 다른 값이라 작은 쪽이 보이지 않았다(실측 2026-09-07: 배당 중앙값이 최대 막대의
		// 0.62% = 170px 차트에서 1px). 이제 성격별로 나눠 각자 축을 준다.
		java.util.TreeMap<java.time.YearMonth, BigDecimal[]> monthlyChartAmounts = new java.util.TreeMap<>();
		for (java.util.Map.Entry<java.time.LocalDate, java.util.List<Activity>> chartDayEntry : activityByDate.entrySet()) {
			BigDecimal[] monthSums = monthlyChartAmounts.computeIfAbsent(java.time.YearMonth.from(chartDayEntry.getKey()), k -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO });
			for (Activity chartActivity : chartDayEntry.getValue()) {
				BigDecimal chartAmount = chartActivity.amount() != null ? chartActivity.amount() : BigDecimal.ZERO;
				if ("TRADE".equals(chartActivity.type())) {
					// 실현손익은 매도에만 붙는다(매수는 null). 요약 카드·연도별 표와 같은 규칙이다.
					if (chartActivity.realizedProfit() != null) { monthSums[1] = monthSums[1].add(chartActivity.realizedProfit()); }
					if ("BUY".equals(chartActivity.tradeType())) { monthSums[2] = monthSums[2].add(chartAmount); }
					else { monthSums[3] = monthSums[3].add(chartAmount); }
				} else { monthSums[0] = monthSums[0].add(chartAmount); }
			}
		}
		// tradeList.jte 의 tradeDataJs 패턴: JS 배열 문자열을 만들어 $unsafe 로 심는다
		java.util.function.Function<String, String> ej = s ->
			s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
		StringBuilder chartLabelSb = new StringBuilder();
		StringBuilder chartDividendSb = new StringBuilder();
		StringBuilder chartRealizedSb = new StringBuilder();
		for (java.util.Map.Entry<java.time.YearMonth, BigDecimal[]> chartMonthEntry : monthlyChartAmounts.entrySet()) {
			if (chartLabelSb.length() > 0) { chartLabelSb.append(","); chartDividendSb.append(","); chartRealizedSb.append(","); }
			chartLabelSb.append("\"").append(chartMonthEntry.getKey().getYear()).append(".").append(String.format("%02d", chartMonthEntry.getKey().getMonthValue())).append("\"");
			chartDividendSb.append(chartMonthEntry.getValue()[0].toPlainString());
			chartRealizedSb.append(chartMonthEntry.getValue()[1].toPlainString());
		}
		String activityChartDataJs = "var activityChartLabels=[" + chartLabelSb + "];"
			+ "var activityChartDividend=[" + chartDividendSb + "];"
			+ "var activityChartRealized=[" + chartRealizedSb + "];"
			+ "var activityChartSeriesLabels=[\"" + ej.apply(dividendLabel) + "\",\"" + ej.apply(realizedProfitLabel) + "\"];";
	}

	<%-- 차트는 활동이 있는 달만 막대로 그린다. 즉 X축이 시간캕이 아니라 빈 달이 통째로 사라진다.
		 실측(전체 기간): 2009-10~2026-08 203개월 중 132개월(65%)이 빠지고, 2010-03 막대 바로 옆에
		 2014-11 막대가 붙는다(실제는 55개월 공백). 막대를 203개로 채우면 읽힐 수 없게 되므로,
		 압축은 그대로 두고 생략된 달 수를 밝힌다. --%>
	!{
		long chartSkippedMonths = 0;
		if (!activityMonths.isEmpty()) {
			java.time.YearMonth chartFirstMonth = activityMonths.get(0);
			java.time.YearMonth chartLastMonth = activityMonths.get(activityMonths.size() - 1);
			long chartSpanMonths = java.time.temporal.ChronoUnit.MONTHS.between(chartFirstMonth, chartLastMonth) + 1;
			chartSkippedMonths = chartSpanMonths - activityMonths.size();
		}
	}
	@if(!activityMonths.isEmpty())
		<div class="rounded-2xl bg-base-100 p-3 shadow-sm mb-4">
			<%-- 성격이 다른 둘을 각자 축으로 그린다. 배당은 매달 꾸준하고 작고, 실현손익은 판 달에만
				 크게 난다 - 한 축에 두면 배당이 1px 이 된다. 부호는 손익 차트에만 뜻이 있다
				 (플러스는 벌었고 마이너스는 잃은 것이라 설명이 필요 없다). --%>
			<div class="mb-1 text-xs font-medium text-base-content/70">${dividendChartTitle}</div>
			<div class="relative w-full" style="height:150px">
				<canvas id="activityDividendChart" role="img" aria-label="${dividendLabel}" class="amount-chart"></canvas>
			</div>
			<div class="mt-3 mb-1 text-xs font-medium text-base-content/70">${realizedChartTitle}</div>
			<div class="relative w-full" style="height:150px">
				<canvas id="activityRealizedChart" role="img" aria-label="${realizedProfitLabel}" class="amount-chart"></canvas>
			</div>
			@if(chartSkippedMonths > 0)
				<p class="mt-2 text-[11px] text-base-content/60" data-chart-skipped-months="${chartSkippedMonths}">
					${java.text.MessageFormat.format(MessageUtil.getMessage("stock.activity.chart.skipped.months"), chartSkippedMonths)}
				</p>
			@endif
		</div>
	@endif

	<!-- View Tabs: 캘린더/타임라인/목록 (클라이언트 사이드 전환, localStorage 유지) -->
	<div role="tablist" class="tabs tabs-box w-full bg-base-200/70 p-1 mb-4" id="activityViewTabs">
		<a role="tab" data-activity-view-tab="calendar" aria-controls="${showCalendarView ? "activityCalendarView" : null}" class="tab flex-1 cursor-pointer text-base-content/60 hover:text-base-content" tabindex="0">${calendarTabLabel}</a>
		<a role="tab" data-activity-view-tab="timeline" aria-controls="${showTimelineView ? "activityTimelineView" : null}" class="tab flex-1 cursor-pointer text-base-content/60 hover:text-base-content" tabindex="0">${timelineTabLabel}</a>
		<a role="tab" data-activity-view-tab="list" aria-controls="${showListView ? "activityListView" : null}" class="tab flex-1 cursor-pointer text-base-content/60 hover:text-base-content" tabindex="0">${listTabLabel}</a>
	</div>

	<!-- Calendar View -->
	@if(showCalendarView)
	<div id="activityCalendarView" role="tabpanel" class="hidden">
		@if(activityMonths.isEmpty())
			@template._components.ui.emptyState(message = noActivityDataLabel, widenRangeAction = !isAllMode)
		@else
			<div class="space-y-3">
				<%-- 기간이 길면 캘린더가 세로로 매우 길어지므로 최신 월만 펼치고 나머지는 접는다 --%>
				!{java.time.YearMonth latestActivityMonth = activityMonths.get(activityMonths.size() - 1);}
				@for(java.time.YearMonth activityMonth : activityMonths)
					!{
						String ymKey = activityMonth.toString();
						java.time.LocalDate monthFirstDay = activityMonth.atDay(1);
						int startPad = monthFirstDay.getDayOfWeek().getValue() % 7;
						int daysInMonth = activityMonth.lengthOfMonth();
						int totalCells = ((startPad + daysInMonth + 6) / 7) * 7;
						String monthTitle = activityMonth.getYear() + "." + String.format("%02d", activityMonth.getMonthValue());
						int monthActivityCount = activityByDate.subMap(monthFirstDay, true, activityMonth.atEndOfMonth(), true).values().stream().mapToInt(java.util.List::size).sum();
					}
					<details class="rounded-2xl bg-base-100 p-3 shadow-sm" open="${activityMonth.equals(latestActivityMonth)}">
						!{BigDecimal[] monthSummary = monthlyChartAmounts.getOrDefault(activityMonth, new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO });}
						<summary class="flex flex-wrap items-baseline gap-x-2 gap-y-1 cursor-pointer select-none tap-target-24">
							<span class="text-sm font-bold">${monthTitle}</span>
							<span class="text-xs text-base-content/60">${countMessage.apply(monthActivityCount)}</span>
							<%-- 접힌 채로도 그 달에 무슨 일이 있었는지 읽히게. 0 인 항목은 적지 않는다. --%>
							<span class="ml-auto flex flex-wrap gap-x-3 text-xs tabular-nums" data-activity-month-summary>
								@if(monthSummary[2].signum() != 0)
									<span><span class="text-base-content/60">${buyLabel}</span> <span class="amount-value">₩${String.format("%,d", StockFormatUtil.displayWon(monthSummary[2]))}</span></span>
								@endif
								@if(monthSummary[3].signum() != 0)
									<span><span class="text-base-content/60">${sellLabel}</span> <span class="amount-value">₩${String.format("%,d", StockFormatUtil.displayWon(monthSummary[3]))}</span></span>
								@endif
								@if(monthSummary[0].signum() != 0)
									<span><span class="text-base-content/60">${dividendLabel}</span> <span class="amount-value text-dividend">₩${String.format("%,d", StockFormatUtil.displayWon(monthSummary[0]))}</span></span>
								@endif
								@if(monthSummary[1].signum() != 0)
									<span><span class="text-base-content/60">${realizedProfitLabel}</span> <span class="amount-value font-semibold ${monthSummary[1].signum() >= 0 ? "text-profit" : "text-loss"}">${monthSummary[1].signum() >= 0 ? "+" : "-"}₩${String.format("%,d", StockFormatUtil.displayWon(monthSummary[1].abs()))}</span></span>
								@endif
							</span>
						</summary>
						<div class="mt-2">
						<!-- 날짜 그리드와 동일한 gap/패딩 + 가운데 정렬로 컬럼 라인을 맞춘다. 토/일은 빨간색 -->
						<div class="grid grid-cols-7 gap-1 text-[11px] text-base-content/60 mb-1 text-center">
							@for(int weekdayIndex = 0; weekdayIndex < weekdayLabels.length; weekdayIndex++)
								<div class="p-1${weekdayIndex == 0 || weekdayIndex == 6 ? " text-error" : ""}">${weekdayLabels[weekdayIndex]}</div>
							@endfor
						</div>
						<div class="grid grid-cols-7 gap-1">
							@for(int cellIndex = 0; cellIndex < totalCells; cellIndex++)
								!{
									java.time.LocalDate cellDate = monthFirstDay.plusDays((long) cellIndex - startPad);
									boolean inMonth = cellIndex >= startPad && cellIndex < startPad + daysInMonth;
									java.util.List<Activity> dayActivities = inMonth ? activityByDate.get(cellDate) : null;
									boolean hasActivity = dayActivities != null && !dayActivities.isEmpty();
									long dayBuyCount = 0;
									long daySellCount = 0;
									long dayDividendCount = 0;
									BigDecimal dayNetAmount = BigDecimal.ZERO;
									if (hasActivity) {
										for (Activity dayActivity : dayActivities) {
											BigDecimal dayAmount = dayActivity.amount() != null ? dayActivity.amount() : BigDecimal.ZERO;
											if ("TRADE".equals(dayActivity.type())) {
												if ("BUY".equals(dayActivity.tradeType())) { dayBuyCount++; dayNetAmount = dayNetAmount.subtract(dayAmount); }
												else { daySellCount++; dayNetAmount = dayNetAmount.add(dayAmount); }
											} else { dayDividendCount++; dayNetAmount = dayNetAmount.add(dayAmount); }
										}
									}
									boolean isToday = inMonth && cellDate.equals(today);
									boolean isWeekend = cellIndex % 7 == 0 || cellIndex % 7 == 6;
									String holidayName = inMonth ? net.luversof.web.gate.util.KoreanHolidays.holidayName(cellDate) : null;
									boolean isRedDay = isWeekend || holidayName != null;
								}
								<%-- 칸 하나에 유틸리티 80자가 실려 전체 기간(2,625칸)에서 210KB 였다. 모양은 main.css 의 .cal-day / .cal-day-act 가 같은 토큰으로 낸다.
								     빠지는 속성(data-cal-*, title)은 줄을 나누면 빈 줄의 탭이 그대로 실리므로 한 줄에 둔다.
								     활동 있는 칸은 클릭으로 그날 상세를 펼치므로 버튼 역할·탭 포커스·aria-expanded 를 준다(수익률 표의 행과 같은 규칙,
								     실측 2026-09-09: 전체 기간 181칸이 키보드로는 열 수 없었다). Enter/Space 는 common.ts 가 받는다. --%>
								<div class="cal-day ${isToday ? "border-primary" : "border-transparent"}${!inMonth ? " cal-day-out" : ""}${hasActivity ? " cal-day-act" : ""}" data-cal-date="${hasActivity ? cellDate.toString() : null}" data-cal-month="${hasActivity ? ymKey : null}" role="${hasActivity ? "button" : null}" tabindex="${hasActivity ? "0" : null}" aria-expanded="${hasActivity ? "false" : null}" aria-controls="${hasActivity ? "cal-detail-" + cellDate.toString() : null}" title="${holidayName}">
									<div class="cal-day-num${isToday ? " text-primary" : (isRedDay ? " text-error" : "")}">${cellDate.getDayOfMonth()}</div>
									@if(holidayName != null)
										<div class="text-[9px] leading-tight text-error/80 truncate" title="${holidayName}">${holidayName}</div>
									@endif
									@if(hasActivity)
										<div class="flex flex-wrap gap-x-1 justify-center leading-tight">
											@if(dayBuyCount > 0)<span class="text-profit">●${dayBuyCount}</span>@endif
											@if(daySellCount > 0)<span class="text-loss">●${daySellCount}</span>@endif
											@if(dayDividendCount > 0)<span class="text-dividend">●${dayDividendCount}</span>@endif
										</div>
										<div class="amount-value text-[10px] text-base-content/60 truncate" title="₩ ${df.format(dayNetAmount)}">₩ ${df.format(dayNetAmount)}</div>
									@endif
								</div>
							@endfor
						</div>
						<!-- 날짜 셀 클릭 시 해당 날짜 상세 목록 표시 (미리 렌더된 hidden 블록을 JS로 토글) -->
						<div class="selection-reveal mt-2" data-cal-panel="${ymKey}">
							<div>
								@for(java.util.Map.Entry<java.time.LocalDate, java.util.List<Activity>> dayEntry : activityByDate.subMap(monthFirstDay, true, activityMonth.atEndOfMonth(), true).entrySet())
									<div class="hidden rounded-xl bg-base-200/50 p-2 mb-1" id="cal-detail-${dayEntry.getKey().toString()}" data-cal-detail="${dayEntry.getKey().toString()}">
										<div class="text-xs font-bold mb-1">${dayEntry.getKey().toString()}</div>
										<div class="space-y-1">
											@for(Activity dayDetailActivity : dayEntry.getValue())
												!{
													boolean detailIsTrade = "TRADE".equals(dayDetailActivity.type());
													boolean detailIsBuy = detailIsTrade && "BUY".equals(dayDetailActivity.tradeType());
													String detailAmountColor = !detailIsTrade ? "text-dividend" : (detailIsBuy ? "text-profit" : "text-loss");
												}
												<div class="flex items-center gap-2 text-xs flex-wrap">
													@if(detailIsTrade)
														<span class="badge badge-sm badge-outline ${detailIsBuy ? "badge-error" : "badge-info"}">${detailIsBuy ? buyLabel : sellLabel}</span>
													@else
														<span class="badge badge-sm badge-outline badge-success">${dividendLabel}</span>
													@endif
													<a href="/stock/item?stockItemId=${dayDetailActivity.stockItemId() != null ? dayDetailActivity.stockItemId().toString() : ""}" class="link link-hover font-bold">${dayDetailActivity.stockItemName()}</a>
													<span class="text-base-content/60 whitespace-nowrap">
														@if(detailIsTrade)
															<span class="amount-value">${dayDetailActivity.quantity()}</span> ${sharesLabel}
														@else
															${dayDetailActivity.description()}
														@endif
													</span>
													<span class="ml-auto font-mono font-bold amount-value ${detailAmountColor}">₩ ${String.format("%,d", StockFormatUtil.displayWon(dayDetailActivity.amount()))}</span>
												</div>
											@endfor
										</div>
									</div>
								@endfor
							</div>
						</div>
						</div>
					</details>
				@endfor
			</div>
		@endif
	</div>
	@endif

	<!-- Timeline View: 날짜 내림차순 세로 타임라인 -->
	@if(showTimelineView)
	<div id="activityTimelineView" role="tabpanel" class="hidden">
		@if(activityByDate.isEmpty())
			@template._components.ui.emptyState(message = noActivityDataLabel, widenRangeAction = !isAllMode)
		@else
			<div class="rounded-2xl bg-base-100 p-4 shadow-sm">
				<%-- 타임라인은 날짜 묶음이 쭉 이어질 뿐이라 5.5화면을 굴려도 지금이 몇 월인지 알 수 없었다
				     (실측 2026-09-10: 이 화면의 제목 수 0, 날짜 묶음 42개). 달이 바뀌는 자리에 구역 제목을 넣는다. --%>
				!{ java.time.YearMonth tlPrevMonth = null; }
				<div class="relative border-l-2 border-base-300 ml-1.5 pl-6 space-y-6">
					@for(java.util.Map.Entry<java.time.LocalDate, java.util.List<Activity>> tlEntry : activityByDate.descendingMap().entrySet())
						!{
							java.time.LocalDate tlDate = tlEntry.getKey();
							java.util.List<Activity> tlActivities = tlEntry.getValue();
							long tlBuyCount = 0;
							long tlSellCount = 0;
							long tlDividendCount = 0;
							BigDecimal tlNetAmount = BigDecimal.ZERO;
							for (Activity tlActivity : tlActivities) {
								BigDecimal tlAmount = tlActivity.amount() != null ? tlActivity.amount() : BigDecimal.ZERO;
								if ("TRADE".equals(tlActivity.type())) {
									if ("BUY".equals(tlActivity.tradeType())) { tlBuyCount++; tlNetAmount = tlNetAmount.subtract(tlAmount); }
									else { tlSellCount++; tlNetAmount = tlNetAmount.add(tlAmount); }
								} else { tlDividendCount++; tlNetAmount = tlNetAmount.add(tlAmount); }
							}
							boolean tlNetPositive = tlNetAmount.compareTo(BigDecimal.ZERO) >= 0;
							String tlWeekday = weekdayLabels[tlDate.getDayOfWeek().getValue() % 7];
							String tlDateTitle = tlDate.getMonthValue() + "/" + tlDate.getDayOfMonth() + " (" + tlWeekday + ")";
							StringBuilder tlSummarySb = new StringBuilder();
							if (tlBuyCount > 0) { tlSummarySb.append(buyLabel).append(" ").append(countMessage.apply(tlBuyCount)); }
							if (tlSellCount > 0) { if (tlSummarySb.length() > 0) { tlSummarySb.append(" · "); } tlSummarySb.append(sellLabel).append(" ").append(countMessage.apply(tlSellCount)); }
							if (tlDividendCount > 0) { if (tlSummarySb.length() > 0) { tlSummarySb.append(" · "); } tlSummarySb.append(dividendLabel).append(" ").append(countMessage.apply(tlDividendCount)); }
							String tlSummaryText = tlSummarySb.toString();
						}
						!{
							java.time.YearMonth tlMonth = java.time.YearMonth.from(tlDate);
							boolean tlMonthChanged = !tlMonth.equals(tlPrevMonth);
							tlPrevMonth = tlMonth;
						}
						@if(tlMonthChanged)
							<h2 class="text-sm font-bold -ml-[31px] pl-[31px] pt-1" data-page-section="${tlMonth.toString()}">${tlMonth.toString()}</h2>
						@endif
						<div class="relative">
							<span class="absolute -left-[31px] top-1 w-3 h-3 rounded-full bg-primary border-2 border-base-100"></span>
							<div class="flex flex-wrap items-baseline gap-x-2 gap-y-0.5 mb-1">
								<span class="text-sm font-bold">${tlDateTitle}</span>
								<span class="text-xs text-base-content/60">${tlSummaryText}</span>
								<span class="text-xs font-mono font-bold amount-value ${tlNetPositive ? "text-profit" : "text-loss"}">${tlNetPositive ? "+" : "-"}₩ ${df.format(tlNetAmount.abs())}</span>
							</div>
							<div class="space-y-1">
								@for(Activity tlRowActivity : tlActivities)
									!{
										boolean tlIsTrade = "TRADE".equals(tlRowActivity.type());
										boolean tlIsBuy = tlIsTrade && "BUY".equals(tlRowActivity.tradeType());
										String tlAmountColor = !tlIsTrade ? "text-dividend" : (tlIsBuy ? "text-profit" : "text-loss");
									}
									<div class="flex items-center gap-2 text-xs flex-wrap">
										@if(tlIsTrade)
											<span class="badge badge-sm badge-outline ${tlIsBuy ? "badge-error" : "badge-info"}">${tlIsBuy ? buyLabel : sellLabel}</span>
										@else
											<span class="badge badge-sm badge-outline badge-success">${dividendLabel}</span>
										@endif
										<a href="/stock/item?stockItemId=${tlRowActivity.stockItemId() != null ? tlRowActivity.stockItemId().toString() : ""}" class="link link-hover font-bold">${tlRowActivity.stockItemName()}</a>
										<span class="text-base-content/60 whitespace-nowrap">
											@if(tlIsTrade)
												<span class="amount-value">${tlRowActivity.quantity()}</span> ${sharesLabel}
											@else
												${tlRowActivity.description()}
											@endif
										</span>
										<span class="ml-auto font-mono font-bold amount-value ${tlAmountColor}">₩ ${String.format("%,d", StockFormatUtil.displayWon(tlRowActivity.amount()))}</span>
									</div>
								@endfor
							</div>
						</div>
					@endfor
				</div>
			</div>
		@endif
	</div>
	@endif

	<!-- List View (기존 테이블) -->
	@if(showListView)
	<div id="activityListView" role="tabpanel" class="hidden">
		<div class="card-body">
			@if(activities == null || activities.isEmpty())
				@template._components.ui.emptyState(message = noActivityDataLabel, widenRangeAction = !isAllMode)
			@else
				<div class="overflow-x-auto">
					<table class="table w-full table-sm table-pin-rows pin-top-nav" aria-label="${listTabLabel}">
						<thead>
							<tr class="bg-base-200">
								<th scope="col">${dateLabel}</th>
								<th scope="col">${typeLabel}</th>
								<th scope="col">${stockNameLabel}</th>
								<th scope="col">${detailLabel}</th>
								<th scope="col" class="text-right">${amountLabel}</th>
							</tr>
						</thead>
						<tbody>
							@for(Activity activity : activities)
								<tr>
									<td class="whitespace-nowrap">
										${activity.date() != null ? activity.date().atZone(zone).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : ""}
									</td>
									<td>
										@if("TRADE".equals(activity.type()))
											<span class="badge badge-sm badge-outline ${"BUY".equals(activity.tradeType()) ? "badge-error" : "badge-info"}">${"BUY".equals(activity.tradeType()) ? buyLabel : sellLabel}</span>
										@else
											<span class="badge badge-sm badge-outline badge-success">${dividendLabel}</span>
										@endif
									</td>
									<td class="font-bold"><a href="/stock/item?stockItemId=${activity.stockItemId() != null ? activity.stockItemId().toString() : ""}" class="link link-hover">${activity.stockItemName()}</a></td>
									<td>
										<div class="flex items-center gap-2 whitespace-nowrap">
											<span>
												@if("TRADE".equals(activity.type()))
													<span class="amount-value">${activity.quantity()}</span> ${sharesLabel}
												@else
													${activity.description()}
												@endif
											</span>

											<!-- Account Badges (Max 2 + Tooltip) -->
											<%-- 계좌는 id 로 들고 다니고 표시 이름은 accountNames(id -> 이름) 에서 찾는다.
												 예전에는 이름 목록만 있어 화면에서 이름으로 id 를 되찾았다. --%>
											@if(activity.accountIds() != null && !activity.accountIds().isEmpty())
												<span class="flex items-center gap-1">
													@for(int i = 0; i < Math.min(2, activity.accountIds().size()); i++)
														<a href="/stock/account?accountId=${activity.accountIds().get(i) != null ? activity.accountIds().get(i).toString() : ""}" class="badge badge-xs badge-outline opacity-70 link link-hover">${accountNames != null ? accountNames.getOrDefault(activity.accountIds().get(i), "") : ""}</a>
													@endfor

													@if(activity.accountIds().size() > 2)
														!{java.util.List<String> allAccountLabels = activity.accountIds().stream().map(id -> accountNames != null ? accountNames.getOrDefault(id, "") : "").filter(n -> !n.isEmpty()).toList();}
														<div class="tooltip tooltip-bottom" role="note" tabindex="0" aria-label="${String.join(", ", allAccountLabels)}" data-tip="${String.join(", ", allAccountLabels)}">
															<span class="badge badge-xs badge-outline opacity-70 cursor-help">+${activity.accountIds().size() - 2}</span>
														</div>
													@endif
												</span>
											@endif
										</div>
									</td>
									<td class="text-right font-mono font-bold amount-value ${"DIVIDEND".equals(activity.type()) ? "text-dividend" : ""}">
										₩ ${String.format("%,d", StockFormatUtil.displayWon(activity.amount()))}
									</td>
								</tr>
							@endfor
						</tbody>
					</table>
				</div>
			@endif
		</div>
	</div>
	@endif

	<script nonce="${CspNonceHolder.getNonce()}">
	(function() {
		const activityPicker = DateRangePicker.create({
			formId:      'activitySearchForm',
			startId:     'activityStartDateInput',
			endId:       'activityEndDateInput',
			instantStartId: 'activityStartInstantInput',
			instantEndId:   'activityEndInstantInput',
			timeZoneId:     'activityTimeZoneInput',
			rangeModeId: 'activityRangeModeInput',
			btnClass:    'date-range-btn',
			minDate:     '${dataFirstDate != null ? dataFirstDate : ""}',
			globalKey:   'globalDateRange',
			rootSelector: '#activityListFragment'
		});
		window.activityPicker = activityPicker;
		// 기본값: 페이지 진입 시 기간 미지정이면 '이번달(mtd)'을 선택 (한번만 적용하도록 세션 가드)
		try {
			if (window.activityPicker && typeof window.activityPicker.getState === 'function') {
				var __s = window.activityPicker.getState();
				if ((!__s || !__s.mode) && (!__s || !__s.start) && (!__s || !__s.end)) {
					var __key = 'dateRangeDefaultApplied:activityPicker';
					try {
						if (!sessionStorage.getItem(__key)) {
						var _btn = document.querySelector('#activityListFragment .date-range-btn');
						window.activityPicker.set('mtd', _btn || null);
						sessionStorage.setItem(__key, '1');
					}
					} catch(e) { window.activityPicker.set('mtd', null); }
				}
			}
		} catch(e) {}
	})();
	</script>

	<!-- 뷰 전환 탭/캘린더 상세 토글은 common.js 의 문서 위임 + htmx:afterSettle 복원으로 처리된다 -->

	<script nonce="${CspNonceHolder.getNonce()}">
	(function() {
		$unsafe{activityChartDataJs}

		// 월별 차트 두 개 — 배당(매달 꾸준함)과 실현손익(판 달에만)은 크기대가 달라 각자 축을 쓴다.
		// 한 축에 올렸을 때 배당은 최대 막대의 0.62%(약 1px)라 사실상 보이지 않았다(실측 2026-09-07).
		// Chart.js 는 stockLayout 에서 1회 로드된다. htmx 재스왑 시 이전 인스턴스를 destroy 한다.
		// 스왑 정착 후 실행(setTimeout 0): 스왑 중엔 이전/새 canvas 가 공존할 수 있다.
		setTimeout(function() {
		if (activityChartLabels.length === 0) return;
		window.ensureStockCharts(function() {
		if (typeof Chart === 'undefined') return;

		function formatSignedCompact(v) {
			var n = Number(v) || 0;
			var abs = Math.abs(n);
			var sign = n < 0 ? '-' : '';
			// 로케일과 무관하게 억/만 을 붙이면 영어 화면에도 한글 단위가 나온다(실측).
			var ko = (document.documentElement.lang || '').toLowerCase().indexOf('ko') === 0;
			var trim = function (x) { var t = x.toFixed(1); return t.slice(-2) === '.0' ? t.slice(0, -2) : t; };
			if (ko) {
				if (abs >= 100000000) return sign + (abs / 100000000).toFixed(0) + '억';
				if (abs >= 10000) return sign + (abs / 10000).toFixed(0) + '만';
				return n.toLocaleString();
			}
			if (abs >= 1000000000) return sign + trim(abs / 1000000000) + 'B';
			if (abs >= 1000000) return sign + trim(abs / 1000000) + 'M';
			if (abs >= 1000) return sign + trim(abs / 1000) + 'K';
			return n.toLocaleString();
		}

		var gridColor = 'rgba(128,128,128,0.1)';

		function drawActivityBar(canvasId, instKey, label, data, colorFn, signed) {
			var canvas = document.getElementById(canvasId);
			if (!canvas) return;
			if (window[instKey]) {
				try { window[instKey].destroy(); } catch (e) {}
				window[instKey] = null;
			}
			window[instKey] = new Chart(canvas, {
				type: 'bar',
				data: {
					labels: activityChartLabels,
					datasets: [{
						label: label,
						data: data,
						backgroundColor: data.map(colorFn),
						borderRadius: 3,
						maxBarThickness: 36
					}]
				},
				options: {
					responsive: true,
					maintainAspectRatio: false,
					interaction: { mode: 'index', intersect: false },
					plugins: {
						legend: { display: false },
						tooltip: {
							callbacks: {
								label: function(ctx) {
									var raw = Number(ctx.parsed.y) || 0;
									// 손익 차트에서만 부호가 뜻이다. 배당은 늘 들어온 돈이라 부호를 붙이지 않는다.
									var prefix = signed ? (raw >= 0 ? '+' : '-') : '';
									return ctx.dataset.label + ': ' + prefix + '₩' + Math.abs(raw).toLocaleString();
								}
							}
						}
					},
					scales: {
						x: { grid: { color: gridColor }, ticks: { font: { size: 10 } } },
						y: { grid: { color: gridColor }, ticks: { font: { size: 10 }, callback: formatSignedCompact } }
					}
				}
			});
		}

		// 배당: 늘 들어오는 돈이라 한 가지 색.
		drawActivityBar('activityDividendChart', 'activityDividendChartInst',
			activityChartSeriesLabels[0], activityChartDividend,
			function() { return 'rgba(34,197,94,0.7)'; }, false);

		// 실현손익: 부호가 곧 뜻이다 - 이익은 붉게, 손실은 푸르게(이 화면의 손익 색 관례).
		drawActivityBar('activityRealizedChart', 'activityRealizedChartInst',
			activityChartSeriesLabels[1], activityChartRealized,
			function(v) { return Number(v) < 0 ? 'rgba(59,130,246,0.7)' : 'rgba(239,68,68,0.7)'; }, true);
		});
		}, 0);
	})();
	</script>
</div>
