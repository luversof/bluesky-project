@import net.luversof.web.gate.stock.util.StockFormatUtil
@import net.luversof.web.gate.util.CspNonceHolder
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
@import net.luversof.web.gate.stock.util.RealizedBasisGap
@import net.luversof.web.gate.stock.util.ChartSeriesJs

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param boolean contentReady = false
@param String accountIdParam = ""
@param Account account = null
@param int holdingCount = 0
@param BigDecimal evaluationAmount = java.math.BigDecimal.ZERO
@param java.time.LocalDate priceBasisDate = null
@param BigDecimal totalBuyCost = java.math.BigDecimal.ZERO
@param BigDecimal evaluationProfit = java.math.BigDecimal.ZERO
@param BigDecimal realizedProfit = java.math.BigDecimal.ZERO
@param BigDecimal realizedProfitOwnBasis = java.math.BigDecimal.ZERO
@param BigDecimal totalDividend = java.math.BigDecimal.ZERO
@param List<TradeProfit> holdings = java.util.List.of()
@param List<TradeResponse> trades = java.util.List.of()
@param List<DividendResponse> dividends = java.util.List.of()
@param List<TradeProfitTimeSeriesPoint> timeSeries = java.util.List.of()
@param List<net.luversof.web.gate.stock.domain.DetailNavEntry> accountNavEntries = java.util.List.of()
@param DateTimeFormatter chartFormatter = null
@param Map<UUID, String> stockNameById = java.util.Map.of()
@param LocalDate filterStartLocal = null
@param LocalDate filterEndLocal = null
@param Instant filterStartInstant = null
@param Instant filterEndInstant = null
@param String filterTimeZone = null
@param String filterRangeMode = null
@param LocalDate coveredStartLocal = null
@param LocalDate coveredEndLocal = null


@template._layout.stockLayout(
	pageTitle = MessageUtil.getMessage("stock.account.detail.breadcrumb"),
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
				@template.stock.htmx.accountDetailContent(
					accountIdParam = accountIdParam,
					account = account,
					holdingCount = holdingCount,
					evaluationAmount = evaluationAmount,
					priceBasisDate = priceBasisDate,
					totalBuyCost = totalBuyCost,
					evaluationProfit = evaluationProfit,
					realizedProfit = realizedProfit,
					realizedProfitOwnBasis = realizedProfitOwnBasis,
					totalDividend = totalDividend,
					holdings = holdings,
					trades = trades,
					dividends = dividends,
					timeSeries = timeSeries,
					accountNavEntries = accountNavEntries,
					chartFormatter = chartFormatter,
					stockNameById = stockNameById,
					filterStartLocal = filterStartLocal,
					filterEndLocal = filterEndLocal,
					filterStartInstant = filterStartInstant,
					filterEndInstant = filterEndInstant,
					filterTimeZone = filterTimeZone,
					filterRangeMode = filterRangeMode,
					coveredStartLocal = coveredStartLocal,
					coveredEndLocal = coveredEndLocal
				)
			@endif
		</div>
	`
)
