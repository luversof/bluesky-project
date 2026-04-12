@import java.util.List
@import net.luversof.web.gate.stock.controller.StockTradeHtmxController.Activity
@import java.time.ZoneId
@import java.time.format.DateTimeFormatter

@param List<Activity> activities

<div class="card bg-base-100 shadow-xl">
    <div class="card-body">
        <div class="flex justify-between items-center mb-4">
            <h2 class="card-title text-xl">최근 활동</h2>
            <div class="join">
                <a href="/stock/activity" class="join-item btn btn-sm btn-ghost">전체보기</a>
            </div>
        </div>
        @if(activities == null || activities.isEmpty())
            <div class="text-center text-base-content/50 py-4">최근 활동 내역이 없습니다.</div>
        @else
            <ul class="space-y-1">
                @for(Activity activity : activities)
                    !{
                        String dotColor = "bg-base-300";
                        String badgeClass = "badge-neutral";
                        String badgeText = "";
                        String amountColor = "text-base-content";

                        if ("DIVIDEND".equals(activity.type())) {
                            dotColor = "bg-success";
                            badgeClass = "badge-success badge-outline";
                            badgeText = "배당";
                            amountColor = "text-dividend";
                        } else if ("TRADE".equals(activity.type())) {
                            if ("BUY".equals(activity.tradeType())) {
                                dotColor = "bg-error";
                                badgeClass = "badge-error badge-outline";
                                badgeText = "매수";
                                amountColor = "text-base-content/60";
                            } else {
                                dotColor = "bg-info";
                                badgeClass = "badge-info badge-outline";
                                badgeText = "매도";
                                amountColor = "text-info";
                            }
                        }
                    }
                    <li class="flex items-start gap-3 py-2 border-b border-base-200 last:border-0">
                        <div class="flex flex-col items-center gap-1 pt-1 shrink-0">
                            <div class="w-2.5 h-2.5 rounded-full ${dotColor}"></div>
                        </div>
                        <div class="flex-1 min-w-0">
                            <div class="flex items-center gap-2 mb-0.5">
                                <span class="badge badge-xs ${badgeClass}">${badgeText}</span>
                                <span class="font-bold text-sm truncate">${activity.stockItemName()}</span>
                            </div>
                            <div class="text-xs text-base-content/50">
                                @if("TRADE".equals(activity.type()))
                                    <span class="amount-value">${activity.quantity()}</span>주
                                @else
                                    ${activity.description()}
                                @endif
                            </div>
                        </div>
                        <div class="text-right shrink-0">
                            <div class="font-mono font-bold text-sm amount-value ${amountColor}">
                                ${String.format("%,d", activity.amount() != null ? activity.amount().longValue() : 0)} 원
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
</div>
