@import net.luversof.web.gate.util.CspNonceHolder
@import java.math.BigDecimal
@import java.util.List
@import java.util.Map
@import net.luversof.web.gate.stock.domain.StockItem
@import net.luversof.web.gate.stock.domain.TradeProfit
@import net.luversof.web.gate.stock.dto.response.AssetStatusAccountHoldingView
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.stock.util.StockFormatUtil
@import net.luversof.web.gate.stock.util.StockAssetStatusUtil
@import net.luversof.web.gate.stock.util.StockDividendCoverageUtil

@param Map<java.util.UUID, TradeProfit> accountTotalMap
@param Map<java.util.UUID, BigDecimal> accountProfitBasisMap
@param java.util.Set<java.util.UUID> manualPrincipalAccountIds = java.util.Set.of()
@param Map<java.util.UUID, List<AssetStatusAccountHoldingView>> accountHoldingMap
@param List<StockItem> stockItemList = java.util.List.of()
@param List<TradeProfit> stockAggregated
@param Map<java.util.UUID, net.luversof.web.gate.stock.util.StockCombinedProfitUtil.Breakdown> stockProfitBreakdown = java.util.Map.of()
@param net.luversof.web.gate.stock.util.StockCombinedProfitUtil.Breakdown stockProfitBreakdownTotal = new net.luversof.web.gate.stock.util.StockCombinedProfitUtil.Breakdown(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
@param BigDecimal totalEvaluationAmount
@param BigDecimal totalEvaluationProfit
@param java.time.LocalDate priceBasisDate = null

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
	String heldOnlyNote = MessageUtil.getMessage("stock.asset.status.combined.held.only");
	String coverageTitle = MessageUtil.getMessage("stock.asset.status.coverage.title");
	String coverageDesc = MessageUtil.getMessage("stock.asset.status.coverage.desc");
	String coverageNoLoss = MessageUtil.getMessage("stock.asset.status.coverage.no.loss");
	String coverageAll = MessageUtil.getMessage("stock.asset.status.coverage.covered.all");
	String coveragePctPattern = MessageUtil.getMessage("stock.asset.status.coverage.covered.pct");
	String showHoldingsLabel = MessageUtil.getMessage("stock.analytics.account.holdings.show");
	String hideHoldingsLabel = MessageUtil.getMessage("stock.analytics.account.holdings.hide");
	String accountHoldingsDetailLabel = MessageUtil.getMessage("stock.analytics.account.holdings.detail");
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
				/* 좁은 화면에서 고정된 첫 칸도 같은 선택 색을 쓰도록 변수로 넘긴다. */
				--sticky-first-bg: color-mix(in oklch, var(--color-secondary) 10%, var(--color-base-100));
			}

			.asset-status-account-row-selected > td:first-child {
				box-shadow: inset 3px 0 0 color-mix(in oklch, var(--color-secondary) 65%, transparent);
			}
		</style>
		@template._components.ui.tableCard(cssClass = "overflow-x-auto", body = @`
			<table class="table table-sm w-full" data-sortable-table data-asset-status-account-table aria-label="${accountSectionTitle}">
				<thead class="bg-base-200">
					<tr>
						<th scope="col">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="account"
									data-sort-type="text">
								<span>${accountLabel}</span>
								<span data-sort-indicator class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="buyAmount"
									data-sort-type="number">
								<span>${buyAmountLabel}</span>
								<span data-sort-indicator class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="evaluationAmount"
									data-sort-type="number">
								<span>${evaluationAmountLabel}</span>
								<span data-sort-indicator class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="evaluationProfit"
									data-sort-type="number">
								<span>${evaluationProfitLabel}</span>
								<span data-sort-indicator class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right hidden md:table-cell">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="principal"
									data-sort-type="number">
								<span>${principalLabel}</span>
								<span data-sort-indicator class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="principalReturn"
									data-sort-type="number">
								<span>${principalReturnLabel}</span>
								<span data-sort-indicator class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="weight"
									data-sort-type="number">
								<span>${weightLabel}</span>
								<span data-sort-indicator class="opacity-60">↕</span>
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
						<td>
							<div class="font-semibold"><a href="/stock/account?accountId=${entry.getKey() != null ? entry.getKey().toString() : ""}" class="link link-hover">${entry.getValue().accountName()}</a></div>
							@if(holdings.isEmpty())
								<div class="text-xs text-base-content/60">${noHoldingsLabel}</div>
							@else
								<button type="button"
										class="btn btn-ghost btn-xs h-auto min-h-6 px-1 mt-1 text-xs text-base-content/60 hover:text-base-content"
										data-account-detail-toggle="${detailRowId}"
										aria-controls="${detailRowId}"
										aria-expanded="false"
										data-expand-label="${showHoldingsLabel}"
										data-collapse-label="${hideHoldingsLabel}">
									<span data-label>${showHoldingsLabel}</span>
									<span class="text-base-content/60">(${holdings.size()})</span>
									<span data-icon class="opacity-60">▼</span>
								</button>
							@endif
						</td>
						<td class="text-right font-mono amount-value text-base-content/60">
							${String.format("%,d", StockFormatUtil.displayWon(buyAmount))}
						</td>
						<td class="text-right font-bold font-mono amount-value text-principal">
							${String.format("%,d", StockFormatUtil.displayWon(evaluationAmount))}
						</td>
						<td class="text-right">
							<div class="font-bold font-mono amount-value ${evaluationProfitAmount.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
								${String.format("%+,d", StockFormatUtil.displayWon(evaluationProfitAmount))}
							</div>
							<div class="text-xs font-semibold ${evaluationProfitAmount.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
								(${StockFormatUtil.signedPct(evaluationProfitRate, 1)})
							</div>
						</td>
						<%-- 원금이 계좌 설정의 수동 입력값이면 그 사실을 드러낸다. 표시가 없으면 계산 원가와
							 구분되지 않아, 그 설정이 사라져도 수익률만 조용히 달라진다. --%>
						!{boolean manualPrincipal = manualPrincipalAccountIds != null && manualPrincipalAccountIds.contains(entry.getKey());}
						<td class="text-right font-mono hidden md:table-cell amount-value text-base-content/60">
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
								${String.format("%+,d", StockFormatUtil.displayWon(principalReturnAmount))}
							</div>
							<div class="text-xs font-semibold">
								(${StockFormatUtil.signedPct(rate, 1)})
							</div>
						</td>
						<td class="text-right font-mono text-base-content/70">${StockFormatUtil.pct(accountWeightPct.doubleValue(), 1)}</td>
					</tr>
					@if(!holdings.isEmpty())
						<tr id="${detailRowId}" class="hidden bg-base-200/40">
							<td colspan="7" class="p-0">
								<div class="px-3 py-3">
									<div class="text-xs font-semibold text-base-content/60 mb-2">${accountHoldingsDetailLabel}</div>
									<div class="overflow-x-auto rounded-box border border-base-300/60 bg-base-100">
										<table class="table table-xs w-full" aria-label="${accountHoldingsDetailLabel}">
											<thead class="bg-base-200/70">
												<tr>
													<th scope="col">${stockNameLabel}</th>
													<th scope="col" class="text-right hidden sm:table-cell">${quantityLabel}</th>
													<th scope="col" class="text-right hidden lg:table-cell">${averageCostLabel}</th>
													<th scope="col" class="text-right hidden md:table-cell">${buyAmountLabel}</th>
													<th scope="col" class="text-right hidden lg:table-cell">${currentPriceLabel}</th>
													<th scope="col" class="text-right">${evaluationAmountLabel}</th>
													<th scope="col" class="text-right">${evaluationProfitLabel}</th>
													<th scope="col" class="text-right hidden xl:table-cell">${weightLabel}</th>
												</tr>
											</thead>
											<tbody>
												@for(AssetStatusAccountHoldingView holding : holdings)
													<tr>
														<td class="font-semibold"><a href="/stock/item?stockItemId=${holding.stockItemId() != null ? holding.stockItemId().toString() : ""}" class="link link-hover">${holding.stockItemName() != null ? holding.stockItemName() : "-"}</a></td>
														<td class="text-right font-mono hidden sm:table-cell amount-value">${String.format("%,d", holding.holdingQuantity())}</td>
														<td class="text-right font-mono hidden lg:table-cell amount-value">${String.format("%,d", holding.averageBuyPrice() != null ? holding.averageBuyPrice().setScale(0, java.math.RoundingMode.HALF_UP).longValue() : 0)}</td>
														<td class="text-right font-mono hidden md:table-cell amount-value text-base-content/60">${String.format("%,d", StockFormatUtil.displayWon(holding.buyAmount()))}</td>
														<td class="text-right font-mono hidden lg:table-cell amount-value">${String.format("%,d", StockFormatUtil.displayWon(holding.currentPrice()))}</td>
														<td class="text-right font-mono amount-value text-principal">${String.format("%,d", StockFormatUtil.displayWon(holding.evaluationAmount()))}</td>
														<td class="text-right font-mono font-bold amount-value ${holding.evaluationProfit() != null && holding.evaluationProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
															${String.format("%+,d", StockFormatUtil.displayWon(holding.evaluationProfit()))}
															<span class="text-xs">(${StockFormatUtil.signedPct(holding.evaluationProfitRatePct() != null ? holding.evaluationProfitRatePct().doubleValue() : 0.0, 1)})</span>
														</td>
														<td class="text-right hidden xl:table-cell">
															<div class="text-xs text-base-content/70">${accountWeightLabel} ${StockFormatUtil.pct(holding.accountWeightPct() != null ? holding.accountWeightPct().doubleValue() : 0.0, 1)}</div>
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
						<td>${totalLabel}</td>
						<td class="text-right font-mono amount-value text-base-content/60">
							${String.format("%,d", StockFormatUtil.displayWon(totalBuyAmount))}
						</td>
						<td class="text-right font-mono amount-value text-principal">
							${String.format("%,d", StockFormatUtil.displayWon(totalAccountEvaluationAmount))}
						</td>
						<td class="text-right ${totalAccountEvaluationProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
							<div class="font-bold font-mono amount-value">
								${String.format("%+,d", StockFormatUtil.displayWon(totalAccountEvaluationProfit))}
							</div>
							<div class="text-xs font-semibold">
								(${StockFormatUtil.signedPct(totalEvaluationProfitRate, 1)})
							</div>
						</td>
						<td class="text-right font-mono hidden md:table-cell amount-value text-base-content/60">
							${String.format("%,d", StockFormatUtil.displayWon(totalPrincipal))}
						</td>
						<td class="text-right ${totalPrincipalReturnAmount.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
							<div class="font-bold font-mono amount-value">
								${String.format("%+,d", StockFormatUtil.displayWon(totalPrincipalReturnAmount))}
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
			<%-- 배당이 평가손실을 얼마나 메웠나. 표의 세 열(평가 손익 · 누적 배당 · 합산 손익)을 눈으로 빼야 보이던 것을
				 종목마다 막대 둘로 놓는다. 막대는 줄 안에서만 견준다(줄의 손실·배당 중 큰 쪽이 100) - 절대 길이로 두면
				 삼성전자 평가익이 나머지를 1px 로 누른다(실측 2026-09-08: 9.99 억 vs ETF 수백만). --%>
			!{java.util.List<StockDividendCoverageUtil.Row> coverageRows = StockDividendCoverageUtil.of(stockAggregated, stockProfitBreakdown);}
			@if(!coverageRows.isEmpty())
				<div class="mb-4 rounded-2xl border border-base-200 bg-base-100 p-4" data-dividend-coverage>
					<div class="mb-2 flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
						<h3 class="text-sm font-semibold" data-page-section>${coverageTitle}</h3>
						<span class="text-xs text-base-content/60">${coverageDesc}</span>
					</div>
					<div class="grid gap-3">
						@for(StockDividendCoverageUtil.Row coverageRow : coverageRows)
							!{String coverageState = coverageRow.coveragePct() == null ? "none" : (coverageRow.fullyCovered() ? "full" : "partial");}
							<div class="grid items-center gap-x-4 gap-y-1 sm:grid-cols-[minmax(9rem,13rem)_1fr_auto]" data-dividend-coverage-row data-coverage-state="${coverageState}" data-coverage-pct="${coverageRow.coveragePct() != null ? String.valueOf(coverageRow.coveragePct()) : ""}">
								<div class="truncate text-sm font-medium" title="${coverageRow.stockItemName()}">${coverageRow.stockItemName()}</div>
								<div class="flex flex-col gap-1">
									<div class="flex items-center gap-2 text-xs">
										<span class="w-16 shrink-0 text-base-content/60">${evaluationProfitLabel}</span>
										<div class="h-2 flex-1 rounded bg-base-200"><div class="h-2 rounded bg-current ${coverageRow.hasLoss() ? "text-loss" : "text-profit"}" style="width:${coverageRow.hasLoss() ? coverageRow.lossBarPct() : 0}%"></div></div>
										<span class="w-28 shrink-0 text-right amount-value ${coverageRow.hasLoss() ? "text-loss" : "text-profit"}">${coverageRow.evaluationProfit().signum() >= 0 ? "+" : "-"}₩${String.format("%,d", StockFormatUtil.displayWon(coverageRow.evaluationProfit().abs()))}</span>
									</div>
									<div class="flex items-center gap-2 text-xs">
										<span class="w-16 shrink-0 text-base-content/60">${dividendTotalLabel}</span>
										<div class="h-2 flex-1 rounded bg-base-200"><div class="h-2 rounded bg-current text-dividend" style="width:${coverageRow.dividendBarPct()}%"></div></div>
										<span class="w-28 shrink-0 text-right amount-value text-dividend">₩${String.format("%,d", StockFormatUtil.displayWon(coverageRow.dividendTotal()))}</span>
									</div>
								</div>
								<div class="text-xs sm:text-right whitespace-nowrap">
									@if(coverageRow.coveragePct() == null)
										<span class="badge badge-ghost badge-sm">${coverageNoLoss}</span>
									@elseif(coverageRow.fullyCovered())
										<span class="badge badge-success badge-outline badge-sm">${coverageAll}</span>
									@else
										<span class="badge badge-warning badge-outline badge-sm">${java.text.MessageFormat.format(coveragePctPattern, coverageRow.coveragePct())}</span>
									@endif
									<div class="mt-1"><span class="text-base-content/60">${combinedProfitLabel}</span> <span class="amount-value font-semibold ${coverageRow.combinedProfit().signum() >= 0 ? "text-profit" : "text-loss"}">${coverageRow.combinedProfit().signum() >= 0 ? "+" : "-"}₩${String.format("%,d", StockFormatUtil.displayWon(coverageRow.combinedProfit().abs()))}</span></div>
								</div>
							</div>
						@endfor
					</div>
				</div>
			@endif
			<div class="mb-3 flex flex-col gap-3">
				<p class="text-xs text-base-content/60">${stockSelectionHelpLabel}</p>
				<%-- 이 표에는 보유 중인 종목만 나온다. 실현손익·배당을 열로 넣으면서 밝히지 않으면
					 합계를 전체 누적으로 오해한다(실측 2026-09-03: 이미 다 판 34 종목에
					 실현 85,279,840 · 배당 6,582,497 이 더 있다). --%>
				<p class="text-xs text-base-content/60" data-asset-status-held-only-note>${heldOnlyNote}</p>
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
				--sticky-first-bg: color-mix(in oklch, var(--color-info) 10%, var(--color-base-100));
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
			<table class="table table-sm w-full" data-sortable-table data-asset-status-stock-table aria-label="${stockSectionTitle}">
				<thead class="bg-base-200">
					<tr>
						<th scope="col">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="stockName"
									data-sort-type="text">
								<span>${stockNameLabel}</span>
								<span data-sort-indicator class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right hidden lg:table-cell">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="quantity"
									data-sort-type="number">
								<span>${quantityLabel}</span>
								<span data-sort-indicator class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right hidden xl:table-cell">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="averageCost"
									data-sort-type="number">
								<span>${averageCostLabel}</span>
								<span data-sort-indicator class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right hidden sm:table-cell">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="buyAmount"
									data-sort-type="number">
								<span>${buyAmountLabel}</span>
								<span data-sort-indicator class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right hidden lg:table-cell">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="currentPrice"
									data-sort-type="number">
								<span>${currentPriceLabel}</span>
								<span data-sort-indicator class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="evaluationAmount"
									data-sort-type="number">
								<span>${evaluationAmountLabel}</span>
								<span data-sort-indicator class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="evaluationProfit"
									data-sort-type="number">
								<span>${evaluationProfitLabel}</span>
								<span data-sort-indicator class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right hidden xl:table-cell">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="realizedProfit"
									data-sort-type="number">
								<span>${realizedProfitLabel}</span>
								<span data-sort-indicator class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right hidden xl:table-cell">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="dividendTotal"
									data-sort-type="number">
								<span>${dividendTotalLabel}</span>
								<span data-sort-indicator class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right hidden lg:table-cell">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="combinedProfit"
									data-sort-type="number">
								<span>${combinedProfitLabel}</span>
								<span data-sort-indicator class="opacity-60">↕</span>
							</button>
						</th>
						<th scope="col" class="text-right">
							<button type="button"
									class="btn btn-ghost btn-xs h-auto min-h-6 px-1 text-xs text-base-content/60 hover:text-base-content"
									data-sort-key="weight"
									data-sort-type="number">
								<span>${weightLabel}</span>
								<span data-sort-indicator class="opacity-60">↕</span>
							</button>
						</th>
					</tr>
				</thead>
				<tbody>
					@if(stockAggregated == null || stockAggregated.isEmpty())
						<tr>
							<td colspan="11">@template._components.ui.emptyState(message = noHoldingsLabel)</td>
						</tr>
					@else
						@for(TradeProfit item : stockAggregated)
							!{var stockRow = StockAssetStatusUtil.stockRow(item, totalEvaluationAmount);}
							!{BigDecimal evalProfitRate = stockRow.evaluationProfitRatePct();}
							!{BigDecimal totalWeightPct = stockRow.totalWeightPct();}
							!{String stockTagTooltip = stockTagTooltipById.apply(item.stockItemId());}
							!{var breakdown = stockProfitBreakdown != null && stockProfitBreakdown.get(item.stockItemId()) != null
								? stockProfitBreakdown.get(item.stockItemId())
								: new net.luversof.web.gate.stock.util.StockCombinedProfitUtil.Breakdown(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);}
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
								data-combined-profit="${breakdown.combinedProfit().toPlainString()}">
								<td class="font-semibold"><a href="/stock/item?stockItemId=${item.stockItemId() != null ? item.stockItemId().toString() : ""}" class="link link-hover" data-stock-tag-tooltip="${stockTagTooltip}">${item.stockItemName() != null ? item.stockItemName() : "-"}</a></td>
								<td class="text-right font-mono hidden lg:table-cell amount-value">${String.format("%,d", item.holdingQuantity())}</td>
								<td class="text-right font-mono hidden xl:table-cell amount-value">${String.format("%,d", item.averageBuyPrice() != null ? item.averageBuyPrice().setScale(0, java.math.RoundingMode.HALF_UP).longValue() : 0)}</td>
								<td class="text-right font-mono hidden sm:table-cell amount-value text-base-content/60">${String.format("%,d", StockFormatUtil.displayWon(item.totalBuyCost()))}</td>
								<td class="text-right font-mono hidden lg:table-cell amount-value">${String.format("%,d", StockFormatUtil.displayWon(item.currentPrice()))}</td>
								<td class="text-right font-mono font-semibold amount-value text-principal">${String.format("%,d", StockFormatUtil.displayWon(item.evaluationAmount()))}</td>
								<td class="text-right font-mono font-bold ${item.evaluationProfit() != null && item.evaluationProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
									<span class="amount-value">${String.format("%+,d", StockFormatUtil.displayWon(item.evaluationProfit()))}</span>
									<span class="text-xs">(${StockFormatUtil.signedPct(evalProfitRate.doubleValue(), 1)})</span>
								</td>
								<%-- 판 적이 없으면 실현손익은 0 이 아니라 '없음' 이다. 배당도 마찬가지. --%>
								<td class="text-right font-mono hidden xl:table-cell">
									@if(breakdown.realizedProfit().signum() != 0)
										<span class="amount-value ${breakdown.realizedProfit().signum() >= 0 ? "text-profit" : "text-loss"}">${String.format("%+,d", StockFormatUtil.displayWon(breakdown.realizedProfit()))}</span>
									@else
										<span class="text-base-content/30">-</span>
									@endif
								</td>
								<td class="text-right font-mono hidden xl:table-cell">
									@if(breakdown.dividendTotal().signum() != 0)
										<span class="amount-value text-dividend">${String.format("%,d", StockFormatUtil.displayWon(breakdown.dividendTotal()))}</span>
									@else
										<span class="text-base-content/30">-</span>
									@endif
								</td>
								<td class="text-right font-mono font-bold hidden lg:table-cell ${breakdown.combinedProfit().signum() >= 0 ? "text-profit" : "text-loss"}">
									<span class="amount-value">${String.format("%+,d", StockFormatUtil.displayWon(breakdown.combinedProfit()))}</span>
								</td>
								<td class="text-right font-mono text-base-content/70">${StockFormatUtil.pct(totalWeightPct.doubleValue(), 1)}</td>
							</tr>
						@endfor
					@endif
				</tbody>
				<tfoot class="bg-base-200 font-bold">
					!{BigDecimal totalStockBuyAmount = StockAssetStatusUtil.totalBuyCost(stockAggregated);}
					<tr>
						<td>${totalLabel}</td>
						<td class="hidden lg:table-cell"></td>
						<td class="hidden xl:table-cell"></td>
						<td class="text-right font-mono hidden sm:table-cell amount-value text-base-content/60">${String.format("%,d", StockFormatUtil.displayWon(totalStockBuyAmount))}</td>
						<td class="hidden lg:table-cell"></td>
						<td class="text-right font-mono amount-value text-principal">${String.format("%,d", StockFormatUtil.displayWon(totalEvaluationAmount))}</td>
						<td class="text-right font-mono font-bold amount-value ${totalEvaluationProfit != null && totalEvaluationProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">${String.format("%+,d", StockFormatUtil.displayWon(totalEvaluationProfit))}</td>
						<td class="text-right font-mono hidden xl:table-cell ${stockProfitBreakdownTotal.realizedProfit().signum() >= 0 ? "text-profit" : "text-loss"}"><span class="amount-value">${String.format("%+,d", StockFormatUtil.displayWon(stockProfitBreakdownTotal.realizedProfit()))}</span></td>
						<td class="text-right font-mono hidden xl:table-cell"><span class="amount-value text-dividend">${String.format("%,d", StockFormatUtil.displayWon(stockProfitBreakdownTotal.dividendTotal()))}</span></td>
						<td class="text-right font-mono font-bold hidden lg:table-cell ${stockProfitBreakdownTotal.combinedProfit().signum() >= 0 ? "text-profit" : "text-loss"}"><span class="amount-value">${String.format("%+,d", StockFormatUtil.displayWon(stockProfitBreakdownTotal.combinedProfit()))}</span></td>
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
