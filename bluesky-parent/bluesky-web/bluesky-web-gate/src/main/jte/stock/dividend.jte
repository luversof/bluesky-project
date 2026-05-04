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
				<h2 class="text-2xl font-bold">${MessageUtil.getMessage("stock.page.dividend.title")}</h2>
			</div>
			<div style="min-height:200px;" hx-get="/stock/htmx/dividend/list" hx-trigger="load" hx-target="this" hx-swap="outerHTML" hx-include="#globalDateRangeInputs input">
				<div class="flex items-center gap-2 text-base-content/60">
					<span class="loading loading-spinner loading-sm"></span>
					<span>${MessageUtil.getMessage("common.message.loading")}</span>
				</div>
			</div>
		</div>
	`
)