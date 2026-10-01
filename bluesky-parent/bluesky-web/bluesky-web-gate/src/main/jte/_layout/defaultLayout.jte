@import net.luversof.web.gate.util.CspNonceHolder
@import io.github.luversof.boot.context.support.MessageUtil

@param gg.jte.Content content
@param String title = "Bluesky"
@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param boolean hideHamburger = false
<%-- 금액 숨김 토글을 머리 막대에 낼지(주식 화면만, 사용자 요청 2026-10-01). 토글이 없는 화면에서는 숨김 상태도 걸지 않는다 -
     끌 단추가 없는데 금액만 흐려지면 안 된다. --%>
@param boolean showHideAmounts = false
<%-- 메뉴 전용 고정 테마(10-01, PoE 화면 = 인게임 다크) — 비면 사이트 라이트/다크 전환을 따른다.
     htmlClass 는 <html> 에 붙는 클래스(테마 토큰을 덮어쓰는 범위, 예: poe-theme). --%>
@param String fixedTheme = ""
@param String htmlClass = ""
<%-- 메뉴 전용 추가 글꼴 CSS(10-01, PoE 화면 = 인게임 한글 글꼴 Spoqa Han Sans Neo) — Pretendard 와 같은 비차단 로드(data-font-css). --%>
@param String extraFontCss = ""

<!DOCTYPE html>
<html lang="${org.springframework.context.i18n.LocaleContextHolder.getLocale().toLanguageTag()}" data-theme="${fixedTheme.isBlank() ? "light" : fixedTheme}" class="${htmlClass}">
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
			@if(fixedTheme.isBlank())
			var t = localStorage.getItem('theme'); if (t) document.documentElement.setAttribute('data-theme', t);
			@endif
			@if(showHideAmounts)
			if (localStorage.getItem('hideAmounts') === 'true') document.documentElement.classList.add('hide-amounts');
			@endif
			if (localStorage.getItem('sidebarCollapsed') === 'true') document.documentElement.classList.add('sidebar-collapsed');
		})();
	</script>
	<%-- 본문 폰트. 한글 웹폰트가 없어 윈도우에서는 맑은 고딕으로 떨어졌다(실측 2026-09-10).
	     자체 호스팅은 정적 파일이 3.2MB 늘고, Google Fonts 는 외부 두 곳을 열어야 했다.
	     Pretendard 가 같은 동적 서브셋을 CDN 으로 제공해 그것을 쓴다(사용자 결정) - 정적 0MB, 외부 한 곳.
	     CSS 와 폰트 파일이 모두 cdn.jsdelivr.net 에서 오므로 CSP 의 style-src·font-src 에 그 한 곳만 있다. --%>
	<link rel="preconnect" href="https://cdn.jsdelivr.net" crossorigin />
	<%-- media="print" 로 받아 렌더를 막지 않는다. 다 받으면 아래 nonce 스크립트가 media 를 all 로 바꾼다.
	     실측 2026-09-11(로그인 상태, /stock): 이 링크가 렌더 차단이라 CDN 이 5초 지연되면 본문 글자가
	     5,296ms 동안 보이지 않았다(즉시 실패 291ms · 정상 540ms). 글꼴은 늦게 와도 글자는 먼저 보여야 한다. --%>
	<link rel="stylesheet" media="print" data-font-css crossorigin
		href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/variable/pretendardvariable-dynamic-subset.min.css" />
	@if(!extraFontCss.isBlank())
		<link rel="stylesheet" media="print" data-font-css crossorigin href="${extraFontCss}" />
	@endif
	<link href="/main.css" rel="stylesheet" />
	<script src="/js/vendor/htmx.min.js"></script>
	<script src="/js/vendor/json-enc.js"></script>
	<script src="/js/common.js"></script>
	<%-- stock 전용 스크립트(date-range-picker/selectionStorage/multiSelectInit/tagFilterChips)는
	     stockLayout 으로 이동했다. board/poe/home 에서는 참조가 없어 로드할 필요가 없다. --%>

	<script nonce="${CspNonceHolder.getNonce()}">
		// 본문 폰트 CSS 를 화면용으로 되돌린다(위 link 는 렌더를 막지 않으려고 media="print" 로 받는다).
		// 인라인 on* 속성은 CSP(script-src 'self' nonce) 에 걸리므로 여기서 붙인다.
		(function() {
			// 글꼴 CSS 가 여럿일 수 있다(PoE 화면은 한글 게임 글꼴을 하나 더) - 첫 것만 바꾸면 나머지는 print 에 머문다
			document.querySelectorAll('link[data-font-css]').forEach(function (fontCss) {
				function activate() { fontCss.media = 'all'; }
				if (fontCss.sheet) { activate(); return; }
				fontCss.addEventListener('load', activate);
				fontCss.addEventListener('error', function() { /* CDN 이 막혀도 화면은 기본 글꼴로 그대로 뜬다 */ });
			});
		})();

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
		<%-- 화면의 기준 시간대. 주소에 timeZone 이 있으면 그것이 이기고(서버도 그렇다),
		     없으면 서버 존이 기준이다 - 브라우저가 찍는 날짜·시각도 이 값을 따라야 한다. --%>
		data-time-zone="${java.time.ZoneId.systemDefault().getId()}"
		data-common-button-search="${MessageUtil.getMessage("common.button.search")}"
		data-common-button-reset="${MessageUtil.getMessage("common.button.reset")}"
		data-common-message-load-error="${MessageUtil.getMessage("common.message.load.error")}"
		data-common-message-action-success="${MessageUtil.getMessage("common.message.action.success")}"
		data-common-message-action-error="${MessageUtil.getMessage("common.message.action.error")}"
		data-stock-label-buy="${MessageUtil.getMessage("stock.trade.label.buy")}"
		data-stock-chart-title-yearly-trade-amount="${MessageUtil.getMessage("stock.trade.chart.title.yearly.trade.amount")}"
		data-stock-label-sell="${MessageUtil.getMessage("stock.trade.label.sell")}"
		data-stock-label-realized-profit="${MessageUtil.getMessage("stock.profit.realized")}"
		<%-- 월별 매매 차트의 실현손익은 '매도한 달에만' 찍힌다. 범례가 그것을 말해 주지 않으면
		     표식 없는 달이 "그 달은 0원" 으로 읽힌다. --%>
		data-stock-label-realized-profit-months="${MessageUtil.getMessage("stock.trade.chart.label.realized.profit.months")}"
		data-stock-label-realized-profit-years="${MessageUtil.getMessage("stock.trade.chart.label.realized.profit.years")}"
		data-stock-chart-title-profit-contribution="${MessageUtil.getMessage("stock.trade.chart.title.profit.contribution")}"
		data-stock-chart-title-buy-concentration="${MessageUtil.getMessage("stock.trade.chart.title.buy.concentration")}"
		data-stock-chart-message-no-sell-data="${MessageUtil.getMessage("stock.trade.chart.message.no.sell.data")}"
		data-stock-chart-message-no-buy-data="${MessageUtil.getMessage("stock.trade.chart.message.no.buy.data")}"
		data-stock-chart-message-no-trade-data="${MessageUtil.getMessage("stock.trade.chart.message.no.trade.data")}"></div>

	<%-- 본문 바로가기. 없으면 키보드 사용자가 화면마다 상단 메뉴를 전부 지나야 한다
	     (실측: 본문 첫 요소까지 Tab 15번). 평소엔 숨어 있다가 포커스되면 나타난다. --%>
	<a href="#mainContent"
	   class="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-[100] focus:px-4 focus:py-2 focus:rounded-box focus:bg-primary focus:text-primary-content focus:shadow-lg">${MessageUtil.getMessage("common.label.skip.to.content")}</a>

	@template._components.body.navbar.navbar(isAuthenticated = isAuthenticated, username = username, loginUrl = loginUrl, profile = profile, hideHamburger = hideHamburger, showHideAmounts = showHideAmounts)

	<%-- 본문 랜드마크. 없으면 화면 낭독기에서 "본문으로 건너뛰기"가 동작하지 않는다
	     (실측: 주식 8개 화면 모두 main 랜드마크 0개). --%>
	<%-- 3화면을 넘는 긴 화면은 화면 안에서 구역으로 뛸 방법이 없었다(실측 2026-09-10: 배당 7.2화면·매매 5.4화면에
	     화면 안 이동 링크는 건너뛰기 링크 1개뿐). common.js 가 data-page-section 을 모아 구역 막대를 만든다. --%>
	<main id="mainContent" tabindex="-1" data-section-nav-label="${io.github.luversof.boot.context.support.MessageUtil.getMessage("stock.page.sections")}">
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
		// 401(세션 끊김)은 안내할 것이 아니라 로그인으로 보낼 일이다 - 페이지·조각·JSON 경로와 같은 /login?redirectUrl= 규약.
		// 실측 2026-09-09: 관리 화면의 갱신 버튼(hx-post)이 401 을 받으면 "데이터 로드 중 오류" 토스트만 뜨고 갈 길이 없었다.
		function showLoadError(evt) {
			var xhr = evt && evt.detail && evt.detail.xhr;
			if (xhr && xhr.status === 401) {
				location.assign('/login?redirectUrl=' + encodeURIComponent(location.href));
				return;
			}
			// 조회가 아닌 요청(저장·갱신)의 실패에 "데이터 로드 중 오류" 는 틀린 말이다(실측 2026-09-09: 시세 갱신 500).
			var verb = evt && evt.detail && evt.detail.requestConfig && evt.detail.requestConfig.verb;
			var isAction = !!verb && verb !== 'get';
			showToast(isAction
				? cfgMessage('commonMessageActionError', 'The request failed.')
				: cfgMessage('commonMessageLoadError', 'Failed to load.'), 'error', 5000);
		}
		document.body.addEventListener('htmx:responseError', showLoadError);
		document.body.addEventListener('htmx:sendError', showLoadError);
		document.body.addEventListener('htmx:timeout', showLoadError);

		// 쓰기 요청(GET 외) 성공 토스트. afterRequest 는 outerHTML 스왑 시 body 까지
		// 버블링되지 않을 수 있어, 로딩바와 같은 방식으로 XHR loadend 에 건다.
		document.body.addEventListener('htmx:beforeSend', function(evt) {
			var verb = evt.detail && evt.detail.requestConfig && evt.detail.requestConfig.verb;
			if (!verb || verb === 'get') return;
			// 본문이 커서 POST 로 보내는 **조회**(PoE2 빌드 요약·엔진 재계산 — 코드 13KB 가 URL 한도를 넘는다)는 쓰기가 아니다 → data-no-success-toast 로 뺀다
			var elt = evt.detail.elt;
			if (elt && elt.closest && elt.closest('[data-no-success-toast]')) return;
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