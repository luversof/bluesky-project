@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.time.ZoneId
@import java.util.List
@import java.util.Map
@import java.util.UUID
@import net.luversof.web.gate.stock.constant.TradeType
@import net.luversof.web.gate.stock.domain.Account
@import net.luversof.web.gate.stock.domain.TradeProfit
@import net.luversof.web.gate.stock.dto.response.TradeResponse
@import net.luversof.web.gate.stock.dto.response.DividendResponse
@import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint
@import java.time.format.DateTimeFormatter
@import java.time.LocalDate
@import java.time.Instant

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param boolean contentReady = false
@param String accountIdParam = ""
@param Account account = null
@param int holdingCount = 0
@param BigDecimal evaluationAmount = java.math.BigDecimal.ZERO
@param BigDecimal totalBuyCost = java.math.BigDecimal.ZERO
@param BigDecimal evaluationProfit = java.math.BigDecimal.ZERO
@param BigDecimal realizedProfit = java.math.BigDecimal.ZERO
@param BigDecimal totalDividend = java.math.BigDecimal.ZERO
@param List<TradeProfit> holdings = java.util.List.of()
@param List<TradeResponse> trades = java.util.List.of()
@param List<DividendResponse> dividends = java.util.List.of()
@param List<TradeProfitTimeSeriesPoint> timeSeries = java.util.List.of()
@param DateTimeFormatter chartFormatter = null
@param Map<UUID, String> stockNameById = java.util.Map.of()
@param LocalDate filterStartLocal = null
@param LocalDate filterEndLocal = null
@param Instant filterStartInstant = null
@param Instant filterEndInstant = null
@param String filterTimeZone = null
@param String filterRangeMode = null

!{
    ZoneId zone = ZoneId.systemDefault();
    String evalColor = evaluationProfit != null && evaluationProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss";
    String realizedColor = realizedProfit != null && realizedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss";
}

@template._layout.stockLayout(
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentStock = @`
		<div class="w-full">
			@if(!contentReady)
				<!-- 셸: 콘텐츠를 전역 기간과 함께 htmx로 로드 (새로고침/이동 시에도 선택 기간 유지) -->
				<div id="accountDetailFragment"
				     hx-get="/stock/account?accountId=${accountIdParam}"
				     hx-trigger="load"
				     hx-include="#globalDateRangeInputs input"
				     hx-select="#accountDetailFragment"
				     hx-swap="outerHTML">
					<div class="flex justify-center p-10"><span class="loading loading-spinner text-primary loading-lg"></span></div>
				</div>
			@else
				<div id="accountDetailFragment" class="space-y-6">
				@if(account == null)
					@template._components.ui.emptyState(message = MessageUtil.getMessage("common.message.no.data"), boxed = true)
				@else
					@template._components.ui.breadcrumbBack(
						homeLabel = MessageUtil.getMessage("layout.menu.main.stock"),
						categoryLabel = MessageUtil.getMessage("stock.account.detail.breadcrumb"),
						backLabel = MessageUtil.getMessage("common.button.back"))

					@template._components.ui.pageHeader(
						title = account.name() != null ? account.name() : "-",
						description = MessageUtil.getMessage("stock.account.detail.holdings.title"))

					@template.stock.htmx.fragments.components.detailDateFilter(
						pickerVar = "accountDetailPicker",
						formId = "accountDetailSearchForm",
						fragmentId = "accountDetailFragment",
						contentUrl = "/stock/account",
						idPrefix = "accountDetail",
						extraName = "accountId",
						extraValue = account.id() != null ? account.id().toString() : "",
						startLocal = filterStartLocal,
						endLocal = filterEndLocal,
						startInstant = filterStartInstant,
						endInstant = filterEndInstant,
						timeZone = filterTimeZone,
						rangeMode = filterRangeMode)

					<!-- 보유/손익 요약 -->
					<div class="grid grid-cols-2 lg:grid-cols-3 gap-3">
						@template._components.ui.statCard(
							label = MessageUtil.getMessage("stock.profit.evaluation.amount"),
							value = String.format("%,d", evaluationAmount != null ? evaluationAmount.longValue() : 0),
							valueTitle = String.format("%,d원", evaluationAmount != null ? evaluationAmount.longValue() : 0),
							tone = "text-principal")
						@template._components.ui.statCard(
							label = MessageUtil.getMessage("stock.item.detail.principal"),
							value = String.format("%,d", totalBuyCost != null ? totalBuyCost.longValue() : 0),
							valueTitle = String.format("%,d원", totalBuyCost != null ? totalBuyCost.longValue() : 0))
						@template._components.ui.statCard(
							label = MessageUtil.getMessage("stock.account.detail.holding.count"),
							value = String.valueOf(holdingCount),
							amount = false)
						@template._components.ui.statCard(
							label = MessageUtil.getMessage("stock.profit.unrealized"),
							value = String.format("%+,d", evaluationProfit != null ? evaluationProfit.longValue() : 0),
							valueTitle = String.format("%,d원", evaluationProfit != null ? evaluationProfit.longValue() : 0),
							tone = evalColor)
						@template._components.ui.statCard(
							label = MessageUtil.getMessage("stock.profit.realized"),
							value = String.format("%+,d", realizedProfit != null ? realizedProfit.longValue() : 0),
							valueTitle = String.format("%,d원", realizedProfit != null ? realizedProfit.longValue() : 0),
							tone = realizedColor)
						@template._components.ui.statCard(
							label = MessageUtil.getMessage("stock.summary.label.cumulative.dividend"),
							value = String.format("%,d", totalDividend != null ? totalDividend.longValue() : 0),
							valueTitle = String.format("%,d원", totalDividend != null ? totalDividend.longValue() : 0),
							tone = "text-dividend")
					</div>

					<!-- 평가액·원가 추이 -->
					@if(timeSeries != null && !timeSeries.isEmpty() && chartFormatter != null)
						<div>
							<h2 class="text-lg font-semibold mb-2 opacity-70">${MessageUtil.getMessage("stock.item.detail.chart.title")}</h2>
							<div class="bg-base-100 rounded-box border border-base-200 p-4">
								<div class="h-64"><canvas id="accountDetailChart" class="amount-chart"></canvas></div>
							</div>
							<p class="text-xs text-base-content/40 mt-1">${MessageUtil.getMessage("stock.account.detail.chart.click.hint")}</p>
						</div>
						<div id="holdings-snapshot-container"></div>
						<script>
						(function() {
							if (typeof window.ensureStockCharts !== 'function') return;
							var series = { labels: [], value: [], cost: [], buyCount: [], dailyRealized: [] };
							@for(TradeProfitTimeSeriesPoint pt : timeSeries)
								series.labels.push("$unsafe{chartFormatter.format(pt.timestamp())}");
								series.value.push($unsafe{pt.totalHoldingsValue() != null ? pt.totalHoldingsValue().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0"});
								series.cost.push($unsafe{pt.totalHoldingsCost() != null ? pt.totalHoldingsCost().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0"});
								series.buyCount.push($unsafe{String.valueOf(pt.buyCount())});
								series.dailyRealized.push($unsafe{pt.dailyRealizedProfit() != null ? pt.dailyRealizedProfit().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0"});
							@endfor
							var __acctId = '${account.id() != null ? account.id().toString() : ""}';
							window.ensureStockCharts(function() {
								if (!window.StockCharts || !window.StockCharts.createHoldingsChart) return;
								var __detailChart = window.StockCharts.createHoldingsChart('accountDetailChart', series, {
									valueLabel: '${MessageUtil.getMessage("stock.item.detail.chart.value")}',
									costLabel: '${MessageUtil.getMessage("stock.item.detail.chart.cost")}',
									profitLabel: '${MessageUtil.getMessage("stock.profit.unrealized")}',
									maxLabel: '${MessageUtil.getMessage("stock.chart.range.max")}',
									minLabel: '${MessageUtil.getMessage("stock.chart.range.min")}'
								}, {
									animate: true,
									showMarkers: true,
									onClick: function(event, elements) {
										if (!elements || !elements.length) return;
										var idx = elements[0].index;
										var date = series.labels[idx];
										if (!date || typeof window.htmx === 'undefined' || !window.htmx.ajax) return;
										window.htmx.ajax('GET', '/stock/htmx/holdings-snapshot?date=' + encodeURIComponent(date) + '&accountId=' + encodeURIComponent(__acctId), {
											target: '#holdings-snapshot-container',
											swap: 'innerHTML'
										});
									}
								});
								setTimeout(function() { try { if (__detailChart) __detailChart.resize(); } catch (e) {} }, 60);
								setTimeout(function() { try { if (__detailChart) __detailChart.resize(); } catch (e) {} }, 250);
							});
						})();
						</script>
					@endif

					<!-- 보유 종목 -->
					<div>
						<h2 class="text-lg font-semibold mb-2 opacity-70">${MessageUtil.getMessage("stock.account.detail.holdings.title")}</h2>
						@if(holdings == null || holdings.isEmpty())
							<div class="bg-base-200 rounded-box p-6 text-center text-sm text-base-content/60">${MessageUtil.getMessage("stock.account.detail.empty.holdings")}</div>
						@else
							@template._components.ui.tableCard(body = @`
								<div class="max-lg:overflow-x-auto">
								<table class="table table-sm w-full table-pin-rows pin-top-nav" data-sortable>
									<thead class="bg-base-200">
										<tr>
											<th scope="col">${MessageUtil.getMessage("stock.account.detail.col.stock")}</th>
											<th scope="col" class="text-right">${MessageUtil.getMessage("common.label.quantity")}</th>
											<th scope="col" class="text-right hidden sm:table-cell">${MessageUtil.getMessage("stock.column.average.cost")}</th>
											<th scope="col" class="text-right">${MessageUtil.getMessage("stock.profit.evaluation.amount")}</th>
											<th scope="col" class="text-right">${MessageUtil.getMessage("stock.profit.unrealized")}</th>
										</tr>
									</thead>
									<tbody>
										@for(TradeProfit holding : holdings)
											!{long evalProfit = holding.evaluationProfitNet() != null ? holding.evaluationProfitNet().longValue() : 0;}
											<tr class="hover">
												<td class="font-semibold"><a href="/stock/item?stockItemId=${holding.stockItemId() != null ? holding.stockItemId().toString() : ""}" class="link link-hover">${holding.stockItemName() != null ? holding.stockItemName() : "-"}</a></td>
												<td class="text-right font-mono amount-value">${String.format("%,d", holding.holdingQuantity())}</td>
												<td class="text-right font-mono amount-value hidden sm:table-cell">${String.format("%,d", holding.averageBuyPrice() != null ? holding.averageBuyPrice().setScale(0, java.math.RoundingMode.HALF_UP).longValue() : 0)}</td>
												<td class="text-right font-mono amount-value">${String.format("%,d", holding.evaluationAmount() != null ? holding.evaluationAmount().longValue() : 0)}</td>
												<td class="text-right font-mono amount-value ${evalProfit >= 0 ? "text-profit" : "text-loss"}">${String.format("%+,d", evalProfit)}</td>
											</tr>
										@endfor
									</tbody>
								</table>
								</div>
							`)
						@endif
					</div>

					<!-- 매매 내역 -->
					<div>
						<h2 class="text-lg font-semibold mb-2 opacity-70">${MessageUtil.getMessage("stock.page.trade.title")}</h2>
						@if(trades == null || trades.isEmpty())
							<div class="bg-base-200 rounded-box p-6 text-center text-sm text-base-content/60">${MessageUtil.getMessage("stock.account.detail.empty.trades")}</div>
						@else
							@template._components.ui.tableCard(body = @`
								<div class="max-lg:overflow-x-auto">
								<table class="table table-sm w-full table-pin-rows pin-top-nav" data-sortable>
									<thead class="bg-base-200">
										<tr>
											<th scope="col">${MessageUtil.getMessage("common.label.date")}</th>
											<th scope="col">${MessageUtil.getMessage("stock.account.detail.col.stock")}</th>
											<th scope="col">${MessageUtil.getMessage("stock.item.detail.col.type")}</th>
											<th scope="col" class="text-right">${MessageUtil.getMessage("common.label.quantity")}</th>
											<th scope="col" class="text-right hidden sm:table-cell">${MessageUtil.getMessage("stock.item.detail.col.price")}</th>
											<th scope="col" class="text-right">${MessageUtil.getMessage("stock.item.detail.col.amount")}</th>
										</tr>
									</thead>
									<tbody>
										@for(TradeResponse trade : trades)
											!{boolean isBuy = trade.type() == TradeType.BUY;}
											!{String tName = trade.stockItemName() != null ? trade.stockItemName() : stockNameById.getOrDefault(trade.stockItemId(), "-");}
											<tr class="hover">
												<td class="tabular-nums">${trade.tradeDate() != null ? trade.tradeDate().atZone(zone).toLocalDate().toString() : "-"}</td>
												<td class="font-semibold"><a href="/stock/item?stockItemId=${trade.stockItemId() != null ? trade.stockItemId().toString() : ""}" class="link link-hover">${tName}</a></td>
												<td><span class="badge badge-sm ${isBuy ? "badge-error badge-outline" : "badge-info badge-outline"}">${isBuy ? MessageUtil.getMessage("stock.trade.label.buy") : MessageUtil.getMessage("stock.trade.label.sell")}</span></td>
												<td class="text-right font-mono amount-value">${String.format("%,d", trade.quantity())}</td>
												<td class="text-right font-mono amount-value hidden sm:table-cell">${String.format("%,d", trade.price() != null ? trade.price().longValue() : 0)}</td>
												<td class="text-right font-mono amount-value">${String.format("%,d", trade.amount() != null ? trade.amount().longValue() : 0)}</td>
											</tr>
										@endfor
									</tbody>
								</table>
								</div>
							`)
						@endif
					</div>

					<!-- 배당 내역 -->
					<div>
						<h2 class="text-lg font-semibold mb-2 opacity-70">${MessageUtil.getMessage("stock.page.dividend.title")}</h2>
						@if(dividends == null || dividends.isEmpty())
							<div class="bg-base-200 rounded-box p-6 text-center text-sm text-base-content/60">${MessageUtil.getMessage("stock.account.detail.empty.dividends")}</div>
						@else
							@template._components.ui.tableCard(body = @`
								<div class="max-lg:overflow-x-auto">
								<table class="table table-sm w-full table-pin-rows pin-top-nav" data-sortable>
									<thead class="bg-base-200">
										<tr>
											<th scope="col">${MessageUtil.getMessage("common.label.date")}</th>
											<th scope="col">${MessageUtil.getMessage("stock.account.detail.col.stock")}</th>
											<th scope="col" class="text-right hidden sm:table-cell">${MessageUtil.getMessage("stock.item.detail.div.gross")}</th>
											<th scope="col" class="text-right hidden sm:table-cell">${MessageUtil.getMessage("stock.item.detail.div.tax")}</th>
											<th scope="col" class="text-right">${MessageUtil.getMessage("stock.item.detail.div.net")}</th>
										</tr>
									</thead>
									<tbody>
										@for(DividendResponse dividend : dividends)
											!{String dName = dividend.stockItemName() != null ? dividend.stockItemName() : stockNameById.getOrDefault(dividend.stockItemId(), "-");}
											<tr class="hover">
												<td class="tabular-nums">${dividend.payDate() != null ? dividend.payDate().atZone(zone).toLocalDate().toString() : "-"}</td>
												<td class="font-semibold"><a href="/stock/item?stockItemId=${dividend.stockItemId() != null ? dividend.stockItemId().toString() : ""}" class="link link-hover">${dName}</a></td>
												<td class="text-right font-mono amount-value hidden sm:table-cell opacity-70">${String.format("%,d", dividend.grossAmount() != null ? dividend.grossAmount().longValue() : 0)}</td>
												<td class="text-right font-mono amount-value hidden sm:table-cell opacity-60">${String.format("%,d", dividend.tax() != null ? dividend.tax().longValue() : 0)}</td>
												<td class="text-right font-mono font-bold amount-value text-dividend">${String.format("%,d", dividend.netAmount() != null ? dividend.netAmount().longValue() : 0)}</td>
											</tr>
										@endfor
									</tbody>
								</table>
								</div>
							`)
						@endif
					</div>
				@endif
				</div>
			@endif
		</div>
	`
)
