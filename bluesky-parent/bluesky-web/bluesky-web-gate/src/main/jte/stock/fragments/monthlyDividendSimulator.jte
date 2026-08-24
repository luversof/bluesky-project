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

@param List<MonthlyDividendSnapshotResponse> monthlyDividendRows = java.util.List.of()
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
	// 기준 데이터의 '언제 시점' 값인지. asOfDate 는 그 종목 지급 이력의 최신 지급일과 같다.
	// 이 값이 오래되면 최근 월 배당금 같은 대표 수치가 조용히 옛 분배금으로 계산된다
	// (실측: 월중배당 4 종목이 30 일 뒤처져 총 최근 월 배당금이 46,635 원 과대).
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
	String monthlyDividendTableBaseUrl = "/stock/simulator?tab=monthly-dividend";
	String currentDirectionIndicator = "asc".equals(monthlyDividendDirection) ? "↑" : "↓";
	String displayOrderDirection = "display-order".equals(monthlyDividendSort) ? ("asc".equals(monthlyDividendDirection) ? "desc" : "asc") : "asc";
	String symbolDirectionToggle = "symbol".equals(monthlyDividendSort) ? ("asc".equals(monthlyDividendDirection) ? "desc" : "asc") : "asc";
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
		<div class="alert alert-success border border-success/20 bg-success/10 text-success-content">
			<span>${successMessage}</span>
		</div>
	@endif
	@if(monthlyDividendErrorMessage != null && !monthlyDividendErrorMessage.isEmpty())
		<div class="alert alert-error border border-error/20 bg-error/10 text-error-content">
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
						<div class="flex flex-wrap items-center gap-2"><form method="post" action="/stock/simulator/monthly-dividend/import-sheet" class="inline"><input type="hidden" name="sort" value="${monthlyDividendSort}" /><input type="hidden" name="direction" value="${monthlyDividendDirection}" /><button type="submit" class="btn btn-secondary btn-sm h-auto max-w-full whitespace-normal">${MessageUtil.getMessage("stock.simulator.monthly.import.sheet")}</button></form><a href="${monthlyDividendReferenceManageUrl}" class="btn btn-outline btn-sm h-auto max-w-full whitespace-normal">${MessageUtil.getMessage("stock.simulator.monthly.reference.manage")}</a></div>
					</div>
					@if(symbolValue != null && !symbolValue.isEmpty() && !referenceValuesAvailable)
						<div class="alert border border-warning/20 bg-warning/10 text-warning-content">
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
						</label>
						<label class="form-control">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.simulator.monthly.form.field.average.buy.price")}</span></div>
							<input name="averageBuyPrice" type="number" min="0" step="0.01" value="${averageBuyPriceValue}" class="input input-bordered w-full" required />
						</label>
						<div class="flex items-end">
							<button type="submit" class="btn btn-primary w-full">${MessageUtil.getMessage("stock.simulator.monthly.form.submit")}</button>
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
							<span class="text-right font-semibold">${bestChoice != null ? bestChoice.stockItemSymbol() : MessageUtil.getMessage("stock.simulator.monthly.summary.no.best.choice")}</span>
						</div>
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
							<h2 class="card-title text-lg">${MessageUtil.getMessage("stock.simulator.monthly.section.table.title")}</h2>
							<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.section.table.desc")}</p>
						</div>
					</div>

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
							<p class="text-xs ${monthlyDividendReorderEnabled ? "text-primary/70" : "text-base-content/60"}">${monthlyDividendReorderEnabled ? monthlyDividendReorderEnabledLabel : monthlyDividendReorderDisabledLabel}</p>
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
								<table class="table table-pin-rows table-zebra table-sm whitespace-nowrap" data-monthly-selection-table>
								<thead>
									<tr>
										<th rowspan="2" scope="col" class="min-w-[8rem] max-w-[11rem] align-middle">
											<div class="flex min-h-[4.5rem] flex-col justify-center gap-1 font-medium">
												<div class="flex items-center gap-1">
													<a href="${monthlyDividendTableBaseUrl}&sort=display-order&direction=${displayOrderDirection}" class="link link-hover no-underline">
														${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.display.order")}
														@if("display-order".equals(monthlyDividendSort))
															<span class="text-xs text-base-content/60">${currentDirectionIndicator}</span>
														@endif
													</a>
													<span>/</span>
													<a href="${monthlyDividendTableBaseUrl}&sort=symbol&direction=${symbolDirectionToggle}" class="link link-hover no-underline">
														${MessageUtil.getMessage("stock.simulator.monthly.table.header.symbol")}
														@if("symbol".equals(monthlyDividendSort))
															<span class="text-xs text-base-content/60">${currentDirectionIndicator}</span>
														@endif
													</a>
												</div>
												<div class="text-[11px] font-medium leading-4 text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.header.name")}</div>
											</div>
										</th>
										<th scope="col" class="col-group-start col-group-label text-center text-[11px] font-medium uppercase tracking-wide text-base-content/60" colspan="3">${MessageUtil.getMessage("stock.simulator.monthly.table.group.reference")}</th>
										<th scope="col" class="col-group-start col-group-label text-center text-[11px] font-medium uppercase tracking-wide text-base-content/60" colspan="3">${MessageUtil.getMessage("stock.simulator.monthly.table.group.input")}</th>
										<th rowspan="2" scope="col" class="col-group-start min-w-[7.5rem] align-middle text-right">
											<div class="flex min-h-[4.5rem] flex-col justify-center gap-1 whitespace-normal text-right">
												<a href="${monthlyDividendTableBaseUrl}&sort=monthly-dividend&direction=${monthlyDividendDirectionToggle}" class="link link-hover block text-sm font-medium no-underline">
													<span class="inline-flex items-center justify-end gap-1">
														@if("monthly-dividend".equals(monthlyDividendSort))
															<span class="text-xs text-base-content/60">${currentDirectionIndicator}</span>
														@endif
														<span>${MessageUtil.getMessage("stock.simulator.monthly.table.header.expected.monthly.dividend")}</span>
													</span>
												</a>
												<div class="text-[11px] font-medium leading-4 text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.header.expected.taxable.base")}</div>
											</div>
										</th>
										<th scope="col" class="col-group-start col-group-end col-group-label text-center text-[11px] font-medium uppercase tracking-wide text-base-content/60" colspan="3">${MessageUtil.getMessage("stock.simulator.monthly.table.group.yield")}</th>
									</tr>
									<tr>
										<th scope="col" class="col-group-start text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.monthly.dividend.per.share")}</th>
										<th scope="col" class="text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.taxable.base.amount")}</th>
										<th scope="col" class="text-right">
											<a href="${monthlyDividendTableBaseUrl}&sort=taxable-base&direction=${taxableBaseDirectionToggle}" class="link link-hover inline-flex items-end gap-1 font-medium no-underline">
												<span class="inline-flex flex-col items-end whitespace-normal leading-4 text-right">
													<span class="whitespace-nowrap">${MessageUtil.getMessage("stock.simulator.monthly.table.header.taxable.base.ratio.line.one")}</span>
													<span class="whitespace-nowrap">${MessageUtil.getMessage("stock.simulator.monthly.table.header.taxable.base.ratio.line.two")}</span>
												</span>
												@if("taxable-base".equals(monthlyDividendSort))
													<span class="text-xs text-base-content/60">${currentDirectionIndicator}</span>
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
										<th scope="col" class="col-group-start min-w-[6.5rem] align-bottom text-right">
											<div class="space-y-1 whitespace-normal text-right">
												<div class="text-[11px] font-medium leading-4 text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.header.market.price.basis")}</div>
												<a href="${monthlyDividendTableBaseUrl}&sort=monthly-yield&direction=${monthlyYieldDirectionToggle}" class="link link-hover block text-xs font-medium leading-4 no-underline">
													${MessageUtil.getMessage("stock.simulator.monthly.table.header.monthly.yield.short")}
													@if("monthly-yield".equals(monthlyDividendSort))
														<span class="ml-1 text-[11px] text-base-content/60">${currentDirectionIndicator}</span>
													@endif
												</a>
												<a href="${monthlyDividendTableBaseUrl}&sort=annual-yield&direction=${annualYieldDirectionToggle}" class="link link-hover block text-xs font-medium leading-4 no-underline">
													${MessageUtil.getMessage("stock.simulator.monthly.table.header.annual.yield.short")}
													@if("annual-yield".equals(monthlyDividendSort))
														<span class="ml-1 text-[11px] text-base-content/60">${currentDirectionIndicator}</span>
													@endif
												</a>
											</div>
										</th>
										<th scope="col" class="min-w-[6.5rem] align-bottom text-right">
											<div class="space-y-1 whitespace-normal text-right">
												<div class="text-[11px] font-medium leading-4 text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.header.on.cost")}</div>
												<a href="${monthlyDividendTableBaseUrl}&sort=monthly-yield-on-cost&direction=${monthlyYieldOnCostDirectionToggle}" class="link link-hover block text-xs font-medium leading-4 no-underline">
													${MessageUtil.getMessage("stock.simulator.monthly.table.header.monthly.yield.short")}
													@if("monthly-yield-on-cost".equals(monthlyDividendSort))
														<span class="ml-1 text-[11px] text-base-content/60">${currentDirectionIndicator}</span>
													@endif
												</a>
												<a href="${monthlyDividendTableBaseUrl}&sort=annual-yield-on-cost&direction=${annualYieldOnCostDirectionToggle}" class="link link-hover block text-xs font-medium leading-4 no-underline">
													${MessageUtil.getMessage("stock.simulator.monthly.table.header.annual.yield.short")}
													@if("annual-yield-on-cost".equals(monthlyDividendSort))
														<span class="ml-1 text-[11px] text-base-content/60">${currentDirectionIndicator}</span>
													@endif
												</a>
											</div>
										</th>
										<th scope="col" class="col-group-end min-w-[6rem] align-bottom text-right">
											<div class="space-y-1 whitespace-normal text-right">
												<div class="whitespace-nowrap">${MessageUtil.getMessage("stock.simulator.monthly.table.header.total.return.on.cost.line.one")}</div>
												<a href="${monthlyDividendTableBaseUrl}&sort=combined-return&direction=${combinedReturnDirectionToggle}" class="link link-hover inline-flex items-center justify-end gap-1 font-medium no-underline">
													<span class="whitespace-nowrap">${MessageUtil.getMessage("stock.simulator.monthly.table.header.expected.combined.return.line.one")}</span>
													@if("combined-return".equals(monthlyDividendSort))
														<span class="text-xs text-base-content/60">${currentDirectionIndicator}</span>
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
												tabindex="0"
												role="button"
												aria-pressed="false"
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
															@if(bestChoice != null && bestChoice.id() != null && bestChoice.id().equals(row.id()))
																<span class="badge badge-success badge-xs">${MessageUtil.getMessage("stock.simulator.compare.best.choice")}</span>
															@endif
														</div>
														<div class="mt-1 text-sm font-normal leading-tight text-base-content/70">${row.stockItemName()}</div>
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
											<td class="text-right">${percentFormat.format(row.averageTaxableBaseRatio1y() != null ? row.averageTaxableBaseRatio1y() : BigDecimal.ZERO)}%</td>
											<td class="text-right"><span class="amount-value">₩${amountFormat.format(row.currentPrice() != null ? row.currentPrice() : BigDecimal.ZERO)}</span></td>
											<td class="align-top text-right">
												<div class="space-y-1 whitespace-nowrap tabular-nums">
													<div>
														<span class="mr-1 text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.quantity.short")}</span>
														<span class="amount-value">${amountFormat.format(row.heldQuantity() != null ? row.heldQuantity() : 0)}</span>
														!{Integer currentHeldQuantity = monthlyDividendCurrentQuantities.get(row.stockItemId());}
														@if(currentHeldQuantity != null && row.heldQuantity() != null && currentHeldQuantity.intValue() != row.heldQuantity().intValue())
														<span class="ml-1 text-[11px] text-warning">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.simulator.monthly.table.cell.quantity.current"), amountFormat.format(currentHeldQuantity))}</span>
														@endif
													</div>
													<div class="text-sm text-base-content/80">
														<span class="mr-1 text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.average.buy.price.short")}</span>
														<span class="amount-value">₩${amountFormat.format(row.averageBuyPrice() != null ? row.averageBuyPrice() : BigDecimal.ZERO)}</span>
														!{BigDecimal currentAverageBuyPrice = monthlyDividendCurrentAverageBuyPrices.get(row.stockItemId());}
														@if(currentAverageBuyPrice != null && row.averageBuyPrice() != null && currentAverageBuyPrice.setScale(0, java.math.RoundingMode.HALF_UP).compareTo(row.averageBuyPrice().setScale(0, java.math.RoundingMode.HALF_UP)) != 0)
														<span class="ml-1 text-[11px] text-warning">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.simulator.monthly.table.cell.average.buy.price.current"), amountFormat.format(currentAverageBuyPrice))}</span>
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
												</div>
											</td>
											<td class="align-top text-right">
												<div class="space-y-1 tabular-nums">
													<div class="${row.totalReturnOnCostPct() != null && row.totalReturnOnCostPct().signum() >= 0 ? "text-profit" : "text-loss"}">${percentFormat.format(row.totalReturnOnCostPct() != null ? row.totalReturnOnCostPct() : BigDecimal.ZERO)}%</div>
													<div class="font-semibold ${row.expectedCombinedReturnPct() != null && row.expectedCombinedReturnPct().signum() >= 0 ? "text-profit" : "text-loss"}">${percentFormat.format(row.expectedCombinedReturnPct() != null ? row.expectedCombinedReturnPct() : BigDecimal.ZERO)}%</div>
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

	.monthly-dividend-row:focus-visible {
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

<script nonce="${CspNonceHolder.getNonce()}">
	(function() {
		function getMonthlyDividendSimulatorLocale() {
			return document.documentElement.getAttribute('lang') || 'ko-KR';
		}

		function formatMonthlyDividendSimulatorNumber(value, fractionDigits) {
			return new Intl.NumberFormat(getMonthlyDividendSimulatorLocale(), {
				minimumFractionDigits: fractionDigits,
				maximumFractionDigits: fractionDigits
			}).format(value);
		}

		function formatMonthlyDividendSimulatorCurrency(value) {
			return '₩' + formatMonthlyDividendSimulatorNumber(value, 0);
		}

		function formatMonthlyDividendSimulatorSignedCurrency(value) {
			const absolute = formatMonthlyDividendSimulatorCurrency(Math.abs(value));
			if (value > 0) {
				return '+' + absolute;
			}
			if (value < 0) {
				return '-' + absolute;
			}
			return absolute;
		}

		function formatMonthlyDividendSimulatorSignedPercent(value) {
			const absolute = formatMonthlyDividendSimulatorNumber(Math.abs(value), 2) + '%';
			if (value > 0) {
				return '+' + absolute;
			}
			if (value < 0) {
				return '-' + absolute;
			}
			return absolute;
		}

		function setMonthlyDividendSimulatorSignedTone(element, value) {
			if (!element) {
				return;
			}

			element.classList.toggle('text-profit', value > 0);
			element.classList.toggle('text-loss', value < 0);
		}

		function setMonthlyDividendSimulatorRowSelection(row, selected) {
			if (!row) {
				return;
			}

			row.classList.toggle('monthly-dividend-row-selected', selected);
			row.setAttribute('aria-pressed', selected ? 'true' : 'false');

			const checkbox = row.querySelector('[data-monthly-selection-toggle]');
			if (checkbox) {
				checkbox.checked = selected;
			}
		}

		function updateMonthlyDividendSimulatorSelectionSummary(section) {
			if (!section) {
				return;
			}

			const summary = section.querySelector('[data-monthly-selection-summary]');
			const table = section.querySelector('[data-monthly-selection-table]');
			if (!summary || !table || !table.tBodies || table.tBodies.length === 0) {
				return;
			}

			const selectedRows = table.tBodies[0].querySelectorAll('[data-monthly-selection-row][aria-pressed="true"]');
			const selectedCount = selectedRows.length;
			const reveal = summary.closest('.selection-reveal');
			if (reveal) {
				reveal.classList.toggle('is-open', selectedCount > 0);
			} else {
				summary.classList.toggle('hidden', selectedCount === 0);
			}

			if (selectedCount === 0) {
				return;
			}

			let totalLatestMonthlyDividend = 0;
			let totalLatestMonthlyDividendMid = 0;
			let totalLatestMonthlyDividendEnd = 0;
			let totalBuyAmount = 0;
			let totalExpectedMonthlyDividend = 0;
			let totalExpectedTaxableBase = 0;
			let totalCurrentMarketValue = 0;

			for (let i = 0; i < selectedRows.length; i++) {
				const row = selectedRows[i];
				const rowLatestMonthlyDividend = Number(row.dataset.latestMonthlyDividend || '0');
				totalLatestMonthlyDividend += rowLatestMonthlyDividend;
				const rowPayoutWindow = row.dataset.payoutWindow || '';
				if (rowPayoutWindow === 'MID_MONTH') {
					totalLatestMonthlyDividendMid += rowLatestMonthlyDividend;
				} else if (rowPayoutWindow === 'MONTH_END') {
					totalLatestMonthlyDividendEnd += rowLatestMonthlyDividend;
				}
				totalBuyAmount += Number(row.dataset.buyAmount || '0');
				totalExpectedMonthlyDividend += Number(row.dataset.expectedMonthlyDividend || '0');
				totalExpectedTaxableBase += Number(row.dataset.expectedTaxableBase || '0');
				totalCurrentMarketValue += Number(row.dataset.currentMarketValue || '0');
			}

			const totalExpectedAnnualDividend = totalExpectedMonthlyDividend * 12;
			const totalExpectedAnnualTaxableBase = totalExpectedTaxableBase * 12;
			const totalExpectedMonthlyReference = Number(summary.dataset.totalExpectedMonthlyDividend || '0');
			const totalCurrentMarketValueReference = Number(summary.dataset.totalCurrentMarketValue || '0');
			const selectionMonthlyWeight = totalExpectedMonthlyReference > 0
				? (totalExpectedMonthlyDividend / totalExpectedMonthlyReference) * 100
				: 0;
			const selectionMarketWeight = totalCurrentMarketValueReference > 0
				? (totalCurrentMarketValue / totalCurrentMarketValueReference) * 100
				: 0;
			const evaluationProfit = totalCurrentMarketValue - totalBuyAmount;
			const evaluationReturn = totalBuyAmount > 0
				? (evaluationProfit / totalBuyAmount) * 100
				: 0;
			const portfolioExpectedAnnualYield = totalCurrentMarketValue > 0
				? (totalExpectedAnnualDividend / totalCurrentMarketValue) * 100
				: 0;

			const countLabel = summary.querySelector('[data-monthly-selection-count]');
			if (countLabel) {
				countLabel.textContent = String(summary.dataset.countTemplate || '').replace('{0}', String(selectedCount));
			}

			const itemCountLabel = summary.querySelector('[data-monthly-selection-item-count]');
			if (itemCountLabel) {
				itemCountLabel.textContent = formatMonthlyDividendSimulatorNumber(selectedCount, 0);
			}

			const monthlyWeightLabel = summary.querySelector('[data-monthly-selection-monthly-weight]');
			if (monthlyWeightLabel) {
				monthlyWeightLabel.textContent = formatMonthlyDividendSimulatorNumber(selectionMonthlyWeight, 1) + '%';
			}

			const marketWeightLabel = summary.querySelector('[data-monthly-selection-market-weight]');
			if (marketWeightLabel) {
				marketWeightLabel.textContent = formatMonthlyDividendSimulatorNumber(selectionMarketWeight, 1) + '%';
			}

			const latestLabel = summary.querySelector('[data-monthly-selection-latest]');
			if (latestLabel) {
				latestLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalLatestMonthlyDividend);
			}

			const latestMidLabel = summary.querySelector('[data-monthly-selection-latest-mid]');
			if (latestMidLabel) {
				latestMidLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalLatestMonthlyDividendMid);
			}

			const latestEndLabel = summary.querySelector('[data-monthly-selection-latest-end]');
			if (latestEndLabel) {
				latestEndLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalLatestMonthlyDividendEnd);
			}

			const monthlyLabel = summary.querySelector('[data-monthly-selection-monthly]');
			if (monthlyLabel) {
				monthlyLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalExpectedMonthlyDividend);
			}

			const annualDividendLabel = summary.querySelector('[data-monthly-selection-annual-dividend]');
			if (annualDividendLabel) {
				annualDividendLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalExpectedAnnualDividend);
			}

			const taxableLabel = summary.querySelector('[data-monthly-selection-taxable]');
			if (taxableLabel) {
				taxableLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalExpectedTaxableBase);
			}

			const annualTaxableLabel = summary.querySelector('[data-monthly-selection-annual-taxable]');
			if (annualTaxableLabel) {
				annualTaxableLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalExpectedAnnualTaxableBase);
			}

			const buyLabel = summary.querySelector('[data-monthly-selection-buy]');
			if (buyLabel) {
				buyLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalBuyAmount);
			}

			const marketLabel = summary.querySelector('[data-monthly-selection-market]');
			if (marketLabel) {
				marketLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalCurrentMarketValue);
			}

			const profitLabel = summary.querySelector('[data-monthly-selection-profit]');
			if (profitLabel) {
				profitLabel.textContent = formatMonthlyDividendSimulatorSignedCurrency(evaluationProfit);
				setMonthlyDividendSimulatorSignedTone(profitLabel, evaluationProfit);
			}

			const profitRateLabel = summary.querySelector('[data-monthly-selection-profit-rate]');
			if (profitRateLabel) {
				profitRateLabel.textContent = formatMonthlyDividendSimulatorSignedPercent(evaluationReturn);
				setMonthlyDividendSimulatorSignedTone(profitRateLabel, evaluationReturn);
			}

			const annualYieldLabel = summary.querySelector('[data-monthly-selection-annual-yield]');
			if (annualYieldLabel) {
				annualYieldLabel.textContent = formatMonthlyDividendSimulatorNumber(portfolioExpectedAnnualYield, 2) + '%';
			}
		}

		function initializeMonthlyDividendSimulatorSelection() {
			const sections = document.querySelectorAll('[data-monthly-simulator-selection-section]');
			for (let i = 0; i < sections.length; i++) {
				const section = sections[i];
				if (section.dataset.selectionInitialized === 'true') {
					continue;
				}

				const table = section.querySelector('[data-monthly-selection-table]');
				if (!table || !table.tBodies || table.tBodies.length === 0) {
					continue;
				}

				section.dataset.selectionInitialized = 'true';

				const rows = table.tBodies[0].querySelectorAll('[data-monthly-selection-row]');
				for (let j = 0; j < rows.length; j++) {
					const row = rows[j];

					row.addEventListener('click', function(event) {
						if (event.target && event.target.closest('button, a, input, label, select, textarea, [data-profile-order-handle]')) {
							return;
						}

						const isSelected = this.getAttribute('aria-pressed') === 'true';
						setMonthlyDividendSimulatorRowSelection(this, !isSelected);
						updateMonthlyDividendSimulatorSelectionSummary(section);
					});

					row.addEventListener('keydown', function(event) {
						if (event.target && event.target.closest('button, a, input, label, select, textarea, [data-profile-order-handle]')) {
							return;
						}

						if (event.key !== 'Enter' && event.key !== ' ') {
							return;
						}

						event.preventDefault();
						const isSelected = this.getAttribute('aria-pressed') === 'true';
						setMonthlyDividendSimulatorRowSelection(this, !isSelected);
						updateMonthlyDividendSimulatorSelectionSummary(section);
					});

					const checkbox = row.querySelector('[data-monthly-selection-toggle]');
					if (checkbox) {
						checkbox.addEventListener('click', function(event) {
							event.stopPropagation();
						});

						checkbox.addEventListener('change', function(event) {
							event.stopPropagation();
							const targetRow = this.closest('[data-monthly-selection-row]');
							setMonthlyDividendSimulatorRowSelection(targetRow, this.checked);
							updateMonthlyDividendSimulatorSelectionSummary(section);
						});
					}
				}

				const clearButton = section.querySelector('[data-monthly-selection-clear]');
				if (clearButton) {
					clearButton.addEventListener('click', function() {
						const selectedRows = table.tBodies[0].querySelectorAll('[data-monthly-selection-row][aria-pressed="true"]');
						for (let k = 0; k < selectedRows.length; k++) {
							setMonthlyDividendSimulatorRowSelection(selectedRows[k], false);
						}

						updateMonthlyDividendSimulatorSelectionSummary(section);
					});
				}

				updateMonthlyDividendSimulatorSelectionSummary(section);
			}
		}

		initializeMonthlyDividendSimulatorSelection();
	})();
</script>