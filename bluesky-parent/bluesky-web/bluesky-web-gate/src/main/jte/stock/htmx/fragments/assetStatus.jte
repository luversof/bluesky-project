@import java.math.BigDecimal
@import java.util.List
@import java.util.Map
@import net.luversof.web.gate.stock.domain.TradeProfit
@import net.luversof.web.gate.stock.dto.response.AssetStatusAccountHoldingView
@import io.github.luversof.boot.context.support.MessageUtil

@param Map<String, TradeProfit> accountTotalMap
@param Map<String, BigDecimal> accountProfitBasisMap
@param Map<String, List<AssetStatusAccountHoldingView>> accountHoldingMap
@param List<TradeProfit> stockAggregated
@param BigDecimal totalEvaluationAmount
@param BigDecimal totalEvaluationProfit

!{
    String accountSectionTitle = MessageUtil.getMessage("stock.analytics.section.account.status");
    String stockSectionTitle = MessageUtil.getMessage("stock.analytics.section.stock.status");
    String allAccountsLabel = MessageUtil.getMessage("stock.analytics.section.all.accounts");
    String accountLabel = MessageUtil.getMessage("common.label.account");
    String buyAmountLabel = MessageUtil.getMessage("stock.trade.label.buy.amount");
    String stockNameLabel = MessageUtil.getMessage("stock.activity.label.stock.name");
    String quantityLabel = MessageUtil.getMessage("common.label.quantity");
    String averageCostLabel = MessageUtil.getMessage("stock.column.average.cost");
    String currentPriceLabel = MessageUtil.getMessage("stock.column.current.price");
    String weightLabel = MessageUtil.getMessage("stock.analytics.account.holdings.weight");
    String accountWeightLabel = MessageUtil.getMessage("stock.analytics.account.holdings.weight.account");
    String totalWeightLabel = MessageUtil.getMessage("stock.analytics.account.holdings.weight.total");
    String evaluationAmountLabel = MessageUtil.getMessage("stock.profit.evaluation.amount");
    String principalLabel = MessageUtil.getMessage("stock.label.principal");
    String evaluationProfitLabel = MessageUtil.getMessage("stock.profit.unrealized");
    String principalReturnLabel = MessageUtil.getMessage("stock.analytics.label.principal.return");
    String totalLabel = MessageUtil.getMessage("common.label.total");
    String noHoldingsLabel = MessageUtil.getMessage("stock.message.no.holdings");
    String showHoldingsLabel = MessageUtil.getMessage("stock.analytics.account.holdings.show");
    String hideHoldingsLabel = MessageUtil.getMessage("stock.analytics.account.holdings.hide");
    String accountHoldingsDetailLabel = MessageUtil.getMessage("stock.analytics.account.holdings.detail");
}

<div class="flex flex-col gap-6">

    <!-- Section 1: 계좌별 현황 테이블 -->
    <div>
        <h2 class="text-lg font-semibold mb-3 opacity-70">${accountSectionTitle}</h2>
        <div class="card bg-base-100 shadow-md overflow-x-auto">
            <table class="table table-sm w-full" data-sortable-table>
                <thead class="bg-base-200">
                    <tr>
                        <th>
                            <button type="button"
                                    class="btn btn-ghost btn-xs h-auto min-h-0 px-1 text-xs text-base-content/60 hover:text-base-content"
                                    data-sort-key="account"
                                    data-sort-type="text">
                                <span>${accountLabel}</span>
                                <span data-sort-indicator class="opacity-60">↕</span>
                            </button>
                        </th>
                        <th class="text-right">
                            <button type="button"
                                    class="btn btn-ghost btn-xs h-auto min-h-0 px-1 text-xs text-base-content/60 hover:text-base-content"
                                    data-sort-key="buyAmount"
                                    data-sort-type="number">
                                <span>${buyAmountLabel}</span>
                                <span data-sort-indicator class="opacity-60">↕</span>
                            </button>
                        </th>
                        <th class="text-right">
                            <button type="button"
                                    class="btn btn-ghost btn-xs h-auto min-h-0 px-1 text-xs text-base-content/60 hover:text-base-content"
                                    data-sort-key="evaluationAmount"
                                    data-sort-type="number">
                                <span>${evaluationAmountLabel}</span>
                                <span data-sort-indicator class="opacity-60">↕</span>
                            </button>
                        </th>
                        <th class="text-right">
                            <button type="button"
                                    class="btn btn-ghost btn-xs h-auto min-h-0 px-1 text-xs text-base-content/60 hover:text-base-content"
                                    data-sort-key="evaluationProfit"
                                    data-sort-type="number">
                                <span>${evaluationProfitLabel}</span>
                                <span data-sort-indicator class="opacity-60">↕</span>
                            </button>
                        </th>
                        <th class="text-right hidden md:table-cell">
                            <button type="button"
                                    class="btn btn-ghost btn-xs h-auto min-h-0 px-1 text-xs text-base-content/60 hover:text-base-content"
                                    data-sort-key="principal"
                                    data-sort-type="number">
                                <span>${principalLabel}</span>
                                <span data-sort-indicator class="opacity-60">↕</span>
                            </button>
                        </th>
                        <th class="text-right">
                            <button type="button"
                                    class="btn btn-ghost btn-xs h-auto min-h-0 px-1 text-xs text-base-content/60 hover:text-base-content"
                                    data-sort-key="principalReturn"
                                    data-sort-type="number">
                                <span>${principalReturnLabel}</span>
                                <span data-sort-indicator class="opacity-60">↕</span>
                            </button>
                        </th>
                    </tr>
                </thead>
                <tbody>
                @for(Map.Entry<String, TradeProfit> entry : accountTotalMap.entrySet())
                    !{TradeProfit acc = entry.getValue();}
                    !{List<AssetStatusAccountHoldingView> holdings = accountHoldingMap != null && accountHoldingMap.get(entry.getKey()) != null ? accountHoldingMap.get(entry.getKey()) : List.of();}
                    !{BigDecimal buyAmount = acc.totalBuyCost() != null ? acc.totalBuyCost() : BigDecimal.ZERO;}
                    !{BigDecimal evaluationAmount = acc.evaluationAmount() != null ? acc.evaluationAmount() : BigDecimal.ZERO;}
                    !{BigDecimal evaluationProfitAmount = acc.evaluationProfit() != null ? acc.evaluationProfit() : BigDecimal.ZERO;}
                    !{double evaluationProfitRate = buyAmount.compareTo(BigDecimal.ZERO) > 0
                        ? evaluationProfitAmount.doubleValue() / buyAmount.doubleValue() * 100
                        : 0.0;}
                    !{BigDecimal profitBasis = accountProfitBasisMap != null && accountProfitBasisMap.get(entry.getKey()) != null ? accountProfitBasisMap.get(entry.getKey()) : buyAmount;}
                    !{BigDecimal principalReturnAmount = evaluationAmount.subtract(profitBasis);}
                    !{double rate = profitBasis.compareTo(BigDecimal.ZERO) > 0
                        ? principalReturnAmount.doubleValue() / profitBasis.doubleValue() * 100
                        : 0.0;}
                    !{String detailRowId = "asset-status-account-detail-" + Math.abs((entry.getKey() != null ? entry.getKey() : "account").hashCode());}
                    <tr class="hover"
                        data-sortable-row
                        data-account="${entry.getKey() != null ? entry.getKey() : ""}"
                        data-buy-amount="${buyAmount.toPlainString()}"
                        data-evaluation-amount="${evaluationAmount.toPlainString()}"
                        data-evaluation-profit="${evaluationProfitAmount.toPlainString()}"
                        data-principal="${profitBasis.toPlainString()}"
                        data-principal-return="${principalReturnAmount.toPlainString()}"
                        data-detail-row-id="${detailRowId}">
                        <td>
                            <div class="font-semibold">${entry.getKey()}</div>
                            @if(holdings.isEmpty())
                                <div class="text-xs opacity-50">${noHoldingsLabel}</div>
                            @else
                                <button type="button"
                                        class="btn btn-ghost btn-xs h-auto min-h-0 px-1 mt-1 text-xs text-base-content/60 hover:text-base-content"
                                        onclick="toggleAssetStatusAccountDetail(this, '${detailRowId}')"
                                        aria-expanded="false"
                                        data-expand-label="${showHoldingsLabel}"
                                        data-collapse-label="${hideHoldingsLabel}">
                                    <span data-label>${showHoldingsLabel}</span>
                                    <span class="opacity-60">(${holdings.size()})</span>
                                    <span data-icon class="opacity-60">▼</span>
                                </button>
                            @endif
                        </td>
                        <td class="text-right font-mono amount-value opacity-70">
                            ${String.format("%,d", buyAmount.longValue())}
                        </td>
                        <td class="text-right font-bold font-mono amount-value text-principal">
                            ${String.format("%,d", evaluationAmount.longValue())}
                        </td>
                        <td class="text-right">
                            <div class="font-bold font-mono amount-value ${evaluationProfitAmount.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                                ${String.format("%+,d", evaluationProfitAmount.longValue())}
                            </div>
                            <div class="text-xs font-semibold ${evaluationProfitAmount.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                                (${String.format("%+.1f%%", evaluationProfitRate)})
                            </div>
                        </td>
                        <td class="text-right font-mono hidden md:table-cell amount-value opacity-70">
                            ${String.format("%,d", profitBasis.longValue())}
                        </td>
                        <td class="text-right ${principalReturnAmount.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                            <div class="font-bold font-mono amount-value">
                                ${String.format("%+,d", principalReturnAmount.longValue())}
                            </div>
                            <div class="text-xs font-semibold">
                                (${String.format("%+.1f%%", rate)})
                            </div>
                        </td>
                    </tr>
                    @if(!holdings.isEmpty())
                        <tr id="${detailRowId}" class="hidden bg-base-200/40">
                            <td colspan="6" class="p-0">
                                <div class="px-3 py-3">
                                    <div class="text-xs font-semibold text-base-content/60 mb-2">${accountHoldingsDetailLabel}</div>
                                    <div class="overflow-x-auto rounded-box border border-base-300/60 bg-base-100">
                                        <table class="table table-xs w-full">
                                            <thead class="bg-base-200/70">
                                                <tr>
                                                    <th>${stockNameLabel}</th>
                                                    <th class="text-right hidden sm:table-cell">${quantityLabel}</th>
                                                    <th class="text-right hidden lg:table-cell">${averageCostLabel}</th>
                                                    <th class="text-right hidden md:table-cell">${buyAmountLabel}</th>
                                                    <th class="text-right hidden lg:table-cell">${currentPriceLabel}</th>
                                                    <th class="text-right">${evaluationAmountLabel}</th>
                                                    <th class="text-right">${evaluationProfitLabel}</th>
                                                    <th class="text-right hidden xl:table-cell">${weightLabel}</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                @for(AssetStatusAccountHoldingView holding : holdings)
                                                    <tr>
                                                        <td class="font-semibold">${holding.stockItemName() != null ? holding.stockItemName() : "-"}</td>
                                                        <td class="text-right font-mono hidden sm:table-cell amount-value">${String.format("%,d", holding.holdingQuantity())}</td>
                                                        <td class="text-right font-mono hidden lg:table-cell amount-value">${String.format("%,d", holding.averageBuyPrice() != null ? holding.averageBuyPrice().setScale(0, java.math.RoundingMode.HALF_UP).longValue() : 0)}</td>
                                                        <td class="text-right font-mono hidden md:table-cell amount-value opacity-70">${String.format("%,d", holding.buyAmount() != null ? holding.buyAmount().longValue() : 0)}</td>
                                                        <td class="text-right font-mono hidden lg:table-cell amount-value">${String.format("%,d", holding.currentPrice() != null ? holding.currentPrice().longValue() : 0)}</td>
                                                        <td class="text-right font-mono amount-value text-principal">${String.format("%,d", holding.evaluationAmount() != null ? holding.evaluationAmount().longValue() : 0)}</td>
                                                        <td class="text-right font-mono font-bold amount-value ${holding.evaluationProfit() != null && holding.evaluationProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                                                            ${String.format("%+,d", holding.evaluationProfit() != null ? holding.evaluationProfit().longValue() : 0)}
                                                            <span class="text-xs opacity-70">(${String.format("%+.1f%%", holding.evaluationProfitRatePct() != null ? holding.evaluationProfitRatePct().doubleValue() : 0.0)})</span>
                                                        </td>
                                                        <td class="text-right hidden xl:table-cell">
                                                            <div class="text-xs text-base-content/70">${accountWeightLabel} ${String.format("%.1f%%", holding.accountWeightPct() != null ? holding.accountWeightPct().doubleValue() : 0.0)}</div>
                                                            <div class="text-xs text-base-content/50">${totalWeightLabel} ${String.format("%.1f%%", holding.totalWeightPct() != null ? holding.totalWeightPct().doubleValue() : 0.0)}</div>
                                                        </td>
                                                    </tr>
                                                @endfor
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            </td>
                        </tr>
                    @endif
                @endfor
                </tbody>
                <tfoot class="bg-base-200 font-bold">
                    !{BigDecimal totalAccountEvaluationAmount = accountTotalMap != null
                        ? accountTotalMap.values().stream().map(acc -> acc.evaluationAmount() != null ? acc.evaluationAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add)
                        : BigDecimal.ZERO;}
                    !{BigDecimal totalBuyAmount = accountTotalMap != null
                        ? accountTotalMap.values().stream().map(acc -> acc.totalBuyCost() != null ? acc.totalBuyCost() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add)
                        : BigDecimal.ZERO;}
                    !{BigDecimal totalPrincipal = accountProfitBasisMap != null
                        ? accountProfitBasisMap.values().stream().map(v -> v != null ? v : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add)
                        : BigDecimal.ZERO;}
                    !{BigDecimal totalAccountEvaluationProfit = accountTotalMap != null
                        ? accountTotalMap.values().stream().map(acc -> acc.evaluationProfit() != null ? acc.evaluationProfit() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add)
                        : BigDecimal.ZERO;}
                    !{BigDecimal totalPrincipalReturnAmount = totalAccountEvaluationAmount.subtract(totalPrincipal);}
                    !{double totalEvaluationProfitRate = totalBuyAmount.compareTo(BigDecimal.ZERO) > 0
                        ? totalAccountEvaluationProfit.doubleValue() / totalBuyAmount.doubleValue() * 100
                        : 0.0;}
                    !{double totalPrincipalReturnRate = totalPrincipal.compareTo(BigDecimal.ZERO) > 0
                        ? totalPrincipalReturnAmount.doubleValue() / totalPrincipal.doubleValue() * 100
                        : 0.0;}
                    <tr>
                        <td>${totalLabel}</td>
                        <td class="text-right font-mono amount-value opacity-70">
                            ${String.format("%,d", totalBuyAmount.longValue())}
                        </td>
                        <td class="text-right font-mono amount-value text-principal">
                            ${String.format("%,d", totalAccountEvaluationAmount.longValue())}
                        </td>
                        <td class="text-right ${totalAccountEvaluationProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                            <div class="font-bold font-mono amount-value">
                                ${String.format("%+,d", totalAccountEvaluationProfit.longValue())}
                            </div>
                            <div class="text-xs font-semibold">
                                (${String.format("%+.1f%%", totalEvaluationProfitRate)})
                            </div>
                        </td>
                        <td class="text-right font-mono hidden md:table-cell amount-value opacity-70">
                            ${String.format("%,d", totalPrincipal.longValue())}
                        </td>
                        <td class="text-right ${totalPrincipalReturnAmount.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                            <div class="font-bold font-mono amount-value">
                                ${String.format("%+,d", totalPrincipalReturnAmount.longValue())}
                            </div>
                            <div class="text-xs font-semibold">
                                (${String.format("%+.1f%%", totalPrincipalReturnRate)})
                            </div>
                        </td>
                    </tr>
                </tfoot>
            </table>
        </div>
    </div>

    <!-- Section 2: 종목별 집계 테이블 (계좌 통합) -->
    <div>
        <h2 class="text-lg font-semibold mb-3 opacity-70">${stockSectionTitle} <span class="text-xs font-normal opacity-60">(${allAccountsLabel})</span></h2>
        <div class="card bg-base-100 shadow-md overflow-x-auto">
            <table class="table table-sm table-zebra w-full" data-sortable-table>
                <thead class="bg-base-200">
                    <tr>
                        <th>
                            <button type="button"
                                    class="btn btn-ghost btn-xs h-auto min-h-0 px-1 text-xs text-base-content/60 hover:text-base-content"
                                    data-sort-key="stockName"
                                    data-sort-type="text">
                                <span>${stockNameLabel}</span>
                                <span data-sort-indicator class="opacity-60">↕</span>
                            </button>
                        </th>
                        <th class="text-right hidden lg:table-cell">
                            <button type="button"
                                    class="btn btn-ghost btn-xs h-auto min-h-0 px-1 text-xs text-base-content/60 hover:text-base-content"
                                    data-sort-key="quantity"
                                    data-sort-type="number">
                                <span>${quantityLabel}</span>
                                <span data-sort-indicator class="opacity-60">↕</span>
                            </button>
                        </th>
                        <th class="text-right hidden xl:table-cell">
                            <button type="button"
                                    class="btn btn-ghost btn-xs h-auto min-h-0 px-1 text-xs text-base-content/60 hover:text-base-content"
                                    data-sort-key="averageCost"
                                    data-sort-type="number">
                                <span>${averageCostLabel}</span>
                                <span data-sort-indicator class="opacity-60">↕</span>
                            </button>
                        </th>
                        <th class="text-right hidden sm:table-cell">
                            <button type="button"
                                    class="btn btn-ghost btn-xs h-auto min-h-0 px-1 text-xs text-base-content/60 hover:text-base-content"
                                    data-sort-key="buyAmount"
                                    data-sort-type="number">
                                <span>${buyAmountLabel}</span>
                                <span data-sort-indicator class="opacity-60">↕</span>
                            </button>
                        </th>
                        <th class="text-right hidden lg:table-cell">
                            <button type="button"
                                    class="btn btn-ghost btn-xs h-auto min-h-0 px-1 text-xs text-base-content/60 hover:text-base-content"
                                    data-sort-key="currentPrice"
                                    data-sort-type="number">
                                <span>${currentPriceLabel}</span>
                                <span data-sort-indicator class="opacity-60">↕</span>
                            </button>
                        </th>
                        <th class="text-right">
                            <button type="button"
                                    class="btn btn-ghost btn-xs h-auto min-h-0 px-1 text-xs text-base-content/60 hover:text-base-content"
                                    data-sort-key="evaluationAmount"
                                    data-sort-type="number">
                                <span>${evaluationAmountLabel}</span>
                                <span data-sort-indicator class="opacity-60">↕</span>
                            </button>
                        </th>
                        <th class="text-right">
                            <button type="button"
                                    class="btn btn-ghost btn-xs h-auto min-h-0 px-1 text-xs text-base-content/60 hover:text-base-content"
                                    data-sort-key="evaluationProfit"
                                    data-sort-type="number">
                                <span>${evaluationProfitLabel}</span>
                                <span data-sort-indicator class="opacity-60">↕</span>
                            </button>
                        </th>
                        <th class="text-right">
                            <button type="button"
                                    class="btn btn-ghost btn-xs h-auto min-h-0 px-1 text-xs text-base-content/60 hover:text-base-content"
                                    data-sort-key="weight"
                                    data-sort-type="number">
                                <span>${weightLabel}</span>
                                <span data-sort-indicator class="opacity-60">↕</span>
                            </button>
                        </th>
                    </tr>
                </thead>
                <tbody>
                    @if(stockAggregated == null || stockAggregated.isEmpty())
                        <tr>
                            <td colspan="8" class="text-center py-8 opacity-50">${noHoldingsLabel}</td>
                        </tr>
                    @else
                        @for(TradeProfit item : stockAggregated)
                            !{BigDecimal evalProfitRate = item.totalBuyCost() != null
                                && item.totalBuyCost().compareTo(BigDecimal.ZERO) > 0
                                && item.evaluationProfit() != null
                                ? item.evaluationProfit().divide(item.totalBuyCost(), 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                                : BigDecimal.ZERO;}
                            !{BigDecimal totalWeightPct = totalEvaluationAmount != null
                                && totalEvaluationAmount.compareTo(BigDecimal.ZERO) > 0
                                && item.evaluationAmount() != null
                                ? item.evaluationAmount().divide(totalEvaluationAmount, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                                : BigDecimal.ZERO;}
                            <tr class="hover"
                                data-sortable-row
                                data-stock-name="${item.stockItemName() != null ? item.stockItemName() : ""}"
                                data-quantity="${BigDecimal.valueOf(item.holdingQuantity()).toPlainString()}"
                                data-average-cost="${item.averageBuyPrice() != null ? item.averageBuyPrice().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                data-buy-amount="${item.totalBuyCost() != null ? item.totalBuyCost().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                data-current-price="${item.currentPrice() != null ? item.currentPrice().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                data-evaluation-amount="${item.evaluationAmount() != null ? item.evaluationAmount().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                data-evaluation-profit="${item.evaluationProfit() != null ? item.evaluationProfit().toPlainString() : BigDecimal.ZERO.toPlainString()}"
                                data-weight="${totalWeightPct.toPlainString()}">
                                <td class="font-semibold">${item.stockItemName() != null ? item.stockItemName() : "-"}</td>
                                <td class="text-right font-mono hidden lg:table-cell amount-value">${String.format("%,d", item.holdingQuantity())}</td>
                                <td class="text-right font-mono hidden xl:table-cell amount-value">${String.format("%,d", item.averageBuyPrice() != null ? item.averageBuyPrice().setScale(0, java.math.RoundingMode.HALF_UP).longValue() : 0)}</td>
                                <td class="text-right font-mono hidden sm:table-cell amount-value opacity-70">${String.format("%,d", item.totalBuyCost() != null ? item.totalBuyCost().longValue() : 0)}</td>
                                <td class="text-right font-mono hidden lg:table-cell amount-value">${String.format("%,d", item.currentPrice() != null ? item.currentPrice().longValue() : 0)}</td>
                                <td class="text-right font-mono font-semibold amount-value text-principal">${String.format("%,d", item.evaluationAmount() != null ? item.evaluationAmount().longValue() : 0)}</td>
                                <td class="text-right font-mono font-bold ${item.evaluationProfit() != null && item.evaluationProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                                    <span class="amount-value">${String.format("%+,d", item.evaluationProfit() != null ? item.evaluationProfit().longValue() : 0)}</span>
                                    <span class="text-xs opacity-70">(${String.format("%+.1f%%", evalProfitRate.doubleValue())})</span>
                                </td>
                                <td class="text-right font-mono text-base-content/70">${String.format("%.1f%%", totalWeightPct.doubleValue())}</td>
                            </tr>
                        @endfor
                    @endif
                </tbody>
                <tfoot class="bg-base-200 font-bold">
                    !{BigDecimal totalStockBuyAmount = stockAggregated != null
                        ? stockAggregated.stream().map(i -> i.totalBuyCost() != null ? i.totalBuyCost() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add)
                        : BigDecimal.ZERO;}
                    <tr>
                        <td>${totalLabel}</td>
                        <td class="hidden lg:table-cell"></td>
                        <td class="hidden xl:table-cell"></td>
                        <td class="text-right font-mono hidden sm:table-cell amount-value opacity-70">${String.format("%,d", totalStockBuyAmount.longValue())}</td>
                        <td class="hidden lg:table-cell"></td>
                        <td class="text-right font-mono amount-value text-principal">${String.format("%,d", totalEvaluationAmount != null ? totalEvaluationAmount.longValue() : 0)}</td>
                        <td class="text-right font-mono font-bold amount-value ${totalEvaluationProfit != null && totalEvaluationProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">${String.format("%+,d", totalEvaluationProfit != null ? totalEvaluationProfit.longValue() : 0)}</td>
                        <td class="text-right font-mono text-base-content/70">${totalEvaluationAmount != null && totalEvaluationAmount.compareTo(BigDecimal.ZERO) > 0 ? "100.0%" : "0.0%"}</td>
                    </tr>
                </tfoot>
            </table>
        </div>
    </div>

</div>

<script>
    function toggleAssetStatusAccountDetail(button, rowId) {
        var detailRow = document.getElementById(rowId);
        if (!button || !detailRow) {
            return;
        }

        var expanded = button.getAttribute('aria-expanded') === 'true';
        var nextExpanded = !expanded;
        button.setAttribute('aria-expanded', nextExpanded ? 'true' : 'false');
        detailRow.classList.toggle('hidden', !nextExpanded);

        var labelEl = button.querySelector('[data-label]');
        if (labelEl) {
            labelEl.textContent = nextExpanded
                ? button.dataset.collapseLabel
                : button.dataset.expandLabel;
        }

        var iconEl = button.querySelector('[data-icon]');
        if (iconEl) {
            iconEl.textContent = nextExpanded ? '▲' : '▼';
        }
    }

    function getAssetStatusSortValue(row, sortKey, sortType) {
        if (!row || !sortKey) {
            return sortType === 'number' ? 0 : '';
        }

        var rawValue = row.dataset[sortKey];
        if (sortType === 'number') {
            var parsedValue = Number(rawValue);
            return Number.isFinite(parsedValue) ? parsedValue : 0;
        }

        return rawValue || '';
    }

    function compareAssetStatusSortValues(leftValue, rightValue, sortType) {
        if (sortType === 'number') {
            return leftValue - rightValue;
        }

        return String(leftValue).localeCompare(String(rightValue), 'ko', {
            numeric: true,
            sensitivity: 'base'
        });
    }

    function updateAssetStatusSortIndicators(table) {
        if (!table) {
            return;
        }

        var activeKey = table.dataset.sortKey;
        var direction = table.dataset.sortDirection;
        var buttons = table.querySelectorAll('[data-sort-key]');
        for (var i = 0; i < buttons.length; i++) {
            var button = buttons[i];
            var indicator = button.querySelector('[data-sort-indicator]');
            var headerCell = button.parentElement;
            var isActive = button.dataset.sortKey === activeKey;

            if (indicator) {
                indicator.textContent = isActive
                    ? (direction === 'asc' ? '▲' : '▼')
                    : '↕';
            }

            if (headerCell) {
                headerCell.setAttribute(
                    'aria-sort',
                    isActive
                        ? (direction === 'asc' ? 'ascending' : 'descending')
                        : 'none'
                );
            }
        }
    }

    function sortAssetStatusTable(table, sortKey, sortType) {
        if (!table || !sortKey || !sortType || !table.tBodies || table.tBodies.length === 0) {
            return;
        }

        var nextDirection;
        if (table.dataset.sortKey === sortKey) {
            nextDirection = table.dataset.sortDirection === 'asc' ? 'desc' : 'asc';
        } else {
            nextDirection = sortType === 'text' ? 'asc' : 'desc';
        }

        table.dataset.sortKey = sortKey;
        table.dataset.sortDirection = nextDirection;

        var tbody = table.tBodies[0];
        var rows = tbody.querySelectorAll('[data-sortable-row]');
        var rowGroups = [];

        for (var i = 0; i < rows.length; i++) {
            var row = rows[i];
            var detailRowId = row.dataset.detailRowId;
            var groupNodes = [row];

            if (detailRowId) {
                var detailRow = document.getElementById(detailRowId);
                if (detailRow && detailRow.parentElement === tbody) {
                    groupNodes.push(detailRow);
                }
            }

            rowGroups.push({
                row: row,
                groupNodes: groupNodes
            });
        }

        rowGroups.sort(function(left, right) {
            var compared = compareAssetStatusSortValues(
                getAssetStatusSortValue(left.row, sortKey, sortType),
                getAssetStatusSortValue(right.row, sortKey, sortType),
                sortType
            );

            if (compared === 0) {
                compared = compareAssetStatusSortValues(
                    left.row.dataset.account || left.row.dataset.stockName || '',
                    right.row.dataset.account || right.row.dataset.stockName || '',
                    'text'
                );
            }

            return nextDirection === 'asc' ? compared : -compared;
        });

        for (var j = 0; j < rowGroups.length; j++) {
            var group = rowGroups[j].groupNodes;
            for (var k = 0; k < group.length; k++) {
                tbody.appendChild(group[k]);
            }
        }

        updateAssetStatusSortIndicators(table);
    }

    function initializeAssetStatusSortableTables() {
        var tables = document.querySelectorAll('[data-sortable-table]');
        for (var i = 0; i < tables.length; i++) {
            var table = tables[i];
            if (table.dataset.sortableInitialized === 'true') {
                continue;
            }

            table.dataset.sortableInitialized = 'true';

            var buttons = table.querySelectorAll('[data-sort-key]');
            for (var j = 0; j < buttons.length; j++) {
                buttons[j].addEventListener('click', function() {
                    var targetTable = this.closest('[data-sortable-table]');
                    sortAssetStatusTable(targetTable, this.dataset.sortKey, this.dataset.sortType);
                });
            }

            updateAssetStatusSortIndicators(table);
        }
    }

    initializeAssetStatusSortableTables();
</script>
