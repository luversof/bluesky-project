@import java.util.List
@import java.math.BigDecimal
@import net.luversof.web.gate.stock.dto.response.HoldingsSnapshotItem
@import io.github.luversof.boot.context.support.MessageUtil

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
                    onclick="document.getElementById('holdings-snapshot-container').innerHTML='';"
                    title="${closeLabel}">✕</button>
        </div>

        @if(holdings == null || holdings.isEmpty())
            @template._components.ui.emptyState(message = noDataLabel, hint = savedDateOnlyLabel, padding = "py-6")
        @else
            !{
                BigDecimal totalPrincipal = holdings.stream()
                    .map(item -> item.avgCost() != null && item.quantity() != null && item.quantity().compareTo(BigDecimal.ZERO) > 0
                        ? item.avgCost().multiply(item.quantity())
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
            }
            <div class="grid gap-4 mb-3 text-sm md:grid-cols-3">
                <div class="stat bg-base-200 rounded-box py-2 px-4">
                    <div class="stat-title text-xs">${principalLabel}</div>
                    <div class="stat-value text-base amount-value">&#x20A9;${String.format("%,d", totalPrincipal.longValue())}</div>
                </div>
                <div class="stat bg-base-200 rounded-box py-2 px-4">
                    <div class="stat-title text-xs">${totalValueLabel}</div>
                    <div class="stat-value text-base amount-value">&#x20A9;${String.format("%,d", totalValue.longValue())}</div>
                </div>
                <div class="stat bg-base-200 rounded-box py-2 px-4">
                    <div class="stat-title text-xs">${totalUnrealizedLabel}</div>
                    <div class="stat-value text-base amount-value ${totalPositive ? "text-profit" : "text-loss"}">
                        ${totalPositive ? "+" : ""}&#x20A9;${String.format("%,d", totalUnrealized.longValue())}
                    </div>
                </div>
            </div>
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
                            BigDecimal cost = item.avgCost() != null && item.quantity() != null && item.quantity().compareTo(BigDecimal.ZERO) > 0
                                ? item.avgCost().multiply(item.quantity())
                                : BigDecimal.ZERO;
                            String pct = cost.compareTo(BigDecimal.ZERO) != 0 && item.unrealizedProfit() != null
                                ? String.format("%.2f", item.unrealizedProfit().divide(cost, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue())
                                : "0.00";
                            // 수량은 정수로 표시
                            String qtyDisplay = item.quantity() != null
                                ? String.format("%,d", item.quantity().longValue())
                                : "0";
                        }
                        <tr>
                            <td>
                                <div class="font-medium">${item.name()}</div>
                                @if(item.symbol() != null && !item.symbol().isEmpty())
                                    <div class="text-xs text-base-content/50">${item.symbol()}</div>
                                @endif
                            </td>
                            <td class="text-right amount-value">${qtyDisplay}</td>
                            <td class="text-right amount-value">${item.avgCost() != null ? "₩" + String.format("%,d", item.avgCost().longValue()) : "-"}</td>
                            <td class="text-right amount-value">${cost.compareTo(BigDecimal.ZERO) > 0 ? "₩" + String.format("%,d", cost.longValue()) : "-"}</td>
                            <td class="text-right amount-value">${item.priceAtDate() != null ? "₩" + String.format("%,d", item.priceAtDate().longValue()) : "-"}</td>
                            <td class="text-right font-medium amount-value">${item.value() != null ? "₩" + String.format("%,d", item.value().longValue()) : "-"}</td>
                            <td class="text-right font-medium ${colorClass} amount-value">
                                ${sign}₩${item.unrealizedProfit() != null ? String.format("%,d", item.unrealizedProfit().longValue()) : "0"}
                            </td>
                            <td class="text-right ${colorClass}">${sign}${pct}%</td>
                        </tr>
                        @endfor
                    </tbody>
                </table>
            </div>
        @endif
`)
