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

@template._layout.stockLayout(
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentStock = @`
		<div class="space-y-6">
			@template._components.ui.pageHeader(title = MessageUtil.getMessage("stock.page.admin.title"))
			<div class="space-y-6">
				<div class="tabs tabs-box w-full bg-base-200/70 p-1">
					<a href="/stock/admin" class="${"data-management".equals(adminTab) ? "tab tab-lg flex-1 font-bold tab-active !bg-primary !text-primary-content shadow-sm" : "tab tab-lg flex-1 text-base-content/60 hover:text-base-content"}">${MessageUtil.getMessage("stock.admin.label.data.management")}</a>
					<a href="/stock/admin?tab=monthly-reference" class="${"monthly-reference".equals(adminTab) ? "tab tab-lg flex-1 font-bold tab-active !bg-primary !text-primary-content shadow-sm" : "tab tab-lg flex-1 text-base-content/60 hover:text-base-content"}">${MessageUtil.getMessage("stock.page.dividend.tab.monthly.reference")}</a>
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
						monthlyDividendReferenceResultIsError = monthlyDividendReferenceResultIsError)
					<script type="module" src="/js/stock/monthlyDividendProfileOrder.js"></script>
				@else
					@template.stock.htmx.fragments.adminActions(isAuthenticated = isAuthenticated, dataStatus = dataStatus, ledgerIntegrity = ledgerIntegrity)
				@endif
			</div>
		</div>
	`
)
