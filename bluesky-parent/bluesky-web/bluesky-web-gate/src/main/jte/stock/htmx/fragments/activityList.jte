@import java.util.List
@import net.luversof.web.gate.stock.controller.StockTradeHtmxController.Activity
@import java.time.ZoneId
@import java.time.format.DateTimeFormatter

@param List<Activity> activities

<div class="card bg-base-100 shadow-xl">
    <div class="card-body">
        @if(activities == null || activities.isEmpty())
            <div class="text-center text-base-content/50 py-10">활동 내역이 없습니다.</div>
        @else
            <div class="overflow-x-auto">
                <table class="table table-zebra w-full">
                    <thead>
                        <tr>
                            <th>일자</th>
                            <th>유형</th>
                            <th>종목명</th>
                            <th>상세</th>
                            <th class="text-right">금액</th>
                        </tr>
                    </thead>
                    <tbody>
                        @for(Activity activity : activities)
                            <tr>
                                <td class="whitespace-nowrap">
                                    ${activity.date() != null ? activity.date().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : ""}
                                </td>
                                <td>
                                    @if("TRADE".equals(activity.type()))
                                        <span class="badge badge-sm badge-outline ${"BUY".equals(activity.tradeType()) ? "badge-error" : "badge-info"}">${"BUY".equals(activity.tradeType()) ? "매수" : "매도"}</span>
                                    @else
                                        <span class="badge badge-sm badge-outline badge-success">배당</span>
                                    @endif
                                </td>
                                <td class="font-bold">${activity.stockItemName()}</td>
                                <td>
                                    @if("TRADE".equals(activity.type()))
                                        <span class="amount-value">${activity.quantity()}</span>주
                                    @else
                                        ${activity.description()}
                                    @endif
                                    
                                    <!-- Account Badges (Max 2 + Tooltip) -->
                                    @if(activity.accountNames() != null && !activity.accountNames().isEmpty())
                                        <div class="mt-1 flex flex-wrap gap-1">
                                            @for(int i = 0; i < Math.min(2, activity.accountNames().size()); i++)
                                                <span class="badge badge-xs badge-outline opacity-70">${activity.accountNames().get(i)}</span>
                                            @endfor
                                            
                                            @if(activity.accountNames().size() > 2)
                                                <div class="tooltip tooltip-right" data-tip="${String.join(", ", activity.accountNames())}">
                                                    <span class="badge badge-xs badge-outline opacity-70 cursor-help">+${activity.accountNames().size() - 2}</span>
                                                </div>
                                            @endif
                                        </div>
                                    @endif
                                </td>
                                <td class="text-right font-mono font-bold amount-value ${"DIVIDEND".equals(activity.type()) ? "text-dividend" : ""}">
                                    ${String.format("%,d", activity.amount() != null ? activity.amount().longValue() : 0)} 원
                                </td>
                            </tr>
                        @endfor
                    </tbody>
                </table>
            </div>
        @endif
    </div>
</div>
