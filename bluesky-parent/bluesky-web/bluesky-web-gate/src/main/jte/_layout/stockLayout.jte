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