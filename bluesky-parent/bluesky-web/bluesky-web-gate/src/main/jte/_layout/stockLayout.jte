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

		<script>
		(function(){
			try {
				function localToIso(ds, addDays) {
					if (!ds) return '';
					const parts = ds.split('-');
					const y = Number(parts[0]);
					const m = Number(parts[1]) - 1;
					const d = Number(parts[2]) + (addDays || 0);
					const dt = new Date(y, m, d, 0, 0, 0, 0);
					return isNaN(dt.getTime()) ? '' : dt.toISOString();
				}

				function applyGlobalDate(obj) {
					try {
						var gStart = document.getElementById('globalStartInstantInput');
						var gEnd = document.getElementById('globalEndInstantInput');
						var gTz = document.getElementById('globalTimeZoneInput');
						var gMode = document.getElementById('globalRangeModeInput');
						if (gStart) gStart.value = obj.start ? localToIso(obj.start, 0) : '';
						if (gEnd) gEnd.value = obj.end ? localToIso(obj.end, 1) : '';
						if (gTz) gTz.value = obj.timeZone || '';
						if (gMode) gMode.value = obj.mode || '';

						// update per-form inputs so HTMX includes the new range when forms submit
						document.querySelectorAll('input[name="startDate"]').forEach(function(inp){ inp.value = obj.start ? localToIso(obj.start, 0) : ''; });
						document.querySelectorAll('input[name="endDate"]').forEach(function(inp){ inp.value = obj.end ? localToIso(obj.end, 1) : ''; });
						document.querySelectorAll('input[name="timeZone"]').forEach(function(inp){ inp.value = obj.timeZone || ''; });
						document.querySelectorAll('input[name="rangeMode"]').forEach(function(inp){ inp.value = obj.mode || ''; });
					} catch (e) {}
				}

				// Listen for global date changes and propagate to forms/fragments
				window.addEventListener('globalDateRange:changed', function(ev) {
					try {
						var detail = ev && ev.detail ? ev.detail : null;
						var obj = null;
						if (detail && (detail.start !== undefined || detail.end !== undefined)) {
							obj = { start: detail.start || '', end: detail.end || '', mode: detail.mode || '', timeZone: detail.timeZone || detail.timezone || '' };
						} else {
							var raw = typeof sessionStorage !== 'undefined' ? sessionStorage.getItem('globalDateRange') : null;
							if (raw) {
								try { obj = JSON.parse(raw); } catch(e) { obj = null; }
							}
						}
						if (!obj) return;
						applyGlobalDate(obj);

						// Refresh trade-history once for this global range (avoid duplicates)
						try {
							var tkey = (obj.start || '') + '::' + (obj.end || '');
							if (!window.lastGlobalTradeKey || window.lastGlobalTradeKey !== tkey) {
								try { window.lastGlobalTradeKey = tkey; } catch(e){}
								if (typeof htmx !== 'undefined' && htmx && typeof htmx.ajax === 'function') {
									var url = '/stock/htmx/trade-history';
									if (obj.start && obj.end) url += '?from=' + obj.start + '&to=' + obj.end;
									htmx.ajax('GET', url, { target: '#trade-history-panel', swap: 'outerHTML' });
								}
							}
						} catch(e) {}

						// Trigger common HTMX-driven search forms to reload (if present)
						try {
							var formsToTrigger = ['tradeSearchForm', 'realizedProfitSearchForm'];
							formsToTrigger.forEach(function(id){
								try {
									var f = document.getElementById(id);
									if (!f) return;
									var hasHx = f.hasAttribute('hx-get') || f.hasAttribute('hx-post') || f.querySelector('[hx-get], [hx-post], [hx-put], [hx-delete]');
									if (hasHx) {
										if (typeof f.requestSubmit === 'function') {
											f.requestSubmit();
										} else {
										f.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
										}
									}
								} catch(e){}
							});
						} catch(e){}
					} catch(e) {}
				}, false);
			} catch(e) {}
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
							@template._components.menuIcon(messageCode = m.getMessageCode(), name = m.getName())
							<span class="btm-nav-label text-[10px]">${m.getName()}</span>
						</a>
					@endif
				@endif
			@endfor
		</nav>
	`
)