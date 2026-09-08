@import net.luversof.web.gate.util.CspNonceHolder
@import java.util.List
@import java.time.ZoneId
@import java.time.format.DateTimeFormatter
@import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint
@import io.github.luversof.boot.context.support.MessageUtil

@param Object timeSeries
@param java.util.List<net.luversof.web.gate.stock.dto.response.TradeProfitPeriodSummary> periodBreakdown = java.util.List.of()
@param String periodBreakdownNote = ""
@param java.util.List<net.luversof.web.gate.stock.util.StockContributionUtil.Contribution> stockContributions = java.util.List.of()
@param net.luversof.web.gate.stock.util.StockContributionUtil.Contribution stockContributionTotal = null
@param net.luversof.web.gate.stock.util.StockContributionUtil.Contribution stockContributionOthers = null
@param int stockContributionOthersCount = 0
@param java.util.List<net.luversof.web.gate.stock.dto.response.YearlyCostSummary> yearlyCosts = java.util.List.of()
@param String rangeMode
@param String dataFirstDate = null
@param java.time.Instant startDate = null
@param java.time.Instant endDate = null
@param String timeZone = null
@param java.util.List<net.luversof.web.gate.stock.domain.Account> accountList = null
@param java.util.List<net.luversof.web.gate.stock.domain.StockItem> stockItemList = null
@param java.util.List<String> stockTagList = java.util.List.of()
@param java.util.List<java.util.UUID> selectedAccountIds = java.util.List.of()
@param java.util.List<java.util.UUID> selectedStockItemIds = java.util.List.of()
@param boolean coreHoldingTagged = false
@param boolean coreHoldingExcluded = false
@param String coreHoldingName = null
@param int coreHoldingCount = 0
@param String coreHoldingWeightPct = null
@param java.util.List<String> selectedStockTags = java.util.List.of()
@param Double periodReturnRatePct = null
@param boolean returnCalculable = false
@param Double timeWeightedReturnPct = null
@param java.math.BigDecimal periodProfit = null
@param java.math.BigDecimal principalDelta = null
@param java.math.BigDecimal unrealizedStart = null
@param java.math.BigDecimal unrealizedEnd = null
@param Double unrealizedEndPct = null
@param java.math.BigDecimal recoveredAmount = null
@param java.math.BigDecimal netNewProfit = null
@param java.util.List<net.luversof.web.gate.stock.dto.response.TradeProfitYearlySummary> yearlySummaries = java.util.List.of()
@param Double maxDrawdownPct = null
@param java.time.LocalDate maxDrawdownPeakDate = null
@param java.time.LocalDate maxDrawdownTroughDate = null
@param Double currentDrawdownPct = null
@param java.math.BigDecimal openingValue = null
@param java.math.BigDecimal closingValue = null
@param Double periodProfitRatePct = null
@param java.math.BigDecimal peakValue = null
@param java.time.LocalDate peakValueDate = null
@param java.math.BigDecimal troughValue = null
@param java.time.LocalDate troughValueDate = null
@param net.luversof.web.gate.stock.controller.StockAssetGrowthHtmxController.TradeHistoryData tradeHistoryData = null
@param String tradeHistoryFilterQuery = ""

!{
    boolean isAllMode = "all".equals(rangeMode);
    java.time.ZoneId zone = net.luversof.web.gate.stock.util.StockZoneUtil.resolve(timeZone);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zone);

    java.time.LocalDate startLocal = startDate != null ? startDate.atZone(zone).toLocalDate() : null;
    java.time.LocalDate endLocal = endDate != null ? endDate.atZone(zone).toLocalDate() : null;
    java.time.LocalDate endDisplayLocal = endDate != null ? endDate.atZone(zone).toLocalDate().minusDays(1) : null;

    // Normalize model-provided `timeSeries` into `ts` for template use
    // (content block 내부 클래스에서 참조하므로 effectively final 이어야 한다 — 단일 할당)
    @SuppressWarnings("unchecked")
    List<TradeProfitTimeSeriesPoint> ts = timeSeries instanceof List ? (List<TradeProfitTimeSeriesPoint>) timeSeries : null;
    // '전체' 는 날짜를 안 보내므로 기간 배지가 빌 수밖에 없다. 그때는 이 화면이 실제로 그린 자료의
    // 구간을 대신 적는다 - 화면마다 덮는 범위가 다르므로 사용자 전체의 최초 일자를 쓰면 틀린 날짜가 나온다.
    var coveredRange = net.luversof.web.gate.stock.util.StockCoveredRangeUtil.covered(ts, net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint::timestamp, zone);

    java.time.LocalDate today = java.time.LocalDate.now(zone);
    boolean canNext = !isAllMode && endLocal != null && endLocal.isBefore(today);
    boolean canPrev = !isAllMode && startLocal != null && dataFirstDate != null && !dataFirstDate.isEmpty()
            && startLocal.isAfter(java.time.LocalDate.parse(dataFirstDate));

    String assetGrowthTitle = MessageUtil.getMessage("stock.page.asset.growth.title");
    String noDataLabel = MessageUtil.getMessage("common.message.no.data");
    String rangeMaxLabel = MessageUtil.getMessage("stock.chart.range.max");
    String rangeMinLabel = MessageUtil.getMessage("stock.chart.range.min");
    String chartDescriptionLabel = MessageUtil.getMessage("stock.asset.growth.chart.description");
    String clickDateHoldingsLabel = MessageUtil.getMessage("stock.asset.growth.chart.click.date");
    String profitAnalysisLabel = MessageUtil.getMessage("stock.asset.growth.profit.analysis");
    String principalLabel = MessageUtil.getMessage("stock.label.principal");
    String totalAssetDatasetLabel = MessageUtil.getMessage("stock.asset.growth.dataset.total.asset");
    String cumulativeRealizedDatasetLabel = MessageUtil.getMessage("stock.asset.growth.dataset.cumulative.realized");
    String cumulativeDividendLabel = MessageUtil.getMessage("stock.summary.label.cumulative.dividend");
    String cumulativeTotalDatasetLabel = MessageUtil.getMessage("stock.asset.growth.dataset.cumulative.total");
    String unrealizedProfitLabel = MessageUtil.getMessage("stock.asset.growth.tooltip.unrealized.profit");
    String clickTradeHistoryLabel = MessageUtil.getMessage("stock.asset.growth.tooltip.click.trade.history");
    String tradeCountLabel = MessageUtil.getMessage("stock.asset.growth.tooltip.trade.count");
    String dailyRealizedProfitLabel = MessageUtil.getMessage("stock.asset.growth.tooltip.daily.realized.profit");
    String totalAssetAxisLabel = MessageUtil.getMessage("stock.asset.growth.axis.total.asset");
    String cumulativeProfitAxisLabel = MessageUtil.getMessage("stock.asset.growth.axis.cumulative.profit");

    // Detail search filter labels (reuse realized-profit keys)
    String detailSearchLabel = MessageUtil.getMessage("common.label.detail") + " " + MessageUtil.getMessage("common.button.search");
    String periodLabel = MessageUtil.getMessage("common.label.period");
    String accountLabel = MessageUtil.getMessage("common.label.account");
    String stockNameLabel = MessageUtil.getMessage("stock.activity.label.stock.name");
    String tagLabel = MessageUtil.getMessage("common.label.tag");
    String tagHelperLabel = MessageUtil.getMessage("stock.filter.tag.helper");
    String tagSummaryLabel = MessageUtil.getMessage("stock.filter.tag.summary.selected");
    String tagStockSummaryPattern = MessageUtil.getMessage("stock.filter.tag.summary.stocks");
    String tagMorePattern = MessageUtil.getMessage("stock.filter.tag.summary.more");
    String allLabel = MessageUtil.getMessage("common.label.all");
    String searchLabel = MessageUtil.getMessage("common.button.search");
    String resetLabel = MessageUtil.getMessage("common.button.reset");
}

@template._components.ui.tableCard(id = "assetGrowthFragment", body = @`
    <div class="card-body">
        <div class="flex items-center gap-2 mb-2">
            <h2 class="card-title text-base-content/70 m-0">${assetGrowthTitle}</h2>
        </div>

        @template.stock.htmx.fragments.dateRangeNavBar(
            pickerName = "assetPicker",
            btnClass = "asset-range-btn",
            canPrev = canPrev,
            canNext = canNext,
            rangeMode = rangeMode,
            startDate = startLocal,
            endDate = endLocal,
            showRangeText = true,
            coveredStartDate = coveredRange.startDate(),
            coveredEndDate = coveredRange.endDate()
        )

        <%-- 핵심 보유 제외 토글. 어느 종목이 핵심인지는 '핵심' 태그가 정한다. 태그를 단 종목이 없으면 나오지 않는다.
             버튼은 폼(hidden 기간 값)과 함께 보내지고, 제외 상태는 폼의 hidden 으로 기간을 바꿔도 유지된다. --%>
        @if(coreHoldingTagged)
            !{
                String coreToggleName = coreHoldingName != null
                    ? coreHoldingName
                    : java.text.MessageFormat.format(MessageUtil.getMessage("stock.summary.core.multi"), coreHoldingCount);
                // 여럿이어도 무엇을 몇 개 빼는지 말한다: "핵심 보유 3종목 제외".
                String coreExcludeLabel = java.text.MessageFormat.format(MessageUtil.getMessage("stock.asset.growth.exclude.core"), coreToggleName);
                String coreExcludingLabel = java.text.MessageFormat.format(MessageUtil.getMessage("stock.asset.growth.exclude.core.active"), coreToggleName);
            }
            @if(coreHoldingExcluded)
                <div class="mb-2 flex flex-wrap items-center gap-2" data-exclude-core="active">
                    <span class="badge badge-warning badge-outline">${coreExcludingLabel}</span>
                    <button type="button" class="btn btn-xs btn-ghost" data-exclude-core-clear
                            hx-get="/stock/htmx/asset-growth/view" hx-include="#assetGrowthSearchForm" hx-vals='{"excludeCore": "false"}'
                            hx-target="#assetGrowthFragment" hx-select="#assetGrowthFragment" hx-swap="outerHTML">${MessageUtil.getMessage("stock.asset.growth.exclude.core.clear")}</button>
                </div>
            @else
                <div class="mb-2 flex flex-wrap items-center gap-2" data-exclude-core="available">
                    <button type="button" class="btn btn-xs btn-outline" data-exclude-core-apply
                            hx-get="/stock/htmx/asset-growth/view" hx-include="#assetGrowthSearchForm" hx-vals='{"excludeCore": "true"}'
                            hx-target="#assetGrowthFragment" hx-select="#assetGrowthFragment" hx-swap="outerHTML">${coreExcludeLabel}</button>
                    @if(coreHoldingWeightPct != null)
                        <span class="text-xs text-base-content/50">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.asset.growth.exclude.core.hint"), coreHoldingWeightPct)}</span>
                    @endif
                </div>
            @endif
        @endif

        <details class="mb-4" open data-detail-filter>
            <summary class="flex items-center gap-2 cursor-pointer select-none text-sm font-medium text-base-content/60 hover:text-base-content py-1 px-2 rounded hover:bg-base-200 w-fit transition-colors">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2a1 1 0 01-.293.707L13 13.414V19a1 1 0 01-.553.894l-4 2A1 1 0 017 21v-7.586L3.293 6.707A1 1 0 013 6V4z" /></svg>
                ${detailSearchLabel}
                @template.stock.htmx.fragments.components.filterBadge(
                    selectedAccountIds = selectedAccountIds,
                    selectedStockItemIds = selectedStockItemIds,
                    selectedStockTags = selectedStockTags
                )
            </summary>
            <form id="assetGrowthSearchForm" class="mt-2 mb-2 rounded-2xl bg-base-100 p-3 shadow-sm"
                  hx-get="/stock/htmx/asset-growth/view"
                  hx-target="#assetGrowthFragment"
                  hx-select="#assetGrowthFragment"
                  hx-swap="outerHTML"
                  hx-trigger="submit">
                <div class="stock-filter-grid">
                    <div class="form-control w-full">
                        <label class="label p-1" id="filterPeriodLabel"><span class="label-text text-xs">${periodLabel}</span></label>
                        <div class="join w-full" role="group" aria-labelledby="filterPeriodLabel">
                            <input type="date" name="assetUiStartDate" aria-label="${MessageUtil.getMessage("common.label.date.start")}" id="assetStartDateInput" class="input input-sm input-bordered join-item w-1/2" value="${startLocal != null ? startLocal.toString() : ""}">
                            <input type="date" name="assetUiEndDate" aria-label="${MessageUtil.getMessage("common.label.date.end")}" id="assetEndDateInput" class="input input-sm input-bordered join-item w-1/2" value="${endDisplayLocal != null ? endDisplayLocal.toString() : ""}">
                        </div>
                    </div>

                    <div class="form-control w-full">
                        <label class="label p-1" for="filterAccountSelect"><span class="label-text text-xs">${accountLabel}</span></label>
                        <select id="filterAccountSelect" name="accountIdList" class="select select-bordered w-full max-h-40 overflow-auto" multiple size="${Math.min(accountList != null ? accountList.size() + 1 : 1, 7)}" data-max-visible="7">
                            <option value="">${allLabel}</option>
                            @if(accountList != null)
                                @for(net.luversof.web.gate.stock.domain.Account account : accountList)
                                    <option value="${account.id().toString()}" selected="${selectedAccountIds != null && selectedAccountIds.contains(account.id())}">${account.name()}</option>
                                @endfor
                            @endif
                        </select>
                    </div>

                    <div class="form-control w-full">
                        <label class="label p-1" for="filterStockSelect"><span class="label-text text-xs">${stockNameLabel}</span></label>
                        <select id="filterStockSelect" name="stockItemIdList" class="select select-bordered w-full max-h-40 overflow-auto bg-base-100" multiple size="${Math.min(stockItemList != null ? stockItemList.size() + 1 : 1, 7)}" data-max-visible="7">
                            <option value="">${allLabel}</option>
                            @if(stockItemList != null)
                                @for(net.luversof.web.gate.stock.domain.StockItem stockItem : stockItemList)
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
                            stockLabel = stockNameLabel,
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
                                hx-get="/stock/htmx/asset-growth/view"
                                hx-target="#assetGrowthFragment"
                                hx-select="#assetGrowthFragment"
                                hx-swap="outerHTML"
                                hx-include="#assetGrowthSearchForm">${searchLabel}</button>
                            <button type="button" class="btn btn-sm text-xs"
                                hx-get="/stock/htmx/asset-growth/view"
                                hx-target="#assetGrowthFragment"
                                hx-select="#assetGrowthFragment"
                                hx-swap="outerHTML"
                                hx-include="#assetGrowthSearchForm"
                                data-selection-reset="true">${resetLabel}</button>
                        </div>
                    </div>
                </div>
                <input type="hidden" name="startDate" id="assetStartInstantInput" value="${startDate != null ? startDate.toString() : ""}">
                <input type="hidden" name="endDate" id="assetEndInstantInput" value="${endDate != null ? endDate.toString() : ""}">
                <input type="hidden" name="timeZone" id="assetTimeZoneInput" value="${timeZone != null ? timeZone : ""}">
                <input type="hidden" name="rangeMode" id="assetRangeModeInput" value="${rangeMode != null ? rangeMode : ""}">
                @if(coreHoldingExcluded)
                    <input type="hidden" name="excludeCore" value="true">
                @endif
            </form>
        </details>

        <script type="module" src="/js/stock/assetGrowth.js"></script>
        <script type="module" src="/js/stock/selectionStorage.js"></script>
        <script type="module" src="/js/stock/tagFilterChips.js"></script>

        <div class="w-full flex flex-col gap-4">
            @if(ts == null || ts.isEmpty())
                <div class="w-full flex justify-center items-center" style="height: 600px;">
                    @template._components.ui.emptyState(message = noDataLabel)
                </div>
            @else
                <p class="min-w-0 text-xs text-base-content/60">
                    <span>📈 ${chartDescriptionLabel} &bull;</span>
                    <span class="inline-block whitespace-nowrap text-info">${clickDateHoldingsLabel}</span>
                </p>
                <%-- 요약은 시리즈와 같은 호출에서 이미 계산돼 왔으므로 별도 요청 없이 바로 렌더한다. --%>
                @template.stock.htmx.fragments.assetGrowthPeriodReturnSummary(
                    fromDate = startLocal != null ? startLocal.toString() : "",
                    toDate = endDisplayLocal != null ? endDisplayLocal.toString() : "",
                    periodReturnRatePct = periodReturnRatePct,
                    returnCalculable = returnCalculable,
                    timeWeightedReturnPct = timeWeightedReturnPct,
                    periodProfit = periodProfit,
                    principalDelta = principalDelta,
                    unrealizedStart = unrealizedStart,
                    unrealizedEnd = unrealizedEnd,
                    unrealizedEndPct = unrealizedEndPct,
                    recoveredAmount = recoveredAmount,
                    netNewProfit = netNewProfit,
                    maxDrawdownPct = maxDrawdownPct,
                    maxDrawdownPeakDate = maxDrawdownPeakDate,
                    maxDrawdownTroughDate = maxDrawdownTroughDate,
                    currentDrawdownPct = currentDrawdownPct,
                    openingValue = openingValue,
                    closingValue = closingValue,
                    periodProfitRatePct = periodProfitRatePct,
                    peakValue = peakValue,
                    peakValueDate = peakValueDate,
                    troughValue = troughValue,
                    troughValueDate = troughValueDate
                )
                <div class="w-full relative" style="height: 450px;">
                    <canvas id="assetGrowthChart" class="amount-chart"></canvas>
                </div>
                <p class="text-xs text-base-content/60 text-center py-1">💰 ${profitAnalysisLabel} &mdash; <span class="text-indigo-400">■</span> ${cumulativeRealizedDatasetLabel} + <span class="text-green-400">■</span> ${cumulativeDividendLabel} = ${cumulativeTotalDatasetLabel}</p>
                <div class="w-full" style="height: 250px;">
                    <canvas id="dividendChart" class="amount-chart"></canvas>
                </div>
                <%-- 표 넷을 탭으로 묶는다. 세로로 쌓으면 아래쪽은 스크롤해야 나와서 있는 줄도 모르게
                     된다(실측 2026-09-07: 이 화면이 5,075px = 5.6 화면, 카드 9 개). 서버가 이미 다
                     그려 보낸 조각을 보이고 숨기기만 하므로 탭을 눌러도 왕복이 없다.

                     탭은 내용이 있는 것만 낸다 - 표마다 안 그리는 조건이 달라서(구간이 하나뿐이거나,
                     그 해에 아무 일도 없었거나) 빈 탭이 생기면 눌렀을 때 아무것도 안 나온다.
                     조건은 각 조각의 것과 같아야 하며, 어긋나면 아래 렌더 검사가 잡는다. --%>
                !{
                    boolean hasBreakdownPanel = (periodBreakdown != null && periodBreakdown.size() > 1)
                            || (periodBreakdownNote != null && !periodBreakdownNote.isEmpty());
                    boolean hasContributionPanel = stockContributions != null && stockContributionTotal != null
                            && stockContributions.size() + stockContributionOthersCount > 1;
                    boolean hasYearlyPanel = yearlySummaries != null && !yearlySummaries.isEmpty();
                    boolean hasCostPanel = yearlyCosts != null && !yearlyCosts.isEmpty();
                    int panelCount = (hasBreakdownPanel ? 1 : 0) + (hasContributionPanel ? 1 : 0)
                            + (hasYearlyPanel ? 1 : 0) + (hasCostPanel ? 1 : 0);
                }

                @if(panelCount > 1)
                    <%-- 탭이 하나뿐이면 고를 것이 없어 자리만 차지한다. --%>
                    <div role="tablist" class="tabs tabs-box w-full bg-base-200/70 p-1 mb-4" data-panel-tab-group="assetGrowthTables">
                        @if(hasBreakdownPanel)
                            <a role="tab" data-panel-tab="breakdown" class="tab flex-1 cursor-pointer text-base-content/60 hover:text-base-content">${MessageUtil.getMessage("stock.asset.growth.monthly.title")}</a>
                        @endif
                        @if(hasContributionPanel)
                            <a role="tab" data-panel-tab="contribution" class="tab flex-1 cursor-pointer text-base-content/60 hover:text-base-content">${MessageUtil.getMessage("stock.asset.growth.contribution.title")}</a>
                        @endif
                        @if(hasYearlyPanel)
                            <a role="tab" data-panel-tab="yearly" class="tab flex-1 cursor-pointer text-base-content/60 hover:text-base-content">${MessageUtil.getMessage("stock.asset.growth.yearly.title")}</a>
                        @endif
                        @if(hasCostPanel)
                            <a role="tab" data-panel-tab="cost" class="tab flex-1 cursor-pointer text-base-content/60 hover:text-base-content">${MessageUtil.getMessage("stock.asset.growth.cost.title")}</a>
                        @endif
                    </div>
                @endif

                <%-- 기간별 손익 - 달 단위로 쪼갠 성과. '연도별 성과' 는 해 단위라, 짧은 구간을 고르면
                     한 줄로 끝나 아무것도 말해 주지 않는다(실측 2026-09-01 '올해': 연도별 1행 vs 월별 8행). --%>
                <div data-panel-group="assetGrowthTables" data-panel="breakdown">
                    @template.stock.htmx.fragments.periodBreakdownTable(periodBreakdown = periodBreakdown, periodBreakdownNote = periodBreakdownNote)
                </div>

                <%-- 종목별 기여 - 같은 기간 손익을 시간이 아니라 종목으로 쪼갠 것이다.
                     그 기간에 다 판 종목까지 들어오므로, 자산 현황(보유 중인 것만)이
                     답하지 못하던 자리를 여기서 채운다. --%>
                <div data-panel-group="assetGrowthTables" data-panel="contribution">
                    @template.stock.htmx.fragments.stockContributionTable(
                        stockContributions = stockContributions,
                        stockContributionTotal = stockContributionTotal,
                        stockContributionOthers = stockContributionOthers,
                        stockContributionOthersCount = stockContributionOthersCount
                    )
                </div>

                <%-- 연도별 성과: 시리즈와 같은 시뮬레이션에서 함께 계산돼 온다(추가 호출 없음). --%>
                <div data-panel-group="assetGrowthTables" data-panel="yearly">
                    @template.stock.htmx.fragments.assetGrowthYearlySummary(yearlySummaries = yearlySummaries)
                </div>

                <%-- 연도별 세금·비용 - 원장에는 다 있는데 합계를 내는 화면이 없던 값들이다. --%>
                <div data-panel-group="assetGrowthTables" data-panel="cost">
                    @template.stock.htmx.fragments.yearlyCostSummary(yearlyCosts = yearlyCosts)
                </div>
                <div id="holdings-snapshot-container"></div>
                <%-- 매매 이력은 이 응답과 같은 기간으로 서버에서 함께 계산해 넣는다(추가 왕복 없음).
                     페이징/정렬은 그대로 /stock/htmx/trade-history 를 다시 불러 이 패널만 교체한다. --%>
                @if(tradeHistoryData != null)
                    @template.stock.htmx.tradeHistory(
                        zone = zone,
                        trades = tradeHistoryData.trades(),
                        totalItems = tradeHistoryData.totalItems(),
                        currentPage = tradeHistoryData.currentPage(),
                        totalPages = tradeHistoryData.totalPages(),
                        pageSize = tradeHistoryData.pageSize(),
                        from = tradeHistoryData.from(),
                        to = tradeHistoryData.to(),
                        totalBuy = tradeHistoryData.totalBuy(),
                        totalSell = tradeHistoryData.totalSell(),
                        totalRealizedProfit = tradeHistoryData.totalRealizedProfit(),
                        totalFee = tradeHistoryData.totalFee(),
                        totalTax = tradeHistoryData.totalTax(),
                        tradePeriod = tradeHistoryData.tradePeriod(),
                        pagination = tradeHistoryData.pagination(),
                        tradeHistoryFilterQuery = tradeHistoryFilterQuery)
                @else
                    <div id="trade-history-panel"
                         hx-get="/stock/htmx/trade-history?from=${startLocal != null ? startLocal.toString() : ""}&to=${endDisplayLocal != null ? endDisplayLocal.toString() : ""}${tradeHistoryFilterQuery}"
                         hx-trigger="load"
                         hx-swap="outerHTML"></div>
                @endif
            @endif
        </div>
    </div>

    <%-- 서버가 아는 것만 넘긴다(타임존 · 선택기 minDate · 라벨). 동작은 /js/stock/assetGrowth.js 에 있다
     (2026-09-08 까지 여기 인라인 스크립트 둘, 308 줄). 시계열 배열은 아래 @if 안의 블록이 싣는다. --%>
<script nonce="${CspNonceHolder.getNonce()}">
    window.assetGrowthConfig = {
        snapshotTz: '${timeZone != null ? timeZone : ""}',
        minDate: '${dataFirstDate != null ? dataFirstDate : ""}',
        series: null,
        labels: {
            totalAsset: '${totalAssetDatasetLabel}',
            principal: '${principalLabel}',
            totalAssetAxis: '${totalAssetAxisLabel}',
            unrealizedProfit: '${unrealizedProfitLabel}',
            rangeMax: '${rangeMaxLabel}',
            rangeMin: '${rangeMinLabel}',
            clickTradeHistory: '${clickTradeHistoryLabel}',
            tradeCount: '${tradeCountLabel}',
            dailyRealizedProfit: '${dailyRealizedProfitLabel}',
            cumulativeRealized: '${cumulativeRealizedDatasetLabel}',
            cumulativeTotal: '${cumulativeTotalDatasetLabel}',
            cumulativeDividend: '${cumulativeDividendLabel}',
            cumulativeProfitAxis: '${cumulativeProfitAxisLabel}'
        }
    };
</script>

    @if(ts != null && !ts.isEmpty())
    <script nonce="${CspNonceHolder.getNonce()}">
        (function() {
            var labels = [];
            var totalValueData = [];
            var totalCostData = [];
            var cumulativeRealizedProfitData = [];
            var cumulativeDividendData = [];
            var tradeCountData = [];
            var buyCountData = [];
            var dailyRealizedProfitData = [];

            @for(Object _pt : ts)
            !{ TradeProfitTimeSeriesPoint pt = (TradeProfitTimeSeriesPoint) _pt; }
                labels.push("$unsafe{formatter.format(pt.timestamp())}");
                totalValueData.push($unsafe{pt.totalHoldingsValue() != null ? pt.totalHoldingsValue().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0"});
                totalCostData.push($unsafe{pt.totalHoldingsCost() != null ? pt.totalHoldingsCost().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0"});
                cumulativeRealizedProfitData.push($unsafe{pt.cumulativeRealizedProfit() != null ? pt.cumulativeRealizedProfit().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0"});
                cumulativeDividendData.push($unsafe{pt.cumulativeDividend() != null ? pt.cumulativeDividend().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0"});
                tradeCountData.push($unsafe{String.valueOf(pt.tradeCount())});
                buyCountData.push($unsafe{String.valueOf(pt.buyCount())});
                dailyRealizedProfitData.push($unsafe{pt.dailyRealizedProfit() != null ? pt.dailyRealizedProfit().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0"});
            @endfor
            window.assetGrowthConfig.series = {
                labels: labels, totalValueData: totalValueData, totalCostData: totalCostData,
                cumulativeRealizedProfitData: cumulativeRealizedProfitData, cumulativeDividendData: cumulativeDividendData,
                tradeCountData: tradeCountData, buyCountData: buyCountData, dailyRealizedProfitData: dailyRealizedProfitData
            };
        })();
    </script>
    @endif
`)
