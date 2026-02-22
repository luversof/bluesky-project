@param boolean isAuthenticated = false

<!-- Chart.js is required for chart fragments -->
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<!-- Dashboard Layout -->
<div class="container mx-auto p-4 space-y-6">

    <!-- Admin Actions -->
    @template.stock.htmx.fragments.adminActions(isAuthenticated = isAuthenticated)
    
    <!-- 1. Summary Cards (Lazy Load) -->
    <div hx-get="/stock/htmx/summary" hx-trigger="load" hx-swap="innerHTML">
        <div class="flex justify-center p-4">
            <span class="loading loading-spinner loading-lg"></span>
        </div>
    </div>

    <!-- 1.5. Portfolio / Current Holdings -->
    <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
            <div class="flex justify-between items-center mb-4">
                <h2 class="card-title text-xl">보유 종목 현황</h2>
                <div class="join">
                    <button class="join-item btn btn-sm btn-active" 
                            hx-get="/stock/htmx/portfolio?viewGroupBy=STOCK" 
                            hx-target="#tab-content"
                            onclick="document.querySelectorAll('.join-item').forEach(el => el.classList.remove('btn-active')); this.classList.add('btn-active');">종목별</button>
                    <button class="join-item btn btn-sm" 
                            hx-get="/stock/htmx/portfolio?viewGroupBy=ACCOUNT" 
                            hx-target="#tab-content"
                            onclick="document.querySelectorAll('.join-item').forEach(el => el.classList.remove('btn-active')); this.classList.add('btn-active');">계좌별</button>
                </div>
            </div>
            <div id="tab-content" 
                 hx-get="/stock/htmx/portfolio?viewGroupBy=STOCK" 
                 hx-trigger="load" 
                 hx-swap="innerHTML">
                 <div class="flex justify-center p-10">
                    <span class="loading loading-spinner loading-lg"></span>
                 </div>
            </div>
        </div>
    </div>

    <!-- 2. Advanced Analytics (Multi-dimensional Analysis) -->
    <div hx-get="/stock/htmx/daily-summary/view" hx-trigger="load" hx-swap="outerHTML">
         <div class="flex justify-center p-10">
            <span class="loading loading-spinner loading-lg"></span>
        </div>
    </div>

</div>
