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
@import net.luversof.web.gate.stock.util.ChartSeriesJs

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param boolean contentReady = false
@param boolean notFound = false
@param String stockItemIdParam = ""
@param StockItem stockItem = null
@param int holdingQuantity = 0
@param BigDecimal averageBuyPrice = java.math.BigDecimal.ZERO
@param BigDecimal currentPrice = java.math.BigDecimal.ZERO
@param boolean currentPriceUnknown = false
@param java.time.LocalDate priceBasisDate = null
@param String priceBasisIntradayTime = null
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
@param String periodBreakdownNote = ""
@param List<net.luversof.web.gate.stock.domain.DetailNavEntry> stockNavEntries = java.util.List.of()
@param List<net.luversof.web.gate.stock.dto.response.StockPriceHistoryPoint> priceHistory = java.util.List.of()
@param List<net.luversof.web.gate.stock.dto.response.StockPriceChartPoint> priceChart = java.util.List.of()
@param BigDecimal periodUnrealizedDelta = null
@param Double periodProfitRatePct = null
@param BigDecimal periodCapitalBase = null
@param net.luversof.web.gate.stock.util.StockHoldingReturnUtil.Row holdingReturn = null
@param BigDecimal realizedCostBasis = java.math.BigDecimal.ZERO
@param java.time.LocalDate holdingFirstBuyDate = null
@param String dataFirstDate = ""
@param String rangeMode = ""


@template._layout.stockLayout(
	pageTitle = notFound ? MessageUtil.getMessage("stock.item.detail.notfound.title") : MessageUtil.getMessage("stock.item.detail.breadcrumb"),
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentStock = @`
		<div class="w-full">
			@if(!contentReady)
				<!-- 셸: 콘텐츠를 전역 기간과 함께 htmx로 로드 (새로고침/이동 시에도 선택 기간 유지) -->
				<%-- URL 의 기간(rangeMode 또는 날짜)이 조각 요청까지 가야 공유 주소가 산다. 없으면 hx-include 의
				     전역 기간(= 마지막으로 보던 기간)만 실려, 주소에 적힌 기간이 조용히 무시됐다(실측 2026-09-11:
				     ?rangeMode=ytd 로 들어가도 저장된 1개월 그대로였다). 기간 세 키를 한 묶음으로 다루는 규칙은
				     common.ts 의 data-params-from-query 훅에 있다.
				     id 는 hx-vals 로 준다 - hx-get 경로에 두면 그 훅이 URL 의 같은 키를 한 번 더 붙여 "id,id" 로
				     묶인다(실측 2026-09-11: 상세가 통째로 '찾을 수 없습니다' 가 됐다). --%>
				<div id="stockItemDetailFragment" data-params-from-query
				     hx-get="/stock/item"
				     hx-vals='{"stockItemId": "${stockItemIdParam}"}'
				     hx-trigger="load"
				     hx-include="#globalDateRangeInputs input"
				     hx-select="#stockItemDetailFragment"
				     hx-swap="outerHTML">
					<div class="flex justify-center p-10"><span class="loading loading-spinner text-primary loading-lg"></span></div>
				</div>
			@else
				<%-- 조각이 이름을 알면 문서 제목도 그 이름으로(common.ts applyFragmentTitle).
				     없는 id 면 "종목을 찾을 수 없습니다" 를 싣는다 - 2026-09-12 까지는 속성이 빠져 제목이 정상 화면과 같았다. --%>
				@template.stock.htmx.stockItemDetailContent(
					stockItemIdParam = stockItemIdParam,
					stockItem = stockItem,
					holdingQuantity = holdingQuantity,
					averageBuyPrice = averageBuyPrice,
					currentPrice = currentPrice,
					currentPriceUnknown = currentPriceUnknown,
					priceBasisDate = priceBasisDate,
					priceBasisIntradayTime = priceBasisIntradayTime,
					evaluationAmount = evaluationAmount,
					evaluationProfit = evaluationProfit,
					realizedProfit = realizedProfit,
					totalBuyCost = totalBuyCost,
					totalDividend = totalDividend,
					trades = trades,
					dividends = dividends,
					timeSeries = timeSeries,
					chartFormatter = chartFormatter,
					filterStartLocal = filterStartLocal,
					filterEndLocal = filterEndLocal,
					filterStartInstant = filterStartInstant,
					filterEndInstant = filterEndInstant,
					filterTimeZone = filterTimeZone,
					filterRangeMode = filterRangeMode,
					coveredStartLocal = coveredStartLocal,
					coveredEndLocal = coveredEndLocal,
					dataFirstDate = dataFirstDate,
					rangeMode = rangeMode,
					accountHoldings = accountHoldings,
					periodBreakdown = periodBreakdown,
					periodBreakdownNote = periodBreakdownNote,
					stockNavEntries = stockNavEntries,
					priceHistory = priceHistory,
					priceChart = priceChart,
					periodUnrealizedDelta = periodUnrealizedDelta,
					periodProfitRatePct = periodProfitRatePct,
					periodCapitalBase = periodCapitalBase,
					holdingReturn = holdingReturn,
					holdingFirstBuyDate = holdingFirstBuyDate,
					realizedCostBasis = realizedCostBasis
				)
			@endif
		</div>
	`
)
