@import net.luversof.web.gate.util.CspNonceHolder
@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.util.List
@import java.util.Map
@import net.luversof.web.gate.stock.domain.StockItem
@import net.luversof.web.gate.stock.dto.request.MonthlyDividendSnapshotUpsertRequest
@import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse
@import net.luversof.web.gate.stock.dto.view.MonthlyDividendSimulatorSummaryView

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String simulatorTab = "sustainability"
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
	boolean monthlyDividendTab = "monthly-dividend".equals(simulatorTab);
	boolean compoundTab = "compound".equals(simulatorTab);
	String pageTitle = compoundTab
		? MessageUtil.getMessage("stock.page.simulator.compound.title")
		: MessageUtil.getMessage("stock.page.simulator.title");
	String description = compoundTab
		? MessageUtil.getMessage("stock.simulator.compound.description")
		: (monthlyDividendTab
			? MessageUtil.getMessage("stock.simulator.monthly.description")
			: MessageUtil.getMessage("stock.simulator.description"));
	String storageNote = compoundTab
		? MessageUtil.getMessage("stock.simulator.compound.storage.note")
		: (monthlyDividendTab
			? MessageUtil.getMessage("stock.simulator.monthly.storage.note")
			: MessageUtil.getMessage("stock.simulator.local.storage.note"));
	String toolTitle = compoundTab
		? MessageUtil.getMessage("stock.simulator.compound.tool.title")
		: (monthlyDividendTab
			? MessageUtil.getMessage("stock.simulator.monthly.tool.title")
			: MessageUtil.getMessage("stock.simulator.tool.dividend.title"));
	String toolDescription = compoundTab
		? MessageUtil.getMessage("stock.simulator.compound.tool.desc")
		: (monthlyDividendTab
			? MessageUtil.getMessage("stock.simulator.monthly.tool.desc")
			: MessageUtil.getMessage("stock.simulator.tool.dividend.desc"));
	String sustainabilityTabLabel = MessageUtil.getMessage("stock.simulator.tab.sustainability");
	String monthlyDividendTabLabel = MessageUtil.getMessage("stock.simulator.tab.monthly.dividend");
	String compoundTabLabel = MessageUtil.getMessage("stock.simulator.tab.compound");
	String scenariosTitle = MessageUtil.getMessage("stock.simulator.section.scenarios.title");
	String scenariosDescription = MessageUtil.getMessage("stock.simulator.section.scenarios.desc");
	String addScenario = MessageUtil.getMessage("stock.simulator.button.add");
	String duplicateScenario = MessageUtil.getMessage("stock.simulator.button.duplicate");
	String deleteScenario = MessageUtil.getMessage("stock.simulator.button.delete");
	String resetStorage = MessageUtil.getMessage("stock.simulator.button.reset");
	String inputsTitle = MessageUtil.getMessage("stock.simulator.section.inputs.title");
	String inputsDescription = MessageUtil.getMessage("stock.simulator.section.inputs.desc");
	String chartTitle = MessageUtil.getMessage("stock.simulator.section.chart.title");
	String chartDescription = MessageUtil.getMessage("stock.simulator.section.chart.desc");
	String cashFlowTitle = MessageUtil.getMessage("stock.simulator.section.cashflow.title");
	String cashFlowDescription = MessageUtil.getMessage("stock.simulator.section.cashflow.desc");
	String assetTitle = MessageUtil.getMessage("stock.simulator.section.asset.title");
	String assetDescription = MessageUtil.getMessage("stock.simulator.section.asset.desc");
	String compareTitle = MessageUtil.getMessage("stock.simulator.section.compare.title");
	String compareDescription = MessageUtil.getMessage("stock.simulator.section.compare.desc");
	String assumptionsTitle = MessageUtil.getMessage("stock.simulator.section.assumptions.title");
	String yearlyTitle = MessageUtil.getMessage("stock.simulator.section.yearly.title");
	String yearlyDescription = MessageUtil.getMessage("stock.simulator.section.yearly.desc");
	String activeSimulatorTabClass = "tab tab-lg flex-1 font-bold tab-active !bg-primary !text-primary-content shadow-sm";
	String inactiveSimulatorTabClass = "tab tab-lg flex-1 text-base-content/60 hover:text-base-content";
}

@template._layout.stockLayout(
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentStock = @`
		<div id="stockSimulatorApp" class="space-y-6">
			@if(!monthlyDividendTab && !compoundTab)
				<script nonce="${CspNonceHolder.getNonce()}">
					window.stockSimulatorI18n = {
						defaultScenarioName: '${MessageUtil.getMessage("stock.simulator.default.scenario.name")}',
						activeScenario: '${MessageUtil.getMessage("stock.simulator.state.active")}',
						localOnly: '${MessageUtil.getMessage("stock.simulator.state.local.only")}',
						fieldPrincipal: '${MessageUtil.getMessage("stock.simulator.field.principal")}',
						fieldCurrentPrice: '${MessageUtil.getMessage("stock.simulator.field.current.price")}',
						fieldAnnualPriceGrowth: '${MessageUtil.getMessage("stock.simulator.field.annual.price.growth")}',
						fieldDividendYield: '${MessageUtil.getMessage("stock.simulator.field.dividend.yield")}',
						fieldAnnualDividendGrowth: '${MessageUtil.getMessage("stock.simulator.field.annual.dividend.growth")}',
						fieldAnnualSpending: '${MessageUtil.getMessage("stock.simulator.field.annual.spending")}',
						fieldAnnualSpendingGrowth: '${MessageUtil.getMessage("stock.simulator.field.annual.spending.growth")}',
						reinvestOn: '${MessageUtil.getMessage("stock.simulator.state.reinvest.on")}',
						reinvestOff: '${MessageUtil.getMessage("stock.simulator.state.reinvest.off")}',
						emptyScenario: '${MessageUtil.getMessage("stock.simulator.empty.no.scenario")}',
						emptyTable: '${MessageUtil.getMessage("stock.simulator.empty.no.table")}',
						emptyMonthlyTable: '${MessageUtil.getMessage("stock.simulator.empty.no.monthly.table")}',
						deleteConfirm: '${MessageUtil.getMessage("stock.simulator.dialog.delete.confirm")}',
						maxScenarios: '${MessageUtil.getMessage("stock.simulator.alert.max.scenarios")}',
						yearlyToggleOpen: '${MessageUtil.getMessage("stock.simulator.toggle.open.monthly")}',
						yearlyToggleClose: '${MessageUtil.getMessage("stock.simulator.toggle.close.monthly")}',
						monthlyDetailsTitle: '${MessageUtil.getMessage("stock.simulator.monthly.details.title")}',
						tableHeaderMonth: '${MessageUtil.getMessage("stock.simulator.table.header.month")}',
						tableHeaderSpendingCoverage: '${MessageUtil.getMessage("stock.simulator.metric.spending.coverage")}',
						tableHeaderYearEndPrice: '${MessageUtil.getMessage("stock.simulator.table.header.year.end.price")}',
						tableHeaderMonthlyDividend: '${MessageUtil.getMessage("stock.simulator.table.header.monthly.dividend")}',
						tableHeaderMonthlySpending: '${MessageUtil.getMessage("stock.simulator.table.header.monthly.spending")}',
						tableHeaderMonthlyGap: '${MessageUtil.getMessage("stock.simulator.table.header.monthly.gap")}',
						tableHeaderSoldShares: '${MessageUtil.getMessage("stock.simulator.metric.sold.shares")}',
						tableHeaderReinvestedShares: '${MessageUtil.getMessage("stock.simulator.table.header.reinvested.shares")}',
						tableHeaderMonthEndPrice: '${MessageUtil.getMessage("stock.simulator.table.header.month.end.price")}',
						tableHeaderMonthEndShares: '${MessageUtil.getMessage("stock.simulator.table.header.month.end.shares")}',
						tableHeaderMonthEndCashReserve: '${MessageUtil.getMessage("stock.simulator.table.header.month.end.cash.reserve")}',
						tableHeaderMonthEndMarketValue: '${MessageUtil.getMessage("stock.simulator.table.header.month.end.market.value")}',
						tableHeaderMonthEndTotalWealth: '${MessageUtil.getMessage("stock.simulator.table.header.month.end.total.wealth")}',
						yearlyDetailGuide: '${MessageUtil.getMessage("stock.simulator.yearly.detail.guide")}',
						compareBestChoice: '${MessageUtil.getMessage("stock.simulator.compare.best.choice")}',
						summarySustainablePeriod: '${MessageUtil.getMessage("stock.simulator.summary.sustainable.period")}',
						summaryFinalWealth: '${MessageUtil.getMessage("stock.simulator.summary.final.wealth")}',
						summaryDepletionYear: '${MessageUtil.getMessage("stock.simulator.summary.depletion.year")}',
						summaryNotDepleted: '${MessageUtil.getMessage("stock.simulator.summary.not.depleted")}',
						summaryDeficitStart: '${MessageUtil.getMessage("stock.simulator.summary.deficit.start")}',
						summaryNoDeficit: '${MessageUtil.getMessage("stock.simulator.summary.no.deficit")}',
						summaryWealthDeclineStart: '${MessageUtil.getMessage("stock.simulator.summary.wealth.decline.start")}',
						summaryNoWealthDecline: '${MessageUtil.getMessage("stock.simulator.summary.no.wealth.decline")}',
						summaryCapitalDrawdownStart: '${MessageUtil.getMessage("stock.simulator.summary.capital.drawdown.start")}',
						summaryNoPrincipalDrawdown: '${MessageUtil.getMessage("stock.simulator.summary.no.capital.drawdown")}',
						summaryYearsLater: '${MessageUtil.getMessage("stock.simulator.summary.years.later")}',
						summaryYearsOrMore: '${MessageUtil.getMessage("stock.simulator.summary.years.or.more")}',
						summaryWithinHorizon: '${MessageUtil.getMessage("stock.simulator.summary.within.horizon")}',
						summaryLatestCoverage: '${MessageUtil.getMessage("stock.simulator.summary.latest.coverage")}',
						summaryNoSpending: '${MessageUtil.getMessage("stock.simulator.summary.no.spending")}',
						seriesAnnualDividend: '${MessageUtil.getMessage("stock.simulator.table.header.annual.dividend")}',
						seriesAnnualSpending: '${MessageUtil.getMessage("stock.simulator.table.header.annual.spending")}',
						seriesAnnualGap: '${MessageUtil.getMessage("stock.simulator.metric.annual.gap")}',
						seriesTotalWealth: '${MessageUtil.getMessage("stock.simulator.table.header.total.wealth")}',
						seriesMarketValue: '${MessageUtil.getMessage("stock.simulator.table.header.market.value")}',
						seriesCashReserve: '${MessageUtil.getMessage("stock.simulator.metric.cash.reserve")}',
						timelinePhaseStable: '${MessageUtil.getMessage("stock.simulator.timeline.phase.stable")}',
						timelinePhaseDeficit: '${MessageUtil.getMessage("stock.simulator.timeline.phase.deficit")}',
						timelinePhaseWealthDecline: '${MessageUtil.getMessage("stock.simulator.timeline.phase.wealth.decline")}',
						timelinePhaseDrawdown: '${MessageUtil.getMessage("stock.simulator.timeline.phase.drawdown")}',
						timelinePhaseDepleted: '${MessageUtil.getMessage("stock.simulator.timeline.phase.depleted")}'
					};
				</script>
			@endif

			<div class="rounded-3xl border border-base-300 bg-gradient-to-br from-base-100 via-base-100 to-base-200 shadow-sm">
				<div class="flex flex-col gap-6 px-6 py-6 lg:flex-row lg:items-end lg:justify-between">
					<div class="space-y-3">
						<div class="badge badge-primary badge-outline">${toolTitle}</div>
						<h1 class="text-3xl font-bold tracking-tight">${pageTitle}</h1>
						<p class="max-w-3xl text-sm leading-6 text-base-content/70">${description}</p>
					</div>
					<div class="max-w-sm rounded-2xl border border-primary/20 bg-primary/5 px-4 py-4 text-sm leading-6 text-base-content/70">
						<p class="font-semibold text-base-content">${toolDescription}</p>
						<p class="mt-2">${storageNote}</p>
					</div>
				</div>
			</div>

			<div class="tabs tabs-box w-full bg-base-200/70 p-1">
				<a href="/stock/simulator?tab=sustainability" class="${!monthlyDividendTab && !compoundTab ? activeSimulatorTabClass : inactiveSimulatorTabClass}">${sustainabilityTabLabel}</a>
				<a href="/stock/simulator?tab=monthly-dividend" class="${monthlyDividendTab ? activeSimulatorTabClass : inactiveSimulatorTabClass}">${monthlyDividendTabLabel}</a>
				<a href="/stock/simulator?tab=compound" class="${compoundTab ? activeSimulatorTabClass : inactiveSimulatorTabClass}">${compoundTabLabel}</a>
			</div>

			@if(monthlyDividendTab)
				@template.stock.fragments.monthlyDividendSimulator(
					monthlyDividendRows = monthlyDividendRows,
					monthlyDividendSummary = monthlyDividendSummary,
					stockItems = stockItems,
					monthlyDividendForm = monthlyDividendForm,
					monthlyDividendBulkInput = monthlyDividendBulkInput,
					monthlyDividendErrorMessage = monthlyDividendErrorMessage,
					monthlyDividendResult = monthlyDividendResult,
					monthlyDividendSavedCount = monthlyDividendSavedCount,
					monthlyDividendSort = monthlyDividendSort,
					monthlyDividendDirection = monthlyDividendDirection,
					monthlyDividendKeyword = monthlyDividendKeyword,
					monthlyDividendMinAnnualYield = monthlyDividendMinAnnualYield,
					monthlyDividendPositiveOnly = monthlyDividendPositiveOnly,
					monthlyDividendHasSavedRows = monthlyDividendHasSavedRows,
					monthlyDividendProfileDisplayOrders = monthlyDividendProfileDisplayOrders,
					monthlyDividendProfileOrderedSymbols = monthlyDividendProfileOrderedSymbols,
					monthlyDividendReorderEnabled = monthlyDividendReorderEnabled
				)
				<script type="module" src="/js/stock/monthlyDividendProfileOrder.js"></script>
			@elseif(compoundTab)
				@template.stock.fragments.compoundSimulator(storageNote = storageNote)
				<script src="/js/stock/compoundSimulator.js"></script>
			@else
			<div class="grid gap-6 xl:grid-cols-[20rem_minmax(0,1fr)]">
				@template._components.ui.card(bodyClass = "gap-4", body = @`
						<div class="space-y-2">
							<h2 class="card-title text-lg">${scenariosTitle}</h2>
							<p class="text-sm leading-6 text-base-content/60">${scenariosDescription}</p>
						</div>
						<button id="stockSimulatorAddScenario" type="button" class="btn btn-primary btn-sm w-full">${addScenario}</button>
						<div id="stockSimulatorScenarioList" class="flex flex-col gap-3"></div>
						<div class="grid grid-cols-2 gap-2">
							<button id="stockSimulatorDuplicateScenario" type="button" class="btn btn-outline btn-sm">${duplicateScenario}</button>
							<button id="stockSimulatorDeleteScenario" type="button" class="btn btn-outline btn-sm btn-error">${deleteScenario}</button>
							<button id="stockSimulatorResetStorage" type="button" class="btn btn-ghost btn-sm col-span-2">${resetStorage}</button>
						</div>
				`)

				<div class="space-y-6">
					@template._components.ui.card(bodyClass = "gap-5", body = @`
							<div class="flex flex-col gap-2 lg:flex-row lg:items-center lg:justify-between">
								<div class="space-y-2">
									<h2 class="card-title text-lg">${inputsTitle}</h2>
									<p class="text-sm leading-6 text-base-content/60">${inputsDescription}</p>
								</div>
								<div class="badge badge-outline badge-lg">${storageNote}</div>
							</div>

							<form id="stockSimulatorForm" class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
								<label class="form-control">
									<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.simulator.field.name")}</span></div>
									<input id="stockSimulatorName" name="name" type="text" maxlength="40" class="input input-bordered w-full" />
								</label>
								<label class="form-control">
									<div class="label justify-between gap-3">
										<span class="label-text font-medium">${MessageUtil.getMessage("stock.simulator.field.principal")}</span>
										<span id="stockSimulatorPrincipalPreview" class="label-text-alt text-right font-medium text-base-content/60"></span>
									</div>
									<input id="stockSimulatorPrincipal" name="principal" type="number" min="10000" step="10000" class="input input-bordered w-full" />
								</label>
								<label class="form-control">
									<div class="label justify-between gap-3">
										<span class="label-text font-medium">${MessageUtil.getMessage("stock.simulator.field.current.price")}</span>
										<span id="stockSimulatorCurrentPricePreview" class="label-text-alt text-right font-medium text-base-content/60"></span>
									</div>
									<input id="stockSimulatorCurrentPrice" name="currentPrice" type="number" min="1" step="1" class="input input-bordered w-full" />
								</label>
								<label class="form-control">
									<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.simulator.field.annual.price.growth")}</span></div>
									<input id="stockSimulatorAnnualPriceGrowthPct" name="annualPriceGrowthPct" type="number" min="-100" max="200" step="0.1" class="input input-bordered w-full" />
								</label>
								<label class="form-control">
									<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.simulator.field.dividend.yield")}</span></div>
									<input id="stockSimulatorDividendYieldPct" name="dividendYieldPct" type="number" min="0" max="100" step="0.1" class="input input-bordered w-full" />
								</label>
								<label class="form-control">
									<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.simulator.field.annual.dividend.growth")}</span></div>
									<input id="stockSimulatorAnnualDividendGrowthPct" name="annualDividendGrowthPct" type="number" min="-100" max="200" step="0.1" class="input input-bordered w-full" />
								</label>
												<label class="form-control">
													<div class="label justify-between gap-3">
														<span class="label-text font-medium">${MessageUtil.getMessage("stock.simulator.field.annual.spending")}</span>
														<span id="stockSimulatorAnnualSpendingPreview" class="label-text-alt text-right font-medium text-base-content/60"></span>
													</div>
													<input id="stockSimulatorAnnualSpending" name="annualSpending" type="number" min="0" step="10000" class="input input-bordered w-full" />
												</label>
												<label class="form-control">
													<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.simulator.field.annual.spending.growth")}</span></div>
													<input id="stockSimulatorAnnualSpendingGrowthPct" name="annualSpendingGrowthPct" type="number" min="-100" max="200" step="0.1" class="input input-bordered w-full" />
												</label>
								<label class="form-control">
									<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.simulator.field.years")}</span></div>
									<input id="stockSimulatorYears" name="years" type="number" min="1" max="100" step="1" class="input input-bordered w-full" />
								</label>
								<div class="form-control md:col-span-2 xl:col-span-2">
									<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.simulator.field.reinvest")}</span></div>
									<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-4">
										<label class="label cursor-pointer justify-start gap-3 p-0">
											<input id="stockSimulatorReinvestDividends" name="reinvestDividends" type="checkbox" class="checkbox checkbox-primary" />
											<span class="label-text leading-6">${MessageUtil.getMessage("stock.simulator.field.reinvest.help")}</span>
										</label>
									</div>
								</div>
							</form>
					`)

					<div id="stockSimulatorSummaryCards" class="grid gap-4 md:grid-cols-2 xl:grid-cols-5"></div>

					<section class="space-y-4">
						<div class="space-y-2">
							<h2 class="text-lg font-semibold text-base-content">${chartTitle}</h2>
							<p class="text-sm leading-6 text-base-content/60">${chartDescription}</p>
						</div>
						<div class="grid gap-4 xl:grid-cols-2">
							@template._components.ui.card(bodyClass = "gap-4", body = @`
									<div class="space-y-2">
										<h3 class="card-title text-base">${cashFlowTitle}</h3>
										<p class="text-sm leading-6 text-base-content/60">${cashFlowDescription}</p>
									</div>
									<div class="h-80 w-full rounded-2xl bg-base-100 p-2">
										<canvas id="stockSimulatorCashFlowChart"></canvas>
									</div>
							`)
							@template._components.ui.card(bodyClass = "gap-4", body = @`
									<div class="space-y-2">
										<h3 class="card-title text-base">${assetTitle}</h3>
										<p class="text-sm leading-6 text-base-content/60">${assetDescription}</p>
									</div>
									<div class="h-80 w-full rounded-2xl bg-base-100 p-2">
										<canvas id="stockSimulatorAssetChart"></canvas>
									</div>
							`)
						</div>
					</section>
				</div>
			</div>

			<div class="grid gap-6 xl:grid-cols-[minmax(0,1fr)_20rem]">
				@template._components.ui.card(bodyClass = "gap-4", body = @`
						<div class="space-y-2">
							<h2 class="card-title text-lg">${compareTitle}</h2>
							<p class="text-sm leading-6 text-base-content/60">${compareDescription}</p>
						</div>
						<div class="flex flex-wrap gap-2 text-xs text-base-content/70">
							<span class="badge border-emerald-500 bg-emerald-50 text-emerald-700">${MessageUtil.getMessage("stock.simulator.timeline.phase.stable")}</span>
							<span class="badge border-amber-500 bg-amber-50 text-amber-700">${MessageUtil.getMessage("stock.simulator.timeline.phase.deficit")}</span>
							<span class="badge border-orange-500 bg-orange-50 text-orange-700">${MessageUtil.getMessage("stock.simulator.timeline.phase.drawdown")}</span>
							<span class="badge border-yellow-500 bg-yellow-50 text-yellow-700">${MessageUtil.getMessage("stock.simulator.timeline.phase.wealth.decline")}</span>
							<span class="badge border-red-500 bg-red-50 text-red-700">${MessageUtil.getMessage("stock.simulator.timeline.phase.depleted")}</span>
						</div>
						<div id="stockSimulatorTimelineList" class="space-y-3"></div>
				`)

				@template._components.ui.card(bodyClass = "gap-4", body = @`
						<h2 class="card-title text-lg">${assumptionsTitle}</h2>
						<ul class="list-disc space-y-3 pl-5 text-sm leading-6 text-base-content/70">
							<li>${MessageUtil.getMessage("stock.simulator.assumption.one")}</li>
							<li>${MessageUtil.getMessage("stock.simulator.assumption.two")}</li>
							<li>${MessageUtil.getMessage("stock.simulator.assumption.three")}</li>
							<li>${MessageUtil.getMessage("stock.simulator.assumption.four")}</li>
							<li>${MessageUtil.getMessage("stock.simulator.assumption.five")}</li>
							<li>${MessageUtil.getMessage("stock.simulator.assumption.six")}</li>
						</ul>
				`)
			</div>

			@template._components.ui.card(bodyClass = "gap-4", body = @`
					<div class="space-y-2">
						<h2 class="card-title text-lg">${yearlyTitle}</h2>
						<p class="text-sm leading-6 text-base-content/60">${yearlyDescription}</p>
					</div>
					<div class="overflow-x-auto">
						<table class="table table-pin-rows table-zebra table-sm">
							<thead>
								<tr>
									<th scope="col">${MessageUtil.getMessage("stock.simulator.table.header.year")}</th>
									<th scope="col">${MessageUtil.getMessage("stock.simulator.table.header.year.end.price")}</th>
									<th scope="col">${MessageUtil.getMessage("stock.simulator.table.header.annual.dividend")}</th>
									<th scope="col">${MessageUtil.getMessage("stock.simulator.table.header.annual.spending")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.simulator.metric.spending.coverage")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.simulator.metric.annual.gap")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.simulator.table.header.annual.sold.shares")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.simulator.table.header.annual.reinvested.shares")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.simulator.table.header.year.end.shares")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.simulator.table.header.year.end.cash.reserve")}</th>
									<th scope="col">${MessageUtil.getMessage("stock.simulator.table.header.year.end.market.value")}</th>
									<th scope="col">${MessageUtil.getMessage("stock.simulator.table.header.year.end.total.wealth")}</th>
								</tr>
							</thead>
							<tbody id="stockSimulatorYearlyTableBody"></tbody>
						</table>
					</div>
			`)

			<script src="/js/stock/stockSimulator.js"></script>
			@endif
		</div>
	`
)