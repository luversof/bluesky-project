@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = ""
@param String profile = ""

@template._layout.stockLayout(
    isAuthenticated = isAuthenticated,
    username = username,
    loginUrl = loginUrl,
    profile = profile,
    contentStock = @`
        <div class="container mx-auto p-4 space-y-6">
            <h1 class="text-2xl font-bold mb-4 ml-1">자산 성장</h1>
            <div class="mb-8">
                <div hx-get="/stock/htmx/asset-growth/view" hx-trigger="load" hx-swap="outerHTML">
                    <div class="flex justify-center p-10">
                        <span class="loading loading-spinner text-primary loading-lg"></span>
                    </div>
                </div>
            </div>
            <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
        </div>
    `
)
