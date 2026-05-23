@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.text.DecimalFormat
@import java.time.LocalDate
@import java.util.List
@import java.util.Map
@import net.luversof.web.gate.stock.domain.StockItem
@import net.luversof.web.gate.stock.dto.request.MonthlyDividendSnapshotUpsertRequest
@import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse
@import net.luversof.web.gate.stock.dto.view.MonthlyDividendSimulatorSummaryView

@param List<MonthlyDividendSnapshotResponse> monthlyDividendRows = java.util.List.of()
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

!{
	DecimalFormat amountFormat = new DecimalFormat("#,##0");
	DecimalFormat percentFormat = new DecimalFormat("#,##0.00");
	boolean monthlyDividendDisplayOrderSort = "display-order".equals(monthlyDividendSort);
	MonthlyDividendSnapshotResponse bestChoice = monthlyDividendSummary != null ? monthlyDividendSummary.bestChoice() : null;
	BigDecimal totalLatestMonthlyDividend = monthlyDividendSummary != null ? monthlyDividendSummary.totalLatestMonthlyDividend() : BigDecimal.ZERO;
	BigDecimal totalExpectedMonthlyDividend = monthlyDividendSummary != null ? monthlyDividendSummary.totalExpectedMonthlyDividend() : BigDecimal.ZERO;
	BigDecimal totalExpectedAnnualDividend = monthlyDividendSummary != null ? monthlyDividendSummary.totalExpectedAnnualDividend() : BigDecimal.ZERO;
	BigDecimal totalExpectedTaxableBaseAmount = monthlyDividendSummary != null ? monthlyDividendSummary.totalExpectedTaxableBaseAmount() : BigDecimal.ZERO;
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
	String monthlyDividendReferenceManageUrl = "/stock/dividend?tab=monthly-reference";
	String totalLatestMonthlyDividendLabel = MessageUtil.getMessage("stock.simulator.monthly.summary.latest.monthly.dividend");
	String totalLatestMonthlyDividendDesc = MessageUtil.getMessage("stock.simulator.monthly.summary.latest.monthly.dividend.desc");
	String totalTaxableBaseSummaryLabel = MessageUtil.getMessage("stock.simulator.monthly.summary.total.taxable.base");
	String totalTaxableBaseSummaryDesc = MessageUtil.getMessage("stock.simulator.monthly.summary.total.taxable.base.desc");
	String monthlySelectionHelperLabel = MessageUtil.getMessage("stock.simulator.monthly.selection.helper");
	String monthlyDividendReorderEnabledLabel = MessageUtil.getMessage("stock.simulator.monthly.reorder.enabled");
	String monthlyDividendReorderDisabledLabel = MessageUtil.getMessage("stock.simulator.monthly.reorder.disabled");
	String selectionSummaryLabel = MessageUtil.getMessage("stock.analytics.selection.summary");
	String selectionCountTemplate = MessageUtil.getMessage("stock.analytics.selection.count");
	String selectionClearLabel = MessageUtil.getMessage("stock.analytics.selection.clear");
	String selectionWeightLabel = MessageUtil.getMessage("stock.analytics.selection.weight");
	String monthlyDividendTableBaseUrl = "/stock/simulator?tab=monthly-dividend";
	String currentDirectionLabel = MessageUtil.getMessage("asc".equals(monthlyDividendDirection) ? "stock.simulator.monthly.direction.asc" : "stock.simulator.monthly.direction.desc");
	String displayOrderDirection = "display-order".equals(monthlyDividendSort) ? ("asc".equals(monthlyDividendDirection) ? "desc" : "asc") : "asc";
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
		successMessage = "월배당 데이터를 저장했습니다.";
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

	<div class="grid gap-6 xl:grid-cols-[24rem_minmax(0,1fr)]">
		<div class="space-y-6">
			<section class="card border border-base-300 bg-base-100 shadow-sm">
				<div class="card-body gap-4">
					<div class="space-y-2">
						<h2 class="card-title text-lg">${MessageUtil.getMessage("stock.simulator.monthly.form.title")}</h2>
						<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.form.desc")}</p>
					</div>
					<form method="get" action="/stock/simulator" class="grid gap-3 rounded-2xl border border-base-300 bg-base-50 p-4">
						<input type="hidden" name="tab" value="monthly-dividend" />
						<input type="hidden" name="sort" value="${monthlyDividendSort}" />
						<input type="hidden" name="direction" value="${monthlyDividendDirection}" />
						<div class="space-y-1">
							<p class="text-sm font-medium text-base-content">${MessageUtil.getMessage("stock.simulator.monthly.prefill.title")}</p>
							<p class="text-xs leading-5 text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.prefill.desc")}</p>
						</div>
						<div class="grid gap-3 md:grid-cols-[minmax(0,1fr)_auto] md:items-end">
							<label class="form-control">
								<div class="label py-1"><span class="label-text text-xs font-medium">${MessageUtil.getMessage("stock.simulator.monthly.form.field.symbol")}</span></div>
								<input name="symbol" type="text" list="monthlyDividendStockOptions" value="${symbolValue}" placeholder="O" class="input input-bordered w-full uppercase" required />
							</label>
							<button type="submit" class="btn btn-outline">${MessageUtil.getMessage("stock.simulator.monthly.prefill.submit")}</button>
						</div>
					</form>
					<div class="rounded-2xl border border-base-300 bg-base-50 p-4">
						<div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
							<div class="space-y-1">
								<p class="text-sm font-medium text-base-content">${MessageUtil.getMessage("stock.simulator.monthly.reference.title")}</p>
								<p class="text-xs leading-5 text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.reference.desc")}</p>
							</div>
							<a href="${monthlyDividendReferenceManageUrl}" class="btn btn-ghost btn-sm">${MessageUtil.getMessage("stock.simulator.monthly.reference.manage")}</a>
						</div>
						@if(referenceValuesAvailable)
							<div class="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
								<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-3">
									<p class="text-xs font-medium uppercase tracking-wide text-base-content/50">${MessageUtil.getMessage("stock.simulator.monthly.form.field.as.of.date")}</p>
									<p class="mt-2 text-base font-semibold text-base-content">${asOfDateValue}</p>
								</div>
								<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-3">
									<p class="text-xs font-medium uppercase tracking-wide text-base-content/50">${MessageUtil.getMessage("stock.simulator.monthly.form.field.latest.monthly.dividend")}</p>
									<p class="mt-2 text-base font-semibold text-base-content">₩${latestMonthlyDividendValue}</p>
								</div>
								<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-3">
									<p class="text-xs font-medium uppercase tracking-wide text-base-content/50">${MessageUtil.getMessage("stock.simulator.monthly.form.field.average.monthly.dividend")}</p>
									<p class="mt-2 text-base font-semibold text-base-content">₩${averageMonthlyDividendValue}</p>
								</div>
								<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-3">
									<p class="text-xs font-medium uppercase tracking-wide text-base-content/50">${MessageUtil.getMessage("stock.simulator.monthly.form.field.taxable.base.ratio")}</p>
									<p class="mt-2 text-base font-semibold text-base-content">${taxableBaseRatioValue}%</p>
								</div>
							</div>
						@else
							<div class="mt-4 alert border border-warning/20 bg-warning/10 text-warning-content">
								<span>${MessageUtil.getMessage("stock.simulator.monthly.reference.empty")}</span>
							</div>
						@endif
					</div>
					<form method="post" action="/stock/simulator/monthly-dividend" class="grid gap-4">
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
						<p class="text-xs leading-5 text-base-content/50">${MessageUtil.getMessage("stock.simulator.monthly.form.hint")}</p>
						<button type="submit" class="btn btn-primary w-full" disabled="${!referenceValuesAvailable}">${MessageUtil.getMessage("stock.simulator.monthly.form.submit")}</button>
					</form>
				</div>
			</section>

			<aside class="card border border-base-300 bg-base-100 shadow-sm">
				<div class="card-body gap-4">
					<h2 class="card-title text-lg">${MessageUtil.getMessage("stock.simulator.monthly.section.criteria.title")}</h2>
					<ul class="list-disc space-y-3 pl-5 text-sm leading-6 text-base-content/70">
						<li>${MessageUtil.getMessage("stock.simulator.monthly.criteria.one")}</li>
						<li>${MessageUtil.getMessage("stock.simulator.monthly.criteria.two")}</li>
						<li>${MessageUtil.getMessage("stock.simulator.monthly.criteria.three")}</li>
						<li>${MessageUtil.getMessage("stock.simulator.monthly.criteria.four")}</li>
					</ul>
				</div>
			</aside>
		</div>

		<div class="space-y-6">
			<div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-6">
				<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-4 shadow-sm">
					<p class="text-xs font-medium uppercase tracking-wide text-base-content/50">${MessageUtil.getMessage("stock.simulator.monthly.summary.item.count")}</p>
					<p class="mt-3 text-2xl font-semibold text-base-content">${amountFormat.format(itemCount)}</p>
					<p class="mt-2 text-sm text-base-content/60">${MessageUtil.getMessage("stock.simulator.tab.monthly.dividend")}</p>
				</div>
				<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-4 shadow-sm">
					<p class="text-xs font-medium uppercase tracking-wide text-base-content/50">${totalLatestMonthlyDividendLabel}</p>
					<p class="mt-3 text-2xl font-semibold text-base-content">₩${amountFormat.format(totalLatestMonthlyDividend)}</p>
					<p class="mt-2 text-sm text-base-content/60">${totalLatestMonthlyDividendDesc}</p>
				</div>
				<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-4 shadow-sm">
					<p class="text-xs font-medium uppercase tracking-wide text-base-content/50">${MessageUtil.getMessage("stock.simulator.monthly.summary.total.monthly.dividend")}</p>
					<p class="mt-3 text-2xl font-semibold text-base-content">₩${amountFormat.format(totalExpectedMonthlyDividend)}</p>
					<p class="mt-2 text-sm text-base-content/60">${MessageUtil.getMessage("stock.simulator.metric.annual.dividend")}</p>
				</div>
				<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-4 shadow-sm">
					<p class="text-xs font-medium uppercase tracking-wide text-base-content/50">${MessageUtil.getMessage("stock.simulator.monthly.summary.total.annual.dividend")}</p>
					<p class="mt-3 text-2xl font-semibold text-base-content">₩${amountFormat.format(totalExpectedAnnualDividend)}</p>
					<p class="mt-2 text-sm text-base-content/60">12 x ${MessageUtil.getMessage("stock.simulator.monthly.summary.total.monthly.dividend")}</p>
				</div>
				<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-4 shadow-sm">
					<p class="text-xs font-medium uppercase tracking-wide text-base-content/50">${totalTaxableBaseSummaryLabel}</p>
					<p class="mt-3 text-2xl font-semibold text-base-content">₩${amountFormat.format(totalExpectedTaxableBaseAmount)}</p>
					<p class="mt-2 text-sm text-base-content/60">${totalTaxableBaseSummaryDesc}</p>
				</div>
				<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-4 shadow-sm">
					<p class="text-xs font-medium uppercase tracking-wide text-base-content/50">${MessageUtil.getMessage("stock.simulator.monthly.summary.portfolio.annual.yield")}</p>
					<p class="mt-3 text-2xl font-semibold text-base-content">${percentFormat.format(portfolioExpectedAnnualYieldPct)}%</p>
					@if(bestChoice != null)
						<p class="mt-2 text-sm text-base-content/60">${bestChoice.stockItemSymbol()} · ${MessageUtil.getMessage("stock.simulator.monthly.summary.best.choice")}</p>
					@else
						<p class="mt-2 text-sm text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.summary.no.best.choice")}</p>
					@endif
				</div>
			</div>

			<section class="card border border-base-300 bg-base-100 shadow-sm">
				<div class="card-body gap-5">
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
							<p class="text-xs text-base-content/50">${monthlySelectionHelperLabel}</p>
							<div id="monthlyDividendSimulatorOrderStatus" class="hidden"></div>
							<p class="text-xs ${monthlyDividendReorderEnabled ? "text-primary/70" : "text-base-content/40"}">${monthlyDividendReorderEnabled ? monthlyDividendReorderEnabledLabel : monthlyDividendReorderDisabledLabel}</p>
							<div class="hidden rounded-2xl border border-primary/15 bg-primary/5 px-4 py-3"
								 data-monthly-selection-summary
								 data-count-template="${selectionCountTemplate}"
								 data-total-expected-monthly-dividend="${totalExpectedMonthlyDividend.toPlainString()}"
								 data-weight-label="${selectionWeightLabel}"
								 aria-live="polite">
								<div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
									<div class="flex flex-wrap items-center gap-2">
										<h3 class="text-sm font-semibold">${selectionSummaryLabel}</h3>
										<span class="badge badge-primary badge-outline" data-monthly-selection-count>${selectionSummaryLabel}</span>
										<span class="badge badge-ghost" data-monthly-selection-weight>${selectionWeightLabel} 0.0%</span>
									</div>
									<button type="button" class="btn btn-ghost btn-xs self-start" data-monthly-selection-clear>${selectionClearLabel}</button>
								</div>
								<div class="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
									<div class="rounded-xl bg-base-100/80 px-3 py-2">
										<div class="text-xs text-base-content/60">${totalLatestMonthlyDividendLabel}</div>
										<div class="mt-1 font-mono font-semibold amount-value" data-monthly-selection-latest>₩0</div>
									</div>
									<div class="rounded-xl bg-base-100/80 px-3 py-2">
										<div class="text-xs text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.summary.total.monthly.dividend")}</div>
										<div class="mt-1 font-mono font-semibold amount-value" data-monthly-selection-monthly>₩0</div>
									</div>
									<div class="rounded-xl bg-base-100/80 px-3 py-2">
										<div class="text-xs text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.summary.total.annual.dividend")}</div>
										<div class="mt-1 font-mono font-semibold amount-value" data-monthly-selection-annual>₩0</div>
									</div>
									<div class="rounded-xl bg-base-100/80 px-3 py-2">
										<div class="text-xs text-base-content/60">${totalTaxableBaseSummaryLabel}</div>
										<div class="mt-1 font-mono font-semibold amount-value" data-monthly-selection-taxable>₩0</div>
									</div>
									<div class="rounded-xl bg-base-100/80 px-3 py-2">
										<div class="text-xs text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.summary.portfolio.annual.yield")}</div>
										<div class="mt-1 font-mono font-semibold" data-monthly-selection-annual-yield>0.00%</div>
									</div>
								</div>
							</div>
							<div class="overflow-x-auto">
								<table class="table table-pin-rows table-zebra table-sm whitespace-nowrap" data-monthly-selection-table>
								<thead>
									<tr>
										<th class="w-10" rowspan="2"></th>
										<th rowspan="2">
											<a href="${monthlyDividendTableBaseUrl}&sort=display-order&direction=${displayOrderDirection}" class="link link-hover font-medium no-underline">
												${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.display.order")} / ${MessageUtil.getMessage("stock.simulator.monthly.table.header.symbol")}
												@if("display-order".equals(monthlyDividendSort))
													<span class="ml-1 text-xs text-base-content/60">${currentDirectionLabel}</span>
												@endif
											</a>
										</th>
										<th rowspan="2">${MessageUtil.getMessage("stock.simulator.monthly.table.header.name")}</th>
										<th class="text-center text-[11px] font-medium uppercase tracking-wide text-base-content/45" colspan="2">${MessageUtil.getMessage("stock.simulator.monthly.table.group.reference")}</th>
										<th class="text-center text-[11px] font-medium uppercase tracking-wide text-base-content/45" colspan="2">${MessageUtil.getMessage("stock.simulator.monthly.table.group.input")}</th>
										<th rowspan="2" class="min-w-[9rem] align-bottom text-right">
											<div class="space-y-1 whitespace-normal text-right">
												<a href="${monthlyDividendTableBaseUrl}&sort=monthly-dividend&direction=${monthlyDividendDirectionToggle}" class="link link-hover block text-sm font-medium no-underline">
													${MessageUtil.getMessage("stock.simulator.monthly.table.header.expected.monthly.dividend")}
													@if("monthly-dividend".equals(monthlyDividendSort))
														<span class="mt-1 block text-[11px] text-base-content/60">${currentDirectionLabel}</span>
													@endif
												</a>
												<div class="text-[11px] font-medium leading-4 text-base-content/45">${MessageUtil.getMessage("stock.simulator.monthly.table.header.expected.taxable.base")}</div>
											</div>
										</th>
										<th class="text-center text-[11px] font-medium uppercase tracking-wide text-base-content/45" colspan="2">${MessageUtil.getMessage("stock.simulator.monthly.table.group.yield")}</th>
										<th class="text-center text-[11px] font-medium uppercase tracking-wide text-base-content/45" colspan="2">${MessageUtil.getMessage("stock.simulator.monthly.table.group.result")}</th>
									</tr>
									<tr>
										<th class="text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.monthly.dividend.per.share")}</th>
										<th class="text-right">
											<a href="${monthlyDividendTableBaseUrl}&sort=taxable-base&direction=${taxableBaseDirectionToggle}" class="link link-hover font-medium no-underline">
												${MessageUtil.getMessage("stock.simulator.monthly.table.header.taxable.base.ratio")}
												@if("taxable-base".equals(monthlyDividendSort))
													<span class="ml-1 text-xs text-base-content/60">${currentDirectionLabel}</span>
												@endif
											</a>
										</th>
										<th class="text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.current.price")}</th>
										<th class="text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.quantity.and.buy.price")}</th>
										<th class="min-w-[8rem] align-bottom text-right">
											<div class="space-y-1 whitespace-normal text-right">
												<div class="text-[11px] font-medium leading-4 text-base-content/45">${MessageUtil.getMessage("stock.simulator.monthly.table.header.market.price.basis")}</div>
												<a href="${monthlyDividendTableBaseUrl}&sort=monthly-yield&direction=${monthlyYieldDirectionToggle}" class="link link-hover block text-xs font-medium leading-4 no-underline">
													${MessageUtil.getMessage("stock.simulator.monthly.table.header.monthly.yield.short")}
													@if("monthly-yield".equals(monthlyDividendSort))
														<span class="mt-1 block text-[11px] text-base-content/60">${currentDirectionLabel}</span>
													@endif
												</a>
												<a href="${monthlyDividendTableBaseUrl}&sort=annual-yield&direction=${annualYieldDirectionToggle}" class="link link-hover block text-xs font-medium leading-4 no-underline">
													${MessageUtil.getMessage("stock.simulator.monthly.table.header.annual.yield.short")}
													@if("annual-yield".equals(monthlyDividendSort))
														<span class="mt-1 block text-[11px] text-base-content/60">${currentDirectionLabel}</span>
													@endif
												</a>
											</div>
										</th>
										<th class="min-w-[8rem] align-bottom text-right">
											<div class="space-y-1 whitespace-normal text-right">
												<div class="text-[11px] font-medium leading-4 text-base-content/45">${MessageUtil.getMessage("stock.simulator.monthly.table.header.on.cost")}</div>
												<a href="${monthlyDividendTableBaseUrl}&sort=monthly-yield-on-cost&direction=${monthlyYieldOnCostDirectionToggle}" class="link link-hover block text-xs font-medium leading-4 no-underline">
													${MessageUtil.getMessage("stock.simulator.monthly.table.header.monthly.yield.short")}
													@if("monthly-yield-on-cost".equals(monthlyDividendSort))
														<span class="mt-1 block text-[11px] text-base-content/60">${currentDirectionLabel}</span>
													@endif
												</a>
												<a href="${monthlyDividendTableBaseUrl}&sort=annual-yield-on-cost&direction=${annualYieldOnCostDirectionToggle}" class="link link-hover block text-xs font-medium leading-4 no-underline">
													${MessageUtil.getMessage("stock.simulator.monthly.table.header.annual.yield.short")}
													@if("annual-yield-on-cost".equals(monthlyDividendSort))
														<span class="mt-1 block text-[11px] text-base-content/60">${currentDirectionLabel}</span>
													@endif
												</a>
											</div>
										</th>
										<th class="text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.total.return.on.cost")}</th>
										<th class="text-right">
											<a href="${monthlyDividendTableBaseUrl}&sort=combined-return&direction=${combinedReturnDirectionToggle}" class="link link-hover font-medium no-underline">
												${MessageUtil.getMessage("stock.simulator.monthly.table.header.expected.combined.return")}
												@if("combined-return".equals(monthlyDividendSort))
													<span class="ml-1 text-xs text-base-content/60">${currentDirectionLabel}</span>
												@endif
											</a>
										</th>
									</tr>
								</thead>
								<tbody data-profile-order-list="true" data-profile-order-status-id="monthlyDividendSimulatorOrderStatus" data-profile-order-endpoint="${monthlyDividendOrderEndpoint}" data-profile-order-row-selector="tr[data-profile-order-row='true']" data-profile-order-handle-selector="[data-profile-order-handle]" data-profile-order-all-symbols="${String.join(",", monthlyDividendProfileOrderedSymbols)}" data-profile-reorder-enabled="${monthlyDividendReorderEnabled}" data-success-message="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.reordered.profile")}" data-fallback-error-message="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.reordered.profile.failed")}">
									@for(MonthlyDividendSnapshotResponse row : monthlyDividendRows)
											!{BigDecimal latestMonthlyDividendAmount = (row.latestMonthlyDividendPerShare() != null ? row.latestMonthlyDividendPerShare() : BigDecimal.ZERO).multiply(BigDecimal.valueOf(row.heldQuantity() != null ? row.heldQuantity().longValue() : 0L));}
											!{String normalizedRowSymbol = row.stockItemSymbol() != null ? row.stockItemSymbol().trim().toUpperCase(java.util.Locale.ROOT) : "";}
											!{Integer rowDisplayOrder = monthlyDividendProfileDisplayOrders != null ? monthlyDividendProfileDisplayOrders.get(normalizedRowSymbol) : null;}
											!{boolean rowReorderable = monthlyDividendReorderEnabled && rowDisplayOrder != null;}
											<tr class="monthly-dividend-row hover${rowReorderable ? " cursor-move" : ""}"
												data-monthly-selection-row
												data-profile-order-row="${rowDisplayOrder != null ? "true" : "false"}"
												data-symbol="${normalizedRowSymbol}"
												tabindex="0"
												role="button"
												aria-pressed="false"
												draggable="${rowReorderable}"
												data-latest-monthly-dividend="${latestMonthlyDividendAmount.toPlainString()}"
												data-expected-monthly-dividend="${(row.expectedMonthlyDividend() != null ? row.expectedMonthlyDividend() : BigDecimal.ZERO).toPlainString()}"
												data-expected-taxable-base="${(row.expectedTaxableBaseAmount() != null ? row.expectedTaxableBaseAmount() : BigDecimal.ZERO).toPlainString()}"
												data-current-market-value="${(row.currentMarketValue() != null ? row.currentMarketValue() : BigDecimal.ZERO).toPlainString()}">
												<td class="align-top pt-2">
													<input type="checkbox"
														class="checkbox checkbox-primary checkbox-sm"
														data-monthly-selection-toggle
														aria-label="${row.stockItemName() != null ? row.stockItemName() : row.stockItemSymbol()}">
												</td>
												<td class="font-medium">
													<div class="flex items-center gap-2">
														<span class="inline-flex items-center gap-1 rounded-full border border-base-300/70 bg-base-200/60 px-2 py-0.5 text-xs text-base-content/60">
															@if(rowReorderable)
																<span class="font-mono select-none" data-profile-order-handle>::</span>
															@else
																<span class="font-mono select-none opacity-40">--</span>
															@endif
															<span data-profile-order-value>${rowDisplayOrder != null ? String.valueOf(rowDisplayOrder) : "-"}</span>
														</span>
														<span>@if(bestChoice != null && bestChoice.id() != null && bestChoice.id().equals(row.id()))<span class="badge badge-success badge-xs mr-2">${MessageUtil.getMessage("stock.simulator.compare.best.choice")}</span>@endif${row.stockItemSymbol()}</span>
													</div>
												</td>
											<td>${row.stockItemName()}</td>
											<td class="align-top text-right">
												<div class="space-y-1 tabular-nums">
													<div>
														<span class="mr-1 text-[11px] text-base-content/45">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.latest.short")}</span>
														₩${amountFormat.format(row.latestMonthlyDividendPerShare() != null ? row.latestMonthlyDividendPerShare() : BigDecimal.ZERO)}
													</div>
													<div class="text-sm text-base-content/80">
														<span class="mr-1 text-[11px] text-base-content/45">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.average.short")}</span>
														₩${amountFormat.format(row.averageMonthlyDividendPerShare1y() != null ? row.averageMonthlyDividendPerShare1y() : BigDecimal.ZERO)}
													</div>
												</div>
											</td>
											<td class="text-right">${percentFormat.format(row.averageTaxableBaseRatio1y() != null ? row.averageTaxableBaseRatio1y() : BigDecimal.ZERO)}%</td>
											<td class="text-right">₩${amountFormat.format(row.currentPrice() != null ? row.currentPrice() : BigDecimal.ZERO)}</td>
											<td class="align-top text-right">
												<div class="space-y-1 tabular-nums">
													<div>
														<span class="mr-1 text-[11px] text-base-content/45">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.quantity.short")}</span>
														${amountFormat.format(row.heldQuantity() != null ? row.heldQuantity() : 0)}
													</div>
													<div class="text-sm text-base-content/80">
														<span class="mr-1 text-[11px] text-base-content/45">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.average.buy.price.short")}</span>
														₩${amountFormat.format(row.averageBuyPrice() != null ? row.averageBuyPrice() : BigDecimal.ZERO)}
													</div>
												</div>
											</td>
											<td class="align-top text-right">
												<div class="space-y-1 tabular-nums">
													<div class="font-medium">
														<span class="mr-1 text-[11px] text-base-content/45">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.expected.monthly.dividend.short")}</span>
														₩${amountFormat.format(row.expectedMonthlyDividend() != null ? row.expectedMonthlyDividend() : BigDecimal.ZERO)}
													</div>
													<div class="text-sm text-base-content/80">
														<span class="mr-1 text-[11px] text-base-content/45">${MessageUtil.getMessage("stock.simulator.monthly.table.cell.expected.taxable.base.short")}</span>
														₩${amountFormat.format(row.expectedTaxableBaseAmount() != null ? row.expectedTaxableBaseAmount() : BigDecimal.ZERO)}
													</div>
												</div>
											</td>
											<td class="align-top text-right">
												<div class="space-y-1 tabular-nums">
													<div>${percentFormat.format(row.expectedMonthlyYieldPct() != null ? row.expectedMonthlyYieldPct() : BigDecimal.ZERO)}%</div>
													<div class="text-sm text-base-content/80">${percentFormat.format(row.expectedAnnualYieldPct() != null ? row.expectedAnnualYieldPct() : BigDecimal.ZERO)}%</div>
												</div>
											</td>
											<td class="align-top text-right">
												<div class="space-y-1 tabular-nums">
													<div>${percentFormat.format(row.expectedMonthlyYieldOnCostPct() != null ? row.expectedMonthlyYieldOnCostPct() : BigDecimal.ZERO)}%</div>
													<div class="text-sm text-base-content/80">${percentFormat.format(row.expectedAnnualYieldOnCostPct() != null ? row.expectedAnnualYieldOnCostPct() : BigDecimal.ZERO)}%</div>
												</div>
											</td>
											<td class="text-right ${row.totalReturnOnCostPct() != null && row.totalReturnOnCostPct().signum() >= 0 ? "text-success" : "text-error"}">${percentFormat.format(row.totalReturnOnCostPct() != null ? row.totalReturnOnCostPct() : BigDecimal.ZERO)}%</td>
											<td class="text-right font-semibold ${row.expectedCombinedReturnPct() != null && row.expectedCombinedReturnPct().signum() >= 0 ? "text-success" : "text-error"}">${percentFormat.format(row.expectedCombinedReturnPct() != null ? row.expectedCombinedReturnPct() : BigDecimal.ZERO)}%</td>
										</tr>
									@endfor
								</tbody>
								</table>
							</div>
						</div>
					@endif
				</div>
			</section>
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
</style>

<script>
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
			summary.classList.toggle('hidden', selectedCount === 0);

			if (selectedCount === 0) {
				return;
			}

			let totalLatestMonthlyDividend = 0;
			let totalExpectedMonthlyDividend = 0;
			let totalExpectedTaxableBase = 0;
			let totalCurrentMarketValue = 0;

			for (let i = 0; i < selectedRows.length; i++) {
				const row = selectedRows[i];
				totalLatestMonthlyDividend += Number(row.dataset.latestMonthlyDividend || '0');
				totalExpectedMonthlyDividend += Number(row.dataset.expectedMonthlyDividend || '0');
				totalExpectedTaxableBase += Number(row.dataset.expectedTaxableBase || '0');
				totalCurrentMarketValue += Number(row.dataset.currentMarketValue || '0');
			}

			const totalExpectedAnnualDividend = totalExpectedMonthlyDividend * 12;
			const totalExpectedMonthlyReference = Number(summary.dataset.totalExpectedMonthlyDividend || '0');
			const selectionWeight = totalExpectedMonthlyReference > 0
				? (totalExpectedMonthlyDividend / totalExpectedMonthlyReference) * 100
				: 0;
			const portfolioExpectedAnnualYield = totalCurrentMarketValue > 0
				? (totalExpectedAnnualDividend / totalCurrentMarketValue) * 100
				: 0;

			const countLabel = summary.querySelector('[data-monthly-selection-count]');
			if (countLabel) {
				countLabel.textContent = String(summary.dataset.countTemplate || '').replace('{0}', String(selectedCount));
			}

			const weightLabel = summary.querySelector('[data-monthly-selection-weight]');
			if (weightLabel) {
				weightLabel.textContent = (summary.dataset.weightLabel || '') + ' ' + formatMonthlyDividendSimulatorNumber(selectionWeight, 1) + '%';
			}

			const latestLabel = summary.querySelector('[data-monthly-selection-latest]');
			if (latestLabel) {
				latestLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalLatestMonthlyDividend);
			}

			const monthlyLabel = summary.querySelector('[data-monthly-selection-monthly]');
			if (monthlyLabel) {
				monthlyLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalExpectedMonthlyDividend);
			}

			const annualLabel = summary.querySelector('[data-monthly-selection-annual]');
			if (annualLabel) {
				annualLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalExpectedAnnualDividend);
			}

			const taxableLabel = summary.querySelector('[data-monthly-selection-taxable]');
			if (taxableLabel) {
				taxableLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalExpectedTaxableBase);
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