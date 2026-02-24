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
	content = @`
		<script>
			// Prevent flash of unblurred amounts
			if (localStorage.getItem('hideAmounts') === 'true') {
				document.documentElement.classList.add('hide-amounts');
			}
		</script>
		<div class="flex flex-col lg:flex-row">
			<div class="grid bg-base-100">
				@template._components.navmenu(menu = "stock", classAppend = "bg-base-100 w-56")
			</div>
			<div class="grid grow mx-4">
				<div class="flex justify-end mb-4">
					<div class="form-control">
						<label class="label cursor-pointer gap-2 bg-base-200/50 hover:bg-base-200 px-4 py-2 rounded-full transition-colors border border-base-300 shadow-sm">
							<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="w-4 h-4 text-base-content/70">
								<path d="M3.53 2.47a.75.75 0 00-1.06 1.06l18 18a.75.75 0 101.06-1.06l-18-18zM22.676 12.553a11.249 11.249 0 01-2.631 4.31l-3.099-3.099a5.25 5.25 0 00-6.71-6.71L7.759 4.577a11.217 11.217 0 014.242-.827c4.97 0 9.185 3.223 10.675 7.69.12.362.12.752 0 1.113z" />
								<path d="M15.75 12c0 .18-.013.357-.037.53l-4.244-4.243A3.75 3.75 0 0115.75 12zM12.53 15.713l-4.243-4.244a3.75 3.75 0 004.243 4.243z" />
								<path d="M6.75 12c0-.619.107-1.213.304-1.764l-3.1-3.1a11.25 11.25 0 00-2.63 4.31c-.12.362-.12.752 0 1.114 1.489 4.467 5.704 7.69 10.675 7.69 1.5 0 2.933-.294 4.242-.827l-2.477-2.477A5.25 5.25 0 016.75 12z" />
							</svg>
							<span class="label-text font-medium text-sm mr-1">금액 숨김</span>
							<input type="checkbox" class="toggle toggle-sm toggle-primary" id="hideAmountsToggle" />
						</label>
					</div>
				</div>
				${contentStock}
			</div>
		</div>
		<script>
			document.addEventListener('DOMContentLoaded', () => {
				const toggle = document.getElementById('hideAmountsToggle');
				if (!toggle) return;
				
				const isHidden = localStorage.getItem('hideAmounts') === 'true';
				if (isHidden) {
					document.documentElement.classList.add('hide-amounts');
					toggle.checked = true;
				}
				
				toggle.addEventListener('change', (e) => {
					if (e.target.checked) {
						document.documentElement.classList.add('hide-amounts');
						localStorage.setItem('hideAmounts', 'true');
					} else {
						document.documentElement.classList.remove('hide-amounts');
						localStorage.setItem('hideAmounts', 'false');
					}
				});
			});
		</script>
	`
)