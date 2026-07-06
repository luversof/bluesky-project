@import java.util.List
@import java.util.Map
@import java.util.UUID
@import java.time.Instant
@import java.time.ZoneId
@import java.math.BigDecimal
@import net.luversof.web.gate.stock.dto.response.TradeResponse
@import net.luversof.web.common.menu.domain.Pagination
@import net.luversof.web.gate.stock.domain.Account
@import net.luversof.web.gate.stock.domain.StockItem
@import net.luversof.web.gate.stock.domain.TradeProfit
@import net.luversof.web.gate.stock.controller.StockTradeHtmxController.AccountRealizedRow
@import net.luversof.web.gate.stock.constant.TradeType
@import java.time.format.DateTimeFormatter
@import java.text.DecimalFormat
@import io.github.luversof.boot.context.support.MessageUtil

@param List<TradeResponse> tradeList
@param List<TradeResponse> allTradeList = java.util.List.of()
@param Pagination pagination
@param int totalItems
@param int totalPages
@param int currentPage
@param int size
@param List<Account> accountList
@param List<StockItem> stockItemList
@param List<StockItem> tagCountStockItemList = stockItemList
@param List<String> stockTagList = java.util.List.of()
@param Map<UUID, String> accountNames
@param UUID selectedAccountId
@param UUID selectedStockItemId
@param List<UUID> selectedAccountIds = java.util.List.of()
@param List<UUID> selectedStockItemIds = java.util.List.of()
@param List<String> selectedStockTags = java.util.List.of()
@param java.time.Instant startDate
@param java.time.Instant endDate
@param String timeZone = null
@param String sort
@param BigDecimal totalFee
@param BigDecimal totalTax
@param BigDecimal totalRealizedProfit
@param BigDecimal totalAllBuyAmount = BigDecimal.ZERO
@param BigDecimal totalAllSellAmount = BigDecimal.ZERO
@param BigDecimal totalAllFee = BigDecimal.ZERO
@param BigDecimal totalAllTax = BigDecimal.ZERO
@param BigDecimal totalAllRealizedProfit = BigDecimal.ZERO
@param String rangeMode = null
@param String dataFirstDate = null
@param List<AccountRealizedRow> accountRealizedList = java.util.List.of()
@param List<TradeProfit> stockRealizedList = java.util.List.of()
@param long realizedWinCount = 0L
@param int realizedStockCount = 0

!{
    String sortField = sort != null ? sort.split(",")[0] : "";
    String sortDir = sort != null && sort.contains(",") ? sort.split(",")[1] : "";
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DecimalFormat decimalFormat = new DecimalFormat("#,##0");
    java.util.function.Function<Object, String> countMessage = value -> java.text.MessageFormat.format(MessageUtil.getMessage("common.count"), value);
    String advancedSearchLabel = MessageUtil.getMessage("stock.trade.label.advanced.search");
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
    String periodStatusLabel = MessageUtil.getMessage("stock.trade.label.period.status");
    String realizedProfitLabel = MessageUtil.getMessage("stock.trade.label.realized.profit");
    String tradingCostLabel = MessageUtil.getMessage("stock.trade.label.trading.cost");
    String feeLabel = MessageUtil.getMessage("stock.trade.label.fee");
    String tradingTaxLabel = MessageUtil.getMessage("stock.trade.label.trading.tax");
    String buyConcentrationTitle = MessageUtil.getMessage("stock.trade.chart.title.buy.concentration");
    String monthlyTradeAmountTitle = MessageUtil.getMessage("stock.trade.chart.title.monthly.trade.amount");
    String detailListLabel = MessageUtil.getMessage("stock.trade.label.detail.list");
    String dateLabel = MessageUtil.getMessage("common.label.date");
    String typeLabel = MessageUtil.getMessage("common.label.type");
    String unitPriceLabel = MessageUtil.getMessage("stock.trade.column.unit.price");
    String quantityLabel = MessageUtil.getMessage("common.label.quantity");
    String amountLabel = MessageUtil.getMessage("common.label.amount");
    String noTradeDataLabel = MessageUtil.getMessage("stock.trade.message.no.data");
    String totalLabelText = MessageUtil.getMessage("common.label.total");
    String byAccountSectionLabel = MessageUtil.getMessage("stock.realized.section.by.account");
    String byStockSectionLabel = MessageUtil.getMessage("stock.realized.section.by.stock");
    String buyAmountLabel = MessageUtil.getMessage("stock.trade.label.buy.amount");
    String sellAmountLabel = MessageUtil.getMessage("stock.profit.sell.amount");
    String returnRateLabel = MessageUtil.getMessage("stock.label.return.rate");
    String winCountBadgeLabel = java.text.MessageFormat.format(
        MessageUtil.getMessage("stock.realized.summary.win.count"), realizedWinCount, realizedStockCount);
    String selectionSummaryLabel = MessageUtil.getMessage("stock.analytics.selection.summary");
    String selectionCountTemplate = MessageUtil.getMessage("stock.analytics.selection.count");
    String selectionClearLabel = MessageUtil.getMessage("stock.analytics.selection.clear");
    java.util.function.Function<TradeType, String> tradeTypeLabel = type -> type == TradeType.BUY ? buyLabel : sellLabel;

    BigDecimal safeBuy = totalAllBuyAmount != null ? totalAllBuyAmount : BigDecimal.ZERO;
    BigDecimal safeSell = totalAllSellAmount != null ? totalAllSellAmount : BigDecimal.ZERO;
    BigDecimal safeFee = totalAllFee != null ? totalAllFee : BigDecimal.ZERO;
    BigDecimal safeTax = totalAllTax != null ? totalAllTax : BigDecimal.ZERO;
    BigDecimal safeRp = totalAllRealizedProfit != null ? totalAllRealizedProfit : BigDecimal.ZERO;
    boolean rpPositive = safeRp.compareTo(BigDecimal.ZERO) >= 0;

    // Build JSON for charts
    java.util.function.Function<String, String> ej = s ->
        s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    DateTimeFormatter ymd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    StringBuilder jsSb = new StringBuilder();
    jsSb.append("const tradeData=[");
    List<TradeResponse> chartList = allTradeList != null ? allTradeList : java.util.List.of();
    for (int ci = 0; ci < chartList.size(); ci++) {
        TradeResponse t = chartList.get(ci);
        if (ci > 0) jsSb.append(",");
        // Use the user's timezone (if provided) when rendering trade dates in JS payload
        java.time.ZoneId _zone = (timeZone != null && !timeZone.isEmpty()) ? java.time.ZoneId.of(timeZone) : java.time.ZoneId.systemDefault();
        jsSb.append("{tradeDate:\"").append(t.tradeDate() != null ? ymd.format(t.tradeDate().atZone(_zone)) : "").append("\"");
        jsSb.append(",stockItem:\"").append(ej.apply(t.stockItemName())).append("\"");
        jsSb.append(",account:\"").append(ej.apply(accountNames != null ? accountNames.getOrDefault(t.accountId(), "") : "")).append("\"");
        jsSb.append(",type:\"").append(t.type() != null ? t.type().name() : "").append("\"");
        jsSb.append(",amount:").append(t.amount() != null ? t.amount().toPlainString() : "0");
        jsSb.append(",profit:").append(t.realizedProfit() != null ? t.realizedProfit().toPlainString() : "0");
        jsSb.append("}");
    }
    jsSb.append("];");
    String tradeDataJs = jsSb.toString();

    // Nav 버튼 활성화 여부 (use client timezone if available)
    boolean isAllMode = "all".equals(rangeMode);
    java.time.ZoneId zone = (timeZone != null && !timeZone.isEmpty()) ? java.time.ZoneId.of(timeZone) : java.time.ZoneId.systemDefault();
    java.time.LocalDate today = java.time.LocalDate.now(zone);
    java.time.LocalDate startLocal = startDate != null ? startDate.atZone(zone).toLocalDate() : null;
    java.time.LocalDate endLocal = endDate != null ? endDate.atZone(zone).toLocalDate() : null;
    boolean canNext = !isAllMode && endLocal != null && endLocal.isBefore(today);
    boolean canPrev = !isAllMode && startLocal != null
        && dataFirstDate != null && !dataFirstDate.isEmpty()
        && startLocal.isAfter(java.time.LocalDate.parse(dataFirstDate));
}

<div id="tradeListFragment">

    @template.stock.htmx.fragments.dateRangeNavBar(
        pickerName = "tradePicker",
        btnClass = "date-range-btn",
        canPrev = canPrev,
        canNext = canNext,
        rangeMode = rangeMode,
        startDate = startLocal,
        endDate = endLocal
    )

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

    <!-- Summary Cards -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-4">

        <!-- Card 1: 매수/매도 금액 -->
        @template._components.ui.card(body = @`
                <div class="text-xs text-base-content/50 mb-1">${periodStatusLabel}</div>
                <div class="flex items-baseline gap-1 mb-2">
                    <span class="text-xl font-bold text-error amount-value">&#8361; ${decimalFormat.format(safeBuy)}</span>
                    <span class="text-xs text-base-content/40">${buyLabel}</span>
                </div>
                <div class="flex gap-4 text-xs text-base-content/50">
                    <span>${sellLabel} <span class="font-medium text-info amount-value">&#8361; ${decimalFormat.format(safeSell)}</span></span>
                    <span class="ml-auto">${countMessage.apply(totalItems)}</span>
                </div>
        `)

        <!-- Card 2: 실현손익 -->
        @template._components.ui.card(body = @`
                <div class="text-xs text-base-content/50 mb-1">${realizedProfitLabel}</div>
                <div class="text-xl font-bold ${rpPositive ? "text-profit" : "text-loss"} amount-value">
                    ${rpPositive ? "+" : ""}&#8361; ${decimalFormat.format(safeRp)}
                </div>
                !{
                    long sellCount = allTradeList != null ? allTradeList.stream()
                        .filter(t -> t.type() == net.luversof.web.gate.stock.constant.TradeType.SELL)
                        .count() : 0L;
                }
                <div class="text-xs text-base-content/40 mt-1">${sellLabel} ${countMessage.apply(sellCount)}</div>
        `)

        <!-- Card 3: 거래 비용 -->
        @template._components.ui.card(body = @`
                <div class="text-xs text-base-content/50 mb-1">${tradingCostLabel}</div>
                <div class="text-xl font-bold amount-value">&#8361; ${decimalFormat.format(safeFee.add(safeTax))}</div>
                <div class="flex gap-4 text-xs text-base-content/50 mt-1">
                    <span>${feeLabel} <span class="font-medium text-base-content/70 amount-value">&#8361; ${decimalFormat.format(safeFee)}</span></span>
                    <span>${tradingTaxLabel} <span class="font-medium text-base-content/70 amount-value">&#8361; ${decimalFormat.format(safeTax)}</span></span>
                </div>
        `)

    </div>

    <!-- Charts Row -->
    <div class="flex flex-wrap gap-4 mb-4 overflow-visible">

        @template.stock.htmx.fragments.components.doughnutShell(
            title = buyConcentrationTitle,
            canvasId = "tradeDonutChart",
            legendId = "tradeDonutLegend",
            titleId = "tradeDonutTitle",
            metricProfitId = "tradeDonutMetricProfit",
            metricBuyId = "tradeDonutMetricBuy",
            tabStockId = "tradeDonutTabStock",
            tabAccountId = "tradeDonutTabAccount"
        )

        @template.stock.htmx.fragments.components.barShell(
            title = monthlyTradeAmountTitle,
            canvasId = "tradeMonthlyChart"
        )

    </div>

    @if(accountRealizedList != null && !accountRealizedList.isEmpty())
    <!-- 계좌별 실현손익 -->
    <div class="mb-4" data-trade-realized-section>
        <h2 class="text-base font-semibold mb-2 opacity-70">${byAccountSectionLabel}</h2>
        <div class="selection-reveal"><div>
        <div class="rounded-2xl border border-info/15 bg-info/5 px-4 py-3 mb-3"
             data-trade-realized-summary
             data-count-template="${selectionCountTemplate}"
             aria-live="polite">
            <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                <div class="flex flex-wrap items-center gap-2">
                    <h3 class="text-sm font-semibold">${selectionSummaryLabel}</h3>
                    <span class="badge badge-info badge-outline" data-trade-realized-selection-count></span>
                </div>
                <button type="button" class="btn btn-ghost btn-xs self-start" data-trade-realized-clear>${selectionClearLabel}</button>
            </div>
            <div class="mt-3 grid gap-3 sm:grid-cols-4">
                <div class="rounded-box bg-base-100/80 px-3 py-2">
                    <div class="text-xs text-base-content/60">${buyAmountLabel}</div>
                    <div class="mt-1 font-mono font-semibold amount-value" data-trade-realized-sel-buy>0</div>
                </div>
                <div class="rounded-box bg-base-100/80 px-3 py-2">
                    <div class="text-xs text-base-content/60">${sellAmountLabel}</div>
                    <div class="mt-1 font-mono font-semibold amount-value" data-trade-realized-sel-sell>0</div>
                </div>
                <div class="rounded-box bg-base-100/80 px-3 py-2">
                    <div class="text-xs text-base-content/60">${realizedProfitLabel}</div>
                    <div class="mt-1 font-mono font-semibold amount-value" data-trade-realized-sel-net>0</div>
                </div>
                <div class="rounded-box bg-base-100/80 px-3 py-2">
                    <div class="text-xs text-base-content/60">${returnRateLabel}</div>
                    <div class="mt-1 font-mono font-semibold" data-trade-realized-sel-rate>-</div>
                </div>
            </div>
        </div>
        </div></div>
        @template._components.ui.tableCard(cssClass = "overflow-x-auto", body = @`
            <table class="table table-sm w-full">
                <thead class="bg-base-200">
                    <tr>
                        <th scope="col">${accountLabel}</th>
                        <th scope="col" class="text-right">${buyAmountLabel}</th>
                        <th scope="col" class="text-right">${sellAmountLabel}</th>
                        <th scope="col" class="text-right">${realizedProfitLabel}</th>
                        <th scope="col" class="text-right">${returnRateLabel}</th>
                    </tr>
                </thead>
                <tbody>
                @for(AccountRealizedRow row : accountRealizedList)
                    !{BigDecimal rowNet = row.realizedNet() != null ? row.realizedNet() : BigDecimal.ZERO;}
                    !{BigDecimal rowSoldCost = row.soldCost() != null ? row.soldCost() : BigDecimal.ZERO;}
                    !{boolean rowHasRate = rowSoldCost.compareTo(BigDecimal.ZERO) > 0;}
                    !{double rowRate = rowHasRate ? rowNet.doubleValue() / rowSoldCost.doubleValue() * 100 : 0.0;}
                    !{boolean rowPositive = rowNet.compareTo(BigDecimal.ZERO) >= 0;}
                    <tr class="cursor-pointer"
                        data-trade-realized-row
                        tabindex="0"
                        role="button"
                        aria-pressed="false"
                        data-buy="${(row.buyAmount() != null ? row.buyAmount() : BigDecimal.ZERO).toPlainString()}"
                        data-sell="${(row.sellAmount() != null ? row.sellAmount() : BigDecimal.ZERO).toPlainString()}"
                        data-net="${rowNet.toPlainString()}"
                        data-sold-cost="${rowSoldCost.toPlainString()}">
                        <td class="font-semibold whitespace-nowrap">${row.name()}</td>
                        <td class="text-right font-mono amount-value">${decimalFormat.format(row.buyAmount() != null ? row.buyAmount() : BigDecimal.ZERO)}</td>
                        <td class="text-right font-mono amount-value">${decimalFormat.format(row.sellAmount() != null ? row.sellAmount() : BigDecimal.ZERO)}</td>
                        <td class="text-right font-bold font-mono amount-value ${rowPositive ? "text-profit" : "text-loss"}">
                            ${String.format("%+,d", rowNet.longValue())}
                        </td>
                        @if(rowHasRate)
                        <td class="text-right font-mono font-semibold ${rowRate >= 0 ? "text-profit" : "text-loss"}">${String.format("%+.2f%%", rowRate)}</td>
                        @else
                        <td class="text-right font-mono opacity-40">-</td>
                        @endif
                    </tr>
                @endfor
                </tbody>
            </table>
        `)
    </div>
    @endif

    @if(stockRealizedList != null && !stockRealizedList.isEmpty())
    <!-- 종목별 실현손익 -->
    <div class="mb-4" data-trade-realized-section>
        <div class="flex items-center justify-between mb-2">
            <h2 class="text-base font-semibold opacity-70">${byStockSectionLabel}</h2>
            <span class="badge badge-sm badge-ghost">${winCountBadgeLabel}</span>
        </div>
        <div class="selection-reveal"><div>
        <div class="rounded-2xl border border-info/15 bg-info/5 px-4 py-3 mb-3"
             data-trade-realized-summary
             data-count-template="${selectionCountTemplate}"
             aria-live="polite">
            <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                <div class="flex flex-wrap items-center gap-2">
                    <h3 class="text-sm font-semibold">${selectionSummaryLabel}</h3>
                    <span class="badge badge-info badge-outline" data-trade-realized-selection-count></span>
                </div>
                <button type="button" class="btn btn-ghost btn-xs self-start" data-trade-realized-clear>${selectionClearLabel}</button>
            </div>
            <div class="mt-3 grid gap-3 sm:grid-cols-4">
                <div class="rounded-box bg-base-100/80 px-3 py-2">
                    <div class="text-xs text-base-content/60">${buyAmountLabel}</div>
                    <div class="mt-1 font-mono font-semibold amount-value" data-trade-realized-sel-buy>0</div>
                </div>
                <div class="rounded-box bg-base-100/80 px-3 py-2">
                    <div class="text-xs text-base-content/60">${sellAmountLabel}</div>
                    <div class="mt-1 font-mono font-semibold amount-value" data-trade-realized-sel-sell>0</div>
                </div>
                <div class="rounded-box bg-base-100/80 px-3 py-2">
                    <div class="text-xs text-base-content/60">${realizedProfitLabel}</div>
                    <div class="mt-1 font-mono font-semibold amount-value" data-trade-realized-sel-net>0</div>
                </div>
                <div class="rounded-box bg-base-100/80 px-3 py-2">
                    <div class="text-xs text-base-content/60">${returnRateLabel}</div>
                    <div class="mt-1 font-mono font-semibold" data-trade-realized-sel-rate>-</div>
                </div>
            </div>
        </div>
        </div></div>
        @template._components.ui.tableCard(cssClass = "overflow-x-auto", body = @`
            <table class="table table-sm w-full">
                <thead class="bg-base-200">
                    <tr>
                        <th scope="col">${stockLabel}</th>
                        <th scope="col" class="text-right">${buyAmountLabel}</th>
                        <th scope="col" class="text-right">${sellAmountLabel}</th>
                        <th scope="col" class="text-right">${realizedProfitLabel}</th>
                        <th scope="col" class="text-right">${returnRateLabel}</th>
                    </tr>
                </thead>
                <tbody>
                @for(TradeProfit item : stockRealizedList)
                    !{BigDecimal itemNet = item.realizedProfitNet() != null ? item.realizedProfitNet() : BigDecimal.ZERO;}
                    !{BigDecimal itemSoldCost = item.totalSellProceeds() != null ? item.totalSellProceeds().subtract(itemNet) : BigDecimal.ZERO;}
                    !{boolean itemHasRate = itemSoldCost.compareTo(BigDecimal.ZERO) > 0;}
                    !{double itemRate = itemHasRate ? itemNet.doubleValue() / itemSoldCost.doubleValue() * 100 : 0.0;}
                    !{boolean itemPositive = itemNet.compareTo(BigDecimal.ZERO) >= 0;}
                    <tr class="cursor-pointer"
                        data-trade-realized-row
                        tabindex="0"
                        role="button"
                        aria-pressed="false"
                        data-buy="${(item.totalBuyAmount() != null ? item.totalBuyAmount() : BigDecimal.ZERO).toPlainString()}"
                        data-sell="${(item.totalSellAmount() != null ? item.totalSellAmount() : BigDecimal.ZERO).toPlainString()}"
                        data-net="${itemNet.toPlainString()}"
                        data-sold-cost="${itemSoldCost.toPlainString()}">
                        <td class="font-semibold whitespace-nowrap"><a href="/stock/item?stockItemId=${item.stockItemId() != null ? item.stockItemId().toString() : ""}" class="link link-hover">${item.stockItemName() != null ? item.stockItemName() : "-"}</a></td>
                        <td class="text-right font-mono amount-value">${decimalFormat.format(item.totalBuyAmount() != null ? item.totalBuyAmount() : BigDecimal.ZERO)}</td>
                        <td class="text-right font-mono amount-value">${decimalFormat.format(item.totalSellAmount() != null ? item.totalSellAmount() : BigDecimal.ZERO)}</td>
                        <td class="text-right font-bold font-mono amount-value ${itemPositive ? "text-profit" : "text-loss"}">
                            ${String.format("%+,d", itemNet.longValue())}
                        </td>
                        @if(itemHasRate)
                        <td class="text-right font-mono font-semibold ${itemRate >= 0 ? "text-profit" : "text-loss"}">${String.format("%+.2f%%", itemRate)}</td>
                        @else
                        <td class="text-right font-mono opacity-40">-</td>
                        @endif
                    </tr>
                @endfor
                </tbody>
            </table>
        `)
    </div>
    @endif

    <script>
    (function() {
        // 계좌별/종목별 실현손익 행 선택 합산 — 배당 효율 랭킹과 동일한 UX
        // (행 클릭 토글 + 하이라이트 + 상단 요약 패널 펼침/접힘 + 선택 해제)
        var sections = document.querySelectorAll('[data-trade-realized-section]');
        for (var si = 0; si < sections.length; si++) {
            (function(section) {
                if (section.dataset.selectionInitialized === 'true') return;
                section.dataset.selectionInitialized = 'true';
                var summary = section.querySelector('[data-trade-realized-summary]');
                if (!summary) return;
                function setRow(row, selected) {
                    if (!row) return;
                    row.classList.toggle('bg-primary/10', selected);
                    row.setAttribute('aria-pressed', selected ? 'true' : 'false');
                }
                function putAmount(sel, value, signed) {
                    var el = summary.querySelector(sel);
                    if (!el) return;
                    var abs = Math.abs(Math.round(value));
                    el.textContent = (signed ? (value >= 0 ? '+' : '-') : '') + '₩' + abs.toLocaleString();
                    if (signed) {
                        el.classList.toggle('text-profit', value >= 0);
                        el.classList.toggle('text-loss', value < 0);
                    }
                }
                function update() {
                    var selected = section.querySelectorAll('[data-trade-realized-row][aria-pressed="true"]');
                    var reveal = summary.closest('.selection-reveal');
                    if (reveal) reveal.classList.toggle('is-open', selected.length > 0);
                    if (selected.length === 0) return;
                    var buy = 0, sell = 0, net = 0, soldCost = 0;
                    for (var i = 0; i < selected.length; i++) {
                        buy += Number(selected[i].dataset.buy || '0');
                        sell += Number(selected[i].dataset.sell || '0');
                        net += Number(selected[i].dataset.net || '0');
                        soldCost += Number(selected[i].dataset.soldCost || '0');
                    }
                    var countEl = summary.querySelector('[data-trade-realized-selection-count]');
                    if (countEl) countEl.textContent = (summary.dataset.countTemplate || '{0}').replace('{0}', selected.length);
                    putAmount('[data-trade-realized-sel-buy]', buy, false);
                    putAmount('[data-trade-realized-sel-sell]', sell, false);
                    putAmount('[data-trade-realized-sel-net]', net, true);
                    var rateEl = summary.querySelector('[data-trade-realized-sel-rate]');
                    if (rateEl) {
                        if (soldCost > 0) {
                            var rate = net / soldCost * 100;
                            rateEl.textContent = (rate >= 0 ? '+' : '') + rate.toFixed(2) + '%';
                            rateEl.classList.toggle('text-profit', rate >= 0);
                            rateEl.classList.toggle('text-loss', rate < 0);
                        } else {
                            rateEl.textContent = '-';
                            rateEl.classList.remove('text-profit', 'text-loss');
                        }
                    }
                }
                var rows = section.querySelectorAll('[data-trade-realized-row]');
                for (var ri = 0; ri < rows.length; ri++) {
                    (function(row) {
                        row.addEventListener('click', function(event) {
                            if (event.target && event.target.closest('button, a, input, label, select, textarea')) return;
                            setRow(this, this.getAttribute('aria-pressed') !== 'true');
                            update();
                        });
                        row.addEventListener('keydown', function(event) {
                            if (event.target && event.target.closest('button, a, input, label, select, textarea')) return;
                            if (event.key !== 'Enter' && event.key !== ' ') return;
                            event.preventDefault();
                            setRow(this, this.getAttribute('aria-pressed') !== 'true');
                            update();
                        });
                    })(rows[ri]);
                }
                var clearButton = section.querySelector('[data-trade-realized-clear]');
                if (clearButton) {
                    clearButton.addEventListener('click', function() {
                        var selected = section.querySelectorAll('[data-trade-realized-row][aria-pressed="true"]');
                        for (var i = 0; i < selected.length; i++) setRow(selected[i], false);
                        update();
                    });
                }
            })(sections[si]);
        }
    })();
    </script>

    <!-- Collapsible Detail List -->
    <details id="tradeDetailSection" class="mb-4" data-persist-open="tradeDetail">
        <summary class="cursor-pointer select-none flex items-center gap-2 font-semibold text-sm p-3 bg-base-200 rounded-box hover:bg-base-300 transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 transition-transform detail-arrow" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
            </svg>
            ${detailListLabel} (${countMessage.apply(totalItems)})
        </summary>

        <div id="tradeListContainer" class="flex flex-col relative mt-2">
            <div class="flex-1 min-h-0 max-lg:overflow-x-auto">
                <table class="table w-full table-sm table-pin-rows pin-top-nav">
                    <thead>
                    <tr class="bg-base-200">
                        <th scope="col" class="cursor-pointer hover:bg-base-300"
                            hx-get="/stock/htmx/trade/list" hx-target="#tradeListFragment" hx-select="#tradeListFragment" hx-swap="outerHTML" hx-include="#tradeSearchForm"
                            hx-vals='{"sort": "tradeDate,${sortField.equals("tradeDate") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            ${dateLabel} @if(sortField.equals("tradeDate"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th scope="col">${typeLabel}</th>
                        <th scope="col" class="cursor-pointer hover:bg-base-300"
                            hx-get="/stock/htmx/trade/list" hx-target="#tradeListFragment" hx-select="#tradeListFragment" hx-swap="outerHTML" hx-include="#tradeSearchForm"
                            hx-vals='{"sort": "stockItemName,${sortField.equals("stockItemName") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            ${stockLabel} @if(sortField.equals("stockItemName"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th scope="col" class="hidden sm:table-cell">${accountLabel}</th>
                        <th scope="col" class="text-right">${unitPriceLabel}</th>
                        <th scope="col" class="text-right">${quantityLabel}</th>
                        <th scope="col" class="text-right cursor-pointer hover:bg-base-300"
                            hx-get="/stock/htmx/trade/list" hx-target="#tradeListFragment" hx-select="#tradeListFragment" hx-swap="outerHTML" hx-include="#tradeSearchForm"
                            hx-vals='{"sort": "amount,${sortField.equals("amount") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            ${amountLabel} @if(sortField.equals("amount"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th scope="col" class="text-right cursor-pointer hover:bg-base-300 hidden lg:table-cell"
                            hx-get="/stock/htmx/trade/list" hx-target="#tradeListFragment" hx-select="#tradeListFragment" hx-swap="outerHTML" hx-include="#tradeSearchForm"
                            hx-vals='{"sort": "fee,${sortField.equals("fee") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            ${feeLabel} @if(sortField.equals("fee"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th scope="col" class="text-right hidden lg:table-cell">${tradingTaxLabel}</th>
                        <th scope="col" class="text-right cursor-pointer hover:bg-base-300"
                            hx-get="/stock/htmx/trade/list" hx-target="#tradeListFragment" hx-select="#tradeListFragment" hx-swap="outerHTML" hx-include="#tradeSearchForm"
                            hx-vals='{"sort": "realizedProfit,${sortField.equals("realizedProfit") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            ${realizedProfitLabel} @if(sortField.equals("realizedProfit"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                    </tr>
                    </thead>
                    <tbody>
                    @for(TradeResponse trade : tradeList)
                    <tr>
                        <td class="whitespace-nowrap">${trade.tradeDate() != null ? dateFormatter.format(trade.tradeDate().atZone(java.time.ZoneId.systemDefault())) : ""}</td>
                        <td>
                            <span class="${trade.type() == TradeType.BUY ? "badge badge-error badge-outline badge-sm" : "badge badge-info badge-outline badge-sm"}">
                                ${tradeTypeLabel.apply(trade.type())}
                            </span>
                        </td>
                        <td class="font-bold whitespace-nowrap"><a href="/stock/item?stockItemId=${trade.stockItemId() != null ? trade.stockItemId().toString() : ""}" class="link link-hover">${trade.stockItemName()}</a></td>
                        <td class="whitespace-nowrap hidden sm:table-cell"><a href="/stock/account?accountId=${trade.accountId() != null ? trade.accountId().toString() : ""}" class="link link-hover">${accountNames != null ? accountNames.getOrDefault(trade.accountId(), "") : ""}</a></td>
                        <td class="text-right font-mono amount-value">${trade.price() != null ? decimalFormat.format(trade.price()) : "0"}</td>
                        <td class="text-right font-mono amount-value">${decimalFormat.format(trade.quantity())}</td>
                        <td class="text-right font-mono amount-value">${trade.amount() != null ? decimalFormat.format(trade.amount()) : "0"}</td>
                        <td class="text-right text-xs text-base-content/60 amount-value hidden lg:table-cell">${trade.fee() != null ? decimalFormat.format(trade.fee()) : "0"}</td>
                        <td class="text-right text-xs text-base-content/60 amount-value hidden lg:table-cell">${trade.tax() != null ? decimalFormat.format(trade.tax()) : "0"}</td>
                        <td class="text-right font-bold font-mono amount-value ${trade.realizedProfit() != null ? (trade.realizedProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss") : ""}">
                            ${trade.realizedProfit() != null && trade.type() == TradeType.SELL ? decimalFormat.format(trade.realizedProfit()) : ""}
                        </td>
                    </tr>
                    @endfor
                    @if(tradeList == null || tradeList.isEmpty())
                    <tr>
                        <td colspan="10">@template._components.ui.emptyState(message = noTradeDataLabel)</td>
                    </tr>
                    @endif
                    </tbody>
                    <tfoot>
                        <tr class="font-bold bg-base-200">
                            <td colspan="6" class="text-right pr-4">${totalLabelText}</td>
                            <td class="text-right font-mono amount-value"></td>
                            <td class="text-right font-mono amount-value hidden lg:table-cell">${totalFee != null ? decimalFormat.format(totalFee) : "0"}</td>
                            <td class="text-right font-mono amount-value hidden lg:table-cell">${totalTax != null ? decimalFormat.format(totalTax) : "0"}</td>
                            <td class="text-right font-mono amount-value ${totalRealizedProfit != null && totalRealizedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                                ${totalRealizedProfit != null ? decimalFormat.format(totalRealizedProfit) : "0"}
                            </td>
                        </tr>
                    </tfoot>
                </table>
            </div>

            @if(pagination != null && pagination.getTotalPage() > 1)
            @template._components.htmxPagination(
                pagination = pagination,
                url = "/stock/htmx/trade/list",
                includeSelector = "#tradeSearchForm",
                targetSelector = "#tradeListFragment")
            @endif
        </div>
    </details>

    <script>
    (function() {
        $unsafe{tradeDataJs}

        window.tradeMonthlyChartInst = null;
        window.tradeDonutChartInst = null;
        window.currentMetric = 'buy';
        window.currentGroupBy = 'stock';

        window.switchTradeDonut = function(metric, groupBy) {
            if (metric) window.currentMetric = metric;
            if (groupBy) window.currentGroupBy = groupBy;
            var elProfit = document.getElementById('tradeDonutMetricProfit');
            var elBuy = document.getElementById('tradeDonutMetricBuy');
            var elStock = document.getElementById('tradeDonutTabStock');
            var elAccount = document.getElementById('tradeDonutTabAccount');
            if (elProfit) elProfit.classList.toggle('tab-active', window.currentMetric === 'profit');
            if (elBuy) elBuy.classList.toggle('tab-active', window.currentMetric === 'buy');
            if (elStock) elStock.classList.toggle('tab-active', window.currentGroupBy === 'stock');
            if (elAccount) elAccount.classList.toggle('tab-active', window.currentGroupBy === 'account');

            if (window.StockCharts) {
                window.tradeDonutChartInst = window.StockCharts.initDonutFromData(tradeData, {
                    canvasId: 'tradeDonutChart',
                    legendId: 'tradeDonutLegend',
                    titleId: 'tradeDonutTitle',
                    metric: window.currentMetric,
                    groupBy: window.currentGroupBy,
                    tooltipId: 'tradeDonutTooltipEl'
                }, window.tradeDonutChartInst);
            }
        };

        function initAll() {
            if (!window.StockCharts) return;
            window.tradeMonthlyChartInst = window.StockCharts.initMonthlyFromData(tradeData, 'tradeMonthlyChart', window.tradeMonthlyChartInst);
            window.tradeDonutChartInst = window.StockCharts.initDonutFromData(tradeData, {
                canvasId: 'tradeDonutChart',
                legendId: 'tradeDonutLegend',
                titleId: 'tradeDonutTitle',
                metric: window.currentMetric,
                groupBy: window.currentGroupBy,
                tooltipId: 'tradeDonutTooltipEl'
            }, window.tradeDonutChartInst);
        }

        window.tradePicker = window.DateRangePicker
            ? window.DateRangePicker.create({
                formId:      'tradeSearchForm',
                startId:     'tradeStartDateInput',
                endId:       'tradeEndDateInput',
                instantStartId: 'tradeStartInstantInput',
                instantEndId:   'tradeEndInstantInput',
                timeZoneId:     'tradeTimeZoneInput',
                rangeModeId: 'tradeRangeModeInput',
                btnClass:    'date-range-btn',
                minDate:     '${dataFirstDate != null ? dataFirstDate : ""}',
                globalKey:   'globalDateRange',
                rootSelector: '#tradeListFragment'
            })
            : { set: function(){}, shift: function(){}, jumpToEdge: function(){} };
        // 기본값: 페이지 진입 시 기간 미지정이면 '이번달(mtd)'을 선택 (한번만 적용하도록 세션 가드)
        try {
            if (window.tradePicker && typeof window.tradePicker.getState === 'function') {
                var __s = window.tradePicker.getState();
                if ((!__s || !__s.mode) && (!__s || !__s.start) && (!__s || !__s.end)) {
                    var __key = 'dateRangeDefaultApplied:tradePicker';
                    try {
                        if (!sessionStorage.getItem(__key)) {
                            var _btn = document.querySelector('#tradeListFragment .date-range-btn');
                            window.tradePicker.set('mtd', _btn || null);
                            sessionStorage.setItem(__key, '1');
                        }
                    } catch(e) { window.tradePicker.set('mtd', null); }
                }
            }
        } catch(e) {}
        // Chart.js / StockCharts 는 stockLayout 에서 1회 로드되어 항상 존재한다. (initAll 내부에서도 자체 가드)
        setTimeout(initAll, 0);
        setTimeout(function() {
            if (window.tradeMonthlyChartInst && window.tradeMonthlyChartInst.resize) window.tradeMonthlyChartInst.resize();
            if (window.tradeDonutChartInst && window.tradeDonutChartInst.resize) window.tradeDonutChartInst.resize();
        }, 200);

        const detail = document.getElementById('tradeDetailSection');
        if (detail) {
            const arrow = detail.querySelector('.detail-arrow');
            detail.addEventListener('toggle', function() {
                if (arrow) arrow.style.transform = detail.open ? 'rotate(90deg)' : '';
            });
        }

        // 페이징 클릭 시 fragment 상단 스크롤
        if (window.__pendingScrollTo === 'tradeListFragment') {
            delete window.__pendingScrollTo;
            var frag = document.getElementById('tradeListFragment');
            if (frag) frag.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
        document.querySelectorAll('#tradeListFragment .join button[hx-get]').forEach(function(btn) {
            btn.addEventListener('click', function() { window.__pendingScrollTo = 'tradeListFragment'; });
        });
    })();
    </script>
</div>
