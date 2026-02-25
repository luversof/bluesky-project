@import java.util.List
@import net.luversof.web.gate.stock.controller.StockHtmxController.Activity
@import java.time.ZoneId
@import java.time.format.DateTimeFormatter

@param List<Activity> activities

<div class="card bg-base-100 shadow-xl">
    <div class="card-body">
        <div class="flex justify-between items-center mb-4">
            <h2 class="card-title text-xl">최근 활동</h2>
            <div class="join">
                <a href="/stock/trade" class="join-item btn btn-sm btn-ghost">매매 내역</a>
                <a href="/stock/dividend" class="join-item btn btn-sm btn-ghost">배당 내역</a>
            </div>
        </div>
        @if(activities == null || activities.isEmpty())
            <div class="text-center text-base-content/50 py-4">최근 활동 내역이 없습니다.</div>
        @else
            <ul class="steps steps-vertical w-full">
                @for(Activity activity : activities)
                    <li class="step step-primary">
                        <div class="flex justify-between w-full text-left">
                            <div>
                                <div class="font-bold">${activity.stockItemName()}</div>
                                <div class="text-sm opacity-70">${activity.description()}</div>
                            </div>
                            <div class="text-right">
                                <div class="font-mono font-bold amount-value ${"DIVIDEND".equals(activity.type()) ? "text-dividend" : ""}">
                                    ${String.format("%,d", activity.amount() != null ? activity.amount().longValue() : 0)} 원
                                </div>
                                <div class="text-xs opacity-50">
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
