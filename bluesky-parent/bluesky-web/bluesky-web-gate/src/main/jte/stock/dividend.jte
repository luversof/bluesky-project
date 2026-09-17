@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
<%-- 다가올 배당(이번 달 남은 날 + 다음 달 예정). 옛 배당 캘린더 탭의 '앞으로' 를 옮겨 왔다 - 2026-09-17 사용자 결정. --%>
@param boolean upcomingHasRows = false
@param net.luversof.web.gate.stock.dto.view.UpcomingDividendScheduleView upcomingSchedule = null
@param java.time.LocalDate upcomingAsOfOldest = null
@param java.time.LocalDate upcomingAsOfNewest = null
@param long upcomingStaleQuantityCount = 0L
@param boolean upcomingCurrentQuantityUnavailable = false
@param BigDecimal upcomingCurrentQuantityTotal = java.math.BigDecimal.ZERO
@param java.util.List<String> shortHistorySymbols = java.util.List.of()

@template._layout.stockLayout(
	pageTitle = MessageUtil.getMessage("stock.page.dividend.title"),
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentStock = @`
		<div class="space-y-6" data-sync-url="/stock/htmx/dividend/list">
			@template._components.ui.pageHeader(title = MessageUtil.getMessage("stock.page.dividend.title"))

			<!-- 월배당 도구: 별도 페이지로 이동(링크). -->
			<div class="flex flex-wrap items-center gap-2 text-sm">
				<span class="text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.tools")}</span>
				<a href="/stock/admin?tab=monthly-reference" class="btn btn-sm btn-outline gap-1">
					${MessageUtil.getMessage("stock.page.dividend.tab.monthly.reference")}
					<svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M14 5l7 7m0 0l-7 7m7-7H3" /></svg>
				</a>
				<a href="/stock/simulator?tab=monthly-dividend" class="btn btn-sm btn-outline gap-1">
					${MessageUtil.getMessage("stock.page.dividend.tab.simulator")}
					<svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M14 5l7 7m0 0l-7 7m7-7H3" /></svg>
				</a>
			</div>

			<%-- 2026-09-17 까지 이 화면은 '실수령 배당' · '배당 캘린더' 두 탭이었다. 지난 달의 캘린더는 실수령 배당을 그 달로 좁힌 것과
			     금액이 겹쳐(탭만의 것은 날짜 격자뿐) 캘린더를 실수령 배당에 통합했다(사용자 결정). 캘린더에만 있던 '앞으로 받을 예정' 은
			     이 구역이 말하고, 기간이 달력 한 달이면 아래 실수령 배당이 그 달 달력을 그린다. 이 구역은 기간 · 필터와 무관하다(미래라서). --%>
			@template.stock.fragments.upcomingDividendSchedule(
				hasRows = upcomingHasRows,
				schedule = upcomingSchedule,
				asOfOldest = upcomingAsOfOldest,
				asOfNewest = upcomingAsOfNewest,
				staleQuantityCount = upcomingStaleQuantityCount,
				currentQuantityUnavailable = upcomingCurrentQuantityUnavailable,
				currentQuantityTotal = upcomingCurrentQuantityTotal,
				shortHistorySymbols = shortHistorySymbols)

			<div class="min-h-[200px]" data-params-from-query="true" hx-get="/stock/htmx/dividend/list" hx-trigger="load" hx-target="this" hx-swap="outerHTML" hx-include="#globalDateRangeInputs input">
				<div class="flex items-center gap-2 text-base-content/60">
					<span class="loading loading-spinner loading-sm text-primary"></span>
					<span>${MessageUtil.getMessage("common.message.loading")}</span>
				</div>
			</div>
		</div>
	`
)
