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
        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-xs text-base-content/50 mb-1">${tradeStatusLabel}</div>
                <div class="flex items-baseline gap-1 mb-2">
                    <span class="text-xl font-bold text-error amount-value">₩ ${df.format(safeBuy)}</span>
                    <span class="text-xs text-base-content/40">${buyLabel}</span>
                </div>
                <div class="flex gap-4 text-xs text-base-content/50">
                    <span>${buyLabel} <span class="font-medium text-base-content/70">${countMessage.apply(buyCount)}</span></span>
                    <span>${sellLabel} <span class="font-medium text-info amount-value">₩ ${df.format(safeSell)}</span> <span class="text-base-content/70">${countMessage.apply(sellCount)}</span></span>
                </div>
            </div>
        </div>

        <!-- Card 2: 배당 수령 -->
        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-xs text-base-content/50 mb-1">${dividendReceivedLabel}</div>
                <div class="flex items-baseline gap-1 mb-2">
                    <span class="text-xl font-bold text-dividend amount-value">₩ ${df.format(safeDividend)}</span>
                </div>
                <div class="text-xs text-base-content/50">${countMessage.apply(dividendCount)}</div>
            </div>
        </div>

        <!-- Card 3: 전체 활동 -->
        <div class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
                <div class="text-xs text-base-content/50 mb-1">${totalActivityLabel}</div>
                <div class="text-xl font-bold">${countMessage.apply(activities != null ? activities.size() : 0)}</div>
                <div class="flex gap-4 text-xs text-base-content/50 mt-1">
                    <span>${tradeLabel} <span class="font-medium text-base-content/70">${countMessage.apply(buyCount + sellCount)}</span></span>
                    <span>${dividendLabel} <span class="font-medium text-base-content/70">${countMessage.apply(dividendCount)}</span></span>
                </div>
            </div>
        </div>
    </div>
        <div class="card-body">
            @if(activities == null || activities.isEmpty())
                @template._components.ui.emptyState(message = noActivityDataLabel)
            @else
                <div>
                    <table class="table table-hover w-full table-sm table-pin-rows pin-top-nav">
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
</div>
