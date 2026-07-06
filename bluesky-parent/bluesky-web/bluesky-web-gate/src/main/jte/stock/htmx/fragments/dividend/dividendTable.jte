@import java.util.List
@import java.math.BigDecimal
@import java.text.DecimalFormat
@import java.time.format.DateTimeFormatter
@import net.luversof.web.gate.stock.dto.response.DividendView
@import net.luversof.web.common.menu.domain.Pagination
@import io.github.luversof.boot.context.support.MessageUtil

@param List<DividendView> dividendList
@param Pagination pagination
@param String sort
@param DecimalFormat decimalFormat
@param java.util.function.Function<BigDecimal, String> percentFormat
@param String detailSummaryLabel
@param BigDecimal totalGrossAmount
@param BigDecimal totalNetAmount
@param BigDecimal totalTax
@param BigDecimal totalTaxableAmount
@param String accountLabel
@param String stockNameLabel
@param String grossAmountLabel
@param String netAmountLabel
@param String taxLabel
@param String taxableAmountLabel
@param String totalLabel

!{
    String sortField = sort != null ? sort.split(",")[0] : "";
    String sortDir = sort != null && sort.contains(",") ? sort.split(",")[1] : "";
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    String dateLabel = MessageUtil.getMessage("common.label.date");
    String quantityLabel = MessageUtil.getMessage("common.label.quantity");
    String referencePriceLabel = MessageUtil.getMessage("stock.column.reference.price");
    String basisCostLabel = MessageUtil.getMessage("stock.dividend.yield.column.basis.cost");
    String yieldOnBasisCostLabel = MessageUtil.getMessage("stock.dividend.yield.column.yield.on.basis.cost");
    String noDividendHistoryLabel = MessageUtil.getMessage("stock.dividend.message.no.history");
}

    <!-- Collapsible Detail List -->
    <details id="detailSection" class="mb-4" data-persist-open="dividendDetail">
        <summary class="cursor-pointer select-none flex items-center gap-2 font-semibold text-sm p-3 bg-base-200 rounded-box hover:bg-base-300 transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 transition-transform detail-arrow" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
            </svg>
            ${detailSummaryLabel}
        </summary>

        <div id="dividendListContainer" class="flex flex-col relative mt-2">
            <div class="flex-1 min-h-0 max-lg:overflow-x-auto">
                <table class="table table-hover w-full table-sm table-pin-rows pin-top-nav">
                    <thead>
                    <tr class="bg-base-200">
                        <th scope="col" class="cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
                            hx-vals='{"sort": "payDate,${sortField.equals("payDate") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            ${dateLabel} @if(sortField.equals("payDate"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th scope="col" class="cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
                            hx-vals='{"sort": "stockItemName,${sortField.equals("stockItemName") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            ${stockNameLabel} @if(sortField.equals("stockItemName"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th scope="col" class="cursor-pointer hover:bg-base-300 hidden sm:table-cell" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
                            hx-vals='{"sort": "accountName,${sortField.equals("accountName") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            ${accountLabel} @if(sortField.equals("accountName"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th scope="col" class="text-right cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
                            hx-vals='{"sort": "grossAmount,${sortField.equals("grossAmount") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            ${grossAmountLabel} @if(sortField.equals("grossAmount"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th scope="col" class="text-right cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
                            hx-vals='{"sort": "netAmount,${sortField.equals("netAmount") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            ${netAmountLabel} @if(sortField.equals("netAmount"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th scope="col" class="text-right hidden md:table-cell">${quantityLabel}</th>
                        <th scope="col" class="text-right hidden xl:table-cell">${referencePriceLabel}</th>
                        <th scope="col" class="text-right hidden xl:table-cell">${basisCostLabel}</th>
                        <th scope="col" class="text-right">${yieldOnBasisCostLabel}</th>
                        <th scope="col" class="text-right cursor-pointer hover:bg-base-300 hidden lg:table-cell" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
                            hx-vals='{"sort": "tax,${sortField.equals("tax") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            ${taxLabel} @if(sortField.equals("tax"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                        <th scope="col" class="text-right cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
                            hx-vals='{"sort": "taxableAmount,${sortField.equals("taxableAmount") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
                            ${taxableAmountLabel} @if(sortField.equals("taxableAmount"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
                        </th>
                    </tr>
                    </thead>
                    <tbody>
                    @for(DividendView item : dividendList)
                    <tr>
                        <td>${item.payDate() != null ? dateFormatter.format(item.payDate().atZone(java.time.ZoneId.systemDefault())) : ""}</td>
                        <td class="font-bold"><a href="/stock/item?stockItemId=${item.stockItemId() != null ? item.stockItemId().toString() : ""}" class="link link-hover">${item.stockItemName()}</a></td>
                        <td class="hidden sm:table-cell"><a href="/stock/account?accountId=${item.accountId() != null ? item.accountId().toString() : ""}" class="link link-hover">${item.accountName()}</a></td>
                        <td class="text-right amount-value">${item.grossAmount() != null ? decimalFormat.format(item.grossAmount()) : "0"}</td>
                        <td class="text-right text-dividend font-bold amount-value">${item.netAmount() != null ? decimalFormat.format(item.netAmount()) : "0"}</td>
                        <td class="text-right hidden md:table-cell amount-value">${item.quantity() != null ? decimalFormat.format(item.quantity()) : "-"}</td>
                        <td class="text-right hidden xl:table-cell amount-value">${item.referencePrice() != null ? decimalFormat.format(item.referencePrice()) : "-"}</td>
                        <td class="text-right hidden xl:table-cell amount-value">${item.principalCost() != null ? decimalFormat.format(item.principalCost()) : "-"}</td>
                        <td class="text-right text-dividend font-medium">${percentFormat.apply(item.yieldOnCostPct())}</td>
                        <td class="text-right hidden lg:table-cell amount-value">${item.tax() != null ? decimalFormat.format(item.tax()) : "0"}</td>
                        <td class="text-right amount-value">${item.taxableAmount() != null ? decimalFormat.format(item.taxableAmount()) : "0"}</td>
                    </tr>
                    @endfor
                    @if(dividendList == null || dividendList.isEmpty())
                    <tr>
                        <td colspan="11">@template._components.ui.emptyState(message = noDividendHistoryLabel)</td>
                    </tr>
                    @endif
                    </tbody>
                    <tfoot>
                        <tr class="font-bold bg-base-200">
                            <td colspan="3" class="text-center sm:text-right sm:pr-4">${totalLabel}</td>
                            <td class="text-right font-mono amount-value">${totalGrossAmount != null ? decimalFormat.format(totalGrossAmount) : "0"}</td>
                            <td class="text-right font-mono text-dividend amount-value">${totalNetAmount != null ? decimalFormat.format(totalNetAmount) : "0"}</td>
                            <td class="text-right hidden md:table-cell">-</td>
                            <td class="text-right hidden xl:table-cell">-</td>
                            <td class="text-right hidden xl:table-cell">-</td>
                            <td class="text-right">-</td>
                            <td class="text-right font-mono hidden lg:table-cell amount-value">${totalTax != null ? decimalFormat.format(totalTax) : "0"}</td>
                            <td class="text-right font-mono amount-value">${totalTaxableAmount != null ? decimalFormat.format(totalTaxableAmount) : "0"}</td>
                        </tr>
                    </tfoot>
                </table>
            </div>

            @if(pagination != null && pagination.getTotalPage() > 1)
            @template._components.htmxPagination(
                pagination = pagination,
                url = "/stock/htmx/dividend/list",
                includeSelector = "#dividendSearchForm",
                targetSelector = "#dividendListFragment")
            @endif
        </div>
    </details>
