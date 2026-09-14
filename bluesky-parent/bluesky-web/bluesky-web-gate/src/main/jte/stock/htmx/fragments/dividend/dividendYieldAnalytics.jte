@import java.util.List
@import java.util.UUID
@import java.math.BigDecimal
@import java.math.RoundingMode
@import java.text.DecimalFormat
@import net.luversof.web.gate.stock.domain.StockItem
@import net.luversof.web.gate.stock.dto.view.DividendYieldGroupView
@import io.github.luversof.boot.context.support.MessageUtil

@param DecimalFormat decimalFormat
@param java.util.function.Function<BigDecimal, String> percentFormat
@param java.util.function.Function<Long, String> countFormat
@param BigDecimal safeAllNet
@param BigDecimal portfolioYieldOnDailyAverageCostPct
@param BigDecimal portfolioYieldOnCostPct
@param BigDecimal portfolioYieldOnMarketPct
@param BigDecimal portfolioYieldAnnualizedPct = null
@param long periodDayCount = 0L
@param BigDecimal periodStartPrincipal = null
@param BigDecimal periodEndPrincipal = null
@param BigDecimal periodPrincipalDelta = null
@param BigDecimal periodPrincipalDeltaPct = null
@param DividendYieldGroupView bestYieldStock
@param DividendYieldGroupView bestYieldAccount
@param List<DividendYieldGroupView> yearlyYieldRows
@param List<DividendYieldGroupView> stockYieldRows
@param List<DividendYieldGroupView> accountYieldRows
@param List<StockItem> stockItemList
@param String accountLabel
@param String stockNameLabel
@param String tagLabel
@param String noDataLabel
@param String grossAmountLabel
@param String netAmountLabel
@param String taxLabel
@param String taxableAmountLabel
@param String totalLabel

!{
	// 이 표들의 "-" 도 두 뜻이다 - 0 원과 값 없음. 0 쪽에만 짧은 까닭을 단다(배당 상세 목록과 같은 규칙).
	String zeroAmountTitle = MessageUtil.getMessage("stock.dividend.zero.amount.title");
	String dividendYieldTitle = MessageUtil.getMessage("stock.dividend.yield.summary.title");
	String bestYieldStockLabel = MessageUtil.getMessage("stock.dividend.yield.summary.best.stock");
	String bestYieldAccountLabel = MessageUtil.getMessage("stock.dividend.yield.summary.best.account");
	String yearlyYieldSectionLabel = MessageUtil.getMessage("stock.dividend.yield.section.yearly");
	String stockYieldSectionLabel = MessageUtil.getMessage("stock.dividend.yield.section.stock");
	String accountYieldSectionLabel = MessageUtil.getMessage("stock.dividend.yield.section.account");
	String yearLabel = MessageUtil.getMessage("stock.dividend.yield.column.year");
	String wholePeriodLabel = MessageUtil.getMessage("stock.dividend.yield.total.row");
	String dailyAverageInvestedCapitalLabel = MessageUtil.getMessage("stock.dividend.yield.column.daily.average.invested.capital");
	String basisAverageInvestedCapitalLabel = MessageUtil.getMessage("stock.dividend.yield.column.basis.average.invested.capital");
	String yieldOnDailyAverageCostLabel = MessageUtil.getMessage("stock.dividend.yield.column.yield.on.daily.average.cost");
	String yieldOnBasisAverageCostLabel = MessageUtil.getMessage("stock.dividend.yield.column.yield.on.basis.average.cost");
	String yieldOnBasisMarketLabel = MessageUtil.getMessage("stock.dividend.yield.column.yield.on.basis.market");
	String dividendYieldPeriodDailyLabel = MessageUtil.getMessage("stock.dividend.yield.summary.period.daily");
	String dividendYieldPeriodBasisLabel = MessageUtil.getMessage("stock.dividend.yield.summary.period.basis");
	String annualizedLabel = MessageUtil.getMessage("stock.dividend.yield.summary.annualized");
	String annualizedNoteLabel = java.text.MessageFormat.format(
		MessageUtil.getMessage("stock.dividend.yield.summary.annualized.note"), periodDayCount);
	String principalChangeLabel = MessageUtil.getMessage("stock.dividend.yield.summary.principal.change");
	String principalStartLabel = MessageUtil.getMessage("stock.dividend.yield.summary.principal.start");
	String principalEndLabel = MessageUtil.getMessage("stock.dividend.yield.summary.principal.end");
	String dividendYieldHelpTitleLabel = MessageUtil.getMessage("stock.dividend.yield.help.title");
	String dividendYieldHelpPrimaryLabel = MessageUtil.getMessage("stock.dividend.yield.help.primary");
	String dividendYieldHelpSecondaryLabel = MessageUtil.getMessage("stock.dividend.yield.help.secondary");
	String dividendYieldHelpRankingLabel = MessageUtil.getMessage("stock.dividend.yield.help.ranking");
	String dividendCountLabel = MessageUtil.getMessage("stock.dividend.yield.column.dividend.count");
	String noYieldAnalyticsLabel = MessageUtil.getMessage("stock.dividend.yield.message.no.analytics");
	String yieldBasisNoteLabel = MessageUtil.getMessage("stock.dividend.yield.basis.note");
	String stockYieldSelectionHelperLabel = MessageUtil.getMessage("stock.dividend.yield.selection.stock.helper");
	String stockYieldSelectionSummaryLabel = MessageUtil.getMessage("stock.dividend.yield.selection.stock.summary");
	String stockYieldSelectionCountLabel = MessageUtil.getMessage("stock.dividend.yield.selection.stock.count");
	String accountYieldSelectionHelperLabel = MessageUtil.getMessage("stock.dividend.yield.selection.account.helper");
	String accountYieldSelectionSummaryLabel = MessageUtil.getMessage("stock.dividend.yield.selection.account.summary");
	String accountYieldSelectionCountLabel = MessageUtil.getMessage("stock.dividend.yield.selection.account.count");
	String dividendYieldSelectionClearLabel = MessageUtil.getMessage("stock.analytics.selection.clear");
	String dividendYieldSelectionWeightLabel = MessageUtil.getMessage("stock.analytics.selection.weight");
	java.util.function.Function<String, String> stockTagTooltipByName = stockName -> {
		if (stockName == null || stockName.isBlank() || stockItemList == null) {
			return tagLabel + ": " + noDataLabel;
		}

		return stockItemList.stream()
			.filter(stockItem -> stockItem != null && stockName.equals(stockItem.name()))
			.findFirst()
			.map(stockItem -> stockItem.tags() != null && !stockItem.tags().isEmpty()
				? tagLabel + ": " + String.join(", ", stockItem.tags())
				: tagLabel + ": " + noDataLabel)
			.orElse(tagLabel + ": " + noDataLabel);
	};
	java.util.function.BiFunction<List<DividendYieldGroupView>, java.util.function.Function<DividendYieldGroupView, BigDecimal>, BigDecimal> sumDividendYieldValue = (rows, extractor) ->
		rows != null
			? rows.stream()
				.filter(row -> row != null)
				.map(row -> {
					BigDecimal value = extractor.apply(row);
					return value != null ? value : BigDecimal.ZERO;
				})
				.reduce(BigDecimal.ZERO, BigDecimal::add)
			: BigDecimal.ZERO;
	java.util.function.Function<List<DividendYieldGroupView>, Long> sumDividendYieldCount = rows ->
		rows != null
			? rows.stream()
				.filter(row -> row != null)
				.mapToLong(DividendYieldGroupView::dividendCount)
				.sum()
			: 0L;
	java.util.function.BiFunction<BigDecimal, BigDecimal, BigDecimal> computeYieldPct = (netAmount, baseAmount) ->
		baseAmount != null && baseAmount.compareTo(BigDecimal.ZERO) > 0
			? (netAmount != null ? netAmount : BigDecimal.ZERO)
				.multiply(BigDecimal.valueOf(100))
				.divide(baseAmount, 2, RoundingMode.HALF_UP)
			: BigDecimal.ZERO;
	BigDecimal stockYieldFooterGrossAmount = sumDividendYieldValue.apply(stockYieldRows, DividendYieldGroupView::totalGrossAmount);
	BigDecimal stockYieldFooterNetAmount = sumDividendYieldValue.apply(stockYieldRows, DividendYieldGroupView::totalNetAmount);
	BigDecimal stockYieldFooterTaxAmount = stockYieldFooterGrossAmount.subtract(stockYieldFooterNetAmount);
	BigDecimal stockYieldFooterTaxableAmount = sumDividendYieldValue.apply(stockYieldRows, DividendYieldGroupView::totalTaxableAmount);
	BigDecimal stockYieldFooterDailyPrincipalCost = sumDividendYieldValue.apply(stockYieldRows, DividendYieldGroupView::averageDailyPrincipalCost);
	BigDecimal stockYieldFooterPrincipalCost = sumDividendYieldValue.apply(stockYieldRows, DividendYieldGroupView::averagePrincipalCost);
	BigDecimal stockYieldFooterMarketValue = sumDividendYieldValue.apply(stockYieldRows, DividendYieldGroupView::averagePrincipalMarketValue);
	BigDecimal stockYieldFooterNetWithPrincipalCost = sumDividendYieldValue.apply(stockYieldRows, DividendYieldGroupView::netAmountWithPrincipalCost);
	BigDecimal stockYieldFooterYieldOnDailyAverageCostPct = computeYieldPct.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterDailyPrincipalCost);
	BigDecimal stockYieldFooterNetWithPrincipalMarket = sumDividendYieldValue.apply(stockYieldRows, DividendYieldGroupView::netAmountWithPrincipalMarket);
	BigDecimal stockYieldFooterYieldOnCostPct = computeYieldPct.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterPrincipalCost);
	BigDecimal stockYieldFooterYieldOnMarketPct = computeYieldPct.apply(stockYieldFooterNetWithPrincipalMarket, stockYieldFooterMarketValue);
	long stockYieldFooterDividendCount = sumDividendYieldCount.apply(stockYieldRows);
	BigDecimal accountYieldFooterGrossAmount = sumDividendYieldValue.apply(accountYieldRows, DividendYieldGroupView::totalGrossAmount);
	BigDecimal accountYieldFooterNetAmount = sumDividendYieldValue.apply(accountYieldRows, DividendYieldGroupView::totalNetAmount);
	BigDecimal accountYieldFooterTaxAmount = accountYieldFooterGrossAmount.subtract(accountYieldFooterNetAmount);
	BigDecimal accountYieldFooterTaxableAmount = sumDividendYieldValue.apply(accountYieldRows, DividendYieldGroupView::totalTaxableAmount);
	BigDecimal accountYieldFooterDailyPrincipalCost = sumDividendYieldValue.apply(accountYieldRows, DividendYieldGroupView::averageDailyPrincipalCost);
	BigDecimal accountYieldFooterPrincipalCost = sumDividendYieldValue.apply(accountYieldRows, DividendYieldGroupView::averagePrincipalCost);
	BigDecimal accountYieldFooterMarketValue = sumDividendYieldValue.apply(accountYieldRows, DividendYieldGroupView::averagePrincipalMarketValue);
	BigDecimal accountYieldFooterNetWithPrincipalCost = sumDividendYieldValue.apply(accountYieldRows, DividendYieldGroupView::netAmountWithPrincipalCost);
	BigDecimal accountYieldFooterYieldOnDailyAverageCostPct = computeYieldPct.apply(accountYieldFooterNetWithPrincipalCost, accountYieldFooterDailyPrincipalCost);
	BigDecimal accountYieldFooterNetWithPrincipalMarket = sumDividendYieldValue.apply(accountYieldRows, DividendYieldGroupView::netAmountWithPrincipalMarket);
	BigDecimal accountYieldFooterYieldOnCostPct = computeYieldPct.apply(accountYieldFooterNetWithPrincipalCost, accountYieldFooterPrincipalCost);
	BigDecimal accountYieldFooterYieldOnMarketPct = computeYieldPct.apply(accountYieldFooterNetWithPrincipalMarket, accountYieldFooterMarketValue);
	long accountYieldFooterDividendCount = sumDividendYieldCount.apply(accountYieldRows);
	// 수익률의 분자는 "기준일 원금이 있는 배당"의 세후 합이라 화면의 배당금(세후) 열과 다르다
	// (실측: 193건 중 5건이 분모에 기여하지 않아 제외된다. 화면에 적힌 배당/원금을 그대로 나누면
	//  7.14% 인데 표시는 7.12% 라서, 값이 틀린 것처럼 보였다).
	// 실제 분자와 제외액을 셀 툴팁으로 붙여 표시값을 화면에서 재현할 수 있게 한다.
	// 걸러진 배당이 없으면 null - JTE 는 null 속성을 통째로 빼므로 의미 없는 title="" 이 나가지 않는다(실측: 전체 기간 40개).
	// 2026-09-11 실측: 그 툴팁이 xl 이상에서만 보이는 열(기준일 원금 수익률·시가 수익률) 10칸에만 붙어 있었고,
	// 정작 어느 폭에서나 보이는 "기간 평균투입원금 수익률" 열은 같은 분자를 쓰면서 아무 설명이 없었다
	// (2020년: 배당 5건 중 2건만 분자에 들어가는데 건수 열은 5건이라고만 적혀 있다). 세 표의 본문·합계 6칸에 같은 근거를 붙인다.
	java.util.function.BiFunction<BigDecimal, BigDecimal, String> yieldBasisTitle = (usedNet, totalNet) -> {
		if (usedNet == null || totalNet == null) {
			return null;
		}
		BigDecimal excludedNet = totalNet.subtract(usedNet);
		if (excludedNet.compareTo(BigDecimal.ZERO) <= 0) {
			return null;
		}
		return java.text.MessageFormat.format(
			MessageUtil.getMessage("stock.dividend.yield.basis.excluded"),
			decimalFormat.format(usedNet), decimalFormat.format(excludedNet));
	};
	// 기준일 원금이 아예 없으면 금액도 수익률도 "-" 가 되는데, 2026-09-12 까지 그 "-" 에는 까닭이 없었다
	// (실측: 연도별 2칸 + 종목별 랭킹 4칸). 왜 비었는지는 옆 칸 문구가 말하고 있었지만 이 칸 자체는 말이 없었다.
	String basisNoneTitle = MessageUtil.getMessage("stock.dividend.yield.basis.none");
	// 기간 일평균 투입원금도 마찬가지다 - 매매 없이 배당만 있는 종목(실측: 하나금융지주)은 이 칸이 비는데 까닭이 없었다.
	String dailyBasisNoneTitle = MessageUtil.getMessage("stock.dividend.yield.daily.basis.none");
	java.util.function.Function<BigDecimal, String> basisNoneOrNull =
		principalCost -> principalCost == null || principalCost.signum() == 0 ? basisNoneTitle : null;
}

	<!-- Row 3: Yield Analytics -->
	<div class="mb-3 px-1">
		<div class="flex items-center gap-2">
			<h2 class="text-sm font-bold" data-page-section>${dividendYieldTitle}</h2>
			<div class="tooltip tooltip-bottom shrink-0" tabindex="0">
				<div class="tooltip-content z-[20] w-64 sm:w-72 max-w-[calc(100vw-2rem)] rounded-box bg-neutral text-neutral-content shadow-lg">
					<div class="space-y-1 text-left text-xs leading-5">
						<div class="font-semibold mb-1">${dividendYieldHelpTitleLabel}</div>
						<div>${dividendYieldHelpPrimaryLabel}</div>
						<div>${dividendYieldHelpSecondaryLabel}</div>
						<div>${dividendYieldHelpRankingLabel}</div>
					</div>
				</div>
				<button type="button" class="btn btn-circle btn-ghost btn-xs text-info" aria-label="${dividendYieldHelpTitleLabel}">
					<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="w-4 h-4 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
				</button>
			</div>
		</div>
		<div class="text-xs text-base-content/60 mt-1">${yieldBasisNoteLabel}</div>
	</div>

	<div class="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-4">
		@template._components.ui.card(body = @`
				<div class="text-xs text-base-content/60 mb-1">${dividendYieldPeriodDailyLabel}</div>
				<div class="text-xl font-bold text-dividend">${percentFormat.apply(portfolioYieldOnDailyAverageCostPct)}</div>
				<div class="text-xs text-base-content/60 mt-1">${dividendYieldPeriodBasisLabel} <span class="font-medium">${percentFormat.apply(portfolioYieldOnCostPct)}</span></div>
				<div class="text-xs text-base-content/60 mt-1">${yieldOnBasisMarketLabel} <span class="font-medium">${percentFormat.apply(portfolioYieldOnMarketPct)}</span></div>
				@if(portfolioYieldAnnualizedPct != null)
				<div class="mt-2 border-t border-base-200/70 pt-2">
					<div class="text-xs text-base-content/60">${annualizedLabel}</div>
					<div class="text-lg font-bold text-dividend">${percentFormat.apply(portfolioYieldAnnualizedPct)}</div>
					<div class="text-[11px] text-base-content/60">${annualizedNoteLabel}</div>
				</div>
				@endif
				@if(periodPrincipalDelta != null)
				<div class="mt-2 border-t border-base-200/70 pt-2">
					<div class="text-xs text-base-content/60">${principalChangeLabel}</div>
					<div class="text-xs text-base-content/70 mt-0.5">
						<span class="text-base-content/60">${principalStartLabel}</span> <span class="font-mono amount-value">₩ ${decimalFormat.format(periodStartPrincipal)}</span>
						<span class="text-base-content/60 mx-1">→</span>
						<span class="text-base-content/60">${principalEndLabel}</span> <span class="font-mono amount-value">₩ ${decimalFormat.format(periodEndPrincipal)}</span>
					</div>
					<div class="text-sm font-semibold mt-0.5 ${periodPrincipalDelta.signum() > 0 ? "text-profit" : periodPrincipalDelta.signum() < 0 ? "text-loss" : "text-base-content/60"}">
						<span class="font-mono amount-value">${periodPrincipalDelta.signum() == 0 ? "" : (periodPrincipalDelta.signum() > 0 ? "+" : "-")}₩ ${decimalFormat.format(periodPrincipalDelta.abs())}</span>
						@if(periodPrincipalDeltaPct != null)
						<span class="text-xs font-normal">(${periodPrincipalDeltaPct.signum() >= 0 ? "+" : ""}${percentFormat.apply(periodPrincipalDeltaPct)})</span>
						@endif
					</div>
				</div>
				@endif
		`)

		@template._components.ui.card(body = @`
				<div class="text-xs text-base-content/60 mb-1">${bestYieldStockLabel}</div>
				@if(bestYieldStock != null)
					<div class="text-sm font-semibold truncate" title="${bestYieldStock.label()}">${bestYieldStock.label()}</div>
					<div class="text-xl font-bold text-dividend">${percentFormat.apply(bestYieldStock.yieldOnDailyAverageCostPct())}</div>
					<div class="text-xs text-base-content/60 mt-1">${dailyAverageInvestedCapitalLabel} <span class="font-medium amount-value">₩ ${bestYieldStock.averageDailyPrincipalCost() != null ? decimalFormat.format(bestYieldStock.averageDailyPrincipalCost()) : "-"}</span></div>
					<div class="text-xs text-base-content/60 mt-1">${yieldOnBasisAverageCostLabel} <span class="font-medium">${percentFormat.apply(bestYieldStock.yieldOnCostPct())}</span></div>
					<div class="text-xs text-base-content/60 mt-1">${netAmountLabel} <span class="font-medium amount-value">₩ ${decimalFormat.format(bestYieldStock.totalNetAmount())}</span></div>
				@else
					<div class="text-sm text-base-content/60">${noYieldAnalyticsLabel}</div>
				@endif
		`)

		@template._components.ui.card(body = @`
				<div class="text-xs text-base-content/60 mb-1">${bestYieldAccountLabel}</div>
				@if(bestYieldAccount != null)
					<div class="text-sm font-semibold truncate" title="${bestYieldAccount.label()}">${bestYieldAccount.label()}</div>
					<div class="text-xl font-bold text-dividend">${percentFormat.apply(bestYieldAccount.yieldOnDailyAverageCostPct())}</div>
					<div class="text-xs text-base-content/60 mt-1">${dailyAverageInvestedCapitalLabel} <span class="font-medium amount-value">₩ ${bestYieldAccount.averageDailyPrincipalCost() != null ? decimalFormat.format(bestYieldAccount.averageDailyPrincipalCost()) : "-"}</span></div>
					<div class="text-xs text-base-content/60 mt-1">${yieldOnBasisAverageCostLabel} <span class="font-medium">${percentFormat.apply(bestYieldAccount.yieldOnCostPct())}</span></div>
					<div class="text-xs text-base-content/60 mt-1">${netAmountLabel} <span class="font-medium amount-value">₩ ${decimalFormat.format(bestYieldAccount.totalNetAmount())}</span></div>
				@else
					<div class="text-sm text-base-content/60">${noYieldAnalyticsLabel}</div>
				@endif
		`)
	</div>

	<div class="grid grid-cols-1 gap-4 mb-4">
		@template._components.ui.card(cssClass = "min-w-0", body = @`
				<h2 class="text-sm font-bold mb-3" data-page-section>${yearlyYieldSectionLabel}</h2>
				@if(yearlyYieldRows == null || yearlyYieldRows.isEmpty())
					<div class="text-sm text-base-content/60">${noYieldAnalyticsLabel}</div>
				@else
					<div class="overflow-x-auto">
						<table class="table table-xs w-full" aria-label="${yearlyYieldSectionLabel}">
							<thead>
								<tr class="text-base-content/60">
									<th scope="col">${yearLabel}</th>
									<th scope="col" class="text-right hidden md:table-cell print:table-cell">${grossAmountLabel}</th>
									<th scope="col" class="text-right">${netAmountLabel}</th>
									<th scope="col" class="text-right hidden md:table-cell print:table-cell">${taxLabel}</th>
									<th scope="col" class="text-right hidden md:table-cell print:table-cell">${taxableAmountLabel}</th>
									<th scope="col" class="text-right hidden md:table-cell print:table-cell">${dailyAverageInvestedCapitalLabel}</th>
									<th scope="col" class="text-right">${yieldOnDailyAverageCostLabel}</th>
									<th scope="col" class="text-right hidden xl:table-cell print:table-cell">${basisAverageInvestedCapitalLabel}</th>
									<th scope="col" class="text-right hidden xl:table-cell print:table-cell">${yieldOnBasisAverageCostLabel}</th>
									<th scope="col" class="text-right">${dividendCountLabel}</th>
								</tr>
							</thead>
							<tbody>
								@for(DividendYieldGroupView row : yearlyYieldRows)
									!{BigDecimal rowTaxAmount = row.totalGrossAmount() != null && row.totalNetAmount() != null ? row.totalGrossAmount().subtract(row.totalNetAmount()) : null;}
									<tr>
										<th scope="row" class="font-medium">${row.label()}</th>
										<td class="text-right hidden md:table-cell print:table-cell amount-value">${decimalFormat.format(row.totalGrossAmount())}</td>
										<td class="text-right amount-value">${decimalFormat.format(row.totalNetAmount())}</td>
										<td class="text-right hidden md:table-cell print:table-cell amount-value" title="${rowTaxAmount != null && rowTaxAmount.signum() == 0 ? zeroAmountTitle : null}" aria-label="${rowTaxAmount != null && rowTaxAmount.signum() == 0 ? zeroAmountTitle : null}">${rowTaxAmount != null && rowTaxAmount.signum() != 0 ? decimalFormat.format(rowTaxAmount) : "-"}</td>
										<td class="text-right hidden md:table-cell print:table-cell amount-value" title="${row.totalTaxableAmount() != null && row.totalTaxableAmount().signum() == 0 ? zeroAmountTitle : null}" aria-label="${row.totalTaxableAmount() != null && row.totalTaxableAmount().signum() == 0 ? zeroAmountTitle : null}">${row.totalTaxableAmount() != null && row.totalTaxableAmount().signum() != 0 ? decimalFormat.format(row.totalTaxableAmount()) : "-"}</td>
										<td class="text-right hidden md:table-cell print:table-cell amount-value" title="${row.averageDailyPrincipalCost() == null ? dailyBasisNoneTitle : null}">${row.averageDailyPrincipalCost() != null ? decimalFormat.format(row.averageDailyPrincipalCost()) : "-"}@if(row.averageDailyPrincipalCost() == null)<span class="sr-only">${dailyBasisNoneTitle}</span>@endif</td>
										<td class="text-right text-dividend font-medium" data-amount-basis="${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}" title="${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}">${row.averageDailyPrincipalCost() != null && row.averageDailyPrincipalCost().signum() > 0 ? percentFormat.apply(row.yieldOnDailyAverageCostPct()) : "-"}@if(yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount()) != null)<span class="sr-only">${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}</span>@endif</td>
										<td class="text-right hidden xl:table-cell print:table-cell amount-value" title="${basisNoneOrNull.apply(row.averagePrincipalCost())}">${row.averagePrincipalCost() != null ? decimalFormat.format(row.averagePrincipalCost()) : "-"}@if(basisNoneOrNull.apply(row.averagePrincipalCost()) != null)<span class="sr-only">${basisNoneTitle}</span>@endif</td>
										<td class="text-right hidden xl:table-cell print:table-cell text-base-content/70" data-amount-basis="${basisNoneOrNull.apply(row.averagePrincipalCost()) != null ? basisNoneTitle : yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}" title="${basisNoneOrNull.apply(row.averagePrincipalCost()) != null ? basisNoneTitle : yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}">${percentFormat.apply(row.yieldOnCostPct())}</td>
										<td class="text-right">${countFormat.apply(row.dividendCount())}</td>
									</tr>
								@endfor
							</tbody>
							<%-- 전체 기간 줄. 금액·건수는 연도 합이고, 원금과 수익률은 연도를 더한 값이 아니라
								 전체 기간의 값이다(연도별 일평균 원금을 더하면 뜻이 없다). 종목별 표의 합계행과 같은
								 값을 쓴다 - 같은 배당을 다른 축으로 쪼갠 것이라 서로 검산이 된다.
								 한 해뿐이면 붙이지 않는다(연도별 성과 표와 같은 규칙). --%>
							@if(yearlyYieldRows != null && yearlyYieldRows.size() > 1)
								!{BigDecimal yearlyTotalGross = sumDividendYieldValue.apply(yearlyYieldRows, DividendYieldGroupView::totalGrossAmount);}
								!{BigDecimal yearlyTotalNet = sumDividendYieldValue.apply(yearlyYieldRows, DividendYieldGroupView::totalNetAmount);}
								!{BigDecimal yearlyTotalTax = yearlyTotalGross.subtract(yearlyTotalNet);}
								!{BigDecimal yearlyTotalTaxable = sumDividendYieldValue.apply(yearlyYieldRows, DividendYieldGroupView::totalTaxableAmount);}
								!{long yearlyTotalCount = sumDividendYieldCount.apply(yearlyYieldRows);}
								<tfoot data-dividend-yearly-total>
									<tr class="bg-base-200 font-bold">
										<th scope="row">${wholePeriodLabel}</th>
										<td class="text-right hidden md:table-cell print:table-cell font-mono amount-value">${decimalFormat.format(yearlyTotalGross)}</td>
										<td class="text-right font-mono text-dividend amount-value">${decimalFormat.format(yearlyTotalNet)}</td>
										<td class="text-right hidden md:table-cell print:table-cell font-mono amount-value" title="${yearlyTotalTax.signum() == 0 ? zeroAmountTitle : null}" aria-label="${yearlyTotalTax.signum() == 0 ? zeroAmountTitle : null}">${yearlyTotalTax.signum() != 0 ? decimalFormat.format(yearlyTotalTax) : "-"}</td>
										<td class="text-right hidden md:table-cell print:table-cell font-mono amount-value" title="${yearlyTotalTaxable.signum() == 0 ? zeroAmountTitle : null}" aria-label="${yearlyTotalTaxable.signum() == 0 ? zeroAmountTitle : null}">${yearlyTotalTaxable.signum() != 0 ? decimalFormat.format(yearlyTotalTaxable) : "-"}</td>
										<td class="text-right hidden md:table-cell print:table-cell font-mono amount-value">${decimalFormat.format(stockYieldFooterDailyPrincipalCost)}</td>
										<td class="text-right text-dividend font-medium" data-amount-basis="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}" title="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}">${stockYieldFooterDailyPrincipalCost.signum() > 0 ? percentFormat.apply(stockYieldFooterYieldOnDailyAverageCostPct) : "-"}@if(yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount) != null)<span class="sr-only">${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}</span>@endif</td>
										<td class="text-right hidden xl:table-cell print:table-cell font-mono amount-value">${decimalFormat.format(stockYieldFooterPrincipalCost)}</td>
										<td class="text-right hidden xl:table-cell print:table-cell text-base-content/70" data-amount-basis="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}" title="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}">${stockYieldFooterPrincipalCost.signum() > 0 ? percentFormat.apply(stockYieldFooterYieldOnCostPct) : "-"}</td>
										<td class="text-right">${countFormat.apply(yearlyTotalCount)}</td>
									</tr>
								</tfoot>
							@endif
						</table>
					</div>
				@endif
		`)

		<div class="card min-w-0 bg-base-100 shadow-sm border border-base-200">
			<div class="card-body p-4" data-dividend-yield-selection-section>
				<h2 class="text-sm font-bold mb-3" data-page-section>${stockYieldSectionLabel}</h2>
				@if(stockYieldRows == null || stockYieldRows.isEmpty())
					<div class="text-sm text-base-content/60">${noYieldAnalyticsLabel}</div>
				@else
					<div class="mb-3 flex flex-col gap-3">
						<p class="text-xs text-base-content/60">${stockYieldSelectionHelperLabel}</p>
						<div class="selection-reveal"><div>
						<div class="rounded-2xl border border-info/15 bg-info/5 px-4 py-3"
							 data-dividend-yield-selection-summary
							 data-count-template="${stockYieldSelectionCountLabel}"
							 data-total-net-amount="${safeAllNet.toPlainString()}"
							 data-weight-label="${dividendYieldSelectionWeightLabel}"
							 data-basis-label="${yieldOnBasisAverageCostLabel}"
							 aria-live="polite">
							<div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
								<div class="flex flex-wrap items-center gap-2">
									<h3 class="text-sm font-semibold">${stockYieldSelectionSummaryLabel}</h3>
									<span class="badge badge-info badge-outline" data-dividend-yield-selection-count>${stockYieldSelectionSummaryLabel}</span>
									<span class="badge badge-ghost" data-dividend-yield-selection-weight-badge>${dividendYieldSelectionWeightLabel} 0.0%</span>
								</div>
								<button type="button" class="btn btn-ghost btn-xs self-start" data-dividend-yield-selection-clear>${dividendYieldSelectionClearLabel}</button>
							</div>
							<div class="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-6">
								<div class="rounded-box bg-base-100/80 px-3 py-2">
									<div class="text-xs text-base-content/60">${grossAmountLabel}</div>
									<div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-gross>0</div>
								</div>
								<div class="rounded-box bg-base-100/80 px-3 py-2">
									<div class="text-xs text-base-content/60">${netAmountLabel}</div>
									<div class="mt-1 font-mono font-semibold amount-value text-dividend" data-dividend-yield-selection-net>0</div>
								</div>
								<div class="rounded-box bg-base-100/80 px-3 py-2">
									<div class="text-xs text-base-content/60">${taxLabel}</div>
									<div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-tax>0</div>
								</div>
								<div class="rounded-box bg-base-100/80 px-3 py-2">
									<div class="text-xs text-base-content/60">${taxableAmountLabel}</div>
									<div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-taxable>0</div>
								</div>
								<div class="rounded-box bg-base-100/80 px-3 py-2">
									<div class="text-xs text-base-content/60">${dailyAverageInvestedCapitalLabel}</div>
									<div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-daily-capital>0</div>
								</div>
								<div class="rounded-box bg-base-100/80 px-3 py-2">
									<div class="text-xs text-base-content/60">${yieldOnDailyAverageCostLabel}</div>
									<div class="mt-1 font-mono font-semibold text-dividend" data-dividend-yield-selection-yield>0.00%</div>
									<div class="text-xs text-base-content/60" data-dividend-yield-selection-basis-yield>${yieldOnBasisAverageCostLabel} 0.00%</div>
								</div>
							</div>
						</div>
						</div></div>
					</div>
					<div class="overflow-x-auto">
						<%-- 이 표는 11열이라 2xl(1536px)에서 펴면 표가 1,380px 인데 본문 열은 1,246px 뿐이라 가로 스크롤이 생긴다
					     (실측 2026-09-10: 1536px 넘침 134px · 1600px 70px · 1660px 10px · 1680px 0px). 1536px 은 1920x1080 을
					     125% 배율로 쓸 때의 폭이라 흔하다. 2026-09-10 에 헤더 정렬 버튼을 접히게 한 뒤 이 표의 최소폭이
					     1,300px -> 840px 로 줄어, 1680px 이 아니라 1150px 부터 넘침 0 이다(1120px 10 · 1150px 0).
					     Tailwind 테마에 3xl 을 넣으면 .container 최대폭까지 같이 바뀌어(게시판 화면 사용) 임의 분기점을 쓴다. --%>
						<table class="table table-xs w-full" data-dividend-yield-selection-table data-dividend-yield-sortable-table aria-label="${stockYieldSectionLabel}">
							<thead>
								<tr class="text-base-content/60">
									<th scope="col">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-start px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="label"
												data-sort-type="text">
											<span>${stockNameLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right hidden min-[1150px]:table-cell print:table-cell">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="grossAmount"
												data-sort-type="number">
											<span>${grossAmountLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="netAmount"
												data-sort-type="number">
											<span>${netAmountLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right hidden min-[1150px]:table-cell print:table-cell">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="taxAmount"
												data-sort-type="number">
											<span>${taxLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right hidden min-[1150px]:table-cell print:table-cell">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="taxableAmount"
												data-sort-type="number">
											<span>${taxableAmountLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right hidden min-[1150px]:table-cell print:table-cell">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="averageDailyPrincipalCost"
												data-sort-type="number">
											<span>${dailyAverageInvestedCapitalLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="yieldOnDailyAverageCost"
												data-sort-type="number">
											<span>${yieldOnDailyAverageCostLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right hidden xl:table-cell print:table-cell">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="averagePrincipalCost"
												data-sort-type="number">
											<span>${basisAverageInvestedCapitalLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right hidden xl:table-cell print:table-cell">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="yieldOnCost"
												data-sort-type="number">
											<span>${yieldOnBasisAverageCostLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right hidden min-[1150px]:table-cell print:table-cell">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="yieldOnMarket"
												data-sort-type="number">
											<span>${yieldOnBasisMarketLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="dividendCount"
												data-sort-type="number">
											<span>${dividendCountLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
								</tr>
							</thead>
							<tbody>
								@for(DividendYieldGroupView row : stockYieldRows)
									!{BigDecimal rowTaxAmount = row.totalGrossAmount() != null && row.totalNetAmount() != null ? row.totalGrossAmount().subtract(row.totalNetAmount()) : null;}
									!{String stockTagTooltip = stockTagTooltipByName.apply(row.label());}
									<tr class="dividend-yield-stock-row"
										data-dividend-yield-row
										data-selection-class="dividend-yield-stock-row-selected"
										tabindex="0"
										aria-selected="false"
										data-row-select="${MessageUtil.getMessage("stock.table.row.select")}"
										data-label="${row.label() != null ? row.label() : ""}"
										data-gross-amount="${row.totalGrossAmount() != null ? row.totalGrossAmount().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-net-amount="${row.totalNetAmount() != null ? row.totalNetAmount().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-tax-amount="${rowTaxAmount != null ? rowTaxAmount.toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-taxable-amount="${row.totalTaxableAmount() != null ? row.totalTaxableAmount().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-average-daily-principal-cost="${row.averageDailyPrincipalCost() != null ? row.averageDailyPrincipalCost().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-yield-on-daily-average-cost="${row.yieldOnDailyAverageCostPct() != null ? row.yieldOnDailyAverageCostPct().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-average-principal-cost="${row.averagePrincipalCost() != null ? row.averagePrincipalCost().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-net-with-principal-cost="${row.netAmountWithPrincipalCost() != null ? row.netAmountWithPrincipalCost().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-yield-on-cost="${row.yieldOnCostPct() != null ? row.yieldOnCostPct().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-yield-on-market="${row.yieldOnMarketPct() != null ? row.yieldOnMarketPct().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-dividend-count="${String.valueOf(row.dividendCount())}">
										<th scope="row" class="font-medium"><a href="/stock/item?stockItemId=${row.groupId() != null ? row.groupId().toString() : ""}" class="link link-hover" data-stock-tag-tooltip="${stockTagTooltip}">${row.label()}</a></th>
										<td class="text-right hidden min-[1150px]:table-cell print:table-cell amount-value">${decimalFormat.format(row.totalGrossAmount())}</td>
										<td class="text-right amount-value">${decimalFormat.format(row.totalNetAmount())}</td>
										<td class="text-right hidden min-[1150px]:table-cell print:table-cell amount-value" title="${rowTaxAmount != null && rowTaxAmount.signum() == 0 ? zeroAmountTitle : null}" aria-label="${rowTaxAmount != null && rowTaxAmount.signum() == 0 ? zeroAmountTitle : null}">${rowTaxAmount != null && rowTaxAmount.signum() != 0 ? decimalFormat.format(rowTaxAmount) : "-"}</td>
										<td class="text-right hidden min-[1150px]:table-cell print:table-cell amount-value" title="${row.totalTaxableAmount() != null && row.totalTaxableAmount().signum() == 0 ? zeroAmountTitle : null}" aria-label="${row.totalTaxableAmount() != null && row.totalTaxableAmount().signum() == 0 ? zeroAmountTitle : null}">${row.totalTaxableAmount() != null && row.totalTaxableAmount().signum() != 0 ? decimalFormat.format(row.totalTaxableAmount()) : "-"}</td>
										<td class="text-right hidden min-[1150px]:table-cell print:table-cell amount-value" title="${row.averageDailyPrincipalCost() == null ? dailyBasisNoneTitle : null}">${row.averageDailyPrincipalCost() != null ? decimalFormat.format(row.averageDailyPrincipalCost()) : "-"}@if(row.averageDailyPrincipalCost() == null)<span class="sr-only">${dailyBasisNoneTitle}</span>@endif</td>
										<td class="text-right text-dividend font-medium" data-amount-basis="${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}" title="${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}">${row.averageDailyPrincipalCost() != null && row.averageDailyPrincipalCost().signum() > 0 ? percentFormat.apply(row.yieldOnDailyAverageCostPct()) : "-"}@if(yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount()) != null)<span class="sr-only">${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}</span>@endif</td>
										<td class="text-right hidden xl:table-cell print:table-cell amount-value" title="${basisNoneOrNull.apply(row.averagePrincipalCost())}">${row.averagePrincipalCost() != null ? decimalFormat.format(row.averagePrincipalCost()) : "-"}@if(basisNoneOrNull.apply(row.averagePrincipalCost()) != null)<span class="sr-only">${basisNoneTitle}</span>@endif</td>
										<td class="text-right hidden xl:table-cell print:table-cell text-base-content/70" data-amount-basis="${basisNoneOrNull.apply(row.averagePrincipalCost()) != null ? basisNoneTitle : yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}" title="${basisNoneOrNull.apply(row.averagePrincipalCost()) != null ? basisNoneTitle : yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}">${percentFormat.apply(row.yieldOnCostPct())}</td>
										<td class="text-right hidden min-[1150px]:table-cell print:table-cell text-base-content/70" data-amount-basis="${yieldBasisTitle.apply(row.netAmountWithPrincipalMarket(), row.totalNetAmount())}" title="${yieldBasisTitle.apply(row.netAmountWithPrincipalMarket(), row.totalNetAmount())}">${percentFormat.apply(row.yieldOnMarketPct())}</td>
										<td class="text-right">${countFormat.apply(row.dividendCount())}</td>
									</tr>
								@endfor
							</tbody>
							<tfoot>
								<tr class="bg-base-200 font-bold">
									<th scope="row">${totalLabel}</th>
									<td class="text-right hidden min-[1150px]:table-cell print:table-cell font-mono amount-value">${decimalFormat.format(stockYieldFooterGrossAmount)}</td>
									<td class="text-right font-mono text-dividend amount-value">${decimalFormat.format(stockYieldFooterNetAmount)}</td>
									<td class="text-right hidden min-[1150px]:table-cell print:table-cell font-mono amount-value">${decimalFormat.format(stockYieldFooterTaxAmount)}</td>
									<td class="text-right hidden min-[1150px]:table-cell print:table-cell font-mono amount-value">${decimalFormat.format(stockYieldFooterTaxableAmount)}</td>
									<td class="text-right hidden min-[1150px]:table-cell print:table-cell font-mono amount-value">${decimalFormat.format(stockYieldFooterDailyPrincipalCost)}</td>
									<td class="text-right text-dividend font-medium" data-amount-basis="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}" title="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}">${percentFormat.apply(stockYieldFooterYieldOnDailyAverageCostPct)}@if(yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount) != null)<span class="sr-only">${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}</span>@endif</td>
									<td class="text-right hidden xl:table-cell print:table-cell font-mono amount-value">${decimalFormat.format(stockYieldFooterPrincipalCost)}</td>
									<td class="text-right hidden xl:table-cell print:table-cell text-base-content/70" data-amount-basis="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}" title="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}">${percentFormat.apply(stockYieldFooterYieldOnCostPct)}</td>
									<td class="text-right hidden min-[1150px]:table-cell print:table-cell text-base-content/70" data-amount-basis="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalMarket, stockYieldFooterNetAmount)}" title="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalMarket, stockYieldFooterNetAmount)}">${percentFormat.apply(stockYieldFooterYieldOnMarketPct)}</td>
									<td class="text-right">${countFormat.apply(stockYieldFooterDividendCount)}</td>
								</tr>
							</tfoot>
						</table>
					</div>
				@endif
			</div>
		</div>

		<div class="card min-w-0 bg-base-100 shadow-sm border border-base-200">
			<div class="card-body p-4" data-dividend-yield-selection-section>
				<h2 class="text-sm font-bold mb-3" data-page-section>${accountYieldSectionLabel}</h2>
				@if(accountYieldRows == null || accountYieldRows.isEmpty())
					<div class="text-sm text-base-content/60">${noYieldAnalyticsLabel}</div>
				@else
					<div class="mb-3 flex flex-col gap-3">
						<p class="text-xs text-base-content/60">${accountYieldSelectionHelperLabel}</p>
						<div class="selection-reveal"><div>
						<div class="rounded-2xl border border-secondary/15 bg-secondary/5 px-4 py-3"
							 data-dividend-yield-selection-summary
							 data-count-template="${accountYieldSelectionCountLabel}"
							 data-total-net-amount="${safeAllNet.toPlainString()}"
							 data-weight-label="${dividendYieldSelectionWeightLabel}"
							 data-basis-label="${yieldOnBasisAverageCostLabel}"
							 aria-live="polite">
							<div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
								<div class="flex flex-wrap items-center gap-2">
									<h3 class="text-sm font-semibold">${accountYieldSelectionSummaryLabel}</h3>
									<span class="badge badge-secondary badge-outline" data-dividend-yield-selection-count>${accountYieldSelectionSummaryLabel}</span>
									<span class="badge badge-ghost" data-dividend-yield-selection-weight-badge>${dividendYieldSelectionWeightLabel} 0.0%</span>
								</div>
								<button type="button" class="btn btn-ghost btn-xs self-start" data-dividend-yield-selection-clear>${dividendYieldSelectionClearLabel}</button>
							</div>
							<div class="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-6">
								<div class="rounded-box bg-base-100/80 px-3 py-2">
									<div class="text-xs text-base-content/60">${grossAmountLabel}</div>
									<div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-gross>0</div>
								</div>
								<div class="rounded-box bg-base-100/80 px-3 py-2">
									<div class="text-xs text-base-content/60">${netAmountLabel}</div>
									<div class="mt-1 font-mono font-semibold amount-value text-dividend" data-dividend-yield-selection-net>0</div>
								</div>
								<div class="rounded-box bg-base-100/80 px-3 py-2">
									<div class="text-xs text-base-content/60">${taxLabel}</div>
									<div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-tax>0</div>
								</div>
								<div class="rounded-box bg-base-100/80 px-3 py-2">
									<div class="text-xs text-base-content/60">${taxableAmountLabel}</div>
									<div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-taxable>0</div>
								</div>
								<div class="rounded-box bg-base-100/80 px-3 py-2">
									<div class="text-xs text-base-content/60">${dailyAverageInvestedCapitalLabel}</div>
									<div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-daily-capital>0</div>
								</div>
								<div class="rounded-box bg-base-100/80 px-3 py-2">
									<div class="text-xs text-base-content/60">${yieldOnDailyAverageCostLabel}</div>
									<div class="mt-1 font-mono font-semibold text-dividend" data-dividend-yield-selection-yield>0.00%</div>
									<div class="text-xs text-base-content/60" data-dividend-yield-selection-basis-yield>${yieldOnBasisAverageCostLabel} 0.00%</div>
								</div>
							</div>
						</div>
						</div></div>
					</div>
					<div class="overflow-x-auto">
						<%-- 이 표는 11열이라 2xl(1536px)에서 펴면 표가 1,380px 인데 본문 열은 1,246px 뿐이라 가로 스크롤이 생긴다
					     (실측 2026-09-10: 1536px 넘침 134px · 1600px 70px · 1660px 10px · 1680px 0px). 1536px 은 1920x1080 을
					     125% 배율로 쓸 때의 폭이라 흔하다. 2026-09-10 에 헤더 정렬 버튼을 접히게 한 뒤 이 표의 최소폭이
					     1,300px -> 840px 로 줄어, 1680px 이 아니라 1150px 부터 넘침 0 이다(1120px 10 · 1150px 0).
					     Tailwind 테마에 3xl 을 넣으면 .container 최대폭까지 같이 바뀌어(게시판 화면 사용) 임의 분기점을 쓴다. --%>
						<table class="table table-xs w-full" data-dividend-yield-selection-table data-dividend-yield-sortable-table aria-label="${accountYieldSectionLabel}">
							<thead>
								<tr class="text-base-content/60">
									<th scope="col">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-start px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="label"
												data-sort-type="text">
											<span>${accountLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right hidden min-[1150px]:table-cell print:table-cell">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="grossAmount"
												data-sort-type="number">
											<span>${grossAmountLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="netAmount"
												data-sort-type="number">
											<span>${netAmountLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right hidden min-[1150px]:table-cell print:table-cell">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="taxAmount"
												data-sort-type="number">
											<span>${taxLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right hidden min-[1150px]:table-cell print:table-cell">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="taxableAmount"
												data-sort-type="number">
											<span>${taxableAmountLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right hidden min-[1150px]:table-cell print:table-cell">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="averageDailyPrincipalCost"
												data-sort-type="number">
											<span>${dailyAverageInvestedCapitalLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="yieldOnDailyAverageCost"
												data-sort-type="number">
											<span>${yieldOnDailyAverageCostLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right hidden xl:table-cell print:table-cell">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="averagePrincipalCost"
												data-sort-type="number">
											<span>${basisAverageInvestedCapitalLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right hidden xl:table-cell print:table-cell">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="yieldOnCost"
												data-sort-type="number">
											<span>${yieldOnBasisAverageCostLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right hidden min-[1150px]:table-cell print:table-cell">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="yieldOnMarket"
												data-sort-type="number">
											<span>${yieldOnBasisMarketLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="dividendCount"
												data-sort-type="number">
											<span>${dividendCountLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
								</tr>
							</thead>
							<tbody>
								@for(DividendYieldGroupView row : accountYieldRows)
									!{BigDecimal rowTaxAmount = row.totalGrossAmount() != null && row.totalNetAmount() != null ? row.totalGrossAmount().subtract(row.totalNetAmount()) : null;}
									<tr class="dividend-yield-account-row"
										data-dividend-yield-row
										data-selection-class="dividend-yield-account-row-selected"
										tabindex="0"
										aria-selected="false"
										data-row-select="${MessageUtil.getMessage("stock.table.row.select")}"
										data-label="${row.label() != null ? row.label() : ""}"
										data-gross-amount="${row.totalGrossAmount() != null ? row.totalGrossAmount().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-net-amount="${row.totalNetAmount() != null ? row.totalNetAmount().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-tax-amount="${rowTaxAmount != null ? rowTaxAmount.toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-taxable-amount="${row.totalTaxableAmount() != null ? row.totalTaxableAmount().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-average-daily-principal-cost="${row.averageDailyPrincipalCost() != null ? row.averageDailyPrincipalCost().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-yield-on-daily-average-cost="${row.yieldOnDailyAverageCostPct() != null ? row.yieldOnDailyAverageCostPct().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-average-principal-cost="${row.averagePrincipalCost() != null ? row.averagePrincipalCost().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-net-with-principal-cost="${row.netAmountWithPrincipalCost() != null ? row.netAmountWithPrincipalCost().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-yield-on-cost="${row.yieldOnCostPct() != null ? row.yieldOnCostPct().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-yield-on-market="${row.yieldOnMarketPct() != null ? row.yieldOnMarketPct().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-dividend-count="${String.valueOf(row.dividendCount())}">
										<th scope="row" class="font-medium"><a href="/stock/account?accountId=${row.groupId() != null ? row.groupId().toString() : ""}" class="link link-hover">${row.label()}</a></th>
										<td class="text-right hidden min-[1150px]:table-cell print:table-cell amount-value">${decimalFormat.format(row.totalGrossAmount())}</td>
										<td class="text-right amount-value">${decimalFormat.format(row.totalNetAmount())}</td>
										<td class="text-right hidden min-[1150px]:table-cell print:table-cell amount-value" title="${rowTaxAmount != null && rowTaxAmount.signum() == 0 ? zeroAmountTitle : null}" aria-label="${rowTaxAmount != null && rowTaxAmount.signum() == 0 ? zeroAmountTitle : null}">${rowTaxAmount != null && rowTaxAmount.signum() != 0 ? decimalFormat.format(rowTaxAmount) : "-"}</td>
										<td class="text-right hidden min-[1150px]:table-cell print:table-cell amount-value" title="${row.totalTaxableAmount() != null && row.totalTaxableAmount().signum() == 0 ? zeroAmountTitle : null}" aria-label="${row.totalTaxableAmount() != null && row.totalTaxableAmount().signum() == 0 ? zeroAmountTitle : null}">${row.totalTaxableAmount() != null && row.totalTaxableAmount().signum() != 0 ? decimalFormat.format(row.totalTaxableAmount()) : "-"}</td>
										<td class="text-right hidden min-[1150px]:table-cell print:table-cell amount-value" title="${row.averageDailyPrincipalCost() == null ? dailyBasisNoneTitle : null}">${row.averageDailyPrincipalCost() != null ? decimalFormat.format(row.averageDailyPrincipalCost()) : "-"}@if(row.averageDailyPrincipalCost() == null)<span class="sr-only">${dailyBasisNoneTitle}</span>@endif</td>
										<td class="text-right text-dividend font-medium" data-amount-basis="${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}" title="${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}">${row.averageDailyPrincipalCost() != null && row.averageDailyPrincipalCost().signum() > 0 ? percentFormat.apply(row.yieldOnDailyAverageCostPct()) : "-"}@if(yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount()) != null)<span class="sr-only">${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}</span>@endif</td>
										<td class="text-right hidden xl:table-cell print:table-cell amount-value" title="${basisNoneOrNull.apply(row.averagePrincipalCost())}">${row.averagePrincipalCost() != null ? decimalFormat.format(row.averagePrincipalCost()) : "-"}@if(basisNoneOrNull.apply(row.averagePrincipalCost()) != null)<span class="sr-only">${basisNoneTitle}</span>@endif</td>
										<td class="text-right hidden xl:table-cell print:table-cell text-base-content/70" data-amount-basis="${basisNoneOrNull.apply(row.averagePrincipalCost()) != null ? basisNoneTitle : yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}" title="${basisNoneOrNull.apply(row.averagePrincipalCost()) != null ? basisNoneTitle : yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}">${percentFormat.apply(row.yieldOnCostPct())}</td>
										<td class="text-right hidden min-[1150px]:table-cell print:table-cell text-base-content/70" data-amount-basis="${yieldBasisTitle.apply(row.netAmountWithPrincipalMarket(), row.totalNetAmount())}" title="${yieldBasisTitle.apply(row.netAmountWithPrincipalMarket(), row.totalNetAmount())}">${percentFormat.apply(row.yieldOnMarketPct())}</td>
										<td class="text-right">${countFormat.apply(row.dividendCount())}</td>
									</tr>
								@endfor
							</tbody>
							<tfoot>
								<tr class="bg-base-200 font-bold">
									<th scope="row">${totalLabel}</th>
									<td class="text-right hidden min-[1150px]:table-cell print:table-cell font-mono amount-value">${decimalFormat.format(accountYieldFooterGrossAmount)}</td>
									<td class="text-right font-mono text-dividend amount-value">${decimalFormat.format(accountYieldFooterNetAmount)}</td>
									<td class="text-right hidden min-[1150px]:table-cell print:table-cell font-mono amount-value">${decimalFormat.format(accountYieldFooterTaxAmount)}</td>
									<td class="text-right hidden min-[1150px]:table-cell print:table-cell font-mono amount-value">${decimalFormat.format(accountYieldFooterTaxableAmount)}</td>
									<td class="text-right hidden min-[1150px]:table-cell print:table-cell font-mono amount-value">${decimalFormat.format(accountYieldFooterDailyPrincipalCost)}</td>
									<td class="text-right text-dividend font-medium" data-amount-basis="${yieldBasisTitle.apply(accountYieldFooterNetWithPrincipalCost, accountYieldFooterNetAmount)}" title="${yieldBasisTitle.apply(accountYieldFooterNetWithPrincipalCost, accountYieldFooterNetAmount)}">${percentFormat.apply(accountYieldFooterYieldOnDailyAverageCostPct)}@if(yieldBasisTitle.apply(accountYieldFooterNetWithPrincipalCost, accountYieldFooterNetAmount) != null)<span class="sr-only">${yieldBasisTitle.apply(accountYieldFooterNetWithPrincipalCost, accountYieldFooterNetAmount)}</span>@endif</td>
									<td class="text-right hidden xl:table-cell print:table-cell font-mono amount-value">${decimalFormat.format(accountYieldFooterPrincipalCost)}</td>
									<td class="text-right hidden xl:table-cell print:table-cell text-base-content/70" data-amount-basis="${yieldBasisTitle.apply(accountYieldFooterNetWithPrincipalCost, accountYieldFooterNetAmount)}" title="${yieldBasisTitle.apply(accountYieldFooterNetWithPrincipalCost, accountYieldFooterNetAmount)}">${percentFormat.apply(accountYieldFooterYieldOnCostPct)}</td>
									<td class="text-right hidden min-[1150px]:table-cell print:table-cell text-base-content/70" data-amount-basis="${yieldBasisTitle.apply(accountYieldFooterNetWithPrincipalMarket, accountYieldFooterNetAmount)}" title="${yieldBasisTitle.apply(accountYieldFooterNetWithPrincipalMarket, accountYieldFooterNetAmount)}">${percentFormat.apply(accountYieldFooterYieldOnMarketPct)}</td>
									<td class="text-right">${countFormat.apply(accountYieldFooterDividendCount)}</td>
								</tr>
							</tfoot>
						</table>
					</div>
				@endif
			</div>
		</div>
	</div>

	<style>
		.dividend-yield-stock-row,
		.dividend-yield-account-row {
			cursor: pointer;
		}

		.dividend-yield-stock-row.dividend-yield-stock-row-selected > td {
			background-color: color-mix(in oklch, var(--color-info) 10%, var(--color-base-100));
		}

		.dividend-yield-stock-row.dividend-yield-stock-row-selected > td:first-child {
			box-shadow: inset 3px 0 0 color-mix(in oklch, var(--color-info) 65%, transparent);
		}

		.dividend-yield-account-row.dividend-yield-account-row-selected > td {
			background-color: color-mix(in oklch, var(--color-secondary) 10%, var(--color-base-100));
		}

		.dividend-yield-account-row.dividend-yield-account-row-selected > td:first-child {
			box-shadow: inset 3px 0 0 color-mix(in oklch, var(--color-secondary) 65%, transparent);
		}

		.dividend-yield-stock-row:focus-visible,
		.dividend-yield-account-row:focus-visible {
			outline: 2px solid color-mix(in oklch, var(--color-info) 35%, transparent);
			outline-offset: -2px;
		}
	</style>
