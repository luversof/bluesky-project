@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"

!{
	String pageTitle = MessageUtil.getMessage("stock.page.simulator.title");
	String description = MessageUtil.getMessage("stock.simulator.description");
	String storageNote = MessageUtil.getMessage("stock.simulator.local.storage.note");
	String toolTitle = MessageUtil.getMessage("stock.simulator.tool.dividend.title");
	String toolDescription = MessageUtil.getMessage("stock.simulator.tool.dividend.desc");
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
	String compareTitle = MessageUtil.getMessage("stock.simulator.section.compare.title");
	String compareDescription = MessageUtil.getMessage("stock.simulator.section.compare.desc");
	String assumptionsTitle = MessageUtil.getMessage("stock.simulator.section.assumptions.title");
	String yearlyTitle = MessageUtil.getMessage("stock.simulator.section.yearly.title");
	String yearlyDescription = MessageUtil.getMessage("stock.simulator.section.yearly.desc");
}

@template._layout.stockLayout(
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentStock = @`
		<div id="stockSimulatorApp" class="container mx-auto space-y-6 p-4">
			<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
			<script>
				window.stockSimulatorI18n = {
					defaultScenarioName: '${MessageUtil.getMessage("stock.simulator.default.scenario.name")}',
					activeScenario: '${MessageUtil.getMessage("stock.simulator.state.active")}',
					localOnly: '${MessageUtil.getMessage("stock.simulator.state.local.only")}',
					reinvestOn: '${MessageUtil.getMessage("stock.simulator.state.reinvest.on")}',
					reinvestOff: '${MessageUtil.getMessage("stock.simulator.state.reinvest.off")}',
					emptyScenario: '${MessageUtil.getMessage("stock.simulator.empty.no.scenario")}',
					emptyTable: '${MessageUtil.getMessage("stock.simulator.empty.no.table")}',
					deleteConfirm: '${MessageUtil.getMessage("stock.simulator.dialog.delete.confirm")}',
					maxScenarios: '${MessageUtil.getMessage("stock.simulator.alert.max.scenarios")}',
					summaryFinalWealth: '${MessageUtil.getMessage("stock.simulator.summary.final.wealth")}',
					summaryAnnualDividend: '${MessageUtil.getMessage("stock.simulator.summary.annual.dividend")}',
					summaryCumulativeDividend: '${MessageUtil.getMessage("stock.simulator.summary.cumulative.dividend")}',
					summaryDepletionYear: '${MessageUtil.getMessage("stock.simulator.summary.depletion.year")}',
					summaryNotDepleted: '${MessageUtil.getMessage("stock.simulator.summary.not.depleted")}',
					summaryDeficitStart: '${MessageUtil.getMessage("stock.simulator.summary.deficit.start")}',
					summaryNoDeficit: '${MessageUtil.getMessage("stock.simulator.summary.no.deficit")}',
					summaryYearsLater: '${MessageUtil.getMessage("stock.simulator.summary.years.later")}',
					summaryYieldOnCost: '${MessageUtil.getMessage("stock.simulator.summary.yield.on.cost")}',
					tooltipPrincipalReturn: '${MessageUtil.getMessage("stock.simulator.tooltip.principal.return")}',
					tableScenario: '${MessageUtil.getMessage("stock.simulator.table.header.scenario")}',
					tableTotalReturn: '${MessageUtil.getMessage("stock.simulator.table.header.total.return")}',
					tableFinalWealth: '${MessageUtil.getMessage("stock.simulator.table.header.final.wealth")}',
					tableFinalAnnualDividend: '${MessageUtil.getMessage("stock.simulator.table.header.final.annual.dividend")}',
					tableCumulativeDividends: '${MessageUtil.getMessage("stock.simulator.table.header.cumulative.dividends")}',
					tableShareCount: '${MessageUtil.getMessage("stock.simulator.table.header.share.count")}'
				};
			</script>

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

			<div class="grid gap-6 xl:grid-cols-[20rem_minmax(0,1fr)]">
				<section class="card border border-base-300 bg-base-100 shadow-sm">
					<div class="card-body gap-4">
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
					</div>
				</section>

				<div class="space-y-6">
					<section class="card border border-base-300 bg-base-100 shadow-sm">
						<div class="card-body gap-5">
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
						</div>
					</section>

					<div id="stockSimulatorSummaryCards" class="grid gap-4 md:grid-cols-2 xl:grid-cols-5"></div>

					<section class="card border border-base-300 bg-base-100 shadow-sm">
						<div class="card-body gap-4">
							<div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
								<div class="space-y-2">
									<h2 class="card-title text-lg">${chartTitle}</h2>
									<p class="text-sm leading-6 text-base-content/60">${chartDescription}</p>
								</div>
								<div id="stockSimulatorMetricSelector" class="flex flex-wrap gap-2">
									<button type="button" class="btn btn-sm" data-metric="totalWealth">${MessageUtil.getMessage("stock.simulator.metric.total.wealth")}</button>
									<button type="button" class="btn btn-sm" data-metric="marketValue">${MessageUtil.getMessage("stock.simulator.metric.market.value")}</button>
									<button type="button" class="btn btn-sm" data-metric="annualDividend">${MessageUtil.getMessage("stock.simulator.metric.annual.dividend")}</button>
									<button type="button" class="btn btn-sm" data-metric="cumulativeDividends">${MessageUtil.getMessage("stock.simulator.metric.cumulative.dividend")}</button>
									<button type="button" class="btn btn-sm" data-metric="shareCount">${MessageUtil.getMessage("stock.simulator.metric.share.count")}</button>
								</div>
							</div>
							<div class="h-96 w-full rounded-2xl bg-base-100 p-2">
								<canvas id="stockSimulatorChart"></canvas>
							</div>
						</div>
					</section>
				</div>
			</div>

			<div class="grid gap-6 xl:grid-cols-[minmax(0,1fr)_20rem]">
				<section class="card border border-base-300 bg-base-100 shadow-sm">
					<div class="card-body gap-4">
						<div class="space-y-2">
							<h2 class="card-title text-lg">${compareTitle}</h2>
							<p class="text-sm leading-6 text-base-content/60">${compareDescription}</p>
						</div>
						<div class="overflow-x-auto">
							<table class="table table-zebra table-sm">
								<thead>
									<tr>
										<th>${MessageUtil.getMessage("stock.simulator.table.header.scenario")}</th>
										<th>${MessageUtil.getMessage("stock.simulator.table.header.total.return")}</th>
										<th>${MessageUtil.getMessage("stock.simulator.table.header.final.wealth")}</th>
										<th>${MessageUtil.getMessage("stock.simulator.table.header.final.annual.dividend")}</th>
										<th>${MessageUtil.getMessage("stock.simulator.table.header.cumulative.dividends")}</th>
										<th>${MessageUtil.getMessage("stock.simulator.table.header.share.count")}</th>
									</tr>
								</thead>
								<tbody id="stockSimulatorCompareTableBody"></tbody>
							</table>
						</div>
					</div>
				</section>

				<aside class="card border border-base-300 bg-base-100 shadow-sm">
					<div class="card-body gap-4">
						<h2 class="card-title text-lg">${assumptionsTitle}</h2>
						<ul class="list-disc space-y-3 pl-5 text-sm leading-6 text-base-content/70">
							<li>${MessageUtil.getMessage("stock.simulator.assumption.one")}</li>
							<li>${MessageUtil.getMessage("stock.simulator.assumption.two")}</li>
							<li>${MessageUtil.getMessage("stock.simulator.assumption.three")}</li>
							<li>${MessageUtil.getMessage("stock.simulator.assumption.four")}</li>
							<li>${MessageUtil.getMessage("stock.simulator.assumption.five")}</li>
						</ul>
					</div>
				</aside>
			</div>

			<section class="card border border-base-300 bg-base-100 shadow-sm">
				<div class="card-body gap-4">
					<div class="space-y-2">
						<h2 class="card-title text-lg">${yearlyTitle}</h2>
						<p class="text-sm leading-6 text-base-content/60">${yearlyDescription}</p>
					</div>
					<div class="overflow-x-auto">
						<table class="table table-pin-rows table-zebra table-sm">
							<thead>
								<tr>
									<th>${MessageUtil.getMessage("stock.simulator.table.header.year")}</th>
									<th>${MessageUtil.getMessage("stock.simulator.table.header.share.price")}</th>
									<th>${MessageUtil.getMessage("stock.simulator.table.header.shares")}</th>
									<th>${MessageUtil.getMessage("stock.simulator.table.header.reinvested.shares")}</th>
									<th>${MessageUtil.getMessage("stock.simulator.table.header.annual.dividend")}</th>
									<th>${MessageUtil.getMessage("stock.simulator.table.header.annual.spending")}</th>
									<th>${MessageUtil.getMessage("stock.simulator.table.header.reinvestable.dividend")}</th>
									<th>${MessageUtil.getMessage("stock.simulator.table.header.cumulative.dividend")}</th>
									<th>${MessageUtil.getMessage("stock.simulator.table.header.market.value")}</th>
									<th>${MessageUtil.getMessage("stock.simulator.table.header.total.wealth")}</th>
								</tr>
							</thead>
							<tbody id="stockSimulatorYearlyTableBody"></tbody>
						</table>
					</div>
				</div>
			</section>

			<script src="/js/stock/stockSimulator.js"></script>
		</div>
	`
)