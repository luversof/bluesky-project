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

@param List<Activity> activities
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
@param long buyCount = 0
@param long sellCount = 0
@param BigDecimal buyAmount = BigDecimal.ZERO
@param BigDecimal sellAmount = BigDecimal.ZERO
@param long dividendCount = 0
@param BigDecimal dividendAmount = BigDecimal.ZERO
@param java.util.Map<String, UUID> activityStockIdByName = java.util.Map.of()
@param java.util.Map<String, UUID> activityAccountIdByName = java.util.Map.of()

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

<div id="activityListFragment">

    !{
        boolean isAllMode = "all".equals(rangeMode);
        java.time.ZoneId zone = (timeZone != null && !timeZone.isEmpty()) ? java.time.ZoneId.of(timeZone) : java.time.ZoneId.systemDefault();
        java.time.LocalDate today = java.time.LocalDate.now(zone);
        java.time.LocalDate startLocal = startDate != null ? startDate.atZone(zone).toLocalDate() : null;
        java.time.LocalDate endLocal = endDate != null ? endDate.atZone(zone).toLocalDate() : null;
        boolean canNext = !isAllMode && endLocal != null && endLocal.isBefore(today);
        boolean canPrev = !isAllMode && startLocal != null && dataFirstDate != null && !dataFirstDate.isEmpty()
            && startLocal.isAfter(java.time.LocalDate.parse(dataFirstDate));
    }
    @template.stock.htmx.fragments.dateRangeNavBar(
        pickerName = "activityPicker",
        btnClass = "date-range-btn",
        canPrev = canPrev,
        canNext = canNext,
        rangeMode = rangeMode,
        startDate = startLocal,
        endDate = endLocal
    )

    <!-- Filter Form (collapsible) -->
    <details class="mb-4" open data-detail-filter>
        <summary class="flex items-center gap-2 cursor-pointer select-none text-sm font-medium text-base-content/60 hover:text-base-content py-1 px-2 rounded hover:bg-base-200 w-fit transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2a1 1 0 01-.293.707L13 13.414V19a1 1 0 01-.553.894l-4 2A1 1 0 017 21v-7.586L3.293 6.707A1 1 0 013 6V4z" /></svg>
            ${advancedSearchLabel}
            @template.stock.htmx.fragments.components.filterBadge(
                startDate = startDate,
                endDate = endDate
            )
        </summary>
    <form id="activitySearchForm" class="mt-2 mb-2 rounded-2xl bg-base-100 p-3 shadow-sm"
          hx-get="/stock/htmx/activity-list"
          hx-target="#activityListFragment"
          hx-select="#activityListFragment"
          hx-swap="outerHTML"
          hx-trigger="submit">
        <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-[minmax(0,18rem)_minmax(0,22rem)_minmax(0,1fr)_minmax(0,19rem)_minmax(0,auto)] xl:items-start">
            <div class="form-control w-full">
                <label class="label p-1" id="filterPeriodLabel"><span class="label-text text-xs">${periodLabel}</span></label>
                <div class="join w-full" role="group" aria-labelledby="filterPeriodLabel">
                    <input type="date" name="activityUiStartDate" id="activityStartDateInput" class="input input-sm input-bordered join-item w-1/2" value="${startDate != null ? startDate.atZone(zone).toLocalDate().toString() : ""}">
                    <input type="date" name="activityUiEndDate" id="activityEndDateInput" class="input input-sm input-bordered join-item w-1/2" value="${endDate != null ? endDate.atZone(zone).toLocalDate().minusDays(1).toString() : ""}">
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
                    <button type="button" class="btn btn-sm btn-primary" data-selection-save="true"
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
                <div class="text-xs text-base-content/50 mb-1">${tradeStatusLabel}</div>
                <div class="flex items-baseline gap-1 mb-2">
                    <span class="text-xl font-bold text-error amount-value">₩ ${df.format(safeBuy)}</span>
                    <span class="text-xs text-base-content/40">${buyLabel}</span>
                </div>
                <div class="flex gap-4 text-xs text-base-content/50">
                    <span>${buyLabel} <span class="font-medium text-base-content/70">${countMessage.apply(buyCount)}</span></span>
                    <span>${sellLabel} <span class="font-medium text-info amount-value">₩ ${df.format(safeSell)}</span> <span class="text-base-content/70">${countMessage.apply(sellCount)}</span></span>
                </div>
        `)

        <!-- Card 2: 배당 수령 -->
        @template._components.ui.card(body = @`
                <div class="text-xs text-base-content/50 mb-1">${dividendReceivedLabel}</div>
                <div class="flex items-baseline gap-1 mb-2">
                    <span class="text-xl font-bold text-dividend amount-value">₩ ${df.format(safeDividend)}</span>
                </div>
                <div class="text-xs text-base-content/50">${countMessage.apply(dividendCount)}</div>
        `)

        <!-- Card 3: 전체 활동 -->
        @template._components.ui.card(body = @`
                <div class="text-xs text-base-content/50 mb-1">${totalActivityLabel}</div>
                <div class="text-xl font-bold">${countMessage.apply(activities != null ? activities.size() : 0)}</div>
                <div class="flex gap-4 text-xs text-base-content/50 mt-1">
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

        // 월별 활동 스택 바 차트: 월(YearMonth) → [매수, 매도, 배당] 금액 합 (활동 있는 월만, 기간 순)
        java.util.TreeMap<java.time.YearMonth, BigDecimal[]> monthlyChartAmounts = new java.util.TreeMap<>();
        for (java.util.Map.Entry<java.time.LocalDate, java.util.List<Activity>> chartDayEntry : activityByDate.entrySet()) {
            BigDecimal[] monthSums = monthlyChartAmounts.computeIfAbsent(java.time.YearMonth.from(chartDayEntry.getKey()), k -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO });
            for (Activity chartActivity : chartDayEntry.getValue()) {
                BigDecimal chartAmount = chartActivity.amount() != null ? chartActivity.amount() : BigDecimal.ZERO;
                if ("TRADE".equals(chartActivity.type())) {
                    if ("BUY".equals(chartActivity.tradeType())) { monthSums[0] = monthSums[0].add(chartAmount); }
                    else { monthSums[1] = monthSums[1].add(chartAmount); }
                } else { monthSums[2] = monthSums[2].add(chartAmount); }
            }
        }
        // tradeList.jte 의 tradeDataJs 패턴: JS 배열 문자열을 만들어 $unsafe 로 심는다
        java.util.function.Function<String, String> ej = s ->
            s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
        StringBuilder chartLabelSb = new StringBuilder();
        StringBuilder chartBuySb = new StringBuilder();
        StringBuilder chartSellSb = new StringBuilder();
        StringBuilder chartDividendSb = new StringBuilder();
        for (java.util.Map.Entry<java.time.YearMonth, BigDecimal[]> chartMonthEntry : monthlyChartAmounts.entrySet()) {
            if (chartLabelSb.length() > 0) { chartLabelSb.append(","); chartBuySb.append(","); chartSellSb.append(","); chartDividendSb.append(","); }
            chartLabelSb.append("\"").append(chartMonthEntry.getKey().getYear()).append(".").append(String.format("%02d", chartMonthEntry.getKey().getMonthValue())).append("\"");
            chartBuySb.append(chartMonthEntry.getValue()[0].toPlainString());
            chartSellSb.append(chartMonthEntry.getValue()[1].toPlainString());
            chartDividendSb.append(chartMonthEntry.getValue()[2].toPlainString());
        }
        String activityChartDataJs = "var activityChartLabels=[" + chartLabelSb + "];"
            + "var activityChartBuy=[" + chartBuySb + "];"
            + "var activityChartSell=[" + chartSellSb + "];"
            + "var activityChartDividend=[" + chartDividendSb + "];"
            + "var activityChartSeriesLabels=[\"" + ej.apply(buyLabel) + "\",\"" + ej.apply(sellLabel) + "\",\"" + ej.apply(dividendLabel) + "\",\"" + ej.apply(MessageUtil.getMessage("stock.activity.chart.net.flow")) + "\"];";
    }

    <!-- 월별 활동 스택 바 차트 (모든 뷰 공통, 활동 없으면 미렌더) -->
    @if(!activityMonths.isEmpty())
        <div class="rounded-2xl bg-base-100 p-3 shadow-sm mb-4">
            <div class="relative w-full" style="height:170px">
                <canvas id="activityMonthlyChart" class="amount-chart"></canvas>
            </div>
        </div>
    @endif

    <!-- View Tabs: 캘린더/타임라인/목록 (클라이언트 사이드 전환, localStorage 유지) -->
    <div role="tablist" class="tabs tabs-box w-full bg-base-200/70 p-1 mb-4" id="activityViewTabs">
        <a role="tab" data-activity-view-tab="calendar" class="tab flex-1 cursor-pointer text-base-content/60 hover:text-base-content">${calendarTabLabel}</a>
        <a role="tab" data-activity-view-tab="timeline" class="tab flex-1 cursor-pointer text-base-content/60 hover:text-base-content">${timelineTabLabel}</a>
        <a role="tab" data-activity-view-tab="list" class="tab flex-1 cursor-pointer text-base-content/60 hover:text-base-content">${listTabLabel}</a>
    </div>

    <!-- Calendar View -->
    <div id="activityCalendarView" class="hidden">
        @if(activityMonths.isEmpty())
            @template._components.ui.emptyState(message = noActivityDataLabel)
        @else
            <div class="space-y-6">
                @for(java.time.YearMonth activityMonth : activityMonths)
                    !{
                        String ymKey = activityMonth.toString();
                        java.time.LocalDate monthFirstDay = activityMonth.atDay(1);
                        int startPad = monthFirstDay.getDayOfWeek().getValue() % 7;
                        int daysInMonth = activityMonth.lengthOfMonth();
                        int totalCells = ((startPad + daysInMonth + 6) / 7) * 7;
                        String monthTitle = activityMonth.getYear() + "." + String.format("%02d", activityMonth.getMonthValue());
                    }
                    <div class="rounded-2xl bg-base-100 p-3 shadow-sm">
                        <div class="text-sm font-bold mb-2">${monthTitle}</div>
                        <!-- 날짜 그리드와 동일한 gap/패딩 + 가운데 정렬로 컬럼 라인을 맞춘다. 토/일은 빨간색 -->
                        <div class="grid grid-cols-7 gap-1 text-[11px] text-base-content/50 mb-1 text-center">
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
                                <div class="min-h-14 rounded-lg p-1 text-[11px] text-center border ${isToday ? "border-primary" : "border-transparent"}${!inMonth ? " opacity-30" : ""}${hasActivity ? " cursor-pointer hover:bg-base-200 transition-colors duration-300 hover:duration-75" : ""}"
                                     data-cal-date="${hasActivity ? cellDate.toString() : null}"
                                     data-cal-month="${hasActivity ? ymKey : null}"
                                     title="${holidayName}">
                                    <div class="font-medium ${isToday ? "text-primary" : (isRedDay ? "text-error" : "text-base-content/70")}">${cellDate.getDayOfMonth()}</div>
                                    @if(holidayName != null)
                                        <div class="text-[9px] leading-tight text-error/80 truncate">${holidayName}</div>
                                    @endif
                                    @if(hasActivity)
                                        <div class="flex flex-wrap gap-x-1 justify-center leading-tight">
                                            @if(dayBuyCount > 0)<span class="text-profit">●${dayBuyCount}</span>@endif
                                            @if(daySellCount > 0)<span class="text-loss">●${daySellCount}</span>@endif
                                            @if(dayDividendCount > 0)<span class="text-dividend">●${dayDividendCount}</span>@endif
                                        </div>
                                        <div class="amount-value text-[10px] text-base-content/60 truncate">₩ ${df.format(dayNetAmount)}</div>
                                    @endif
                                </div>
                            @endfor
                        </div>
                        <!-- 날짜 셀 클릭 시 해당 날짜 상세 목록 표시 (미리 렌더된 hidden 블록을 JS로 토글) -->
                        <div class="selection-reveal mt-2" data-cal-panel="${ymKey}">
                            <div>
                                @for(java.util.Map.Entry<java.time.LocalDate, java.util.List<Activity>> dayEntry : activityByDate.subMap(monthFirstDay, true, activityMonth.atEndOfMonth(), true).entrySet())
                                    <div class="hidden rounded-xl bg-base-200/50 p-2 mb-1" data-cal-detail="${dayEntry.getKey().toString()}">
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
                                                    <a href="/stock/item?stockItemId=${activityStockIdByName.get(dayDetailActivity.stockItemName()) != null ? activityStockIdByName.get(dayDetailActivity.stockItemName()).toString() : ""}" class="link link-hover font-bold">${dayDetailActivity.stockItemName()}</a>
                                                    <span class="text-base-content/60 whitespace-nowrap">
                                                        @if(detailIsTrade)
                                                            <span class="amount-value">${dayDetailActivity.quantity()}</span> ${sharesLabel}
                                                        @else
                                                            ${dayDetailActivity.description()}
                                                        @endif
                                                    </span>
                                                    <span class="ml-auto font-mono font-bold amount-value ${detailAmountColor}">₩ ${String.format("%,d", dayDetailActivity.amount() != null ? dayDetailActivity.amount().longValue() : 0)}</span>
                                                </div>
                                            @endfor
                                        </div>
                                    </div>
                                @endfor
                            </div>
                        </div>
                    </div>
                @endfor
            </div>
        @endif
    </div>

    <!-- Timeline View: 날짜 내림차순 세로 타임라인 -->
    <div id="activityTimelineView" class="hidden">
        @if(activityByDate.isEmpty())
            @template._components.ui.emptyState(message = noActivityDataLabel)
        @else
            <div class="rounded-2xl bg-base-100 p-4 shadow-sm">
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
                        <div class="relative">
                            <span class="absolute -left-[31px] top-1 w-3 h-3 rounded-full bg-primary border-2 border-base-100"></span>
                            <div class="flex flex-wrap items-baseline gap-x-2 gap-y-0.5 mb-1">
                                <span class="text-sm font-bold">${tlDateTitle}</span>
                                <span class="text-xs text-base-content/50">${tlSummaryText}</span>
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
                                        <a href="/stock/item?stockItemId=${activityStockIdByName.get(tlRowActivity.stockItemName()) != null ? activityStockIdByName.get(tlRowActivity.stockItemName()).toString() : ""}" class="link link-hover font-bold">${tlRowActivity.stockItemName()}</a>
                                        <span class="text-base-content/60 whitespace-nowrap">
                                            @if(tlIsTrade)
                                                <span class="amount-value">${tlRowActivity.quantity()}</span> ${sharesLabel}
                                            @else
                                                ${tlRowActivity.description()}
                                            @endif
                                        </span>
                                        <span class="ml-auto font-mono font-bold amount-value ${tlAmountColor}">₩ ${String.format("%,d", tlRowActivity.amount() != null ? tlRowActivity.amount().longValue() : 0)}</span>
                                    </div>
                                @endfor
                            </div>
                        </div>
                    @endfor
                </div>
            </div>
        @endif
    </div>

    <!-- List View (기존 테이블) -->
    <div id="activityListView" class="hidden">
        <div class="card-body">
            @if(activities == null || activities.isEmpty())
                @template._components.ui.emptyState(message = noActivityDataLabel)
            @else
                <div class="max-lg:overflow-x-auto">
                    <table class="table w-full table-sm table-pin-rows pin-top-nav">
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
                                        ${activity.date() != null ? activity.date().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : ""}
                                    </td>
                                    <td>
                                        @if("TRADE".equals(activity.type()))
                                            <span class="badge badge-sm badge-outline ${"BUY".equals(activity.tradeType()) ? "badge-error" : "badge-info"}">${"BUY".equals(activity.tradeType()) ? buyLabel : sellLabel}</span>
                                        @else
                                            <span class="badge badge-sm badge-outline badge-success">${dividendLabel}</span>
                                        @endif
                                    </td>
                                    <td class="font-bold"><a href="/stock/item?stockItemId=${activityStockIdByName.get(activity.stockItemName()) != null ? activityStockIdByName.get(activity.stockItemName()).toString() : ""}" class="link link-hover">${activity.stockItemName()}</a></td>
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
                                            @if(activity.accountNames() != null && !activity.accountNames().isEmpty())
                                                <span class="flex items-center gap-1">
                                                    @for(int i = 0; i < Math.min(2, activity.accountNames().size()); i++)
                                                        <a href="/stock/account?accountId=${activityAccountIdByName.get(activity.accountNames().get(i)) != null ? activityAccountIdByName.get(activity.accountNames().get(i)).toString() : ""}" class="badge badge-xs badge-outline opacity-70 link link-hover">${activity.accountNames().get(i)}</a>
                                                    @endfor

                                                    @if(activity.accountNames().size() > 2)
                                                        <div class="tooltip tooltip-right" data-tip="${String.join(", ", activity.accountNames())}">
                                                            <span class="badge badge-xs badge-outline opacity-70 cursor-help">+${activity.accountNames().size() - 2}</span>
                                                        </div>
                                                    @endif
                                                </span>
                                            @endif
                                        </div>
                                    </td>
                                    <td class="text-right font-mono font-bold amount-value ${"DIVIDEND".equals(activity.type()) ? "text-dividend" : ""}">
                                        ₩ ${String.format("%,d", activity.amount() != null ? activity.amount().longValue() : 0)}
                                    </td>
                                </tr>
                            @endfor
                        </tbody>
                    </table>
                </div>
            @endif
        </div>
    </div>

    <script>
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

    <script>
    (function() {
        $unsafe{activityChartDataJs}

        // 월별 활동 스택 바 차트 — Chart.js 는 stockLayout 에서 1회 로드됨(tradeList.jte 와 동일 패턴).
        // htmx 재스왑 시 이전 인스턴스를 destroy 하여 중복 생성 방지.
        // 스왑 정착 후 실행(setTimeout 0): 스왑 중엔 이전/새 canvas 가 공존할 수 있다.
        setTimeout(function() {
        var chartCanvas = document.getElementById('activityMonthlyChart');
        if (!chartCanvas || typeof Chart === 'undefined' || activityChartLabels.length === 0) return;
        if (window.activityMonthlyChartInst) {
            try { window.activityMonthlyChartInst.destroy(); } catch (e) {}
            window.activityMonthlyChartInst = null;
        }
        var gridColor = 'rgba(128,128,128,0.1)';
        // 현금 흐름 관점: 유입(매도+배당)은 0축 위로 스택, 유출(매수)은 음수로 아래에,
        // 순흐름(유입-유출)은 라인으로 오버레이. (단순 3종 합산 스택은 방향이 달라 합계가 무의미)
        var activityChartBuyOut = activityChartBuy.map(function(v) { return -Number(v || 0); });
        var activityChartNetFlow = activityChartLabels.map(function(_, i) {
            return Number(activityChartSell[i] || 0) + Number(activityChartDividend[i] || 0) - Number(activityChartBuy[i] || 0);
        });
        function formatSignedCompact(v) {
            var n = Number(v) || 0;
            var abs = Math.abs(n);
            var sign = n < 0 ? '-' : '';
            if (abs >= 100000000) return sign + (abs / 100000000).toFixed(0) + '억';
            if (abs >= 10000) return sign + (abs / 10000).toFixed(0) + '만';
            return n.toLocaleString();
        }
        window.activityMonthlyChartInst = new Chart(chartCanvas, {
            type: 'bar',
            data: {
                labels: activityChartLabels,
                datasets: [
                    { label: activityChartSeriesLabels[1], data: activityChartSell, backgroundColor: 'rgba(59,130,246,0.7)', borderRadius: 3, maxBarThickness: 36, stack: 'flow', order: 1 },
                    { label: activityChartSeriesLabels[2], data: activityChartDividend, backgroundColor: 'rgba(34,197,94,0.7)', borderRadius: 3, maxBarThickness: 36, stack: 'flow', order: 1 },
                    { label: activityChartSeriesLabels[0], data: activityChartBuyOut, backgroundColor: 'rgba(239,68,68,0.7)', borderRadius: 3, maxBarThickness: 36, stack: 'flow', order: 1 },
                    { label: activityChartSeriesLabels[3], data: activityChartNetFlow, type: 'line', borderColor: 'rgba(107,114,128,0.9)', backgroundColor: 'rgba(107,114,128,0.9)', borderWidth: 2, pointRadius: 3, fill: false, tension: 0, order: 0 }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'top', labels: { font: { size: 11 }, boxWidth: 12 } },
                    tooltip: {
                        callbacks: {
                            label: function(ctx) {
                                var raw = Number(ctx.parsed.y) || 0;
                                // 데이터셋별 명시적 판별:
                                // - line(순흐름): 부호 그대로 (+/- 표기)
                                // - 매수(인덱스 2, 음수로 넣은 유출): 절대값 표시
                                // - 매도/배당: 값 그대로
                                if (ctx.dataset.type === 'line') {
                                    return ctx.dataset.label + ': ' + (raw >= 0 ? '+' : '-') + '₩' + Math.abs(raw).toLocaleString();
                                }
                                if (ctx.datasetIndex === 2) {
                                    return ctx.dataset.label + ': ₩' + Math.abs(raw).toLocaleString();
                                }
                                return ctx.dataset.label + ': ₩' + raw.toLocaleString();
                            }
                        }
                    }
                },
                scales: {
                    x: { stacked: true, grid: { color: gridColor }, ticks: { font: { size: 10 } } },
                    y: {
                        stacked: true,
                        grid: { color: gridColor },
                        ticks: { font: { size: 10 }, callback: formatSignedCompact }
                    }
                }
            }
        });
        }, 0);
    })();
    </script>
</div>
