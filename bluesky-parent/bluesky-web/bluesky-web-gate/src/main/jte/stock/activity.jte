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
		<div class="space-y-6" data-sync-url="/stock/htmx/activity-list">
			@template._components.ui.pageHeader(title = MessageUtil.getMessage("stock.page.activity.title"))

			<div data-params-from-query="true" hx-get="/stock/htmx/activity-list" hx-trigger="load" hx-swap="outerHTML" hx-include="#globalDateRangeInputs input">
				<div class="flex justify-center p-10">
					<span class="loading loading-spinner loading-lg text-primary"></span>
				</div>
			</div>
		</div>
	`
)
