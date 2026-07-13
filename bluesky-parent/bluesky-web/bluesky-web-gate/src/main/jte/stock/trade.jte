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
		<div class="space-y-6" data-sync-url="/stock/htmx/trade/list">
			@template._components.ui.pageHeader(title = MessageUtil.getMessage("stock.page.trade.title"))

			<div style="min-height:200px;" data-params-from-query="true" hx-get="/stock/htmx/trade/list" hx-trigger="load" hx-target="this" hx-swap="outerHTML" hx-include="#globalDateRangeInputs input">
				<span class="loading loading-spinner text-primary"></span>
			</div>
		</div>
	`
)