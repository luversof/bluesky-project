@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.time.ZoneId
@import java.util.List
@import net.luversof.web.gate.stock.constant.TradeType
@import net.luversof.web.gate.stock.domain.StockItem
@import net.luversof.web.gate.stock.dto.response.TradeResponse
@import net.luversof.web.gate.stock.dto.response.DividendResponse
@import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint
@import java.time.format.DateTimeFormatter

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param StockItem stockItem = null
@param int holdingQuantity = 0
@param BigDecimal averageBuyPrice = java.math.BigDecimal.ZERO
@param BigDecimal currentPrice = java.math.BigDecimal.ZERO
@param BigDecimal evaluationAmount = java.math.BigDecimal.ZERO
@param BigDecimal evaluationProfit = java.math.BigDecimal.ZERO
@param BigDecimal realizedProfit = java.math.BigDecimal.ZERO
@param BigDecimal totalBuyCost = java.math.BigDecimal.ZERO
@param BigDecimal totalDividend = java.math.BigDecimal.ZERO
@param List<TradeResponse> trades = java.util.List.of()
@param List<DividendResponse> dividends = java.util.List.of()
@param List<TradeProfitTimeSeriesPoint> timeSeries = java.util.List.of()
@param DateTimeFormatter chartFormatter = null

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
		<div class="container mx-auto p-4 space-y-6">
			@if(stockItem == null)
				<div class="bg-base-200 rounded-box p-10 text-center text-base-content/60">${MessageUtil.getMessage("common.message.no.data")}</div>
			@else
				@template._components.ui.pageHeader(
					title = stockItem.name() != null ? stockItem.name() : stockItem.symbol(),
					description = (stockItem.symbol() != null ? stockItem.symbol() : "") + (stockItem.market() != null && !stockItem.market().isEmpty() ? " · " + stockItem.market() : ""))

				@if(stockItem.tags() != null && !stockItem.tags().isEmpty())
					<div class="flex flex-wrap gap-1.5 -mt-2">
						@for(String tag : stockItem.tags())
							<span class="badge badge-sm badge-ghost">${tag}</span>
						@endfor
					</div>
				@endif

				<!-- 보유/손익 요약 -->
				<div class="grid grid-cols-2 lg:grid-cols-4 gap-3">
					@template._components.ui.statCard(
						label = MessageUtil.getMessage("common.label.quantity"),
						value = String.format("%,d", holdingQuantity) + MessageUtil.getMessage("stock.activity.label.shares"),
						amount = false)
					@template._components.ui.statCard(
						label = MessageUtil.getMessage("stock.column.average.cost"),
						value = String.format("%,d", averageBuyPrice != null ? averageBuyPrice.longValue() : 0),
						valueTitle = String.format("%,d원", averageBuyPrice != null ? averageBuyPrice.longValue() : 0))
					@template._components.ui.statCard(
						label = MessageUtil.getMessage("stock.column.current.price"),
						value = String.format("%,d", currentPrice != null ? currentPrice.longValue() : 0),
						valueTitle = String.format("%,d원", currentPrice != null ? currentPrice.longValue() : 0))
					@template._components.ui.statCard(
						label = MessageUtil.getMessage("stock.profit.evaluation.amount"),
						value = String.format("%,d", evaluationAmount != null ? evaluationAmount.longValue() : 0),
						valueTitle = String.format("%,d원", evaluationAmount != null ? evaluationAmount.longValue() : 0),
						tone = "text-principal")
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
						label = MessageUtil.getMessage("stock.item.detail.principal"),
						value = String.format("%,d", totalBuyCost != null ? totalBuyCost.longValue() : 0),
						valueTitle = String.format("%,d원", totalBuyCost != null ? totalBuyCost.longValue() : 0))
					@template._components.ui.statCard(
						label = MessageUtil.getMessage("stock.summary.label.cumulative.dividend"),
						value = String.format("%,d", totalDividend != null ? totalDividend.longValue() : 0),
						valueTitle = String.format("%,d원", totalDividend != null ? totalDividend.longValue() : 0),
						tone = "text-dividend")
				</div>

				<!-- 보유 평가액·원가 추이 -->
				@if(timeSeries != null && !timeSeries.isEmpty() && chartFormatter != null)
					<div>
						<h2 class="text-lg font-semibold mb-2 opacity-70">${MessageUtil.getMessage("stock.item.detail.chart.title")}</h2>
						<div class="bg-base-100 rounded-box border border-base-200 p-4">
							<div class="h-64"><canvas id="stockDetailChart"></canvas></div>
						</div>
					</div>
					<script>
					(function() {
						if (typeof Chart === 'undefined') return;
						var ctx = document.getElementById('stockDetailChart');
						if (!ctx) return;
						var labels = [];
						var valueData = [];
						var costData = [];
						@for(TradeProfitTimeSeriesPoint pt : timeSeries)
							labels.push("$unsafe{chartFormatter.format(pt.timestamp())}");
							valueData.push($unsafe{pt.totalHoldingsValue() != null ? pt.totalHoldingsValue().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0"});
							costData.push($unsafe{pt.totalHoldingsCost() != null ? pt.totalHoldingsCost().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0"});
						@endfor
						var fmt = function(v) {
							return window.StockCharts ? window.StockCharts.formatCompactNumber(v) : new Intl.NumberFormat().format(v);
						};
						new Chart(ctx, {
							type: 'line',
							data: {
								labels: labels,
								datasets: [
									{ label: '${MessageUtil.getMessage("stock.item.detail.chart.value")}', data: valueData, borderColor: '#3e6cdf', backgroundColor: 'rgba(62,108,223,0.12)', fill: true, tension: 0.2, pointRadius: 0, borderWidth: 2 },
									{ label: '${MessageUtil.getMessage("stock.item.detail.chart.cost")}', data: costData, borderColor: '#9ca3af', borderDash: [5, 5], fill: false, tension: 0.2, pointRadius: 0, borderWidth: 1.5 }
								]
							},
							options: {
								responsive: true,
								maintainAspectRatio: false,
								interaction: { mode: 'index', intersect: false },
								plugins: { legend: { display: true, position: 'bottom' } },
								scales: { y: { ticks: { callback: function(v) { return fmt(v); } } } }
							}
						});
					})();
					</script>
				@endif

				<!-- 매매 내역 -->
				<div>
					<h2 class="text-lg font-semibold mb-2 opacity-70">${MessageUtil.getMessage("stock.page.trade.title")}</h2>
					@if(trades == null || trades.isEmpty())
						<div class="bg-base-200 rounded-box p-6 text-center text-sm text-base-content/60">${MessageUtil.getMessage("stock.item.detail.empty.trades")}</div>
					@else
						@template._components.ui.tableCard(body = @`
							<table class="table table-sm w-full">
								<thead class="bg-base-200">
									<tr>
										<th>${MessageUtil.getMessage("common.label.date")}</th>
										<th>${MessageUtil.getMessage("stock.item.detail.col.type")}</th>
										<th class="text-right">${MessageUtil.getMessage("common.label.quantity")}</th>
										<th class="text-right">${MessageUtil.getMessage("stock.item.detail.col.price")}</th>
										<th class="text-right">${MessageUtil.getMessage("stock.item.detail.col.amount")}</th>
										<th class="text-right hidden sm:table-cell">${MessageUtil.getMessage("stock.item.detail.col.fees")}</th>
										<th class="text-right">${MessageUtil.getMessage("stock.profit.realized")}</th>
									</tr>
								</thead>
								<tbody>
									@for(TradeResponse trade : trades)
										!{boolean isBuy = trade.type() == TradeType.BUY;}
										!{long fees = (trade.fee() != null ? trade.fee().longValue() : 0) + (trade.tax() != null ? trade.tax().longValue() : 0);}
										<tr class="hover">
											<td class="tabular-nums">${trade.tradeDate() != null ? trade.tradeDate().atZone(zone).toLocalDate().toString() : "-"}</td>
											<td><span class="badge badge-sm ${isBuy ? "badge-error badge-outline" : "badge-info badge-outline"}">${isBuy ? MessageUtil.getMessage("stock.trade.label.buy") : MessageUtil.getMessage("stock.trade.label.sell")}</span></td>
											<td class="text-right font-mono amount-value">${String.format("%,d", trade.quantity())}</td>
											<td class="text-right font-mono amount-value">${String.format("%,d", trade.price() != null ? trade.price().longValue() : 0)}</td>
											<td class="text-right font-mono amount-value">${String.format("%,d", trade.amount() != null ? trade.amount().longValue() : 0)}</td>
											<td class="text-right font-mono amount-value hidden sm:table-cell opacity-60">${String.format("%,d", fees)}</td>
											<td class="text-right font-mono amount-value ${!isBuy && trade.realizedProfit() != null && trade.realizedProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : (!isBuy && trade.realizedProfit() != null ? "text-loss" : "opacity-40")}">${!isBuy && trade.realizedProfit() != null ? String.format("%+,d", trade.realizedProfit().longValue()) : "-"}</td>
										</tr>
									@endfor
								</tbody>
							</table>
						`)
					@endif
				</div>

				<!-- 배당 내역 -->
				<div>
					<h2 class="text-lg font-semibold mb-2 opacity-70">${MessageUtil.getMessage("stock.page.dividend.title")}</h2>
					@if(dividends == null || dividends.isEmpty())
						<div class="bg-base-200 rounded-box p-6 text-center text-sm text-base-content/60">${MessageUtil.getMessage("stock.item.detail.empty.dividends")}</div>
					@else
						@template._components.ui.tableCard(body = @`
							<table class="table table-sm w-full">
								<thead class="bg-base-200">
									<tr>
										<th>${MessageUtil.getMessage("common.label.date")}</th>
										<th class="text-right hidden sm:table-cell">${MessageUtil.getMessage("stock.item.detail.div.gross")}</th>
										<th class="text-right hidden sm:table-cell">${MessageUtil.getMessage("stock.item.detail.div.tax")}</th>
										<th class="text-right">${MessageUtil.getMessage("stock.item.detail.div.net")}</th>
									</tr>
								</thead>
								<tbody>
									@for(DividendResponse dividend : dividends)
										<tr class="hover">
											<td class="tabular-nums">${dividend.payDate() != null ? dividend.payDate().atZone(zone).toLocalDate().toString() : "-"}</td>
											<td class="text-right font-mono amount-value hidden sm:table-cell opacity-70">${String.format("%,d", dividend.grossAmount() != null ? dividend.grossAmount().longValue() : 0)}</td>
											<td class="text-right font-mono amount-value hidden sm:table-cell opacity-60">${String.format("%,d", dividend.tax() != null ? dividend.tax().longValue() : 0)}</td>
											<td class="text-right font-mono font-bold amount-value text-dividend">${String.format("%,d", dividend.netAmount() != null ? dividend.netAmount().longValue() : 0)}</td>
										</tr>
									@endfor
								</tbody>
							</table>
						`)
					@endif
				</div>
			@endif
		</div>
	`
)
