@param boolean isAuthenticated = false

<!-- Dashboard Layout -->
<div class="space-y-6">
    <!-- 1. Summary Cards + Charts + Top/Bottom Rankings -->
    <div hx-get="/stock/htmx/summary?showCharts=true" hx-trigger="load" hx-swap="innerHTML">
        <div class="flex justify-center p-4">
            <span class="loading loading-spinner loading-lg"></span>
        </div>
    </div>

    <!-- 2. Recent Activities (Full width) -->
    <div hx-get="/stock/htmx/recent-activities" hx-trigger="intersect once" hx-swap="outerHTML">
        <div class="flex justify-center p-10">
            <span class="loading loading-spinner loading-lg"></span>
        </div>
    </div>

    <!-- 3. Advanced Analytics (Multi-dimensional Analysis) -->
    <div hx-get="/stock/htmx/daily-summary/view" hx-trigger="intersect once" hx-swap="outerHTML">
         <div class="flex justify-center p-10">
            <span class="loading loading-spinner loading-lg"></span>
        </div>
    </div>

</div>
