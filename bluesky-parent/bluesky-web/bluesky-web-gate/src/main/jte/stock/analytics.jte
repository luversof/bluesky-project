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
			<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>

			<h1 class="text-2xl font-bold mb-4 ml-1">자산 현황</h1>

			<!-- Section 1: 요약 카드 + 비중 차트 -->
			<div hx-get="/stock/htmx/summary" hx-trigger="load" hx-swap="innerHTML">
				<div class="flex justify-center p-10">
					<span class="loading loading-spinner text-primary loading-lg"></span>
				</div>
			</div>

			<!-- Section 2: 계좌별 카드 + 종목별 집계 테이블 -->
			<div hx-get="/stock/htmx/asset-status" hx-trigger="load" hx-swap="innerHTML">
				<div class="flex justify-center p-10">
					<span class="loading loading-spinner text-primary loading-lg"></span>
				</div>
			</div>

		</div>
	`
)