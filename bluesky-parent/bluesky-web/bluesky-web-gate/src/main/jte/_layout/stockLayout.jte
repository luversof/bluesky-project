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
		<div class="flex flex-col lg:flex-row min-h-[calc(100vh-4rem)]">
			<div class="hidden lg:block lg:w-56 shrink-0 border-r border-base-200 bg-base-100">
				<div class="sticky top-16 h-[calc(100vh-4rem)] overflow-y-auto py-4">
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

		<script>
		(function(){
			try {
				const raw = typeof sessionStorage !== 'undefined' ? sessionStorage.getItem('globalDateRange') : null;
				if (raw) {
					const obj = JSON.parse(raw);
					function localToIso(ds, addDays) {
						if (!ds) return '';
						const parts = ds.split('-');
						const y = Number(parts[0]);
						const m = Number(parts[1]) - 1;
						const d = Number(parts[2]) + (addDays || 0);
						return new Date(y, m, d, 0, 0, 0, 0).toISOString();
					}
					var gStart = document.getElementById('globalStartInstantInput');
					var gEnd = document.getElementById('globalEndInstantInput');
					var gTz = document.getElementById('globalTimeZoneInput');
					var gMode = document.getElementById('globalRangeModeInput');
					if (gStart) gStart.value = obj.start ? localToIso(obj.start, 0) : '';
					if (gEnd) gEnd.value = obj.end ? localToIso(obj.end, 1) : '';
					if (gTz) gTz.value = obj.timeZone || '';
					if (gMode) gMode.value = obj.mode || '';
				}
			} catch (e) {}
		})();
		</script>

		<!-- stock charts helper (loaded after page content to ensure Chart.js is available when pages include it) -->
		<script src="/js/stock-charts.js"></script>

		<!-- 모바일 하단 탭바 (lg 이상에서 숨김) -->
		<nav class="btm-nav btm-nav-sm lg:hidden z-50 border-t border-base-200 bg-base-100/95 backdrop-blur">
			@for(var menuItem : net.luversof.web.gate.util.TemplateUtil.getMenuList("stock"))
				@if(menuItem instanceof net.luversof.web.common.menu.domain.Menu m)
					@if(m.isDisplay())
						<a href="${m.getUrl()}" class="${m.isCurrentMenu() ? "active text-primary" : "text-base-content/60"}">
							@template._components.menuIcon(name = m.getName())
							<span class="btm-nav-label text-[10px]">${m.getName()}</span>
						</a>
					@endif
				@endif
			@endfor
		</nav>
	`
)