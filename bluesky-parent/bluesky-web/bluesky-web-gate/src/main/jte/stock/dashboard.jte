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
                <%-- 다른 주식 화면과 달리 보이는 제목이 없어 h1 이 하나도 없었다(실측).
                     화면에는 영향 없이 낭독기에서 화면 이름을 알 수 있도록 숨김 제목을 둔다. --%>
                <h1 class="sr-only">${MessageUtil.getMessage("layout.menu.stock.dashboard")}</h1>
                @template.stock.htmx.dashboardContent(isAuthenticated = isAuthenticated)
        `
)