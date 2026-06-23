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
            @template._components.ui.pageHeader(title = MessageUtil.getMessage("stock.page.asset.growth.title"))
            <!-- Chart.js 는 stockLayout 에서 1회 로드됨. -->
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
