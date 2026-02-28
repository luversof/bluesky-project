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
                @template.stock.htmx.dashboardContent(isAuthenticated = isAuthenticated)
        `
)