@import io.github.luversof.boot.context.support.MessageUtil
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
			<div class="hidden lg:block lg:w-56 shrink-0 stock-sidebar-spacer">
				<div class="fixed top-16 left-0 z-40 h-[calc(100vh-4rem)] w-56 overflow-y-auto border-r border-base-200 bg-base-100 py-4 flex flex-col stock-sidebar">
					@template._components.navmenu(menu = "stock", classAppend = "w-full")
					<div class="mt-auto border-t border-base-200 px-2 pt-2">
						<button type="button" class="sidebar-collapse-btn w-full font-medium rounded-box flex items-center justify-end gap-3 px-4 py-3 text-base-content/60 hover:bg-base-200 hover:text-base-content transition-all duration-300 hover:duration-75"
								onclick="document.documentElement.classList.toggle('sidebar-collapsed');try{localStorage.setItem('sidebarCollapsed',document.documentElement.classList.contains('sidebar-collapsed'));}catch(e){}"
								title="${MessageUtil.getMessage("layout.sidebar.toggle")}" aria-label="${MessageUtil.getMessage("layout.sidebar.toggle")}">
							<svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 shrink-0 sidebar-toggle-icon transition-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor">
								<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
							</svg>
						</button>
					</div>
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
		<script src="/js/stock/multiSelectDropdown.js"></script>

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
		<script>
			// dock 이 좌우 스크롤이라 현재 메뉴가 화면 밖일 수 있음 — 로드 시 가운데로 보이게
			(function() {
				var active = document.querySelector('.dock .dock-active');
				if (active && active.scrollIntoView) active.scrollIntoView({ inline: 'center', block: 'nearest' });
			})();
		</script>
	`
)
