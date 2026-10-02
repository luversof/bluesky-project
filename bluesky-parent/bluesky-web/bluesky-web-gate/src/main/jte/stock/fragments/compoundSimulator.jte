@import io.github.luversof.boot.context.support.MessageUtil


!{
	String inputsTitle = MessageUtil.getMessage("stock.simulator.compound.section.inputs.title");
	String inputsDescription = MessageUtil.getMessage("stock.simulator.compound.section.inputs.desc");
	String quickAddSmallLabel = MessageUtil.getMessage("stock.simulator.compound.quickadd.small");
	String quickAddLargeLabel = MessageUtil.getMessage("stock.simulator.compound.quickadd.large");
	// 증감 버튼은 필드마다 같은 글자(+100만·+1,000만·0)를 쓴다 - 실측 2026-09-12(접근성 트리): 여섯 버튼이
	// 세 이름을 두 번씩 나눠 가져 어느 칸을 건드리는지 알 수 없었다. 눈으로는 바로 위 칸을 보면 알지만 이름만 듣는 쪽은 모른다.
	String quickAddAriaPattern = MessageUtil.getMessage("stock.simulator.compound.quickadd.add.aria");
	String quickResetAriaPattern = MessageUtil.getMessage("stock.simulator.compound.quickadd.reset.aria");
	String initialLabel = MessageUtil.getMessage("stock.simulator.compound.field.initial");
	String contributionLabel = MessageUtil.getMessage("stock.simulator.compound.field.contribution");
	String frequencyLabel = MessageUtil.getMessage("stock.simulator.compound.field.frequency");
	String frequencyYearlyLabel = MessageUtil.getMessage("stock.simulator.compound.frequency.yearly");
	String frequencyMonthlyLabel = MessageUtil.getMessage("stock.simulator.compound.frequency.monthly");
	String monthlyNote = MessageUtil.getMessage("stock.simulator.compound.monthly.note");
	String effectiveRateTemplate = MessageUtil.getMessage("stock.simulator.compound.effective.rate");
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
	String ratioHeader = MessageUtil.getMessage("stock.simulator.compound.table.ratio");
	// 연말 자산이 0 이하인 해는 원금/수익 비율이 성립하지 않는다. 그때 대시만 남기면
	// 낭독기에는 하이픈 하나로만 간다 - 까닭을 함께 내려 준다.
	String ratioNoneLabel = MessageUtil.getMessage("stock.simulator.compound.table.ratio.none");
}

<div id="stockCompoundSimulator" class="grid gap-6 grid-cols-[minmax(0,1fr)] xl:grid-cols-[24rem_minmax(0,1fr)]" data-series-principal="${seriesPrincipalLabel}" data-series-profit="${seriesProfitLabel}" data-ratio-none="${ratioNoneLabel}">
	@template._components.ui.card(bodyClass = "gap-4", body = @`
			<div class="flex flex-col gap-2">
				<div class="space-y-2">
					<h2 class="card-title text-lg">${inputsTitle}</h2>
					<p class="text-sm leading-6 text-base-content/60">${inputsDescription}</p>
				</div>
				<%-- 저장 안내는 위 머리 상자에만 둔다(2026-10-01, 같은 문장이 한 화면에 두 번이었다). --%>
			</div>
			<%-- 우측 결과 영역과 높이 균형을 맞추기 위해 md 이상에서 2열 배치 --%>
			<form id="stockCompoundForm" class="grid gap-4 md:grid-cols-2">
				<label class="form-control">
					<%-- 라벨 줄은 자리가 모자라면 미리보기 금액을 다음 줄로 내린다(2026-09-30): 영어 "Recurring Contribution" 옆에서 금액이 "20,000,/000" 으로 갈렸다(1280 · 1440px). --%>
					<div class="label flex-wrap justify-between gap-x-3 gap-y-0">
						<span class="label-text font-medium">${initialLabel}</span>
						<span id="stockCompoundInitialPreview" class="label-text-alt text-right font-medium text-base-content/60 amount-value"></span>
					</div>
					<input id="stockCompoundInitial" name="initial" type="number" min="0" step="10000" value="0" class="input input-bordered w-full" aria-label="${initialLabel}" />
					<%-- 0 개수를 세지 않도록 만원 단위 증감 버튼 제공 (클릭 시 즉시 재계산) --%>
					<div class="mt-1 flex flex-wrap gap-1">
						<button type="button" class="btn btn-xs btn-ghost" aria-label="${java.text.MessageFormat.format(quickAddAriaPattern, initialLabel, quickAddSmallLabel)}" data-amount-add-target="stockCompoundInitial" data-amount-add="1000000">${quickAddSmallLabel}</button>
						<button type="button" class="btn btn-xs btn-ghost" aria-label="${java.text.MessageFormat.format(quickAddAriaPattern, initialLabel, quickAddLargeLabel)}" data-amount-add-target="stockCompoundInitial" data-amount-add="10000000">${quickAddLargeLabel}</button>
						<button type="button" class="btn btn-xs btn-ghost" aria-label="${java.text.MessageFormat.format(quickResetAriaPattern, initialLabel)}" data-amount-add-target="stockCompoundInitial" data-amount-add="0">0</button>
					</div>
				</label>
				<label class="form-control">
					<div class="label flex-wrap justify-between gap-x-3 gap-y-0">
						<span class="label-text font-medium">${contributionLabel}</span>
						<span id="stockCompoundContributionPreview" class="label-text-alt text-right font-medium text-base-content/60 amount-value"></span>
					</div>
					<input id="stockCompoundContribution" name="contribution" type="number" min="0" step="10000" value="20000000" class="input input-bordered w-full" aria-label="${contributionLabel}" />
					<div class="mt-1 flex flex-wrap gap-1">
						<button type="button" class="btn btn-xs btn-ghost" aria-label="${java.text.MessageFormat.format(quickAddAriaPattern, contributionLabel, quickAddSmallLabel)}" data-amount-add-target="stockCompoundContribution" data-amount-add="1000000">${quickAddSmallLabel}</button>
						<button type="button" class="btn btn-xs btn-ghost" aria-label="${java.text.MessageFormat.format(quickAddAriaPattern, contributionLabel, quickAddLargeLabel)}" data-amount-add-target="stockCompoundContribution" data-amount-add="10000000">${quickAddLargeLabel}</button>
						<button type="button" class="btn btn-xs btn-ghost" aria-label="${java.text.MessageFormat.format(quickResetAriaPattern, contributionLabel)}" data-amount-add-target="stockCompoundContribution" data-amount-add="0">0</button>
					</div>
				</label>
				<label class="form-control">
					<div class="label"><span class="label-text font-medium">${frequencyLabel}</span></div>
					<select id="stockCompoundFrequency" name="frequency" class="select select-bordered w-full">
						<option value="yearly" selected>${frequencyYearlyLabel}</option>
						<option value="monthly">${frequencyMonthlyLabel}</option>
					</select>
				</label>
				<%-- 연/12 로 나눈다는 사실만으로는 그 결과를 알 수 없다. 입력 연 12%는 월복리로 실효 연 12.68%가 되고,
				     20년이면 최종 자산이 실효 12% 가정보다 8.6% 커진다(실측 2026-09-11). 그 수치를 함께 적는다. --%>
				<p id="stockCompoundMonthlyNote" class="hidden text-xs leading-5 text-base-content/60 md:col-span-2">${monthlyNote}
					<span data-compound-effective data-template="${effectiveRateTemplate}"></span></p>
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

	<%-- 결과 카드 열 수는 화면 폭이 아니라 결과 자리 폭으로 정한다(2026-09-30). 1280px 에서는 입력 폼 옆이라 자리가 616px 인데
	     xl:grid-cols-4 가 네 칸으로 나눠 카드 안이 92px - "70,122,/240" 처럼 금액이 갈렸다(실측). 네 칸은 자리가 896px(@4xl) 넘을 때만:
	     1440px(776px) 에서도 네 칸이면 132px 로 11 글자까지라 30 년 복리의 "1,234,567,890" 은 또 갈린다. --%>
	<div class="space-y-6 @container">
		<div class="grid gap-4 @lg:grid-cols-2 @4xl:grid-cols-4">
			@template._components.ui.card(bodyClass = "gap-1 py-4", body = @`
					<div class="text-xs font-semibold text-base-content/60">${summaryFinalLabel}</div>
					<div id="stockCompoundFinalValue" class="font-mono text-xl font-bold tabular-nums amount-value">-</div>
			`)
			@template._components.ui.card(bodyClass = "gap-1 py-4", body = @`
					<div class="text-xs font-semibold text-base-content/60">${summaryPrincipalLabel}</div>
					<div id="stockCompoundTotalPrincipal" class="font-mono text-xl font-bold tabular-nums amount-value">-</div>
			`)
			@template._components.ui.card(bodyClass = "gap-1 py-4", body = @`
					<div class="text-xs font-semibold text-base-content/60">${summaryProfitLabel}</div>
					<div id="stockCompoundTotalProfit" class="font-mono text-xl font-bold tabular-nums amount-value text-profit">-</div>
			`)
			@template._components.ui.card(bodyClass = "gap-1 py-4", body = @`
					<div class="text-xs font-semibold text-base-content/60">${summaryReturnLabel}</div>
					<div id="stockCompoundTotalReturn" class="font-mono text-xl font-bold tabular-nums text-profit">-</div>
			`)
		</div>

		@template._components.ui.card(bodyClass = "gap-4", body = @`
				<div class="space-y-2">
					<h3 class="card-title text-base">${chartTitle}</h3>
					<p class="text-sm leading-6 text-base-content/60">${chartDescription}</p>
				</div>
				<div id="stockCompoundRatio" class="space-y-1">
					<div class="flex h-3 w-full overflow-hidden rounded-full bg-base-200">
						<div id="stockCompoundRatioPrincipalBar" class="h-3" style="background: var(--color-compound-principal)"></div>
						<div id="stockCompoundRatioProfitBar" class="h-3" style="background: var(--color-compound-profit)"></div>
					</div>
					<div class="flex flex-wrap justify-between gap-x-3 text-xs text-base-content/60">
						<span><span class="mr-1 inline-block h-2 w-2 rounded-full" style="background: var(--color-compound-principal)"></span>${seriesPrincipalLabel} <span id="stockCompoundRatioPrincipalPct" class="font-mono tabular-nums font-semibold"></span></span>
						<span><span class="mr-1 inline-block h-2 w-2 rounded-full" style="background: var(--color-compound-profit)"></span>${seriesProfitLabel} <span id="stockCompoundRatioProfitPct" class="font-mono tabular-nums font-semibold"></span></span>
					</div>
				</div>
				<div class="h-80 w-full rounded-2xl bg-base-100 p-2">
					<canvas id="stockCompoundGrowthChart" role="img" aria-label="${chartTitle}" class="amount-chart"></canvas>
				</div>
		`)

		@template._components.ui.card(bodyClass = "gap-4", body = @`
				<div class="space-y-2">
					<h3 class="card-title text-base">${yearlyTitle}</h3>
					<p class="text-sm leading-6 text-base-content/60">${yearlyDescription}</p>
				</div>
				<div class="overflow-x-auto">
					<table class="table table-zebra table-sm" aria-label="${yearlyTitle}">
						<thead>
							<tr>
								<th scope="col">${yearHeader}</th>
								<th scope="col" class="text-right">${contributionHeader}</th>
								<th scope="col" class="text-right">${principalHeader}</th>
								<th scope="col" class="text-right">${interestHeader}</th>
								<th scope="col" class="text-right">${balanceHeader}</th>
								<th scope="col" class="text-right">${ratioHeader}</th>
							</tr>
						</thead>
						<tbody id="stockCompoundYearlyTableBody"></tbody>
					</table>
				</div>
		`)
	</div>
</div>
