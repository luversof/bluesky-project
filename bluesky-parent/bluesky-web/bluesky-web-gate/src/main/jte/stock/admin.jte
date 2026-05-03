@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"

@template._layout.stockLayout(
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentStock = @`
		<div class="container mx-auto p-4 space-y-6">
			<div class="flex justify-between items-center mb-6">
				<h1 class="text-2xl font-bold">${MessageUtil.getMessage("stock.page.admin.title")}</h1>
			</div>
			@template.stock.htmx.fragments.adminActions(isAuthenticated = isAuthenticated)
		</div>
	`
)
