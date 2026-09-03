@import net.luversof.web.gate.stock.util.StockFormatUtil
@import net.luversof.web.gate.util.CspNonceHolder
@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.time.ZoneId
@import java.util.List
@import net.luversof.web.gate.stock.constant.TradeType
@import net.luversof.web.gate.stock.domain.StockItem
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
@param String stockItemIdParam = ""
@param StockItem stockItem = null
@param int holdingQuantity = 0
@param BigDecimal averageBuyPrice = java.math.BigDecimal.ZERO
@param BigDecimal currentPrice = java.math.BigDecimal.ZERO
@param java.time.LocalDate priceBasisDate = null
@param BigDecimal evaluationAmount = java.math.BigDecimal.ZERO
@param BigDecimal evaluationProfit = java.math.BigDecimal.ZERO
@param BigDecimal realizedProfit = java.math.BigDecimal.ZERO
@param BigDecimal totalBuyCost = java.math.BigDecimal.ZERO
@param BigDecimal totalDividend = java.math.BigDecimal.ZERO
@param List<TradeResponse> trades = java.util.List.of()
@param List<DividendResponse> dividends = java.util.List.of()
@param List<TradeProfitTimeSeriesPoint> timeSeries = java.util.List.of()
@param DateTimeFormatter chartFormatter = null
@param LocalDate filterStartLocal = null
@param LocalDate filterEndLocal = null
@param Instant filterStartInstant = null
@param Instant filterEndInstant = null
@param String filterTimeZone = null
@param String filterRangeMode = null
@param LocalDate coveredStartLocal = null
@param LocalDate coveredEndLocal = null
@param List<TradeProfit> accountHoldings = java.util.List.of()
@param List<net.luversof.web.gate.stock.dto.response.TradeProfitPeriodSummary> periodBreakdown = java.util.List.of()
@param List<net.luversof.web.gate.stock.domain.DetailNavEntry> stockNavEntries = java.util.List.of()
@param List<net.luversof.web.gate.stock.dto.response.StockPriceHistoryPoint> priceHistory = java.util.List.of()
@param BigDecimal periodUnrealizedDelta = null
@param Double periodProfitRatePct = null

!{
    // 아래 표의 날짜 칸은 이 페이지가 차트/필터에 넘기는 존과 같아야 한다.
    // 예전에는 서버 존이라 브라우저 존이 다르면 한 화면 안에서 하루가 어긋났다.
    ZoneId zone = net.luversof.web.gate.stock.util.StockZoneUtil.resolve(filterTimeZone);
    String evalColor = evaluationProfit != null && evaluationProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss";
    String realizedColor = realizedProfit != null && realizedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss";
    BigDecimal evalCostBasis = (evaluationAmount != null ? evaluationAmount : BigDecimal.ZERO).subtract(evaluationProfit != null ? evaluationProfit : BigDecimal.ZERO);
    double evalProfitRatePct = evalCostBasis.signum() != 0 && evaluationProfit != null ? evaluationProfit.doubleValue() / evalCostBasis.doubleValue() * 100.0 : 0.0;

    // 합산 손익 = <b>기간</b> 평가 변동 + 실현손익 + 배당.
    //
    // 세 값이 카드로 흩어져 있어서 "이 종목에서 결국 얼마를 벌었나" 를 사람이 더해야 알 수 있었다.
    //
    // 평가 몫은 현재 시점 평가손익이 아니라 <b>기간 평가 변동</b>(기말 - 기초)이다. 현재 시점 값을 더하면
    // 아래 '기간별 손익' 표의 합과 맞지 않는다 - 실측 2026-09-01 삼성전자 '올해' 2.9 억 차이, '최근 1년'
    // 은 부호까지 반대였다. 같은 화면의 두 숫자가 안 맞으면 어느 쪽이 맞는지 알 수 없다.
    //
    // 이렇게 더한 값은 요약의 periodProfit 과 1 원 오차 없이 같다(실측 두 기간 모두 차이 0).
    BigDecimal unrealizedDelta = periodUnrealizedDelta != null ? periodUnrealizedDelta : BigDecimal.ZERO;
    BigDecimal combinedProfit = unrealizedDelta
        .add(realizedProfit != null ? realizedProfit : BigDecimal.ZERO)
        .add(totalDividend != null ? totalDividend : BigDecimal.ZERO);
    boolean combinedPositive = combinedProfit.signum() >= 0;
    String deltaColor = unrealizedDelta.signum() >= 0 ? "text-profit" : "text-loss";
    // 비율은 자산 성장 화면과 같은 정의(기초 평가액 + 기간 중 순유입 원금 대비)를 그대로 받아 쓴다.
    // 화면마다 다른 분모를 쓰면 같은 이름이 다른 수를 뜻하게 된다.
    boolean combinedRateAvailable = periodProfitRatePct != null;
    double combinedRatePct = combinedRateAvailable ? periodProfitRatePct : 0.0;
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
				<div id="stockItemDetailFragment"
				     hx-get="/stock/item?stockItemId=${stockItemIdParam}"
				     hx-trigger="load"
				     hx-include="#globalDateRangeInputs input"
				     hx-select="#stockItemDetailFragment"
				     hx-swap="outerHTML">
					<div class="flex justify-center p-10"><span class="loading loading-spinner text-primary loading-lg"></span></div>
				</div>
			@else
				<div id="stockItemDetailFragment" class="space-y-6">
				@if(stockItem == null)
					@template._components.ui.emptyState(message = MessageUtil.getMessage("common.message.no.data"), boxed = true)
				@else
					@template._components.ui.breadcrumbBack(
						homeLabel = MessageUtil.getMessage("layout.menu.main.stock"),
						categoryLabel = MessageUtil.getMessage("stock.item.detail.breadcrumb"),
						backLabel = MessageUtil.getMessage("common.button.back"))

					<%-- 제목 옆 전환기 - 다른 종목을 보려고 뒤로 갔다가 목록에서 다시 찾을 필요가 없게 한다. --%>
					@template._components.ui.pageHeader(
						title = stockItem.name() != null ? stockItem.name() : stockItem.symbol(),
						description = (stockItem.symbol() != null ? stockItem.symbol() : "") + (stockItem.market() != null && !stockItem.market().isEmpty() ? " · " + stockItem.market() : ""),
						actions = @`@template.stock.htmx.fragments.detailNavSwitcher(entries = stockNavEntries, switchLabel = MessageUtil.getMessage("stock.detail.switch.stock"))`)

					@if(stockItem.tags() != null && !stockItem.tags().isEmpty())
						<div class="flex flex-wrap gap-1.5 -mt-2">
							@for(String tag : stockItem.tags())
								<span class="badge badge-sm badge-ghost">${tag}</span>
							@endfor
						</div>
					@endif

					@template.stock.htmx.fragments.components.detailDateFilter(
						pickerVar = "stockDetailPicker",
						formId = "stockDetailSearchForm",
						fragmentId = "stockItemDetailFragment",
						contentUrl = "/stock/item",
						idPrefix = "stockDetail",
						extraName = "stockItemId",
						extraValue = stockItem.id() != null ? stockItem.id().toString() : "",
						startLocal = filterStartLocal,
						endLocal = filterEndLocal,
						startInstant = filterStartInstant,
						endInstant = filterEndInstant,
						timeZone = filterTimeZone,
						rangeMode = filterRangeMode,
						coveredStartDate = coveredStartLocal,
						coveredEndDate = coveredEndLocal)

					<%-- "현재가"·평가액은 마지막으로 수집된 종가 기준이라, 오늘 시세가 아직 없으면 며칠 전 값일 수 있다.
					     어느 날 기준인지 밝혀 실시간 시세로 오해하지 않게 한다(자산현황·포트폴리오와 같은 표기). --%>
					@if(priceBasisDate != null)
						<p class="mb-2 text-xs text-base-content/60">${MessageUtil.getMessage("stock.asset.status.price.basis").replace("{0}", priceBasisDate.toString())}</p>
					@endif
					<%-- 합산 손익 - 평가손익·실현손익·배당이 카드로 흩어져 있어 사람이 더해야 알 수 있었다.
					     아래 격자의 세 값을 그대로 더한 것이라, 화면에서 눈으로 검산할 수 있게 셋을 함께 적는다. --%>
					<div class="mb-3 rounded-2xl border border-base-300/60 bg-base-100/80 px-4 py-3">
						<div class="text-sm font-semibold text-base-content/70">${MessageUtil.getMessage("stock.item.detail.combined.profit")}</div>
						<div class="mt-1 flex flex-wrap items-baseline gap-x-3 gap-y-1">
							<span class="text-3xl font-bold leading-none amount-value ${combinedPositive ? "text-profit" : "text-loss"}"
								  title="${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(combinedProfit))}">
								${String.format("%+,d", StockFormatUtil.displayWon(combinedProfit))}
							</span>
							@if(combinedRateAvailable)
								<span class="text-lg font-bold ${combinedPositive ? "text-profit" : "text-loss"}">
									${String.format("%+.2f%%", combinedRatePct)}
								</span>
								<span class="text-[11px] text-base-content/50">${MessageUtil.getMessage("stock.item.detail.combined.basis")}</span>
							@endif
						</div>
						<%-- 셋을 그대로 적어 화면에서 더해 볼 수 있게 한다. --%>
						<p class="mt-1 text-[11px] leading-4 flex flex-wrap items-baseline gap-x-1.5">
							<span class="text-base-content/60">${MessageUtil.getMessage("stock.item.detail.combined.unrealized.delta")}</span>
							<span class="amount-value ${deltaColor}">${String.format("%+,d", StockFormatUtil.displayWon(unrealizedDelta))}</span>
							<span class="text-base-content/35">+</span>
							<span class="text-base-content/60">${MessageUtil.getMessage("stock.profit.realized")}</span>
							<span class="amount-value ${realizedColor}">${String.format("%+,d", StockFormatUtil.displayWon(realizedProfit))}</span>
							<span class="text-base-content/35">+</span>
							<span class="text-base-content/60">${MessageUtil.getMessage("stock.summary.label.cumulative.dividend")}</span>
							<span class="amount-value text-dividend">${String.format("%+,d", StockFormatUtil.displayWon(totalDividend))}</span>
						</p>
						<p class="mt-0.5 text-[11px] leading-4 text-base-content/35">${MessageUtil.getMessage("stock.item.detail.combined.note")}</p>
					</div>

					<!-- 보유/손익 요약 -->
					<div class="grid grid-cols-2 lg:grid-cols-4 gap-3">
						@template._components.ui.statCard(
							label = MessageUtil.getMessage("common.label.quantity"),
							value = String.format("%,d", holdingQuantity) + MessageUtil.getMessage("stock.activity.label.shares"),
							amount = false)
						@template._components.ui.statCard(
							label = MessageUtil.getMessage("stock.column.average.cost"),
							value = String.format("%,d", StockFormatUtil.displayWon(averageBuyPrice)),
							valueTitle = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(averageBuyPrice)))
						@template._components.ui.statCard(
							label = MessageUtil.getMessage("stock.column.current.price"),
							value = String.format("%,d", StockFormatUtil.displayWon(currentPrice)),
							valueTitle = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(currentPrice)))
						@template._components.ui.statCard(
							label = MessageUtil.getMessage("stock.profit.evaluation.amount"),
							value = String.format("%,d", StockFormatUtil.displayWon(evaluationAmount)),
							valueTitle = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(evaluationAmount)),
							tone = "text-principal")
						@template._components.ui.statCard(
							label = MessageUtil.getMessage("stock.profit.unrealized"),
							value = String.format("%+,d", StockFormatUtil.displayWon(evaluationProfit)),
							valueTitle = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(evaluationProfit)),
							sub = String.format("%+.1f%%", evalProfitRatePct),
							tone = evalColor)
						@template._components.ui.statCard(
							label = MessageUtil.getMessage("stock.profit.realized"),
							value = String.format("%+,d", StockFormatUtil.displayWon(realizedProfit)),
							valueTitle = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(realizedProfit)),
							tone = realizedColor)
						@template._components.ui.statCard(
							label = MessageUtil.getMessage("stock.item.detail.principal"),
							value = String.format("%,d", StockFormatUtil.displayWon(totalBuyCost)),
							valueTitle = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(totalBuyCost)))
						@template._components.ui.statCard(
							label = MessageUtil.getMessage("stock.summary.label.cumulative.dividend"),
							value = String.format("%,d", StockFormatUtil.displayWon(totalDividend)),
							valueTitle = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(totalDividend)),
							tone = "text-dividend")
					</div>

					<%-- 기간별 손익 - 고른 기간을 달(또는 해)로 쪼개 언제 벌고 언제 잃었는지 보여준다.
					     위의 합산 손익과 같은 쪼갬(평가 변동 / 실현+배당)을 써서 두 자리가 같은 뜻을 말한다. --%>
					@template.stock.htmx.fragments.periodBreakdownTable(periodBreakdown = periodBreakdown)

					<%-- 주가 추이 - 보유 평가액 추이만으로는 <b>주가 자체</b>를 볼 수 없다. 평가액은 수량이 바뀌면 같이
					     움직이므로, 산 뒤로 주가가 어떻게 됐는지는 그 선에서 읽어낼 수 없다.
					     점선은 <b>지금</b> 평균단가라 과거 구간에도 같은 값이 그어진다(평단은 매매마다 달라졌다).
					     그래도 "지금 내 단가가 이 구간 어디쯤인지" 를 한눈에 보여주므로 함께 긋고, 이름으로 밝힌다. --%>
					@if(priceHistory != null && !priceHistory.isEmpty())
						<div>
							<h2 class="text-lg font-semibold mb-2 opacity-70">${MessageUtil.getMessage("stock.item.detail.price.chart.title")}</h2>
							<div class="bg-base-100 rounded-box border border-base-200 p-4">
								<div class="h-64"><canvas id="stockPriceChart" class="amount-chart"></canvas></div>
							</div>
							<p class="text-xs text-base-content/50 mt-1">${MessageUtil.getMessage("stock.item.detail.price.chart.desc")}</p>
						</div>
						<script nonce="${CspNonceHolder.getNonce()}">
						(function() {
							if (typeof window.ensureStockCharts !== 'function') return;
							var avg = $unsafe{averageBuyPrice != null ? averageBuyPrice.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0"};
							var series = { labels: [], value: [], cost: [], buyCount: [], dailyRealized: [] };
							@for(net.luversof.web.gate.stock.dto.response.StockPriceHistoryPoint pt : priceHistory)
								series.labels.push("$unsafe{pt.tradeDate() != null ? pt.tradeDate().toString() : ""}");
								series.value.push($unsafe{pt.closePrice() != null ? pt.closePrice().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() : "0"});
								series.cost.push(avg);
							@endfor
							window.ensureStockCharts(function() {
								if (!window.StockCharts || !window.StockCharts.createHoldingsChart) return;
								var __priceChart = window.StockCharts.createHoldingsChart('stockPriceChart', series, {
									valueLabel: '${MessageUtil.getMessage("stock.item.detail.price.chart.close")}',
									costLabel: '${MessageUtil.getMessage("stock.item.detail.price.chart.average")}',
									maxLabel: '${MessageUtil.getMessage("stock.chart.range.max")}',
									minLabel: '${MessageUtil.getMessage("stock.chart.range.min")}'
								}, { animate: true, showMarkers: false });
								setTimeout(function() { try { if (__priceChart) __priceChart.resize(); } catch (e) {} }, 60);
							});
						})();
						</script>
					@endif

					<!-- 보유 평가액·원가 추이 -->
					@if(timeSeries != null && !timeSeries.isEmpty() && chartFormatter != null)
						<div>
							<h2 class="text-lg font-semibold mb-2 opacity-70">${MessageUtil.getMessage("stock.item.detail.chart.title")}</h2>
							<div class="bg-base-100 rounded-box border border-base-200 p-4">
								<div class="h-64"><canvas id="stockDetailChart" class="amount-chart"></canvas></div>
							</div>
						</div>
						<script nonce="${CspNonceHolder.getNonce()}">
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
							window.ensureStockCharts(function() {
								if (!window.StockCharts || !window.StockCharts.createHoldingsChart) return;
								var __detailChart = window.StockCharts.createHoldingsChart('stockDetailChart', series, {
									valueLabel: '${MessageUtil.getMessage("stock.item.detail.chart.value")}',
									costLabel: '${MessageUtil.getMessage("stock.item.detail.chart.cost")}',
									profitLabel: '${MessageUtil.getMessage("stock.profit.unrealized")}',
									maxLabel: '${MessageUtil.getMessage("stock.chart.range.max")}',
									minLabel: '${MessageUtil.getMessage("stock.chart.range.min")}'
								}, { animate: true, showMarkers: true });
								setTimeout(function() { try { if (__detailChart) __detailChart.resize(); } catch (e) {} }, 60);
								setTimeout(function() { try { if (__detailChart) __detailChart.resize(); } catch (e) {} }, 250);
							});
						})();
						</script>
					@endif

					<!-- 계좌별 보유 현황 -->
					<div>
						<h2 class="text-lg font-semibold mb-2 opacity-70">${MessageUtil.getMessage("stock.item.detail.account.holdings.title")}</h2>
						@if(accountHoldings == null || accountHoldings.isEmpty())
							<div class="bg-base-200 rounded-box p-6 text-center text-sm text-base-content/60">${MessageUtil.getMessage("common.message.no.data")}</div>
						@else
							@template._components.ui.tableCard(body = @`
								<div class="overflow-x-auto">
								<table class="table table-sm w-full table-pin-rows pin-top-nav" data-sortable>
									<thead class="bg-base-200">
										<tr>
											<th scope="col">${MessageUtil.getMessage("common.label.account")}</th>
											<th scope="col" class="text-right">${MessageUtil.getMessage("common.label.quantity")}</th>
											<th scope="col" class="text-right">${MessageUtil.getMessage("stock.profit.evaluation.amount")}</th>
											<th scope="col" class="text-right">${MessageUtil.getMessage("stock.profit.unrealized")}</th>
											<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analytics.account.holdings.weight")}</th>
										</tr>
									</thead>
									<tbody>
										@for(TradeProfit holding : accountHoldings)
											!{long acctEvalProfit = holding.evaluationProfit() != null ? holding.evaluationProfit().longValue() : 0;}
											!{double acctWeightPct = evaluationAmount != null && evaluationAmount.signum() != 0 && holding.evaluationAmount() != null ? holding.evaluationAmount().doubleValue() / evaluationAmount.doubleValue() * 100 : 0.0;}
											!{BigDecimal acctCostBasis = (holding.evaluationAmount() != null ? holding.evaluationAmount() : BigDecimal.ZERO).subtract(holding.evaluationProfit() != null ? holding.evaluationProfit() : BigDecimal.ZERO);}
											!{double acctEvalRatePct = acctCostBasis.signum() != 0 && holding.evaluationProfit() != null ? holding.evaluationProfit().doubleValue() / acctCostBasis.doubleValue() * 100.0 : 0.0;}
											<tr>
												<%-- 계좌 상세로 잇는다. 이 방향 링크만 없어서 계좌 → 종목은 되는데 종목 → 계좌는 뒤로 가야 했다. --%>
												<td class="font-semibold">
													@if(holding.accountId() != null)
														<a href="/stock/account?accountId=${holding.accountId().toString()}" class="link link-hover">${holding.accountName() != null ? holding.accountName() : "-"}</a>
													@else
														${holding.accountName() != null ? holding.accountName() : "-"}
													@endif
												</td>
												<td class="text-right font-mono amount-value">${String.format("%,d", holding.holdingQuantity())}</td>
												<td class="text-right font-mono amount-value">${String.format("%,d", StockFormatUtil.displayWon(holding.evaluationAmount()))}</td>
												<td class="text-right font-mono amount-value ${acctEvalProfit >= 0 ? "text-profit" : "text-loss"}">${String.format("%+,d", acctEvalProfit)} <span class="text-xs">(${String.format("%+.1f%%", acctEvalRatePct)})</span></td>
												<td class="text-right font-mono text-base-content/70">${String.format("%.1f%%", acctWeightPct)}</td>
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
							<div class="bg-base-200 rounded-box p-6 text-center text-sm text-base-content/60">${MessageUtil.getMessage("stock.item.detail.empty.trades")}</div>
						@else
							@template._components.ui.tableCard(body = @`
								<div class="overflow-x-auto">
								<table class="table table-sm w-full table-pin-rows pin-top-nav" data-sortable>
									<thead class="bg-base-200">
										<tr>
											<th scope="col">${MessageUtil.getMessage("common.label.date")}</th>
											<th scope="col">${MessageUtil.getMessage("stock.item.detail.col.type")}</th>
											<th scope="col" class="text-right">${MessageUtil.getMessage("common.label.quantity")}</th>
											<th scope="col" class="text-right">${MessageUtil.getMessage("stock.item.detail.col.price")}</th>
											<th scope="col" class="text-right">${MessageUtil.getMessage("stock.item.detail.col.amount")}</th>
											<th scope="col" class="text-right hidden sm:table-cell">${MessageUtil.getMessage("stock.item.detail.col.fees")}</th>
											<th scope="col" class="text-right">${MessageUtil.getMessage("stock.profit.realized")}</th>
										</tr>
									</thead>
									<tbody>
										@for(TradeResponse trade : trades)
											!{boolean isBuy = trade.type() == TradeType.BUY;}
											!{long fees = (trade.fee() != null ? trade.fee().longValue() : 0) + (trade.tax() != null ? trade.tax().longValue() : 0);}
											<tr>
												<td class="tabular-nums">${trade.tradeDate() != null ? trade.tradeDate().atZone(zone).toLocalDate().toString() : "-"}</td>
												<td><span class="badge badge-sm ${isBuy ? "badge-error badge-outline" : "badge-info badge-outline"}">${isBuy ? MessageUtil.getMessage("stock.trade.label.buy") : MessageUtil.getMessage("stock.trade.label.sell")}</span></td>
												<td class="text-right font-mono amount-value">${String.format("%,d", trade.quantity())}</td>
												<td class="text-right font-mono amount-value">${String.format("%,d", StockFormatUtil.displayWon(trade.price()))}</td>
												<td class="text-right font-mono amount-value">${String.format("%,d", StockFormatUtil.displayWon(trade.amount()))}</td>
												<td class="text-right font-mono amount-value hidden sm:table-cell opacity-60">${String.format("%,d", fees)}</td>
												<td class="text-right font-mono amount-value ${!isBuy && trade.realizedProfit() != null && trade.realizedProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : (!isBuy && trade.realizedProfit() != null ? "text-loss" : "opacity-40")}">${!isBuy && trade.realizedProfit() != null ? String.format("%+,d", StockFormatUtil.displayWon(trade.realizedProfit())) : "-"}</td>
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
							<div class="bg-base-200 rounded-box p-6 text-center text-sm text-base-content/60">${MessageUtil.getMessage("stock.item.detail.empty.dividends")}</div>
						@else
							@template._components.ui.tableCard(body = @`
								<div class="overflow-x-auto">
								<table class="table table-sm w-full table-pin-rows pin-top-nav" data-sortable>
									<thead class="bg-base-200">
										<tr>
											<th scope="col">${MessageUtil.getMessage("common.label.date")}</th>
											<th scope="col" class="text-right hidden sm:table-cell">${MessageUtil.getMessage("stock.item.detail.div.gross")}</th>
											<th scope="col" class="text-right hidden sm:table-cell">${MessageUtil.getMessage("stock.item.detail.div.tax")}</th>
											<th scope="col" class="text-right">${MessageUtil.getMessage("stock.item.detail.div.net")}</th>
										</tr>
									</thead>
									<tbody>
										@for(DividendResponse dividend : dividends)
											<tr>
												<td class="tabular-nums">${dividend.payDate() != null ? dividend.payDate().atZone(zone).toLocalDate().toString() : "-"}</td>
												<td class="text-right font-mono amount-value hidden sm:table-cell opacity-70">${String.format("%,d", StockFormatUtil.displayWon(dividend.grossAmount()))}</td>
												<td class="text-right font-mono amount-value hidden sm:table-cell opacity-60">${String.format("%,d", StockFormatUtil.displayWon(dividend.tax()))}</td>
												<td class="text-right font-mono font-bold amount-value text-dividend">${String.format("%,d", StockFormatUtil.displayWon(dividend.netAmount()))}</td>
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
