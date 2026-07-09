@import io.github.luversof.boot.context.support.MessageUtil

@param String storageNote = ""

!{
	String inputsTitle = MessageUtil.getMessage("stock.simulator.compound.section.inputs.title");
	String inputsDescription = MessageUtil.getMessage("stock.simulator.compound.section.inputs.desc");
	String initialLabel = MessageUtil.getMessage("stock.simulator.compound.field.initial");
	String contributionLabel = MessageUtil.getMessage("stock.simulator.compound.field.contribution");
	String timingLabel = MessageUtil.getMessage("stock.simulator.compound.field.timing");
	String timingBeginLabel = MessageUtil.getMessage("stock.simulator.compound.timing.begin");
	String timingEndLabel = MessageUtil.getMessage("stock.simulator.compound.timing.end");
	String rateLabel = MessageUtil.getMessage("stock.simulator.compound.field.rate");
	String yearsLabel = MessageUtil.getMessage("stock.simulator.compound.field.years");
	String summaryFinalLabel = MessageUtil.getMessage("stock.simulator.compound.summary.final");
	String summaryPrincipalLabel = MessageUtil.getMessage("stock.simulator.compound.summary.principal");
	String summaryProfitLabel = MessageUtil.getMessage("stock.simulator.compound.summary.profit");
	String summaryReturnLabel = MessageUtil.getMessage("stock.simulator.compound.summary.return");
	String chartTitle = MessageUtil.getMessage("stock.simulator.compound.chart.title");
	String chartDescription = MessageUtil.getMessage("stock.simulator.compound.chart.desc");
	String seriesPrincipalLabel = MessageUtil.getMessage("stock.simulator.compound.series.principal");
	String seriesProfitLabel = MessageUtil.getMessage("stock.simulator.compound.series.profit");
	String yearlyTitle = MessageUtil.getMessage("stock.simulator.compound.section.yearly.title");
	String yearlyDescription = MessageUtil.getMessage("stock.simulator.compound.section.yearly.desc");
	String yearHeader = MessageUtil.getMessage("stock.simulator.table.header.year");
	String contributionHeader = MessageUtil.getMessage("stock.simulator.compound.table.contribution");
	String principalHeader = MessageUtil.getMessage("stock.simulator.compound.table.principal");
	String interestHeader = MessageUtil.getMessage("stock.simulator.compound.table.interest");
	String balanceHeader = MessageUtil.getMessage("stock.simulator.compound.table.balance");
}

<div id="stockCompoundSimulator" class="grid gap-6 xl:grid-cols-[24rem_minmax(0,1fr)]" data-series-principal="${seriesPrincipalLabel}" data-series-profit="${seriesProfitLabel}">
	@template._components.ui.card(bodyClass = "gap-4", body = @`
			<div class="flex flex-col gap-2">
				<div class="space-y-2">
					<h2 class="card-title text-lg">${inputsTitle}</h2>
					<p class="text-sm leading-6 text-base-content/60">${inputsDescription}</p>
				</div>
				<div class="badge badge-outline">${storageNote}</div>
			</div>
			<form id="stockCompoundForm" class="grid gap-4">
				<label class="form-control">
					<div class="label justify-between gap-3">
						<span class="label-text font-medium">${initialLabel}</span>
						<span id="stockCompoundInitialPreview" class="label-text-alt text-right font-medium text-base-content/60"></span>
					</div>
					<input id="stockCompoundInitial" name="initial" type="number" min="0" step="10000" value="0" class="input input-bordered w-full" />
				</label>
				<label class="form-control">
					<div class="label justify-between gap-3">
						<span class="label-text font-medium">${contributionLabel}</span>
						<span id="stockCompoundContributionPreview" class="label-text-alt text-right font-medium text-base-content/60"></span>
					</div>
					<input id="stockCompoundContribution" name="contribution" type="number" min="0" step="10000" value="20000000" class="input input-bordered w-full" />
				</label>
				<label class="form-control">
					<div class="label"><span class="label-text font-medium">${timingLabel}</span></div>
					<select id="stockCompoundTiming" name="timing" class="select select-bordered w-full">
						<option value="begin" selected>${timingBeginLabel}</option>
						<option value="end">${timingEndLabel}</option>
					</select>
				</label>
				<label class="form-control">
					<div class="label"><span class="label-text font-medium">${rateLabel}</span></div>
					<input id="stockCompoundRatePct" name="ratePct" type="number" min="-100" max="200" step="0.1" value="8" class="input input-bordered w-full" />
				</label>
				<label class="form-control">
					<div class="label"><span class="label-text font-medium">${yearsLabel}</span></div>
					<input id="stockCompoundYears" name="years" type="number" min="1" max="100" step="1" value="3" class="input input-bordered w-full" />
				</label>
			</form>
	`)

	<div class="space-y-6">
		<div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
			@template._components.ui.card(bodyClass = "gap-1 py-4", body = @`
					<div class="text-xs font-semibold text-base-content/50">${summaryFinalLabel}</div>
					<div id="stockCompoundFinalValue" class="font-mono text-xl font-bold tabular-nums">-</div>
			`)
			@template._components.ui.card(bodyClass = "gap-1 py-4", body = @`
					<div class="text-xs font-semibold text-base-content/50">${summaryPrincipalLabel}</div>
					<div id="stockCompoundTotalPrincipal" class="font-mono text-xl font-bold tabular-nums">-</div>
			`)
			@template._components.ui.card(bodyClass = "gap-1 py-4", body = @`
					<div class="text-xs font-semibold text-base-content/50">${summaryProfitLabel}</div>
					<div id="stockCompoundTotalProfit" class="font-mono text-xl font-bold tabular-nums text-profit">-</div>
			`)
			@template._components.ui.card(bodyClass = "gap-1 py-4", body = @`
					<div class="text-xs font-semibold text-base-content/50">${summaryReturnLabel}</div>
					<div id="stockCompoundTotalReturn" class="font-mono text-xl font-bold tabular-nums text-profit">-</div>
			`)
		</div>

		@template._components.ui.card(bodyClass = "gap-4", body = @`
				<div class="space-y-2">
					<h3 class="card-title text-base">${chartTitle}</h3>
					<p class="text-sm leading-6 text-base-content/60">${chartDescription}</p>
				</div>
				<div class="h-80 w-full rounded-2xl bg-base-100 p-2">
					<canvas id="stockCompoundGrowthChart"></canvas>
				</div>
		`)

		@template._components.ui.card(bodyClass = "gap-4", body = @`
				<div class="space-y-2">
					<h3 class="card-title text-base">${yearlyTitle}</h3>
					<p class="text-sm leading-6 text-base-content/60">${yearlyDescription}</p>
				</div>
				<div class="overflow-x-auto">
					<table class="table table-zebra table-sm">
						<thead>
							<tr>
								<th scope="col">${yearHeader}</th>
								<th scope="col" class="text-right">${contributionHeader}</th>
								<th scope="col" class="text-right">${principalHeader}</th>
								<th scope="col" class="text-right">${interestHeader}</th>
								<th scope="col" class="text-right">${balanceHeader}</th>
							</tr>
						</thead>
						<tbody id="stockCompoundYearlyTableBody"></tbody>
					</table>
				</div>
		`)
	</div>
</div>
