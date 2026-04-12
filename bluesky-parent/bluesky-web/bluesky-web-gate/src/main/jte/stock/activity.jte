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
				<h1 class="text-2xl font-bold">활동 내역</h1>
			</div>

			<div hx-get="/stock/htmx/activity-list" hx-trigger="load" hx-swap="outerHTML">
				<div class="flex justify-center p-10">
					<span class="loading loading-spinner loading-lg"></span>
				</div>
			</div>
		</div>
	`
)
