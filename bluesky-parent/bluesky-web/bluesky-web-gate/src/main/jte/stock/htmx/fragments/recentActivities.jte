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
            <ul class="steps steps-vertical w-full">
                @for(Activity activity : activities)
                    !{
                        String stepColor = "step-neutral";
                        String icon = "📝";
                        String amountColor = "text-base-content";
                        String prefix = "";
                        
                        if ("DIVIDEND".equals(activity.type())) {
                            icon = "🎁";
                            stepColor = "step-success";
                            amountColor = "text-dividend";
                            prefix = "";
                        } else if ("TRADE".equals(activity.type())) {
                            if ("BUY".equals(activity.tradeType())) {
                                icon = "🛒";
                                stepColor = "";
                                amountColor = "text-base-content/50";
                                prefix = "";
                            } else {
                                icon = "💰";
                                stepColor = "step-info";
                                amountColor = "text-info";
                                prefix = "";
                            }
                        }
                    }
                    <li class="step ${stepColor}" data-content="${icon}">
                        <div class="flex justify-between w-full text-left pl-2">
                            <div>
                                <div class="font-bold">${activity.stockItemName()}</div>
                                <div class="text-sm opacity-70">
                                    @if("TRADE".equals(activity.type()))
                                        ${"BUY".equals(activity.tradeType()) ? "매수" : "매도"} <span class="amount-value">${activity.quantity()}</span>주
                                    @else
                                        ${activity.description()}
                                    @endif
                                </div>
                            </div>
                            <div class="text-right">
                                <div class="font-mono font-bold amount-value ${amountColor}">
                                    ${prefix}${String.format("%,d", activity.amount() != null ? activity.amount().longValue() : 0)} 원
                                </div>
                                <div class="text-xs opacity-50 mt-1">
                                    ${activity.date() != null ? activity.date().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : ""}
                                </div>
                            </div>
                        </div>
                    </li>
                @endfor
            </ul>
        @endif
    </div>
</div>
