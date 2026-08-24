@import net.luversof.web.gate.util.CspNonceHolder
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
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<%-- htmx 가 스왑한 fragment 의 인라인 스크립트에 현재 페이지의 CSP nonce 를 재부여한다 (fragment 는 별도 요청이라 nonce 가 다름).
	     [의도된 트레이드오프] 이 설정은 스왑된 콘텐츠 안의 "모든" <script> 에 nonce 를 부여하므로,
	     htmx 스왑 경로로 주입된 마크업에 대해서는 nonce CSP 의 XSS 방어가 약해진다.
	     fragment 는 전부 우리 서버가 렌더링하고 사용자 입력은 JTE 가 이스케이프한다는 전제 위의 결정이며,
	     $unsafe{} 로 출력하는 값에 외부 입력이 섞이지 않도록 유지하는 것이 전제 조건이다. --%>
	<meta name="htmx-config" content='{"inlineScriptNonce":"${CspNonceHolder.getNonce()}"}'>
	<title>${title}</title>
	<script nonce="${CspNonceHolder.getNonce()}">
		(function() {
			var t = localStorage.getItem('theme'); if (t) document.documentElement.setAttribute('data-theme', t);
			if (localStorage.getItem('hideAmounts') === 'true') document.documentElement.classList.add('hide-amounts');
			if (localStorage.getItem('sidebarCollapsed') === 'true') document.documentElement.classList.add('sidebar-collapsed');
		})();
	</script>
	<link href="/main.css" rel="stylesheet" />
	<script src="/js/vendor/htmx.min.js"></script>
	<script src="/js/vendor/json-enc.js"></script>
	<script src="/js/common.js"></script>
	<%-- stock 전용 스크립트(date-range-picker/selectionStorage/multiSelectInit/tagFilterChips)는
	     stockLayout 으로 이동했다. board/poe/home 에서는 참조가 없어 로드할 필요가 없다. --%>

	<script nonce="${CspNonceHolder.getNonce()}">
		// 차트가 필요한 시점에만 Chart.js + stock-charts 헬퍼를 순서대로 로드한다.
		// (차트 없는 화면에서 228KB 를 받지 않기 위함. 동시 호출은 큐로 모아 1회만 로드.)
		window.ensureStockCharts = (function() {
			var loading = false;
			var queue = [];
			function loadScript(src, done) {
				var s = document.createElement('script');
				s.src = src;
				s.onload = function() { done(); };
				s.onerror = function() { console.warn('Failed to load ' + src); done(); };
				document.head.appendChild(s);
			}
			function flush() {
				loading = false;
				var cbs = queue;
				queue = [];
				cbs.forEach(function(cb) { try { cb(); } catch (e) { console.error(e); } });
			}
			return function(cb) {
				if (typeof cb !== 'function') cb = function() {};
				if (window.Chart && window.StockCharts) { cb(); return; }
				queue.push(cb);
				if (loading) return;
				loading = true;
				var afterChart = function() {
					if (window.StockCharts) { flush(); return; }
					loadScript('/js/stock-charts.js', flush);
				};
				if (window.Chart) { afterChart(); }
				else { loadScript('/js/vendor/chart.umd.min.js', afterChart); }
			};
		})();
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
		data-common-message-action-success="${MessageUtil.getMessage("common.message.action.success")}"
		data-stock-label-buy="${MessageUtil.getMessage("stock.trade.label.buy")}"
		data-stock-label-sell="${MessageUtil.getMessage("stock.trade.label.sell")}"
		data-stock-chart-title-profit-contribution="${MessageUtil.getMessage("stock.trade.chart.title.profit.contribution")}"
		data-stock-chart-title-buy-concentration="${MessageUtil.getMessage("stock.trade.chart.title.buy.concentration")}"
		data-stock-chart-message-no-sell-data="${MessageUtil.getMessage("stock.trade.chart.message.no.sell.data")}"
		data-stock-chart-message-no-buy-data="${MessageUtil.getMessage("stock.trade.chart.message.no.buy.data")}"></div>

	<%-- 본문 바로가기. 없으면 키보드 사용자가 화면마다 상단 메뉴를 전부 지나야 한다
	     (실측: 본문 첫 요소까지 Tab 15번). 평소엔 숨어 있다가 포커스되면 나타난다. --%>
	<a href="#mainContent"
	   class="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-[100] focus:px-4 focus:py-2 focus:rounded-box focus:bg-primary focus:text-primary-content focus:shadow-lg">${MessageUtil.getMessage("common.label.skip.to.content")}</a>

	@template._components.body.navbar.navbar(isAuthenticated = isAuthenticated, username = username, loginUrl = loginUrl, profile = profile, hideHamburger = hideHamburger)

	<%-- 본문 랜드마크. 없으면 화면 낭독기에서 "본문으로 건너뛰기"가 동작하지 않는다
	     (실측: 주식 8개 화면 모두 main 랜드마크 0개). --%>
	<main id="mainContent" tabindex="-1">
		${content}
	</main>
	
	@template._components.body.footer()

	<!-- 전역 상단 로딩바: htmx 요청 진행 중 표시 -->
	<div id="app-progress" aria-hidden="true"></div>

	<!-- 전역 토스트: htmx 요청 실패 알림 + 쓰기 요청(GET 외) 성공 알림 -->
	<div id="app-toast" class="toast toast-top toast-end z-[100]"></div>
	<script nonce="${CspNonceHolder.getNonce()}">
	(function() {
		// 전역 로딩바: 동시 요청 카운트로 표시/숨김.
		// 감소는 XHR의 loadend(항상 발생)에 건다 — afterRequest는 outerHTML 스왑으로
		// 트리거 요소가 제거되면 body까지 버블링되지 않아 카운터가 안 내려간다.
		var bar = document.getElementById('app-progress');
		if (bar) {
			var active = 0;
			var dec = function() {
				active = Math.max(0, active - 1);
				if (active === 0) bar.classList.remove('active');
			};
			document.body.addEventListener('htmx:beforeSend', function(evt) {
				active++;
				bar.classList.add('active');
				var xhr = evt.detail && evt.detail.xhr;
				if (xhr && xhr.addEventListener) {
					xhr.addEventListener('loadend', dec, { once: true });
				} else {
					setTimeout(dec, 3000); // 안전장치: xhr 없으면 강제 해제
				}
			});
		}
	})();
	(function() {
		function showToast(msg, type, ttl) {
			var toast = document.getElementById('app-toast');
			if (!toast) return;
			var el = document.createElement('div');
			el.className = 'alert ' + (type === 'success' ? 'alert-success' : 'alert-error') + ' shadow-lg cursor-pointer max-w-sm';
			el.setAttribute('role', type === 'success' ? 'status' : 'alert');
			var span = document.createElement('span');
			span.className = 'text-sm';
			span.textContent = msg;
			el.appendChild(span);
			el.addEventListener('click', function() { el.remove(); });
			toast.appendChild(el);
			setTimeout(function() { if (el.parentNode) el.remove(); }, ttl);
		}
		function cfgMessage(key, fallback) {
			var cfg = document.getElementById('app-config');
			return (cfg && cfg.dataset[key]) || fallback;
		}
		function showLoadError() {
			showToast(cfgMessage('commonMessageLoadError', 'Failed to load.'), 'error', 5000);
		}
		document.body.addEventListener('htmx:responseError', showLoadError);
		document.body.addEventListener('htmx:sendError', showLoadError);
		document.body.addEventListener('htmx:timeout', showLoadError);

		// 쓰기 요청(GET 외) 성공 토스트. afterRequest 는 outerHTML 스왑 시 body 까지
		// 버블링되지 않을 수 있어, 로딩바와 같은 방식으로 XHR loadend 에 건다.
		document.body.addEventListener('htmx:beforeSend', function(evt) {
			var verb = evt.detail && evt.detail.requestConfig && evt.detail.requestConfig.verb;
			if (!verb || verb === 'get') return;
			var xhr = evt.detail.xhr;
			if (!xhr || !xhr.addEventListener) return;
			xhr.addEventListener('loadend', function() {
				if (xhr.status >= 200 && xhr.status < 300) {
					showToast(cfgMessage('commonMessageActionSuccess', 'Done.'), 'success', 2500);
				}
			}, { once: true });
		});
	})();
	</script>

</body>
</html>