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
		<div class="space-y-6">
			@template._components.ui.pageHeader(title = MessageUtil.getMessage("stock.page.activity.title"))

			<div hx-get="/stock/htmx/activity-list" hx-trigger="load" hx-swap="outerHTML" hx-include="#globalDateRangeInputs input">
				<div class="flex justify-center p-10">
					<span class="loading loading-spinner loading-lg text-primary"></span>
				</div>
			</div>
		</div>
	`
)
