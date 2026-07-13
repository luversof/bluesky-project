@param boolean isAuthenticated = false

<!-- Dashboard Layout -->
<div class="space-y-6">
    <!-- 1. 요약 허브: 총자산/통합손익 + 자산 추이 미니 차트 + 지표 스트립 -->
    <div hx-get="/stock/htmx/summary" hx-trigger="load" hx-swap="innerHTML">
        <%-- 로딩 스켈레톤: 히어로 2열 + 지표 4칸 형태를 흉내낸다 --%>
        <div class="flex flex-col gap-4 animate-pulse" aria-hidden="true">
            <div class="grid grid-cols-1 lg:grid-cols-2 gap-3">
                <div class="card bg-base-100 shadow-sm"><div class="card-body p-5 gap-3">
                    <div class="h-3 w-24 rounded bg-base-300"></div>
                    <div class="h-9 w-48 rounded bg-base-300"></div>
                    <div class="h-3 w-36 rounded bg-base-200"></div>
                    <div class="h-6 w-full rounded bg-base-200 mt-3"></div>
                </div></div>
                <div class="card bg-base-100 shadow-sm"><div class="card-body p-4 gap-2">
                    <div class="h-3 w-32 rounded bg-base-300"></div>
                    <div class="h-36 w-full rounded bg-base-200"></div>
                </div></div>
            </div>
            <div class="grid grid-cols-2 lg:grid-cols-4 gap-3">
                <div class="h-20 rounded-box bg-base-200"></div>
                <div class="h-20 rounded-box bg-base-200"></div>
                <div class="h-20 rounded-box bg-base-200"></div>
                <div class="h-20 rounded-box bg-base-200"></div>
            </div>
        </div>
    </div>

    <!-- 2. 최근 활동 + 다가올 배당 + 보유 비중 (2열/3열) -->
    <div class="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-4">
        <div hx-get="/stock/htmx/recent-activities" hx-trigger="intersect once" hx-swap="outerHTML">
            @template.stock.htmx.fragments.cardSkeleton()
        </div>
        <div hx-get="/stock/htmx/summary/upcoming-dividends" hx-trigger="load" hx-swap="outerHTML">
            @template.stock.htmx.fragments.cardSkeleton()
        </div>
        <div hx-get="/stock/htmx/summary/allocation" hx-trigger="load" hx-swap="outerHTML">
            @template.stock.htmx.fragments.cardSkeleton()
        </div>
    </div>
</div>
