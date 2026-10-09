@import java.util.List
@import java.util.UUID
@import java.math.BigDecimal
@import java.math.RoundingMode
@import java.text.DecimalFormat
@import net.luversof.web.gate.stock.domain.StockItem
@import net.luversof.web.gate.stock.dto.view.DividendYieldGroupView
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.stock.util.StockDetailLinkUtil
@import net.luversof.web.gate.stock.util.StockFormatUtil
@import net.luversof.web.gate.stock.util.DividendHoldingProfitUtil

@param DecimalFormat decimalFormat
@param java.util.function.Function<BigDecimal, String> percentFormat
@param java.util.function.Function<Long, String> countFormat
@param BigDecimal safeAllNet
@param BigDecimal portfolioYieldOnDailyAverageCostPct
@param BigDecimal portfolioYieldOnCostPct
@param BigDecimal portfolioYieldOnMarketPct
@param BigDecimal portfolioYieldAnnualizedPct = null
@param DividendYieldGroupView portfolioYield = null
@param java.util.Map<UUID, BigDecimal> currentValueByStock = null
@param java.util.Map<UUID, BigDecimal> currentValueByAccount = null
@param java.util.Map<UUID, BigDecimal> evaluationProfitByStock = null
@param java.util.Map<UUID, BigDecimal> evaluationProfitByAccount = null
@param java.util.Map<UUID, BigDecimal> realizedProfitByStock = null
@param java.util.Map<UUID, BigDecimal> realizedProfitByAccount = null
@param java.util.Map<UUID, BigDecimal> totalBuyCostByStock = null
@param java.util.Map<UUID, BigDecimal> totalBuyCostByAccount = null
@param BigDecimal ttmYieldPct = null
@param BigDecimal ttmAverageDailyPrincipalCost = null
@param BigDecimal ttmNetWithPrincipalCost = null
@param BigDecimal ttmNetAmount = null
@param java.time.LocalDate ttmStartDate = null
@param java.time.LocalDate ttmEndDate = null
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
	// 용어 풀이(사용자 요청 2026-09-28: "볼 때마다 이게 무슨 의미인지 헷갈려"). 풍선 · 용어 풀이 · 정렬 단추 설명이 같은 문구를 쓴다.
	String yieldGlossaryTitleLabel = MessageUtil.getMessage("stock.dividend.yield.term.glossary.title");
	String dailyCapitalTermDesc = MessageUtil.getMessage("stock.dividend.yield.term.daily.average.invested.capital.desc");
	String basisCapitalTermDesc = MessageUtil.getMessage("stock.dividend.yield.term.basis.average.invested.capital.desc");
	String basisMarketTermDesc = MessageUtil.getMessage("stock.dividend.yield.term.basis.market.desc");
	// 2026-09-28 사용자 요청("다양한 수익률과 투입원금이 잘 눈에 안 들어온다") - 기본은 연 수익률(들고 있던 날 기준) 하나, 나머지는 '열 더 보기'.
	String heldAverageLabel = MessageUtil.getMessage("stock.dividend.yield.column.held.average.principal");
	// 2026-09-28 사용자: "평균 투입원금은 기간 평균이라 - 표를 보며 지금 평가 금액이 얼마인지 알고 싶다". 기본 열은 현재 평가금액, 평균 투입원금은 열 더 보기.
	String currentValueLabel = MessageUtil.getMessage("stock.dividend.yield.column.current.value");
	String currentValueNoneTitle = MessageUtil.getMessage("stock.dividend.yield.current.value.none");
	java.util.Map<UUID, BigDecimal> stockCurrentValues = currentValueByStock != null ? currentValueByStock : java.util.Map.of();
	java.util.Map<UUID, BigDecimal> accountCurrentValues = currentValueByAccount != null ? currentValueByAccount : java.util.Map.of();
	// 원금 대비 이익인가 손해인가(사용자 요청 2026-09-28: "평가금액이 원금에 대해 수익인지 손해인지", "합산 수익률도"). 원금 = 평가금액 - 평가 손익.
	String evaluationProfitLabel = MessageUtil.getMessage("stock.dividend.yield.column.evaluation.profit");
	String combinedProfitLabel = MessageUtil.getMessage("stock.dividend.yield.column.combined.profit");
	String evaluationProfitTermDesc = MessageUtil.getMessage("stock.dividend.yield.term.evaluation.profit.desc");
	String combinedProfitTermDesc = MessageUtil.getMessage("stock.dividend.yield.term.combined.profit.desc");
	// 실현 손익(사용자 승인 2026-09-28: 평가 + 실현 + 배당 = 합산). 판 적이 없으면 대시와 까닭(자산현황과 같은 문구).
	String realizedProfitLabel = MessageUtil.getMessage("stock.dividend.yield.column.realized.profit");
	String realizedProfitTermDesc = MessageUtil.getMessage("stock.dividend.yield.term.realized.profit.desc");
	String realizedProfitNoneTitle = MessageUtil.getMessage("stock.profit.realized.none");
	java.util.Map<UUID, BigDecimal> stockRealizedProfits = realizedProfitByStock != null ? realizedProfitByStock : java.util.Map.of();
	java.util.Map<UUID, BigDecimal> accountRealizedProfits = realizedProfitByAccount != null ? realizedProfitByAccount : java.util.Map.of();
	java.util.Map<UUID, BigDecimal> stockTotalBuyCosts = totalBuyCostByStock != null ? totalBuyCostByStock : java.util.Map.of();
	java.util.Map<UUID, BigDecimal> accountTotalBuyCosts = totalBuyCostByAccount != null ? totalBuyCostByAccount : java.util.Map.of();
	String evaluationProfitNoneTitle = MessageUtil.getMessage("stock.dividend.yield.evaluation.profit.none");
	java.util.Map<UUID, BigDecimal> stockEvaluationProfits = evaluationProfitByStock != null ? evaluationProfitByStock : java.util.Map.of();
	java.util.Map<UUID, BigDecimal> accountEvaluationProfits = evaluationProfitByAccount != null ? evaluationProfitByAccount : java.util.Map.of();
	// 손익 색은 빨강 = 이익 · 파랑 = 손해 · 0 은 중립(게이트 관례).
	java.util.function.Function<BigDecimal, String> profitClass = value -> value == null || value.signum() == 0 ? "text-base-content/70" : (value.signum() > 0 ? "text-profit" : "text-loss");
	java.util.function.Function<BigDecimal, String> signedPctText = value -> value == null ? "-" : StockFormatUtil.signedPct(value.doubleValue(), 2);
	java.util.function.Function<BigDecimal, String> combinedRateText = value -> java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.yield.combined.rate"), value == null ? "-" : StockFormatUtil.signedPct(value.doubleValue(), 2));
	BigDecimal stockCurrentValueTotal = stockCurrentValues.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
	BigDecimal accountCurrentValueTotal = accountCurrentValues.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
	String annualizedYieldLabel = MessageUtil.getMessage("stock.dividend.yield.column.annualized");
	String heldTermDesc = MessageUtil.getMessage("stock.dividend.yield.term.held.average.principal.desc");
	String annualizedTermDesc = MessageUtil.getMessage("stock.dividend.yield.term.annualized.desc");
	String extraColumnsToggleLabel = MessageUtil.getMessage("stock.dividend.yield.extra.toggle");
	String shortHeldLabel = MessageUtil.getMessage("stock.dividend.yield.cell.short.held");
	String ttmTitleLabel = MessageUtil.getMessage("stock.dividend.yield.ttm.title");
	String ttmNoneLabel = MessageUtil.getMessage("stock.dividend.yield.ttm.none");
	String periodAnnualizedLabel = MessageUtil.getMessage("stock.dividend.yield.period.annualized");
	String otherBasisLabel = MessageUtil.getMessage("stock.dividend.yield.other.basis");
	java.util.function.Function<Long, String> heldDaysText = days -> java.text.MessageFormat.format(
		MessageUtil.getMessage("stock.dividend.yield.cell.held.days"), days);
	java.util.function.Function<Long, String> shortHeldDesc = days -> java.text.MessageFormat.format(
		MessageUtil.getMessage("stock.dividend.yield.cell.short.held.desc"), days);
	// 올해처럼 기간이 한 해를 다 못 채운 연도 - 연 수익률은 그 날수를 1 년으로 늘린 값이라 밝힌다.
	java.util.function.Function<DividendYieldGroupView, Long> partialYearDays = row -> {
		try {
			int year = Integer.parseInt(row.label());
			return row.periodDayCount() > 0 && row.periodDayCount() < java.time.Year.of(year).length() ? row.periodDayCount() : null;
		} catch (NumberFormatException ex) {
			return null;
		}
	};
	BigDecimal yearlyMaxAnnualized = yearlyYieldRows == null ? BigDecimal.ZERO : yearlyYieldRows.stream()
		.map(DividendYieldGroupView::annualizedYieldPct).filter(java.util.Objects::nonNull)
		.reduce(BigDecimal.ZERO, BigDecimal::max);
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
	java.util.function.Function<List<DividendYieldGroupView>, BigDecimal> footerAnnualizedPct = rows -> {
		BigDecimal daySum = sumDividendYieldValue.apply(rows, DividendYieldGroupView::principalCostDaySum);
		BigDecimal usedNet = sumDividendYieldValue.apply(rows, DividendYieldGroupView::netAmountWithPrincipalCost);
		return net.luversof.web.gate.stock.util.StockYieldUtil.annualizedPctOnHeldDays(usedNet, daySum);
	};
	BigDecimal footerHeldAverage = portfolioYield != null ? portfolioYield.heldAverageDailyPrincipalCost() : null;
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
	// '기준일 평균시가 수익률' 만 분모가 제 열을 안 가졌다 — 형제 둘(기간 일평균 투입원금 ·
	// 배당 기준일 평균원금)은 그 자리가 열로 나와 있어 읽는 사람이 검산할 수 있는데 이 수익률만 못 한다
	// (실측 2026-09-15: 종목 16칸 + 계좌 6칸). 값은 이미 모델에 있으므로(averagePrincipalMarketValue)
	// 분자와 분모를 함께 밝힌다. 이 문장은 같은 행의 다른 근거와 **내용이 다르므로** sr-only 로도 내보낸다.
	java.util.function.BiFunction<BigDecimal, BigDecimal, String> marketBasisTitle = (usedNet, base) -> {
		if (base == null || base.signum() <= 0) {
			return null;
		}
		return java.text.MessageFormat.format(
			MessageUtil.getMessage("stock.dividend.yield.market.basis"),
			decimalFormat.format(usedNet == null ? BigDecimal.ZERO : usedNet), decimalFormat.format(base));
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
		<details class="mt-1 text-xs" data-yield-glossary>
			<summary class="w-fit cursor-pointer text-base-content/70 hover:text-base-content">${yieldGlossaryTitleLabel}</summary>
			<dl class="mt-1 space-y-2 rounded-box bg-base-200/60 px-3 py-2 leading-5">
				<div>
					<dt class="font-semibold">${annualizedYieldLabel}</dt>
					<dd id="dividend-yield-term-annualized" class="text-base-content/70">${annualizedTermDesc}</dd>
				</div>
				<div>
					<dt class="font-semibold">${evaluationProfitLabel}</dt>
					<dd id="dividend-yield-term-evaluation-profit" class="text-base-content/70">${evaluationProfitTermDesc}</dd>
				</div>
				<div>
					<dt class="font-semibold">${realizedProfitLabel}</dt>
					<dd id="dividend-yield-term-realized-profit" class="text-base-content/70">${realizedProfitTermDesc}</dd>
				</div>
				<div>
					<dt class="font-semibold">${combinedProfitLabel}</dt>
					<dd id="dividend-yield-term-combined-profit" class="text-base-content/70">${combinedProfitTermDesc}</dd>
				</div>
				<div>
					<dt class="font-semibold">${heldAverageLabel}</dt>
					<dd id="dividend-yield-term-held" class="text-base-content/70">${heldTermDesc}</dd>
				</div>
<div>
					<dt class="font-semibold">${dailyAverageInvestedCapitalLabel}</dt>
					<dd id="dividend-yield-term-daily" class="text-base-content/70">${dailyCapitalTermDesc}</dd>
				</div>
				<div>
					<dt class="font-semibold">${basisAverageInvestedCapitalLabel}</dt>
					<dd id="dividend-yield-term-basis" class="text-base-content/70">${basisCapitalTermDesc}</dd>
				</div>
				<div>
					<dt class="font-semibold">${yieldOnBasisMarketLabel}</dt>
					<dd id="dividend-yield-term-market" class="text-base-content/70">${basisMarketTermDesc}</dd>
				</div>
			</dl>
		</details>
	</div>

	<div class="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-4">
		@template._components.ui.card(body = @`
				<div class="text-xs text-base-content/60 mb-1" data-ttm-title>${ttmTitleLabel}@if(ttmStartDate != null && ttmEndDate != null) <span class="text-base-content/60">(<span class="whitespace-nowrap date-whole">${ttmStartDate.toString()}</span> ~ <span class="whitespace-nowrap date-whole">${ttmEndDate.toString()}</span>)</span>@endif</div>
				@if(ttmYieldPct != null)
					<div class="text-2xl font-bold text-dividend" data-ttm-yield>${java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.yield.ttm.value"), percentFormat.apply(ttmYieldPct))}</div>
					<p class="text-xs text-base-content/70 mt-1 leading-5 amount-value" data-ttm-sentence>${java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.yield.ttm.sentence"), decimalFormat.format(ttmAverageDailyPrincipalCost != null ? ttmAverageDailyPrincipalCost : BigDecimal.ZERO), decimalFormat.format(ttmNetWithPrincipalCost != null ? ttmNetWithPrincipalCost : BigDecimal.ZERO))}</p>
					@if(ttmNetAmount != null)
					<div class="text-xs text-base-content/60 mt-1 amount-value" data-ttm-monthly>${java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.yield.ttm.monthly"), decimalFormat.format(ttmNetAmount.divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP)))}</div>
					@endif
				@else
					<div class="text-sm text-base-content/60 mt-1" data-ttm-none>${ttmNoneLabel}</div>
				@endif
				@if(portfolioYieldAnnualizedPct != null)
				<div class="mt-2 border-t border-base-200/70 pt-2 text-xs text-base-content/60" data-period-annualized>
					@template._components.ui.termHint(label = periodAnnualizedLabel, desc = annualizedTermDesc) <span class="font-semibold text-dividend">${percentFormat.apply(portfolioYieldAnnualizedPct)}</span>
					<div class="text-[11px] text-base-content/60">${annualizedNoteLabel}</div>
				</div>
				@endif
				<details class="mt-2 text-xs" data-yield-other-basis>
					<summary class="w-fit cursor-pointer text-base-content/70 hover:text-base-content">${otherBasisLabel}</summary>
					<div class="mt-1 space-y-1">
						<div class="text-base-content/60">@template._components.ui.termHint(label = dividendYieldPeriodDailyLabel, desc = dailyCapitalTermDesc) <span class="font-medium">${percentFormat.apply(portfolioYieldOnDailyAverageCostPct)}</span></div>
						<div class="text-base-content/60">@template._components.ui.termHint(label = dividendYieldPeriodBasisLabel, desc = basisCapitalTermDesc) <span class="font-medium">${percentFormat.apply(portfolioYieldOnCostPct)}</span></div>
						<div class="text-base-content/60">@template._components.ui.termHint(label = yieldOnBasisMarketLabel, desc = basisMarketTermDesc) <span class="font-medium">${percentFormat.apply(portfolioYieldOnMarketPct)}</span></div>
					</div>
				</details>
				@if(periodPrincipalDelta != null)
				<div class="mt-2 border-t border-base-200/70 pt-2">
					<div class="text-xs text-base-content/60">${principalChangeLabel}</div>
					<div class="text-xs text-base-content/70 mt-0.5">
						<span class="text-base-content/60">${principalStartLabel}</span> <span class="font-mono amount-value">₩${decimalFormat.format(periodStartPrincipal)}</span>
						<span class="text-base-content/60 mx-1" aria-hidden="true">→</span>
						<span class="text-base-content/60">${principalEndLabel}</span> <span class="font-mono amount-value">₩${decimalFormat.format(periodEndPrincipal)}</span>
					</div>
					<div class="text-sm font-semibold mt-0.5 ${periodPrincipalDelta.signum() > 0 ? "text-profit" : periodPrincipalDelta.signum() < 0 ? "text-loss" : "text-base-content/60"}">
						<span class="font-mono amount-value">${periodPrincipalDelta.signum() == 0 ? "" : (periodPrincipalDelta.signum() > 0 ? "+" : "-")}₩${decimalFormat.format(periodPrincipalDelta.abs())}</span>
						@if(periodPrincipalDeltaPct != null)
						<span class="text-xs font-normal">(${periodPrincipalDelta.signum() > 0 ? "+" : ""}${percentFormat.apply(periodPrincipalDeltaPct)})</span>
						@endif
					</div>
				</div>
				@endif
		`)

		@template._components.ui.card(body = @`
				<div class="text-xs text-base-content/60 mb-1">${bestYieldStockLabel}</div>
				@if(bestYieldStock != null)
					<div class="text-sm font-semibold truncate" title="${bestYieldStock.label()}">${bestYieldStock.label()}</div>
					<div class="text-xl font-bold text-dividend" data-best-annualized>${java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.yield.ttm.value"), percentFormat.apply(bestYieldStock.annualizedYieldPct()))}</div>
					<div class="text-xs text-base-content/60 mt-1" data-best-current-value>${currentValueLabel} @if(bestYieldStock.groupId() != null && stockCurrentValues.get(bestYieldStock.groupId()) != null)<span class="font-medium amount-value">₩${decimalFormat.format(stockCurrentValues.get(bestYieldStock.groupId()))}</span>@else<span>${currentValueNoneTitle}</span>@endif <span class="text-base-content/60">· ${heldDaysText.apply(bestYieldStock.heldDayCount())}</span></div>
<div class="text-xs text-base-content/60 mt-1">${netAmountLabel} <span class="font-medium amount-value">₩${decimalFormat.format(bestYieldStock.totalNetAmount())}</span></div>
				@else
					<div class="text-sm text-base-content/60">${noYieldAnalyticsLabel}</div>
				@endif
		`)

		@template._components.ui.card(body = @`
				<div class="text-xs text-base-content/60 mb-1">${bestYieldAccountLabel}</div>
				@if(bestYieldAccount != null)
					<div class="text-sm font-semibold truncate" title="${bestYieldAccount.label()}">${bestYieldAccount.label()}</div>
					<div class="text-xl font-bold text-dividend" data-best-annualized>${java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.yield.ttm.value"), percentFormat.apply(bestYieldAccount.annualizedYieldPct()))}</div>
					<div class="text-xs text-base-content/60 mt-1" data-best-current-value>${currentValueLabel} @if(bestYieldAccount.groupId() != null && accountCurrentValues.get(bestYieldAccount.groupId()) != null)<span class="font-medium amount-value">₩${decimalFormat.format(accountCurrentValues.get(bestYieldAccount.groupId()))}</span>@else<span>${currentValueNoneTitle}</span>@endif <span class="text-base-content/60">· ${heldDaysText.apply(bestYieldAccount.heldDayCount())}</span></div>
<div class="text-xs text-base-content/60 mt-1">${netAmountLabel} <span class="font-medium amount-value">₩${decimalFormat.format(bestYieldAccount.totalNetAmount())}</span></div>
				@else
					<div class="text-sm text-base-content/60">${noYieldAnalyticsLabel}</div>
				@endif
		`)
	</div>

	<div class="grid grid-cols-1 gap-4 mb-4">
		@template._components.ui.card(cssClass = "min-w-0", body = @`
				<div data-yield-extra-scope>
				<div class="mb-3 flex flex-wrap items-center justify-between gap-2">
					<h2 class="text-sm font-bold" data-page-section>${yearlyYieldSectionLabel}</h2>
					<label class="label cursor-pointer gap-2 text-xs text-base-content/70 w-fit" data-yield-extra-toggle-label><input type="checkbox" class="toggle toggle-xs" data-yield-extra-toggle>${extraColumnsToggleLabel}</label>
				</div>
				@if(yearlyYieldRows == null || yearlyYieldRows.isEmpty())
					<div class="text-sm text-base-content/60">${noYieldAnalyticsLabel}</div>
				@else
					<div class="overflow-x-auto">
						<table class="table table-xs w-full" aria-label="${yearlyYieldSectionLabel}">
							<thead>
								<tr class="text-base-content/60">
									<th scope="col">${yearLabel}</th>
									<th scope="col" class="text-right yield-extra-col">${grossAmountLabel}</th>
									<th scope="col" class="text-right">@template._components.ui.termHint(label = netAmountLabel, desc = annualizedTermDesc)</th>
									<th scope="col" class="text-right yield-extra-col">@template._components.ui.termHint(label = heldAverageLabel, desc = heldTermDesc)</th>
									<th scope="col" class="text-right yield-extra-col">${taxLabel}</th>
									<th scope="col" class="text-right yield-extra-col">${taxableAmountLabel}</th>
									<th scope="col" class="text-right yield-extra-col">@template._components.ui.termHint(label = dailyAverageInvestedCapitalLabel, desc = dailyCapitalTermDesc)</th>
									<th scope="col" class="text-right yield-extra-col">${yieldOnDailyAverageCostLabel}</th>
									<th scope="col" class="text-right yield-extra-col">@template._components.ui.termHint(label = basisAverageInvestedCapitalLabel, desc = basisCapitalTermDesc)</th>
									<th scope="col" class="text-right yield-extra-col">${yieldOnBasisAverageCostLabel}</th>
									<th scope="col" class="text-right">${dividendCountLabel}</th>
								</tr>
							</thead>
							<tbody>
								@for(DividendYieldGroupView row : yearlyYieldRows)
									!{BigDecimal rowTaxAmount = row.totalGrossAmount() != null && row.totalNetAmount() != null ? row.totalGrossAmount().subtract(row.totalNetAmount()) : null;}
									<tr>
										<th scope="row" class="font-medium">${row.label()}@if(partialYearDays.apply(row) != null) <span class="text-[11px] font-normal text-base-content/60" data-year-partial data-stock-tag-tooltip="${java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.yield.year.partial.desc"), partialYearDays.apply(row))}">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.yield.year.partial"), partialYearDays.apply(row))}</span><span class="sr-only">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.yield.year.partial.desc"), partialYearDays.apply(row))}</span>@endif</th>
										<td class="text-right yield-extra-col amount-value">${decimalFormat.format(row.totalGrossAmount())}</td>
										<td class="text-right" data-annualized-cell data-amount-basis="${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}" title="${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}">@if(yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount()) != null)<span class="sr-only">${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}</span>@endif<div class="amount-value">${decimalFormat.format(row.totalNetAmount())}</div> <div class="text-[11px] whitespace-nowrap">@if(row.annualizedYieldPct() != null)<span class="font-semibold text-dividend" data-annualized-value>${java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.yield.ttm.value"), percentFormat.apply(row.annualizedYieldPct()))}</span>@else<span class="text-base-content/60" aria-hidden="true" title="${dailyBasisNoneTitle}">-</span><span class="sr-only">${dailyBasisNoneTitle}</span>@endif@if(row.heldDayCount() > 0)<span class="text-base-content/60" data-held-days> · ${heldDaysText.apply(row.heldDayCount())}</span>@if(row.shortHeld()) <span class="badge badge-outline badge-xs align-middle" data-short-held-badge data-stock-tag-tooltip="${shortHeldDesc.apply(row.heldDayCount())}">${shortHeldLabel}</span><span class="sr-only">${shortHeldDesc.apply(row.heldDayCount())}</span>@endif@endif</div>@if(yearlyMaxAnnualized.signum() > 0 && row.annualizedYieldPct() != null && row.groupId() == null)<div class="ml-auto mt-0.5 h-1 w-16 rounded-full bg-base-200" aria-hidden="true" data-yearly-bar><div class="h-1 rounded-full text-dividend yield-bar-fill" style="width: ${row.annualizedYieldPct().multiply(BigDecimal.valueOf(100)).divide(yearlyMaxAnnualized, 1, RoundingMode.HALF_UP).max(BigDecimal.ZERO).min(BigDecimal.valueOf(100)).toPlainString()}%"></div></div>@endif</td>
										<td class="text-right amount-value yield-extra-col" data-held-average-cell title="${row.heldAverageDailyPrincipalCost() == null ? dailyBasisNoneTitle : null}">${row.heldAverageDailyPrincipalCost() != null ? decimalFormat.format(row.heldAverageDailyPrincipalCost()) : "-"}</td>
										<td class="text-right yield-extra-col amount-value" title="${rowTaxAmount != null && rowTaxAmount.signum() == 0 ? zeroAmountTitle : null}" aria-label="${rowTaxAmount != null && rowTaxAmount.signum() == 0 ? zeroAmountTitle : null}">${rowTaxAmount != null && rowTaxAmount.signum() != 0 ? decimalFormat.format(rowTaxAmount) : "-"}</td>
										<td class="text-right yield-extra-col amount-value" title="${row.totalTaxableAmount() != null && row.totalTaxableAmount().signum() == 0 ? zeroAmountTitle : null}" aria-label="${row.totalTaxableAmount() != null && row.totalTaxableAmount().signum() == 0 ? zeroAmountTitle : null}">${row.totalTaxableAmount() != null && row.totalTaxableAmount().signum() != 0 ? decimalFormat.format(row.totalTaxableAmount()) : "-"}</td>
										<td class="text-right yield-extra-col amount-value" title="${row.averageDailyPrincipalCost() == null ? dailyBasisNoneTitle : null}">${row.averageDailyPrincipalCost() != null ? decimalFormat.format(row.averageDailyPrincipalCost()) : "-"}@if(row.averageDailyPrincipalCost() == null)<span class="sr-only">${dailyBasisNoneTitle}</span>@endif</td>
										<td class="text-right text-dividend font-medium yield-extra-col" data-amount-basis="${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}" title="${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}">${row.averageDailyPrincipalCost() != null && row.averageDailyPrincipalCost().signum() > 0 ? percentFormat.apply(row.yieldOnDailyAverageCostPct()) : "-"}</td>
										<td class="text-right yield-extra-col amount-value" title="${basisNoneOrNull.apply(row.averagePrincipalCost())}">${row.averagePrincipalCost() != null ? decimalFormat.format(row.averagePrincipalCost()) : "-"}@if(basisNoneOrNull.apply(row.averagePrincipalCost()) != null)<span class="sr-only">${basisNoneTitle}</span>@endif</td>
										<td class="text-right yield-extra-col text-base-content/70" data-amount-basis="${basisNoneOrNull.apply(row.averagePrincipalCost()) != null ? basisNoneTitle : yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}" title="${basisNoneOrNull.apply(row.averagePrincipalCost()) != null ? basisNoneTitle : yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}">${percentFormat.apply(row.yieldOnCostPct())}</td>
										<td class="text-right whitespace-nowrap">${countFormat.apply(row.dividendCount())}</td>
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
										<td class="text-right yield-extra-col font-mono amount-value">${decimalFormat.format(yearlyTotalGross)}</td>
										<td class="text-right text-dividend" data-annualized-total data-amount-basis="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}" title="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}">@if(yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount) != null)<span class="sr-only">${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}</span>@endif<div class="font-mono amount-value">${decimalFormat.format(yearlyTotalNet)}</div> <div class="text-[11px] font-normal whitespace-nowrap">${footerAnnualizedPct.apply(yearlyYieldRows) != null ? java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.yield.ttm.value"), percentFormat.apply(footerAnnualizedPct.apply(yearlyYieldRows))) : "-"}@if(portfolioYield != null && portfolioYield.heldDayCount() > 0)<span class="text-base-content/60"> · ${heldDaysText.apply(portfolioYield.heldDayCount())}</span>@endif</div></td>
										<td class="text-right font-mono amount-value yield-extra-col" data-held-average-total>${footerHeldAverage != null ? decimalFormat.format(footerHeldAverage) : "-"}</td>
										<td class="text-right yield-extra-col font-mono amount-value" title="${yearlyTotalTax.signum() == 0 ? zeroAmountTitle : null}" aria-label="${yearlyTotalTax.signum() == 0 ? zeroAmountTitle : null}">${yearlyTotalTax.signum() != 0 ? decimalFormat.format(yearlyTotalTax) : "-"}</td>
										<td class="text-right yield-extra-col font-mono amount-value" title="${yearlyTotalTaxable.signum() == 0 ? zeroAmountTitle : null}" aria-label="${yearlyTotalTaxable.signum() == 0 ? zeroAmountTitle : null}">${yearlyTotalTaxable.signum() != 0 ? decimalFormat.format(yearlyTotalTaxable) : "-"}</td>
										<td class="text-right yield-extra-col font-mono amount-value">${decimalFormat.format(stockYieldFooterDailyPrincipalCost)}</td>
										<td class="text-right text-dividend font-medium yield-extra-col" data-amount-basis="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}" title="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}">${stockYieldFooterDailyPrincipalCost.signum() > 0 ? percentFormat.apply(stockYieldFooterYieldOnDailyAverageCostPct) : "-"}</td>
										<td class="text-right yield-extra-col font-mono amount-value">${decimalFormat.format(stockYieldFooterPrincipalCost)}</td>
										<td class="text-right yield-extra-col text-base-content/70" data-amount-basis="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}" title="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}">${stockYieldFooterPrincipalCost.signum() > 0 ? percentFormat.apply(stockYieldFooterYieldOnCostPct) : "-"}</td>
										<td class="text-right whitespace-nowrap">${countFormat.apply(yearlyTotalCount)}</td>
									</tr>
								</tfoot>
							@endif
						</table>
					</div>
				@endif
				</div>
		`)

		<div class="card min-w-0 bg-base-100 shadow-sm border border-base-200">
			<div class="card-body p-4" data-dividend-yield-selection-section data-yield-extra-scope>
				<div class="mb-3 flex flex-wrap items-center justify-between gap-2">
					<h2 class="text-sm font-bold" data-page-section>${stockYieldSectionLabel}</h2>
					<label class="label cursor-pointer gap-2 text-xs text-base-content/70 w-fit" data-yield-extra-toggle-label><input type="checkbox" class="toggle toggle-xs" data-yield-extra-toggle>${extraColumnsToggleLabel}</label>
				</div>
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
									<div class="text-xs text-base-content/60">@template._components.ui.termHint(label = dailyAverageInvestedCapitalLabel, desc = dailyCapitalTermDesc)</div>
									<div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-daily-capital>0</div>
								</div>
								<div class="rounded-box bg-base-100/80 px-3 py-2">
								<div class="text-xs text-base-content/60">@template._components.ui.termHint(label = annualizedYieldLabel, desc = annualizedTermDesc)</div>
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
									<th scope="col" class="text-right yield-extra-col">
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
											data-sort-key="currentValue"
											data-sort-type="number">
										<span>${currentValueLabel}</span>
										<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
									</button>
								</th>
								<th scope="col" class="text-right">
									<button type="button"
											class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
											data-sort-key="evaluationProfit" aria-describedby="dividend-yield-term-evaluation-profit"
											data-sort-type="number">
										@template._components.ui.termHint(label = evaluationProfitLabel, desc = evaluationProfitTermDesc)
										<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
									</button>
								</th>
								<th scope="col" class="text-right">
									<button type="button"
											class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
											data-sort-key="realizedProfit" aria-describedby="dividend-yield-term-realized-profit"
											data-sort-type="number">
										@template._components.ui.termHint(label = realizedProfitLabel, desc = realizedProfitTermDesc)
										<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
									</button>
								</th>
									<th scope="col" class="text-right">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
											data-sort-key="netAmount" aria-describedby="dividend-yield-term-annualized"
											data-sort-type="number">
										@template._components.ui.termHint(label = netAmountLabel, desc = annualizedTermDesc)
										<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
									</button>
								</th>
								<th scope="col" class="text-right">
									<button type="button"
											class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
											data-sort-key="combinedProfit" aria-describedby="dividend-yield-term-combined-profit"
											data-sort-type="number">
										@template._components.ui.termHint(label = combinedProfitLabel, desc = combinedProfitTermDesc)
										<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
									</button>
								</th>
								<th scope="col" class="text-right yield-extra-col">
									<button type="button"
											class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
											data-sort-key="heldAverageDailyPrincipalCost" aria-describedby="dividend-yield-term-held"
											data-sort-type="number">
										@template._components.ui.termHint(label = heldAverageLabel, desc = heldTermDesc)
										<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
									</button>
								</th>
<th scope="col" class="text-right yield-extra-col">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="taxAmount"
												data-sort-type="number">
											<span>${taxLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right yield-extra-col">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="taxableAmount"
												data-sort-type="number">
											<span>${taxableAmountLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right yield-extra-col">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="averageDailyPrincipalCost" aria-describedby="dividend-yield-term-daily"
												data-sort-type="number">
											@template._components.ui.termHint(label = dailyAverageInvestedCapitalLabel, desc = dailyCapitalTermDesc)
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
								<th scope="col" class="text-right yield-extra-col">
									<button type="button"
											class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
											data-sort-key="yieldOnDailyAverageCost"
												data-sort-type="number">
											<span>${yieldOnDailyAverageCostLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right yield-extra-col">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="averagePrincipalCost" aria-describedby="dividend-yield-term-basis"
												data-sort-type="number">
											@template._components.ui.termHint(label = basisAverageInvestedCapitalLabel, desc = basisCapitalTermDesc)
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right yield-extra-col">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="yieldOnCost"
												data-sort-type="number">
											<span>${yieldOnBasisAverageCostLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right yield-extra-col">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="yieldOnMarket" aria-describedby="dividend-yield-term-market"
												data-sort-type="number">
											@template._components.ui.termHint(label = yieldOnBasisMarketLabel, desc = basisMarketTermDesc)
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
									!{var holdingProfit = DividendHoldingProfitUtil.ofRow(row, stockCurrentValues, stockEvaluationProfits, stockRealizedProfits, stockTotalBuyCosts);}
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
										data-dividend-count="${String.valueOf(row.dividendCount())}"
										data-annualized-yield="${row.annualizedYieldPct() != null ? row.annualizedYieldPct().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-held-average-daily-principal-cost="${row.heldAverageDailyPrincipalCost() != null ? row.heldAverageDailyPrincipalCost().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-principal-cost-day-sum="${row.principalCostDaySum() != null ? row.principalCostDaySum().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-held-day-count="${String.valueOf(row.heldDayCount())}"
										data-short-held="${String.valueOf(row.shortHeld())}"
										data-evaluation-profit="${holdingProfit != null && holdingProfit.evaluationProfit() != null ? holdingProfit.evaluationProfit().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-realized-profit="${holdingProfit != null && holdingProfit.realizedProfit() != null ? holdingProfit.realizedProfit().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-combined-profit="${holdingProfit != null ? holdingProfit.combinedProfit().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-current-value="${row.groupId() != null && stockCurrentValues.get(row.groupId()) != null ? stockCurrentValues.get(row.groupId()).toPlainString() : BigDecimal.ZERO.toPlainString()}">
										<th scope="row" class="font-medium"><a href="${StockDetailLinkUtil.item(row.groupId() != null ? row.groupId().toString() : "")}" class="link link-hover" data-stock-tag-tooltip="${stockTagTooltip}">${row.label()}</a></th>
										<td class="text-right yield-extra-col amount-value">${decimalFormat.format(row.totalGrossAmount())}</td>
										<td class="text-right amount-value" data-current-value-cell>@if(row.groupId() != null && stockCurrentValues.get(row.groupId()) != null)${decimalFormat.format(stockCurrentValues.get(row.groupId()))}@else<span class="text-base-content/60" aria-hidden="true" title="${currentValueNoneTitle}">-</span><span class="sr-only">${currentValueNoneTitle}</span>@endif</td>
										<td class="text-right" data-evaluation-profit-cell>@if(holdingProfit != null && holdingProfit.held())<span class="amount-value font-semibold ${profitClass.apply(holdingProfit.evaluationProfit())}">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(holdingProfit.evaluationProfit()))}</span> <div class="text-[11px] whitespace-nowrap ${profitClass.apply(holdingProfit.evaluationProfitPct())}">(${signedPctText.apply(holdingProfit.evaluationProfitPct())})</div>@else<span class="text-base-content/60" aria-hidden="true" title="${evaluationProfitNoneTitle}">-</span><span class="sr-only">${evaluationProfitNoneTitle}</span>@endif</td>
										<td class="text-right" data-realized-profit-cell>@if(holdingProfit != null && holdingProfit.realizedProfit() != null)<span class="amount-value font-semibold ${profitClass.apply(holdingProfit.realizedProfit())}">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(holdingProfit.realizedProfit()))}</span>@else<span class="text-base-content/60" aria-hidden="true" title="${realizedProfitNoneTitle}">-</span><span class="sr-only">${realizedProfitNoneTitle}</span>@endif</td>
										<td class="text-right" data-annualized-cell data-amount-basis="${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}" title="${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}">@if(yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount()) != null)<span class="sr-only">${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}</span>@endif<div class="amount-value">${decimalFormat.format(row.totalNetAmount())}</div> <div class="text-[11px] whitespace-nowrap">@if(row.annualizedYieldPct() != null)<span class="font-semibold text-dividend" data-annualized-value>${java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.yield.ttm.value"), percentFormat.apply(row.annualizedYieldPct()))}</span>@else<span class="text-base-content/60" aria-hidden="true" title="${dailyBasisNoneTitle}">-</span><span class="sr-only">${dailyBasisNoneTitle}</span>@endif@if(row.heldDayCount() > 0)<span class="text-base-content/60" data-held-days> · ${heldDaysText.apply(row.heldDayCount())}</span>@if(row.shortHeld()) <span class="badge badge-outline badge-xs align-middle" data-short-held-badge data-stock-tag-tooltip="${shortHeldDesc.apply(row.heldDayCount())}">${shortHeldLabel}</span><span class="sr-only">${shortHeldDesc.apply(row.heldDayCount())}</span>@endif@endif</div>@if(yearlyMaxAnnualized.signum() > 0 && row.annualizedYieldPct() != null && row.groupId() == null)<div class="ml-auto mt-0.5 h-1 w-16 rounded-full bg-base-200" aria-hidden="true" data-yearly-bar><div class="h-1 rounded-full text-dividend yield-bar-fill" style="width: ${row.annualizedYieldPct().multiply(BigDecimal.valueOf(100)).divide(yearlyMaxAnnualized, 1, RoundingMode.HALF_UP).max(BigDecimal.ZERO).min(BigDecimal.valueOf(100)).toPlainString()}%"></div></div>@endif</td>
										<td class="text-right" data-combined-profit-cell>@if(holdingProfit != null)<span class="amount-value font-semibold ${profitClass.apply(holdingProfit.combinedProfit())}">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(holdingProfit.combinedProfit()))}</span>@if(holdingProfit.combinedProfitPct() != null)<div class="text-[11px] whitespace-nowrap ${profitClass.apply(holdingProfit.combinedProfitPct())}">${combinedRateText.apply(holdingProfit.combinedProfitPct())}</div>@endif@else<span class="text-base-content/60" aria-hidden="true" title="${evaluationProfitNoneTitle}">-</span>@endif</td>
										<td class="text-right amount-value yield-extra-col" data-held-average-cell title="${row.heldAverageDailyPrincipalCost() == null ? dailyBasisNoneTitle : null}">${row.heldAverageDailyPrincipalCost() != null ? decimalFormat.format(row.heldAverageDailyPrincipalCost()) : "-"}</td>
										<td class="text-right yield-extra-col amount-value" title="${rowTaxAmount != null && rowTaxAmount.signum() == 0 ? zeroAmountTitle : null}" aria-label="${rowTaxAmount != null && rowTaxAmount.signum() == 0 ? zeroAmountTitle : null}">${rowTaxAmount != null && rowTaxAmount.signum() != 0 ? decimalFormat.format(rowTaxAmount) : "-"}</td>
										<td class="text-right yield-extra-col amount-value" title="${row.totalTaxableAmount() != null && row.totalTaxableAmount().signum() == 0 ? zeroAmountTitle : null}" aria-label="${row.totalTaxableAmount() != null && row.totalTaxableAmount().signum() == 0 ? zeroAmountTitle : null}">${row.totalTaxableAmount() != null && row.totalTaxableAmount().signum() != 0 ? decimalFormat.format(row.totalTaxableAmount()) : "-"}</td>
										<td class="text-right yield-extra-col amount-value" title="${row.averageDailyPrincipalCost() == null ? dailyBasisNoneTitle : null}">${row.averageDailyPrincipalCost() != null ? decimalFormat.format(row.averageDailyPrincipalCost()) : "-"}@if(row.averageDailyPrincipalCost() == null)<span class="sr-only">${dailyBasisNoneTitle}</span>@endif</td>
										<td class="text-right text-dividend font-medium yield-extra-col" data-amount-basis="${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}" title="${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}">${row.averageDailyPrincipalCost() != null && row.averageDailyPrincipalCost().signum() > 0 ? percentFormat.apply(row.yieldOnDailyAverageCostPct()) : "-"}</td>
										<td class="text-right yield-extra-col amount-value" title="${basisNoneOrNull.apply(row.averagePrincipalCost())}">${row.averagePrincipalCost() != null ? decimalFormat.format(row.averagePrincipalCost()) : "-"}@if(basisNoneOrNull.apply(row.averagePrincipalCost()) != null)<span class="sr-only">${basisNoneTitle}</span>@endif</td>
										<td class="text-right yield-extra-col text-base-content/70" data-amount-basis="${basisNoneOrNull.apply(row.averagePrincipalCost()) != null ? basisNoneTitle : yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}" title="${basisNoneOrNull.apply(row.averagePrincipalCost()) != null ? basisNoneTitle : yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}">${percentFormat.apply(row.yieldOnCostPct())}</td>
										<td class="text-right yield-extra-col text-base-content/70" data-amount-basis="${marketBasisTitle.apply(row.netAmountWithPrincipalMarket(), row.averagePrincipalMarketValue())}" title="${marketBasisTitle.apply(row.netAmountWithPrincipalMarket(), row.averagePrincipalMarketValue())}">${percentFormat.apply(row.yieldOnMarketPct())}@if(marketBasisTitle.apply(row.netAmountWithPrincipalMarket(), row.averagePrincipalMarketValue()) != null)<span class="sr-only">${marketBasisTitle.apply(row.netAmountWithPrincipalMarket(), row.averagePrincipalMarketValue())}</span>@endif</td>
										<td class="text-right whitespace-nowrap">${countFormat.apply(row.dividendCount())}</td>
									</tr>
								@endfor
							</tbody>
							<tfoot>
								<tr class="bg-base-200 font-bold">
									<th scope="row">${totalLabel}</th>
									<td class="text-right yield-extra-col font-mono amount-value">${decimalFormat.format(stockYieldFooterGrossAmount)}</td>
										!{var holdingProfitTotal = DividendHoldingProfitUtil.total(stockYieldRows, stockCurrentValues, stockEvaluationProfits, stockRealizedProfits, stockTotalBuyCosts);}
										<td class="text-right font-mono amount-value" data-current-value-total>${decimalFormat.format(stockCurrentValueTotal)}</td>
										<td class="text-right" data-evaluation-profit-total>@if(holdingProfitTotal != null && holdingProfitTotal.held())<span class="amount-value font-semibold ${profitClass.apply(holdingProfitTotal.evaluationProfit())}">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(holdingProfitTotal.evaluationProfit()))}</span> <div class="text-[11px] whitespace-nowrap ${profitClass.apply(holdingProfitTotal.evaluationProfitPct())}">(${signedPctText.apply(holdingProfitTotal.evaluationProfitPct())})</div>@else<span class="text-base-content/60" aria-hidden="true" title="${evaluationProfitNoneTitle}">-</span><span class="sr-only">${evaluationProfitNoneTitle}</span>@endif</td>
										<td class="text-right" data-realized-profit-total>@if(holdingProfitTotal != null && holdingProfitTotal.realizedProfit() != null)<span class="amount-value font-semibold ${profitClass.apply(holdingProfitTotal.realizedProfit())}">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(holdingProfitTotal.realizedProfit()))}</span>@else<span class="text-base-content/60" aria-hidden="true" title="${realizedProfitNoneTitle}">-</span><span class="sr-only">${realizedProfitNoneTitle}</span>@endif</td>
										<td class="text-right text-dividend" data-annualized-total data-amount-basis="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}" title="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}">@if(yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount) != null)<span class="sr-only">${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}</span>@endif<div class="font-mono amount-value">${decimalFormat.format(stockYieldFooterNetAmount)}</div> <div class="text-[11px] font-normal whitespace-nowrap">${footerAnnualizedPct.apply(stockYieldRows) != null ? java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.yield.ttm.value"), percentFormat.apply(footerAnnualizedPct.apply(stockYieldRows))) : "-"}@if(portfolioYield != null && portfolioYield.heldDayCount() > 0)<span class="text-base-content/60"> · ${heldDaysText.apply(portfolioYield.heldDayCount())}</span>@endif</div></td>
										<td class="text-right" data-combined-profit-total>@if(holdingProfitTotal != null)<span class="amount-value font-semibold ${profitClass.apply(holdingProfitTotal.combinedProfit())}">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(holdingProfitTotal.combinedProfit()))}</span>@if(holdingProfitTotal.combinedProfitPct() != null)<div class="text-[11px] whitespace-nowrap ${profitClass.apply(holdingProfitTotal.combinedProfitPct())}">${combinedRateText.apply(holdingProfitTotal.combinedProfitPct())}</div>@endif@else<span class="text-base-content/60" aria-hidden="true" title="${evaluationProfitNoneTitle}">-</span>@endif</td>
										<td class="text-right font-mono amount-value yield-extra-col" data-held-average-total>${footerHeldAverage != null ? decimalFormat.format(footerHeldAverage) : "-"}</td>
										<td class="text-right yield-extra-col font-mono amount-value">${decimalFormat.format(stockYieldFooterTaxAmount)}</td>
									<td class="text-right yield-extra-col font-mono amount-value">${decimalFormat.format(stockYieldFooterTaxableAmount)}</td>
									<td class="text-right yield-extra-col font-mono amount-value">${decimalFormat.format(stockYieldFooterDailyPrincipalCost)}</td>
									<td class="text-right text-dividend font-medium yield-extra-col" data-amount-basis="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}" title="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}">${percentFormat.apply(stockYieldFooterYieldOnDailyAverageCostPct)}</td>
									<td class="text-right yield-extra-col font-mono amount-value">${decimalFormat.format(stockYieldFooterPrincipalCost)}</td>
									<td class="text-right yield-extra-col text-base-content/70" data-amount-basis="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}" title="${yieldBasisTitle.apply(stockYieldFooterNetWithPrincipalCost, stockYieldFooterNetAmount)}">${percentFormat.apply(stockYieldFooterYieldOnCostPct)}</td>
									<td class="text-right yield-extra-col text-base-content/70" data-amount-basis="${marketBasisTitle.apply(stockYieldFooterNetWithPrincipalMarket, stockYieldFooterMarketValue)}" title="${marketBasisTitle.apply(stockYieldFooterNetWithPrincipalMarket, stockYieldFooterMarketValue)}">${percentFormat.apply(stockYieldFooterYieldOnMarketPct)}@if(marketBasisTitle.apply(stockYieldFooterNetWithPrincipalMarket, stockYieldFooterMarketValue) != null)<span class="sr-only">${marketBasisTitle.apply(stockYieldFooterNetWithPrincipalMarket, stockYieldFooterMarketValue)}</span>@endif</td>
									<td class="text-right whitespace-nowrap">${countFormat.apply(stockYieldFooterDividendCount)}</td>
								</tr>
							</tfoot>
						</table>
					</div>
				@endif
			</div>
		</div>

		<div class="card min-w-0 bg-base-100 shadow-sm border border-base-200">
			<div class="card-body p-4" data-dividend-yield-selection-section data-yield-extra-scope>
				<div class="mb-3 flex flex-wrap items-center justify-between gap-2">
					<h2 class="text-sm font-bold" data-page-section>${accountYieldSectionLabel}</h2>
					<label class="label cursor-pointer gap-2 text-xs text-base-content/70 w-fit" data-yield-extra-toggle-label><input type="checkbox" class="toggle toggle-xs" data-yield-extra-toggle>${extraColumnsToggleLabel}</label>
				</div>
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
									<div class="text-xs text-base-content/60">@template._components.ui.termHint(label = dailyAverageInvestedCapitalLabel, desc = dailyCapitalTermDesc)</div>
									<div class="mt-1 font-mono font-semibold amount-value" data-dividend-yield-selection-daily-capital>0</div>
								</div>
								<div class="rounded-box bg-base-100/80 px-3 py-2">
								<div class="text-xs text-base-content/60">@template._components.ui.termHint(label = annualizedYieldLabel, desc = annualizedTermDesc)</div>
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
									<th scope="col" class="text-right yield-extra-col">
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
											data-sort-key="currentValue"
											data-sort-type="number">
										<span>${currentValueLabel}</span>
										<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
									</button>
								</th>
								<th scope="col" class="text-right">
									<button type="button"
											class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
											data-sort-key="evaluationProfit" aria-describedby="dividend-yield-term-evaluation-profit"
											data-sort-type="number">
										@template._components.ui.termHint(label = evaluationProfitLabel, desc = evaluationProfitTermDesc)
										<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
									</button>
								</th>
								<th scope="col" class="text-right">
									<button type="button"
											class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
											data-sort-key="realizedProfit" aria-describedby="dividend-yield-term-realized-profit"
											data-sort-type="number">
										@template._components.ui.termHint(label = realizedProfitLabel, desc = realizedProfitTermDesc)
										<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
									</button>
								</th>
									<th scope="col" class="text-right">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
											data-sort-key="netAmount" aria-describedby="dividend-yield-term-annualized"
											data-sort-type="number">
										@template._components.ui.termHint(label = netAmountLabel, desc = annualizedTermDesc)
										<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
									</button>
								</th>
								<th scope="col" class="text-right">
									<button type="button"
											class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
											data-sort-key="combinedProfit" aria-describedby="dividend-yield-term-combined-profit"
											data-sort-type="number">
										@template._components.ui.termHint(label = combinedProfitLabel, desc = combinedProfitTermDesc)
										<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
									</button>
								</th>
								<th scope="col" class="text-right yield-extra-col">
									<button type="button"
											class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
											data-sort-key="heldAverageDailyPrincipalCost" aria-describedby="dividend-yield-term-held"
											data-sort-type="number">
										@template._components.ui.termHint(label = heldAverageLabel, desc = heldTermDesc)
										<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
									</button>
								</th>
<th scope="col" class="text-right yield-extra-col">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="taxAmount"
												data-sort-type="number">
											<span>${taxLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right yield-extra-col">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="taxableAmount"
												data-sort-type="number">
											<span>${taxableAmountLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right yield-extra-col">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="averageDailyPrincipalCost" aria-describedby="dividend-yield-term-daily"
												data-sort-type="number">
											@template._components.ui.termHint(label = dailyAverageInvestedCapitalLabel, desc = dailyCapitalTermDesc)
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
								<th scope="col" class="text-right yield-extra-col">
									<button type="button"
											class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
											data-sort-key="yieldOnDailyAverageCost"
												data-sort-type="number">
											<span>${yieldOnDailyAverageCostLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right yield-extra-col">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="averagePrincipalCost" aria-describedby="dividend-yield-term-basis"
												data-sort-type="number">
											@template._components.ui.termHint(label = basisAverageInvestedCapitalLabel, desc = basisCapitalTermDesc)
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right yield-extra-col">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="yieldOnCost"
												data-sort-type="number">
											<span>${yieldOnBasisAverageCostLabel}</span>
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									<th scope="col" class="text-right yield-extra-col">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="yieldOnMarket" aria-describedby="dividend-yield-term-market"
												data-sort-type="number">
											@template._components.ui.termHint(label = yieldOnBasisMarketLabel, desc = basisMarketTermDesc)
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
									!{var holdingProfit = DividendHoldingProfitUtil.ofRow(row, accountCurrentValues, accountEvaluationProfits, accountRealizedProfits, accountTotalBuyCosts);}
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
										data-dividend-count="${String.valueOf(row.dividendCount())}"
										data-annualized-yield="${row.annualizedYieldPct() != null ? row.annualizedYieldPct().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-held-average-daily-principal-cost="${row.heldAverageDailyPrincipalCost() != null ? row.heldAverageDailyPrincipalCost().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-principal-cost-day-sum="${row.principalCostDaySum() != null ? row.principalCostDaySum().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-held-day-count="${String.valueOf(row.heldDayCount())}"
										data-short-held="${String.valueOf(row.shortHeld())}"
										data-evaluation-profit="${holdingProfit != null && holdingProfit.evaluationProfit() != null ? holdingProfit.evaluationProfit().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-realized-profit="${holdingProfit != null && holdingProfit.realizedProfit() != null ? holdingProfit.realizedProfit().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-combined-profit="${holdingProfit != null ? holdingProfit.combinedProfit().toPlainString() : BigDecimal.ZERO.toPlainString()}"
										data-current-value="${row.groupId() != null && accountCurrentValues.get(row.groupId()) != null ? accountCurrentValues.get(row.groupId()).toPlainString() : BigDecimal.ZERO.toPlainString()}">
										<th scope="row" class="font-medium"><a href="${StockDetailLinkUtil.account(row.groupId() != null ? row.groupId().toString() : "")}" class="link link-hover">${row.label()}</a></th>
										<td class="text-right yield-extra-col amount-value">${decimalFormat.format(row.totalGrossAmount())}</td>
										<td class="text-right amount-value" data-current-value-cell>@if(row.groupId() != null && accountCurrentValues.get(row.groupId()) != null)${decimalFormat.format(accountCurrentValues.get(row.groupId()))}@else<span class="text-base-content/60" aria-hidden="true" title="${currentValueNoneTitle}">-</span><span class="sr-only">${currentValueNoneTitle}</span>@endif</td>
										<td class="text-right" data-evaluation-profit-cell>@if(holdingProfit != null && holdingProfit.held())<span class="amount-value font-semibold ${profitClass.apply(holdingProfit.evaluationProfit())}">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(holdingProfit.evaluationProfit()))}</span> <div class="text-[11px] whitespace-nowrap ${profitClass.apply(holdingProfit.evaluationProfitPct())}">(${signedPctText.apply(holdingProfit.evaluationProfitPct())})</div>@else<span class="text-base-content/60" aria-hidden="true" title="${evaluationProfitNoneTitle}">-</span><span class="sr-only">${evaluationProfitNoneTitle}</span>@endif</td>
										<td class="text-right" data-realized-profit-cell>@if(holdingProfit != null && holdingProfit.realizedProfit() != null)<span class="amount-value font-semibold ${profitClass.apply(holdingProfit.realizedProfit())}">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(holdingProfit.realizedProfit()))}</span>@else<span class="text-base-content/60" aria-hidden="true" title="${realizedProfitNoneTitle}">-</span><span class="sr-only">${realizedProfitNoneTitle}</span>@endif</td>
										<td class="text-right" data-annualized-cell data-amount-basis="${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}" title="${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}">@if(yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount()) != null)<span class="sr-only">${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}</span>@endif<div class="amount-value">${decimalFormat.format(row.totalNetAmount())}</div> <div class="text-[11px] whitespace-nowrap">@if(row.annualizedYieldPct() != null)<span class="font-semibold text-dividend" data-annualized-value>${java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.yield.ttm.value"), percentFormat.apply(row.annualizedYieldPct()))}</span>@else<span class="text-base-content/60" aria-hidden="true" title="${dailyBasisNoneTitle}">-</span><span class="sr-only">${dailyBasisNoneTitle}</span>@endif@if(row.heldDayCount() > 0)<span class="text-base-content/60" data-held-days> · ${heldDaysText.apply(row.heldDayCount())}</span>@if(row.shortHeld()) <span class="badge badge-outline badge-xs align-middle" data-short-held-badge data-stock-tag-tooltip="${shortHeldDesc.apply(row.heldDayCount())}">${shortHeldLabel}</span><span class="sr-only">${shortHeldDesc.apply(row.heldDayCount())}</span>@endif@endif</div>@if(yearlyMaxAnnualized.signum() > 0 && row.annualizedYieldPct() != null && row.groupId() == null)<div class="ml-auto mt-0.5 h-1 w-16 rounded-full bg-base-200" aria-hidden="true" data-yearly-bar><div class="h-1 rounded-full text-dividend yield-bar-fill" style="width: ${row.annualizedYieldPct().multiply(BigDecimal.valueOf(100)).divide(yearlyMaxAnnualized, 1, RoundingMode.HALF_UP).max(BigDecimal.ZERO).min(BigDecimal.valueOf(100)).toPlainString()}%"></div></div>@endif</td>
										<td class="text-right" data-combined-profit-cell>@if(holdingProfit != null)<span class="amount-value font-semibold ${profitClass.apply(holdingProfit.combinedProfit())}">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(holdingProfit.combinedProfit()))}</span>@if(holdingProfit.combinedProfitPct() != null)<div class="text-[11px] whitespace-nowrap ${profitClass.apply(holdingProfit.combinedProfitPct())}">${combinedRateText.apply(holdingProfit.combinedProfitPct())}</div>@endif@else<span class="text-base-content/60" aria-hidden="true" title="${evaluationProfitNoneTitle}">-</span>@endif</td>
										<td class="text-right amount-value yield-extra-col" data-held-average-cell title="${row.heldAverageDailyPrincipalCost() == null ? dailyBasisNoneTitle : null}">${row.heldAverageDailyPrincipalCost() != null ? decimalFormat.format(row.heldAverageDailyPrincipalCost()) : "-"}</td>
										<td class="text-right yield-extra-col amount-value" title="${rowTaxAmount != null && rowTaxAmount.signum() == 0 ? zeroAmountTitle : null}" aria-label="${rowTaxAmount != null && rowTaxAmount.signum() == 0 ? zeroAmountTitle : null}">${rowTaxAmount != null && rowTaxAmount.signum() != 0 ? decimalFormat.format(rowTaxAmount) : "-"}</td>
										<td class="text-right yield-extra-col amount-value" title="${row.totalTaxableAmount() != null && row.totalTaxableAmount().signum() == 0 ? zeroAmountTitle : null}" aria-label="${row.totalTaxableAmount() != null && row.totalTaxableAmount().signum() == 0 ? zeroAmountTitle : null}">${row.totalTaxableAmount() != null && row.totalTaxableAmount().signum() != 0 ? decimalFormat.format(row.totalTaxableAmount()) : "-"}</td>
										<td class="text-right yield-extra-col amount-value" title="${row.averageDailyPrincipalCost() == null ? dailyBasisNoneTitle : null}">${row.averageDailyPrincipalCost() != null ? decimalFormat.format(row.averageDailyPrincipalCost()) : "-"}@if(row.averageDailyPrincipalCost() == null)<span class="sr-only">${dailyBasisNoneTitle}</span>@endif</td>
										<td class="text-right text-dividend font-medium yield-extra-col" data-amount-basis="${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}" title="${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}">${row.averageDailyPrincipalCost() != null && row.averageDailyPrincipalCost().signum() > 0 ? percentFormat.apply(row.yieldOnDailyAverageCostPct()) : "-"}</td>
										<td class="text-right yield-extra-col amount-value" title="${basisNoneOrNull.apply(row.averagePrincipalCost())}">${row.averagePrincipalCost() != null ? decimalFormat.format(row.averagePrincipalCost()) : "-"}@if(basisNoneOrNull.apply(row.averagePrincipalCost()) != null)<span class="sr-only">${basisNoneTitle}</span>@endif</td>
										<td class="text-right yield-extra-col text-base-content/70" data-amount-basis="${basisNoneOrNull.apply(row.averagePrincipalCost()) != null ? basisNoneTitle : yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}" title="${basisNoneOrNull.apply(row.averagePrincipalCost()) != null ? basisNoneTitle : yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}">${percentFormat.apply(row.yieldOnCostPct())}</td>
										<td class="text-right yield-extra-col text-base-content/70" data-amount-basis="${marketBasisTitle.apply(row.netAmountWithPrincipalMarket(), row.averagePrincipalMarketValue())}" title="${marketBasisTitle.apply(row.netAmountWithPrincipalMarket(), row.averagePrincipalMarketValue())}">${percentFormat.apply(row.yieldOnMarketPct())}@if(marketBasisTitle.apply(row.netAmountWithPrincipalMarket(), row.averagePrincipalMarketValue()) != null)<span class="sr-only">${marketBasisTitle.apply(row.netAmountWithPrincipalMarket(), row.averagePrincipalMarketValue())}</span>@endif</td>
										<td class="text-right whitespace-nowrap">${countFormat.apply(row.dividendCount())}</td>
									</tr>
								@endfor
							</tbody>
							<tfoot>
								<tr class="bg-base-200 font-bold">
									<th scope="row">${totalLabel}</th>
									<td class="text-right yield-extra-col font-mono amount-value">${decimalFormat.format(accountYieldFooterGrossAmount)}</td>
										!{var holdingProfitTotal = DividendHoldingProfitUtil.total(accountYieldRows, accountCurrentValues, accountEvaluationProfits, accountRealizedProfits, accountTotalBuyCosts);}
										<td class="text-right font-mono amount-value" data-current-value-total>${decimalFormat.format(accountCurrentValueTotal)}</td>
										<td class="text-right" data-evaluation-profit-total>@if(holdingProfitTotal != null && holdingProfitTotal.held())<span class="amount-value font-semibold ${profitClass.apply(holdingProfitTotal.evaluationProfit())}">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(holdingProfitTotal.evaluationProfit()))}</span> <div class="text-[11px] whitespace-nowrap ${profitClass.apply(holdingProfitTotal.evaluationProfitPct())}">(${signedPctText.apply(holdingProfitTotal.evaluationProfitPct())})</div>@else<span class="text-base-content/60" aria-hidden="true" title="${evaluationProfitNoneTitle}">-</span><span class="sr-only">${evaluationProfitNoneTitle}</span>@endif</td>
										<td class="text-right" data-realized-profit-total>@if(holdingProfitTotal != null && holdingProfitTotal.realizedProfit() != null)<span class="amount-value font-semibold ${profitClass.apply(holdingProfitTotal.realizedProfit())}">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(holdingProfitTotal.realizedProfit()))}</span>@else<span class="text-base-content/60" aria-hidden="true" title="${realizedProfitNoneTitle}">-</span><span class="sr-only">${realizedProfitNoneTitle}</span>@endif</td>
										<td class="text-right text-dividend" data-annualized-total data-amount-basis="${yieldBasisTitle.apply(accountYieldFooterNetWithPrincipalCost, accountYieldFooterNetAmount)}" title="${yieldBasisTitle.apply(accountYieldFooterNetWithPrincipalCost, accountYieldFooterNetAmount)}">@if(yieldBasisTitle.apply(accountYieldFooterNetWithPrincipalCost, accountYieldFooterNetAmount) != null)<span class="sr-only">${yieldBasisTitle.apply(accountYieldFooterNetWithPrincipalCost, accountYieldFooterNetAmount)}</span>@endif<div class="font-mono amount-value">${decimalFormat.format(accountYieldFooterNetAmount)}</div> <div class="text-[11px] font-normal whitespace-nowrap">${footerAnnualizedPct.apply(accountYieldRows) != null ? java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.yield.ttm.value"), percentFormat.apply(footerAnnualizedPct.apply(accountYieldRows))) : "-"}@if(portfolioYield != null && portfolioYield.heldDayCount() > 0)<span class="text-base-content/60"> · ${heldDaysText.apply(portfolioYield.heldDayCount())}</span>@endif</div></td>
										<td class="text-right" data-combined-profit-total>@if(holdingProfitTotal != null)<span class="amount-value font-semibold ${profitClass.apply(holdingProfitTotal.combinedProfit())}">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(holdingProfitTotal.combinedProfit()))}</span>@if(holdingProfitTotal.combinedProfitPct() != null)<div class="text-[11px] whitespace-nowrap ${profitClass.apply(holdingProfitTotal.combinedProfitPct())}">${combinedRateText.apply(holdingProfitTotal.combinedProfitPct())}</div>@endif@else<span class="text-base-content/60" aria-hidden="true" title="${evaluationProfitNoneTitle}">-</span>@endif</td>
										<td class="text-right font-mono amount-value yield-extra-col" data-held-average-total>${footerHeldAverage != null ? decimalFormat.format(footerHeldAverage) : "-"}</td>
										<td class="text-right yield-extra-col font-mono amount-value">${decimalFormat.format(accountYieldFooterTaxAmount)}</td>
									<td class="text-right yield-extra-col font-mono amount-value">${decimalFormat.format(accountYieldFooterTaxableAmount)}</td>
									<td class="text-right yield-extra-col font-mono amount-value">${decimalFormat.format(accountYieldFooterDailyPrincipalCost)}</td>
									<td class="text-right text-dividend font-medium yield-extra-col" data-amount-basis="${yieldBasisTitle.apply(accountYieldFooterNetWithPrincipalCost, accountYieldFooterNetAmount)}" title="${yieldBasisTitle.apply(accountYieldFooterNetWithPrincipalCost, accountYieldFooterNetAmount)}">${percentFormat.apply(accountYieldFooterYieldOnDailyAverageCostPct)}</td>
									<td class="text-right yield-extra-col font-mono amount-value">${decimalFormat.format(accountYieldFooterPrincipalCost)}</td>
									<td class="text-right yield-extra-col text-base-content/70" data-amount-basis="${yieldBasisTitle.apply(accountYieldFooterNetWithPrincipalCost, accountYieldFooterNetAmount)}" title="${yieldBasisTitle.apply(accountYieldFooterNetWithPrincipalCost, accountYieldFooterNetAmount)}">${percentFormat.apply(accountYieldFooterYieldOnCostPct)}</td>
									<td class="text-right yield-extra-col text-base-content/70" data-amount-basis="${marketBasisTitle.apply(accountYieldFooterNetWithPrincipalMarket, accountYieldFooterMarketValue)}" title="${marketBasisTitle.apply(accountYieldFooterNetWithPrincipalMarket, accountYieldFooterMarketValue)}">${percentFormat.apply(accountYieldFooterYieldOnMarketPct)}@if(marketBasisTitle.apply(accountYieldFooterNetWithPrincipalMarket, accountYieldFooterMarketValue) != null)<span class="sr-only">${marketBasisTitle.apply(accountYieldFooterNetWithPrincipalMarket, accountYieldFooterMarketValue)}</span>@endif</td>
									<td class="text-right whitespace-nowrap">${countFormat.apply(accountYieldFooterDividendCount)}</td>
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

		/* 행의 첫 칸은 th(scope=row)다 - td 만 칠하면 체크박스 · 이름 칸만 선택 배경이 빠지고 왼쪽 선택 막대(td:first-child)는 아예 안 그려진다(사용자 제보 2026-09-28). */
		.dividend-yield-stock-row.dividend-yield-stock-row-selected > :is(th, td) {
			background-color: color-mix(in oklch, var(--color-info) 10%, var(--color-base-100));
		}

		.dividend-yield-stock-row.dividend-yield-stock-row-selected > :first-child {
			box-shadow: inset 3px 0 0 color-mix(in oklch, var(--color-info) 65%, transparent);
		}

		.dividend-yield-account-row.dividend-yield-account-row-selected > :is(th, td) {
			background-color: color-mix(in oklch, var(--color-secondary) 10%, var(--color-base-100));
		}

		.dividend-yield-account-row.dividend-yield-account-row-selected > :first-child {
			box-shadow: inset 3px 0 0 color-mix(in oklch, var(--color-secondary) 65%, transparent);
		}

		.dividend-yield-stock-row:focus-visible,
		.dividend-yield-account-row:focus-visible {
			outline: 2px solid color-mix(in oklch, var(--color-info) 35%, transparent);
			outline-offset: -2px;
		}
	</style>