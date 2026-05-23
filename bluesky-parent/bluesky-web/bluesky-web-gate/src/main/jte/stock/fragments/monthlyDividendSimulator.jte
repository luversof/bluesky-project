@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.text.DecimalFormat
@import java.time.LocalDate
@import java.util.List
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
@param String monthlyDividendSort = "combined-return"
@param String monthlyDividendDirection = "desc"
@param String monthlyDividendKeyword = ""
@param BigDecimal monthlyDividendMinAnnualYield = null
@param boolean monthlyDividendPositiveOnly = false
@param boolean monthlyDividendHasSavedRows = false

!{
	DecimalFormat amountFormat = new DecimalFormat("#,##0");
	DecimalFormat percentFormat = new DecimalFormat("#,##0.00");
	MonthlyDividendSnapshotResponse bestChoice = monthlyDividendSummary != null ? monthlyDividendSummary.bestChoice() : null;
	BigDecimal totalExpectedMonthlyDividend = monthlyDividendSummary != null ? monthlyDividendSummary.totalExpectedMonthlyDividend() : BigDecimal.ZERO;
	BigDecimal totalExpectedAnnualDividend = monthlyDividendSummary != null ? monthlyDividendSummary.totalExpectedAnnualDividend() : BigDecimal.ZERO;
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
						<input type="hidden" name="keyword" value="${monthlyDividendKeyword}" />
						<input type="hidden" name="minAnnualYield" value="${minAnnualYieldValue}" />
						@if(monthlyDividendPositiveOnly)
							<input type="hidden" name="positiveOnly" value="true" />
						@endif
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
						<input type="hidden" name="keyword" value="${monthlyDividendKeyword}" />
						<input type="hidden" name="minAnnualYield" value="${minAnnualYieldValue}" />
						@if(monthlyDividendPositiveOnly)
							<input type="hidden" name="positiveOnly" value="true" />
						@endif
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
			<div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
				<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-4 shadow-sm">
					<p class="text-xs font-medium uppercase tracking-wide text-base-content/50">${MessageUtil.getMessage("stock.simulator.monthly.summary.item.count")}</p>
					<p class="mt-3 text-2xl font-semibold text-base-content">${amountFormat.format(itemCount)}</p>
					<p class="mt-2 text-sm text-base-content/60">${MessageUtil.getMessage("stock.simulator.tab.monthly.dividend")}</p>
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
					<div class="flex flex-col gap-2 xl:flex-row xl:items-start xl:justify-between">
						<div class="space-y-2">
							<h2 class="card-title text-lg">${MessageUtil.getMessage("stock.simulator.monthly.section.table.title")}</h2>
							<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.section.table.desc")}</p>
						</div>
						<form method="get" action="/stock/simulator" class="grid gap-3 md:grid-cols-2 xl:grid-cols-5">
							<input type="hidden" name="tab" value="monthly-dividend" />
							<label class="form-control">
								<div class="label py-1"><span class="label-text text-xs font-medium">${MessageUtil.getMessage("stock.simulator.monthly.filter.keyword")}</span></div>
								<input name="keyword" type="text" value="${monthlyDividendKeyword}" class="input input-bordered input-sm w-full" />
							</label>
							<label class="form-control">
								<div class="label py-1"><span class="label-text text-xs font-medium">${MessageUtil.getMessage("stock.simulator.monthly.filter.min.annual.yield")}</span></div>
								<input name="minAnnualYield" type="number" min="0" step="0.01" value="${minAnnualYieldValue}" class="input input-bordered input-sm w-full" />
							</label>
							<label class="form-control">
								<div class="label py-1"><span class="label-text text-xs font-medium">${MessageUtil.getMessage("stock.simulator.monthly.filter.sort")}</span></div>
								<select name="sort" class="select select-bordered select-sm w-full">
									@if("combined-return".equals(monthlyDividendSort))
										<option value="combined-return" selected>${MessageUtil.getMessage("stock.simulator.monthly.sort.combined.return")}</option>
									@else
										<option value="combined-return">${MessageUtil.getMessage("stock.simulator.monthly.sort.combined.return")}</option>
									@endif
									@if("annual-yield-on-cost".equals(monthlyDividendSort))
										<option value="annual-yield-on-cost" selected>${MessageUtil.getMessage("stock.simulator.monthly.sort.annual.yield.on.cost")}</option>
									@else
										<option value="annual-yield-on-cost">${MessageUtil.getMessage("stock.simulator.monthly.sort.annual.yield.on.cost")}</option>
									@endif
									@if("annual-yield".equals(monthlyDividendSort))
										<option value="annual-yield" selected>${MessageUtil.getMessage("stock.simulator.monthly.sort.annual.yield")}</option>
									@else
										<option value="annual-yield">${MessageUtil.getMessage("stock.simulator.monthly.sort.annual.yield")}</option>
									@endif
									@if("monthly-dividend".equals(monthlyDividendSort))
										<option value="monthly-dividend" selected>${MessageUtil.getMessage("stock.simulator.monthly.sort.monthly.dividend")}</option>
									@else
										<option value="monthly-dividend">${MessageUtil.getMessage("stock.simulator.monthly.sort.monthly.dividend")}</option>
									@endif
									@if("taxable-base".equals(monthlyDividendSort))
										<option value="taxable-base" selected>${MessageUtil.getMessage("stock.simulator.monthly.sort.taxable.base")}</option>
									@else
										<option value="taxable-base">${MessageUtil.getMessage("stock.simulator.monthly.sort.taxable.base")}</option>
									@endif
									@if("updated-date".equals(monthlyDividendSort))
										<option value="updated-date" selected>${MessageUtil.getMessage("stock.simulator.monthly.sort.updated.date")}</option>
									@else
										<option value="updated-date">${MessageUtil.getMessage("stock.simulator.monthly.sort.updated.date")}</option>
									@endif
								</select>
							</label>
							<label class="form-control">
								<div class="label py-1"><span class="label-text text-xs font-medium">${MessageUtil.getMessage("stock.simulator.monthly.filter.direction")}</span></div>
								<select name="direction" class="select select-bordered select-sm w-full">
									@if("desc".equals(monthlyDividendDirection))
										<option value="desc" selected>${MessageUtil.getMessage("stock.simulator.monthly.direction.desc")}</option>
									@else
										<option value="desc">${MessageUtil.getMessage("stock.simulator.monthly.direction.desc")}</option>
									@endif
									@if("asc".equals(monthlyDividendDirection))
										<option value="asc" selected>${MessageUtil.getMessage("stock.simulator.monthly.direction.asc")}</option>
									@else
										<option value="asc">${MessageUtil.getMessage("stock.simulator.monthly.direction.asc")}</option>
									@endif
								</select>
							</label>
							<div class="flex flex-col justify-end gap-2">
								<label class="label cursor-pointer justify-start gap-2 rounded-xl border border-base-300 px-3 py-2">
									@if(monthlyDividendPositiveOnly)
										<input name="positiveOnly" type="checkbox" value="true" class="checkbox checkbox-primary checkbox-sm" checked />
									@else
										<input name="positiveOnly" type="checkbox" value="true" class="checkbox checkbox-primary checkbox-sm" />
									@endif
									<span class="label-text text-sm">${MessageUtil.getMessage("stock.simulator.monthly.filter.positive.only")}</span>
								</label>
								<div class="flex gap-2">
									<button type="submit" class="btn btn-primary btn-sm flex-1">${MessageUtil.getMessage("stock.simulator.monthly.filter.apply")}</button>
									<a href="/stock/simulator?tab=monthly-dividend" class="btn btn-ghost btn-sm flex-1">${MessageUtil.getMessage("stock.simulator.monthly.filter.reset")}</a>
								</div>
							</div>
						</form>
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
						<div class="overflow-x-auto">
							<table class="table table-pin-rows table-zebra table-sm whitespace-nowrap">
								<thead>
									<tr>
										<th>${MessageUtil.getMessage("stock.simulator.monthly.table.header.symbol")}</th>
										<th>${MessageUtil.getMessage("stock.simulator.monthly.table.header.name")}</th>
										<th class="text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.current.price")}</th>
										<th class="text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.latest.monthly.dividend")}</th>
										<th class="text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.average.monthly.dividend")}</th>
										<th class="text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.taxable.base.ratio")}</th>
										<th class="text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.quantity")}</th>
										<th class="text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.average.buy.price")}</th>
										<th class="text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.expected.monthly.dividend")}</th>
										<th class="text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.expected.monthly.yield")}</th>
										<th class="text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.expected.annual.yield")}</th>
										<th class="text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.expected.monthly.yield.on.cost")}</th>
										<th class="text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.expected.annual.yield.on.cost")}</th>
										<th class="text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.expected.taxable.base")}</th>
										<th class="text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.total.return.on.cost")}</th>
										<th class="text-right">${MessageUtil.getMessage("stock.simulator.monthly.table.header.expected.combined.return")}</th>
									</tr>
								</thead>
								<tbody>
									@for(MonthlyDividendSnapshotResponse row : monthlyDividendRows)
										<tr>
											<td class="font-medium">@if(bestChoice != null && bestChoice.id() != null && bestChoice.id().equals(row.id()))<span class="badge badge-success badge-xs mr-2">${MessageUtil.getMessage("stock.simulator.compare.best.choice")}</span>@endif${row.stockItemSymbol()}</td>
											<td>${row.stockItemName()}</td>
											<td class="text-right">₩${amountFormat.format(row.currentPrice() != null ? row.currentPrice() : BigDecimal.ZERO)}</td>
											<td class="text-right">₩${amountFormat.format(row.latestMonthlyDividendPerShare() != null ? row.latestMonthlyDividendPerShare() : BigDecimal.ZERO)}</td>
											<td class="text-right">₩${amountFormat.format(row.averageMonthlyDividendPerShare1y() != null ? row.averageMonthlyDividendPerShare1y() : BigDecimal.ZERO)}</td>
											<td class="text-right">${percentFormat.format(row.averageTaxableBaseRatio1y() != null ? row.averageTaxableBaseRatio1y() : BigDecimal.ZERO)}%</td>
											<td class="text-right">${amountFormat.format(row.heldQuantity() != null ? row.heldQuantity() : 0)}</td>
											<td class="text-right">₩${amountFormat.format(row.averageBuyPrice() != null ? row.averageBuyPrice() : BigDecimal.ZERO)}</td>
											<td class="text-right">₩${amountFormat.format(row.expectedMonthlyDividend() != null ? row.expectedMonthlyDividend() : BigDecimal.ZERO)}</td>
											<td class="text-right">${percentFormat.format(row.expectedMonthlyYieldPct() != null ? row.expectedMonthlyYieldPct() : BigDecimal.ZERO)}%</td>
											<td class="text-right">${percentFormat.format(row.expectedAnnualYieldPct() != null ? row.expectedAnnualYieldPct() : BigDecimal.ZERO)}%</td>
											<td class="text-right">${percentFormat.format(row.expectedMonthlyYieldOnCostPct() != null ? row.expectedMonthlyYieldOnCostPct() : BigDecimal.ZERO)}%</td>
											<td class="text-right">${percentFormat.format(row.expectedAnnualYieldOnCostPct() != null ? row.expectedAnnualYieldOnCostPct() : BigDecimal.ZERO)}%</td>
											<td class="text-right">₩${amountFormat.format(row.expectedTaxableBaseAmount() != null ? row.expectedTaxableBaseAmount() : BigDecimal.ZERO)}</td>
											<td class="text-right ${row.totalReturnOnCostPct() != null && row.totalReturnOnCostPct().signum() >= 0 ? "text-success" : "text-error"}">${percentFormat.format(row.totalReturnOnCostPct() != null ? row.totalReturnOnCostPct() : BigDecimal.ZERO)}%</td>
											<td class="text-right font-semibold ${row.expectedCombinedReturnPct() != null && row.expectedCombinedReturnPct().signum() >= 0 ? "text-success" : "text-error"}">${percentFormat.format(row.expectedCombinedReturnPct() != null ? row.expectedCombinedReturnPct() : BigDecimal.ZERO)}%</td>
										</tr>
									@endfor
								</tbody>
							</table>
						</div>
					@endif
				</div>
			</section>
		</div>
	</div>
</div>