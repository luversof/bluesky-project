@import java.math.BigDecimal
@import java.math.RoundingMode
@import java.util.List
@import java.util.Map
@import net.luversof.web.gate.stock.domain.TradeProfit

@param Map<String, TradeProfit> accountRealizedMap
@param List<TradeProfit> stockRealizedList
@param BigDecimal totalRealizedProfit
@param BigDecimal totalSellProceeds
@param BigDecimal totalFees
@param double winRate
@param TradeProfit bestStock
@param TradeProfit worstStock
@param List<String> chartProfitLabels
@param List<Long> chartProfitValues
@param List<String> chartLossLabels
@param List<Long> chartLossValues
@param String rangeMode = null
@param String dataFirstDate = null
@param java.time.Instant startDate = null
@param java.time.Instant endDate = null
@param String timeZone = null
@param java.util.List<net.luversof.web.gate.stock.domain.Account> accountList = null
@param java.util.List<net.luversof.web.gate.stock.domain.StockItem> stockItemList = null
@param java.util.UUID selectedAccountId = null
@param java.util.UUID selectedStockItemId = null
@param String debugTradeAccountIds = null
@param Integer debugFinalAccountListSize = null
@param Integer debugFinalStockItemListSize = null

<%-- 수익률 기준: 매도한 주식의 취득원가 = 매도수령액 - 실현손익 --%>
!{BigDecimal totalSoldCost = totalSellProceeds != null && totalRealizedProfit != null
    ? totalSellProceeds.subtract(totalRealizedProfit)
    : BigDecimal.ZERO;}
!{double totalRate = totalSoldCost.compareTo(BigDecimal.ZERO) > 0 && totalRealizedProfit != null
    ? totalRealizedProfit.doubleValue() / totalSoldCost.doubleValue() * 100
    : 0.0;}

<div id="realizedProfitFragment" class="flex flex-col gap-6">
    <!-- DEBUG: tradeAccountIds=${debugTradeAccountIds} finalAccountListSize=${debugFinalAccountListSize} finalStockItemListSize=${debugFinalStockItemListSize} -->

    !{
        boolean isAllMode = "all".equals(rangeMode);
        java.time.ZoneId zone = (timeZone != null && !timeZone.isEmpty())
            ? java.time.ZoneId.of(timeZone)
            : java.time.ZoneId.systemDefault();
        java.time.LocalDate startLocal = startDate != null ? startDate.atZone(zone).toLocalDate() : null;
        java.time.LocalDate endLocal = endDate != null ? endDate.atZone(zone).toLocalDate() : null;
        java.time.LocalDate today = java.time.LocalDate.now(zone);
        boolean canNext = !isAllMode && endLocal != null && endLocal.isBefore(today);
        boolean canPrev = !isAllMode && startLocal != null && dataFirstDate != null && !dataFirstDate.isEmpty()
            && startLocal.isAfter(java.time.LocalDate.parse(dataFirstDate));
    }
    @template.stock.htmx.fragments.dateRangeNavBar(
        pickerName = "realizedProfitPicker",
        btnClass = "date-range-btn",
        canPrev = canPrev,
        canNext = canNext,
        rangeMode = rangeMode,
        startDate = startLocal,
        endDate = endLocal
    )

    <details class="mb-4"$unsafe{startDate != null ? " open" : ""}>
        <summary class="flex items-center gap-2 cursor-pointer select-none text-sm font-medium text-base-content/60 hover:text-base-content py-1 px-2 rounded hover:bg-base-200 w-fit transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2a1 1 0 01-.293.707L13 13.414V19a1 1 0 01-.553.894l-4 2A1 1 0 017 21v-7.586L3.293 6.707A1 1 0 013 6V4z" /></svg>
            상세 검색
            @template.stock.htmx.fragments.components.filterBadge(
                startDate = startDate,
                endDate = endDate
            )
        </summary>
    <form id="realizedProfitSearchForm" class="flex flex-wrap gap-2 mt-2 mb-2 items-end bg-base-100 p-2 rounded-box shadow-sm"
          hx-get="/stock/htmx/realized-profit"
          hx-target="#realizedProfitFragment"
          hx-select="#realizedProfitFragment"
          hx-swap="outerHTML"
          hx-trigger="submit">
        <div class="form-control w-full sm:w-auto">
            <label class="label p-1"><span class="label-text text-xs">기간</span></label>
            <div class="join">
                <input type="date" name="realizedUiStartDate" id="realizedStartDateInput" class="input input-sm input-bordered join-item" value="${startDate != null ? startDate.atZone(zone).toLocalDate().toString() : ""}">
                <input type="date" name="realizedUiEndDate" id="realizedEndDateInput" class="input input-sm input-bordered join-item" value="${endDate != null ? endDate.atZone(zone).toLocalDate().minusDays(1).toString() : ""}">
            </div>
        </div>
            <div class="form-control w-full sm:w-auto">
                <label class="label p-1"><span class="label-text text-xs">계좌</span></label>
                <select name="accountIdList" class="select-bordered w-full max-h-52 overflow-auto" multiple data-max-visible="8">
                    <option value="">전체</option>
                    @if(accountList != null)
                        @for(net.luversof.web.gate.stock.domain.Account account : accountList)
                            <option value="${account.id().toString()}" selected="${selectedAccountId != null && account.id().equals(selectedAccountId)}">${account.name()}</option>
                        @endfor
                    @endif
                </select>
            </div>

            <div class="form-control w-full sm:w-auto">
                <label class="label p-1"><span class="label-text text-xs">종목</span></label>
                <select name="stockItemIdList" class="select-bordered w-full max-h-52 overflow-auto" multiple data-max-visible="8">
                    <option value="">전체</option>
                    @if(stockItemList != null)
                        @for(net.luversof.web.gate.stock.domain.StockItem stockItem : stockItemList)
                            <option value="${stockItem.id().toString()}" selected="${selectedStockItemId != null && stockItem.id().equals(selectedStockItemId)}">${stockItem.name()}</option>
                        @endfor
                    @endif
                </select>
            </div>
        <input type="hidden" name="startDate" id="realizedStartInstantInput" value="${startDate != null ? startDate.toString() : ""}">
        <input type="hidden" name="endDate" id="realizedEndInstantInput" value="${endDate != null ? endDate.toString() : ""}">
        <input type="hidden" name="timeZone" id="realizedTimeZoneInput" value="${timeZone != null ? timeZone : ""}">
        <input type="hidden" name="rangeMode" id="realizedRangeModeInput" value="${rangeMode != null ? rangeMode : ""}">
        <button type="button" class="btn btn-sm btn-primary" data-selection-save="true"
            hx-get="/stock/htmx/realized-profit"
            hx-target="#realizedProfitFragment"
            hx-select="#realizedProfitFragment"
            hx-swap="outerHTML"
            hx-include="#realizedProfitSearchForm">조회</button>
        <button type="button" class="btn btn-sm text-xs"
            hx-get="/stock/htmx/realized-profit"
            hx-target="#realizedProfitFragment"
            hx-select="#realizedProfitFragment"
                hx-swap="outerHTML"
                hx-include="#realizedProfitSearchForm"
            data-selection-reset="true">초기화</button>
    </form>
    </details>

    <script type="module" src="/js/stock/selectionStorage.js"></script>

    <!-- Section 0: 요약 카드 -->
    <div class="stats stats-vertical lg:stats-horizontal shadow w-full bg-base-100">

        <div class="stat">
            <div class="stat-figure text-primary">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="inline-block w-8 h-8 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"></path></svg>
            </div>
            <div class="stat-title">총 실현손익 (순)</div>
            <div class="stat-value font-bold amount-value ${totalRealizedProfit != null && totalRealizedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                ${String.format("%+,d", totalRealizedProfit != null ? totalRealizedProfit.longValue() : 0)}
            </div>
            <div class="stat-desc mt-1 opacity-60">
                매도 원가: <span class="amount-value">${String.format("%,d", totalSoldCost.longValue())}</span>
            </div>
        </div>

        @if(bestStock != null)
        !{double bestRate = bestStock.totalSellProceeds() != null && bestStock.realizedProfitNet() != null
            && bestStock.totalSellProceeds().subtract(bestStock.realizedProfitNet()).compareTo(BigDecimal.ZERO) > 0
            ? bestStock.realizedProfitNet().doubleValue() / bestStock.totalSellProceeds().subtract(bestStock.realizedProfitNet()).doubleValue() * 100
            : 0.0;}
        <div class="stat">
            <div class="stat-figure text-success">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="inline-block w-8 h-8 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"></path></svg>
            </div>
            <div class="stat-title">최고 수익 종목</div>
            <div class="stat-value text-base font-bold text-profit">
                ${bestStock.stockItemName() != null ? bestStock.stockItemName() : "-"}
            </div>
            <div class="stat-desc font-semibold mt-1 text-profit">
                <span class="amount-value">${String.format("%+,d", bestStock.realizedProfitNet() != null ? bestStock.realizedProfitNet().longValue() : 0)}</span>
                <span class="ml-1">(${String.format("%+.1f%%", bestRate)})</span>
            </div>
        </div>
        @endif

        @if(worstStock != null)
        !{double worstRate = worstStock.totalSellProceeds() != null && worstStock.realizedProfitNet() != null
            && worstStock.totalSellProceeds().subtract(worstStock.realizedProfitNet()).compareTo(BigDecimal.ZERO) > 0
            ? worstStock.realizedProfitNet().doubleValue() / worstStock.totalSellProceeds().subtract(worstStock.realizedProfitNet()).doubleValue() * 100
            : 0.0;}
        <div class="stat">
            <div class="stat-figure text-error">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="inline-block w-8 h-8 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 17h8m0 0V9m0 8l-8-8-4 4-6-6"></path></svg>
            </div>
            <div class="stat-title">최대 손실 종목</div>
            <div class="stat-value text-base font-bold text-loss">
                ${worstStock.stockItemName() != null ? worstStock.stockItemName() : "-"}
            </div>
            <div class="stat-desc font-semibold mt-1 text-loss">
                <span class="amount-value">${String.format("%+,d", worstStock.realizedProfitNet() != null ? worstStock.realizedProfitNet().longValue() : 0)}</span>
                <span class="ml-1">(${String.format("%+.1f%%", worstRate)})</span>
            </div>
        </div>
        @endif

        <div class="stat">
            <div class="stat-figure text-warning">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="inline-block w-8 h-8 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z"></path></svg>
            </div>
            <div class="stat-title">총 거래비용 (수수료+세금)</div>
            <div class="stat-value text-base font-bold amount-value text-warning">
                ${String.format("%,d", totalFees != null ? totalFees.longValue() : 0)}
            </div>
            <div class="stat-desc mt-1 opacity-70">매수·매도 수수료 + 거래세</div>
        </div>

        <div class="stat">
            <div class="stat-figure text-accent">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="inline-block w-8 h-8 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z"></path></svg>
            </div>
            <div class="stat-title">수익 종목 비율</div>
            <div class="stat-value font-bold text-accent">${String.format("%.1f%%", winRate)}</div>
            <div class="stat-desc mt-1">
                ${(long)(stockRealizedList != null ? stockRealizedList.stream().filter(tp -> tp.realizedProfitNet() != null && tp.realizedProfitNet().compareTo(BigDecimal.ZERO) > 0).count() : 0)}승 /
                ${stockRealizedList != null ? stockRealizedList.size() : 0}종목
            </div>
        </div>

    </div>

    <!-- Section 1: 수익 Top5 / 손실 Worst5 차트 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">

        @if(chartProfitLabels != null && !chartProfitLabels.isEmpty())
        <div class="card bg-base-100 shadow-md">
            <div class="card-body p-4">
                <h2 class="text-base font-semibold mb-2 text-profit">수익 Top ${chartProfitLabels.size()}</h2>
                <div class="relative w-full amount-chart" style="height: ${Math.max(160, chartProfitLabels.size() * 40)}px;">
                    <canvas id="chartProfit"></canvas>
                </div>
            </div>
        </div>
        <script>
        (function() {
            var labels = [];
            @for(String lbl : chartProfitLabels)
                labels.push("${lbl.replace("\"", "\\\"").replace("\\", "\\\\")} ");
            @endfor
            var values = [];
            @for(Long val : chartProfitValues)
                values.push($unsafe{String.valueOf(val)});
            @endfor
            var config = {
                type: 'bar',
                data: {
                    labels: labels,
                    datasets: [{ label: '실현손익', data: values,
                        backgroundColor: 'rgba(34,197,94,0.7)', borderColor: 'rgb(34,197,94)',
                        borderWidth: 1, borderRadius: 4 }]
                },
                options: {
                    indexAxis: 'y', responsive: true, maintainAspectRatio: false,
                    plugins: { legend: { display: false },
                        tooltip: { callbacks: { label: function(c) {
                            return ' +' + c.parsed.x.toLocaleString() + '원'; } } } },
                    scales: {
                        x: { ticks: { callback: function(v) {
                            return v.toLocaleString(); } } },
                        y: { ticks: { font: { size: 12 } } }
                    }
                }
            };
            window.ensureStockCharts(function() { window['chartProfitInstance'] = window.StockCharts.createChart('chartProfit', config, window['chartProfitInstance']); });
        })();
        </script>
        @endif

        @if(chartLossLabels != null && !chartLossLabels.isEmpty())
        <div class="card bg-base-100 shadow-md">
            <div class="card-body p-4">
                <h2 class="text-base font-semibold mb-2 text-loss">손실 Worst ${chartLossLabels.size()}</h2>
                <div class="relative w-full amount-chart" style="height: ${Math.max(160, chartLossLabels.size() * 40)}px;">
                    <canvas id="chartLoss"></canvas>
                </div>
            </div>
        </div>
        <script>
        (function() {
            var labels = [];
            @for(String lbl : chartLossLabels)
                labels.push("${lbl.replace("\"", "\\\"").replace("\\", "\\\\")} ");
            @endfor
            var values = [];
            @for(Long val : chartLossValues)
                values.push($unsafe{String.valueOf(val)});
            @endfor
            var config = {
                type: 'bar',
                data: {
                    labels: labels,
                    datasets: [{ label: '실현손익', data: values,
                        backgroundColor: 'rgba(239,68,68,0.7)', borderColor: 'rgb(239,68,68)',
                        borderWidth: 1, borderRadius: 4 }]
                },
                options: {
                    indexAxis: 'y', responsive: true, maintainAspectRatio: false,
                    plugins: { legend: { display: false },
                        tooltip: { callbacks: { label: function(c) {
                            return ' ' + c.parsed.x.toLocaleString() + '원'; } } } },
                    scales: {
                        x: { ticks: { callback: function(v) {
                            return v.toLocaleString(); } } },
                        y: { ticks: { font: { size: 12 } } }
                    }
                }
            };
            window.ensureStockCharts(function() { window['chartLossInstance'] = window.StockCharts.createChart('chartLoss', config, window['chartLossInstance']); });
        })();
        </script>
        @endif

    </div>

    <!-- Section 2: 계좌별 실현손익 -->
    <div>
        <h2 class="text-lg font-semibold mb-3 opacity-70">계좌별 실현손익</h2>
        <div class="card bg-base-100 shadow-md overflow-x-auto">
            <table id="accountRealizedTable" class="table table-sm w-full">
                <thead class="bg-base-200">
                    <tr>
                        <th class="cursor-pointer select-none" onclick="sortAccountTable(0)" title="계좌명 정렬">
                            계좌명 <span id="acc-sort-icon-0" class="opacity-40">↕</span>
                        </th>
                        <th class="text-right cursor-pointer select-none" onclick="sortAccountTable(1)" title="실현손익 정렬">
                            실현손익 <span id="acc-sort-icon-1" class="opacity-40">↕</span>
                        </th>
                        <th class="text-right hidden sm:table-cell cursor-pointer select-none" onclick="sortAccountTable(2)" title="수익률 정렬">
                            수익률 <span id="acc-sort-icon-2" class="opacity-40">↕</span>
                        </th>
                        <th class="text-right hidden sm:table-cell">평가손익</th>
                    </tr>
                </thead>
                <tbody id="accountRealizedTbody">
                !{int[] accIdx = {0};}
                @for(Map.Entry<String, TradeProfit> entry : accountRealizedMap.entrySet())
                    !{accIdx[0]++;}
                    !{TradeProfit acc = entry.getValue();}
                    !{double accRate = acc.totalBuyCost() != null && acc.totalBuyCost().compareTo(BigDecimal.ZERO) > 0 && acc.realizedProfit() != null
                        ? acc.realizedProfit().doubleValue() / acc.totalBuyCost().doubleValue() * 100
                        : 0.0;}
                    <tr class="hover account-row" data-initial="${accIdx[0]}">
                        <td data-val="${entry.getKey()}">
                            <div class="font-semibold">${entry.getKey()}</div>
                            <div class="text-xs opacity-50">주식</div>
                        </td>
                        <td class="text-right font-bold font-mono amount-value ${acc.realizedProfit() != null && acc.realizedProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}"
                            data-val="$unsafe{acc.realizedProfit() != null ? acc.realizedProfit().toPlainString() : "0"}">
                            ${String.format("%+,d", acc.realizedProfit() != null ? acc.realizedProfit().longValue() : 0)}
                        </td>
                        <td class="text-right font-mono hidden sm:table-cell font-semibold ${accRate >= 0 ? "text-profit" : "text-loss"}"
                            data-val="$unsafe{String.format("%.6f", accRate)}">
                            ${String.format("%+.2f%%", accRate)}
                        </td>
                        <td class="text-right font-mono hidden sm:table-cell amount-value ${acc.evaluationProfit() != null && acc.evaluationProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                            ${String.format("%+,d", acc.evaluationProfit() != null ? acc.evaluationProfit().longValue() : 0)}
                        </td>
                    </tr>
                @endfor
                </tbody>
                <tfoot class="bg-base-200 font-bold">
                    !{BigDecimal totalAccountRealized = accountRealizedMap.values().stream()
                        .map(acc -> acc.realizedProfit() != null ? acc.realizedProfit() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);}
                    <tr>
                        <td>합계</td>
                        <td class="text-right font-mono amount-value ${totalAccountRealized.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                            ${String.format("%+,d", totalAccountRealized.longValue())}
                        </td>
                        <td class="hidden sm:table-cell"></td>
                        <td class="hidden sm:table-cell"></td>
                    </tr>
                </tfoot>
            </table>
        </div>
    </div>

    <!-- Section 3: 종목별 실현손익 -->
    <div>
        <div class="flex items-center justify-between mb-3">
            <h2 class="text-lg font-semibold opacity-70">종목별 실현손익
                <span class="text-xs font-normal opacity-60">(실현손익 발생 종목 · 전체 계좌 합산)</span>
            </h2>
            @if(stockRealizedList != null && stockRealizedList.size() > 10)
            <button id="toggleStockTable" data-expanded="false" onclick="toggleStockRows()"
                class="btn btn-xs btn-ghost opacity-70">
                전체 ${stockRealizedList.size()}개 보기 ▼
            </button>
            @endif
        </div>
        <div class="card bg-base-100 shadow-md overflow-x-auto">
            <table id="stockRealizedTable" class="table table-sm table-zebra w-full">
                <thead class="bg-base-200">
                    <tr>
                        <th class="cursor-pointer select-none" onclick="sortStockTable(0)" title="종목명 정렬">
                            종목명 <span id="sort-icon-0" class="opacity-40">↕</span>
                        </th>
                        <th class="text-right cursor-pointer select-none" onclick="sortStockTable(1)" title="실현손익 정렬">
                            실현손익 <span id="sort-icon-1" class="opacity-40">↕</span>
                        </th>
                        <th class="text-right hidden sm:table-cell cursor-pointer select-none" onclick="sortStockTable(2)" title="수익률 정렬">
                            수익률 <span id="sort-icon-2" class="opacity-40">↕</span>
                        </th>
                        <th class="text-right hidden md:table-cell cursor-pointer select-none" onclick="sortStockTable(3)" title="매도분 원가 정렬">
                            매도분 원가 <span id="sort-icon-3" class="opacity-40">↕</span>
                        </th>
                        <th class="text-right hidden lg:table-cell">총 거래비용</th>
                        <th class="text-right hidden xl:table-cell">매도수량</th>
                    </tr>
                </thead>
                <tbody id="stockRealizedTbody">
                    @if(stockRealizedList == null || stockRealizedList.isEmpty())
                        <tr>
                            <td colspan="6" class="text-center py-8 opacity-50">실현손익 내역이 없습니다.</td>
                        </tr>
                    @else
                        !{int[] rowIdx = {0};}
                        @for(TradeProfit item : stockRealizedList)
                            !{rowIdx[0]++;}
                            !{BigDecimal itemFees = BigDecimal.ZERO
                                .add(item.totalBuyFee() != null ? item.totalBuyFee() : BigDecimal.ZERO)
                                .add(item.totalSellFee() != null ? item.totalSellFee() : BigDecimal.ZERO)
                                .add(item.totalSellTax() != null ? item.totalSellTax() : BigDecimal.ZERO);}
                            !{BigDecimal itemSoldCost = item.totalSellProceeds() != null && item.realizedProfitNet() != null
                                ? item.totalSellProceeds().subtract(item.realizedProfitNet())
                                : BigDecimal.ZERO;}
                            !{double itemRate = itemSoldCost.compareTo(BigDecimal.ZERO) > 0 && item.realizedProfitNet() != null
                                ? item.realizedProfitNet().doubleValue() / itemSoldCost.doubleValue() * 100
                                : 0.0;}
                            
                            <tr class="hover stock-row" data-initial="${rowIdx[0]}">
                                <td data-val="${item.stockItemName() != null ? item.stockItemName() : ""}">
                                    <div class="font-semibold">${item.stockItemName() != null ? item.stockItemName() : "-"}</div>
                                    <div class="flex items-center gap-1 mt-0.5">
                                        @if(item.holdingQuantity() == 0)
                                            <span class="badge badge-xs badge-ghost opacity-60">전량매도</span>
                                        @else
                                            <span class="badge badge-xs badge-outline opacity-60">보유 ${item.holdingQuantity()}주</span>
                                        @endif
                                    </div>
                                </td>
                                <td class="text-right" data-val="$unsafe{item.realizedProfitNet() != null ? item.realizedProfitNet().toPlainString() : "0"}">
                                    <div class="font-bold font-mono amount-value ${item.realizedProfitNet() != null && item.realizedProfitNet().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                                        ${String.format("%+,d", item.realizedProfitNet() != null ? item.realizedProfitNet().longValue() : 0)}
                                    </div>
                                    
                                </td>
                                <td class="text-right font-mono hidden sm:table-cell font-semibold ${itemRate >= 0 ? "text-profit" : "text-loss"}"
                                    data-val="$unsafe{String.format("%.6f", itemRate)}">
                                    ${String.format("%+.2f%%", itemRate)}
                                </td>
                                <td class="text-right font-mono hidden md:table-cell amount-value opacity-70"
                                    data-val="$unsafe{itemSoldCost.toPlainString()}">
                                    ${String.format("%,d", itemSoldCost.longValue())}
                                </td>
                                <td class="text-right font-mono hidden lg:table-cell amount-value text-warning opacity-80">
                                    ${String.format("%,d", itemFees.longValue())}
                                </td>
                                <td class="text-right font-mono hidden xl:table-cell opacity-70">
                                    ${item.totalSellQuantity()}주
                                </td>
                            </tr>
                        @endfor
                    @endif
                </tbody>
                <tfoot class="bg-base-200 font-bold">
                    !{BigDecimal totalStockFees = stockRealizedList != null
                        ? stockRealizedList.stream().map(i -> {
                            BigDecimal f = BigDecimal.ZERO;
                            if (i.totalBuyFee() != null) f = f.add(i.totalBuyFee());
                            if (i.totalSellFee() != null) f = f.add(i.totalSellFee());
                            if (i.totalSellTax() != null) f = f.add(i.totalSellTax());
                            return f;
                          }).reduce(BigDecimal.ZERO, BigDecimal::add)
                        : BigDecimal.ZERO;}
                    <tr>
                        <td>합계</td>
                        <td class="text-right font-mono amount-value ${totalRealizedProfit != null && totalRealizedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                            ${String.format("%+,d", totalRealizedProfit != null ? totalRealizedProfit.longValue() : 0)}
                        </td>
                        <td class="hidden sm:table-cell"></td>
                        <td class="text-right font-mono hidden md:table-cell amount-value opacity-70">
                            ${String.format("%,d", totalSoldCost.longValue())}
                        </td>
                        <td class="text-right font-mono hidden lg:table-cell amount-value text-warning opacity-80">
                            ${String.format("%,d", totalStockFees.longValue())}
                        </td>
                        <td class="hidden xl:table-cell"></td>
                    </tr>
                </tfoot>
            </table>
        </div>
    </div>

    <script>
    (function() {
        var sortState = { col: -1, asc: false };
        var INITIAL_SHOW = 10;

        function applyVisibility() {
            var rows = document.querySelectorAll('#stockRealizedTbody .stock-row');
            var btn = document.getElementById('toggleStockTable');
            var expanded = btn && btn.dataset.expanded === 'true';
            rows.forEach(function(r, i) {
                r.classList.toggle('hidden', !expanded && i >= INITIAL_SHOW);
            });
        }

        window.toggleStockRows = function() {
            var btn = document.getElementById('toggleStockTable');
            if (!btn) return;
            var expanded = btn.dataset.expanded === 'true';
            btn.dataset.expanded = expanded ? 'false' : 'true';
            var total = document.querySelectorAll('#stockRealizedTbody .stock-row').length;
            btn.textContent = (!expanded) ? '접기 ▲' : '전체 ' + total + '개 보기 ▼';
            applyVisibility();
        };

        window.sortStockTable = function(colIdx) {
            var tbody = document.getElementById('stockRealizedTbody');
            if (!tbody) return;
            var rows = Array.from(tbody.querySelectorAll('.stock-row'));
            var asc;
            if (sortState.col === colIdx) {
                if (sortState.asc === false) { asc = true; }
                else { asc = null; } // 세 번째 클릭: 원래 순서 복원
            } else {
                asc = false; // 첫 클릭: 내림차순
            }
            // 아이콘 초기화
            [0,1,2,3].forEach(function(i) {
                var el = document.getElementById('sort-icon-' + i);
                if (el) { el.textContent = '↕'; el.className = 'opacity-40'; }
            });
            if (asc === null) {
                // 원래 순서
                rows.sort(function(a, b) {
                    return parseInt(a.dataset.initial) - parseInt(b.dataset.initial);
                });
                sortState = { col: -1, asc: false };
            } else {
                var icon = document.getElementById('sort-icon-' + colIdx);
                if (icon) { icon.textContent = asc ? '↑' : '↓'; icon.className = 'opacity-80'; }
                rows.sort(function(a, b) {
                    var tds_a = a.querySelectorAll('td');
                    var tds_b = b.querySelectorAll('td');
                    var va = tds_a[colIdx] ? tds_a[colIdx].dataset.val || '' : '';
                    var vb = tds_b[colIdx] ? tds_b[colIdx].dataset.val || '' : '';
                    if (colIdx === 0) {
                        return asc ? va.localeCompare(vb, 'ko') : vb.localeCompare(va, 'ko');
                    }
                    return asc ? parseFloat(va) - parseFloat(vb) : parseFloat(vb) - parseFloat(va);
                });
                sortState = { col: colIdx, asc: asc };
            }
            rows.forEach(function(r) { tbody.appendChild(r); });
            applyVisibility();
        };

        // 초기 접기 상태 적용
        applyVisibility();
    })();

    var accSortState = { col: -1, asc: false };

    window.sortAccountTable = function(colIdx) {
        var tbody = document.getElementById('accountRealizedTbody');
        if (!tbody) return;
        var rows = Array.from(tbody.querySelectorAll('.account-row'));
        var asc;
        if (accSortState.col === colIdx) {
            if (accSortState.asc === false) { asc = true; }
            else { asc = null; }
        } else {
            asc = false;
        }
        [0,1,2].forEach(function(i) {
            var el = document.getElementById('acc-sort-icon-' + i);
            if (el) { el.textContent = '↕'; el.className = 'opacity-40'; }
        });
        if (asc === null) {
            rows.sort(function(a, b) {
                return parseInt(a.dataset.initial) - parseInt(b.dataset.initial);
            });
            accSortState = { col: -1, asc: false };
        } else {
            var icon = document.getElementById('acc-sort-icon-' + colIdx);
            if (icon) { icon.textContent = asc ? '↑' : '↓'; icon.className = 'opacity-80'; }
            rows.sort(function(a, b) {
                var tds_a = a.querySelectorAll('td');
                var tds_b = b.querySelectorAll('td');
                var va = tds_a[colIdx] ? tds_a[colIdx].dataset.val || '' : '';
                var vb = tds_b[colIdx] ? tds_b[colIdx].dataset.val || '' : '';
                if (colIdx === 0) {
                    return asc ? va.localeCompare(vb, 'ko') : vb.localeCompare(va, 'ko');
                }
                return asc ? parseFloat(va) - parseFloat(vb) : parseFloat(vb) - parseFloat(va);
            });
            accSortState = { col: colIdx, asc: asc };
        }
        rows.forEach(function(r) { tbody.appendChild(r); });
    };
    </script>

    <script>
    (function() {
        var realizedProfitPicker = DateRangePicker.create({
            formId:      'realizedProfitSearchForm',
            startId:     'realizedStartDateInput',
            endId:       'realizedEndDateInput',
            instantStartId: 'realizedStartInstantInput',
            instantEndId: 'realizedEndInstantInput',
            timeZoneId:  'realizedTimeZoneInput',
            rangeModeId: 'realizedRangeModeInput',
            btnClass:    'date-range-btn',
            minDate:     '${dataFirstDate != null ? dataFirstDate : ""}',
            globalKey:   'globalDateRange',
            rootSelector: '#realizedProfitFragment'
        });
        window.realizedProfitPicker = realizedProfitPicker;
        // Prevent infinite auto-submit loops: if the DateRangePicker already
        // recorded a one-time apply for this form and it matches current
        // globalDateRange, skip auto-apply. Otherwise continue and re-apply.
        try {
            var __appliedKey = '__globalDateRangeApplied:' + ('realizedProfitSearchForm');
            var __existingApplied = null;
            try { __existingApplied = sessionStorage.getItem(__appliedKey); } catch(e) {}
            var __currentGlobal = null;
            try { __currentGlobal = sessionStorage.getItem('globalDateRange'); } catch(e) {}
            if (__existingApplied && __currentGlobal && __existingApplied === __currentGlobal) {
                try { console.debug('[realized-profit] appliedKey matches current global, skipping auto-apply'); } catch(e) {}
                return;
            }
        } catch(e) {}
        // 기본값: 페이지 진입 시 기간 미지정이면 '이번달(mtd)'을 선택 (한번만 적용하도록 세션 가드)
        try {
            if (window.realizedProfitPicker && typeof window.realizedProfitPicker.getState === 'function') {
                var __s = window.realizedProfitPicker.getState();
                if ((!__s || !__s.mode) && (!__s || !__s.start) && (!__s || !__s.end)) {
                    var __key = 'dateRangeDefaultApplied:realizedProfitPicker';
                    try {
                        if (!sessionStorage.getItem(__key)) {
                                var _btn = document.querySelector('#realizedProfitFragment .date-range-btn');
                                window.realizedProfitPicker.set('mtd', _btn || null);
                                sessionStorage.setItem(__key, '1');
                            }
                    } catch(e) { window.realizedProfitPicker.set('mtd', null); }
                }
            }
        } catch(e) {}

        // Ensure any globally requested range (from other fragments) is applied
        // when this fragment loads. This forces the server-side search to use
        // the same requested start/end instead of falling back to server defaults.
        try {
            var __raw = (typeof sessionStorage !== 'undefined') ? sessionStorage.getItem('globalDateRange') : null;
            if (__raw) {
                try {
                    var __g = JSON.parse(__raw);
                    if (__g && (__g.start || __g.end || __g.mode)) {
                        try { console.debug('[realized-profit] globalDateRange found on load', __g); } catch(e) {}
                        var __cur = window.realizedProfitPicker && typeof window.realizedProfitPicker.getState === 'function' ? window.realizedProfitPicker.getState() : { start: '', end: '', mode: '' };
                        var needApply = (__cur.start !== (__g.start || '') || __cur.end !== (__g.end || '') || __cur.mode !== (__g.mode || ''));
                        // If the server-rendered header range differs from the requested
                        // global range, force a submit even if input values already match
                        // (some fragments are rendered with label-snapped dates on server).
                        var headerRangeText = null;
                        try {
                            var spans = document.querySelectorAll('#realizedProfitFragment span');
                            for (var i = 0; i < spans.length; i++) {
                                var t = (spans[i].textContent || '').trim();
                                if (/\d{4}-\d{2}-\d{2}\s*~\s*\d{4}-\d{2}-\d{2}/.test(t)) { headerRangeText = t; break; }
                            }
                        } catch (e) {}
                        var headerStart = '', headerEnd = '';
                        try {
                            if (headerRangeText) {
                                var mm = headerRangeText.match(/(\d{4}-\d{2}-\d{2})\s*~\s*(\d{4}-\d{2}-\d{2})/);
                                if (mm) { headerStart = mm[1]; headerEnd = mm[2]; }
                            }
                        } catch (e) {}
                        var forceApply = false;
                        try {
                            if (headerStart || headerEnd) {
                                if (headerStart !== (__g.start || '') || headerEnd !== (__g.end || '')) {
                                    forceApply = true;
                                }
                            }
                        } catch (e) {}

                        var shouldApply = needApply || forceApply;
                        try { console.debug('[realized-profit] needApply=%o forceApply=%o header=%o', needApply, forceApply, headerRangeText); } catch(e) {}
                        if (shouldApply) {
                            try { console.debug('[realized-profit] applying globalDateRange to form and submitting', __g); } catch(e) {}
                            var sEl = document.getElementById('realizedStartDateInput');
                            var eEl = document.getElementById('realizedEndDateInput');
                            var mEl = document.getElementById('realizedRangeModeInput');
                            if (sEl) sEl.value = __g.start || '';
                            if (eEl) eEl.value = __g.end || '';
                            if (mEl) mEl.value = __g.mode || '';
                            try {
                                function localDateToInstantIso(ds, addDays) {
                                    if (!ds) return "";
                                    var parts = ds.split("-");
                                    var y = Number.parseInt(parts[0], 10);
                                    var m = Number.parseInt(parts[1], 10) - 1;
                                    var d = Number.parseInt(parts[2], 10);
                                    var dt = new Date(y, m, d + (addDays || 0), 0, 0, 0, 0);
                                    return dt.toISOString();
                                }
                                var instS = document.getElementById('realizedStartInstantInput');
                                var instE = document.getElementById('realizedEndInstantInput');
                                var tzEl = document.getElementById('realizedTimeZoneInput');
                                if (instS) instS.value = __g.start ? localDateToInstantIso(__g.start, 0) : '';
                                if (instE) instE.value = __g.end ? localDateToInstantIso(__g.end, 1) : '';
                                try {
                                    var tz = (typeof Intl !== 'undefined' && Intl.DateTimeFormat) ? Intl.DateTimeFormat().resolvedOptions().timeZone : null;
                                    if (tzEl && tz) tzEl.value = tz;
                                    else if (tzEl && __g.timeZone) tzEl.value = __g.timeZone;
                                } catch (ee) {}
                            } catch (e) {}
                            var form = document.getElementById('realizedProfitSearchForm');
                            // Prevent repeated auto-submits: mark this fragment as applied
                            // before submitting so subsequent fragment loads won't re-trigger.
                            try {
                                var __appliedKey = '__globalDateRangeApplied:realizedProfitSearchForm';
                                try { sessionStorage.setItem(__appliedKey, __raw || JSON.stringify(__g)); } catch(e) {}
                            } catch(e) {}
                            if (form) {
                                try {
                                    try { console.debug('[realized-profit] submitting form values', {start: sEl && sEl.value, end: eEl && eEl.value, instantStart: instS && instS.value, instantEnd: instE && instE.value, mode: mEl && mEl.value}); } catch(e) {}
                                    if (typeof form.requestSubmit === 'function') {
                                        form.requestSubmit();
                                    } else {
                                        try {
                                            var ev = new Event('submit', { bubbles: true, cancelable: true });
                                            var prevented = !form.dispatchEvent(ev);
                                            if (!prevented) {
                                                var tmp = document.createElement('button');
                                                tmp.type = 'submit';
                                                tmp.style.display = 'none';
                                                form.appendChild(tmp);
                                                tmp.click();
                                                tmp.remove();
                                            }
                                        } catch (e2) {
                                            try { form.submit(); } catch (e3) {}
                                        }
                                    }
                                } catch(e) { try { form.submit(); } catch(_) {} }
                            }
                        }
                    }
                } catch(e) {}
            }
        } catch(e) {}
    })();
    </script>

</div>
