@import java.util.List
@import java.math.BigDecimal
@import net.luversof.web.gate.stock.controller.StockTradeHtmxController.Activity
@import java.time.ZoneId
@import java.time.format.DateTimeFormatter
@import io.github.luversof.boot.context.support.MessageUtil

@param List<Activity> activities
@param String thisMonthLabel
@param long buyCount
@param long sellCount
@param BigDecimal buyAmount
@param BigDecimal sellAmount
@param long dividendCount
@param BigDecimal dividendAmount
@param java.util.Map<String, java.util.UUID> activityStockIdByName = java.util.Map.of()

!{
    java.util.function.Function<Object, String> countMessage = value -> java.text.MessageFormat.format(MessageUtil.getMessage("common.count"), value);
    String recentActivityTitle = MessageUtil.getMessage("stock.recent.activity.title");
    String viewAllLabel = MessageUtil.getMessage("common.button.view.all");
    String noActivityLabel = MessageUtil.getMessage("stock.activity.message.no.data");
    String dividendLabel = MessageUtil.getMessage("stock.activity.label.dividend");
    String buyLabel = MessageUtil.getMessage("stock.trade.label.buy");
    String sellLabel = MessageUtil.getMessage("stock.trade.label.sell");
    String sharesLabel = MessageUtil.getMessage("stock.activity.label.shares");
    String monthSummaryLabel = java.text.MessageFormat.format(MessageUtil.getMessage("stock.recent.activity.month.summary"), thisMonthLabel);
}

@template._components.ui.card(cssClass = "h-full", body = @`
    <div class="flex justify-between items-center mb-3">
        <h2 class="card-title text-xl">${recentActivityTitle}</h2>
        <a href="/stock/activity" class="link link-hover text-xs">${viewAllLabel} →</a>
    </div>
    <div class="flex gap-4">
        <%-- 좌: 활동 목록 --%>
        <div class="flex-1 min-w-0">
            @if(activities == null || activities.isEmpty())
                <div class="text-center text-base-content/50 py-4">${noActivityLabel}</div>
            @else
                <ul class="space-y-0">
                    @for(Activity activity : activities)
                        !{
                            String badgeClass = "badge-neutral";
                            String badgeText = "";
                            String amountColor = "text-base-content";

                            if ("DIVIDEND".equals(activity.type())) {
                                badgeClass = "badge-success badge-outline";
                                badgeText = dividendLabel;
                                amountColor = "text-dividend";
                            } else if ("TRADE".equals(activity.type())) {
                                if ("BUY".equals(activity.tradeType())) {
                                    badgeClass = "badge-error badge-outline";
                                    badgeText = buyLabel;
                                    amountColor = "text-base-content/60";
                                } else {
                                    badgeClass = "badge-info badge-outline";
                                    badgeText = sellLabel;
                                    amountColor = "text-info";
                                }
                            }
                        }
                        <li class="flex items-start gap-2 py-1.5 border-b border-base-200 last:border-0">
                            <div class="flex-1 min-w-0">
                                <div class="flex items-center gap-1.5 mb-0.5">
                                    <span class="badge badge-xs ${badgeClass}">${badgeText}</span>
                                    <a href="/stock/item?stockItemId=${activityStockIdByName.get(activity.stockItemName()) != null ? activityStockIdByName.get(activity.stockItemName()).toString() : ""}" class="font-bold text-sm truncate link link-hover">${activity.stockItemName()}</a>
                                </div>
                                <div class="text-xs text-base-content/50">
                                    @if("TRADE".equals(activity.type()))
                                        <span class="amount-value">${activity.quantity()}</span>${sharesLabel}
                                    @else
                                        ${activity.description()}
                                    @endif
                                </div>
                            </div>
                            <div class="text-right shrink-0">
                                <div class="font-mono font-bold text-xs amount-value ${amountColor}">
                                    ₩${String.format("%,d", activity.amount() != null ? activity.amount().longValue() : 0)}
                                </div>
                                <div class="text-xs text-base-content/40 mt-0.5">
                                    ${activity.date() != null ? activity.date().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MM-dd")) : ""}
                                </div>
                            </div>
                        </li>
                    @endfor
                </ul>
            @endif
        </div>
        <%-- 우: 이번 달 요약 --%>
        <div class="w-36 shrink-0 border-l border-base-200 pl-3">
            <div class="text-xs font-semibold text-base-content/50 mb-2">${monthSummaryLabel}</div>
            <div class="space-y-2">
                <div class="flex justify-between items-center">
                    <span class="badge badge-xs badge-error badge-outline">${buyLabel}</span>
                    <div class="text-right">
                        <div class="text-xs text-base-content/50">${countMessage.apply(buyCount)}</div>
                        <div class="font-mono text-xs font-bold amount-value text-base-content/70">
                            ₩${String.format("%,d", buyAmount != null ? buyAmount.longValue() : 0)}
                        </div>
                    </div>
                </div>
                <div class="flex justify-between items-center">
                    <span class="badge badge-xs badge-info badge-outline">${sellLabel}</span>
                    <div class="text-right">
                        <div class="text-xs text-base-content/50">${countMessage.apply(sellCount)}</div>
                        <div class="font-mono text-xs font-bold amount-value text-info">
                            ₩${String.format("%,d", sellAmount != null ? sellAmount.longValue() : 0)}
                        </div>
                    </div>
                </div>
                <div class="flex justify-between items-center">
                    <span class="badge badge-xs badge-success badge-outline">${dividendLabel}</span>
                    <div class="text-right">
                        <div class="text-xs text-base-content/50">${countMessage.apply(dividendCount)}</div>
                        <div class="font-mono text-xs font-bold amount-value text-dividend">
                            ₩${String.format("%,d", dividendAmount != null ? dividendAmount.longValue() : 0)}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
`)