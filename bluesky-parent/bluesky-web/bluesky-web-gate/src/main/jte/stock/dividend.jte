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
@param String dividendTab = "history"
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

@template._layout.stockLayout(
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentStock = @`
		<div class="container mx-auto p-4 space-y-6">
			<div class="flex justify-between items-center mb-6">
				<h2 class="text-2xl font-bold">${MessageUtil.getMessage("stock.page.dividend.title")}</h2>
			</div>
			<div class="min-h-[200px]" hx-get="/stock/htmx/dividend/list" hx-trigger="load" hx-target="this" hx-swap="outerHTML" hx-include="#globalDateRangeInputs input">
				<div class="flex items-center gap-2 text-base-content/60">
					<span class="loading loading-spinner loading-sm"></span>
					<span>${MessageUtil.getMessage("common.message.loading")}</span>
				</div>
			</div>
		</div>
	`
)