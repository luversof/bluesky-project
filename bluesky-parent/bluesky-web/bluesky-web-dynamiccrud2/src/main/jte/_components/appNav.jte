<%-- 앱 자체 이동(전체 메뉴 · 관리). 사용자가 만든 프로젝트 · 메뉴 이름(왼쪽)과 섞이지 않게 상단 오른쪽에 묶어 두고,
     지금 구역은 눌린 모양 + aria-current 로 보인다 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t

@param String current = null
@param boolean showAdmin = true

<nav class="app-nav" aria-label="${t("layout.nav.label")}">
	<a href="/" class="app-nav-link" aria-current="${"menu".equals(current) ? "page" : null}">${t("layout.nav.menus")}</a>
	@if(showAdmin)
		<a href="/admin" class="app-nav-link" aria-current="${"admin".equals(current) ? "page" : null}">${t("layout.nav.admin")}</a>
	@endif
</nav>
