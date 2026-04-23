@import java.util.List
@import net.luversof.web.gate.stock.controller.StockTradeHtmxController.Activity
@import java.time.ZoneId
@import java.time.format.DateTimeFormatter
@import java.time.Instant
@import java.math.BigDecimal
@import java.text.DecimalFormat

@param List<Activity> activities
@param java.time.Instant startDate = null
@param java.time.Instant endDate = null
@param String timeZone = null
@param String rangeMode = null
@param String dataFirstDate = null
@param long buyCount = 0
@param long sellCount = 0
@param BigDecimal buyAmount = BigDecimal.ZERO
@param BigDecimal sellAmount = BigDecimal.ZERO
@param long dividendCount = 0
@param BigDecimal dividendAmount = BigDecimal.ZERO

!{
    DecimalFormat df = new DecimalFormat("#,##0");
    BigDecimal safeBuy = buyAmount != null ? buyAmount : BigDecimal.ZERO;
    BigDecimal safeSell = sellAmount != null ? sellAmount : BigDecimal.ZERO;
    BigDecimal safeDividend = dividendAmount != null ? dividendAmount : BigDecimal.ZERO;
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
    <details class="mb-4"$unsafe{startDate != null ? " open" : ""}>
        <summary class="flex items-center gap-2 cursor-pointer select-none text-sm font-medium text-base-content/60 hover:text-base-content py-1 px-2 rounded hover:bg-base-200 w-fit transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2a1 1 0 01-.293.707L13 13.414V19a1 1 0 01-.553.894l-4 2A1 1 0 017 21v-7.586L3.293 6.707A1 1 0 013 6V4z" /></svg>
            상세 검색
            @template.stock.htmx.fragments.components.filterBadge(
                startDate = startDate,
                endDate = endDate
            )
        </summary>
    <form id="activitySearchForm" class="flex flex-wrap gap-2 mt-2 mb-2 items-end bg-base-100 p-2 rounded-box shadow-sm"
          hx-get="/stock/htmx/activity-list"
          hx-target="#activityListFragment"
          hx-select="#activityListFragment"
          hx-swap="outerHTML"
          hx-trigger="submit">
        <div class="form-control w-full sm:w-auto">
            <label class="label p-1"><span class="label-text text-xs">기간</span></label>
            <div class="join">
                <input type="date" name="activityUiStartDate" id="activityStartDateInput" class="input input-sm input-bordered join-item" value="${startDate != null ? startDate.atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString() : ""}">
                <input type="date" name="activityUiEndDate" id="activityEndDateInput" class="input input-sm input-bordered join-item" value="${endDate != null ? endDate.atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString() : ""}">
            </div>
        </div>
        <input type="hidden" name="rangeMode" id="activityRangeModeInput" value="${rangeMode != null ? rangeMode : ""}">
        <input type="hidden" name="startDate" id="activityStartInstantInput" value="${startDate != null ? startDate.toString() : ""}">
        <input type="hidden" name="endDate" id="activityEndInstantInput" value="${endDate != null ? endDate.toString() : ""}">
        <input type="hidden" name="timeZone" id="activityTimeZoneInput" value="${timeZone != null ? timeZone : ""}">
        <button type="submit" class="btn btn-sm btn-primary">조회</button>
        <button type="button" class="btn btn-sm text-xs"
                hx-get="/stock/htmx/activity-list"
                hx-target="#activityListFragment"
                hx-select="#activityListFragment"
                hx-swap="outerHTML">초기화</button>
    </form>
    </details>

    <!-- Summary Cards -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-4">

        <!-- Card 1: 매매 현황 -->
        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-xs text-base-content/50 mb-1">매매 현황</div>
                <div class="flex items-baseline gap-1 mb-2">
                    <span class="text-xl font-bold text-error amount-value">₩ ${df.format(safeBuy)}</span>
                    <span class="text-xs text-base-content/40">매수</span>
                </div>
                <div class="flex gap-4 text-xs text-base-content/50">
                    <span>매수 <span class="font-medium text-base-content/70">${buyCount}건</span></span>
                    <span>매도 <span class="font-medium text-info amount-value">₩ ${df.format(safeSell)}</span> <span class="text-base-content/70">${sellCount}건</span></span>
                </div>
            </div>
        </div>

        <!-- Card 2: 배당 수령 -->
        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-xs text-base-content/50 mb-1">배당 수령</div>
                <div class="flex items-baseline gap-1 mb-2">
                    <span class="text-xl font-bold text-dividend amount-value">₩ ${df.format(safeDividend)}</span>
                </div>
                <div class="text-xs text-base-content/50">${dividendCount}건</div>
            </div>
        </div>

        <!-- Card 3: 전체 활동 -->
        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-xs text-base-content/50 mb-1">전체 활동</div>
                <div class="text-xl font-bold">${activities != null ? activities.size() : 0}<span class="text-sm font-normal text-base-content/50 ml-1">건</span></div>
                <div class="flex gap-4 text-xs text-base-content/50 mt-1">
                    <span>매매 <span class="font-medium text-base-content/70">${buyCount + sellCount}건</span></span>
                    <span>배당 <span class="font-medium text-base-content/70">${dividendCount}건</span></span>
                </div>
            </div>
        </div>
    </div>
        <div class="card-body">
            @if(activities == null || activities.isEmpty())
                <div class="text-center text-base-content/50 py-10">활동 내역이 없습니다.</div>
            @else
                <div class="overflow-x-auto">
                    <table class="table table-zebra w-full">
                        <thead>
                            <tr>
                                <th>일자</th>
                                <th>유형</th>
                                <th>종목명</th>
                                <th>상세</th>
                                <th class="text-right">금액</th>
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
                                            <span class="badge badge-sm badge-outline ${"BUY".equals(activity.tradeType()) ? "badge-error" : "badge-info"}">${"BUY".equals(activity.tradeType()) ? "매수" : "매도"}</span>
                                        @else
                                            <span class="badge badge-sm badge-outline badge-success">배당</span>
                                        @endif
                                    </td>
                                    <td class="font-bold">${activity.stockItemName()}</td>
                                    <td>
                                        @if("TRADE".equals(activity.type()))
                                            <span class="amount-value">${activity.quantity()}</span>주
                                        @else
                                            ${activity.description()}
                                        @endif

                                        <!-- Account Badges (Max 2 + Tooltip) -->
                                        @if(activity.accountNames() != null && !activity.accountNames().isEmpty())
                                            <div class="mt-1 flex flex-wrap gap-1">
                                                @for(int i = 0; i < Math.min(2, activity.accountNames().size()); i++)
                                                    <span class="badge badge-xs badge-outline opacity-70">${activity.accountNames().get(i)}</span>
                                                @endfor

                                                @if(activity.accountNames().size() > 2)
                                                    <div class="tooltip tooltip-right" data-tip="${String.join(", ", activity.accountNames())}">
                                                        <span class="badge badge-xs badge-outline opacity-70 cursor-help">+${activity.accountNames().size() - 2}</span>
                                                    </div>
                                                @endif
                                            </div>
                                        @endif
                                    </td>
                                    <td class="text-right font-mono font-bold amount-value ${"DIVIDEND".equals(activity.type()) ? "text-dividend" : ""}">
                                        ${String.format("%,d", activity.amount() != null ? activity.amount().longValue() : 0)} 원
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
            minDate:     '${dataFirstDate != null ? dataFirstDate : ""}'
        });
        window.activityPicker = activityPicker;
    })();
    </script>
</div>
