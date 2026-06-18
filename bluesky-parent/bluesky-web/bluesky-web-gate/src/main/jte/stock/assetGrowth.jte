@import io.github.luversof.boot.context.support.MessageUtil

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
            <h1 class="text-2xl font-bold mb-4 ml-1">${MessageUtil.getMessage("stock.page.asset.growth.title")}</h1>
            <!-- Load Chart.js and plugins synchronously before the fragment is loaded -->
            <script src="/js/vendor/chart.umd.min.js"></script>
            <script src="/js/vendor/hammer.min.js"></script>
            <script src="/js/vendor/chartjs-plugin-zoom.min.js"></script>
            <div class="mb-8">
                <div hx-get="/stock/htmx/asset-growth/view" hx-trigger="load" hx-swap="outerHTML" hx-include="#globalDateRangeInputs input">
                    <div class="flex justify-center p-10">
                        <span class="loading loading-spinner text-primary loading-lg"></span>
                    </div>
                </div>
            </div>
        </div>
    `
)
