@import net.luversof.web.gate.util.CspNonceHolder
@import java.util.List
@import java.util.Map
@import java.util.UUID
@import java.time.Instant
@import java.time.ZoneId
@import java.math.BigDecimal
@import net.luversof.web.gate.stock.dto.response.TradeResponse
@import net.luversof.web.gate.stock.domain.Account
@import net.luversof.web.gate.stock.domain.StockItem
@import net.luversof.web.gate.stock.domain.TradeProfit
@import net.luversof.web.gate.stock.controller.StockTradeHtmxController.AccountRealizedRow
@import java.time.format.DateTimeFormatter
@import java.text.DecimalFormat
@import io.github.luversof.boot.context.support.MessageUtil

@param List<TradeResponse> tradeList
@param List<TradeResponse> allTradeList = java.util.List.of()
@param int totalItems
@param List<Account> accountList
@param List<StockItem> stockItemList
@param List<StockItem> tagCountStockItemList = stockItemList
@param List<String> stockTagList = java.util.List.of()
@param Map<UUID, String> accountNames
@param UUID selectedAccountId
@param UUID selectedStockItemId
@param List<UUID> selectedAccountIds = java.util.List.of()
@param List<UUID> selectedStockItemIds = java.util.List.of()
@param List<String> selectedStockTags = java.util.List.of()
@param java.time.Instant startDate
@param java.time.Instant endDate
@param String timeZone = null
@param String sort
@param BigDecimal totalFee
@param BigDecimal totalTax
@param BigDecimal totalRealizedProfit
@param BigDecimal totalAllBuyAmount = BigDecimal.ZERO
@param BigDecimal totalAllSellAmount = BigDecimal.ZERO
@param BigDecimal totalAllFee = BigDecimal.ZERO
@param BigDecimal totalAllTax = BigDecimal.ZERO
@param BigDecimal totalAllRealizedProfit = BigDecimal.ZERO
@param String rangeMode = null
@param String dataFirstDate = null
@param List<AccountRealizedRow> accountRealizedList = java.util.List.of()
@param List<TradeProfit> stockRealizedList = java.util.List.of()
@param long realizedWinCount = 0L
@param int realizedStockCount = 0

!{
	DecimalFormat decimalFormat = new DecimalFormat("#,##0");
	java.util.function.Function<Object, String> countMessage = value -> java.text.MessageFormat.format(MessageUtil.getMessage("common.count"), value);

	// 여러 섹션이 공유하는 라벨 (하위 컴포넌트에 param 으로 전달)
	String accountLabel = MessageUtil.getMessage("common.label.account");
	String stockLabel = MessageUtil.getMessage("common.label.stock");
	String buyLabel = MessageUtil.getMessage("stock.trade.label.buy");
	String sellLabel = MessageUtil.getMessage("stock.trade.label.sell");
	String realizedProfitLabel = MessageUtil.getMessage("stock.trade.label.realized.profit");
	String feeLabel = MessageUtil.getMessage("stock.trade.label.fee");
	String tradingTaxLabel = MessageUtil.getMessage("stock.trade.label.trading.tax");
	String buyConcentrationTitle = MessageUtil.getMessage("stock.trade.chart.title.buy.concentration");
	String monthlyTradeAmountTitle = MessageUtil.getMessage("stock.trade.chart.title.monthly.trade.amount");

	// Build JSON for charts
	java.util.function.Function<String, String> ej = s ->
		s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
	DateTimeFormatter ymd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	// 차트(stock-charts.ts)가 쓰는 것은 날짜·종목·계좌·구분·금액·실현손익이다. 종목/계좌 이름은 줄마다 되풀이되므로
	// 색인표(S/A)로 한 번만 싣고 줄은 배열로 줄인다(실측 2026-09-09: 전체 기간 321 건에서 31KB → 약 11KB). 모양은 인라인에서 되펼친다.
	java.time.ZoneId chartZone = net.luversof.web.gate.stock.util.StockZoneUtil.resolve(timeZone);
	List<TradeResponse> chartList = allTradeList != null ? allTradeList : java.util.List.of();
	java.util.LinkedHashMap<String, Integer> stockIndex = new java.util.LinkedHashMap<>();
	java.util.LinkedHashMap<String, Integer> accountIndex = new java.util.LinkedHashMap<>();
	StringBuilder rowsSb = new StringBuilder();
	for (int ci = 0; ci < chartList.size(); ci++) {
		TradeResponse tr = chartList.get(ci);
		String stockName = tr.stockItemName() != null ? tr.stockItemName() : "";
		String accountName = accountNames != null ? accountNames.getOrDefault(tr.accountId(), "") : "";
		int si = stockIndex.computeIfAbsent(stockName, k -> stockIndex.size());
		int ai = accountIndex.computeIfAbsent(accountName, k -> accountIndex.size());
		if (ci > 0) rowsSb.append(",");
		rowsSb.append("[\"").append(tr.tradeDate() != null ? ymd.format(tr.tradeDate().atZone(chartZone)) : "").append("\",")
			.append(si).append(",").append(ai).append(",\"").append(tr.type() != null ? tr.type().name() : "").append("\",")
			.append(tr.amount() != null ? tr.amount().toPlainString() : "0").append(",")
			.append(tr.realizedProfit() != null ? tr.realizedProfit().toPlainString() : "0").append("]");
	}
	java.util.function.Function<java.util.Set<String>, String> namesJs = names -> {
		StringBuilder sb = new StringBuilder("[");
		int i = 0;
		for (String name : names) {
			if (i++ > 0) sb.append(",");
			sb.append("\"").append(ej.apply(name)).append("\"");
		}
		return sb.append("]").toString();
	};
	String tradeDataJs = "const S=" + namesJs.apply(stockIndex.keySet()) + ",A=" + namesJs.apply(accountIndex.keySet())
		+ ",tradeData=[" + rowsSb + "].map(function(r){return {tradeDate:r[0],stockItem:S[r[1]],account:A[r[2]],type:r[3],amount:r[4],profit:r[5]};});";

	// Nav 버튼 활성화 여부 (use client timezone if available)
	boolean isAllMode = "all".equals(rangeMode);
	java.time.ZoneId zone = net.luversof.web.gate.stock.util.StockZoneUtil.resolve(timeZone);
	java.time.LocalDate today = java.time.LocalDate.now(zone);
	java.time.LocalDate startLocal = startDate != null ? startDate.atZone(zone).toLocalDate() : null;
	// '전체' 는 날짜를 안 보내므로 기간 배지가 빌 수밖에 없다. 그때는 이 화면이 실제로 그린 자료의
	// 구간을 대신 적는다 - 화면마다 덮는 범위가 다르므로 사용자 전체의 최초 일자를 쓰면 틀린 날짜가 나온다.
	var coveredRange = net.luversof.web.gate.stock.util.StockCoveredRangeUtil.covered(allTradeList, net.luversof.web.gate.stock.dto.response.TradeResponse::tradeDate, zone);
	java.time.LocalDate endLocal = endDate != null ? endDate.atZone(zone).toLocalDate() : null;
	boolean canNext = !isAllMode && endLocal != null && endLocal.isBefore(today);
	boolean canPrev = !isAllMode && startLocal != null
		&& dataFirstDate != null && !dataFirstDate.isEmpty()
		&& startLocal.isAfter(java.time.LocalDate.parse(dataFirstDate));
}

<div id="tradeListFragment">

	@template.stock.htmx.fragments.dateRangeNavBar(
		pickerName = "tradePicker",
		btnClass = "date-range-btn",
		canPrev = canPrev,
		canNext = canNext,
		rangeMode = rangeMode,
		startDate = startLocal,
		endDate = endLocal,
		coveredStartDate = coveredRange.startDate(),
		coveredEndDate = coveredRange.endDate()
	)

	@template.stock.htmx.fragments.trade.tradeSearchForm(
		accountList = accountList,
		stockItemList = stockItemList,
		tagCountStockItemList = tagCountStockItemList,
		stockTagList = stockTagList,
		selectedAccountId = selectedAccountId,
		selectedStockItemId = selectedStockItemId,
		selectedAccountIds = selectedAccountIds,
		selectedStockItemIds = selectedStockItemIds,
		selectedStockTags = selectedStockTags,
		startDate = startDate,
		endDate = endDate,
		timeZone = timeZone,
		rangeMode = rangeMode,
		zone = zone,
		accountLabel = accountLabel,
		stockLabel = stockLabel
	)

	@template.stock.htmx.fragments.trade.tradeSummaryCards(
		allTradeList = allTradeList,
		totalItems = totalItems,
		totalAllBuyAmount = totalAllBuyAmount,
		totalAllSellAmount = totalAllSellAmount,
		totalAllFee = totalAllFee,
		totalAllTax = totalAllTax,
		totalAllRealizedProfit = totalAllRealizedProfit,
		decimalFormat = decimalFormat,
		countMessage = countMessage,
		buyLabel = buyLabel,
		sellLabel = sellLabel,
		realizedProfitLabel = realizedProfitLabel,
		feeLabel = feeLabel,
		tradingTaxLabel = tradingTaxLabel
	)

	<!-- Charts Row -->
	<div class="flex flex-wrap gap-4 mb-4 overflow-visible">

		@template.stock.htmx.fragments.components.doughnutShell(
			title = buyConcentrationTitle,
			canvasId = "tradeDonutChart",
			legendId = "tradeDonutLegend",
			titleId = "tradeDonutTitle",
			metricProfitId = "tradeDonutMetricProfit",
			metricBuyId = "tradeDonutMetricBuy",
			tabStockId = "tradeDonutTabStock",
			tabAccountId = "tradeDonutTabAccount"
		)

		@template.stock.htmx.fragments.components.barShell(
			title = monthlyTradeAmountTitle,
			canvasId = "tradeMonthlyChart"
		)

	</div>

	@template.stock.htmx.fragments.trade.tradeRealizedSections(
		accountRealizedList = accountRealizedList,
		stockRealizedList = stockRealizedList,
		realizedWinCount = realizedWinCount,
		realizedStockCount = realizedStockCount,
		decimalFormat = decimalFormat,
		accountLabel = accountLabel,
		stockLabel = stockLabel,
		realizedProfitLabel = realizedProfitLabel
	)

	<%-- 달/해 단위 쪼갬. 카드는 고른 기간 전체를 답하고 아래 목록은 거래 하나하나라, 그 사이가
		 비어 있었다. 월별 매매 금액 차트가 모양은 주지만 수를 주지 않는다 - 그 해 수수료가
		 얼마였는지, 실현손익이 얼마였는지는 읽을 수 없다.
		 집계는 게이트에서 한다 - 화면이 이미 고른 기간의 매매 원장을 통째로 받고 있다. --%>
	@template.stock.htmx.fragments.trade.tradePeriodBreakdown(
		tradePeriods = net.luversof.web.gate.stock.util.StockTradePeriodUtil.of(allTradeList, zone)
	)

	@template.stock.htmx.fragments.trade.tradeDetailList(
		tradeList = tradeList,
		zone = zone,
		totalItems = totalItems,
		accountNames = accountNames,
		sort = sort,
		totalFee = totalFee,
		totalTax = totalTax,
		totalRealizedProfit = totalRealizedProfit,
		decimalFormat = decimalFormat,
		countMessage = countMessage,
		accountLabel = accountLabel,
		stockLabel = stockLabel,
		realizedProfitLabel = realizedProfitLabel,
		feeLabel = feeLabel,
		tradingTaxLabel = tradingTaxLabel,
		buyLabel = buyLabel,
		sellLabel = sellLabel
	)

	<script nonce="${CspNonceHolder.getNonce()}">
	(function() {
		$unsafe{tradeDataJs}

		window.tradeMonthlyChartInst = null;
		window.tradeDonutChartInst = null;
		window.currentMetric = 'buy';
		window.currentGroupBy = 'stock';

		window.switchTradeDonut = function(metric, groupBy) {
			if (metric) window.currentMetric = metric;
			if (groupBy) window.currentGroupBy = groupBy;
			var elProfit = document.getElementById('tradeDonutMetricProfit');
			var elBuy = document.getElementById('tradeDonutMetricBuy');
			var elStock = document.getElementById('tradeDonutTabStock');
			var elAccount = document.getElementById('tradeDonutTabAccount');
			if (elProfit) elProfit.classList.toggle('tab-active', window.currentMetric === 'profit');
			if (elBuy) elBuy.classList.toggle('tab-active', window.currentMetric === 'buy');
			if (elStock) elStock.classList.toggle('tab-active', window.currentGroupBy === 'stock');
			if (elAccount) elAccount.classList.toggle('tab-active', window.currentGroupBy === 'account');

			window.ensureStockCharts(function() {
				if (!window.StockCharts) return;
				window.tradeDonutChartInst = window.StockCharts.initDonutFromData(tradeData, {
					canvasId: 'tradeDonutChart',
					legendId: 'tradeDonutLegend',
					titleId: 'tradeDonutTitle',
					metric: window.currentMetric,
					groupBy: window.currentGroupBy,
					tooltipId: 'tradeDonutTooltipEl'
				}, window.tradeDonutChartInst);
			});
		};

		function initAll() {
			window.ensureStockCharts(function() {
				if (!window.StockCharts) return;
				window.tradeMonthlyChartInst = window.StockCharts.initMonthlyFromData(tradeData, 'tradeMonthlyChart', window.tradeMonthlyChartInst);
				window.tradeDonutChartInst = window.StockCharts.initDonutFromData(tradeData, {
					canvasId: 'tradeDonutChart',
					legendId: 'tradeDonutLegend',
					titleId: 'tradeDonutTitle',
					metric: window.currentMetric,
					groupBy: window.currentGroupBy,
					tooltipId: 'tradeDonutTooltipEl'
				}, window.tradeDonutChartInst);
			});
		}

		window.tradePicker = window.DateRangePicker
			? window.DateRangePicker.create({
				formId:      'tradeSearchForm',
				startId:     'tradeStartDateInput',
				endId:       'tradeEndDateInput',
				instantStartId: 'tradeStartInstantInput',
				instantEndId:   'tradeEndInstantInput',
				timeZoneId:     'tradeTimeZoneInput',
				rangeModeId: 'tradeRangeModeInput',
				btnClass:    'date-range-btn',
				minDate:     '${dataFirstDate != null ? dataFirstDate : ""}',
				globalKey:   'globalDateRange',
				rootSelector: '#tradeListFragment'
			})
			: { set: function(){}, shift: function(){}, jumpToEdge: function(){} };
		// 기본값: 페이지 진입 시 기간 미지정이면 '이번달(mtd)'을 선택 (한번만 적용하도록 세션 가드)
		try {
			if (window.tradePicker && typeof window.tradePicker.getState === 'function') {
				var __s = window.tradePicker.getState();
				if ((!__s || !__s.mode) && (!__s || !__s.start) && (!__s || !__s.end)) {
					var __key = 'dateRangeDefaultApplied:tradePicker';
					try {
						if (!sessionStorage.getItem(__key)) {
							var _btn = document.querySelector('#tradeListFragment .date-range-btn');
							window.tradePicker.set('mtd', _btn || null);
							sessionStorage.setItem(__key, '1');
						}
					} catch(e) { window.tradePicker.set('mtd', null); }
				}
			}
		} catch(e) {}
		// Chart.js / StockCharts 는 stockLayout 에서 1회 로드되어 항상 존재한다. (initAll 내부에서도 자체 가드)
		setTimeout(initAll, 0);
		setTimeout(function() {
			if (window.tradeMonthlyChartInst && window.tradeMonthlyChartInst.resize) window.tradeMonthlyChartInst.resize();
			if (window.tradeDonutChartInst && window.tradeDonutChartInst.resize) window.tradeDonutChartInst.resize();
		}, 200);

		const detail = document.getElementById('tradeDetailSection');
		if (detail) {
			const arrow = detail.querySelector('.detail-arrow');
			detail.addEventListener('toggle', function() {
				if (arrow) arrow.style.transform = detail.open ? 'rotate(90deg)' : '';
			});
		}

		// 페이징 클릭 시 fragment 상단 스크롤
		if (window.__pendingScrollTo === 'tradeListFragment') {
			delete window.__pendingScrollTo;
			var frag = document.getElementById('tradeListFragment');
			if (frag) frag.scrollIntoView({ behavior: 'smooth', block: 'start' });
		}
		document.querySelectorAll('#tradeListFragment .join button[hx-get]').forEach(function(btn) {
			btn.addEventListener('click', function() { window.__pendingScrollTo = 'tradeListFragment'; });
		});
	})();
	</script>
</div>
