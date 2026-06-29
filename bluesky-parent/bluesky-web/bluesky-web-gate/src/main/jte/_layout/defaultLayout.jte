@import io.github.luversof.boot.context.support.MessageUtil

@param gg.jte.Content content
@param String title = "Bluesky"
@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param boolean hideHamburger = false

<!DOCTYPE html>
<html lang="${org.springframework.context.i18n.LocaleContextHolder.getLocale().toLanguageTag()}" data-theme="light">
<head>
	<title>${title}</title>
	<script>
		(function() {
			var t = localStorage.getItem('theme'); if (t) document.documentElement.setAttribute('data-theme', t);
			if (localStorage.getItem('hideAmounts') === 'true') document.documentElement.classList.add('hide-amounts');
			if (localStorage.getItem('sidebarCollapsed') === 'true') document.documentElement.classList.add('sidebar-collapsed');
		})();
	</script>
	<link href="/main.css" rel="stylesheet" />
	<script src="/js/vendor/theme-change.js"></script>
	<script src="/js/vendor/htmx.min.js"></script>
	<script src="/js/vendor/json-enc.js"></script>
	<script src="/js/common.js"></script>
	<script src="/js/date-range-picker.js"></script>
	<script type="module" src="/js/stock/selectionStorage.js"></script>
	<script type="module" src="/js/stock/multiSelectInit.js"></script>
	<script type="module" src="/js/stock/tagFilterChips.js"></script>

	<script>
		// Ensure stock-charts helper is available; loads /js/stock-charts.js on demand
		window.ensureStockCharts = function(cb) {
			if (window.StockCharts) {
				if (typeof cb === 'function') cb();
				return;
			}
			var s = document.createElement('script');
			s.src = '/js/stock-charts.js';
			s.onload = function() { if (typeof cb === 'function') cb(); };
			s.onerror = function() { console.warn('Failed to load /js/stock-charts.js'); if (typeof cb === 'function') cb(); };
			document.head.appendChild(s);
		};
	</script>
</head>

<body>
	<div
		id="app-config"
		data-login-url="/login"
		data-locale="${org.springframework.context.i18n.LocaleContextHolder.getLocale().toLanguageTag()}"
		data-common-button-search="${MessageUtil.getMessage("common.button.search")}"
		data-common-button-reset="${MessageUtil.getMessage("common.button.reset")}"
		data-common-message-load-error="${MessageUtil.getMessage("common.message.load.error")}"
		data-stock-label-buy="${MessageUtil.getMessage("stock.trade.label.buy")}"
		data-stock-label-sell="${MessageUtil.getMessage("stock.trade.label.sell")}"
		data-stock-chart-title-profit-contribution="${MessageUtil.getMessage("stock.trade.chart.title.profit.contribution")}"
		data-stock-chart-title-buy-concentration="${MessageUtil.getMessage("stock.trade.chart.title.buy.concentration")}"
		data-stock-chart-message-no-sell-data="${MessageUtil.getMessage("stock.trade.chart.message.no.sell.data")}"
		data-stock-chart-message-no-buy-data="${MessageUtil.getMessage("stock.trade.chart.message.no.buy.data")}"></div>

	@template._components.body.navbar.navbar(isAuthenticated = isAuthenticated, username = username, loginUrl = loginUrl, profile = profile, hideHamburger = hideHamburger)

	${content}
	
	@template._components.body.footer()

	<!-- 전역 상단 로딩바: htmx 요청 진행 중 표시 -->
	<div id="app-progress" aria-hidden="true"></div>

	<!-- 전역 토스트: htmx 요청 실패(서버 오류/네트워크/타임아웃) 알림 -->
	<div id="app-toast" class="toast toast-top toast-end z-[100]"></div>
	<script>
	(function() {
		// 전역 로딩바: 동시 요청 카운트로 표시/숨김
		var bar = document.getElementById('app-progress');
		if (bar) {
			var active = 0;
			document.body.addEventListener('htmx:beforeRequest', function() {
				active++;
				bar.classList.add('active');
			});
			document.body.addEventListener('htmx:afterRequest', function() {
				active = Math.max(0, active - 1);
				if (active === 0) bar.classList.remove('active');
			});
		}
	})();
	(function() {
		function showLoadError() {
			var toast = document.getElementById('app-toast');
			if (!toast) return;
			var cfg = document.getElementById('app-config');
			var msg = (cfg && cfg.dataset.commonMessageLoadError) || 'Failed to load.';
			var el = document.createElement('div');
			el.className = 'alert alert-error shadow-lg cursor-pointer max-w-sm';
			el.setAttribute('role', 'alert');
			var span = document.createElement('span');
			span.className = 'text-sm';
			span.textContent = msg;
			el.appendChild(span);
			el.addEventListener('click', function() { el.remove(); });
			toast.appendChild(el);
			setTimeout(function() { if (el.parentNode) el.remove(); }, 5000);
		}
		document.body.addEventListener('htmx:responseError', showLoadError);
		document.body.addEventListener('htmx:sendError', showLoadError);
		document.body.addEventListener('htmx:timeout', showLoadError);
	})();
	</script>

</body>
</html>