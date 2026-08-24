@import net.luversof.web.gate.util.CspNonceHolder
@import java.util.List
@import java.math.BigDecimal
@import net.luversof.web.gate.stock.dto.response.HoldingsSnapshotItem
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.stock.util.StockFormatUtil

@param List<HoldingsSnapshotItem> holdings
@param String date

!{
    String snapshotTitle = MessageUtil.getMessage("stock.holdings.snapshot.title");
    String asOfLabel = date != null && !date.isEmpty()
        ? java.text.MessageFormat.format(MessageUtil.getMessage("common.label.as.of"), date)
        : null;
    String closeLabel = MessageUtil.getMessage("common.button.close");
    String noDataLabel = MessageUtil.getMessage("stock.holdings.snapshot.message.no.data");
    String savedDateOnlyLabel = MessageUtil.getMessage("stock.holdings.snapshot.message.saved.dates.only");
    String totalValueLabel = MessageUtil.getMessage("stock.profit.evaluation.amount");
    String totalUnrealizedLabel = MessageUtil.getMessage("stock.profit.unrealized");
    String principalLabel = MessageUtil.getMessage("stock.label.principal");
    String stockNameLabel = MessageUtil.getMessage("stock.activity.label.stock.name");
    String quantityLabel = MessageUtil.getMessage("common.label.quantity");
    String averageCostLabel = MessageUtil.getMessage("stock.column.average.cost");
    String referencePriceLabel = MessageUtil.getMessage("stock.column.reference.price");
    String evaluationAmountLabel = MessageUtil.getMessage("stock.profit.evaluation.amount");
    String evaluationProfitLabel = MessageUtil.getMessage("stock.profit.unrealized");
    String returnRateLabel = MessageUtil.getMessage("stock.label.return.rate");
}

@template._components.ui.card(cssClass = "mt-4", body = @`
        <div class="flex items-center justify-between mb-3">
            <h3 class="card-title text-base">
                &#128203; ${snapshotTitle}
                @if(asOfLabel != null)
                    <span class="text-sm font-normal text-base-content/60 ml-2">${asOfLabel}</span>
                @endif
            </h3>
            <button type="button" class="btn btn-xs btn-ghost btn-circle"
                    data-holdings-snapshot-close
                    title="${closeLabel}">✕</button>
        </div>

        @if(holdings == null || holdings.isEmpty())
            @template._components.ui.emptyState(message = noDataLabel, hint = savedDateOnlyLabel, padding = "py-6")
        @else
            !{
                // 원금은 평가액에서 평가손익을 빼서 얻는다. avgCost 는 표기용으로 소수 2 자리 반올림돼 오므로
                // avgCost x 수량 으로 만들면 정밀 원가와 어긋난다. api-stock 은 unrealizedProfit 을
                // value - totalCost 로 만들므로 value - unrealizedProfit 이 곧 그 totalCost 다.
                // 실측 2026-08-24: 반올림으로 만들면 이 표의 합계 세 칸이 서로 맞지 않았다
                // (2026-08-19 총평가 - 총원금 - 총평가손익 = 28원, 2026-06-18 은 -22원).
                BigDecimal totalPrincipal = holdings.stream()
                    .map(item -> item.value() != null
                        ? item.value().subtract(item.unrealizedProfit() != null ? item.unrealizedProfit() : BigDecimal.ZERO)
                        : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalValue = holdings.stream()
                    .map(HoldingsSnapshotItem::value)
                    .filter(v -> v != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalUnrealized = holdings.stream()
                    .map(HoldingsSnapshotItem::unrealizedProfit)
                    .filter(v -> v != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                boolean totalPositive = totalUnrealized.compareTo(BigDecimal.ZERO) >= 0;
                // 고른 날짜에 시세가 없으면 그 이전 마지막 종가로 계산된다. 표의 "기준가"가 고른 날짜의
                // 시세로 보이면 안 되므로, 실제 종가 일자가 고른 날짜보다 이르면 그 사실을 밝힌다
                // (실측: 2026-08-22 를 고르면 수집이 2026-08-20 에서 멈춰 있어 그 날 종가가 쓰인다).
                java.time.LocalDate latestPriceDate = holdings.stream()
                    .map(HoldingsSnapshotItem::priceDate)
                    .filter(v -> v != null)
                    .max(java.time.LocalDate::compareTo)
                    .orElse(null);
                boolean priceOlderThanRequested = latestPriceDate != null && date != null && !date.isEmpty()
                    && latestPriceDate.isBefore(java.time.LocalDate.parse(date));
            }
            <div class="grid gap-4 mb-3 text-sm md:grid-cols-3">
                <div class="stat bg-base-200 rounded-box py-2 px-4">
                    <div class="stat-title text-xs">${principalLabel}</div>
                    <div class="stat-value text-base amount-value">&#x20A9;${String.format("%,d", StockFormatUtil.displayWon(totalPrincipal))}</div>
                </div>
                <div class="stat bg-base-200 rounded-box py-2 px-4">
                    <div class="stat-title text-xs">${totalValueLabel}</div>
                    <div class="stat-value text-base amount-value">&#x20A9;${String.format("%,d", StockFormatUtil.displayWon(totalValue))}</div>
                </div>
                <div class="stat bg-base-200 rounded-box py-2 px-4">
                    <div class="stat-title text-xs">${totalUnrealizedLabel}</div>
                    <div class="stat-value text-base amount-value ${totalPositive ? "text-profit" : "text-loss"}">
                        ${totalPositive ? "+" : ""}&#x20A9;${String.format("%,d", StockFormatUtil.displayWon(totalUnrealized))}
                    </div>
                </div>
            </div>
            @if(priceOlderThanRequested)
                <p class="mb-2 text-xs text-base-content/60">${MessageUtil.getMessage("stock.asset.status.price.basis").replace("{0}", latestPriceDate.toString())}</p>
            @endif
            <div class="overflow-x-auto">
                <table class="table table-xs table-zebra w-full">
                    <thead>
                        <tr class="text-xs text-base-content/60">
                            <th scope="col">${stockNameLabel}</th>
                            <th scope="col" class="text-right">${quantityLabel}</th>
                            <th scope="col" class="text-right">${averageCostLabel}</th>
                            <th scope="col" class="text-right">${principalLabel}</th>
                            <th scope="col" class="text-right">${referencePriceLabel}</th>
                            <th scope="col" class="text-right">${evaluationAmountLabel}</th>
                            <th scope="col" class="text-right">${evaluationProfitLabel}</th>
                            <th scope="col" class="text-right">${returnRateLabel}</th>
                        </tr>
                    </thead>
                    <tbody>
                        @for(HoldingsSnapshotItem item : holdings)
                        !{
                            boolean isProfit = item.unrealizedProfit() != null && item.unrealizedProfit().compareTo(BigDecimal.ZERO) >= 0;
                            String colorClass = isProfit ? "text-profit" : "text-loss";
                            String sign = isProfit ? "+" : "";
                            // 위 합계와 같은 기준(평가액 - 평가손익)으로 만든다.
                            BigDecimal cost = item.value() != null
                                ? item.value().subtract(item.unrealizedProfit() != null ? item.unrealizedProfit() : BigDecimal.ZERO)
                                : BigDecimal.ZERO;
                            String pct = cost.compareTo(BigDecimal.ZERO) != 0 && item.unrealizedProfit() != null
                                ? String.format("%.2f", item.unrealizedProfit().divide(cost, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue())
                                : "0.00";
                            // 수량은 정수로 표시
                            String qtyDisplay = item.quantity() != null
                                ? String.format("%,d", StockFormatUtil.displayWon(item.quantity()))
                                : "0";
                        }
                        <tr>
                            <td>
                                <div class="font-medium">${item.name()}</div>
                                @if(item.symbol() != null && !item.symbol().isEmpty())
                                    <div class="text-xs text-base-content/60">${item.symbol()}</div>
                                @endif
                            </td>
                            <td class="text-right amount-value">${qtyDisplay}</td>
                            <td class="text-right amount-value">${item.avgCost() != null ? "₩" + String.format("%,d", StockFormatUtil.displayWon(item.avgCost())) : "-"}</td>
                            <td class="text-right amount-value">${cost.compareTo(BigDecimal.ZERO) > 0 ? "₩" + String.format("%,d", StockFormatUtil.displayWon(cost)) : "-"}</td>
                            <td class="text-right amount-value">${item.priceAtDate() != null ? "₩" + String.format("%,d", StockFormatUtil.displayWon(item.priceAtDate())) : "-"}</td>
                            <td class="text-right font-medium amount-value">${item.value() != null ? "₩" + String.format("%,d", StockFormatUtil.displayWon(item.value())) : "-"}</td>
                            <td class="text-right font-medium ${colorClass} amount-value">
                                ${sign}₩${item.unrealizedProfit() != null ? String.format("%,d", StockFormatUtil.displayWon(item.unrealizedProfit())) : "0"}
                            </td>
                            <td class="text-right ${colorClass}">${sign}${pct}%</td>
                        </tr>
                        @endfor
                    </tbody>
                </table>
            </div>
        @endif
`)

<script nonce="${CspNonceHolder.getNonce()}">
    (function() {
        var buttons = document.querySelectorAll('[data-holdings-snapshot-close]');
        for (var i = 0; i < buttons.length; i++) {
            var button = buttons[i];
            if (button.dataset.holdingsSnapshotCloseBound === 'true') {
                continue;
            }
            button.dataset.holdingsSnapshotCloseBound = 'true';
            button.addEventListener('click', function() {
                document.getElementById('holdings-snapshot-container').innerHTML = '';
            });
        }
    })();
</script>
