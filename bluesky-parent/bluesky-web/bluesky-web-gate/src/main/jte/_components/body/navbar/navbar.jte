@import net.luversof.web.gate.util.CspNonceHolder
@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param boolean hideHamburger = false

<div class="navbar bg-base-100/90 backdrop-blur sticky top-0 z-50 shadow-sm">
	<div class="navbar-start">
		@if(!hideHamburger)
		<div class="dropdown">
			<div tabindex="0" role="button" class="btn btn-ghost lg:hidden">
				<svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h8m-8 6h16" />
				</svg>
			</div>
			<ul class="menu menu-sm dropdown-content bg-base-100 rounded-box z-1 mt-3 w-52 p-2 shadow">
				@for(var menuItem : net.luversof.web.gate.util.TemplateUtil.getMenuList("main"))
					@if(menuItem instanceof net.luversof.web.common.menu.domain.Menu m)
						@if(m.isDisplay())
							<li>
								<a class="${m.isCurrentMenu() ? "active font-bold" : "hover:bg-base-200"}" href="${m.getUrl()}">
									${m.getName()}
								</a>
							</li>
						@endif
					@endif
				@endfor
			</ul>
		</div>
		@endif
		@template._components.body.navbar.start.title(profile = profile)
	</div>
	<div class="navbar-center hidden lg:flex h-full">
		<ul class="menu menu-horizontal px-1 h-full items-center gap-2">
			@for(var menuItem : net.luversof.web.gate.util.TemplateUtil.getMenuList("main"))
				@if(menuItem instanceof net.luversof.web.common.menu.domain.Menu m)
					@if(m.isDisplay())
						<li class="h-full flex items-center">
							<a class="font-medium px-4 py-2 rounded-none border-b-2 transition-colors duration-300 hover:duration-75 h-full flex items-center ${m.isCurrentMenu() ? "border-primary text-primary font-bold" : "border-transparent hover:border-base-content/30 text-base-content/80 hover:text-base-content hover:bg-transparent"}" href="${m.getUrl()}">
								${m.getName()}
							</a>
						</li>
					@endif
				@endif
			@endfor
		</ul>
	</div>
	<div class="navbar-end gap-1">
		<!-- 금액 숨김 토글 -->
		<label id="hideAmountsLabel" class="label cursor-pointer gap-1.5 px-2 py-1.5 rounded-full hover:bg-base-200 transition-colors" title="금액 숨김/표시">
			<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="w-4 h-4 text-base-content/50" id="hideAmountsIcon">
				<path d="M3.53 2.47a.75.75 0 00-1.06 1.06l18 18a.75.75 0 101.06-1.06l-18-18zM22.676 12.553a11.249 11.249 0 01-2.631 4.31l-3.099-3.099a5.25 5.25 0 00-6.71-6.71L7.759 4.577a11.217 11.217 0 014.242-.827c4.97 0 9.185 3.223 10.675 7.69.12.362.12.752 0 1.113z" />
				<path d="M15.75 12c0 .18-.013.357-.037.53l-4.244-4.243A3.75 3.75 0 0115.75 12zM12.53 15.713l-4.243-4.244a3.75 3.75 0 004.243 4.243z" />
				<path d="M6.75 12c0-.619.107-1.213.304-1.764l-3.1-3.1a11.25 11.25 0 00-2.63 4.31c-.12.362-.12.752 0 1.114 1.489 4.467 5.704 7.69 10.675 7.69 1.5 0 2.933-.294 4.242-.827l-2.477-2.477A5.25 5.25 0 016.75 12z" />
			</svg>
			<span class="text-xs font-medium hidden sm:inline text-base-content/50" id="hideAmountsText">금액 숨김</span>
			<input type="checkbox" class="toggle toggle-xs toggle-primary" id="hideAmountsToggle" />
		</label>
		@template._components.body.navbar.end.profileArea(isAuthenticated = isAuthenticated, username = username, loginUrl = loginUrl)
		@template._components.body.navbar.end.locale()
		@template._components.body.navbar.end.theme()
	</div>
	<script nonce="${CspNonceHolder.getNonce()}">
		document.addEventListener('DOMContentLoaded', function() {
			var toggle = document.getElementById('hideAmountsToggle');
			var label = document.getElementById('hideAmountsLabel');
			var icon = document.getElementById('hideAmountsIcon');
			var text = document.getElementById('hideAmountsText');
			if (!toggle) return;

			function applyToggleStyle(checked) {
				if (checked) {
					label.classList.add('bg-primary/10');
					icon.classList.replace('text-base-content/50', 'text-primary');
					if (text) { text.classList.replace('text-base-content/50', 'text-primary'); }
				} else {
					label.classList.remove('bg-primary/10');
					icon.classList.replace('text-primary', 'text-base-content/50');
					if (text) { text.classList.replace('text-primary', 'text-base-content/50'); }
				}
			}

			toggle.checked = localStorage.getItem('hideAmounts') === 'true';
			applyToggleStyle(toggle.checked);

			toggle.addEventListener('change', function(e) {
				if (e.target.checked) {
					document.documentElement.classList.add('hide-amounts');
					localStorage.setItem('hideAmounts', 'true');
				} else {
					document.documentElement.classList.remove('hide-amounts');
					localStorage.setItem('hideAmounts', 'false');
				}
				applyToggleStyle(e.target.checked);
			});
		});
	</script>
</div>