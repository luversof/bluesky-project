@import io.github.luversof.boot.context.support.MessageUtil
<%-- 배당 화면 "종소세 신고" 탭(2026-11-28). 배당 내역 조회와 따로 본다 - 귀속연도 단위라 기간 · 필터를 쓰지 않는다. --%>
@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param net.luversof.web.gate.stock.service.DividendTaxReportSupport.View taxReport = null
@param String taxInputResult = ""

@template._layout.stockLayout(
	pageTitle = MessageUtil.getMessage("stock.page.dividend.tab.tax"),
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentStock = @`
		<div class="space-y-6">
			@template._components.ui.pageHeader(title = MessageUtil.getMessage("stock.page.dividend.title"))
			@template.stock.fragments.dividendTabs(current = "tax")
			@template.stock.fragments.dividendTaxReport(view = taxReport, inputResult = taxInputResult)
		</div>
	`
)
