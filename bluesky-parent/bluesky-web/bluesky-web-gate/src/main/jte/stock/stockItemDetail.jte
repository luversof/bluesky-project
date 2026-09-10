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


@template._layout.stockLayout(
	pageTitle = MessageUtil.getMessage("stock.item.detail.breadcrumb"),
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
				<%-- 조각이 이름을 알면 문서 제목도 그 이름으로(common.ts applyFragmentTitle). 없는 id 면 속성이 빠져 제목은 그대로다. --%>
				@template.stock.htmx.stockItemDetailContent(
					stockItemIdParam = stockItemIdParam,
					stockItem = stockItem,
					holdingQuantity = holdingQuantity,
					averageBuyPrice = averageBuyPrice,
					currentPrice = currentPrice,
					priceBasisDate = priceBasisDate,
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
					accountHoldings = accountHoldings,
					periodBreakdown = periodBreakdown,
					stockNavEntries = stockNavEntries,
					priceHistory = priceHistory,
					periodUnrealizedDelta = periodUnrealizedDelta,
					periodProfitRatePct = periodProfitRatePct
				)
			@endif
		</div>
	`
)
