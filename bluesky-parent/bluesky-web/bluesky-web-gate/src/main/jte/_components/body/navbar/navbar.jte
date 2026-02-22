@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"

<div class="navbar bg-base-100 shadow-sm">
	<div class="navbar-start">
		<div class="dropdown">
			<div tabindex="0" role="button" class="btn btn-ghost lg:hidden">
				<svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h8m-8 6h16" />
				</svg>
			</div>
			<ul class="menu menu-sm dropdown-content bg-base-100 rounded-box z-1 mt-3 w-52 p-2 shadow">
				@for(var menuItem : net.luversof.web.gate.util.ThymeleafUtil.getMenuList("main"))
					@if(menuItem instanceof net.luversof.web.common.menu.domain.Menu m)
						@if(m.isDisplay())
							<li>
								<a class="${m.isCurrentMenu() ? "menu-active" : ""}" href="${m.getUrl()}">
									${m.getName()}
								</a>
							</li>
						@endif
					@endif
				@endfor
			</ul>
		</div>
		@template._components.body.navbar.start.title(profile = profile)
	</div>
	<div class="navbar-center hidden lg:flex">
		<ul class="menu menu-horizontal px-1">
			@for(var menuItem : net.luversof.web.gate.util.ThymeleafUtil.getMenuList("main"))
				@if(menuItem instanceof net.luversof.web.common.menu.domain.Menu m)
					@if(m.isDisplay())
						<li>
							<a class="${m.isCurrentMenu() ? "menu-active" : ""}" href="${m.getUrl()}">
								${m.getName()}
							</a>
						</li>
					@endif
				@endif
			@endfor
		</ul>
	</div>
	<div class="navbar-end">
		@template._components.body.navbar.end.profileArea(isAuthenticated = isAuthenticated, username = username, loginUrl = loginUrl)
		@template._components.body.navbar.end.theme()
	</div>
</div>