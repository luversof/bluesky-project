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
			<h2 class="text-2xl font-bold mb-4">매매 내역</h2>
			<div style="min-height:200px;" hx-get="/stock/htmx/trade/list" hx-trigger="load" hx-target="this" hx-swap="outerHTML">
				<span class="loading loading-spinner text-primary"></span>
			</div>
		</div>
	`
)