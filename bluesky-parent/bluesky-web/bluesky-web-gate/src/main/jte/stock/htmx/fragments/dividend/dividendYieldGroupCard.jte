@import java.util.List
@import java.util.UUID
@import java.math.BigDecimal
@import java.math.RoundingMode
@import net.luversof.web.gate.stock.dto.view.DividendYieldGroupView
@import net.luversof.web.gate.stock.util.DividendYieldViewUtil
@import net.luversof.web.gate.stock.util.StockAmountUtil
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.stock.util.StockDetailLinkUtil
@import net.luversof.web.gate.stock.util.StockFormatUtil
@import net.luversof.web.gate.stock.util.DividendHoldingProfitUtil
@import net.luversof.web.gate.stock.support.StockViewSupport

<%-- 배당 수익 분석의 종목별 · 계좌별 카드 하나(선택 합산 상자 · 정렬 표 · 합계 줄).
	 2026-10-22 까지 dividendYieldAnalytics.jte 가 종목 카드와 계좌 카드를 290 줄씩 한 벌씩 들고 있었다. 다른 것은 아래 인자뿐이다 -
	 이름 · 머리 문구 · 색(info / secondary) · 행 목록과 맵 · 상세 링크(종목 / 계좌) · 태그 풍선(종목만).
	 문구와 셈 도우미는 부모와 같은 정의를 여기에도 둔다(부모의 연도별 표가 함께 쓴다). 합계 줄 값은 이 카드의 행으로 셈한다.
	 색 클래스는 Tailwind 가 소스 글자로 모으므로 부르는 쪽이 통째로 넘긴다(border-${tone} 처럼 만들면 CSS 에서 빠진다). --%>
<%-- "stock" | "account": 행 클래스(dividend-yield-stock-row …)와 상세 링크를 고른다. --%>
@param String kind
@param String nameLabel
@param String sectionLabel
@param String selectionHelperLabel
@param String selectionSummaryLabel
@param String selectionCountLabel
@param String summaryToneClass
@param String badgeToneClass
@param List<DividendYieldGroupView> groupRows
@param java.util.Map<UUID, BigDecimal> currentValues
@param java.util.Map<UUID, BigDecimal> evaluationProfits
@param java.util.Map<UUID, BigDecimal> realizedProfits
@param java.util.Map<UUID, BigDecimal> totalBuyCosts
@param BigDecimal currentValueTotal
<%-- 종목 카드만 - 계좌 카드는 null(속성이 빠진다). --%>
@param java.util.function.Function<String, String> tagTooltipByName = null
@param java.text.DecimalFormat decimalFormat
@param java.util.function.Function<BigDecimal, String> percentFormat
@param java.util.function.Function<Long, String> countFormat
@param BigDecimal safeAllNet
@param DividendYieldGroupView portfolioYield
@param String grossAmountLabel
@param String netAmountLabel
@param String taxLabel
@param String taxableAmountLabel
@param String totalLabel
<%-- 연도 막대(연도별 표에서 옮겨 온 줄 - groupId 가 없는 행에만 그린다). 부모와 같은 값을 받는다. --%>
@param BigDecimal yearlyMaxAnnualized

!{
	String zeroAmountTitle = MessageUtil.getMessage("stock.dividend.zero.amount.title");
	String dailyAverageInvestedCapitalLabel = MessageUtil.getMessage("stock.dividend.yield.column.daily.average.invested.capital");
	String basisAverageInvestedCapitalLabel = MessageUtil.getMessage("stock.dividend.yield.column.basis.average.invested.capital");
	String yieldOnDailyAverageCostLabel = MessageUtil.getMessage("stock.dividend.yield.column.yield.on.daily.average.cost");
	String yieldOnBasisAverageCostLabel = MessageUtil.getMessage("stock.dividend.yield.column.yield.on.basis.average.cost");
	String yieldOnBasisMarketLabel = MessageUtil.getMessage("stock.dividend.yield.column.yield.on.basis.market");
	String dividendCountLabel = MessageUtil.getMessage("stock.dividend.yield.column.dividend.count");
	String noYieldAnalyticsLabel = MessageUtil.getMessage("stock.dividend.yield.message.no.analytics");
	String dailyCapitalTermDesc = MessageUtil.getMessage("stock.dividend.yield.term.daily.average.invested.capital.desc");
	String basisCapitalTermDesc = MessageUtil.getMessage("stock.dividend.yield.term.basis.average.invested.capital.desc");
	String basisMarketTermDesc = MessageUtil.getMessage("stock.dividend.yield.term.basis.market.desc");
	String heldAverageLabel = MessageUtil.getMessage("stock.dividend.yield.column.held.average.principal");
	String currentValueLabel = MessageUtil.getMessage("stock.dividend.yield.column.current.value");
	String currentValueNoneTitle = MessageUtil.getMessage("stock.dividend.yield.current.value.none");
	String evaluationProfitLabel = MessageUtil.getMessage("stock.dividend.yield.column.evaluation.profit");
	String combinedProfitLabel = MessageUtil.getMessage("stock.dividend.yield.column.combined.profit");
	String evaluationProfitTermDesc = MessageUtil.getMessage("stock.dividend.yield.term.evaluation.profit.desc");
	String combinedProfitTermDesc = MessageUtil.getMessage("stock.dividend.yield.term.combined.profit.desc");
	String realizedProfitLabel = MessageUtil.getMessage("stock.dividend.yield.column.realized.profit");
	String realizedProfitTermDesc = MessageUtil.getMessage("stock.dividend.yield.term.realized.profit.desc");
	String realizedProfitNoneTitle = MessageUtil.getMessage("stock.profit.realized.none");
	String evaluationProfitNoneTitle = MessageUtil.getMessage("stock.dividend.yield.evaluation.profit.none");
	// 손익 색은 빨강 = 이익 · 파랑 = 손해 · 0 은 중립(게이트 관례).
	java.util.function.Function<BigDecimal, String> profitClass = value -> value == null || value.signum() == 0 ? "text-base-content/70" : (value.signum() > 0 ? "text-profit" : "text-loss");
	java.util.function.Function<BigDecimal, String> signedPctText = value -> value == null ? "-" : StockFormatUtil.signedPct(value.doubleValue(), 2);
	java.util.function.Function<BigDecimal, String> combinedRateText = value -> StockViewSupport.msg("stock.dividend.yield.combined.rate", value == null ? "-" : StockFormatUtil.signedPct(value.doubleValue(), 2));
	String annualizedYieldLabel = MessageUtil.getMessage("stock.dividend.yield.column.annualized");
	String heldTermDesc = MessageUtil.getMessage("stock.dividend.yield.term.held.average.principal.desc");
	String annualizedTermDesc = MessageUtil.getMessage("stock.dividend.yield.term.annualized.desc");
	String extraColumnsToggleLabel = MessageUtil.getMessage("stock.dividend.yield.extra.toggle");
	String shortHeldLabel = MessageUtil.getMessage("stock.dividend.yield.cell.short.held");
	// 합계 셈 · 칸 근거 문구(분자에서 빠진 배당 · 시가 분모 · 기준일 원금 없음 · 보유 일수)는 DividendYieldViewUtil 한 곳에 있다 - 부모의 연도별 표와
	// 종목/계좌 카드가 같은 식을 한 벌씩 들고 있던 것을 모았다(2026-10-22). 까닭과 실측은 그 클래스 설명에.
	java.util.function.Function<Long, String> heldDaysText = DividendYieldViewUtil::heldDaysText;
	java.util.function.Function<Long, String> shortHeldDesc = DividendYieldViewUtil::shortHeldDesc;
	String dividendYieldSelectionClearLabel = MessageUtil.getMessage("stock.analytics.selection.clear");
	String dividendYieldSelectionWeightLabel = MessageUtil.getMessage("stock.analytics.selection.weight");
	java.util.function.BiFunction<List<DividendYieldGroupView>, java.util.function.Function<DividendYieldGroupView, BigDecimal>, BigDecimal> sumDividendYieldValue = StockAmountUtil::sum;
	java.util.function.Function<List<DividendYieldGroupView>, Long> sumDividendYieldCount = DividendYieldViewUtil::dividendCount;
	java.util.function.BiFunction<BigDecimal, BigDecimal, BigDecimal> computeYieldPct = DividendYieldViewUtil::yieldPct;
	BigDecimal footerGrossAmount = sumDividendYieldValue.apply(groupRows, DividendYieldGroupView::totalGrossAmount);
	BigDecimal footerNetAmount = sumDividendYieldValue.apply(groupRows, DividendYieldGroupView::totalNetAmount);
	BigDecimal footerTaxAmount = footerGrossAmount.subtract(footerNetAmount);
	BigDecimal footerTaxableAmount = sumDividendYieldValue.apply(groupRows, DividendYieldGroupView::totalTaxableAmount);
	BigDecimal footerDailyPrincipalCost = sumDividendYieldValue.apply(groupRows, DividendYieldGroupView::averageDailyPrincipalCost);
	BigDecimal footerPrincipalCost = sumDividendYieldValue.apply(groupRows, DividendYieldGroupView::averagePrincipalCost);
	BigDecimal footerMarketValue = sumDividendYieldValue.apply(groupRows, DividendYieldGroupView::averagePrincipalMarketValue);
	BigDecimal footerNetWithPrincipalCost = sumDividendYieldValue.apply(groupRows, DividendYieldGroupView::netAmountWithPrincipalCost);
	BigDecimal footerYieldOnDailyAverageCostPct = computeYieldPct.apply(footerNetWithPrincipalCost, footerDailyPrincipalCost);
	BigDecimal footerNetWithPrincipalMarket = sumDividendYieldValue.apply(groupRows, DividendYieldGroupView::netAmountWithPrincipalMarket);
	BigDecimal footerYieldOnCostPct = computeYieldPct.apply(footerNetWithPrincipalCost, footerPrincipalCost);
	BigDecimal footerYieldOnMarketPct = computeYieldPct.apply(footerNetWithPrincipalMarket, footerMarketValue);
	long footerDividendCount = sumDividendYieldCount.apply(groupRows);
	java.util.function.Function<List<DividendYieldGroupView>, BigDecimal> footerAnnualizedPct = DividendYieldViewUtil::footerAnnualizedPct;
	BigDecimal footerHeldAverage = portfolioYield != null ? portfolioYield.heldAverageDailyPrincipalCost() : null;
	java.util.function.BiFunction<BigDecimal, BigDecimal, String> yieldBasisTitle = (usedNet, totalNet) -> DividendYieldViewUtil.basisTitle(usedNet, totalNet, decimalFormat);
	java.util.function.BiFunction<BigDecimal, BigDecimal, String> marketBasisTitle = (usedNet, base) -> DividendYieldViewUtil.marketBasisTitle(usedNet, base, decimalFormat);
	String basisNoneTitle = MessageUtil.getMessage("stock.dividend.yield.basis.none");
	String dailyBasisNoneTitle = MessageUtil.getMessage("stock.dividend.yield.daily.basis.none");
	java.util.function.Function<BigDecimal, String> basisNoneOrNull = DividendYieldViewUtil::basisNoneOrNull;
}
		<div class="card min-w-0 bg-base-100 shadow-sm border border-base-200">
			<div class="card-body p-4" data-dividend-yield-selection-section data-yield-extra-scope>
				<div class="mb-3 flex flex-wrap items-center justify-between gap-2">
					<h2 class="text-sm font-bold" data-page-section>${sectionLabel}</h2>
					<label class="label cursor-pointer gap-2 text-xs text-base-content/70 w-fit" data-yield-extra-toggle-label><input type="checkbox" class="toggle toggle-xs" data-yield-extra-toggle>${extraColumnsToggleLabel}</label>
				</div>
				@if(groupRows == null || groupRows.isEmpty())
					<div class="text-sm text-base-content/60">${noYieldAnalyticsLabel}</div>
				@else
					<div class="mb-3 flex flex-col gap-3">
						<p class="text-xs text-base-content/60">${selectionHelperLabel}</p>
						<div class="selection-reveal"><div>
						<div class="rounded-2xl border ${summaryToneClass} px-4 py-3"
							 data-dividend-yield-selection-summary
							 data-count-template="${selectionCountLabel}"
							 data-total-net-amount="${safeAllNet.toPlainString()}"
							 data-weight-label="${dividendYieldSelectionWeightLabel}"
							 data-basis-label="${yieldOnBasisAverageCostLabel}"
							 aria-live="polite">
							<div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
								<div class="flex flex-wrap items-center gap-2">
									<h3 class="text-sm font-semibold">${selectionSummaryLabel}</h3>
									<span class="badge ${badgeToneClass} badge-outline" data-dividend-yield-selection-count>${selectionSummaryLabel}</span>
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
						<table class="table table-xs w-full" data-dividend-yield-selection-table data-dividend-yield-sortable-table aria-label="${sectionLabel}">
							<thead>
								<tr class="text-base-content/60">
									@template.stock.htmx.fragments.components.sortButtonHeader(sortKey = "label", sortType = "text", label = nameLabel, buttonClass = "btn btn-ghost btn-xs h-auto min-h-6 w-full justify-start px-1 text-xs text-base-content/60 hover:text-base-content")
									@template.stock.htmx.fragments.components.sortButtonHeader(sortKey = "grossAmount", sortType = "number", label = grossAmountLabel, cellClass = "text-right yield-extra-col", buttonClass = "btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content")
								@template.stock.htmx.fragments.components.sortButtonHeader(sortKey = "currentValue", sortType = "number", label = currentValueLabel, cellClass = "text-right", buttonClass = "btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content")
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
@template.stock.htmx.fragments.components.sortButtonHeader(sortKey = "taxAmount", sortType = "number", label = taxLabel, cellClass = "text-right yield-extra-col", buttonClass = "btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content")
									@template.stock.htmx.fragments.components.sortButtonHeader(sortKey = "taxableAmount", sortType = "number", label = taxableAmountLabel, cellClass = "text-right yield-extra-col", buttonClass = "btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content")
									<th scope="col" class="text-right yield-extra-col">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="averageDailyPrincipalCost" aria-describedby="dividend-yield-term-daily"
												data-sort-type="number">
											@template._components.ui.termHint(label = dailyAverageInvestedCapitalLabel, desc = dailyCapitalTermDesc)
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
								@template.stock.htmx.fragments.components.sortButtonHeader(sortKey = "yieldOnDailyAverageCost", sortType = "number", label = yieldOnDailyAverageCostLabel, cellClass = "text-right yield-extra-col", buttonClass = "btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content")
									<th scope="col" class="text-right yield-extra-col">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="averagePrincipalCost" aria-describedby="dividend-yield-term-basis"
												data-sort-type="number">
											@template._components.ui.termHint(label = basisAverageInvestedCapitalLabel, desc = basisCapitalTermDesc)
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									@template.stock.htmx.fragments.components.sortButtonHeader(sortKey = "yieldOnCost", sortType = "number", label = yieldOnBasisAverageCostLabel, cellClass = "text-right yield-extra-col", buttonClass = "btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content")
									<th scope="col" class="text-right yield-extra-col">
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content"
												data-sort-key="yieldOnMarket" aria-describedby="dividend-yield-term-market"
												data-sort-type="number">
											@template._components.ui.termHint(label = yieldOnBasisMarketLabel, desc = basisMarketTermDesc)
											<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
										</button>
									</th>
									@template.stock.htmx.fragments.components.sortButtonHeader(sortKey = "dividendCount", sortType = "number", label = dividendCountLabel, cellClass = "text-right", buttonClass = "btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content")
								</tr>
							</thead>
							<tbody>
								@for(DividendYieldGroupView row : groupRows)
									!{var holdingProfit = DividendHoldingProfitUtil.ofRow(row, currentValues, evaluationProfits, realizedProfits, totalBuyCosts);}
									!{BigDecimal rowTaxAmount = row.totalGrossAmount() != null && row.totalNetAmount() != null ? row.totalGrossAmount().subtract(row.totalNetAmount()) : null;}
									!{String tagTooltip = tagTooltipByName != null ? tagTooltipByName.apply(row.label()) : null;}
									<tr class="dividend-yield-${kind}-row"
										data-dividend-yield-row
										data-selection-class="dividend-yield-${kind}-row-selected"
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
										data-current-value="${row.groupId() != null && currentValues.get(row.groupId()) != null ? currentValues.get(row.groupId()).toPlainString() : BigDecimal.ZERO.toPlainString()}">
										<th scope="row" class="font-medium"><a href="${"account".equals(kind) ? StockDetailLinkUtil.account(row.groupId() != null ? row.groupId().toString() : "") : StockDetailLinkUtil.item(row.groupId() != null ? row.groupId().toString() : "")}" class="link link-hover" data-stock-tag-tooltip="${tagTooltip}">${row.label()}</a></th>
										<td class="text-right yield-extra-col amount-value">${decimalFormat.format(row.totalGrossAmount())}</td>
										<td class="text-right amount-value" data-current-value-cell>@if(row.groupId() != null && currentValues.get(row.groupId()) != null)${decimalFormat.format(currentValues.get(row.groupId()))}@else<span class="text-base-content/60" aria-hidden="true" title="${currentValueNoneTitle}">-</span><span class="sr-only">${currentValueNoneTitle}</span>@endif</td>
										<td class="text-right" data-evaluation-profit-cell>@if(holdingProfit != null && holdingProfit.held())<span class="amount-value font-semibold ${profitClass.apply(holdingProfit.evaluationProfit())}">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(holdingProfit.evaluationProfit()))}</span> <div class="text-[11px] whitespace-nowrap ${profitClass.apply(holdingProfit.evaluationProfitPct())}">(${signedPctText.apply(holdingProfit.evaluationProfitPct())})</div>@else<span class="text-base-content/60" aria-hidden="true" title="${evaluationProfitNoneTitle}">-</span><span class="sr-only">${evaluationProfitNoneTitle}</span>@endif</td>
										<td class="text-right" data-realized-profit-cell>@if(holdingProfit != null && holdingProfit.realizedProfit() != null)<span class="amount-value font-semibold ${profitClass.apply(holdingProfit.realizedProfit())}">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(holdingProfit.realizedProfit()))}</span>@else<span class="text-base-content/60" aria-hidden="true" title="${realizedProfitNoneTitle}">-</span><span class="sr-only">${realizedProfitNoneTitle}</span>@endif</td>
										<td class="text-right" data-annualized-cell data-amount-basis="${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}" title="${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}">@if(yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount()) != null)<span class="sr-only">${yieldBasisTitle.apply(row.netAmountWithPrincipalCost(), row.totalNetAmount())}</span>@endif<div class="amount-value">${decimalFormat.format(row.totalNetAmount())}</div> <div class="text-[11px] whitespace-nowrap">@if(row.annualizedYieldPct() != null)<span class="font-semibold text-dividend" data-annualized-value>${StockViewSupport.msg("stock.dividend.yield.ttm.value", percentFormat.apply(row.annualizedYieldPct()))}</span>@else<span class="text-base-content/60" aria-hidden="true" title="${dailyBasisNoneTitle}">-</span><span class="sr-only">${dailyBasisNoneTitle}</span>@endif@if(row.heldDayCount() > 0)<span class="text-base-content/60" data-held-days> · ${heldDaysText.apply(row.heldDayCount())}</span>@if(row.shortHeld()) <span class="badge badge-outline badge-xs align-middle" data-short-held-badge data-stock-tag-tooltip="${shortHeldDesc.apply(row.heldDayCount())}">${shortHeldLabel}</span><span class="sr-only">${shortHeldDesc.apply(row.heldDayCount())}</span>@endif@endif</div>@if(yearlyMaxAnnualized.signum() > 0 && row.annualizedYieldPct() != null && row.groupId() == null)<div class="ml-auto mt-0.5 h-1 w-16 rounded-full bg-base-200" aria-hidden="true" data-yearly-bar><div class="h-1 rounded-full text-dividend yield-bar-fill" style="width: ${row.annualizedYieldPct().multiply(BigDecimal.valueOf(100)).divide(yearlyMaxAnnualized, 1, RoundingMode.HALF_UP).max(BigDecimal.ZERO).min(BigDecimal.valueOf(100)).toPlainString()}%"></div></div>@endif</td>
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
									<td class="text-right yield-extra-col font-mono amount-value">${decimalFormat.format(footerGrossAmount)}</td>
										!{var holdingProfitTotal = DividendHoldingProfitUtil.total(groupRows, currentValues, evaluationProfits, realizedProfits, totalBuyCosts);}
										<td class="text-right font-mono amount-value" data-current-value-total>${decimalFormat.format(currentValueTotal)}</td>
										<td class="text-right" data-evaluation-profit-total>@if(holdingProfitTotal != null && holdingProfitTotal.held())<span class="amount-value font-semibold ${profitClass.apply(holdingProfitTotal.evaluationProfit())}">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(holdingProfitTotal.evaluationProfit()))}</span> <div class="text-[11px] whitespace-nowrap ${profitClass.apply(holdingProfitTotal.evaluationProfitPct())}">(${signedPctText.apply(holdingProfitTotal.evaluationProfitPct())})</div>@else<span class="text-base-content/60" aria-hidden="true" title="${evaluationProfitNoneTitle}">-</span><span class="sr-only">${evaluationProfitNoneTitle}</span>@endif</td>
										<td class="text-right" data-realized-profit-total>@if(holdingProfitTotal != null && holdingProfitTotal.realizedProfit() != null)<span class="amount-value font-semibold ${profitClass.apply(holdingProfitTotal.realizedProfit())}">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(holdingProfitTotal.realizedProfit()))}</span>@else<span class="text-base-content/60" aria-hidden="true" title="${realizedProfitNoneTitle}">-</span><span class="sr-only">${realizedProfitNoneTitle}</span>@endif</td>
										<td class="text-right text-dividend" data-annualized-total data-amount-basis="${yieldBasisTitle.apply(footerNetWithPrincipalCost, footerNetAmount)}" title="${yieldBasisTitle.apply(footerNetWithPrincipalCost, footerNetAmount)}">@if(yieldBasisTitle.apply(footerNetWithPrincipalCost, footerNetAmount) != null)<span class="sr-only">${yieldBasisTitle.apply(footerNetWithPrincipalCost, footerNetAmount)}</span>@endif<div class="font-mono amount-value">${decimalFormat.format(footerNetAmount)}</div> <div class="text-[11px] font-normal whitespace-nowrap">${footerAnnualizedPct.apply(groupRows) != null ? StockViewSupport.msg("stock.dividend.yield.ttm.value", percentFormat.apply(footerAnnualizedPct.apply(groupRows))) : "-"}@if(portfolioYield != null && portfolioYield.heldDayCount() > 0)<span class="text-base-content/60"> · ${heldDaysText.apply(portfolioYield.heldDayCount())}</span>@endif</div></td>
										<td class="text-right" data-combined-profit-total>@if(holdingProfitTotal != null)<span class="amount-value font-semibold ${profitClass.apply(holdingProfitTotal.combinedProfit())}">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(holdingProfitTotal.combinedProfit()))}</span>@if(holdingProfitTotal.combinedProfitPct() != null)<div class="text-[11px] whitespace-nowrap ${profitClass.apply(holdingProfitTotal.combinedProfitPct())}">${combinedRateText.apply(holdingProfitTotal.combinedProfitPct())}</div>@endif@else<span class="text-base-content/60" aria-hidden="true" title="${evaluationProfitNoneTitle}">-</span>@endif</td>
										<td class="text-right font-mono amount-value yield-extra-col" data-held-average-total>${footerHeldAverage != null ? decimalFormat.format(footerHeldAverage) : "-"}</td>
										<td class="text-right yield-extra-col font-mono amount-value">${decimalFormat.format(footerTaxAmount)}</td>
									<td class="text-right yield-extra-col font-mono amount-value">${decimalFormat.format(footerTaxableAmount)}</td>
									<td class="text-right yield-extra-col font-mono amount-value">${decimalFormat.format(footerDailyPrincipalCost)}</td>
									<td class="text-right text-dividend font-medium yield-extra-col" data-amount-basis="${yieldBasisTitle.apply(footerNetWithPrincipalCost, footerNetAmount)}" title="${yieldBasisTitle.apply(footerNetWithPrincipalCost, footerNetAmount)}">${percentFormat.apply(footerYieldOnDailyAverageCostPct)}</td>
									<td class="text-right yield-extra-col font-mono amount-value">${decimalFormat.format(footerPrincipalCost)}</td>
									<td class="text-right yield-extra-col text-base-content/70" data-amount-basis="${yieldBasisTitle.apply(footerNetWithPrincipalCost, footerNetAmount)}" title="${yieldBasisTitle.apply(footerNetWithPrincipalCost, footerNetAmount)}">${percentFormat.apply(footerYieldOnCostPct)}</td>
									<td class="text-right yield-extra-col text-base-content/70" data-amount-basis="${marketBasisTitle.apply(footerNetWithPrincipalMarket, footerMarketValue)}" title="${marketBasisTitle.apply(footerNetWithPrincipalMarket, footerMarketValue)}">${percentFormat.apply(footerYieldOnMarketPct)}@if(marketBasisTitle.apply(footerNetWithPrincipalMarket, footerMarketValue) != null)<span class="sr-only">${marketBasisTitle.apply(footerNetWithPrincipalMarket, footerMarketValue)}</span>@endif</td>
									<td class="text-right whitespace-nowrap">${countFormat.apply(footerDividendCount)}</td>
								</tr>
							</tfoot>
						</table>
					</div>
				@endif
			</div>
		</div>
