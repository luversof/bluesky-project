@import net.luversof.web.gate.util.CspNonceHolder
@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.math.RoundingMode
@import java.text.DecimalFormat
@import java.time.LocalDate
@import java.util.List
@import java.util.Map
@import net.luversof.web.gate.stock.domain.StockItem
@import net.luversof.web.gate.stock.dto.request.MonthlyDividendSnapshotUpsertRequest
@import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse
@import net.luversof.web.gate.stock.dto.view.MonthlyDividendSimulatorSummaryView
@import net.luversof.web.gate.stock.util.StockFormatUtil
@import net.luversof.web.gate.stock.util.StockDetailLinkUtil

@param List<MonthlyDividendSnapshotResponse> monthlyDividendRows = java.util.List.of()
<%-- 이 표의 '현재가'가 어느 날 종가인지. 배당 기준값(스냅샷 시점)과 시세는 시점이 다르다. --%>
@param java.time.LocalDate monthlyDividendPriceBasisDate = null
@param boolean monthlyDividendCurrentHoldingsUnavailable = false
<%-- 종목 -> 저장된 지급 이력으로 계산한 1년 평균 과세표준 비중(관리 탭과 같은 계산). 저장값과 벌어지면 그 자리에서 알린다. --%>
@param java.util.Map<java.util.UUID, BigDecimal> monthlyDividendReferenceTaxableRatios = java.util.Map.of()
<%-- 종목별 최근 1 년 배당을 과세이연 계좌 몫 / 그 밖 계좌 몫으로 나눈 것. 비중 칸 아래 설명에 쓴다. --%>
@param java.util.Map<java.util.UUID, net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport.TaxableRatioBasis> monthlyDividendTaxableRatioBasis = java.util.Map.of()
@param BigDecimal monthlyDividendReferenceTaxableTotal = null
@param int monthlyDividendReferenceTaxableStaleCount = 0
<%-- 원장의 현재 보유 수량(종목 id -> 수량). 스냅샷 수량과 다르면 표에 함께 적는다. --%>
@param java.util.Map<java.util.UUID, Integer> monthlyDividendCurrentQuantities = java.util.Map.of()
@param long monthlyDividendStaleQuantityCount = 0L
@param BigDecimal monthlyDividendCurrentQuantityTotal = java.math.BigDecimal.ZERO
<%-- 원장의 현재 평균단가. 스냅샷 평단과 다르면 표에 함께 적는다 - 원가기준 수익률이 이 값으로 계산되기 때문이다. --%>
@param java.util.Map<java.util.UUID, BigDecimal> monthlyDividendCurrentAverageBuyPrices = java.util.Map.of()
@param MonthlyDividendSimulatorSummaryView monthlyDividendSummary = null
@param List<StockItem> stockItems = java.util.List.of()
@param MonthlyDividendSnapshotUpsertRequest monthlyDividendForm = null
@param String monthlyDividendBulkInput = ""
@param String monthlyDividendErrorMessage = ""
@param String monthlyDividendResult = ""
@param Integer monthlyDividendSavedCount = null
@param String monthlyDividendSort = "display-order"
@param String monthlyDividendDirection = "asc"
@param String monthlyDividendKeyword = ""
@param BigDecimal monthlyDividendMinAnnualYield = null
@param boolean monthlyDividendPositiveOnly = false
@param boolean monthlyDividendHasSavedRows = false
@param Map<String, Integer> monthlyDividendProfileDisplayOrders = java.util.Map.of()
@param List<String> monthlyDividendProfileOrderedSymbols = java.util.List.of()
@param boolean monthlyDividendReorderEnabled = false
@param Map<String, String> monthlyDividendPayoutWindowBySymbol = java.util.Map.of()

!{
	DecimalFormat amountFormat = new DecimalFormat("#,##0");
	DecimalFormat percentFormat = new DecimalFormat("#,##0.00");
	boolean monthlyDividendDisplayOrderSort = "display-order".equals(monthlyDividendSort);
	MonthlyDividendSnapshotResponse bestChoice = monthlyDividendSummary != null ? monthlyDividendSummary.bestChoice() : null;
	// 행마다 비교 우위 순위. 1 위 하나만 배지를 달던 표에 순위를 전부 적는다(사용자 요청 2026-09-17).
	java.util.Map<java.util.UUID, Integer> comparisonRanks = monthlyDividendSummary != null && monthlyDividendSummary.comparisonRanks() != null ? monthlyDividendSummary.comparisonRanks() : java.util.Map.of();
	String comparisonRankPattern = MessageUtil.getMessage("stock.simulator.compare.rank");
	BigDecimal totalLatestMonthlyDividend = monthlyDividendSummary != null ? monthlyDividendSummary.totalLatestMonthlyDividend() : BigDecimal.ZERO;
	BigDecimal totalLatestMonthlyDividendMidMonth = monthlyDividendSummary != null ? monthlyDividendSummary.totalLatestMonthlyDividendMidMonth() : BigDecimal.ZERO;
	BigDecimal totalLatestMonthlyDividendMonthEnd = monthlyDividendSummary != null ? monthlyDividendSummary.totalLatestMonthlyDividendMonthEnd() : BigDecimal.ZERO;
	BigDecimal totalExpectedMonthlyDividend = monthlyDividendSummary != null ? monthlyDividendSummary.totalExpectedMonthlyDividend() : BigDecimal.ZERO;
	BigDecimal totalExpectedAnnualDividend = monthlyDividendSummary != null ? monthlyDividendSummary.totalExpectedAnnualDividend() : BigDecimal.ZERO;
	BigDecimal totalExpectedTaxableBaseAmount = monthlyDividendSummary != null ? monthlyDividendSummary.totalExpectedTaxableBaseAmount() : BigDecimal.ZERO;
	BigDecimal totalExpectedAnnualTaxableBaseAmount = monthlyDividendSummary != null ? monthlyDividendSummary.totalExpectedAnnualTaxableBaseAmount() : BigDecimal.ZERO;
	BigDecimal totalBuyAmount = monthlyDividendSummary != null ? monthlyDividendSummary.totalBuyAmount() : BigDecimal.ZERO;
	BigDecimal totalCurrentMarketValue = monthlyDividendSummary != null ? monthlyDividendSummary.totalCurrentMarketValue() : BigDecimal.ZERO;
	BigDecimal totalEvaluationProfit = totalCurrentMarketValue.subtract(totalBuyAmount);
	BigDecimal totalEvaluationReturnPct = totalBuyAmount.signum() > 0
		? totalEvaluationProfit.multiply(BigDecimal.valueOf(100)).divide(totalBuyAmount, 2, RoundingMode.HALF_UP)
		: BigDecimal.ZERO;
	BigDecimal portfolioExpectedAnnualYieldPct = monthlyDividendSummary != null ? monthlyDividendSummary.portfolioExpectedAnnualYieldPct() : BigDecimal.ZERO;
	int itemCount = monthlyDividendSummary != null ? monthlyDividendSummary.itemCount() : 0;
	MonthlyDividendSnapshotUpsertRequest form = monthlyDividendForm != null ? monthlyDividendForm : new MonthlyDividendSnapshotUpsertRequest();
	boolean referenceValuesAvailable = form.getLatestMonthlyDividendPerShare() != null && form.getAverageMonthlyDividendPerShare1y() != null && form.getAverageTaxableBaseRatio1y() != null;
	String asOfDateValue = String.valueOf(form.getAsOfDate() != null ? form.getAsOfDate() : LocalDate.now());
	String symbolValue = form.getSymbol() != null ? form.getSymbol() : "";
	String latestMonthlyDividendValue = form.getLatestMonthlyDividendPerShare() != null ? form.getLatestMonthlyDividendPerShare().stripTrailingZeros().toPlainString() : "";
	String averageMonthlyDividendValue = form.getAverageMonthlyDividendPerShare1y() != null ? form.getAverageMonthlyDividendPerShare1y().stripTrailingZeros().toPlainString() : "";
	String taxableBaseRatioValue = form.getAverageTaxableBaseRatio1y() != null ? form.getAverageTaxableBaseRatio1y().stripTrailingZeros().toPlainString() : "";
	String heldQuantityValue = form.getHeldQuantity() != null ? String.valueOf(form.getHeldQuantity()) : "";
	String averageBuyPriceValue = form.getAverageBuyPrice() != null ? form.getAverageBuyPrice().stripTrailingZeros().toPlainString() : "";
	// 폼은 저장된 스냅샷 값을 그대로 채운다 - 원장과 어긋날 수 있다. 아래 표는 이미 "현재 N" 으로 알려 주는데
	// 정작 고치는 자리인 폼에는 아무 말이 없었다(실측 2026-09-12: 8 종목 중 3 종목의 평균단가가 달랐고
	// 표에는 "현재 4,915" · "현재 4,367" · "현재 11,281" 이 떠 있었지만 폼에는 4,914 · 4,381 · 11,307 만 있었다).
	java.util.UUID formStockItemId = null;
	if (!symbolValue.isBlank() && stockItems != null) {
		for (StockItem candidate : stockItems) {
			if (candidate != null && candidate.symbol() != null && candidate.symbol().equalsIgnoreCase(symbolValue.trim())) {
				formStockItemId = candidate.id();
				break;
			}
		}
	}
	Integer formCurrentQuantity = formStockItemId != null ? monthlyDividendCurrentQuantities.get(formStockItemId) : null;
	BigDecimal formCurrentAverageBuyPrice = formStockItemId != null ? monthlyDividendCurrentAverageBuyPrices.get(formStockItemId) : null;
	// 표와 같은 판정: 수량은 그대로, 평균단가는 원 단위로 반올림해 견준다.
	boolean formQuantityDiffers = formCurrentQuantity != null && form.getHeldQuantity() != null
			&& formCurrentQuantity.intValue() != form.getHeldQuantity().intValue();
	boolean formAverageBuyPriceDiffers = formCurrentAverageBuyPrice != null && form.getAverageBuyPrice() != null
			&& formCurrentAverageBuyPrice.setScale(0, java.math.RoundingMode.HALF_UP)
					.compareTo(form.getAverageBuyPrice().setScale(0, java.math.RoundingMode.HALF_UP)) != 0;
	String minAnnualYieldValue = monthlyDividendMinAnnualYield != null ? monthlyDividendMinAnnualYield.stripTrailingZeros().toPlainString() : "";
	String monthlyDividendReferenceManageUrl = "/stock/admin?tab=monthly-reference";
	String totalLatestMonthlyDividendLabel = MessageUtil.getMessage("stock.simulator.monthly.summary.latest.monthly.dividend");
	String payoutWindowMidMonthLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.mid.month");
	String payoutWindowMonthEndLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.month.end");
	String totalMonthlyTaxableBaseSummaryLabel = MessageUtil.getMessage("stock.simulator.monthly.summary.total.taxable.base");
	String totalAnnualTaxableBaseSummaryLabel = MessageUtil.getMessage("stock.simulator.monthly.summary.total.annual.taxable.base");
	String totalBuyAmountLabel = MessageUtil.getMessage("stock.simulator.monthly.table.header.buy.amount");
	String totalCurrentMarketValueLabel = MessageUtil.getMessage("stock.simulator.monthly.table.header.market.value");
	String monthlySelectionHelperLabel = MessageUtil.getMessage("stock.simulator.monthly.selection.helper");
	String monthlyDividendReorderEnabledLabel = MessageUtil.getMessage("stock.simulator.monthly.reorder.enabled");
	String monthlyDividendReorderDisabledLabel = MessageUtil.getMessage("stock.simulator.monthly.reorder.disabled");
	String selectionSummaryLabel = MessageUtil.getMessage("stock.analytics.selection.summary");
	String selectionCountTemplate = MessageUtil.getMessage("stock.analytics.selection.count");
	String selectionClearLabel = MessageUtil.getMessage("stock.analytics.selection.clear");
	String selectionOverviewLabel = MessageUtil.getMessage("stock.simulator.monthly.selection.card.overview");
	// 이 표의 '저장된 기준값' 3열은 관리 탭의 기준 데이터를 그때그때 계산한 값이 아니라, 저장해 둔 스냅샷이다.
	// 둘이 벌어질 수 있다 - 실측 2026-09-11, 같은 이름('1년 평균 과세표준 비중')인데 8종목 전부 달랐다:
	//   476800 관리 82.79% / 여기 17.35%, 329200 관리 100.00% / 여기 13.42%, 498400 관리 4.46% / 여기 4.06%.
	// (주당 분배금은 두 화면이 일치한다 - 비중만 벌어졌다.) 그래서 열 이름을 '기준 데이터' 에서 바꿔
	// 관리 탭의 값과 같은 것처럼 읽히지 않게 했다. 값 자체는 사용자가 관리 화면의 '이 값으로 시뮬레이터 채우기' 로 갱신한다.
	// 기준 데이터의 '언제 시점' 값인지. asOfDate 는 그 종목 지급 이력의 최신 지급일과 같다.
	// 이 값이 오래되면 최근 월 배당금 같은 대표 수치가 조용히 옛 분배금으로 계산된다
	// (실측: 월중배당 4 종목이 30 일 뒤처져 총 최근 월 배당금이 과대).
	java.time.LocalDate oldestAsOfDate = monthlyDividendRows.stream()
		.map(net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse::asOfDate)
		.filter(java.util.Objects::nonNull).min(java.time.LocalDate::compareTo).orElse(null);
	java.time.LocalDate newestAsOfDate = monthlyDividendRows.stream()
		.map(net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse::asOfDate)
		.filter(java.util.Objects::nonNull).max(java.time.LocalDate::compareTo).orElse(null);
	String selectionDividendLabel = MessageUtil.getMessage("stock.simulator.monthly.selection.card.dividend");
	String selectionInvestmentLabel = MessageUtil.getMessage("stock.simulator.monthly.selection.card.investment");
	String selectionEfficiencyLabel = MessageUtil.getMessage("stock.simulator.monthly.selection.card.efficiency");
	String selectionItemCountLabel = MessageUtil.getMessage("stock.simulator.monthly.selection.item.count");
	String selectionMonthlyWeightLabel = MessageUtil.getMessage("stock.simulator.monthly.selection.weight.monthly");
	String selectionMarketWeightLabel = MessageUtil.getMessage("stock.simulator.monthly.selection.weight.market.value");
	String selectionInvestmentProfitLabel = MessageUtil.getMessage("stock.simulator.monthly.selection.investment.profit");
	String selectionInvestmentReturnLabel = MessageUtil.getMessage("stock.simulator.monthly.selection.investment.return");
	// 머리칸 정렬은 전체 이동 링크다. 화면의 필터(검색어·최소 연배당 수익률·양수만)를 함께 싣지 않으면
	// 정렬 한 번에 필터가 조용히 풀린다 - 실측 2026-09-13: keyword=TIGER 로 3 행이던 표가 정렬 클릭 뒤
	// 8 행(전체)이 되었고 검색어 칸도 비워졌다.
	String monthlyDividendTableBaseUrl = "/stock/simulator?tab=monthly-dividend"
		+ (monthlyDividendKeyword == null || monthlyDividendKeyword.isBlank() ? ""
			: "&keyword=" + java.net.URLEncoder.encode(monthlyDividendKeyword, java.nio.charset.StandardCharsets.UTF_8))
		+ (monthlyDividendMinAnnualYield == null ? ""
			: "&minAnnualYield=" + monthlyDividendMinAnnualYield.toPlainString())
		+ (monthlyDividendPositiveOnly ? "&positiveOnly=true" : "");
	String currentDirectionIndicator = "asc".equals(monthlyDividendDirection) ? "↑" : "↓";
	String displayOrderDirection = "display-order".equals(monthlyDividendSort) ? ("asc".equals(monthlyDividendDirection) ? "desc" : "asc") : "asc";
	String symbolDirectionToggle = "symbol".equals(monthlyDividendSort) ? ("asc".equals(monthlyDividendDirection) ? "desc" : "asc") : "asc";
	// 두 묶음(현재가 기준 / 평단 기준)에 같은 이름의 정렬 링크가 각각 있다. 화면에서는 바로 위 묶음 제목으로
	// 갈리지만 링크 이름만 들으면 구분되지 않는다 - 실측 2026-09-11: 월배당 수익률/연배당 수익률이 각각
	// 두 번씩이고 정렬 키는 monthly-yield 와 monthly-yield-on-cost 로 다르다.
	String marketBasisLabel = MessageUtil.getMessage("stock.simulator.monthly.table.header.market.price.basis");
	String onCostBasisLabel = MessageUtil.getMessage("stock.simulator.monthly.table.header.on.cost");
	String marketYieldAriaSort = ("monthly-yield".equals(monthlyDividendSort) || "annual-yield".equals(monthlyDividendSort))
		? ("asc".equals(monthlyDividendDirection) ? "ascending" : "descending") : "none";
	String onCostYieldAriaSort = ("monthly-yield-on-cost".equals(monthlyDividendSort) || "annual-yield-on-cost".equals(monthlyDividendSort))
		? ("asc".equals(monthlyDividendDirection) ? "ascending" : "descending") : "none";
	// 한 칸에 정렬 링크가 둘인 머리글(표시 순서 / 종목코드)과 예상 합산 칸에는 상태가 없었다.
	// 실측 2026-09-12: sort=symbol / sort=combined-return / 기본(display-order) 셋 다 aria-sort 가 붙은 칸이
	// 0 개였고, 어느 열로 정렬됐는지는 링크 이름에 섞인 "↑" 글자로만 알 수 있었다.
	// 나머지 네 칸(예상 월 배당금·과세 표준 비중·수익률 둘)은 이미 제대로 켜진다.
	String displayOrderAriaSort = ("display-order".equals(monthlyDividendSort) || "symbol".equals(monthlyDividendSort))
		? ("asc".equals(monthlyDividendDirection) ? "ascending" : "descending") : "none";
	String combinedReturnAriaSort = "combined-return".equals(monthlyDividendSort)
		? ("asc".equals(monthlyDividendDirection) ? "ascending" : "descending") : "none";
	String monthlyYieldDirectionToggle = "monthly-yield".equals(monthlyDividendSort) ? ("asc".equals(monthlyDividendDirection) ? "desc" : "asc") : "desc";
	String monthlyDividendDirectionToggle = "monthly-dividend".equals(monthlyDividendSort) ? ("asc".equals(monthlyDividendDirection) ? "desc" : "asc") : "desc";
	String taxableBaseDirectionToggle = "taxable-base".equals(monthlyDividendSort) ? ("asc".equals(monthlyDividendDirection) ? "desc" : "asc") : "asc";
	String annualYieldDirectionToggle = "annual-yield".equals(monthlyDividendSort) ? ("asc".equals(monthlyDividendDirection) ? "desc" : "asc") : "desc";
	String monthlyYieldOnCostDirectionToggle = "monthly-yield-on-cost".equals(monthlyDividendSort) ? ("asc".equals(monthlyDividendDirection) ? "desc" : "asc") : "desc";
	String annualYieldOnCostDirectionToggle = "annual-yield-on-cost".equals(monthlyDividendSort) ? ("asc".equals(monthlyDividendDirection) ? "desc" : "asc") : "desc";
	String combinedReturnDirectionToggle = "combined-return".equals(monthlyDividendSort) ? ("asc".equals(monthlyDividendDirection) ? "desc" : "asc") : "desc";
	String monthlyDividendOrderEndpoint = "/stock/dividend/monthly-reference/profile/order";
	String successMessage = "";
	if ("single-saved".equals(monthlyDividendResult)) {
		successMessage = MessageUtil.getMessage("stock.simulator.monthly.saved");
	} else if ("sheet-imported".equals(monthlyDividendResult)) {
		successMessage = java.text.MessageFormat.format(
			MessageUtil.getMessage("stock.simulator.monthly.sheet.imported"),
			monthlyDividendSavedCount != null ? monthlyDividendSavedCount : 0);
	}
}

<div class="space-y-6">
	@if(!successMessage.isEmpty())
		<div class="alert alert-success border border-success/20 bg-success/10 text-success-content" role="status">
			<span>${successMessage}</span>
		</div>
	@endif
	@if(monthlyDividendErrorMessage != null && !monthlyDividendErrorMessage.isEmpty())
		<div class="alert alert-error border border-error/20 bg-error/10 text-error-content" role="alert">
			<span>${monthlyDividendErrorMessage}</span>
		</div>
	@endif

	<datalist id="monthlyDividendStockOptions">
		@for(StockItem stockItem : stockItems)
			<option value="${stockItem.symbol()}">${stockItem.name()}</option>
		@endfor
	</datalist>

	<div class="space-y-6">
		<div class="space-y-6">
			@template._components.ui.card(bodyClass = "gap-5", body = @`
					<div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
						<div class="space-y-2">
							<h2 class="card-title text-lg">${MessageUtil.getMessage("stock.simulator.monthly.form.title")}</h2>
							<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.form.desc")}</p>
						</div>
						<div class="flex flex-wrap items-center gap-2"><form method="post" action="/stock/simulator/monthly-dividend/import-sheet" class="inline" data-submit-overlay="true" data-submit-overlay-title="${MessageUtil.getMessage("stock.simulator.monthly.import.sheet")}" data-submit-overlay-desc="${MessageUtil.getMessage("stock.simulator.monthly.import.sheet.overlay.desc")}"><input type="hidden" name="sort" value="${monthlyDividendSort}" /><input type="hidden" name="direction" value="${monthlyDividendDirection}" /><button type="submit" class="btn btn-secondary btn-sm h-auto max-w-full whitespace-normal">${MessageUtil.getMessage("stock.simulator.monthly.import.sheet")}</button></form><a href="${monthlyDividendReferenceManageUrl}" class="btn btn-outline btn-sm h-auto max-w-full whitespace-normal">${MessageUtil.getMessage("stock.simulator.monthly.reference.manage")}</a></div>
					</div>
					@if(symbolValue != null && !symbolValue.isEmpty() && !referenceValuesAvailable)
						<div class="alert border border-warning/20 bg-warning/10 text-warning-content" role="status">
							<div class="flex w-full flex-col gap-3 md:flex-row md:items-center md:justify-between">
								<span>${MessageUtil.getMessage("stock.simulator.monthly.reference.empty")}</span>
								<a href="${monthlyDividendReferenceManageUrl}" class="btn btn-warning btn-sm">${MessageUtil.getMessage("stock.simulator.monthly.reference.manage")}</a>
							</div>
						</div>
					@endif
					<form method="post" action="/stock/simulator/monthly-dividend" class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
						<input type="hidden" name="sort" value="${monthlyDividendSort}" />
						<input type="hidden" name="direction" value="${monthlyDividendDirection}" />
						<label class="form-control">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.simulator.monthly.form.field.symbol")}</span></div>
							<input name="symbol" type="text" list="monthlyDividendStockOptions" value="${symbolValue}" placeholder="O" class="input input-bordered w-full uppercase" required />
						</label>
						<label class="form-control">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.simulator.monthly.form.field.quantity")}</span></div>
							<input name="heldQuantity" type="number" min="1" step="1" value="${heldQuantityValue}" class="input input-bordered w-full" required />
							@if(formQuantityDiffers)
								<span class="label-text-alt mt-1 text-warning amount-value" data-form-current-quantity>${java.text.MessageFormat.format(MessageUtil.getMessage("stock.simulator.monthly.table.cell.quantity.current"), amountFormat.format(formCurrentQuantity))}</span>
							@endif
						</label>
						<label class="form-control">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.simulator.monthly.form.field.average.buy.price")}</span></div>
							<input name="averageBuyPrice" type="number" min="0" step="0.01" value="${averageBuyPriceValue}" class="input input-bordered w-full" required />
							@if(formAverageBuyPriceDiffers)
								<span class="label-text-alt mt-1 text-warning amount-value" data-form-current-average-buy-price>${java.text.MessageFormat.format(MessageUtil.getMessage("stock.simulator.monthly.table.cell.average.buy.price.current"), amountFormat.format(formCurrentAverageBuyPrice))}</span>
							@endif
						</label>
						<%-- 좁은 폭에서는 단추 글자가 줄을 바꾼다 - 글꼴 200% 에서 "시뮬레이터 반영" 한 줄이 칸보다 넓어 페이지를 밀었다(320px, 실측 2026-09-17).
						     줄은 띄어쓰기에서 바꾸고(break-keep - 안 주면 "시뮬레 / 이터 반 / 영"), 단어가 칸보다 넓을 때만 그 안에서 끊는다. --%>
						<div class="flex items-end">
							<button type="submit" class="btn btn-primary w-full max-sm:h-auto max-sm:min-h-0 max-sm:py-2 max-sm:whitespace-normal max-sm:break-keep max-sm:wrap-anywhere">${MessageUtil.getMessage("stock.simulator.monthly.form.submit")}</button>
						</div>
						<p class="text-xs leading-5 text-base-content/60 md:col-span-2 xl:col-span-4">${MessageUtil.getMessage("stock.simulator.monthly.form.hint")}</p>
					</form>
			`)
		</div>

		<div class="space-y-6">
			<div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
				<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-4 shadow-sm">
					<div class="text-xs text-base-content/60">${selectionOverviewLabel}</div>
					<div class="mt-3 space-y-3">
						<div class="flex items-center justify-between gap-3 text-sm">
							<span class="text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.summary.item.count")}</span>
							<span class="font-mono font-semibold">${amountFormat.format(itemCount)}</span>
						</div>
						@if(oldestAsOfDate != null)
						<div class="flex items-center justify-between gap-3 text-sm">
							<span class="text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.summary.as.of.date")}</span>
							<span class="font-mono">${oldestAsOfDate.toString()}${newestAsOfDate != null && !newestAsOfDate.equals(oldestAsOfDate) ? " ~ " + newestAsOfDate.toString() : ""}</span>
						</div>
						@endif
						<%-- 시세 기준일은 스냅샷 기준일과 다르다 - 실측 2026-09-11: 스냅샷 2026-08-19 인데 현재가는 2026-09-09 종가였다. --%>
						@if(monthlyDividendPriceBasisDate != null)
						<div class="text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.asset.status.price.basis").replace("{0}", monthlyDividendPriceBasisDate.toString())}</div>
						@endif
						<%-- 원장 조회가 실패하면 어긋난 줄에 붙던 "현재 N" 표시가 통째로 사라진다. 표시가 없는 표는
						     "원장과 같다" 로 읽히므로(실측 2026-09-12: 8 줄 중 3 줄이 표시를 달고 있었다) 실패를 밝힌다. --%>
						@if(monthlyDividendCurrentHoldingsUnavailable)
						<div class="text-[11px] text-warning" data-ledger-unavailable="true">${MessageUtil.getMessage("stock.holdings.ledger.unavailable")}</div>
						@endif
						<%-- 이 카드의 합계는 스냅샷에 저장된 보유 수량으로 계산된다. 표의 각 행은 수량이 달라지면 "현재 N 주"를
						     경고색으로 알리는데 합계만 조용하면 사용자는 현재 기준으로 읽는다. 요약 화면의 다가오는 배당 카드와
						     같은 문구를 쓴다. --%>
						@if(monthlyDividendStaleQuantityCount > 0)
						<div class="text-[11px] text-warning">
							${java.text.MessageFormat.format(
								MessageUtil.getMessage("stock.summary.upcoming.dividend.stale.quantity"),
								String.valueOf(monthlyDividendStaleQuantityCount),
								"₩" + String.format("%,d", StockFormatUtil.displayWon(monthlyDividendCurrentQuantityTotal)))}
						</div>
						@endif
						<div class="flex items-start justify-between gap-3 text-sm">
							<span class="text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.summary.best.choice")}</span>
							<%-- 이 카드는 화면의 결론이다 - 실측 2026-09-12: 종목코드만 적혀 "498400" 이었는데, 같은 화면의 표는
								 줄마다 "498400 KODEX 200타겟위클리커버드콜" 처럼 코드와 이름을 함께 적는다. 코드만으로는 어느 종목인지 모른다. --%>
							<span class="text-right font-semibold">
								@if(bestChoice != null)
									${bestChoice.stockItemSymbol()}
									@if(bestChoice.stockItemName() != null && !bestChoice.stockItemName().isBlank())
										<span class="block text-xs font-normal text-base-content/60" data-best-choice-name>${bestChoice.stockItemName()}</span>
									@endif
								@else
									${MessageUtil.getMessage("stock.simulator.monthly.summary.no.best.choice")}
								@endif
							</span>
						</div>
						<%-- 1 위 하나만 말하던 카드다. 무엇으로 순위를 매기는지 적고, 표를 그 순서로 여는 길을 준다(사용자 요청 2026-09-17:
						     "비교 우위를 최상 1 개만 표시하는데 순위를 확인할 수 있으면"). 합산 수익률 열 정렬이 같은 비교기를 쓴다. --%>
						<p class="text-[11px] leading-4 text-base-content/60" data-comparison-basis>
							${MessageUtil.getMessage("stock.simulator.monthly.summary.comparison.basis")}
							@if(bestChoice != null)
								<a class="link link-hover whitespace-nowrap" href="${monthlyDividendTableBaseUrl}&sort=combined-return&direction=desc#monthlyDividendCompareTable" data-comparison-rank-link>${MessageUtil.getMessage("stock.simulator.monthly.summary.comparison.rank.link")}</a>
							@endif
						</p>
					</div>
				</div>
				<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-4 shadow-sm">
					<div class="text-xs text-base-content/60">${selectionDividendLabel}</div>
					<div class="mt-3 space-y-3">
						<div>
							<div class="text-[11px] text-base-content/60">${totalLatestMonthlyDividendLabel}</div>
							<div class="mt-1 font-mono font-semibold amount-value">₩${amountFormat.format(totalLatestMonthlyDividend)}</div>
								<div class="mt-0.5 flex flex-wrap gap-x-2 text-[11px] text-base-content/60"><span>${payoutWindowMidMonthLabel} <span class="font-mono amount-value">₩${amountFormat.format(totalLatestMonthlyDividendMidMonth)}</span></span><span>${payoutWindowMonthEndLabel} <span class="font-mono amount-value">₩${amountFormat.format(totalLatestMonthlyDividendMonthEnd)}</span></span></div>
						</div>
						<div>
							<div class="text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.summary.total.monthly.dividend")}</div>
							<div class="mt-1 font-mono font-semibold amount-value">₩${amountFormat.format(totalExpectedMonthlyDividend)}</div>
						</div>
						<div>
							<div class="text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.summary.total.annual.dividend")}</div>
							<div class="mt-1 font-mono font-semibold amount-value">₩${amountFormat.format(totalExpectedAnnualDividend)}</div>
						</div>
					</div>
				</div>
				<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-4 shadow-sm">
					<div class="text-xs text-base-content/60">${selectionInvestmentLabel}</div>
					<div class="mt-3 space-y-3">
						<div>
							<div class="text-[11px] text-base-content/60">${totalBuyAmountLabel}</div>
							<div class="mt-1 font-mono font-semibold amount-value">₩${amountFormat.format(totalBuyAmount)}</div>
						</div>
						<div>
							<div class="text-[11px] text-base-content/60">${totalCurrentMarketValueLabel}</div>
							<div class="mt-1 font-mono font-semibold amount-value">₩${amountFormat.format(totalCurrentMarketValue)}</div>
						</div>
						<div class="border-t border-base-200/70 pt-3 space-y-2">
							<div class="flex items-center justify-between gap-3 text-sm">
								<span class="text-base-content/60">${selectionInvestmentProfitLabel}</span>
								<span class="amount-value font-mono font-semibold ${totalEvaluationProfit.signum() > 0 ? "text-profit" : totalEvaluationProfit.signum() < 0 ? "text-loss" : ""}">${totalEvaluationProfit.signum() > 0 ? "+₩" : totalEvaluationProfit.signum() < 0 ? "-₩" : "₩"}${amountFormat.format(totalEvaluationProfit.abs())}</span>
							</div>
							<div class="flex items-center justify-between gap-3 text-sm">
								<span class="text-base-content/60">${selectionInvestmentReturnLabel}</span>
								<span class="font-mono font-semibold ${totalEvaluationReturnPct.signum() > 0 ? "text-profit" : totalEvaluationReturnPct.signum() < 0 ? "text-loss" : ""}">${totalEvaluationReturnPct.signum() > 0 ? "+" : totalEvaluationReturnPct.signum() < 0 ? "-" : ""}${percentFormat.format(totalEvaluationReturnPct.abs())}%</span>
							</div>
						</div>
					</div>
				</div>
				<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-4 shadow-sm">
					<div class="text-xs text-base-content/60">${selectionEfficiencyLabel}</div>
					<div class="mt-3 space-y-3">
						<div>
							<div class="text-[11px] text-base-content/60">${totalMonthlyTaxableBaseSummaryLabel}</div>
							<div class="mt-1 font-mono font-semibold amount-value">₩${amountFormat.format(totalExpectedTaxableBaseAmount)}</div>
							@if(monthlyDividendReferenceTaxableStaleCount > 0 && monthlyDividendReferenceTaxableTotal != null)
								<div class="mt-1 text-[11px] text-base-content/60 amount-value" data-reference-taxable-total="${monthlyDividendReferenceTaxableTotal.toPlainString()}">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.simulator.monthly.summary.taxable.reference"), amountFormat.format(monthlyDividendReferenceTaxableTotal), String.valueOf(monthlyDividendReferenceTaxableStaleCount))}</div>
							@endif
						</div>
						<div>
							<div class="text-[11px] text-base-content/60">${totalAnnualTaxableBaseSummaryLabel}</div>
							<div class="mt-1 font-mono font-semibold amount-value">₩${amountFormat.format(totalExpectedAnnualTaxableBaseAmount)}</div>
						</div>
						<div>
							<div class="text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.summary.portfolio.annual.yield")}</div>
							<div class="mt-1 font-mono font-semibold">${percentFormat.format(portfolioExpectedAnnualYieldPct)}%</div>
						</div>
					</div>
				</div>
			</div>

			@template._components.ui.card(bodyClass = "gap-5", body = @`
					<div class="space-y-2">
						<div class="space-y-2">
							<h2 class="card-title text-lg" id="monthlyDividendCompareTable">${MessageUtil.getMessage("stock.simulator.monthly.section.table.title")}</h2>
							<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.section.table.desc")}</p>
						</div>
					</div>

					<%-- 표 좁히기. 서버는 오래전부터 keyword · minAnnualYield · positiveOnly 를 받았지만 화면에 입력 칸이 없어 주소를 직접 쳐야만
					     걸렸다(실측 2026-09-21: name="keyword" 입력 요소 0 개). 사용자 선택 2026-09-21: "검색·필터를 화면으로".
					     이 표는 보유 종목만 다룬다 - 안 가진 종목은 '월배당 ETF' 화면에서 본다. --%>
					<form method="get" action="/stock/simulator" class="grid gap-3 rounded-box bg-base-200/40 p-3 md:grid-cols-2 xl:grid-cols-4" data-monthly-filter-form>
						<input type="hidden" name="tab" value="monthly-dividend" />
						<input type="hidden" name="sort" value="${monthlyDividendSort}" />
						<input type="hidden" name="direction" value="${monthlyDividendDirection}" />
						<label class="form-control min-w-0 max-w-full">
							<div class="label py-1"><span class="label-text text-xs font-medium">${MessageUtil.getMessage("stock.simulator.monthly.filter.keyword")}</span></div>
							<input name="keyword" type="search" value="${monthlyDividendKeyword}" placeholder="${MessageUtil.getMessage("stock.simulator.monthly.filter.keyword.placeholder")}" class="input input-bordered input-sm w-full max-w-full" />
						</label>
						<label class="form-control min-w-0 max-w-full">
							<div class="label py-1"><span class="label-text text-xs font-medium">${MessageUtil.getMessage("stock.simulator.monthly.filter.min.annual.yield")}</span></div>
							<input name="minAnnualYield" type="number" step="0.01" min="0" value="${minAnnualYieldValue}" class="input input-bordered input-sm w-full max-w-full" />
						</label>
						<label class="label cursor-pointer justify-start gap-2 self-end py-2">
							<input name="positiveOnly" type="checkbox" value="true" class="checkbox checkbox-sm" checked="${monthlyDividendPositiveOnly}" />
							<span class="label-text text-xs font-medium">${MessageUtil.getMessage("stock.simulator.monthly.filter.positive.only")}</span>
						</label>
						<div class="flex items-end gap-2">
							<button type="submit" class="btn btn-primary btn-sm max-sm:h-auto max-sm:min-h-0 max-sm:py-2 max-sm:whitespace-normal max-sm:break-keep max-sm:wrap-anywhere">${MessageUtil.getMessage("stock.simulator.monthly.filter.apply")}</button>
							<%-- 필터 지우기 링크는 일부러 기준 주소(필터를 싣는 주소)를 쓰지 않는다(SimulatorSortKeepsFilterTest 예외). --%>
							<a data-filter-reset href="/stock/simulator?tab=monthly-dividend&sort=${monthlyDividendSort}&direction=${monthlyDividendDirection}" class="btn btn-ghost btn-sm max-sm:h-auto max-sm:min-h-0 max-sm:py-2 max-sm:whitespace-normal max-sm:break-keep max-sm:wrap-anywhere">${MessageUtil.getMessage("stock.simulator.monthly.filter.reset")}</a>
						</div>
					</form>

					@if(monthlyDividendRows == null || monthlyDividendRows.isEmpty())
						<div class="rounded-2xl border border-dashed border-base-300 bg-base-50 px-6 py-10 text-center">
							@if(monthlyDividendHasSavedRows)
								<p class="text-lg font-semibold text-base-content">${MessageUtil.getMessage("stock.simulator.monthly.empty.filtered.title")}</p>
								<p class="mt-2 text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.empty.filtered.desc")}</p>
							@else
								<p class="text-lg font-semibold text-base-content">${MessageUtil.getMessage("stock.simulator.monthly.empty.title")}</p>
								<p class="mt-2 text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.empty.desc")}</p>
							@endif
						</div>
					@else
						<div class="space-y-4" data-monthly-simulator-selection-section>
							<p class="text-xs text-base-content/60">${monthlySelectionHelperLabel}</p>
							<div id="monthlyDividendSimulatorOrderStatus" class="hidden"></div>
							<p class="text-xs ${monthlyDividendReorderEnabled ? "text-primary" : "text-base-content/60"}">${monthlyDividendReorderEnabled ? monthlyDividendReorderEnabledLabel : monthlyDividendReorderDisabledLabel}</p>
							<div class="selection-reveal"><div>
							<div class="rounded-2xl border border-primary/15 bg-primary/5 px-4 py-3"
								 data-monthly-selection-summary
								 data-count-template="${selectionCountTemplate}"
								 data-total-expected-monthly-dividend="${totalExpectedMonthlyDividend.toPlainString()}"
								 data-total-current-market-value="${totalCurrentMarketValue.toPlainString()}"
								 aria-live="polite">
								<div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
									<div class="flex flex-wrap items-center gap-2">
										<h3 class="text-sm font-semibold">${selectionSummaryLabel}</h3>
										<span class="badge badge-primary badge-outline" data-monthly-selection-count>${selectionSummaryLabel}</span>
									</div>
									<button type="button" class="btn btn-ghost btn-xs self-start" data-monthly-selection-clear>${selectionClearLabel}</button>
								</div>
								<div class="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
									<div class="rounded-box bg-base-100/80 px-3 py-3">
										<div class="text-xs text-base-content/60">${selectionOverviewLabel}</div>
										<div class="mt-3 space-y-2">
											<div class="flex items-center justify-between gap-3 text-sm">
												<span class="text-base-content/60">${selectionItemCountLabel}</span>
												<span class="font-mono font-semibold" data-monthly-selection-item-count>0</span>
											</div>
											<div class="flex items-center justify-between gap-3 text-sm">
												<span class="text-base-content/60">${selectionMonthlyWeightLabel}</span>
												<span class="font-mono font-semibold" data-monthly-selection-monthly-weight>0.0%</span>
											</div>
											<div class="flex items-center justify-between gap-3 text-sm">
												<span class="text-base-content/60">${selectionMarketWeightLabel}</span>
												<span class="font-mono font-semibold" data-monthly-selection-market-weight>0.0%</span>
											</div>
										</div>
									</div>
									<div class="rounded-box bg-base-100/80 px-3 py-2">
										<div class="text-xs text-base-content/60">${selectionDividendLabel}</div>
										<div class="mt-3 space-y-3">
											<div>
												<div class="text-[11px] text-base-content/60">${totalLatestMonthlyDividendLabel}</div>
												<div class="mt-1 font-mono font-semibold amount-value" data-monthly-selection-latest>₩0</div>
													<div class="mt-0.5 flex flex-wrap gap-x-2 text-[11px] text-base-content/60"><span>${payoutWindowMidMonthLabel} <span class="font-mono amount-value" data-monthly-selection-latest-mid>₩0</span></span><span>${payoutWindowMonthEndLabel} <span class="font-mono amount-value" data-monthly-selection-latest-end>₩0</span></span></div>
											</div>
											<div>
												<div class="text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.summary.total.monthly.dividend")}</div>
												<div class="mt-1 font-mono font-semibold amount-value" data-monthly-selection-monthly>₩0</div>
											</div>
											<div>
												<div class="text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.summary.total.annual.dividend")}</div>
												<div class="mt-1 font-mono font-semibold amount-value" data-monthly-selection-annual-dividend>₩0</div>
											</div>
										</div>
									</div>
									<div class="rounded-box bg-base-100/80 px-3 py-2">
										<div class="text-xs text-base-content/60">${selectionInvestmentLabel}</div>
										<div class="mt-3 space-y-3">
											<div>
												<div class="text-[11px] text-base-content/60">${totalBuyAmountLabel}</div>
												<div class="mt-1 font-mono font-semibold amount-value" data-monthly-selection-buy>₩0</div>
											</div>
											<div>
												<div class="text-[11px] text-base-content/60">${totalCurrentMarketValueLabel}</div>
												<div class="mt-1 font-mono font-semibold amount-value" data-monthly-selection-market>₩0</div>
											</div>
											<div class="border-t border-base-200/70 pt-3 space-y-2">
												<div class="flex items-center justify-between gap-3 text-sm">
													<span class="text-base-content/60">${selectionInvestmentProfitLabel}</span>
													<span class="amount-value font-mono font-semibold" data-monthly-selection-profit>₩0</span>
												</div>
												<div class="flex items-center justify-between gap-3 text-sm">
													<span class="text-base-content/60">${selectionInvestmentReturnLabel}</span>
													<span class="font-mono font-semibold" data-monthly-selection-profit-rate>0.00%</span>
												</div>
											</div>
										</div>
									</div>
									<div class="rounded-box bg-base-100/80 px-3 py-2">
										<div class="text-xs text-base-content/60">${selectionEfficiencyLabel}</div>
										<div class="mt-3 space-y-3">
											<div>
												<div class="text-[11px] text-base-content/60">${totalMonthlyTaxableBaseSummaryLabel}</div>
												<div class="mt-1 font-mono font-semibold amount-value" data-monthly-selection-taxable>₩0</div>
											</div>
											<div>
												<div class="text-[11px] text-base-content/60">${totalAnnualTaxableBaseSummaryLabel}</div>
												<div class="mt-1 font-mono font-semibold amount-value" data-monthly-selection-annual-taxable>₩0</div>
											</div>
											<div>
												<div class="text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.summary.portfolio.annual.yield")}</div>
												<div class="mt-1 font-mono font-semibold" data-monthly-selection-annual-yield>0.00%</div>
											</div>
										</div>
									</div>
								</div>
							</div>
							</div></div>
							<div class="overflow-x-auto">
								<table class="table table-pin-rows table-zebra table-sm whitespace-nowrap print-dense" data-monthly-selection-table aria-label="${MessageUtil.getMessage("stock.simulator.monthly.tool.title")}">
								<thead>
									<tr>
										<th rowspan="2" scope="col" aria-sort="${displayOrderAriaSort}" class="min-w-[8rem] max-w-[11rem] align-middle">
											<div class="flex min-h-[4.5rem] flex-col justify-center gap-1 font-medium">
												<div class="flex items-center gap-1">
													<a href="${monthlyDividendTableBaseUrl}&sort=display-order&direction=${displayOrderDirection}" class="link link-hover no-underline">
														${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.display.order")}
														@if("display-order".equals(monthlyDividendSort))
															<span class="text-xs text-base-content/60" aria-hidden="true">${currentDirectionIndicator}</span>
														@endif
													</a>
													<span>/</span>
													<a href="${monthlyDividendTableBaseUrl}&sort=symbol&direction=${symbolDirectionToggle}" class="link link-hover no-underline">
														${MessageUtil.getMessage("stock.simulator.monthly.table.header.symbol")}
														@if("symbol".equals(monthlyDividendSort))
															<span class="text-xs text-base-content/60" aria-hidden="true">${currentDirectionIndicator}</span>
														@endif
													</a>
												</div>
												<div class="text-[11px] font-medium leading-4 text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.header.name")}</div>
											</div>
										</th>
										<%-- 여러 칸을 덮는 묶음 머리글은 scope="colgroup" 이라야 그 아래 칸들과 이어진다.
										     scope="col" 은 자기 칸 하나만 가리켜, 낭독기가 "저장된 기준값" 을 첫 열에만 붙인다
										     - 실측 2026-09-12: colspan=3 머리글 셋(저장된 기준값 · 입력값 · 수익률)이 모두 col 이었다. --%>
										<th scope="colgroup" class="col-group-start col-group-label text-center text-[11px] font-medium uppercase tracking-wide text-base-content/60" colspan="3">${MessageUtil.getMessage("stock.simulator.monthly.table.group.reference")}</th>
										<th scope="colgroup" class="col-group-start col-group-label text-center text-[11px] font-medium uppercase tracking-wide text-base-content/60" colspan="3">${MessageUtil.getMessage("stock.simulator.monthly.table.group.input")}</th>
										<th rowspan="2" scope="col" class="col-group-start min-w-[7.5rem] align-middle text-right" aria-sort="${"monthly-dividend".equals(monthlyDividendSort) ? ("asc".equals(monthlyDividendDirection) ? "ascending" : "descending") : "none"}">
											<div class="flex min-h-[4.5rem] flex-col justify-center gap-1 whitespace-normal text-right">
												<a href="${monthlyDividendTableBaseUrl}&sort=monthly-dividend&direction=${monthlyDividendDirectionToggle}" class="link link-hover block text-sm font-medium no-underline">
													<span class="inline-flex items-center justify-end gap-1">
														@if("monthly-dividend".equals(monthlyDividendSort))
															<span class="text-xs text-base-content/60" aria-hidden="true">${currentDirectionIndicator}</span>
														@endif
														<span>${MessageUtil.getMessage("stock.simulator.monthly.table.header.expected.monthly.dividend")}</span>
													</span>
												</a>
												<div class="text-[11px] font-medium leading-4 text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.header.expected.taxable.base")}</div>
											</div>
										</th>
										<th scope="colgroup" class="col-group-start col-group-end col-group-label text-center text-[11px] font-medium uppercase tracking-wide text-base-content/60" colspan="3">${MessageUtil.getMessage("stock.simulator.monthly.table.group.yield")}</th>
									</tr>
									<tr>
										<th scope="col" class="col-group-start text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.monthly.dividend.per.share")}</th>
										<th scope="col" class="text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.taxable.base.amount")}</th>
										<th scope="col" class="text-right" aria-sort="${"taxable-base".equals(monthlyDividendSort) ? ("asc".equals(monthlyDividendDirection) ? "ascending" : "descending") : "none"}">
											<a href="${monthlyDividendTableBaseUrl}&sort=taxable-base&direction=${taxableBaseDirectionToggle}" class="link link-hover inline-flex items-end gap-1 font-medium no-underline">
												<span class="inline-flex flex-col items-end whitespace-normal leading-4 text-right">
													<span class="whitespace-nowrap">${MessageUtil.getMessage("stock.simulator.monthly.table.header.taxable.base.ratio.line.one")}</span>
													<span class="whitespace-nowrap">${MessageUtil.getMessage("stock.simulator.monthly.table.header.taxable.base.ratio.line.two")}</span>
												</span>
												@if("taxable-base".equals(monthlyDividendSort))
													<span class="text-xs text-base-content/60" aria-hidden="true">${currentDirectionIndicator}</span>
												@endif
											</a>
										</th>
										<th scope="col" class="col-group-start text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.current.price")}</th>
										<th scope="col" class="min-w-[6.5rem] align-bottom text-right">
											<div class="space-y-1 whitespace-normal text-right">
												<div>${MessageUtil.getMessage("stock.simulator.monthly.table.header.quantity")}</div>
												<div class="text-[11px] font-medium leading-4 text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.header.average.buy.price")}</div>
											</div>
										</th>
										<th scope="col" class="min-w-[6.5rem] align-bottom text-right">
											<div class="space-y-1 whitespace-normal text-right">
												<div>${MessageUtil.getMessage("stock.simulator.monthly.table.header.buy.amount")}</div>
												<div class="text-[11px] font-medium leading-4 text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.header.market.value")}</div>
											</div>
										</th>
										<th scope="col" aria-sort="${marketYieldAriaSort}" class="col-group-start min-w-[6.5rem] align-bottom text-right">
											<div class="space-y-1 whitespace-normal text-right">
												<div class="text-[11px] font-medium leading-4 text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.header.market.price.basis")}</div>
												<a href="${monthlyDividendTableBaseUrl}&sort=monthly-yield&direction=${monthlyYieldDirectionToggle}" class="link link-hover block py-1 text-xs font-medium leading-4 no-underline">
													${MessageUtil.getMessage("stock.simulator.monthly.table.header.monthly.yield.short")}<span class="sr-only"> ${marketBasisLabel}</span>
													@if("monthly-yield".equals(monthlyDividendSort))
														<span class="ml-1 text-[11px] text-base-content/60" aria-hidden="true">${currentDirectionIndicator}</span>
													@endif
												</a>
												<a href="${monthlyDividendTableBaseUrl}&sort=annual-yield&direction=${annualYieldDirectionToggle}" class="link link-hover block py-1 text-xs font-medium leading-4 no-underline">
													${MessageUtil.getMessage("stock.simulator.monthly.table.header.annual.yield.short")}<span class="sr-only"> ${marketBasisLabel}</span>
													@if("annual-yield".equals(monthlyDividendSort))
														<span class="ml-1 text-[11px] text-base-content/60" aria-hidden="true">${currentDirectionIndicator}</span>
													@endif
												</a>
											</div>
										</th>
										<th scope="col" aria-sort="${onCostYieldAriaSort}" class="min-w-[6.5rem] align-bottom text-right">
											<div class="space-y-1 whitespace-normal text-right">
												<div class="text-[11px] font-medium leading-4 text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.header.on.cost")}</div>
												<a href="${monthlyDividendTableBaseUrl}&sort=monthly-yield-on-cost&direction=${monthlyYieldOnCostDirectionToggle}" class="link link-hover block py-1 text-xs font-medium leading-4 no-underline">
													${MessageUtil.getMessage("stock.simulator.monthly.table.header.monthly.yield.short")}<span class="sr-only"> ${onCostBasisLabel}</span>
													@if("monthly-yield-on-cost".equals(monthlyDividendSort))
														<span class="ml-1 text-[11px] text-base-content/60" aria-hidden="true">${currentDirectionIndicator}</span>
													@endif
												</a>
												<a href="${monthlyDividendTableBaseUrl}&sort=annual-yield-on-cost&direction=${annualYieldOnCostDirectionToggle}" class="link link-hover block py-1 text-xs font-medium leading-4 no-underline">
													${MessageUtil.getMessage("stock.simulator.monthly.table.header.annual.yield.short")}<span class="sr-only"> ${onCostBasisLabel}</span>
													@if("annual-yield-on-cost".equals(monthlyDividendSort))
														<span class="ml-1 text-[11px] text-base-content/60" aria-hidden="true">${currentDirectionIndicator}</span>
													@endif
												</a>
											</div>
										</th>
										<th scope="col" aria-sort="${combinedReturnAriaSort}" class="col-group-end min-w-[6rem] align-bottom text-right">
											<div class="space-y-1 whitespace-normal text-right">
												<%-- This column carries two different numbers, so both need names. Until 2026-09-12 the first
												 line said only "on cost", which is the same phrase the previous column uses as a basis - so the
												 price return had no name at all and the column read as one metric with two numbers. --%>
												<div class="text-[11px] font-medium leading-4 text-base-content/60">${onCostBasisLabel}</div>
												<div class="whitespace-nowrap">${MessageUtil.getMessage("stock.simulator.monthly.table.header.total.return.on.cost.line.one")}</div>
												<a href="${monthlyDividendTableBaseUrl}&sort=combined-return&direction=${combinedReturnDirectionToggle}" class="link link-hover inline-flex items-center justify-end gap-1 font-medium no-underline">
													<span class="whitespace-nowrap">${MessageUtil.getMessage("stock.simulator.monthly.table.header.expected.combined.return.line.one")}</span>
													@if("combined-return".equals(monthlyDividendSort))
														<span class="text-xs text-base-content/60" aria-hidden="true">${currentDirectionIndicator}</span>
													@endif
												</a>
											</div>
										</th>
									</tr>
								</thead>
								<tbody data-profile-order-list="true" data-profile-order-status-id="monthlyDividendSimulatorOrderStatus" data-profile-order-endpoint="${monthlyDividendOrderEndpoint}" data-profile-order-row-selector="tr[data-profile-order-row='true']" data-profile-order-handle-selector="[data-profile-order-handle]" data-profile-order-all-symbols="${String.join(",", monthlyDividendProfileOrderedSymbols)}" data-profile-reorder-enabled="${monthlyDividendReorderEnabled}" data-success-message="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.reordered.profile")}" data-fallback-error-message="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.reordered.profile.failed")}">
									@for(MonthlyDividendSnapshotResponse row : monthlyDividendRows)
											!{BigDecimal latestMonthlyDividendAmount = (row.latestMonthlyDividendPerShare() != null ? row.latestMonthlyDividendPerShare() : BigDecimal.ZERO).multiply(BigDecimal.valueOf(row.heldQuantity() != null ? row.heldQuantity().longValue() : 0L));}
											!{BigDecimal buyAmount = (row.averageBuyPrice() != null ? row.averageBuyPrice() : BigDecimal.ZERO).multiply(BigDecimal.valueOf(row.heldQuantity() != null ? row.heldQuantity().longValue() : 0L));}
											!{BigDecimal marketValue = row.currentMarketValue() != null ? row.currentMarketValue() : BigDecimal.ZERO;}
											!{BigDecimal latestMonthlyYieldPct = marketValue.signum() > 0 ? latestMonthlyDividendAmount.multiply(BigDecimal.valueOf(100)).divide(marketValue, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;}
											!{BigDecimal latestAnnualYieldPct = latestMonthlyYieldPct.multiply(BigDecimal.valueOf(12));}
											!{BigDecimal latestMonthlyYieldOnCostPct = buyAmount.signum() > 0 ? latestMonthlyDividendAmount.multiply(BigDecimal.valueOf(100)).divide(buyAmount, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;}
											!{BigDecimal latestAnnualYieldOnCostPct = latestMonthlyYieldOnCostPct.multiply(BigDecimal.valueOf(12));}
											!{String normalizedRowSymbol = row.stockItemSymbol() != null ? row.stockItemSymbol().trim().toUpperCase(java.util.Locale.ROOT) : "";}
											!{Integer rowDisplayOrder = monthlyDividendProfileDisplayOrders != null ? monthlyDividendProfileDisplayOrders.get(normalizedRowSymbol) : null;}
											!{boolean rowReorderable = monthlyDividendReorderEnabled && rowDisplayOrder != null;}
											!{BigDecimal taxableBaseRatio = row.averageTaxableBaseRatio1y() != null ? row.averageTaxableBaseRatio1y() : BigDecimal.ZERO;}
											!{BigDecimal latestPerShareTaxableBase = (row.latestMonthlyDividendPerShare() != null ? row.latestMonthlyDividendPerShare() : BigDecimal.ZERO).multiply(taxableBaseRatio).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);}
											!{BigDecimal averagePerShareTaxableBase = (row.averageMonthlyDividendPerShare1y() != null ? row.averageMonthlyDividendPerShare1y() : BigDecimal.ZERO).multiply(taxableBaseRatio).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);}
											<tr class="monthly-dividend-row${rowReorderable ? " cursor-move" : ""}"
												data-monthly-selection-row
												data-profile-order-row="${rowDisplayOrder != null ? "true" : "false"}"
												data-symbol="${normalizedRowSymbol}"
												aria-selected="false"
												data-row-select="${MessageUtil.getMessage("stock.table.row.select")}"
												draggable="${rowReorderable}"
												data-latest-monthly-dividend="${latestMonthlyDividendAmount.toPlainString()}"
												data-buy-amount="${buyAmount.toPlainString()}"
												data-expected-monthly-dividend="${(row.expectedMonthlyDividend() != null ? row.expectedMonthlyDividend() : BigDecimal.ZERO).toPlainString()}"
												data-expected-taxable-base="${(row.expectedTaxableBaseAmount() != null ? row.expectedTaxableBaseAmount() : BigDecimal.ZERO).toPlainString()}"
												data-current-market-value="${(row.currentMarketValue() != null ? row.currentMarketValue() : BigDecimal.ZERO).toPlainString()}"
													data-payout-window="${monthlyDividendPayoutWindowBySymbol.getOrDefault(normalizedRowSymbol, "")}">
												<td class="align-top font-medium">
													<div class="min-w-0">
														<div class="flex items-center gap-2 whitespace-nowrap">
															<span class="inline-flex items-center gap-1 text-xs text-base-content/60">
															@if(rowReorderable)
																<span class="font-mono select-none" data-profile-order-handle>::</span>
															@else
																<span class="font-mono select-none opacity-40">--</span>
															@endif
															<span data-profile-order-value>${rowDisplayOrder != null ? String.valueOf(rowDisplayOrder) : "-"}</span>
														</span>
															<span>${row.stockItemSymbol()}</span>
															!{Integer comparisonRank = row.id() != null ? comparisonRanks.get(row.id()) : null;}
															@if(comparisonRank != null)
																<span class="badge badge-xs ${comparisonRank == 1 ? "badge-success" : "badge-ghost"}" data-comparison-rank="${comparisonRank}">${java.text.MessageFormat.format(comparisonRankPattern, String.valueOf(comparisonRank))}</span>
															@endif
														</div>
														<%-- 다른 표 17개(476행)는 모두 종목명을 상세로 잇는데 이 표만 링크가 없었다(실측 2026-09-11). --%>
																					<div class="mt-1 text-sm font-normal leading-tight text-base-content/70">@if(row.stockItemId() != null)<a class="link link-hover" href="${StockDetailLinkUtil.item(row.stockItemId().toString())}">${row.stockItemName()}</a>@else${row.stockItemName()}@endif</div>
												<%-- 행마다 기준일이 다르다. 위 요약은 "기준일 2026-08-19 ~ 2026-09-02" 처럼 범위로만 적는데,
												     그 범위는 갱신이 밀린 것과 지급 시기가 다른 것을 구분해 주지 못한다 - 실측 2026-09-11:
												     월중 지급 4 종목이 08-19, 월말 지급 4 종목이 09-02 로 둘 다 자기 최신 지급일이었다(지연 아님).
												     행에 적어 두면 어느 종목이 어느 기준일인지 바로 보인다. --%>
												@if(row.asOfDate() != null)
													<div class="mt-0.5 text-[11px] font-normal leading-tight text-base-content/60" data-row-as-of>${MessageUtil.getMessage("stock.simulator.monthly.summary.as.of.date")} <span class="font-mono">${row.asOfDate().toString()}</span></div>
												@endif
													</div>
												</td>
											<td class="align-top text-right">
												<div class="space-y-1 tabular-nums">
													<div>
														<span class="mr-1 text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.latest.short")}</span>
														<span class="amount-value">₩${amountFormat.format(row.latestMonthlyDividendPerShare() != null ? row.latestMonthlyDividendPerShare() : BigDecimal.ZERO)}</span>
													</div>
													<div class="text-sm text-base-content/80">
														<span class="mr-1 text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.average.short")}</span>
														<span class="amount-value">₩${amountFormat.format(row.averageMonthlyDividendPerShare1y() != null ? row.averageMonthlyDividendPerShare1y() : BigDecimal.ZERO)}</span>
													</div>
												</div>
											</td>
											<td class="align-top text-right">
												<div class="space-y-1 tabular-nums">
													<div>
														<span class="mr-1 text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.latest.short")}</span>
														<span class="amount-value">₩${amountFormat.format(latestPerShareTaxableBase)}</span>
													</div>
													<div class="text-sm text-base-content/80">
														<span class="mr-1 text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.average.short")}</span>
														<span class="amount-value">₩${amountFormat.format(averagePerShareTaxableBase)}</span>
													</div>
												</div>
											</td>
											<td class="text-right">
												<div class="whitespace-nowrap tabular-nums">${percentFormat.format(row.averageTaxableBaseRatio1y() != null ? row.averageTaxableBaseRatio1y() : BigDecimal.ZERO)}%</div>
												<%-- 이 비중은 원장 최근 1 년의 (과세금액 / 세전)을 모든 계좌 합쳐 낸 값이다. 과세이연 계좌(ISA · 연금저축)는
												     과세금액이 0 으로 적혀 비중을 끌어내린다 - 실측 2026-09-17: 과세이연 계좌에서만 받은 두 종목이 0%,
												     네 계좌 중 과세 계좌 한 곳만 100% 인 종목이 13.42% 였고, 사용자는 "0% 인데 실제는 100%" 로 읽었다.
												     계산은 두고(사용자 결정) 그 나눔을 적는다. --%>
												!{var taxableRatioBasis = row.stockItemId() != null ? monthlyDividendTaxableRatioBasis.get(row.stockItemId()) : null;}
												@if(taxableRatioBasis != null && taxableRatioBasis.deferredOnly())
													<div class="text-[11px] text-base-content/60 whitespace-nowrap" data-taxable-ratio-basis="deferred-only">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.taxable.ratio.deferred.only")}</div>
												@elseif(taxableRatioBasis != null && taxableRatioBasis.mixed())
													<div class="text-[11px] text-base-content/60 whitespace-nowrap tabular-nums" data-taxable-ratio-basis="taxable-accounts">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.simulator.monthly.table.cell.taxable.ratio.taxable.accounts"), percentFormat.format(taxableRatioBasis.taxableAccountRatioPct()))}</div>
												@endif
												<%-- 운용사 지급 이력 기준 비중이 1%p 이상 다르면 함께 적는다. 예전엔 '저장값이 낡았다' 고 보고 경고색과
												     "저장값을 갱신합니다" 링크를 달았지만, 지급 이력을 가져올 때마다 비중을 원장 실적으로 다시 덮으므로
												     갱신해도 그대로다(막다른 안내였다) - 차이는 낡은 게 아니라 기준이 다른 것이다. --%>
												!{BigDecimal referenceTaxableRatio = monthlyDividendReferenceTaxableRatios.get(row.stockItemId());}
												@if(referenceTaxableRatio != null && referenceTaxableRatio.subtract(row.averageTaxableBaseRatio1y() != null ? row.averageTaxableBaseRatio1y() : BigDecimal.ZERO).abs().compareTo(new BigDecimal("1")) >= 0)
												<%-- 링크는 그 종목의 지급 이력을 보러 가는 길이다(실측 2026-09-11: 이 표의 행에는 링크가 하나도 없었다). --%>
												<div class="text-[11px] text-base-content/60 whitespace-nowrap tabular-nums" data-reference-taxable-ratio="${referenceTaxableRatio.toPlainString()}">
													<a class="link link-hover" href="/stock/admin?tab=monthly-reference&amp;symbol=${java.net.URLEncoder.encode(row.stockItemSymbol() != null ? row.stockItemSymbol() : "", java.nio.charset.StandardCharsets.UTF_8)}"
													   title="${MessageUtil.getMessage("stock.simulator.monthly.table.cell.taxable.ratio.reference.link")}">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.simulator.monthly.table.cell.taxable.ratio.reference"), percentFormat.format(referenceTaxableRatio))}<span class="sr-only">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.taxable.ratio.reference.link")}</span></a>
												</div>
												@endif
											</td>
											<td class="text-right"><span class="amount-value">₩${amountFormat.format(row.currentPrice() != null ? row.currentPrice() : BigDecimal.ZERO)}</span></td>
											<td class="align-top text-right">
												<div class="space-y-1 whitespace-nowrap tabular-nums">
													<div>
														<span class="mr-1 text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.quantity.short")}</span>
														<span class="amount-value">${amountFormat.format(row.heldQuantity() != null ? row.heldQuantity() : 0)}</span>
														!{Integer currentHeldQuantity = monthlyDividendCurrentQuantities.get(row.stockItemId());}
														@if(currentHeldQuantity != null && row.heldQuantity() != null && currentHeldQuantity.intValue() != row.heldQuantity().intValue())
														<span class="ml-1 text-[11px] text-warning amount-value">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.simulator.monthly.table.cell.quantity.current"), amountFormat.format(currentHeldQuantity))}</span>
														@endif
													</div>
													<div class="text-sm text-base-content/80">
														<span class="mr-1 text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.average.buy.price.short")}</span>
														<span class="amount-value">₩${amountFormat.format(row.averageBuyPrice() != null ? row.averageBuyPrice() : BigDecimal.ZERO)}</span>
														!{BigDecimal currentAverageBuyPrice = monthlyDividendCurrentAverageBuyPrices.get(row.stockItemId());}
														@if(currentAverageBuyPrice != null && row.averageBuyPrice() != null && currentAverageBuyPrice.setScale(0, java.math.RoundingMode.HALF_UP).compareTo(row.averageBuyPrice().setScale(0, java.math.RoundingMode.HALF_UP)) != 0)
														<span class="ml-1 text-[11px] text-warning amount-value">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.simulator.monthly.table.cell.average.buy.price.current"), amountFormat.format(currentAverageBuyPrice))}</span>
														@endif
													</div>
												</div>
											</td>
											<td class="align-top text-right">
												<div class="space-y-1 whitespace-nowrap tabular-nums">
													<div>
														<span class="mr-1 text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.buy.amount.short")}</span>
														<span class="amount-value">₩${amountFormat.format(buyAmount)}</span>
													</div>
													<div class="text-sm text-base-content/80">
														<span class="mr-1 text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.market.value.short")}</span>
														<span class="amount-value">₩${amountFormat.format(marketValue)}</span>
													</div>
												</div>
											</td>
											<td class="align-top text-right">
												<div class="space-y-1 tabular-nums">
													<div class="font-medium">
														<span class="mr-1 text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.average.short")}</span>
														<span class="amount-value">₩${amountFormat.format(row.expectedMonthlyDividend() != null ? row.expectedMonthlyDividend() : BigDecimal.ZERO)}</span>
													</div>
													<div class="text-sm text-base-content/80">
														<span class="mr-1 text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.latest.short")}</span>
														<span class="amount-value">₩${amountFormat.format(latestMonthlyDividendAmount)}</span>
													</div>
													<div class="text-sm text-base-content/60">
														<span class="mr-1 text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.expected.taxable.base.short")}</span>
														<span class="amount-value">₩${amountFormat.format(row.expectedTaxableBaseAmount() != null ? row.expectedTaxableBaseAmount() : BigDecimal.ZERO)}</span>
													</div>
												</div>
											</td>
											<td class="align-top text-right">
												<div class="space-y-1 whitespace-nowrap tabular-nums">
													<div>
														<span class="mr-1 text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.average.short")}</span>
														<span>${percentFormat.format(row.expectedMonthlyYieldPct() != null ? row.expectedMonthlyYieldPct() : BigDecimal.ZERO)}% · ${percentFormat.format(row.expectedAnnualYieldPct() != null ? row.expectedAnnualYieldPct() : BigDecimal.ZERO)}%</span>
													</div>
													<div class="text-sm text-base-content/80">
														<span class="mr-1 text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.latest.short")}</span>
														<span>${percentFormat.format(latestMonthlyYieldPct)}% · ${percentFormat.format(latestAnnualYieldPct)}%</span>
													</div>
												</div>
											</td>
											<td class="align-top text-right">
												<div class="space-y-1 whitespace-nowrap tabular-nums">
													<div>
														<span class="mr-1 text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.average.short")}</span>
														<span>${percentFormat.format(row.expectedMonthlyYieldOnCostPct() != null ? row.expectedMonthlyYieldOnCostPct() : BigDecimal.ZERO)}% · ${percentFormat.format(row.expectedAnnualYieldOnCostPct() != null ? row.expectedAnnualYieldOnCostPct() : BigDecimal.ZERO)}%</span>
													</div>
													<div class="text-sm text-base-content/80">
														<span class="mr-1 text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.latest.short")}</span>
														<span>${percentFormat.format(latestMonthlyYieldOnCostPct)}% · ${percentFormat.format(latestAnnualYieldOnCostPct)}%</span>
													</div>
													<%-- 평단 기준이 현재가 기준보다 높은지 낮은지(사용자 요청 2026-09-17, 고른 방식: 차이 표시). 두 수익률은 같은 배당을
													     평단 원가와 현재 평가액으로 나눈 것이라 방향은 '현재가가 평단보다 위인가' 하나로 정해지고 평균 · 최근 두 줄이 늘 같다 -
													     그래서 한 번만, 연 평균 기준 차이(%p)로 적는다. 색만으로 말하지 않는다(기호 + 부호 + 낭독 문장). --%>
													!{BigDecimal onCostGap = row.expectedAnnualYieldOnCostPct() != null && row.expectedAnnualYieldPct() != null
														? row.expectedAnnualYieldOnCostPct().subtract(row.expectedAnnualYieldPct()).setScale(2, RoundingMode.HALF_UP)
														: null;}
													@if(onCostGap != null)
														!{String onCostGapAmount = percentFormat.format(onCostGap.abs());}
														!{String onCostGapKind = onCostGap.signum() > 0 ? "higher" : (onCostGap.signum() < 0 ? "lower" : "same");}
														!{String onCostGapNote = onCostGap.signum() == 0
															? MessageUtil.getMessage("stock.simulator.monthly.table.cell.on.cost.gap.same")
															: java.text.MessageFormat.format(onCostGap.signum() > 0
																? MessageUtil.getMessage("stock.simulator.monthly.table.cell.on.cost.gap.higher")
																: MessageUtil.getMessage("stock.simulator.monthly.table.cell.on.cost.gap.lower"), onCostGapAmount);}
														!{String onCostGapValue = java.text.MessageFormat.format(
															MessageUtil.getMessage("stock.simulator.monthly.table.cell.on.cost.gap.value"),
															(onCostGap.signum() > 0 ? "+" : (onCostGap.signum() < 0 ? "-" : "")) + onCostGapAmount);}
														<div class="text-[11px] font-medium ${onCostGap.signum() > 0 ? "text-profit" : (onCostGap.signum() < 0 ? "text-loss" : "text-base-content/60")}" data-on-cost-gap="${onCostGapKind}" data-on-cost-gap-pct="${onCostGap.toPlainString()}" title="${onCostGapNote}"><span aria-hidden="true">${onCostGap.signum() > 0 ? "▲ " : (onCostGap.signum() < 0 ? "▼ " : "")}${onCostGapValue}</span><span class="sr-only">${onCostGapNote}</span></div>
													@endif
												</div>
											</td>
											<td class="align-top text-right">
												<div class="space-y-1 tabular-nums">
													<%-- 손익 방향은 부호로도 말한다 - 실측 2026-09-12(고대비 모드): 이 열의 색이 모두 검정 한 가지로
														 합쳐지면 "10.69%" 가 이득인지 손실인지 알 수 없다. 음수는 이미 "-" 를 달고 있었고(16 값 중 7),
														 양수 9 개만 맨 숫자였다. 칸 폭 98px 에 "-13.92%" 가 이미 들어가므로 폭은 그대로다. --%>
													<div class="${row.totalReturnOnCostPct() != null && row.totalReturnOnCostPct().signum() >= 0 ? "text-profit" : "text-loss"}">${StockFormatUtil.signedPct(row.totalReturnOnCostPct() != null ? row.totalReturnOnCostPct().doubleValue() : 0.0d, 2)}</div>
													<div class="font-semibold ${row.expectedCombinedReturnPct() != null && row.expectedCombinedReturnPct().signum() >= 0 ? "text-profit" : "text-loss"}">${StockFormatUtil.signedPct(row.expectedCombinedReturnPct() != null ? row.expectedCombinedReturnPct().doubleValue() : 0.0d, 2)}</div>
												</div>
											</td>
										</tr>
									@endfor
								</tbody>
								</table>
							</div>
						</div>
					@endif
			`)

			@template._components.ui.card(bodyClass = "gap-4", body = @`
					<h2 class="card-title text-lg">${MessageUtil.getMessage("stock.simulator.monthly.section.criteria.title")}</h2>
					<ul class="list-disc space-y-3 pl-5 text-sm leading-6 text-base-content/70">
						<li>${MessageUtil.getMessage("stock.simulator.monthly.criteria.one")}</li>
						<li>${MessageUtil.getMessage("stock.simulator.monthly.criteria.two")}</li>
						<li>${MessageUtil.getMessage("stock.simulator.monthly.criteria.three")}</li>
						<li>${MessageUtil.getMessage("stock.simulator.monthly.criteria.four")}</li>
						<li>${MessageUtil.getMessage("stock.simulator.monthly.criteria.five")}</li>
						<li>${MessageUtil.getMessage("stock.simulator.monthly.criteria.six")}</li>
					</ul>
			`)
		</div>
	</div>
</div>

<style>
	.monthly-dividend-row {
		cursor: pointer;
	}

	.monthly-dividend-row.monthly-dividend-row-selected > td {
		background-color: color-mix(in oklch, var(--color-primary) 10%, var(--color-base-100));
	}

	.monthly-dividend-row.monthly-dividend-row-selected > td:first-child {
		box-shadow: inset 3px 0 0 color-mix(in oklch, var(--color-primary) 65%, transparent);
	}

	/* 행은 더 이상 초점을 받지 않는다(선택은 첫 칸 체크박스). 체크박스에 초점이 오면 행 전체를 표시한다. */
	.monthly-dividend-row:has(input:focus-visible) {
		outline: 2px solid color-mix(in oklch, var(--color-primary) 35%, transparent);
		outline-offset: -2px;
	}

	/* 상단 그룹(기준 데이터/입력값/수익률)이 어느 열에 대한 것인지 세로 구분선으로 명확히 한다.
	   구분선은 헤더(제목) 영역까지만 두고 본문은 zebra 줄무늬만 유지한다. 그룹 라벨 셀만 옅게 강조. */
	[data-monthly-selection-table] thead .col-group-start {
		border-left: 1px solid color-mix(in oklch, var(--color-base-content) 16%, transparent);
	}
	[data-monthly-selection-table] thead .col-group-end {
		border-right: 1px solid color-mix(in oklch, var(--color-base-content) 16%, transparent);
	}
	[data-monthly-selection-table] thead .col-group-label {
		background-color: color-mix(in oklch, var(--color-base-content) 6%, transparent);
	}
</style>

<%-- 행 선택 합계는 /js/stock/monthlyDividendSimulator.js 에 있다(2026-09-08 까지 여기 240 줄 인라인 스크립트). --%>
@template._components.ui.submitOverlay()
<script type="module" src="/js/stock/monthlyDividendSimulator.js"></script>