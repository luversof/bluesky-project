@import net.luversof.web.gate.util.CspNonceHolder
@import java.math.BigDecimal
@import java.util.List
@import java.util.Map
@import net.luversof.web.gate.stock.domain.StockItem
@import net.luversof.web.gate.stock.domain.TradeProfit
@import net.luversof.web.gate.stock.dto.response.AssetStatusAccountHoldingView
@import net.luversof.web.gate.stock.dto.response.AssetStatusStockAccountView
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.stock.util.StockFormatUtil
@import net.luversof.web.gate.stock.util.StockAssetStatusUtil
@import net.luversof.web.gate.stock.util.StockHoldingReturnUtil
@import net.luversof.web.gate.stock.util.StockDetailLinkUtil

@param Map<java.util.UUID, TradeProfit> accountTotalMap
@param Map<java.util.UUID, BigDecimal> accountProfitBasisMap
@param java.util.Set<java.util.UUID> manualPrincipalAccountIds = java.util.Set.of()
@param Map<java.util.UUID, List<AssetStatusAccountHoldingView>> accountHoldingMap
<%-- 종목마다 그 종목을 가진 계좌 - 종목 줄의 "보유 계좌 보기" 가 펼친다(사용자 요청 2026-09-17). --%>
@param Map<java.util.UUID, List<AssetStatusStockAccountView>> stockHoldingAccountMap = java.util.Map.of()
@param List<StockItem> stockItemList = java.util.List.of()
@param List<TradeProfit> stockAggregated
@param Map<java.util.UUID, net.luversof.web.gate.stock.util.StockCombinedProfitUtil.Breakdown> stockProfitBreakdown = java.util.Map.of()
@param net.luversof.web.gate.stock.util.StockCombinedProfitUtil.Breakdown stockProfitBreakdownTotal = new net.luversof.web.gate.stock.util.StockCombinedProfitUtil.Breakdown(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
@param int soldOutStockCount = 0
@param BigDecimal soldOutRealizedProfit = java.math.BigDecimal.ZERO
@param BigDecimal soldOutDividend = java.math.BigDecimal.ZERO
@param BigDecimal totalEvaluationAmount
@param BigDecimal totalEvaluationProfit
@param java.time.LocalDate priceBasisDate = null
<%-- 종목별 최초 매수일과 '오늘'(요청 존). 보유 기간·연평균 수익률이 쓴다. --%>
@param Map<java.util.UUID, java.time.LocalDate> firstBuyDateByStockItem = java.util.Map.of()
@param java.time.LocalDate holdingBasisDate = null
<%-- 종목별 하루치 순현금흐름 - 연평균 수익률(XIRR)의 재료(사용자 결정 2026-09-17). 없으면 연평균을 비운다. --%>
@param Map<java.util.UUID, List<net.luversof.web.gate.stock.dto.response.StockCashFlowResponse>> cashFlowsByStockItem = java.util.Map.of()

!{
	String accountSectionTitle = MessageUtil.getMessage("stock.analytics.section.account.status");
	String stockSectionTitle = MessageUtil.getMessage("stock.analytics.section.stock.status");
	String allAccountsLabel = MessageUtil.getMessage("stock.analytics.section.all.accounts");
	String accountLabel = MessageUtil.getMessage("common.label.account");
	String buyAmountLabel = MessageUtil.getMessage("stock.trade.label.buy.amount");
	String stockNameLabel = MessageUtil.getMessage("stock.activity.label.stock.name");
	String quantityLabel = MessageUtil.getMessage("common.label.quantity");
	String averageCostLabel = MessageUtil.getMessage("stock.column.average.cost");
	String currentPriceLabel = MessageUtil.getMessage("stock.column.current.price");
	String weightLabel = MessageUtil.getMessage("stock.analytics.account.holdings.weight");
	String accountWeightLabel = MessageUtil.getMessage("stock.analytics.account.holdings.weight.account");
	String totalWeightLabel = MessageUtil.getMessage("stock.analytics.account.holdings.weight.total");
	String evaluationAmountLabel = MessageUtil.getMessage("stock.profit.evaluation.amount");
	String principalLabel = MessageUtil.getMessage("stock.label.principal");
	String evaluationProfitLabel = MessageUtil.getMessage("stock.profit.unrealized");
	String principalReturnLabel = MessageUtil.getMessage("stock.analytics.label.principal.return");
	String totalLabel = MessageUtil.getMessage("common.label.total");
	String noHoldingsLabel = MessageUtil.getMessage("stock.message.no.holdings");
	// 종목 상세와 같은 문구를 쓴다 - 같은 값을 다른 이름으로 부르면 견줄 수 없다.
	String realizedProfitLabel = MessageUtil.getMessage("stock.profit.realized");
	String dividendTotalLabel = MessageUtil.getMessage("stock.asset.status.col.dividend.total");
	String combinedProfitLabel = MessageUtil.getMessage("stock.asset.status.col.combined.profit");
	// 보유 기간 · 연평균 수익률 · 배당 상쇄율. 셋 다 표의 값에서 나온 것이라 새 열을 만들지 않고
	// 각자 자기 값이 있는 칸 안에 둔다 - 이 표는 열을 하나 더 붙이면 넓은 화면에서도 넘친다
	// (실측 2026-09-10: 11 열 최소폭 1,063px, 1340px 부터 넘침 0).
	String holdingPeriodLabel = MessageUtil.getMessage("stock.asset.status.col.holding.period");
	String holdingPeriodUnknownLabel = MessageUtil.getMessage("stock.asset.status.col.holding.period.unknown");
	String holdingYearsPattern = MessageUtil.getMessage("stock.asset.status.cell.holding.years");
	String holdingYearsOnlyPattern = MessageUtil.getMessage("stock.asset.status.cell.holding.years.only");
	String holdingMonthsPattern = MessageUtil.getMessage("stock.asset.status.cell.holding.months");
	String holdingDaysPattern = MessageUtil.getMessage("stock.asset.status.cell.holding.days");
	String holdingSincePattern = MessageUtil.getMessage("stock.asset.status.cell.holding.since");
	String annualizedNameLabel = MessageUtil.getMessage("stock.asset.status.cell.annualized.name");
	String annualizedPattern = MessageUtil.getMessage("stock.asset.status.cell.annualized");
	String annualizedShortPattern = MessageUtil.getMessage("stock.asset.status.cell.annualized.short");
	String coveragePattern = MessageUtil.getMessage("stock.asset.status.cell.coverage");
	String coverageAllLabel = MessageUtil.getMessage("stock.asset.status.cell.coverage.all");
	String heldOnlyNote = MessageUtil.getMessage("stock.asset.status.combined.held.only");
	String heldOnlyDetailNote = MessageUtil.getMessage("stock.asset.status.combined.held.only.detail");
	String showHoldingsLabel = MessageUtil.getMessage("stock.analytics.account.holdings.show");
	String hideHoldingsLabel = MessageUtil.getMessage("stock.analytics.account.holdings.hide");
	String accountHoldingsDetailLabel = MessageUtil.getMessage("stock.analytics.account.holdings.detail");
	// 계좌마다 펼쳐지는 표가 전부 같은 이름이면 낭독기의 표 목록에서 구분이 안 된다
	// (실측 2026-09-12: "계좌 보유 종목 상세" 다섯 개). 이름에 계좌를 넣는다.
	String accountHoldingsDetailNamedPattern = MessageUtil.getMessage("stock.analytics.account.holdings.detail.named");
	String showStockAccountsLabel = MessageUtil.getMessage("stock.analytics.stock.accounts.show");
	String hideStockAccountsLabel = MessageUtil.getMessage("stock.analytics.stock.accounts.hide");
	String stockAccountsDetailLabel = MessageUtil.getMessage("stock.analytics.stock.accounts.detail.title");
	String stockAccountsDetailNamedPattern = MessageUtil.getMessage("stock.analytics.stock.accounts.detail.named");
	String stockWeightInStockLabel = MessageUtil.getMessage("stock.analytics.stock.accounts.weight.stock");
	// 펼침 버튼도 줄마다 같은 글자다 - 실측 2026-09-12: "보유 종목 보기 (3) ▼" 2개 · "(4) ▼" 3개가 이름으로 갈리지 않았다.
	String accountHoldingsToggleAriaPattern = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.action.aria");
	String accountSelectionHelpLabel = MessageUtil.getMessage("stock.analytics.account.selection.helper");
	String accountSelectionSummaryLabel = MessageUtil.getMessage("stock.analytics.account.selection.summary");
	String accountSelectionCountTemplate = MessageUtil.getMessage("stock.analytics.account.selection.count");
	String stockSelectionHelpLabel = MessageUtil.getMessage("stock.analytics.selection.helper");
	String stockSelectionSummaryLabel = MessageUtil.getMessage("stock.analytics.selection.summary");
	String stockSelectionCountTemplate = MessageUtil.getMessage("stock.analytics.selection.count");
	String stockSelectionClearLabel = MessageUtil.getMessage("stock.analytics.selection.clear");
	String stockSelectionWeightLabel = MessageUtil.getMessage("stock.analytics.selection.weight");
	String tagLabel = MessageUtil.getMessage("common.label.tag");
	String noDataLabel = MessageUtil.getMessage("common.message.no.data");
	java.util.function.Function<java.util.UUID, String> stockTagTooltipById = stockItemId -> {
		if (stockItemId == null || stockItemList == null) {
			return tagLabel + ": " + noDataLabel;
		}

		return stockItemList.stream()
			.filter(stockItem -> stockItem != null && stockItemId.equals(stockItem.id()))
			.findFirst()
			.map(stockItem -> stockItem.tags() != null && !stockItem.tags().isEmpty()
				? tagLabel + ": " + String.join(", ", stockItem.tags())
				: tagLabel + ": " + noDataLabel)
			.orElse(tagLabel + ": " + noDataLabel);
	};
}

<div class="flex flex-col gap-6">

	<!-- Section 1: 계좌별 현황 테이블 -->
	<div data-asset-status-account-section>
		<%-- "현재가"는 마지막으로 수집된 종가라, 오늘 시세가 없으면 며칠 전 값일 수 있다.
			 어느 날 기준인지 밝혀 사용자가 실시간 시세로 오해하지 않게 한다. --%>
		<div class="flex flex-wrap items-baseline gap-2 mb-3">
			<h2 class="text-lg font-semibold text-base-content/70" data-page-section>${accountSectionTitle}</h2>
			@if(priceBasisDate != null)
				<span class="text-xs text-base-content/60">${MessageUtil.getMessage("stock.asset.status.price.basis").replace("{0}", priceBasisDate.toString())}</span>
			@endif
		</div>
		@if(accountTotalMap != null && !accountTotalMap.isEmpty())
			<div class="mb-3 flex flex-col gap-3">
				<p class="text-xs text-base-content/60">${accountSelectionHelpLabel}</p>
				<div class="selection-reveal"><div>
				<div class="rounded-2xl border border-secondary/15 bg-secondary/5 px-4 py-3"
					 data-asset-status-account-selection-summary
					 data-count-template="${accountSelectionCountTemplate}"
					 data-total-evaluation-amount="${totalEvaluationAmount != null ? totalEvaluationAmount.toPlainString() : BigDecimal.ZERO.toPlainString()}"
					 aria-live="polite">
					<div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
						<div class="flex flex-wrap items-center gap-2">
							<h3 class="text-sm font-semibold">${accountSelectionSummaryLabel}</h3>
							<span class="badge badge-secondary badge-outline" data-account-selection-count>${accountSelectionSummaryLabel}</span>
						</div>
						<button type="button" class="btn btn-ghost btn-xs self-start" data-account-selection-clear>${stockSelectionClearLabel}</button>
					</div>
					<div class="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-6">
						<div class="rounded-box bg-base-100/80 px-3 py-2">
							<div class="text-xs text-base-content/60">${buyAmountLabel}</div>
							<div class="mt-1 font-mono font-semibold amount-value" data-account-selection-buy-amount>0</div>
						</div>
						<div class="rounded-box bg-base-100/80 px-3 py-2">
							<div class="text-xs text-base-content/60">${evaluationAmountLabel}</div>
							<div class="mt-1 font-mono font-semibold amount-value text-principal" data-account-selection-evaluation-amount>0</div>
						</div>
						<div class="rounded-box bg-base-100/80 px-3 py-2">
							<div class="text-xs text-base-content/60">${evaluationProfitLabel}</div>
							<div class="mt-1 font-mono font-semibold amount-value" data-account-selection-evaluation-profit>0</div>
							<div class="text-xs text-base-content/60" data-account-selection-profit-rate>(0.0%)</div>
						</div>
						<div class="rounded-box bg-base-100/80 px-3 py-2">
							<div class="text-xs text-base-content/60">${principalLabel}</div>
							<div class="mt-1 font-mono font-semibold amount-value" data-account-selection-principal>0</div>
						</div>
						<div class="rounded-box bg-base-100/80 px-3 py-2">
							<div class="text-xs text-base-content/60">${principalReturnLabel}</div>
							<div class="mt-1 font-mono font-semibold amount-value" data-account-selection-principal-return>0</div>
							<div class="text-xs text-base-content/60" data-account-selection-principal-return-rate>(0.0%)</div>
						</div>
						<div class="rounded-box bg-base-100/80 px-3 py-2">
							<div class="text-xs text-base-content/60">${weightLabel}</div>
							<div class="mt-1 font-mono font-semibold" data-account-selection-weight>0.0%</div>
						</div>
					</div>
				</div>
				</div></div>
			</div>
		@endif
		<style>
			.asset-status-account-row-selected > td {
				background-color: color-mix(in oklch, var(--color-secondary) 10%, var(--color-base-100));
			}

			.asset-status-account-row-selected > td:first-child {
				box-shadow: inset 3px 0 0 color-mix(in oklch, var(--color-secondary) 65%, transparent);
			}
		</style>
		@template._components.ui.tableCard(cssClass = "overflow-x-auto", body = @`
			<%-- 투자원금 열은 md(768px)에서 폈는데 그 폭에서 이 표는 813px 이 필요하고 본문 열은 734px 뿐이다.
			     실측 2026-09-10: 767px 6열·표 733·넘침 0 → 768px 7열·표 813·컨 734·넘침 79 → 820px 27 → 845px 2 → 850px 0.
			     넓혔는데 더 나빠지는 구간이라 들어가는 폭에서 편다. 안쪽의 계좌별 보유 상세 표는 별도 분기점이므로 건드리지 않는다.
			     인쇄는 A4 794px 라 이 분기점 아래로 떨어져 투자원금이 빠졌었다(실측 2026-09-10: 인쇄 7열 → 6열).
			     인쇄에는 스크롤이 없어 열이 좁아질 뿐 표 폭은 760px 로 같으므로(넘침 0) print 에서는 항상 보인다. --%>
			<table class="table table-sm w-full" data-sortable-table data-asset-status-account-table aria-label="${accountSectionTitle}">
				<thead class="bg-base-200">
					<tr>
						<th scope="col">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="account"
									data-sort-type="text">
								<span>${accountLabel}</span>
								<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="buyAmount"
									data-sort-type="number">
								<span>${buyAmountLabel}</span>
								<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="evaluationAmount"
									data-sort-type="number">
								<span>${evaluationAmountLabel}</span>
								<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="evaluationProfit"
									data-sort-type="number">
								<span>${evaluationProfitLabel}</span>
								<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right hidden min-[850px]:table-cell print:table-cell">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="principal"
									data-sort-type="number">
								<span>${principalLabel}</span>
								<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="principalReturn"
									data-sort-type="number">
								<span>${principalReturnLabel}</span>
								<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="weight"
									data-sort-type="number">
								<span>${weightLabel}</span>
								<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
							</button>
						</th>
					</tr>
				</thead>
				<tbody>
				@for(Map.Entry<java.util.UUID, TradeProfit> entry : accountTotalMap.entrySet())
					!{TradeProfit acc = entry.getValue();}
					!{List<AssetStatusAccountHoldingView> holdings = accountHoldingMap != null && accountHoldingMap.get(entry.getKey()) != null ? accountHoldingMap.get(entry.getKey()) : List.of();}
					<%-- 파생 수치는 StockAssetStatusUtil 이 계산하고 검사한다. 여기서는 꺼내 쓰기만 한다. --%>
					!{var accountRow = StockAssetStatusUtil.accountRow(acc, accountProfitBasisMap != null ? accountProfitBasisMap.get(entry.getKey()) : null, totalEvaluationAmount);}
					!{BigDecimal buyAmount = accountRow.buyAmount();}
					!{BigDecimal evaluationAmount = accountRow.evaluationAmount();}
					!{BigDecimal accountWeightPct = accountRow.weightPct();}
					!{BigDecimal evaluationProfitAmount = accountRow.evaluationProfit();}
					!{double evaluationProfitRate = accountRow.evaluationProfitRate();}
					!{BigDecimal profitBasis = accountRow.principal();}
					!{BigDecimal principalReturnAmount = accountRow.principalReturn();}
					!{double rate = accountRow.principalReturnRate();}
					!{String detailRowId = "asset-status-account-detail-" + (entry.getKey() != null ? entry.getKey().toString() : "account");}
					<tr
						data-account-selectable-row
						data-sortable-row
						data-selected="false"
						tabindex="0"
						aria-selected="false"
						data-row-select="${MessageUtil.getMessage("stock.table.row.select")}"
						data-account="${entry.getValue().accountName() != null ? entry.getValue().accountName() : ""}"
						data-buy-amount="${buyAmount.toPlainString()}"
						data-evaluation-amount="${evaluationAmount.toPlainString()}"
						data-evaluation-profit="${evaluationProfitAmount.toPlainString()}"
						data-principal="${profitBasis.toPlainString()}"
						data-principal-return="${principalReturnAmount.toPlainString()}"
						data-weight="${accountWeightPct.toPlainString()}"
						data-detail-row-id="${detailRowId}">
						<th scope="row">
							<div class="font-semibold"><a href="${StockDetailLinkUtil.account(entry.getKey() != null ? entry.getKey().toString() : "")}" class="link link-hover">${entry.getValue().accountName()}</a></div>
							@if(holdings.isEmpty())
								<div class="text-xs text-base-content/60">${noHoldingsLabel}</div>
							@else
								<button type="button"
										class="btn btn-ghost btn-xs h-auto min-h-6 px-1 mt-1 text-xs text-base-content/60 hover:text-base-content"
										data-account-detail-toggle="${detailRowId}"
										aria-controls="${detailRowId}"
										aria-expanded="false"
										aria-label="${java.text.MessageFormat.format(accountHoldingsToggleAriaPattern, showHoldingsLabel, entry.getValue().accountName() != null ? entry.getValue().accountName() : "")}"
										data-expand-label="${showHoldingsLabel}"
										data-collapse-label="${hideHoldingsLabel}"
										data-expand-aria="${java.text.MessageFormat.format(accountHoldingsToggleAriaPattern, showHoldingsLabel, entry.getValue().accountName() != null ? entry.getValue().accountName() : "")}"
										data-collapse-aria="${java.text.MessageFormat.format(accountHoldingsToggleAriaPattern, hideHoldingsLabel, entry.getValue().accountName() != null ? entry.getValue().accountName() : "")}">
									<span data-label>${showHoldingsLabel}</span>
									<span class="text-base-content/60">(${holdings.size()})</span>
									<span data-icon class="opacity-60">▼</span>
								</button>
							@endif
						</th>
						<td class="text-right font-mono amount-value text-base-content/60">
							${String.format("%,d", StockFormatUtil.displayWon(buyAmount))}
						</td>
						<td class="text-right font-bold font-mono amount-value text-principal">
							${String.format("%,d", StockFormatUtil.displayWon(evaluationAmount))}
						</td>
						<td class="text-right">
							<div class="font-bold font-mono amount-value ${evaluationProfitAmount.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
								${StockFormatUtil.signedWon(StockFormatUtil.displayWon(evaluationProfitAmount))}
							</div>
							<div class="text-xs font-semibold ${evaluationProfitAmount.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
								(${StockFormatUtil.signedPct(evaluationProfitRate, 1)})
							</div>
						</td>
						<%-- 원금이 계좌 설정의 수동 입력값이면 그 사실을 드러낸다. 표시가 없으면 계산 원가와
							 구분되지 않아, 그 설정이 사라져도 수익률만 조용히 달라진다. --%>
						!{boolean manualPrincipal = manualPrincipalAccountIds != null && manualPrincipalAccountIds.contains(entry.getKey());}
						<td class="text-right font-mono hidden min-[850px]:table-cell print:table-cell amount-value text-base-content/60">
							${String.format("%,d", StockFormatUtil.displayWon(profitBasis))}
							@if(manualPrincipal)
								<span class="badge badge-ghost badge-xs align-middle ml-1"
									  title="${MessageUtil.getMessage("stock.asset.status.label.manual.principal")}">
									${MessageUtil.getMessage("stock.asset.status.label.manual.principal")}
								</span>
							@endif
						</td>
						<td class="text-right ${principalReturnAmount.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
							<div class="font-bold font-mono amount-value">
								${StockFormatUtil.signedWon(StockFormatUtil.displayWon(principalReturnAmount))}
							</div>
							<div class="text-xs font-semibold">
								(${StockFormatUtil.signedPct(rate, 1)})
							</div>
						</td>
						<td class="text-right font-mono text-base-content/70">${StockFormatUtil.pct(accountWeightPct.doubleValue(), 1)}</td>
					</tr>
					@if(!holdings.isEmpty())
						<tr id="${detailRowId}" class="hidden bg-base-200/40" data-account-detail-row>
							<td colspan="7" class="p-0">
								<div class="px-3 py-3">
									<div class="text-xs font-semibold text-base-content/60 mb-2">${accountHoldingsDetailLabel}</div>
									<div class="overflow-x-auto rounded-box border border-base-300/60 bg-base-100">
										<table class="table table-xs w-full" aria-label="${entry.getValue().accountName() != null && !entry.getValue().accountName().isBlank() ? java.text.MessageFormat.format(accountHoldingsDetailNamedPattern, entry.getValue().accountName()) : accountHoldingsDetailLabel}">
											<thead class="bg-base-200/70">
												<tr>
													<th scope="col">${stockNameLabel}</th>
													<th scope="col" class="text-right hidden sm:table-cell print:table-cell">${quantityLabel}</th>
													<th scope="col" class="text-right hidden lg:table-cell print:table-cell">${averageCostLabel}</th>
													<th scope="col" class="text-right hidden md:table-cell print:table-cell">${buyAmountLabel}</th>
													<th scope="col" class="text-right hidden lg:table-cell print:table-cell">${currentPriceLabel}</th>
													<th scope="col" class="text-right">${evaluationAmountLabel}</th>
													<th scope="col" class="text-right">${evaluationProfitLabel}</th>
													<th scope="col" class="text-right hidden xl:table-cell print:table-cell">${weightLabel}</th>
												</tr>
											</thead>
											<tbody>
												<%-- 같은 이유로 계좌 안 비중도 함께 배분한다(실측: 다섯 표 중 둘이 100.1%). --%>
												!{
													java.util.List<BigDecimal> holdingWeightValues = new java.util.ArrayList<>();
													for (AssetStatusAccountHoldingView weightHolding : holdings) {
														holdingWeightValues.add(weightHolding.accountWeightPct() != null ? weightHolding.accountWeightPct() : BigDecimal.ZERO);
													}
													java.util.List<String> holdingWeightShown = StockFormatUtil.balancedPct(holdingWeightValues, 1);
													java.util.Map<java.util.UUID, String> holdingWeightLabelById = new java.util.HashMap<>();
													int holdingWeightIndex = 0;
													for (AssetStatusAccountHoldingView weightHolding : holdings) {
														holdingWeightLabelById.put(weightHolding.stockItemId(), holdingWeightShown.get(holdingWeightIndex++));
													}
												}
												@for(AssetStatusAccountHoldingView holding : holdings)
													<tr>
														<th scope="row" class="font-semibold"><a href="${StockDetailLinkUtil.item(holding.stockItemId() != null ? holding.stockItemId().toString() : "")}" class="link link-hover">${holding.stockItemName() != null ? holding.stockItemName() : "-"}</a></th>
														<td class="text-right font-mono hidden sm:table-cell print:table-cell amount-value">${String.format("%,d", holding.holdingQuantity())}</td>
														<td class="text-right font-mono hidden lg:table-cell print:table-cell amount-value">${String.format("%,d", holding.averageBuyPrice() != null ? holding.averageBuyPrice().setScale(0, java.math.RoundingMode.HALF_UP).longValue() : 0)}</td>
														<td class="text-right font-mono hidden md:table-cell print:table-cell amount-value text-base-content/60">${String.format("%,d", StockFormatUtil.displayWon(holding.buyAmount()))}</td>
														<td class="text-right font-mono hidden lg:table-cell print:table-cell amount-value">${String.format("%,d", StockFormatUtil.displayWon(holding.currentPrice()))}</td>
														<td class="text-right font-mono amount-value text-principal">${String.format("%,d", StockFormatUtil.displayWon(holding.evaluationAmount()))}</td>
														<td class="text-right font-mono font-bold amount-value ${holding.evaluationProfit() != null && holding.evaluationProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
															${StockFormatUtil.signedWon(StockFormatUtil.displayWon(holding.evaluationProfit()))}
															<span class="text-xs">(${StockFormatUtil.signedPct(holding.evaluationProfitRatePct() != null ? holding.evaluationProfitRatePct().doubleValue() : 0.0, 1)})</span>
														</td>
														<td class="text-right hidden xl:table-cell print:table-cell">
															<div class="text-xs text-base-content/70">${accountWeightLabel} ${holdingWeightLabelById.getOrDefault(holding.stockItemId(), StockFormatUtil.pct(holding.accountWeightPct() != null ? holding.accountWeightPct().doubleValue() : 0.0, 1))}</div>
															<div class="text-xs text-base-content/60">${totalWeightLabel} ${StockFormatUtil.pct(holding.totalWeightPct() != null ? holding.totalWeightPct().doubleValue() : 0.0, 1)}</div>
														</td>
													</tr>
												@endfor
											</tbody>
										</table>
									</div>
								</div>
							</td>
						</tr>
					@endif
				@endfor
				</tbody>
				<tfoot class="bg-base-200 font-bold">
					!{var accountTotals = StockAssetStatusUtil.accountTotals(accountTotalMap, accountProfitBasisMap);}
					!{BigDecimal totalAccountEvaluationAmount = accountTotals.evaluationAmount();}
					!{BigDecimal totalBuyAmount = accountTotals.buyAmount();}
					!{BigDecimal totalPrincipal = accountTotals.principal();}
					!{BigDecimal totalAccountEvaluationProfit = accountTotals.evaluationProfit();}
					!{BigDecimal totalPrincipalReturnAmount = accountTotals.principalReturn();}
					!{double totalEvaluationProfitRate = accountTotals.evaluationProfitRate();}
					!{double totalPrincipalReturnRate = accountTotals.principalReturnRate();}
					<tr>
						<th scope="row">${totalLabel}</th>
						<td class="text-right font-mono amount-value text-base-content/60">
							${String.format("%,d", StockFormatUtil.displayWon(totalBuyAmount))}
						</td>
						<td class="text-right font-mono amount-value text-principal">
							${String.format("%,d", StockFormatUtil.displayWon(totalAccountEvaluationAmount))}
						</td>
						<td class="text-right ${totalAccountEvaluationProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
							<div class="font-bold font-mono amount-value">
								${StockFormatUtil.signedWon(StockFormatUtil.displayWon(totalAccountEvaluationProfit))}
							</div>
							<div class="text-xs font-semibold">
								(${StockFormatUtil.signedPct(totalEvaluationProfitRate, 1)})
							</div>
						</td>
						<td class="text-right font-mono hidden min-[850px]:table-cell print:table-cell amount-value text-base-content/60">
							${String.format("%,d", StockFormatUtil.displayWon(totalPrincipal))}
						</td>
						<td class="text-right ${totalPrincipalReturnAmount.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
							<div class="font-bold font-mono amount-value">
								${StockFormatUtil.signedWon(StockFormatUtil.displayWon(totalPrincipalReturnAmount))}
							</div>
							<div class="text-xs font-semibold">
								(${StockFormatUtil.signedPct(totalPrincipalReturnRate, 1)})
							</div>
						</td>
						<td class="text-right font-mono text-base-content/70">100.0%</td>
					</tr>
				</tfoot>
			</table>
		`)
	</div>

	<!-- Section 2: 종목별 집계 테이블 (계좌 통합) -->
	<div data-asset-status-stock-section>
		<h2 class="text-lg font-semibold mb-3 text-base-content/70" data-page-section="${stockSectionTitle}">${stockSectionTitle} <span class="text-xs font-normal text-base-content/60">(${allAccountsLabel})</span></h2>
		@if(stockAggregated != null && !stockAggregated.isEmpty())
			<div class="mb-3 flex flex-col gap-3">
				<p class="text-xs text-base-content/60">${stockSelectionHelpLabel}</p>
				<%-- 이 표에는 보유 중인 종목만 나온다. 실현손익·배당을 열로 넣으면서 밝히지 않으면
					 합계를 전체 누적으로 오해한다(실측 2026-09-03: 이미 다 판 34 종목에
					 실현 85,279,840 · 배당 6,582,497 이 더 있다). --%>
				<%-- 얼마나 작은지도 적는다 - 실측 2026-09-11: 이 합계의 실현손익은 140,350,295 인데 대시보드·매매·
					 자산 성장은 225,630,135 를 적는다(차이 85,279,840, 37.8%). 차이를 숫자로 밝히지 않으면
					 같은 이름의 값이 화면마다 다른 이유를 알 길이 없다. 판 종목이 없으면 종전 문구 그대로. --%>
				<p class="text-xs text-base-content/60" data-asset-status-held-only-note data-amount-basis-text data-amount-basis="${soldOutStockCount > 0 ? java.text.MessageFormat.format(heldOnlyDetailNote, String.valueOf(soldOutStockCount), String.format("%,d", StockFormatUtil.displayWon(soldOutRealizedProfit)), String.format("%,d", StockFormatUtil.displayWon(soldOutDividend))) : heldOnlyNote}">${soldOutStockCount > 0 ? java.text.MessageFormat.format(heldOnlyDetailNote, String.valueOf(soldOutStockCount), String.format("%,d", StockFormatUtil.displayWon(soldOutRealizedProfit)), String.format("%,d", StockFormatUtil.displayWon(soldOutDividend))) : heldOnlyNote}</p>
				<div class="selection-reveal"><div>
				<div class="rounded-2xl border border-info/15 bg-info/5 px-4 py-3"
					 data-asset-status-stock-selection-summary
					 data-count-template="${stockSelectionCountTemplate}"
					 data-total-evaluation-amount="${totalEvaluationAmount != null ? totalEvaluationAmount.toPlainString() : BigDecimal.ZERO.toPlainString()}"
					 aria-live="polite">
					<div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
						<div class="flex flex-wrap items-center gap-2">
							<h3 class="text-sm font-semibold">${stockSelectionSummaryLabel}</h3>
							<span class="badge badge-info badge-outline" data-selection-count>${stockSelectionSummaryLabel}</span>
						</div>
						<button type="button" class="btn btn-ghost btn-xs self-start" data-selection-clear>${stockSelectionClearLabel}</button>
					</div>
					<div class="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
						<div class="rounded-box bg-base-100/80 px-3 py-2">
							<div class="text-xs text-base-content/60">${buyAmountLabel}</div>
							<div class="mt-1 font-mono font-semibold amount-value" data-selection-buy-amount>0</div>
						</div>
						<div class="rounded-box bg-base-100/80 px-3 py-2">
							<div class="text-xs text-base-content/60">${evaluationAmountLabel}</div>
							<div class="mt-1 font-mono font-semibold amount-value text-principal" data-selection-evaluation-amount>0</div>
						</div>
						<div class="rounded-box bg-base-100/80 px-3 py-2">
							<div class="text-xs text-base-content/60">${evaluationProfitLabel}</div>
							<div class="mt-1 font-mono font-semibold amount-value" data-selection-evaluation-profit>0</div>
							<div class="text-xs text-base-content/60" data-selection-profit-rate>(0.0%)</div>
						</div>
						<div class="rounded-box bg-base-100/80 px-3 py-2">
							<div class="text-xs text-base-content/60">${stockSelectionWeightLabel}</div>
							<div class="mt-1 font-mono font-semibold amount-value" data-selection-weight>0.0%</div>
						</div>
					</div>
				</div>
				</div></div>
			</div>
		@endif
		<style>
			.asset-status-stock-row {
				cursor: pointer;
			}

			.asset-status-stock-row.asset-status-stock-row-selected > td {
				background-color: color-mix(in oklch, var(--color-info) 10%, var(--color-base-100));
			}

			.asset-status-stock-row.asset-status-stock-row-selected > td:first-child {
				box-shadow: inset 3px 0 0 color-mix(in oklch, var(--color-info) 65%, transparent);
			}

			.asset-status-stock-row:focus-visible {
				outline: 2px solid color-mix(in oklch, var(--color-info) 35%, transparent);
				outline-offset: -2px;
			}
		</style>
		@template._components.ui.tableCard(cssClass = "overflow-x-auto", body = @`
			<%-- 평균 단가·실현 손익·누적 배당 열은 xl(1280px)에서 폈는데, 그 폭에서 이 표는 1,125px 이 필요하고 본문 열은 1,022px 뿐이다.
			     실측 2026-09-10: 1279px 8열·표 1,021·넘침 0 → 1280px 11열·표 1,125·컨테이너 1,022·넘침 103 → 1366px 17 → 1400px 0. 2026-09-10 헤더 접기 뒤 최소폭이 1,063px 이 되어 1340px 부터 넘침 0(1320px 1).
			     넓혔는데 더 나빠지는 구간이라 실제로 들어가는 폭에서 편다. 같은 파일의 계좌별 보유 상세 표(47열)는 xl 에서 들어가므로 그대로 둔다. --%>
			<table class="table table-sm w-full print-dense" data-sortable-table data-asset-status-stock-table aria-label="${stockSectionTitle}">
				<thead class="bg-base-200">
					<tr>
						<th scope="col">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="stockName"
									data-sort-type="text">
								<span>${stockNameLabel}</span>
								<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
							</button>
						</th>
						<%-- 보유 기간은 종목 이름 다음에 온다 - "이 종목을 언제부터 들고 있나" 는 이름 바로 옆에서 읽힌다.
						     정렬 값은 행의 data-holding-days(일수)다. '6년 6개월' 같은 표시 문자열로 정렬하면 6개월이 6년보다 뒤에 온다. --%>
						<th scope="col">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="holdingDays"
									data-sort-type="number">
								<span>${holdingPeriodLabel}</span>
								<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right hidden lg:table-cell print:table-cell">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="quantity"
									data-sort-type="number">
								<span>${quantityLabel}</span>
								<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right hidden min-[1340px]:table-cell print:table-cell">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="averageCost"
									data-sort-type="number">
								<span>${averageCostLabel}</span>
								<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right hidden sm:table-cell print:table-cell">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="buyAmount"
									data-sort-type="number">
								<span>${buyAmountLabel}</span>
								<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right hidden lg:table-cell print:table-cell">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="currentPrice"
									data-sort-type="number">
								<span>${currentPriceLabel}</span>
								<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="evaluationAmount"
									data-sort-type="number">
								<span>${evaluationAmountLabel}</span>
								<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="evaluationProfit"
									data-sort-type="number">
								<span>${evaluationProfitLabel}</span>
								<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right hidden min-[1340px]:table-cell print:table-cell">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="realizedProfit"
									data-sort-type="number">
								<span>${realizedProfitLabel}</span>
								<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right hidden min-[1340px]:table-cell print:table-cell">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="dividendTotal"
									data-sort-type="number">
								<span>${dividendTotalLabel}</span>
								<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right hidden lg:table-cell print:table-cell">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="combinedProfit"
									data-sort-type="number">
								<span>${combinedProfitLabel}</span>
								<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="weight"
									data-sort-type="number">
								<span>${weightLabel}</span>
								<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
							</button>
						</th>
					</tr>
				</thead>
				<tbody>
					@if(stockAggregated == null || stockAggregated.isEmpty())
						<tr>
							<td colspan="12">@template._components.ui.emptyState(message = noHoldingsLabel)</td>
						</tr>
					@else
						<%-- 비중은 행마다 따로 반올림하면 표시값의 합이 어긋난다(실측 2026-09-11: 이 표 9행이 99.9%).
						     한 번에 최대잔여법으로 배분해 합이 100.0% 가 되게 한다. --%>
						!{
							java.util.List<BigDecimal> stockWeightValues = new java.util.ArrayList<>();
							for (TradeProfit weightItem : stockAggregated) {
								stockWeightValues.add(StockAssetStatusUtil.stockRow(weightItem, totalEvaluationAmount).totalWeightPct());
							}
							java.util.List<String> stockWeightShown = StockFormatUtil.balancedPct(stockWeightValues, 1);
							java.util.Map<java.util.UUID, String> stockWeightLabelById = new java.util.HashMap<>();
							int stockWeightIndex = 0;
							for (TradeProfit weightItem : stockAggregated) {
								stockWeightLabelById.put(weightItem.stockItemId(), stockWeightShown.get(stockWeightIndex++));
							}
						}
						@for(TradeProfit item : stockAggregated)
							!{var stockRow = StockAssetStatusUtil.stockRow(item, totalEvaluationAmount);}
							!{BigDecimal evalProfitRate = stockRow.evaluationProfitRatePct();}
							!{BigDecimal totalWeightPct = stockRow.totalWeightPct();}
							!{String stockTagTooltip = stockTagTooltipById.apply(item.stockItemId());}
							!{var breakdown = stockProfitBreakdown != null && stockProfitBreakdown.get(item.stockItemId()) != null
								? stockProfitBreakdown.get(item.stockItemId())
								: new net.luversof.web.gate.stock.util.StockCombinedProfitUtil.Breakdown(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);}
							!{java.time.LocalDate firstBuyDate = firstBuyDateByStockItem == null ? null : firstBuyDateByStockItem.get(item.stockItemId());}
							!{List<AssetStatusStockAccountView> holdingAccounts = stockHoldingAccountMap != null && stockHoldingAccountMap.get(item.stockItemId()) != null ? stockHoldingAccountMap.get(item.stockItemId()) : List.of();}
							!{String stockDetailRowId = "asset-status-stock-detail-" + (item.stockItemId() != null ? item.stockItemId().toString() : "stock");}
							!{String stockRowName = item.stockItemName() != null ? item.stockItemName() : "";}
							!{var holdingReturn = StockHoldingReturnUtil.of(firstBuyDate, holdingBasisDate, cashFlowsByStockItem == null ? null : cashFlowsByStockItem.get(item.stockItemId()), item.evaluationAmount(), item.evaluationProfit(), breakdown.dividendTotal());}
							!{String holdingPeriodText = !holdingReturn.hasPeriod() ? ""
								: (holdingReturn.daysOnly()
									? java.text.MessageFormat.format(holdingDaysPattern, String.valueOf(holdingReturn.holdingDays()))
									: (holdingReturn.years() == 0
										? java.text.MessageFormat.format(holdingMonthsPattern, String.valueOf(holdingReturn.months()))
										: (holdingReturn.months() == 0
											? java.text.MessageFormat.format(holdingYearsOnlyPattern, String.valueOf(holdingReturn.years()))
											: java.text.MessageFormat.format(holdingYearsPattern, String.valueOf(holdingReturn.years()), String.valueOf(holdingReturn.months())))));}
							<%-- 배당 상쇄율은 평가손익이 마이너스일 때만 뜻이 있다 - 덮을 것이 없으면 적지 않는다. --%>
							!{String coverageText = holdingReturn.coveragePct() == null || breakdown.dividendTotal().signum() <= 0 ? ""
								: (holdingReturn.coveragePct() >= 100 ? coverageAllLabel : java.text.MessageFormat.format(coveragePattern, String.valueOf(holdingReturn.coveragePct())));}
							!{String annualizedText = holdingReturn.annualizedPct() == null ? ""
								: java.text.MessageFormat.format(annualizedPattern, StockFormatUtil.signedPct(holdingReturn.annualizedPct().doubleValue(), 1));}
							<%-- 보유 1 년 미만은 연환산이 확대된 값이다(실측 2026-09-14: 117 일 -6.0% -> 연 -17.5%). 무슨 값인지 근거를 함께 준다. --%>
							!{String annualizedNote = holdingReturn.shortTerm()
								? java.text.MessageFormat.format(annualizedShortPattern, String.valueOf(holdingReturn.holdingDays()))
								: annualizedNameLabel;}
							<tr class="asset-status-stock-row"
								data-sortable-row
								data-stock-selectable-row
								tabindex="0"
								aria-selected="false"
								data-row-select="${MessageUtil.getMessage("stock.table.row.select")}"
								data-stock-name="${item.stockItemName() != null ? item.stockItemName() : ""}"
								data-quantity="${BigDecimal.valueOf(item.holdingQuantity()).toPlainString()}"
								data-average-cost="${item.averageBuyPrice() != null ? item.averageBuyPrice().toPlainString() : BigDecimal.ZERO.toPlainString()}"
								data-buy-amount="${item.totalBuyCost() != null ? item.totalBuyCost().toPlainString() : BigDecimal.ZERO.toPlainString()}"
								data-current-price="${item.currentPrice() != null ? item.currentPrice().toPlainString() : BigDecimal.ZERO.toPlainString()}"
								data-evaluation-amount="${item.evaluationAmount() != null ? item.evaluationAmount().toPlainString() : BigDecimal.ZERO.toPlainString()}"
								data-evaluation-profit="${item.evaluationProfit() != null ? item.evaluationProfit().toPlainString() : BigDecimal.ZERO.toPlainString()}"
								data-weight="${totalWeightPct.toPlainString()}"
								data-realized-profit="${breakdown.realizedProfit().toPlainString()}"
								data-dividend-total="${breakdown.dividendTotal().toPlainString()}"
								data-combined-profit="${breakdown.combinedProfit().toPlainString()}"
								data-holding-days="${holdingReturn.holdingDays()}"
								data-detail-row-id="${stockDetailRowId}">
								<th scope="row" class="font-semibold">
									<div><a href="${StockDetailLinkUtil.item(item.stockItemId() != null ? item.stockItemId().toString() : "")}" class="link link-hover" data-stock-tag-tooltip="${stockTagTooltip}">${item.stockItemName() != null ? item.stockItemName() : "-"}</a></div>
									<%-- 계좌 표의 "보유 종목 보기" 와 같은 모양 · 같은 여닫기(assetStatus.ts). 이름에 종목을 넣는다 - 줄마다 같은 글자면 버튼끼리 갈리지 않는다. --%>
									@if(!holdingAccounts.isEmpty())
										<button type="button"
												class="btn btn-ghost btn-xs h-auto min-h-6 px-1 mt-1 text-xs font-normal text-base-content/60 hover:text-base-content"
												data-stock-detail-toggle="${stockDetailRowId}"
												aria-controls="${stockDetailRowId}"
												aria-expanded="false"
												aria-label="${java.text.MessageFormat.format(accountHoldingsToggleAriaPattern, showStockAccountsLabel, stockRowName)}"
												data-expand-label="${showStockAccountsLabel}"
												data-collapse-label="${hideStockAccountsLabel}"
												data-expand-aria="${java.text.MessageFormat.format(accountHoldingsToggleAriaPattern, showStockAccountsLabel, stockRowName)}"
												data-collapse-aria="${java.text.MessageFormat.format(accountHoldingsToggleAriaPattern, hideStockAccountsLabel, stockRowName)}">
											<span data-label>${showStockAccountsLabel}</span>
											<span class="text-base-content/60">(${holdingAccounts.size()})</span>
											<span data-icon class="opacity-60">▼</span>
										</button>
									@endif
								</th>
								<td class="text-base-content/70" data-holding-period>
									@if(holdingReturn.hasPeriod())
										<span title="${java.text.MessageFormat.format(holdingSincePattern, firstBuyDate.toString())}">${holdingPeriodText}</span><span class="sr-only">, ${java.text.MessageFormat.format(holdingSincePattern, firstBuyDate.toString())}</span>
									@else
										<%-- 최초 매수일을 모르는 종목. 빈 칸은 "0 일"로 읽히므로 사유를 적는다. --%>
										<span class="text-base-content/60" aria-hidden="true" title="${holdingPeriodUnknownLabel}">-</span><span class="sr-only">${holdingPeriodUnknownLabel}</span>
									@endif
								</td>
								<td class="text-right font-mono hidden lg:table-cell print:table-cell amount-value">${String.format("%,d", item.holdingQuantity())}</td>
								<td class="text-right font-mono hidden min-[1340px]:table-cell print:table-cell amount-value">${String.format("%,d", item.averageBuyPrice() != null ? item.averageBuyPrice().setScale(0, java.math.RoundingMode.HALF_UP).longValue() : 0)}</td>
								<td class="text-right font-mono hidden sm:table-cell print:table-cell amount-value text-base-content/60">${String.format("%,d", StockFormatUtil.displayWon(item.totalBuyCost()))}</td>
								<td class="text-right font-mono hidden lg:table-cell print:table-cell amount-value">${String.format("%,d", StockFormatUtil.displayWon(item.currentPrice()))}</td>
								<td class="text-right font-mono font-semibold amount-value text-principal">${String.format("%,d", StockFormatUtil.displayWon(item.evaluationAmount()))}</td>
								<td class="text-right font-mono font-bold ${item.evaluationProfit() != null && item.evaluationProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
									<span class="amount-value">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(item.evaluationProfit()))}</span>
									<span class="text-xs">(${StockFormatUtil.signedPct(evalProfitRate.doubleValue(), 1)})</span>
								@if(!coverageText.isEmpty())
									<%-- 배당이 이 평가손실의 얼마를 덮었나. 자산 현황에 따로 있던 카드가 막대 두 개로 말하던 값인데, 막대는 줄 안에서 정규화돼 늘 한쪽이 100%였다(=배지와 같은 말). --%>
									<div class="text-xs font-normal text-dividend" data-dividend-coverage data-coverage-pct="${holdingReturn.coveragePct()}">${coverageText}</div>
								@endif
								</td>
								<%-- 판 적이 없으면 실현손익은 0 이 아니라 '없음' 이다. 배당도 마찬가지. --%>
								<td class="text-right font-mono hidden min-[1340px]:table-cell print:table-cell">
									@if(breakdown.realizedProfit().signum() != 0)
										<span class="amount-value ${breakdown.realizedProfit().signum() >= 0 ? "text-profit" : "text-loss"}">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(breakdown.realizedProfit()))}</span>
									@else
										<span class="text-base-content/60" aria-hidden="true" title="${MessageUtil.getMessage("stock.profit.realized.none")}">-</span><span class="sr-only">${MessageUtil.getMessage("stock.profit.realized.none")}</span>
									@endif
								</td>
								<td class="text-right font-mono hidden min-[1340px]:table-cell print:table-cell">
									@if(breakdown.dividendTotal().signum() != 0)
										<span class="amount-value text-dividend">${String.format("%,d", StockFormatUtil.displayWon(breakdown.dividendTotal()))}</span>
									@else
										<span class="text-base-content/60" aria-hidden="true" title="${MessageUtil.getMessage("stock.dividend.received.none")}">-</span><span class="sr-only">${MessageUtil.getMessage("stock.dividend.received.none")}</span>
									@endif
								</td>
								<td class="text-right font-mono font-bold hidden lg:table-cell print:table-cell ${breakdown.combinedProfit().signum() >= 0 ? "text-profit" : "text-loss"}">
									<span class="amount-value">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(breakdown.combinedProfit()))}</span>
								@if(!annualizedText.isEmpty())
									<%-- 1 년이 안 된 보유의 연평균은 흐리게 둔다(요구: 표시하되 약하게). 다만 /50 은 대비 3.55:1 로
									     읽히지 않았다 - /60(4.95:1) 이 기준을 넘는 가장 흐린 단계다. 여전히 /70 보다 약하다. --%>
									<div class="text-xs font-normal ${holdingReturn.shortTerm() ? "text-base-content/60" : "text-base-content/70"}" data-annualized-return data-short-term="${String.valueOf(holdingReturn.shortTerm())}" title="${annualizedNote}"><span class="sr-only">${annualizedNameLabel} </span>${annualizedText}<span class="sr-only">, ${annualizedNote}</span></div>
								@endif
								</td>
								<td class="text-right font-mono text-base-content/70">${stockWeightLabelById.getOrDefault(item.stockItemId(), StockFormatUtil.pct(totalWeightPct.doubleValue(), 1))}</td>
							</tr>
							<%-- 이 종목을 가진 계좌들. 계좌 표의 보유 종목 상세와 같은 계좌 x 종목 행이라, 종이에는 계좌 표 쪽만 펼쳐 찍는다(같은 내용이 두 번 찍히지 않게). --%>
							@if(!holdingAccounts.isEmpty())
								<tr id="${stockDetailRowId}" class="hidden bg-base-200/40" data-stock-detail-row>
									<td colspan="12" class="p-0">
										<div class="px-3 py-3">
											<div class="text-xs font-semibold text-base-content/60 mb-2">${stockAccountsDetailLabel}</div>
											<div class="overflow-x-auto rounded-box border border-base-300/60 bg-base-100">
												<table class="table table-xs w-full" aria-label="${!stockRowName.isBlank() ? java.text.MessageFormat.format(stockAccountsDetailNamedPattern, stockRowName) : stockAccountsDetailLabel}">
													<thead class="bg-base-200/70">
														<tr>
															<th scope="col">${accountLabel}</th>
															<th scope="col" class="text-right hidden sm:table-cell print:table-cell">${quantityLabel}</th>
															<th scope="col" class="text-right hidden lg:table-cell print:table-cell">${averageCostLabel}</th>
															<th scope="col" class="text-right hidden md:table-cell print:table-cell">${buyAmountLabel}</th>
															<th scope="col" class="text-right">${evaluationAmountLabel}</th>
															<th scope="col" class="text-right">${evaluationProfitLabel}</th>
															<th scope="col" class="text-right hidden xl:table-cell print:table-cell">${weightLabel}</th>
														</tr>
													</thead>
													<tbody>
														<%-- 종목 안 비중은 함께 배분해 합이 100.0% 가 되게 한다(계좌 표 펼침과 같은 이유). --%>
														!{
															java.util.List<BigDecimal> accountShareValues = new java.util.ArrayList<>();
															for (AssetStatusStockAccountView shareAccount : holdingAccounts) {
																accountShareValues.add(shareAccount.stockWeightPct() != null ? shareAccount.stockWeightPct() : BigDecimal.ZERO);
															}
															java.util.List<String> accountShareShown = StockFormatUtil.balancedPct(accountShareValues, 1);
														}
														@for(int accountIndex = 0; accountIndex < holdingAccounts.size(); accountIndex++)
															!{AssetStatusStockAccountView holdingAccount = holdingAccounts.get(accountIndex);}
															<tr data-stock-detail-account="${holdingAccount.accountId() != null ? holdingAccount.accountId().toString() : ""}">
																<th scope="row" class="font-semibold"><a href="${StockDetailLinkUtil.account(holdingAccount.accountId() != null ? holdingAccount.accountId().toString() : "")}" class="link link-hover">${holdingAccount.accountName() != null ? holdingAccount.accountName() : "-"}</a></th>
																<td class="text-right font-mono hidden sm:table-cell print:table-cell amount-value">${String.format("%,d", holdingAccount.holdingQuantity())}</td>
																<td class="text-right font-mono hidden lg:table-cell print:table-cell amount-value">${String.format("%,d", holdingAccount.averageBuyPrice() != null ? holdingAccount.averageBuyPrice().setScale(0, java.math.RoundingMode.HALF_UP).longValue() : 0)}</td>
																<td class="text-right font-mono hidden md:table-cell print:table-cell amount-value text-base-content/60">${String.format("%,d", StockFormatUtil.displayWon(holdingAccount.buyAmount()))}</td>
																<td class="text-right font-mono amount-value text-principal">${String.format("%,d", StockFormatUtil.displayWon(holdingAccount.evaluationAmount()))}</td>
																<td class="text-right font-mono font-bold amount-value ${holdingAccount.evaluationProfit() != null && holdingAccount.evaluationProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
																	${StockFormatUtil.signedWon(StockFormatUtil.displayWon(holdingAccount.evaluationProfit()))}
																	<span class="text-xs">(${StockFormatUtil.signedPct(holdingAccount.evaluationProfitRatePct() != null ? holdingAccount.evaluationProfitRatePct().doubleValue() : 0.0, 1)})</span>
																</td>
																<td class="text-right hidden xl:table-cell print:table-cell">
																	<div class="text-xs text-base-content/70">${stockWeightInStockLabel} ${accountShareShown.get(accountIndex)}</div>
																	<div class="text-xs text-base-content/60">${totalWeightLabel} ${StockFormatUtil.pct(holdingAccount.totalWeightPct() != null ? holdingAccount.totalWeightPct().doubleValue() : 0.0, 1)}</div>
																</td>
															</tr>
														@endfor
													</tbody>
												</table>
											</div>
										</div>
									</td>
								</tr>
							@endif
						@endfor
					@endif
				</tbody>
				<tfoot class="bg-base-200 font-bold">
					!{BigDecimal totalStockBuyAmount = StockAssetStatusUtil.totalBuyCost(stockAggregated);}
					<tr>
						<th scope="row">${totalLabel}</th>
						<%-- 보유 기간 열은 합계가 없다(기간을 더할 수는 없다). 칸 수만 맞춘다. --%>
						<td></td>
						<td class="hidden lg:table-cell print:table-cell"></td>
						<td class="hidden min-[1340px]:table-cell print:table-cell"></td>
						<td class="text-right font-mono hidden sm:table-cell print:table-cell amount-value text-base-content/60">${String.format("%,d", StockFormatUtil.displayWon(totalStockBuyAmount))}</td>
						<td class="hidden lg:table-cell print:table-cell"></td>
						<td class="text-right font-mono amount-value text-principal">${String.format("%,d", StockFormatUtil.displayWon(totalEvaluationAmount))}</td>
						<td class="text-right font-mono font-bold amount-value ${totalEvaluationProfit != null && totalEvaluationProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(totalEvaluationProfit))}</td>
						<td class="text-right font-mono hidden min-[1340px]:table-cell print:table-cell ${stockProfitBreakdownTotal.realizedProfit().signum() >= 0 ? "text-profit" : "text-loss"}"><span class="amount-value">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(stockProfitBreakdownTotal.realizedProfit()))}</span></td>
						<td class="text-right font-mono hidden min-[1340px]:table-cell print:table-cell"><span class="amount-value text-dividend">${String.format("%,d", StockFormatUtil.displayWon(stockProfitBreakdownTotal.dividendTotal()))}</span></td>
						<td class="text-right font-mono font-bold hidden lg:table-cell print:table-cell ${stockProfitBreakdownTotal.combinedProfit().signum() >= 0 ? "text-profit" : "text-loss"}"><span class="amount-value">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(stockProfitBreakdownTotal.combinedProfit()))}</span></td>
						<td class="text-right font-mono text-base-content/70">${totalEvaluationAmount != null && totalEvaluationAmount.compareTo(BigDecimal.ZERO) > 0 ? "100.0%" : "0.0%"}</td>
					</tr>
				</tfoot>
			</table>
		`)
	</div>

</div>

<%-- 브라우저 동작(상세 펼침·정렬·선택 합계·태그 툴팁)은 /js/stock/assetStatus.js 에 있다.
	 2026-09-08 까지 여기 669 줄 인라인 스크립트였다 - JTE 식이 없는 순수 JS 라 파일로 뺐다.
	 모듈 스크립트는 문서당 한 번 실행되고, 재스왑은 htmx:afterSettle 에서 다시 초기화한다. --%>
<script type="module" src="/js/stock/assetStatus.js"></script>
