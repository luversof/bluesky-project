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

			<h1 class="text-2xl font-bold mb-4 ml-1">실현 손익 현황</h1>

			<!-- Chart.js: fragment 내 차트 사용 -->
			<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>

			<div hx-get="/stock/htmx/realized-profit" hx-trigger="load" hx-swap="outerHTML">
				<div class="flex justify-center p-10">
					<span class="loading loading-spinner text-primary loading-lg"></span>
				</div>
			</div>

		</div>
	`
)
