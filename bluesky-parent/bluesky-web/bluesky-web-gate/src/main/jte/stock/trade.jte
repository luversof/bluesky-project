@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"

@template._layout.stockLayout(
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentStock = @`
		<div class="container mx-auto p-4 space-y-6">
			<div class="flex justify-between items-center mb-6">
				<h2 class="text-2xl font-bold">${MessageUtil.getMessage("stock.page.trade.title")}</h2>
			</div>
			<div style="min-height:200px;" hx-get="/stock/htmx/trade/list" hx-trigger="load" hx-target="this" hx-swap="outerHTML" hx-include="#globalDateRangeInputs input">
				<span class="loading loading-spinner text-primary"></span>
			</div>
		</div>
	`
)