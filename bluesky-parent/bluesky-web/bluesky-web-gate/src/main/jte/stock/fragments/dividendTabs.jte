@import io.github.luversof.boot.context.support.MessageUtil
<%-- 배당 화면 탭(2026-11-28): 배당 내역 조회 | 종소세 신고. 종소세 신고는 귀속연도 단위라 기간 · 필터와 무관해 따로 둔다(사용자 요청). --%>
@param String current = "history"
<nav aria-label="${MessageUtil.getMessage("stock.page.dividend.title")}">
	<div class="tabs tabs-box w-full bg-base-200/70 p-1">
		<a href="/stock/dividend" aria-current="${"history".equals(current) ? "page" : null}" class="${"history".equals(current) ? "tab tab-lg flex-1 font-semibold tab-active !bg-primary !text-primary-content shadow-sm" : "tab tab-lg flex-1 text-base-content/60 hover:text-base-content"}">${MessageUtil.getMessage("stock.page.dividend.tab.history")}</a>
		<a href="/stock/dividend/tax" aria-current="${"tax".equals(current) ? "page" : null}" class="${"tax".equals(current) ? "tab tab-lg flex-1 font-semibold tab-active !bg-primary !text-primary-content shadow-sm" : "tab tab-lg flex-1 text-base-content/60 hover:text-base-content"}">${MessageUtil.getMessage("stock.page.dividend.tab.tax")}</a>
	</div>
</nav>
