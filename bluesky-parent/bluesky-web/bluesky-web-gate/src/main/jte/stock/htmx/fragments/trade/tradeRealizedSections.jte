@import net.luversof.web.gate.util.CspNonceHolder
@import java.util.List
@import java.math.BigDecimal
@import java.text.DecimalFormat
@import net.luversof.web.gate.stock.domain.TradeProfit
@import net.luversof.web.gate.stock.controller.StockTradeHtmxController.AccountRealizedRow
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.stock.util.StockFormatUtil
@import net.luversof.web.gate.stock.util.RealizedBasisGap

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
    // 이 절의 다른 숫자는 전부 '판 것' 기준인데 매수금액만 '산 것 전체' 기준이었다. 그래서 두 표의
    // 매수금액 합이 서로 달랐다(실측 2026-08-24: 계좌별 1,779,858,067 vs 종목별 1,658,970,083,
    // 차이 120,887,984 = 6.79%. 같은 종목을 한 계좌에서만 팔고 다른 계좌에선 들고 있으면 어떤
    // 필터로도 두 값을 맞출 수 없다). 옆 칸 수익률의 실제 분모인 매도원가로 바꿔 절 전체가 한 기준을
    // 쓰게 한다 - 매도금액·실현손익·거래세는 이미 두 표가 정확히 일치한다.
    String soldCostLabel = MessageUtil.getMessage("stock.realized.column.sold.cost");
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
                    <div class="text-xs text-base-content/60">${soldCostLabel}</div>
                    <div class="mt-1 font-mono font-semibold amount-value" data-trade-realized-sel-sold-cost>0</div>
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
                        <th scope="col" class="text-right">${soldCostLabel}</th>
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
                        data-sell="${(row.sellAmount() != null ? row.sellAmount() : BigDecimal.ZERO).toPlainString()}"
                        data-net="${rowNet.toPlainString()}"
                        data-sold-cost="${rowSoldCost.toPlainString()}">
                        <td class="font-semibold whitespace-nowrap">@if(row.accountId() != null)<a href="/stock/account?accountId=${row.accountId().toString()}" class="link link-hover">${row.name()}</a>@else${row.name()}@endif</td>
                        <td class="text-right font-mono amount-value">${decimalFormat.format(rowSoldCost)}</td>
                        <td class="text-right font-mono amount-value">${decimalFormat.format(row.sellAmount() != null ? row.sellAmount() : BigDecimal.ZERO)}</td>
                        <td class="text-right font-bold font-mono amount-value ${rowPositive ? "text-profit" : "text-loss"}">
                            ${String.format("%+,d", StockFormatUtil.displayWon(rowNet))}
                            <%-- 기록된 실현손익은 계좌를 합친 원가를 따른다(실측 2026-08-23: 매도 54건 중
                                 종목 단위 원가로 50건이 재현되고 계좌x종목 단위로는 38건뿐). 그래서 이 값은
                                 이 계좌의 매수와 맞지 않을 수 있다 - 연금저축1 415,053 vs 2,063,739,
                                 ISA 1,555,597 vs 14,921. 크게 갈릴 때만 밝힌다(작은 차이까지 적으면 소음). --%>
                            !{BigDecimal rowOwn = row.realizedOwnBasis() != null ? row.realizedOwnBasis() : BigDecimal.ZERO;}
                            !{BigDecimal rowBasisGap = RealizedBasisGap.amount(rowNet, rowOwn);}
                            @if(RealizedBasisGap.isNotable(rowNet, rowOwn))
                                <span class="block text-[10px] font-normal text-base-content/60" data-realized-basis-gap="${StockFormatUtil.displayWon(rowBasisGap)}"
                                      title="${MessageUtil.getMessage("stock.trade.realized.basis.gap.title")}">
                                    ${java.text.MessageFormat.format(MessageUtil.getMessage("stock.trade.realized.basis.gap"), String.format("%+,d", StockFormatUtil.displayWon(rowOwn)))}
                                </span>
                            @endif
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
                    <div class="text-xs text-base-content/60">${soldCostLabel}</div>
                    <div class="mt-1 font-mono font-semibold amount-value" data-trade-realized-sel-sold-cost>0</div>
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
                        <th scope="col" class="text-right">${soldCostLabel}</th>
                        <th scope="col" class="text-right">${sellAmountLabel}</th>
                        <th scope="col" class="text-right">${realizedProfitLabel}</th>
                        <th scope="col" class="text-right">${returnRateLabel}</th>
                    </tr>
                </thead>
                <tbody>
                @for(TradeProfit item : stockRealizedList)
                    <%-- 실현손익은 거래에 기록된 값(증권사 기준)으로 통일한다. 계좌별 표·헤드라인·거래목록과 같은 기준이라야 합이 맞는다. --%>
                    !{BigDecimal itemNet = item.realizedProfit() != null ? item.realizedProfit() : BigDecimal.ZERO;}
                    <%-- 매도 원가 = 매도금액 - 증권거래세 - 기록된 실현손익. 기록된 실현손익은 증권사가 세금까지
                         뺀 뒤의 값이라 세금을 빼지 않으면 원가가 부풀고, 아래 선택 합계의 수익률(net/soldCost)도
                         그만큼 낮게 나온다. 계좌별 표와 같은 식을 쓴다. --%>
                    !{BigDecimal itemSellTax = item.totalSellTax() != null ? item.totalSellTax() : BigDecimal.ZERO;}
                    !{BigDecimal itemSoldCost = item.totalSellAmount() != null ? item.totalSellAmount().subtract(itemSellTax).subtract(itemNet) : BigDecimal.ZERO;}
                    !{boolean itemHasRate = itemSoldCost.compareTo(BigDecimal.ZERO) > 0;}
                    !{double itemRate = itemHasRate ? itemNet.doubleValue() / itemSoldCost.doubleValue() * 100 : 0.0;}
                    !{boolean itemPositive = itemNet.compareTo(BigDecimal.ZERO) >= 0;}
                    <tr class="cursor-pointer"
                        data-trade-realized-row
                        tabindex="0"
                        role="button"
                        aria-pressed="false"
                        data-sell="${(item.totalSellAmount() != null ? item.totalSellAmount() : BigDecimal.ZERO).toPlainString()}"
                        data-net="${itemNet.toPlainString()}"
                        data-sold-cost="${itemSoldCost.toPlainString()}">
                        <td class="font-semibold whitespace-nowrap"><a href="/stock/item?stockItemId=${item.stockItemId() != null ? item.stockItemId().toString() : ""}" class="link link-hover">${item.stockItemName() != null ? item.stockItemName() : "-"}</a></td>
                        <td class="text-right font-mono amount-value">${decimalFormat.format(itemSoldCost)}</td>
                        <td class="text-right font-mono amount-value">${decimalFormat.format(item.totalSellAmount() != null ? item.totalSellAmount() : BigDecimal.ZERO)}</td>
                        <td class="text-right font-bold font-mono amount-value ${itemPositive ? "text-profit" : "text-loss"}">
                            ${String.format("%+,d", StockFormatUtil.displayWon(itemNet))}
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
                    var sell = 0, net = 0, soldCost = 0;
                    for (var i = 0; i < selected.length; i++) {
                        sell += Number(selected[i].dataset.sell || '0');
                        net += Number(selected[i].dataset.net || '0');
                        soldCost += Number(selected[i].dataset.soldCost || '0');
                    }
                    var countEl = summary.querySelector('[data-trade-realized-selection-count]');
                    if (countEl) countEl.textContent = (summary.dataset.countTemplate || '{0}').replace('{0}', selected.length);
                    putAmount('[data-trade-realized-sel-sold-cost]', soldCost, false);
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
