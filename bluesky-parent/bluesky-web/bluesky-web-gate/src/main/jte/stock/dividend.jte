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
		<div class="container mx-auto px-4 py-8">
			<h2 class="text-2xl font-bold mb-4">${MessageUtil.getMessage("stock.page.dividend.title")}</h2>
			<div style="min-height:200px;" hx-get="/stock/htmx/dividend/list" hx-trigger="load" hx-target="this" hx-swap="outerHTML" hx-include="#globalDateRangeInputs input">
				<div class="flex items-center gap-2 text-base-content/60">
					<span class="loading loading-spinner loading-sm"></span>
					<span>${MessageUtil.getMessage("common.message.loading")}</span>
				</div>
			</div>
		</div>
	`
)