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
			@template._components.ui.pageHeader(
				title = MessageUtil.getMessage("stock.page.analytics.title"),
				description = MessageUtil.getMessage("stock.page.analytics.desc"))

			<!-- 계좌별 카드 + 종목별 집계 테이블 (요약·비중 차트는 대시보드에서 제공) -->
			<div hx-get="/stock/htmx/asset-status" hx-trigger="load" hx-swap="innerHTML">
				<div class="flex justify-center p-10">
					<span class="loading loading-spinner text-primary loading-lg"></span>
				</div>
			</div>

		</div>
	`
)