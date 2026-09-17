@import io.github.luversof.boot.context.support.MessageUtil
@import java.util.List
@import net.luversof.web.gate.stock.domain.StockItem
@import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutImportRequest
@import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutUpsertRequest
@import net.luversof.web.gate.stock.dto.request.MonthlyDividendProfileUpsertRequest
@import net.luversof.web.gate.stock.dto.response.MonthlyDividendPayoutResponse
@import net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse
@import net.luversof.web.gate.stock.dto.view.MonthlyDividendReferenceSummaryView

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String adminTab = "data-management"
@param net.luversof.web.gate.stock.dto.response.DataStatusResponse dataStatus = null
@param net.luversof.web.gate.stock.dto.response.LedgerIntegrityResponse ledgerIntegrity = null
@param List<StockItem> stockItems = java.util.List.of()
@param List<MonthlyDividendProfileResponse> monthlyDividendProfiles = java.util.List.of()
@param List<MonthlyDividendPayoutResponse> monthlyDividendPayouts = java.util.List.of()
@param MonthlyDividendReferenceSummaryView monthlyDividendReferenceSummary = null
@param MonthlyDividendProfileUpsertRequest monthlyDividendProfileForm = null
@param MonthlyDividendPayoutUpsertRequest monthlyDividendPayoutForm = null
@param MonthlyDividendPayoutImportRequest monthlyDividendPayoutImportForm = null
@param String selectedMonthlyDividendSymbol = ""
@param String selectedMonthlyDividendSourceUrl = ""
@param String monthlyDividendProfileSort = "display-order"
@param String monthlyDividendProfileDirection = "asc"
@param boolean monthlyDividendProfileExists = false
@param boolean monthlyDividendPayoutExists = false
@param String selectedMonthlyDividendPayoutKey = ""
@param String selectedMonthlyDividendPayoutRecordDate = ""
@param String selectedMonthlyDividendPayoutPayDate = ""
@param String monthlyDividendReferenceErrorMessage = ""
@param String monthlyDividendReferenceResult = ""
@param String monthlyDividendReferenceResultMessage = ""
@param boolean monthlyDividendReferenceResultIsError = false
@param String monthlyDividendReferenceWarningMessage = ""

!{
	// 두 탭이 브라우저 탭·기록에서 같은 이름("관리")이었다 - 실측 2026-09-12: 13 화면 제목 전수에서
	// 같은 제목을 쓰는 화면은 이곳뿐이었다. 화면 제목(H1)은 "관리" 그대로 두고 문서 제목에만 탭을 덧붙인다.
	String adminTabLabel = "monthly-reference".equals(adminTab)
		? MessageUtil.getMessage("stock.page.dividend.tab.monthly.reference")
		: MessageUtil.getMessage("stock.admin.label.data.management");
	String adminPageTitle = MessageUtil.getMessage("stock.page.admin.title") + " · " + adminTabLabel;
}
@template._layout.stockLayout(
	pageTitle = adminPageTitle,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentStock = @`
		<div class="space-y-6">
			@template._components.ui.pageHeader(title = MessageUtil.getMessage("stock.page.admin.title"))
			<div class="space-y-6">
				<div class="tabs tabs-scroll tabs-box w-full bg-base-200/70 p-1">
					<a href="/stock/admin" aria-current="${"data-management".equals(adminTab) ? "page" : null}" class="${"data-management".equals(adminTab) ? "tab tab-lg flex-1 font-bold tab-active !bg-primary !text-primary-content shadow-sm" : "tab tab-lg flex-1 text-base-content/60 hover:text-base-content"}">${MessageUtil.getMessage("stock.admin.label.data.management")}</a>
					<a href="/stock/admin?tab=monthly-reference" aria-current="${"monthly-reference".equals(adminTab) ? "page" : null}" class="${"monthly-reference".equals(adminTab) ? "tab tab-lg flex-1 font-bold tab-active !bg-primary !text-primary-content shadow-sm" : "tab tab-lg flex-1 text-base-content/60 hover:text-base-content"}">${MessageUtil.getMessage("stock.page.dividend.tab.monthly.reference")}</a>
				</div>

				@if("monthly-reference".equals(adminTab))
					@template.stock.fragments.monthlyDividendReference(
						stockItems = stockItems,
						monthlyDividendProfiles = monthlyDividendProfiles,
						monthlyDividendPayouts = monthlyDividendPayouts,
						monthlyDividendReferenceSummary = monthlyDividendReferenceSummary,
						monthlyDividendProfileForm = monthlyDividendProfileForm,
						monthlyDividendPayoutForm = monthlyDividendPayoutForm,
						monthlyDividendPayoutImportForm = monthlyDividendPayoutImportForm,
						selectedMonthlyDividendSymbol = selectedMonthlyDividendSymbol,
						selectedMonthlyDividendSourceUrl = selectedMonthlyDividendSourceUrl,
						monthlyDividendProfileSort = monthlyDividendProfileSort,
						monthlyDividendProfileDirection = monthlyDividendProfileDirection,
						monthlyDividendProfileExists = monthlyDividendProfileExists,
						monthlyDividendPayoutExists = monthlyDividendPayoutExists,
						selectedMonthlyDividendPayoutKey = selectedMonthlyDividendPayoutKey,
						selectedMonthlyDividendPayoutRecordDate = selectedMonthlyDividendPayoutRecordDate,
						selectedMonthlyDividendPayoutPayDate = selectedMonthlyDividendPayoutPayDate,
						monthlyDividendReferenceErrorMessage = monthlyDividendReferenceErrorMessage,
						monthlyDividendReferenceResult = monthlyDividendReferenceResult,
						monthlyDividendReferenceResultMessage = monthlyDividendReferenceResultMessage,
						monthlyDividendReferenceResultIsError = monthlyDividendReferenceResultIsError,
						monthlyDividendReferenceWarningMessage = monthlyDividendReferenceWarningMessage)
					<script type="module" src="/js/stock/monthlyDividendProfileOrder.js"></script>
					<script type="module" src="/js/stock/submitOverlay.js"></script>
				@else
					@template.stock.htmx.fragments.adminActions(isAuthenticated = isAuthenticated, dataStatus = dataStatus, ledgerIntegrity = ledgerIntegrity)
				@endif
			</div>
		</div>
	`
)
