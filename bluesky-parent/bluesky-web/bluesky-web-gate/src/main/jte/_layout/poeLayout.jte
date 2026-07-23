@import net.luversof.web.gate.util.CspNonceHolder
@import io.github.luversof.boot.context.support.MessageUtil
@param gg.jte.Content contentPoe
@param String title = "Bluesky PoE"
@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"

<%-- PoE 섹션 공용 레이아웃 — stockLayout 과 같은 좌측 서브메뉴 구조 (menu.poe 설정 기반) --%>
@template._layout.defaultLayout(
	title = title,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	hideHamburger = true,
	content = @`
		<div class="flex flex-col lg:flex-row min-h-[calc(100vh-4rem)]">
			<div class="hidden lg:block lg:w-56 shrink-0 stock-sidebar-spacer">
				<div class="fixed top-16 left-0 z-40 h-[calc(100vh-4rem)] w-56 overflow-y-auto border-r border-base-200 bg-base-100 py-4 flex flex-col stock-sidebar">
					@template._components.navmenu(menu = "poe", classAppend = "w-full")
					<div class="mt-auto border-t border-base-200 px-2 pt-2">
						<button type="button" class="sidebar-collapse-btn w-full font-medium rounded-box flex items-center justify-end gap-3 px-4 py-3 text-base-content/60 hover:bg-base-200 hover:text-base-content transition-all duration-300 hover:duration-75"
								data-sidebar-collapse-toggle
								title="${MessageUtil.getMessage("layout.sidebar.toggle")}" aria-label="${MessageUtil.getMessage("layout.sidebar.toggle")}">
							<svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 shrink-0 sidebar-toggle-icon transition-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor">
								<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
							</svg>
						</button>
					</div>
				</div>
			</div>
			<script nonce="${CspNonceHolder.getNonce()}">
				(function() {
					var button = document.querySelector('[data-sidebar-collapse-toggle]');
					if (!button) {
						return;
					}
					button.addEventListener('click', function() {
						document.documentElement.classList.toggle('sidebar-collapsed');
						try {
							localStorage.setItem('sidebarCollapsed', document.documentElement.classList.contains('sidebar-collapsed'));
						} catch (e) {}
					});
				})();
			</script>
			<div class="flex-1 min-w-0 p-4 pb-20 lg:pb-4 relative">
				${contentPoe}
			</div>
		</div>

		<!-- 모바일 하단 탭바 (lg 이상에서 숨김) -->
		<%-- 메뉴 7개가 좁은 화면(375px)에서 안 접혀 "시뮬레이터·데이터 관리"가 화면 밖으로 잘렸다(모바일 감사) —
	     가로 스크롤을 허용하고 항목 수축을 막아 전 항목 접근 가능하게 한다 --%>
		<nav class="dock dock-sm lg:hidden z-50 border-t border-base-200 bg-base-100/95 backdrop-blur overflow-x-auto">
			@for(var menuItem : net.luversof.web.gate.util.TemplateUtil.getMenuList("poe"))
				@if(menuItem instanceof net.luversof.web.common.menu.domain.Menu m)
					@if(m.isDisplay())
						<a href="${m.getUrl()}" class="shrink-0 min-w-16 ${m.isCurrentMenu() ? "dock-active text-primary" : "text-base-content/60"}">
							@template._components.menuIcon(messageCode = m.getMessageCode(), name = m.getName())
							<span class="dock-label text-[10px]">${m.getName()}</span>
						</a>
					@endif
				@endif
			@endfor
		</nav>
	`
)
