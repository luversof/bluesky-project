@import java.util.List
@import java.util.Map
@import java.util.UUID
@import java.math.BigDecimal
@import java.time.format.DateTimeFormatter
@import java.text.DecimalFormat
@import net.luversof.web.gate.stock.dto.response.TradeResponse
@import net.luversof.web.common.menu.domain.Pagination
@import net.luversof.web.gate.stock.constant.TradeType
@import io.github.luversof.boot.context.support.MessageUtil

@param List<TradeResponse> tradeList
@param Pagination pagination
@param int totalItems
@param Map<UUID, String> accountNames
@param String sort
@param BigDecimal totalFee
@param BigDecimal totalTax
@param BigDecimal totalRealizedProfit
@param DecimalFormat decimalFormat
@param java.util.function.Function<Object, String> countMessage
@param String accountLabel
@param String stockLabel
@param String realizedProfitLabel
@param String feeLabel
@param String tradingTaxLabel
@param String buyLabel
@param String sellLabel

!{
    String sortField = sort != null ? sort.split(",")[0] : "";
    String sortDir = sort != null && sort.contains(",") ? sort.split(",")[1] : "";
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    String detailListLabel = MessageUtil.getMessage("stock.trade.label.detail.list");
    String dateLabel = MessageUtil.getMessage("common.label.date");
    String typeLabel = MessageUtil.getMessage("common.label.type");
    String unitPriceLabel = MessageUtil.getMessage("stock.trade.column.unit.price");
    String quantityLabel = MessageUtil.getMessage("common.label.quantity");
    String amountLabel = MessageUtil.getMessage("common.label.amount");
    String noTradeDataLabel = MessageUtil.getMessage("stock.trade.message.no.data");
    String totalLabelText = MessageUtil.getMessage("common.label.total");
    java.util.function.Function<TradeType, String> tradeTypeLabel = type -> type == TradeType.BUY ? buyLabel : sellLabel;
}

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
