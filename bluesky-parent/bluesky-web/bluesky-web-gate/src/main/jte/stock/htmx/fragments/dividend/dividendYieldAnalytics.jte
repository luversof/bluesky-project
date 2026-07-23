@import java.util.List
@import java.util.UUID
@import java.math.BigDecimal
@import java.math.RoundingMode
@import java.text.DecimalFormat
@import net.luversof.web.gate.stock.domain.StockItem
@import net.luversof.web.gate.stock.dto.view.DividendYieldGroupView
@import io.github.luversof.boot.context.support.MessageUtil

@param DecimalFormat decimalFormat
@param java.util.function.Function<BigDecimal, String> percentFormat
@param java.util.function.Function<Long, String> countFormat
@param BigDecimal safeAllNet
@param BigDecimal portfolioYieldOnDailyAverageCostPct
@param BigDecimal portfolioYieldOnCostPct
@param BigDecimal portfolioYieldOnMarketPct
@param BigDecimal portfolioYieldAnnualizedPct = null
@param long periodDayCount = 0
@param BigDecimal periodStartPrincipal = null
@param BigDecimal periodEndPrincipal = null
@param BigDecimal periodPrincipalDelta = null
@param BigDecimal periodPrincipalDeltaPct = null
@param DividendYieldGroupView bestYieldStock
@param DividendYieldGroupView bestYieldAccount
@param List<DividendYieldGroupView> yearlyYieldRows
@param List<DividendYieldGroupView> stockYieldRows
@param List<DividendYieldGroupView> accountYieldRows
@param java.util.Map<String, UUID> yieldStockIdByName
@param java.util.Map<String, UUID> yieldAccountIdByName
@param List<StockItem> stockItemList
@param String accountLabel
@param String stockNameLabel
@param String tagLabel
@param String noDataLabel
@param String grossAmountLabel
@param String netAmountLabel
@param String taxLabel
@param String taxableAmountLabel
@param String totalLabel

!{
    String dividendYieldTitle = MessageUtil.getMessage("stock.dividend.yield.summary.title");
    String bestYieldStockLabel = MessageUtil.getMessage("stock.dividend.yield.summary.best.stock");
    String bestYieldAccountLabel = MessageUtil.getMessage("stock.dividend.yield.summary.best.account");
    String yearlyYieldSectionLabel = MessageUtil.getMessage("stock.dividend.yield.section.yearly");
    String stockYieldSectionLabel = MessageUtil.getMessage("stock.dividend.yield.section.stock");
    String accountYieldSectionLabel = MessageUtil.getMessage("stock.dividend.yield.section.account");
    String yearLabel = MessageUtil.getMessage("stock.dividend.yield.column.year");
    String dailyAverageInvestedCapitalLabel = MessageUtil.getMessage("stock.dividend.yield.column.daily.average.invested.capital");
    String basisAverageInvestedCapitalLabel = MessageUtil.getMessage("stock.dividend.yield.column.basis.average.invested.capital");
    String yieldOnDailyAverageCostLabel = MessageUtil.getMessage("stock.dividend.yield.column.yield.on.daily.average.cost");
    String yieldOnBasisAverageCostLabel = MessageUtil.getMessage("stock.dividend.yield.column.yield.on.basis.average.cost");
    String yieldOnBasisMarketLabel = MessageUtil.getMessage("stock.dividend.yield.column.yield.on.basis.market");
    String dividendYieldPeriodDailyLabel = MessageUtil.getMessage("stock.dividend.yield.summary.period.daily");
    String dividendYieldPeriodBasisLabel = MessageUtil.getMessage("stock.dividend.yield.summary.period.basis");
    String annualizedLabel = MessageUtil.getMessage("stock.dividend.yield.summary.annualized");
    String annualizedNoteLabel = java.text.MessageFormat.format(
        MessageUtil.getMessage("stock.dividend.yield.summary.annualized.note"), periodDayCount);
    String principalChangeLabel = MessageUtil.getMessage("stock.dividend.yield.summary.principal.change");
    String principalStartLabel = MessageUtil.getMessage("stock.dividend.yield.summary.principal.start");
    String principalEndLabel = MessageUtil.getMessage("stock.dividend.yield.summary.principal.end");
    String dividendYieldHelpTitleLabel = MessageUtil.getMessage("stock.dividend.yield.help.title");
    String dividendYieldHelpPrimaryLabel = MessageUtil.getMessage("stock.dividend.yield.help.primary");
    String dividendYieldHelpSecondaryLabel = MessageUtil.getMessage("stock.dividend.yield.help.secondary");
    String dividendYieldHelpRankingLabel = MessageUtil.getMessage("stock.dividend.yield.help.ranking");
    String dividendCountLabel = MessageUtil.getMessage("stock.dividend.yield.column.dividend.count");
    String noYieldAnalyticsLabel = MessageUtil.getMessage("stock.dividend.yield.message.no.analytics");
    String yieldBasisNoteLabel = MessageUtil.getMessage("stock.dividend.yield.basis.note");
    String stockYieldSelectionHelperLabel = MessageUtil.getMessage("stock.dividend.yield.selection.stock.helper");
    String stockYieldSelectionSummaryLabel = MessageUtil.getMessage("stock.dividend.yield.selection.stock.summary");
    String stockYieldSelectionCountLabel = MessageUtil.getMessage("stock.dividend.yield.selection.stock.count");
    String accountYieldSelectionHelperLabel = MessageUtil.getMessage("stock.dividend.yield.selection.account.helper");
    String accountYieldSelectionSummaryLabel = MessageUtil.getMessage("stock.dividend.yield.selection.account.summary");
    String accountYieldSelectionCountLabel = MessageUtil.getMessage("stock.dividend.yield.selection.account.count");
    String dividendYieldSelectionClearLabel = MessageUtil.getMessage("stock.analytics.selection.clear");
    String dividendYieldSelectionWeightLabel = MessageUtil.getMessage("stock.analytics.selection.weight");
    java.util.function.Function<String, String> stockTagTooltipByName = stockName -> {
        if (stockName == null || stockName.isBlank() || stockItemList == null) {
            return tagLabel + ": " + noDataLabel;
        }

        return stockItemList.stream()
            .filter(stockItem -> stockItem != null && stockName.equals(stockItem.name()))
            .findFirst()
            .map(stockItem -> stockItem.tags() != null && !stockItem.tags().isEmpty()
                ? tagLabel + ": " + String.join(", ", stockItem.tags())
                : tagLabel + ": " + noDataLabel)
            .orElse(tagLabel + ": " + noDataLabel);
    };
    java.util.function.BiFunction<List<DividendYieldGroupView>, java.util.function.Function<DividendYieldGroupView, BigDecimal>, BigDecimal> sumDividendYieldValue = (rows, extractor) ->
        rows != null
            ? rows.stream()
                .filter(row -> row != null)
                .map(row -> {
                    BigDecimal value = extractor.apply(row);
                    return value != null ? value : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
            : BigDecimal.ZERO;
    java.util.function.Function<List<DividendYieldGroupView>, Long> sumDividendYieldCount = rows ->
        rows != null
            ? rows.stream()
                .filter(row -> row != null)
                .mapToLong(DividendYieldGroupView::dividendCount)
                .sum()
            : 0L;
    java.util.function.BiFunction<BigDecimal, BigDecimal, BigDecimal> computeYieldPct = (netAmount, baseAmount) ->
        baseAmount != null && baseAmount.compareTo(BigDecimal.ZERO) > 0
            ? (netAmount != null ? netAmount : BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(100))
                .divide(baseAmount, 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
    BigDecimal stockYieldFooterGrossAmount = sumDividendYieldValue.apply(stockYieldRows, DividendYieldGroupView::totalGrossAmount);
    BigDecimal stockYieldFooterNetAmount = sumDividendYieldValue.apply(stockYieldRows, DividendYieldGroupView::totalNetAmount);
    BigDecimal stockYieldFooterTaxAmount = stockYieldFooterGrossAmount.subtract(stockYieldFooterNetAmount);
    BigDecimal stockYieldFooterTaxableAmount = sumDividendYieldValue.apply(stockYieldRows, DividendYieldGroupView::totalTaxableAmount);
    BigDecimal stockYieldFooterDailyPrincipalCost = sumDividendYieldValue.apply(stockYieldRows, DividendYieldGroupView::averageDailyPrincipalCost);
    BigDecimal stockYieldFooterPrincipalCost = sumDividendYieldValue.apply(stockYieldRows, DividendYieldGroupView::averagePrincipalCost);
    BigDecimal stockYieldFooterMarketValue = sumDividendYieldValue.apply(stockYieldRows, DividendYieldGroupView::averagePrincipalMarketValue);
    BigDecimal stockYieldFooterYieldOnDailyAverageCostPct = computeYieldPct.apply(stockYieldFooterNetAmount, stockYieldFooterDailyPrincipalCost);
    BigDecimal stockYieldFooterYieldOnCostPct = computeYieldPct.apply(stockYieldFooterNetAmount, stockYieldFooterPrincipalCost);
    BigDecimal stockYieldFooterYieldOnMarketPct = computeYieldPct.apply(stockYieldFooterNetAmount, stockYieldFooterMarketValue);
    long stockYieldFooterDividendCount = sumDividendYieldCount.apply(stockYieldRows);
    BigDecimal accountYieldFooterGrossAmount = sumDividendYieldValue.apply(accountYieldRows, DividendYieldGroupView::totalGrossAmount);
    BigDecimal accountYieldFooterNetAmount = sumDividendYieldValue.apply(accountYieldRows, DividendYieldGroupView::totalNetAmount);
    BigDecimal accountYieldFooterTaxAmount = accountYieldFooterGrossAmount.subtract(accountYieldFooterNetAmount);
    BigDecimal accountYieldFooterTaxableAmount = sumDividendYieldValue.apply(accountYieldRows, DividendYieldGroupView::totalTaxableAmount);
    BigDecimal accountYieldFooterDailyPrincipalCost = sumDividendYieldValue.apply(accountYieldRows, DividendYieldGroupView::averageDailyPrincipalCost);
    BigDecimal accountYieldFooterPrincipalCost = sumDividendYieldValue.apply(accountYieldRows, DividendYieldGroupView::averagePrincipalCost);
    BigDecimal accountYieldFooterMarketValue = sumDividendYieldValue.apply(accountYieldRows, DividendYieldGroupView::averagePrincipalMarketValue);
    BigDecimal accountYieldFooterYieldOnDailyAverageCostPct = computeYieldPct.apply(accountYieldFooterNetAmount, accountYieldFooterDailyPrincipalCost);
    BigDecimal accountYieldFooterYieldOnCostPct = computeYieldPct.apply(accountYieldFooterNetAmount, accountYieldFooterPrincipalCost);
    BigDecimal accountYieldFooterYieldOnMarketPct = computeYieldPct.apply(accountYieldFooterNetAmount, accountYieldFooterMarketValue);
    long accountYieldFooterDividendCount = sumDividendYieldCount.apply(accountYieldRows);
}

    <!-- Row 3: Yield Analytics -->
    <div class="mb-3 px-1">
        <div class="flex items-center gap-2">
            <div class="text-sm font-bold">${dividendYieldTitle}</div>
            <div class="tooltip tooltip-right shrink-0">
                <div class="tooltip-content z-[20] w-64 sm:w-72 max-w-[calc(100vw-2rem)] rounded-box bg-neutral text-neutral-content shadow-lg">
                    <div class="space-y-1 text-left text-xs leading-5">
                        <div class="font-semibold mb-1">${dividendYieldHelpTitleLabel}</div>
                        <div>${dividendYieldHelpPrimaryLabel}</div>
                        <div>${dividendYieldHelpSecondaryLabel}</div>
                        <div>${dividendYieldHelpRankingLabel}</div>
                    </div>
                </div>
                <button type="button" class="btn btn-circle btn-ghost btn-xs text-info" aria-label="${dividendYieldHelpTitleLabel}">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="w-4 h-4 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                </button>
            </div>
        </div>
        <div class="text-xs text-base-content/50 mt-1">${yieldBasisNoteLabel}</div>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-4">
        @template._components.ui.card(body = @`
                <div class="text-xs text-base-content/50 mb-1">${dividendYieldPeriodDailyLabel}</div>
                <div class="text-xl font-bold text-dividend">${percentFormat.apply(portfolioYieldOnDailyAverageCostPct)}</div>
                <div class="text-xs text-base-content/40 mt-1">${dividendYieldPeriodBasisLabel} <span class="font-medium">${percentFormat.apply(portfolioYieldOnCostPct)}</span></div>
                <div class="text-xs text-base-content/40 mt-1">${yieldOnBasisMarketLabel} <span class="font-medium">${percentFormat.apply(portfolioYieldOnMarketPct)}</span></div>
                @if(portfolioYieldAnnualizedPct != null)
                <div class="mt-2 border-t border-base-200/70 pt-2">
                    <div class="text-xs text-base-content/50">${annualizedLabel}</div>
                    <div class="text-lg font-bold text-dividend">${percentFormat.apply(portfolioYieldAnnualizedPct)}</div>
                    <div class="text-[11px] text-base-content/40">${annualizedNoteLabel}</div>
                </div>
                @endif
                @if(periodPrincipalDelta != null)
                <div class="mt-2 border-t border-base-200/70 pt-2">
                    <div class="text-xs text-base-content/50">${principalChangeLabel}</div>
                    <div class="text-xs text-base-content/70 mt-0.5">
                        <span class="text-base-content/45">${principalStartLabel}</span> <span class="font-mono amount-value">₩ ${decimalFormat.format(periodStartPrincipal)}</span>
                        <span class="text-base-content/40 mx-1">→</span>
                        <span class="text-base-content/45">${principalEndLabel}</span> <span class="font-mono amount-value">₩ ${decimalFormat.format(periodEndPrincipal)}</span>
                    </div>
                    <div class="text-sm font-semibold mt-0.5 ${periodPrincipalDelta.signum() > 0 ? "text-profit" : periodPrincipalDelta.signum() < 0 ? "text-loss" : "text-base-content/60"}">
                        <span class="font-mono amount-value">${periodPrincipalDelta.signum() >= 0 ? "+" : "-"}₩ ${decimalFormat.format(periodPrincipalDelta.abs())}</span>
                        @if(periodPrincipalDeltaPct != null)
                        <span class="text-xs font-normal">(${periodPrincipalDeltaPct.signum() >= 0 ? "+" : ""}${percentFormat.apply(periodPrincipalDeltaPct)})</span>
                        @endif
                    </div>
                </div>
                @endif
        `)

        @template._components.ui.card(body = @`
                <div class="text-xs text-base-content/50 mb-1">${bestYieldStockLabel}</div>
                @if(bestYieldStock != null)
                    <div class="text-sm font-semibold truncate">${bestYieldStock.label()}</div>
                    <div class="text-xl font-bold text-dividend">${percentFormat.apply(bestYieldStock.yieldOnDailyAverageCostPct())}</div>
                    <div class="text-xs text-base-content/40 mt-1">${dailyAverageInvestedCapitalLabel} <span class="font-medium amount-value">₩ ${bestYieldStock.averageDailyPrincipalCost() != null ? decimalFormat.format(bestYieldStock.averageDailyPrincipalCost()) : "-"}</span></div>
                    <div class="text-xs text-base-content/40 mt-1">${yieldOnBasisAverageCostLabel} <span class="font-medium">${percentFormat.apply(bestYieldStock.yieldOnCostPct())}</span></div>
                    <div class="text-xs text-base-content/40 mt-1">${netAmountLabel} <span class="font-medium amount-value">₩ ${decimalFormat.format(bestYieldStock.totalNetAmount())}</span></div>
                @else
                    <div class="text-sm text-base-content/40">${noYieldAnalyticsLabel}</div>
                @endif
        `)

        @template._components.ui.card(body = @`
                <div class="text-xs text-base-content/50 mb-1">${bestYieldAccountLabel}</div>
                @if(bestYieldAccount != null)
                    <div class="text-sm font-semibold truncate">${bestYieldAccount.label()}</div>
                    <div class="text-xl font-bold text-dividend">${percentFormat.apply(bestYieldAccount.yieldOnDailyAverageCostPct())}</div>
                    <div class="text-xs text-base-content/40 mt-1">${dailyAverageInvestedCapitalLabel} <span class="font-medium amount-value">₩ ${bestYieldAccount.averageDailyPrincipalCost() != null ? decimalFormat.format(bestYieldAccount.averageDailyPrincipalCost()) : "-"}</span></div>
                    <div class="text-xs text-base-content/40 mt-1">${yieldOnBasisAverageCostLabel} <span class="font-medium">${percentFormat.apply(bestYieldAccount.yieldOnCostPct())}</span></div>
                    <div class="text-xs text-base-content/40 mt-1">${netAmountLabel} <span class="font-medium amount-value">₩ ${decimalFormat.format(bestYieldAccount.totalNetAmount())}</span></div>
                @else
                    <div class="text-sm text-base-content/40">${noYieldAnalyticsLabel}</div>
                @endif
        `)
    </div>

    <div class="grid grid-cols-1 gap-4 mb-4">
        @template._components.ui.card(cssClass = "min-w-0", body = @`
                <div class="text-sm font-bold mb-3">${yearlyYieldSectionLabel}</div>
                @if(yearlyYieldRows == null || yearlyYieldRows.isEmpty())
                    <div class="text-sm text-base-content/40">${noYieldAnalyticsLabel}</div>
                @else
                    <div class="overflow-x-auto">
                        <table class="table table-xs w-full">
                            <thead>
                                <tr class="text-base-content/60">
                                    <th scope="col">${yearLabel}</th>
                                    <th scope="col" class="text-right hidden 2xl:table-cell">${grossAmountLabel}</th>
                                    <th scope="col" class="text-right">${netAmountLabel}</th>
                                    <th scope="col" class="text-right hidden 2xl:table-cell">${taxLabel}</th>
                                    <th scope="col" class="text-right hidden 2xl:table-cell">${taxableAmountLabel}</th>
                                    <th scope="col" class="text-right hidden 2xl:table-cell">${dailyAverageInvestedCapitalLabel}</th>
                                    <th scope="col" class="text-right">${yieldOnDailyAverageCostLabel}</th>
                                    <th scope="col" class="text-right hidden xl:table-cell">${basisAverageInvestedCapitalLabel}</th>
                                    <th scope="col" class="text-right hidden xl:table-cell">${yieldOnBasisAverageCostLabel}</th>
                                    <th scope="col" class="text-right">${dividendCountLabel}</th>
                                </tr>
                            </thead>
                            <tbody>
                                @for(DividendYieldGroupView row : yearlyYieldRows)
                                    !{BigDecimal rowTaxAmount = row.totalGrossAmount() != null && row.totalNetAmount() != null ? row.totalGrossAmount().subtract(row.totalNetAmount()) : null;}
                                    <tr>
                                        <td class="font-medium">${row.label()}</td>
                                        <td class="text-right hidden 2xl:table-cell amount-value">${decimalFormat.format(row.totalGrossAmount())}</td>
                                        <td class="text-right amount-value">${decimalFormat.format(row.totalNetAmount())}</td>
                                        <td class="text-right hidden 2xl:table-cell amount-value">${rowTaxAmount != null ? decimalFormat.format(rowTaxAmount) : "-"}</td>
                                        <td class="text-right hidden 2xl:table-cell amount-value">${decimalFormat.format(row.totalTaxableAmount())}</td>
                                        <td class="text-right hidden 2xl:table-cell amount-value">${row.averageDailyPrincipalCost() != null ? decimalFormat.format(row.averageDailyPrincipalCost()) : "-"}</td>
                                        <td class="text-right text-dividend font-medium">${percentFormat.apply(row.yieldOnDailyAverageCostPct())}</td>
                                        <td class="text-right hidden xl:table-cell amount-value">${row.averagePrincipalCost() != null ? decimalFormat.format(row.averagePrincipalCost()) : "-"}</td>
                                        <td class="text-right hidden xl:table-cell text-base-content/70">${percentFormat.apply(row.yieldOnCostPct())}</td>
                                        <td class="text-right">${countFormat.apply(row.dividendCount())}</td>
                                    </tr>
                                @endfor
                            </tbody>
                        </table>
                    </div>
                @endif
        `)

        <div class="card min-w-0 bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4" data-dividend-yield-selection-section>
                <div class="text-sm font-bold mb-3">${stockYieldSectionLabel}</div>
                @if(stockYieldRows == null || stockYieldRows.isEmpty())
                    <div class="text-sm text-base-content/40">${noYieldAnalyticsLabel}</div>
                @else
                    <div class="mb-3 flex flex-col gap-3">
                        <p class="text-xs text-base-content/50">${stockYieldSelectionHelperLabel}</p>
                        <div class="selection-reveal"><div>
                        <div class="rounded-2xl border border-info/15 bg-info/5 px-4 py-3"
                             data-dividend-yield-selection-summary
                             data-count-template="${stockYieldSelectionCountLabel}"
                             data-total-net-amount="${safeAllNet.toPlainString()}"
                             data-weight-label="${dividendYieldSelectionWeightLabel}"
                             data-basis-label="${yieldOnBasisAverageCostLabel}"
                             aria-live="polite">
                            <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                                <div class="flex flex-wrap items-center gap-2">
                                    <h3 class="text-sm font-semibold">${stockYieldSelectionSummaryLabel}</h3>
                                    <span class="badge badge-info badge-outline" data-dividend-yield-selection-count>${stockYieldSelectionSummaryLabel}</span>
                                    <span class="badge badge-ghost" data-dividend-yield-selection-weight-badge>${dividendYieldSelectionWeightLabel} 0.0%</span>
                                </div>
                                <button type="button" class="btn btn-ghost btn-xs self-start" data-dividend-yield-selection-clear>${dividendYieldSelectionClearLabel}</button>
                            </div>
                            <div class="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-6">
                                <div class="rounded-box bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${grossAmountLabel}</div>
                                    <div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-gross>0</div>
                                </div>
                                <div class="rounded-box bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${netAmountLabel}</div>
                                    <div class="mt-1 font-mono font-semibold amount-value text-dividend" data-dividend-yield-selection-net>0</div>
                                </div>
                                <div class="rounded-box bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${taxLabel}</div>
                                    <div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-tax>0</div>
                                </div>
                                <div class="rounded-box bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${taxableAmountLabel}</div>
                                    <div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-taxable>0</div>
                                </div>
                                <div class="rounded-box bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${dailyAverageInvestedCapitalLabel}</div>
                                    <div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-daily-capital>0</div>
                                </div>
                                <div class="rounded-box bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${yieldOnDailyAverageCostLabel}</div>
                                    <div class="mt-1 font-mono font-semibold text-dividend" data-dividend-yield-selection-yield>0.00%</div>
                                    <div class="text-xs text-base-content/60" data-dividend-yield-selection-basis-yield>${yieldOnBasisAverageCostLabel} 0.00%</div>
                                </div>
                            </div>
                        </div>
                        </div></div>
                    </div>
                    <div class="overflow-x-auto">
                        <table class="table table-xs w-full" data-dividend-yield-selection-table data-dividend-yield-sortable-table>
                            <thead>
                                <tr class="text-base-content/60">
                                    <th scope="col">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-start px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="label"
                                                data-sort-type="text">
                                            <span>${stockNameLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                    <th scope="col" class="text-right hidden 2xl:table-cell">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="grossAmount"
                                                data-sort-type="number">
                                            <span>${grossAmountLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                    <th scope="col" class="text-right">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="netAmount"
                                                data-sort-type="number">
                                            <span>${netAmountLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                    <th scope="col" class="text-right hidden 2xl:table-cell">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="taxAmount"
                                                data-sort-type="number">
                                            <span>${taxLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                    <th scope="col" class="text-right hidden 2xl:table-cell">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="taxableAmount"
                                                data-sort-type="number">
                                            <span>${taxableAmountLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                    <th scope="col" class="text-right hidden 2xl:table-cell">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="averageDailyPrincipalCost"
                                                data-sort-type="number">
                                            <span>${dailyAverageInvestedCapitalLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                    <th scope="col" class="text-right">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="yieldOnDailyAverageCost"
                                                data-sort-type="number">
                                            <span>${yieldOnDailyAverageCostLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                    <th scope="col" class="text-right hidden xl:table-cell">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="averagePrincipalCost"
                                                data-sort-type="number">
                                            <span>${basisAverageInvestedCapitalLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                    <th scope="col" class="text-right hidden xl:table-cell">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="yieldOnCost"
                                                data-sort-type="number">
                                            <span>${yieldOnBasisAverageCostLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                    <th scope="col" class="text-right hidden 2xl:table-cell">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="yieldOnMarket"
                                                data-sort-type="number">
                                            <span>${yieldOnBasisMarketLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                    <th scope="col" class="text-right">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="dividendCount"
                                                data-sort-type="number">
                                            <span>${dividendCountLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                </tr>
                            </thead>
                            <tbody>
                                @for(DividendYieldGroupView row : stockYieldRows)
                                    !{BigDecimal rowTaxAmount = row.totalGrossAmount() != null && row.totalNetAmount() != null ? row.totalGrossAmount().subtract(row.totalNetAmount()) : null;}
                                    !{String stockTagTooltip = stockTagTooltipByName.apply(row.label());}
                                    <tr class="dividend-yield-stock-row"
                                        data-dividend-yield-row
                                        data-selection-class="dividend-yield-stock-row-selected"
                                        tabindex="0"
                                        role="button"
                                        aria-pressed="false"
                                        data-label="${row.label() != null ? row.label() : ""}"
                                        data-gross-amount="${row.totalGrossAmount() != null ? row.totalGrossAmount().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-net-amount="${row.totalNetAmount() != null ? row.totalNetAmount().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-tax-amount="${rowTaxAmount != null ? rowTaxAmount.toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-taxable-amount="${row.totalTaxableAmount() != null ? row.totalTaxableAmount().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-average-daily-principal-cost="${row.averageDailyPrincipalCost() != null ? row.averageDailyPrincipalCost().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-yield-on-daily-average-cost="${row.yieldOnDailyAverageCostPct() != null ? row.yieldOnDailyAverageCostPct().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-average-principal-cost="${row.averagePrincipalCost() != null ? row.averagePrincipalCost().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-yield-on-cost="${row.yieldOnCostPct() != null ? row.yieldOnCostPct().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-yield-on-market="${row.yieldOnMarketPct() != null ? row.yieldOnMarketPct().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-dividend-count="${String.valueOf(row.dividendCount())}">
                                        <td class="font-medium"><a href="/stock/item?stockItemId=${yieldStockIdByName.get(row.label()) != null ? yieldStockIdByName.get(row.label()).toString() : ""}" class="link link-hover" data-stock-tag-tooltip="${stockTagTooltip}">${row.label()}</a></td>
                                        <td class="text-right hidden 2xl:table-cell amount-value">${decimalFormat.format(row.totalGrossAmount())}</td>
                                        <td class="text-right amount-value">${decimalFormat.format(row.totalNetAmount())}</td>
                                        <td class="text-right hidden 2xl:table-cell amount-value">${rowTaxAmount != null ? decimalFormat.format(rowTaxAmount) : "-"}</td>
                                        <td class="text-right hidden 2xl:table-cell amount-value">${decimalFormat.format(row.totalTaxableAmount())}</td>
                                        <td class="text-right hidden 2xl:table-cell amount-value">${row.averageDailyPrincipalCost() != null ? decimalFormat.format(row.averageDailyPrincipalCost()) : "-"}</td>
                                        <td class="text-right text-dividend font-medium">${percentFormat.apply(row.yieldOnDailyAverageCostPct())}</td>
                                        <td class="text-right hidden xl:table-cell amount-value">${row.averagePrincipalCost() != null ? decimalFormat.format(row.averagePrincipalCost()) : "-"}</td>
                                        <td class="text-right hidden xl:table-cell text-base-content/70">${percentFormat.apply(row.yieldOnCostPct())}</td>
                                        <td class="text-right hidden 2xl:table-cell text-base-content/70">${percentFormat.apply(row.yieldOnMarketPct())}</td>
                                        <td class="text-right">${countFormat.apply(row.dividendCount())}</td>
                                    </tr>
                                @endfor
                            </tbody>
                            <tfoot>
                                <tr class="bg-base-200 font-bold">
                                    <td>${totalLabel}</td>
                                    <td class="text-right hidden 2xl:table-cell font-mono amount-value">${decimalFormat.format(stockYieldFooterGrossAmount)}</td>
                                    <td class="text-right font-mono text-dividend amount-value">${decimalFormat.format(stockYieldFooterNetAmount)}</td>
                                    <td class="text-right hidden 2xl:table-cell font-mono amount-value">${decimalFormat.format(stockYieldFooterTaxAmount)}</td>
                                    <td class="text-right hidden 2xl:table-cell font-mono amount-value">${decimalFormat.format(stockYieldFooterTaxableAmount)}</td>
                                    <td class="text-right hidden 2xl:table-cell font-mono amount-value">${decimalFormat.format(stockYieldFooterDailyPrincipalCost)}</td>
                                    <td class="text-right text-dividend font-medium">${percentFormat.apply(stockYieldFooterYieldOnDailyAverageCostPct)}</td>
                                    <td class="text-right hidden xl:table-cell font-mono amount-value">${decimalFormat.format(stockYieldFooterPrincipalCost)}</td>
                                    <td class="text-right hidden xl:table-cell text-base-content/70">${percentFormat.apply(stockYieldFooterYieldOnCostPct)}</td>
                                    <td class="text-right hidden 2xl:table-cell text-base-content/70">${percentFormat.apply(stockYieldFooterYieldOnMarketPct)}</td>
                                    <td class="text-right">${countFormat.apply(stockYieldFooterDividendCount)}</td>
                                </tr>
                            </tfoot>
                        </table>
                    </div>
                @endif
            </div>
        </div>

        <div class="card min-w-0 bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4" data-dividend-yield-selection-section>
                <div class="text-sm font-bold mb-3">${accountYieldSectionLabel}</div>
                @if(accountYieldRows == null || accountYieldRows.isEmpty())
                    <div class="text-sm text-base-content/40">${noYieldAnalyticsLabel}</div>
                @else
                    <div class="mb-3 flex flex-col gap-3">
                        <p class="text-xs text-base-content/50">${accountYieldSelectionHelperLabel}</p>
                        <div class="selection-reveal"><div>
                        <div class="rounded-2xl border border-secondary/15 bg-secondary/5 px-4 py-3"
                             data-dividend-yield-selection-summary
                             data-count-template="${accountYieldSelectionCountLabel}"
                             data-total-net-amount="${safeAllNet.toPlainString()}"
                             data-weight-label="${dividendYieldSelectionWeightLabel}"
                             data-basis-label="${yieldOnBasisAverageCostLabel}"
                             aria-live="polite">
                            <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                                <div class="flex flex-wrap items-center gap-2">
                                    <h3 class="text-sm font-semibold">${accountYieldSelectionSummaryLabel}</h3>
                                    <span class="badge badge-secondary badge-outline" data-dividend-yield-selection-count>${accountYieldSelectionSummaryLabel}</span>
                                    <span class="badge badge-ghost" data-dividend-yield-selection-weight-badge>${dividendYieldSelectionWeightLabel} 0.0%</span>
                                </div>
                                <button type="button" class="btn btn-ghost btn-xs self-start" data-dividend-yield-selection-clear>${dividendYieldSelectionClearLabel}</button>
                            </div>
                            <div class="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-6">
                                <div class="rounded-box bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${grossAmountLabel}</div>
                                    <div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-gross>0</div>
                                </div>
                                <div class="rounded-box bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${netAmountLabel}</div>
                                    <div class="mt-1 font-mono font-semibold amount-value text-dividend" data-dividend-yield-selection-net>0</div>
                                </div>
                                <div class="rounded-box bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${taxLabel}</div>
                                    <div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-tax>0</div>
                                </div>
                                <div class="rounded-box bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${taxableAmountLabel}</div>
                                    <div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-taxable>0</div>
                                </div>
                                <div class="rounded-box bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${dailyAverageInvestedCapitalLabel}</div>
                                    <div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-daily-capital>0</div>
                                </div>
                                <div class="rounded-box bg-base-100/80 px-3 py-2">
                                    <div class="text-xs text-base-content/60">${yieldOnDailyAverageCostLabel}</div>
                                    <div class="mt-1 font-mono font-semibold text-dividend" data-dividend-yield-selection-yield>0.00%</div>
                                    <div class="text-xs text-base-content/60" data-dividend-yield-selection-basis-yield>${yieldOnBasisAverageCostLabel} 0.00%</div>
                                </div>
                            </div>
                        </div>
                        </div></div>
                    </div>
                    <div class="overflow-x-auto">
                        <table class="table table-xs w-full" data-dividend-yield-selection-table data-dividend-yield-sortable-table>
                            <thead>
                                <tr class="text-base-content/60">
                                    <th scope="col">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-start px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="label"
                                                data-sort-type="text">
                                            <span>${accountLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                    <th scope="col" class="text-right hidden 2xl:table-cell">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="grossAmount"
                                                data-sort-type="number">
                                            <span>${grossAmountLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                    <th scope="col" class="text-right">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="netAmount"
                                                data-sort-type="number">
                                            <span>${netAmountLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                    <th scope="col" class="text-right hidden 2xl:table-cell">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="taxAmount"
                                                data-sort-type="number">
                                            <span>${taxLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                    <th scope="col" class="text-right hidden 2xl:table-cell">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="taxableAmount"
                                                data-sort-type="number">
                                            <span>${taxableAmountLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                    <th scope="col" class="text-right hidden 2xl:table-cell">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="averageDailyPrincipalCost"
                                                data-sort-type="number">
                                            <span>${dailyAverageInvestedCapitalLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                    <th scope="col" class="text-right">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="yieldOnDailyAverageCost"
                                                data-sort-type="number">
                                            <span>${yieldOnDailyAverageCostLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                    <th scope="col" class="text-right hidden xl:table-cell">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="averagePrincipalCost"
                                                data-sort-type="number">
                                            <span>${basisAverageInvestedCapitalLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                    <th scope="col" class="text-right hidden xl:table-cell">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="yieldOnCost"
                                                data-sort-type="number">
                                            <span>${yieldOnBasisAverageCostLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                    <th scope="col" class="text-right hidden 2xl:table-cell">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="yieldOnMarket"
                                                data-sort-type="number">
                                            <span>${yieldOnBasisMarketLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                    <th scope="col" class="text-right">
                                        <button type="button"
                                                class="btn btn-ghost btn-xs h-auto min-h-0 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
                                                data-sort-key="dividendCount"
                                                data-sort-type="number">
                                            <span>${dividendCountLabel}</span>
                                            <span data-sort-indicator class="opacity-60">↕</span>
                                        </button>
                                    </th>
                                </tr>
                            </thead>
                            <tbody>
                                @for(DividendYieldGroupView row : accountYieldRows)
                                    !{BigDecimal rowTaxAmount = row.totalGrossAmount() != null && row.totalNetAmount() != null ? row.totalGrossAmount().subtract(row.totalNetAmount()) : null;}
                                    <tr class="dividend-yield-account-row"
                                        data-dividend-yield-row
                                        data-selection-class="dividend-yield-account-row-selected"
                                        tabindex="0"
                                        role="button"
                                        aria-pressed="false"
                                        data-label="${row.label() != null ? row.label() : ""}"
                                        data-gross-amount="${row.totalGrossAmount() != null ? row.totalGrossAmount().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-net-amount="${row.totalNetAmount() != null ? row.totalNetAmount().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-tax-amount="${rowTaxAmount != null ? rowTaxAmount.toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-taxable-amount="${row.totalTaxableAmount() != null ? row.totalTaxableAmount().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-average-daily-principal-cost="${row.averageDailyPrincipalCost() != null ? row.averageDailyPrincipalCost().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-yield-on-daily-average-cost="${row.yieldOnDailyAverageCostPct() != null ? row.yieldOnDailyAverageCostPct().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-average-principal-cost="${row.averagePrincipalCost() != null ? row.averagePrincipalCost().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-yield-on-cost="${row.yieldOnCostPct() != null ? row.yieldOnCostPct().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-yield-on-market="${row.yieldOnMarketPct() != null ? row.yieldOnMarketPct().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                        data-dividend-count="${String.valueOf(row.dividendCount())}">
                                        <td class="font-medium"><a href="/stock/account?accountId=${yieldAccountIdByName.get(row.label()) != null ? yieldAccountIdByName.get(row.label()).toString() : ""}" class="link link-hover">${row.label()}</a></td>
                                        <td class="text-right hidden 2xl:table-cell amount-value">${decimalFormat.format(row.totalGrossAmount())}</td>
                                        <td class="text-right amount-value">${decimalFormat.format(row.totalNetAmount())}</td>
                                        <td class="text-right hidden 2xl:table-cell amount-value">${rowTaxAmount != null ? decimalFormat.format(rowTaxAmount) : "-"}</td>
                                        <td class="text-right hidden 2xl:table-cell amount-value">${decimalFormat.format(row.totalTaxableAmount())}</td>
                                        <td class="text-right hidden 2xl:table-cell amount-value">${row.averageDailyPrincipalCost() != null ? decimalFormat.format(row.averageDailyPrincipalCost()) : "-"}</td>
                                        <td class="text-right text-dividend font-medium">${percentFormat.apply(row.yieldOnDailyAverageCostPct())}</td>
                                        <td class="text-right hidden xl:table-cell amount-value">${row.averagePrincipalCost() != null ? decimalFormat.format(row.averagePrincipalCost()) : "-"}</td>
                                        <td class="text-right hidden xl:table-cell text-base-content/70">${percentFormat.apply(row.yieldOnCostPct())}</td>
                                        <td class="text-right hidden 2xl:table-cell text-base-content/70">${percentFormat.apply(row.yieldOnMarketPct())}</td>
                                        <td class="text-right">${countFormat.apply(row.dividendCount())}</td>
                                    </tr>
                                @endfor
                            </tbody>
                            <tfoot>
                                <tr class="bg-base-200 font-bold">
                                    <td>${totalLabel}</td>
                                    <td class="text-right hidden 2xl:table-cell font-mono amount-value">${decimalFormat.format(accountYieldFooterGrossAmount)}</td>
                                    <td class="text-right font-mono text-dividend amount-value">${decimalFormat.format(accountYieldFooterNetAmount)}</td>
                                    <td class="text-right hidden 2xl:table-cell font-mono amount-value">${decimalFormat.format(accountYieldFooterTaxAmount)}</td>
                                    <td class="text-right hidden 2xl:table-cell font-mono amount-value">${decimalFormat.format(accountYieldFooterTaxableAmount)}</td>
                                    <td class="text-right hidden 2xl:table-cell font-mono amount-value">${decimalFormat.format(accountYieldFooterDailyPrincipalCost)}</td>
                                    <td class="text-right text-dividend font-medium">${percentFormat.apply(accountYieldFooterYieldOnDailyAverageCostPct)}</td>
                                    <td class="text-right hidden xl:table-cell font-mono amount-value">${decimalFormat.format(accountYieldFooterPrincipalCost)}</td>
                                    <td class="text-right hidden xl:table-cell text-base-content/70">${percentFormat.apply(accountYieldFooterYieldOnCostPct)}</td>
                                    <td class="text-right hidden 2xl:table-cell text-base-content/70">${percentFormat.apply(accountYieldFooterYieldOnMarketPct)}</td>
                                    <td class="text-right">${countFormat.apply(accountYieldFooterDividendCount)}</td>
                                </tr>
                            </tfoot>
                        </table>
                    </div>
                @endif
            </div>
        </div>
    </div>

    <style>
        .dividend-yield-stock-row,
        .dividend-yield-account-row {
            cursor: pointer;
        }

        .dividend-yield-stock-row.dividend-yield-stock-row-selected > td {
            background-color: color-mix(in oklch, var(--color-info) 10%, var(--color-base-100));
        }

        .dividend-yield-stock-row.dividend-yield-stock-row-selected > td:first-child {
            box-shadow: inset 3px 0 0 color-mix(in oklch, var(--color-info) 65%, transparent);
        }

        .dividend-yield-account-row.dividend-yield-account-row-selected > td {
            background-color: color-mix(in oklch, var(--color-secondary) 10%, var(--color-base-100));
        }

        .dividend-yield-account-row.dividend-yield-account-row-selected > td:first-child {
            box-shadow: inset 3px 0 0 color-mix(in oklch, var(--color-secondary) 65%, transparent);
        }

        .dividend-yield-stock-row:focus-visible,
        .dividend-yield-account-row:focus-visible {
            outline: 2px solid color-mix(in oklch, var(--color-info) 35%, transparent);
            outline-offset: -2px;
        }
    </style>
