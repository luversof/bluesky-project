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
		})();
	</script>
	<link href="/main.css" rel="stylesheet" />
	<script src="https://unpkg.com/theme-change"></script>
	<script src="https://unpkg.com/htmx.org"></script>
	<script src="https://unpkg.com/htmx-ext-json-enc/json-enc.js"></script>
	<script src="/js/common.js"></script>
	<script src="/js/date-range-picker.js"></script>
	<script type="module" src="/js/stock/selectionStorage.js"></script>
	<script type="module" src="/js/stock/multiSelectInit.js"></script>

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

</body>
</html>