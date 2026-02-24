@import java.util.List
@import java.util.Map
@import java.math.BigDecimal
@import net.luversof.web.gate.stock.domain.TradeProfit

@param String sort = ""
@param String viewGroupBy = "ACCOUNT"
@param List<TradeProfit> tradeProfitList
@param Map<String, TradeProfit> accountTotalMap
@param BigDecimal totalEvaluationAmount
@param BigDecimal totalEvaluationProfit
@param BigDecimal totalRealizedProfit

!{String sortField = sort != null && !sort.isEmpty() ? sort.split(",")[0] : "";}
!{String sortDir = sort != null && sort.contains(",") ? sort.split(",")[1] : "";}

<div class="overflow-x-auto">
    <table class="table table-hover table-striped w-full">
        <thead>
        <tr class="bg-base-200">
            <th class="cursor-pointer hover:bg-base-300 ${"STOCK".equals(viewGroupBy) || "ACCOUNT".equals(viewGroupBy) ? "hidden" : ""}" 
                hx-get="/stock/htmx/portfolio" hx-target="#tab-content"
                hx-vals='{"sort": "accountName,${"accountName".equals(sortField) && "asc".equals(sortDir) ? "desc" : "asc"}", "viewGroupBy": "${viewGroupBy}"}'>
                계좌 @if("accountName".equals(sortField))<span>${"asc".equals(sortDir) ? "▲" : "▼"}</span>@endif
            </th>
            <th class="cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/portfolio" hx-target="#tab-content"
                hx-vals='{"sort": "stockItemName,${"stockItemName".equals(sortField) && "asc".equals(sortDir) ? "desc" : "asc"}", "viewGroupBy": "${viewGroupBy}"}'>
                종목명 @if("stockItemName".equals(sortField))<span>${"asc".equals(sortDir) ? "▲" : "▼"}</span>@endif
            </th>
            <th class="text-right cursor-pointer hover:bg-base-300 hidden lg:table-cell" hx-get="/stock/htmx/portfolio" hx-target="#tab-content"
                hx-vals='{"sort": "holdingQuantity,${"holdingQuantity".equals(sortField) && "asc".equals(sortDir) ? "desc" : "asc"}", "viewGroupBy": "${viewGroupBy}"}'>
                보유수량 @if("holdingQuantity".equals(sortField))<span>${"asc".equals(sortDir) ? "▲" : "▼"}</span>@endif
            </th>
            <th class="text-right cursor-pointer hover:bg-base-300 hidden xl:table-cell" hx-get="/stock/htmx/portfolio" hx-target="#tab-content"
                hx-vals='{"sort": "averageBuyPrice,${"averageBuyPrice".equals(sortField) && "asc".equals(sortDir) ? "desc" : "asc"}", "viewGroupBy": "${viewGroupBy}"}'>
                평균단가 @if("averageBuyPrice".equals(sortField))<span>${"asc".equals(sortDir) ? "▲" : "▼"}</span>@endif
            </th>
            <th class="text-right cursor-pointer hover:bg-base-300 hidden lg:table-cell" hx-get="/stock/htmx/portfolio" hx-target="#tab-content"
                hx-vals='{"sort": "currentPrice,${"currentPrice".equals(sortField) && "asc".equals(sortDir) ? "desc" : "asc"}", "viewGroupBy": "${viewGroupBy}"}'>
                현재가 @if("currentPrice".equals(sortField))<span>${"asc".equals(sortDir) ? "▲" : "▼"}</span>@endif
            </th>
            <th class="text-right cursor-pointer hover:bg-base-300 hidden md:table-cell" hx-get="/stock/htmx/portfolio" hx-target="#tab-content"
                hx-vals='{"sort": "evaluationAmount,${"evaluationAmount".equals(sortField) && "asc".equals(sortDir) ? "desc" : "asc"}", "viewGroupBy": "${viewGroupBy}"}'>
                평가금액 @if("evaluationAmount".equals(sortField))<span>${"asc".equals(sortDir) ? "▲" : "▼"}</span>@endif
            </th>
            <th class="text-right cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/portfolio" hx-target="#tab-content"
                hx-vals='{"sort": "evaluationProfit,${"evaluationProfit".equals(sortField) && "asc".equals(sortDir) ? "desc" : "asc"}", "viewGroupBy": "${viewGroupBy}"}'>
                평가손익 @if("evaluationProfit".equals(sortField))<span>${"asc".equals(sortDir) ? "▲" : "▼"}</span>@endif
            </th>
            <th class="text-right cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/portfolio" hx-target="#tab-content"
                hx-vals='{"sort": "realizedProfit,${"realizedProfit".equals(sortField) && "asc".equals(sortDir) ? "desc" : "asc"}", "viewGroupBy": "${viewGroupBy}"}'>
                실현손익 @if("realizedProfit".equals(sortField))<span>${"asc".equals(sortDir) ? "▲" : "▼"}</span>@endif
            </th>
        </tr>
        </thead>
        <tbody>
        @if(tradeProfitList != null)
            !{int index = 0;}
            @for(TradeProfit item : tradeProfitList)
                @if("ACCOUNT".equals(viewGroupBy) && (index == 0 || !item.accountName().equals(tradeProfitList.get(index - 1).accountName())))
                    <tr>
                        <!-- Distinct Account Header: Primary Tint + Left Align -->
                        <td colspan="8" class="bg-primary/10 font-bold text-left px-4 py-3">${item.accountName()}</td>
                    </tr>
                @endif
                <tr>
                    <td class="${"STOCK".equals(viewGroupBy) || "ACCOUNT".equals(viewGroupBy) ? "hidden" : ""}">${item.accountName()}</td>
                    <td class="font-bold pl-8">${item.stockItemName()}</td> <!-- Indent Item -->
                    <td class="text-right hidden lg:table-cell text-principal amount-value">${String.format("%,d", item.holdingQuantity())}</td>
                    <td class="text-right hidden xl:table-cell text-principal amount-value">${String.format("%,d", item.averageBuyPrice() != null ? item.averageBuyPrice().longValue() : 0)}</td>
                    <td class="text-right hidden lg:table-cell text-principal amount-value">${String.format("%,d", item.currentPrice() != null ? item.currentPrice().longValue() : 0)}</td>
                    <td class="text-right hidden md:table-cell text-principal amount-value">${String.format("%,d", item.evaluationAmount() != null ? item.evaluationAmount().longValue() : 0)}</td>
                    <td class="text-right font-bold amount-value ${item.evaluationProfit() != null && item.evaluationProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                        ${String.format("%,d", item.evaluationProfit() != null ? item.evaluationProfit().longValue() : 0)}
                    </td>
                    <td class="text-right amount-value ${item.realizedProfit() != null && item.realizedProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                        ${String.format("%,d", item.realizedProfit() != null ? item.realizedProfit().longValue() : 0)}
                    </td>
                </tr>
                @if("ACCOUNT".equals(viewGroupBy) && (index == tradeProfitList.size() - 1 || !item.accountName().equals(tradeProfitList.get(index + 1).accountName())))
                    <tr class="bg-base-200/50 font-bold border-t border-base-300">
                        <td class="${"STOCK".equals(viewGroupBy) || "ACCOUNT".equals(viewGroupBy) ? "hidden" : ""}"></td>
                        <td class="text-right pr-4">소계</td> <!-- Right Align Label adjacent to values -->
                        <td class="hidden lg:table-cell"></td>
                        <td class="hidden xl:table-cell"></td>
                        <td class="hidden lg:table-cell"></td>
                        <td class="text-right hidden md:table-cell text-principal opacity-80 amount-value">
                            ${String.format("%,d", accountTotalMap.get(item.accountName()).evaluationAmount() != null ? accountTotalMap.get(item.accountName()).evaluationAmount().longValue() : 0)}
                        </td>
                        <td class="text-right amount-value ${accountTotalMap.get(item.accountName()).evaluationProfit() != null && accountTotalMap.get(item.accountName()).evaluationProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                            ${String.format("%,d", accountTotalMap.get(item.accountName()).evaluationProfit() != null ? accountTotalMap.get(item.accountName()).evaluationProfit().longValue() : 0)}
                        </td>
                        <td class="text-right amount-value ${accountTotalMap.get(item.accountName()).realizedProfit() != null && accountTotalMap.get(item.accountName()).realizedProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                            ${String.format("%,d", accountTotalMap.get(item.accountName()).realizedProfit() != null ? accountTotalMap.get(item.accountName()).realizedProfit().longValue() : 0)}
                        </td>
                    </tr>
                @endif
                !{index++;}
            @endfor
        @endif
        </tbody>
        <tfoot>
        <tr class="bg-base-200 font-bold">
            <td class="${"STOCK".equals(viewGroupBy) || "ACCOUNT".equals(viewGroupBy) ? "hidden" : ""}"></td>
            <td class="text-center">합계</td>
            <td class="hidden lg:table-cell"></td>
            <td class="hidden xl:table-cell"></td>
            <td class="hidden lg:table-cell"></td>
            <td class="text-right hidden md:table-cell text-principal amount-value">${String.format("%,d", totalEvaluationAmount != null ? totalEvaluationAmount.longValue() : 0)}</td>
            <td class="text-right amount-value ${totalEvaluationProfit != null && totalEvaluationProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                ${String.format("%,d", totalEvaluationProfit != null ? totalEvaluationProfit.longValue() : 0)}
            </td>
            <td class="text-right amount-value ${totalRealizedProfit != null && totalRealizedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
                ${String.format("%,d", totalRealizedProfit != null ? totalRealizedProfit.longValue() : 0)}
            </td>
        </tr>
        </tfoot>
    </table>
</div>