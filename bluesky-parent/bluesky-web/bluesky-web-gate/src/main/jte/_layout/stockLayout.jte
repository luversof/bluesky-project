@param gg.jte.Content contentStock
@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"

@template._layout.defaultLayout(
	title = "Bluesky Stock",
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	hideHamburger = true,
	content = @`
		<!-- 차트 라이브러리/헬퍼: 페이지·프래그먼트 스크립트보다 먼저 1회만 로드 -->
		<script src="/js/vendor/chart.umd.min.js"></script>
		<script src="/js/stock-charts.js"></script>

		<div class="flex flex-col lg:flex-row min-h-[calc(100vh-4rem)]">
			<div class="hidden lg:block lg:w-56 shrink-0">
				<div class="fixed top-16 left-0 z-40 h-[calc(100vh-4rem)] w-56 overflow-y-auto border-r border-base-200 bg-base-100 py-4">
					@template._components.navmenu(menu = "stock", classAppend = "w-full")
				</div>
			</div>
			<div class="flex-1 min-w-0 p-4 pb-20 lg:pb-4 relative">
				${contentStock}
			</div>
		</div>

		<!-- Global date-range inputs (used by HTMX initial fragment loads) -->
		<div id="globalDateRangeInputs" style="display:none">
			<input type="hidden" id="globalStartInstantInput" name="startDate" value="">
			<input type="hidden" id="globalEndInstantInput" name="endDate" value="">
			<input type="hidden" id="globalTimeZoneInput" name="timeZone" value="">
			<input type="hidden" id="globalRangeModeInput" name="rangeMode" value="">
		</div>

		<script src="/js/stock/globalDateRange.js"></script>
		<script src="/js/stock/detailFilterState.js"></script>
		<script src="/js/stock/tableSort.js"></script>

		<!-- 모바일 하단 탭바 (lg 이상에서 숨김) -->
		<nav class="dock dock-sm lg:hidden z-50 border-t border-base-200 bg-base-100/95 backdrop-blur">
			@for(var menuItem : net.luversof.web.gate.util.TemplateUtil.getMenuList("stock"))
				@if(menuItem instanceof net.luversof.web.common.menu.domain.Menu m)
					@if(m.isDisplay())
						<a href="${m.getUrl()}" class="${m.isCurrentMenu() ? "dock-active text-primary" : "text-base-content/60"}">
							@template._components.menuIcon(messageCode = m.getMessageCode(), name = m.getName())
							<span class="dock-label text-[10px]">${m.getName()}</span>
						</a>
					@endif
				@endif
			@endfor
		</nav>
	`
)
