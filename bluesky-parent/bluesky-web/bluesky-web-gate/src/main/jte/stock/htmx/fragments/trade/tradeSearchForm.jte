@import java.util.List
@import java.util.UUID
@import java.time.Instant
@import java.time.ZoneId
@import net.luversof.web.gate.stock.domain.Account
@import net.luversof.web.gate.stock.domain.StockItem
@import io.github.luversof.boot.context.support.MessageUtil

@param List<Account> accountList
@param List<StockItem> stockItemList
@param List<StockItem> tagCountStockItemList
@param List<String> stockTagList
@param UUID selectedAccountId
@param UUID selectedStockItemId
@param List<UUID> selectedAccountIds
@param List<UUID> selectedStockItemIds
@param List<String> selectedStockTags
@param Instant startDate
@param Instant endDate
@param String timeZone
@param String rangeMode
@param ZoneId zone
@param String accountLabel
@param String stockLabel

!{
    String advancedSearchLabel = MessageUtil.getMessage("stock.trade.label.advanced.search");
    String periodLabel = MessageUtil.getMessage("common.label.period");
    String tagLabel = MessageUtil.getMessage("common.label.tag");
    String tagHelperLabel = MessageUtil.getMessage("stock.filter.tag.helper");
    String tagSummaryLabel = MessageUtil.getMessage("stock.filter.tag.summary.selected");
    String tagStockSummaryPattern = MessageUtil.getMessage("stock.filter.tag.summary.stocks");
    String tagMorePattern = MessageUtil.getMessage("stock.filter.tag.summary.more");
    String allLabel = MessageUtil.getMessage("stock.label.period.all");
    String searchLabel = MessageUtil.getMessage("common.button.search");
    String resetLabel = MessageUtil.getMessage("common.button.reset");
}

    <!-- Filter & Search Form (collapsible) -->
    <details class="mb-4" open data-detail-filter>
        <summary class="flex items-center gap-2 cursor-pointer select-none text-sm font-medium text-base-content/60 hover:text-base-content py-1 px-2 rounded hover:bg-base-200 w-fit transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2a1 1 0 01-.293.707L13 13.414V19a1 1 0 01-.553.894l-4 2A1 1 0 017 21v-7.586L3.293 6.707A1 1 0 013 6V4z" /></svg>
            ${advancedSearchLabel}
            @template.stock.htmx.fragments.components.filterBadge(
                selectedAccountId = selectedAccountId,
                selectedStockItemId = selectedStockItemId,
                startDate = startDate,
                endDate = endDate
            )
        </summary>
    <form id="tradeSearchForm" class="mt-2 mb-2 rounded-2xl bg-base-100 p-3 shadow-sm"
          hx-get="/stock/htmx/trade/list"
          hx-target="#tradeListFragment"
          hx-select="#tradeListFragment"
          hx-swap="outerHTML"
          hx-trigger="submit">

        <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-[minmax(0,18rem)_minmax(0,22rem)_minmax(0,1fr)_minmax(0,19rem)_minmax(0,auto)] xl:items-start">
            <div class="form-control w-full">
                <label class="label p-1" id="filterPeriodLabel"><span class="label-text text-xs">${periodLabel}</span></label>
                <div class="join w-full" role="group" aria-labelledby="filterPeriodLabel">
                    <input type="date" name="tradeUiStartDate" id="tradeStartDateInput" class="input input-sm input-bordered join-item w-1/2" value="${startDate != null ? startDate.atZone(zone).toLocalDate().toString() : ""}">
                    <input type="date" name="tradeUiEndDate" id="tradeEndDateInput" class="input input-sm input-bordered join-item w-1/2" value="${endDate != null ? endDate.atZone(zone).toLocalDate().minusDays(1).toString() : ""}">
                </div>
            </div>

            <div class="form-control w-full">
                <label class="label p-1" for="filterAccountSelect"><span class="label-text text-xs">${accountLabel}</span></label>
                <select id="filterAccountSelect" name="accountIdList" class="select select-bordered w-full max-h-40 overflow-auto" multiple size="${Math.min(accountList != null ? accountList.size() + 1 : 1, 7)}" data-max-visible="7">
                    <option value="">${allLabel}</option>
                    @for(Account account : accountList)
                    <option value="${account.id().toString()}" selected="${selectedAccountIds != null && selectedAccountIds.contains(account.id())}">${account.name()}</option>
                    @endfor
                </select>
            </div>

            <div class="form-control w-full">
                <label class="label p-1" for="filterStockSelect"><span class="label-text text-xs">${stockLabel}</span></label>
                <select id="filterStockSelect" name="stockItemIdList" class="select select-bordered w-full max-h-40 overflow-auto bg-base-100" multiple size="${Math.min(stockItemList != null ? stockItemList.size() + 1 : 1, 7)}" data-max-visible="7">
                    <option value="">${allLabel}</option>
                    @for(StockItem stockItem : stockItemList)
                    !{String stockItemTags = stockItem.tags() != null ? String.join("|", stockItem.tags()) : "";}
                    <option value="${stockItem.id().toString()}"
                            selected="${selectedStockItemIds != null && selectedStockItemIds.contains(stockItem.id())}"
                            data-stock-tags="${stockItemTags}">${stockItem.name()}</option>
                    @endfor
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
                    stockItemList = stockItemList,
                    tagCountStockItemList = tagCountStockItemList
                )

                <div class="flex justify-end gap-2 xl:self-end">
                    <button type="button" class="btn btn-sm btn-primary" data-selection-save="true"
                        hx-get="/stock/htmx/trade/list"
                        hx-target="#tradeListFragment"
                        hx-select="#tradeListFragment"
                        hx-swap="outerHTML"
                        hx-include="#tradeSearchForm">${searchLabel}</button>
                    <button type="button" class="btn btn-sm text-xs"
                        hx-get="/stock/htmx/trade/list"
                        hx-target="#tradeListFragment"
                        hx-select="#tradeListFragment"
                            hx-swap="outerHTML"
                            hx-include="#tradeSearchForm"
                        data-selection-reset="true">${resetLabel}</button>
                </div>
            </div>
        </div>
        <input type="hidden" name="startDate" id="tradeStartInstantInput" value="${startDate != null ? startDate.toString() : ""}">
        <input type="hidden" name="endDate" id="tradeEndInstantInput" value="${endDate != null ? endDate.toString() : ""}">
        <input type="hidden" name="timeZone" id="tradeTimeZoneInput" value="${timeZone != null ? timeZone : ""}">
        <input type="hidden" name="rangeMode" id="tradeRangeModeInput" value="${rangeMode != null ? rangeMode : ""}">

    </form>
    </details>

    <script type="module" src="/js/stock/selectionStorage.js"></script>
    <script type="module" src="/js/stock/tagFilterChips.js"></script>
