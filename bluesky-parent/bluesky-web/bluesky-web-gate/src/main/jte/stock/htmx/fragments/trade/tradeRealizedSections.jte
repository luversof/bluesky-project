@import net.luversof.web.gate.util.CspNonceHolder
@import java.util.List
@import java.math.BigDecimal
@import java.text.DecimalFormat
@import net.luversof.web.gate.stock.domain.TradeProfit
@import net.luversof.web.gate.stock.controller.StockTradeHtmxController.AccountRealizedRow
@import io.github.luversof.boot.context.support.MessageUtil

@param List<AccountRealizedRow> accountRealizedList
@param List<TradeProfit> stockRealizedList
@param long realizedWinCount
@param int realizedStockCount
@param DecimalFormat decimalFormat
@param String accountLabel
@param String stockLabel
@param String realizedProfitLabel

!{
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
}

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

    <script nonce="${CspNonceHolder.getNonce()}">
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
