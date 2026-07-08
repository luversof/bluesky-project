@param boolean isAuthenticated = false

<!-- Dashboard Layout -->
<div class="space-y-6">
    <!-- 1. 요약 허브: 총자산/통합손익 + 자산 추이 미니 차트 + 지표 스트립 -->
    <div hx-get="/stock/htmx/summary" hx-trigger="load" hx-swap="innerHTML">
        <div class="flex justify-center p-4">
            <span class="loading loading-spinner loading-lg text-primary"></span>
        </div>
    </div>

    <!-- 2. 최근 활동 + 다가올 배당 + 보유 비중 (2열/3열) -->
    <div class="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-4">
        <div hx-get="/stock/htmx/recent-activities" hx-trigger="intersect once" hx-swap="outerHTML">
            <div class="flex justify-center p-10">
                <span class="loading loading-spinner loading-lg text-primary"></span>
            </div>
        </div>
        <div hx-get="/stock/htmx/summary/upcoming-dividends" hx-trigger="load" hx-swap="outerHTML">
            <div class="flex justify-center p-10">
                <span class="loading loading-spinner loading-lg text-primary"></span>
            </div>
        </div>
        <div hx-get="/stock/htmx/summary/allocation" hx-trigger="load" hx-swap="outerHTML">
            <div class="flex justify-center p-10">
                <span class="loading loading-spinner loading-lg text-primary"></span>
            </div>
        </div>
    </div>
</div>
