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
		<div class="flex flex-col lg:flex-row">
			<div class="grid bg-base-100">
				@template._components.navmenu(menu = "stock", classAppend = "bg-base-100 w-56")
			</div>
			<div class="grid grow mx-4">
				${contentStock}
			</div>
		</div>
	`
)