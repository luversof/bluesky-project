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
@import net.luversof.web.gate.stock.util.StockDetailLinkUtil

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
<%-- 실현 손익 카드의 수익률 분모 = 판 주식 원가(같은 기간의 매도 금액 - 실현 손익). 판 적이 없으면 0. --%>
@param BigDecimal realizedCostBasis = java.math.BigDecimal.ZERO
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
<%-- 합산 비율의 분모(넣어 둔 돈 = 기초 평가액 + max(기간 순유입, 0)). 세 몫의 비율도 이 분모로 낸다. --%>
@param BigDecimal periodCapitalBase = null
<%-- 보유 전체 기준 보유 기간 · 연평균 수익률(복리). 자산 현황의 종목 줄과 같은 계산이다. --%>
@param net.luversof.web.gate.stock.util.StockHoldingReturnUtil.Row holdingReturn = null
@param java.time.LocalDate holdingFirstBuyDate = null
@param String dataFirstDate = ""
<%-- 지금 기간 모드. 빈 안내에 '전체 기간으로 보기' 를 띄울지 정한다. --%>
@param String rangeMode = ""

!{
	// 아래 표의 날짜 칸은 이 페이지가 차트/필터에 넘기는 존과 같아야 한다.
	// 예전에는 서버 존이라 브라우저 존이 다르면 한 화면 안에서 하루가 어긋났다.
	ZoneId zone = net.luversof.web.gate.stock.util.StockZoneUtil.resolve(filterTimeZone);
	String evalColor = evaluationProfit != null && evaluationProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss";
	String realizedColor = realizedProfit != null && realizedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss";
	BigDecimal evalCostBasis = (evaluationAmount != null ? evaluationAmount : BigDecimal.ZERO).subtract(evaluationProfit != null ? evaluationProfit : BigDecimal.ZERO);
	double evalProfitRatePct = evalCostBasis.signum() != 0 && evaluationProfit != null ? evaluationProfit.doubleValue() / evalCostBasis.doubleValue() * 100.0 : 0.0;
	// 원가가 0 이면 수익률은 <b>없는 값</b>이다(0 / 0). 예전에는 0.0 으로 떨어져 "0 0.0%" 가 잰 값처럼 보였다 -
	// 실측 2026-09-13: 보유가 없는 종목 77 개와 계좌 1 개(동양증권 - 매수 2.75 억 · 실현 +3,309 만)가 모두 그랬다.
	// 금액 0 은 맞지만 비율은 모른다. 모르면 적지 않는다.
	boolean evalRateKnown = evalCostBasis.signum() != 0;
	// 카드마다 그 돈의 원금 대비 비율(사용자 선택 2026-09-17 "카드마다 그 원금 대비"): 평가 손익 = 보유 원가 · 실현 손익 = 판 주식 원가 ·
	// 기간 배당 = 보유 원가. 위 합산 상자의 세 몫은 '넣어 둔 돈' 대비라 더하면 합산이 되지만 카드는 각자의 원금을 말한다 - 그래서 기준을 글자로 붙인다.
	// 실측 2026-09-17 TIGER 리츠부동산인프라(전체): 평가 -7.1% · 실현 +2.4%(874,593 / 37,199,152) · 배당 9.3%(5,587,497 / 60,159,793).
	String holdingBasisLabel = MessageUtil.getMessage("stock.item.detail.rate.basis.holding");
	boolean realizedRateKnown = realizedCostBasis != null && realizedCostBasis.signum() > 0;
	double realizedCostRatePct = realizedRateKnown && realizedProfit != null ? realizedProfit.doubleValue() * 100.0 / realizedCostBasis.doubleValue() : 0.0;
	boolean dividendRateKnown = evalCostBasis.signum() > 0 && holdingQuantity > 0;
	double dividendCostRatePct = dividendRateKnown && totalDividend != null ? totalDividend.doubleValue() * 100.0 / evalCostBasis.doubleValue() : 0.0;
	// 세금 0 은 잰 값이지만 표마다 다르게 적혀 있었다 - 배당 이력 · 수익률 표는 이미 "-" 와 사유를 달고,
	// 상세 두 화면만 "0" 이었다. 실측 2026-09-13: 배당 202 건 중 147 건(72.8%)이 세금 0 이라
	// 같은 값이 화면마다 달리 보였다. 사유는 title 과 aria-label 에 함께 단다(마우스 전용이 되지 않게).
	String zeroAmountTitle = MessageUtil.getMessage("stock.dividend.zero.amount.title");

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
	// 세 몫(평가 변동 · 실현 · 배당)에도 비율을 붙인다 - 사용자 요청 2026-09-17: "보유 손익 + 배당 손익 표시(수익률 퍼센트도)".
	// 합산 비율과 같은 분모를 써야 셋을 더한 값이 합산 비율이 된다(화면에서 검산할 수 있게).
	boolean componentRateAvailable = combinedRateAvailable && periodCapitalBase != null && periodCapitalBase.signum() > 0;
	double unrealizedDeltaRatePct = componentRateAvailable ? unrealizedDelta.doubleValue() * 100.0 / periodCapitalBase.doubleValue() : 0.0;
	double realizedRatePct = componentRateAvailable && realizedProfit != null ? realizedProfit.doubleValue() * 100.0 / periodCapitalBase.doubleValue() : 0.0;
	double dividendRatePct = componentRateAvailable && totalDividend != null ? totalDividend.doubleValue() * 100.0 / periodCapitalBase.doubleValue() : 0.0;
	// 보유 전체 기준 보유 기간 · 연평균. 지금 보유하지 않는 종목에는 '보유 기간' 이 없다(자산 현황도 보유 종목만 적는다).
	boolean holdingReturnShown = holdingReturn != null && holdingReturn.hasPeriod() && holdingQuantity > 0 && holdingFirstBuyDate != null;
	String holdingPeriodText = !holdingReturnShown ? ""
		: (holdingReturn.daysOnly()
			? java.text.MessageFormat.format(MessageUtil.getMessage("stock.asset.status.cell.holding.days"), String.valueOf(holdingReturn.holdingDays()))
			: (holdingReturn.years() == 0
				? java.text.MessageFormat.format(MessageUtil.getMessage("stock.asset.status.cell.holding.months"), String.valueOf(holdingReturn.months()))
				: (holdingReturn.months() == 0
					? java.text.MessageFormat.format(MessageUtil.getMessage("stock.asset.status.cell.holding.years.only"), String.valueOf(holdingReturn.years()))
					: java.text.MessageFormat.format(MessageUtil.getMessage("stock.asset.status.cell.holding.years"), String.valueOf(holdingReturn.years()), String.valueOf(holdingReturn.months())))));
	// 카드 값은 비율만 적는다 - 라벨이 이미 '연평균 수익률' 이라 "연 +3.4%" 의 '연' 은 겹치고, 큰 숫자 글꼴 안에서 한 글자만 다른 글꼴로 떠 보였다.
	String holdingAnnualizedText = !holdingReturnShown || holdingReturn.annualizedPct() == null ? ""
		: StockFormatUtil.signedPct(holdingReturn.annualizedPct().doubleValue(), 1);
}

<div id="stockItemDetailFragment" class="space-y-6" data-page-title="${stockItem != null ? (stockItem.name() != null ? stockItem.name() : stockItem.symbol()) : MessageUtil.getMessage("stock.item.detail.notfound.title")}">
@if(stockItem == null)
	<%-- 없는 id 로 들어와도 페이지다: 빵부스러기와 h1 이 있어야 뒤로 갈 길이 보이고 axe page-has-heading-one 에 걸리지 않는다(실측 2026-09-09). --%>
	@template._components.ui.breadcrumbBack(
		homeLabel = MessageUtil.getMessage("layout.menu.main.stock"),
		categoryLabel = MessageUtil.getMessage("stock.item.detail.breadcrumb"),
		backLabel = MessageUtil.getMessage("common.button.back"))
	@template._components.ui.pageHeader(title = MessageUtil.getMessage("stock.item.detail.breadcrumb"))
	@template._components.ui.emptyState(message = MessageUtil.getMessage("stock.item.detail.notfound"), boxed = true)
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

	<%-- dataFirstDate 가 없으면 '가장 이른 기간으로'(«) 가 목표를 못 정해 비활성이 된다.
	     JTE 는 호출 인자 목록 안에 주석을 못 쓴다 - 여기에 적는다. --%>
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
		coveredEndDate = coveredEndLocal,
		dataFirstDate = dataFirstDate)

	<%-- "현재가"·평가액은 마지막으로 수집된 종가 기준이라, 오늘 시세가 아직 없으면 며칠 전 값일 수 있다.
	     어느 날 기준인지 밝혀 실시간 시세로 오해하지 않게 한다(자산현황·포트폴리오와 같은 표기). --%>
	@if(priceBasisDate != null)
		<p class="mb-2 text-xs text-base-content/60">${net.luversof.web.gate.stock.util.StockPriceBasisUtil.basisMessage(priceBasisIntradayTime).replace("{0}", priceBasisDate.toString())}</p>
	@endif
	<%-- 합산 손익 - 평가손익·실현손익·배당이 카드로 흩어져 있어 사람이 더해야 알 수 있었다.
	     아래 격자의 세 값을 그대로 더한 것이라, 화면에서 눈으로 검산할 수 있게 셋을 함께 적는다. --%>
	<div class="mb-3 rounded-2xl border border-base-300/60 bg-base-100/80 px-4 py-3">
		<div class="text-sm font-semibold text-base-content/70">${MessageUtil.getMessage("stock.item.detail.combined.profit")}</div>
		<div class="mt-1 flex flex-wrap items-baseline gap-x-3 gap-y-1">
			<span class="text-3xl font-bold leading-none amount-value ${combinedPositive ? "text-profit" : "text-loss"}"
				  title="${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(combinedProfit))}">
				${StockFormatUtil.signedWon(StockFormatUtil.displayWon(combinedProfit))}
			</span>
			@if(combinedRateAvailable)
				<span class="text-lg font-bold ${combinedPositive ? "text-profit" : "text-loss"}">
					${StockFormatUtil.signedPct(combinedRatePct, 2)}
				</span>
				<span class="text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.item.detail.combined.basis")}</span>
			@endif
		</div>
		<%-- 셋을 그대로 적어 화면에서 더해 볼 수 있게 한다. --%>
		<p class="mt-1 text-[11px] leading-4 flex flex-wrap items-baseline gap-x-1.5" data-combined-breakdown>
			<span class="text-base-content/60">${MessageUtil.getMessage("stock.item.detail.combined.unrealized.delta")}</span>
			<span class="amount-value ${deltaColor}">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(unrealizedDelta))}</span>
			@if(componentRateAvailable)<span class="${deltaColor}" data-component-rate="unrealized">(${StockFormatUtil.signedPct(unrealizedDeltaRatePct, 2)})</span>@endif
			<span class="text-base-content/60">+</span>
			<span class="text-base-content/60">${MessageUtil.getMessage("stock.profit.realized")}</span>
			<span class="amount-value ${realizedColor}">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(realizedProfit))}</span>
			@if(componentRateAvailable)<span class="${realizedColor}" data-component-rate="realized">(${StockFormatUtil.signedPct(realizedRatePct, 2)})</span>@endif
			<span class="text-base-content/60">+</span>
			<span class="text-base-content/60">${MessageUtil.getMessage("stock.item.detail.period.dividend")}</span>
			<span class="amount-value text-dividend">${StockFormatUtil.signedWon(StockFormatUtil.displayWon(totalDividend))}</span>
			@if(componentRateAvailable)<span class="text-dividend" data-component-rate="dividend">(${StockFormatUtil.signedPct(dividendRatePct, 2)})</span>@endif
		</p>
		<p class="mt-0.5 text-[11px] leading-4 text-base-content/60">${MessageUtil.getMessage("stock.item.detail.combined.note")}</p>
		<%-- 보유 전체 기준 보유 기간 · 연평균은 아래 카드 줄로 옮겼다(사용자 선택 2026-09-17 - 이 자리의 작은 줄은 눈에 띄지 않았다).
		     배당이 지금 평가손실을 얼마나 덮었는지만 여기 남긴다. --%>
		@if(holdingReturnShown && holdingReturn.coveragePct() != null && holdingReturn.coveragePct() > 0)
			<p class="mt-2 border-t border-base-300/60 pt-2 text-[11px] leading-4" data-holding-coverage-line>
				<span class="text-dividend" data-holding-coverage="${holdingReturn.coveragePct()}">${holdingReturn.coveragePct() >= 100 ? MessageUtil.getMessage("stock.asset.status.cell.coverage.all") : java.text.MessageFormat.format(MessageUtil.getMessage("stock.asset.status.cell.coverage"), String.valueOf(holdingReturn.coveragePct()))}</span>
			</p>
		@endif
	</div>

	<!-- 보유/손익 요약 -->
	<%-- 이 카드 여덟 장 중 셋(실현 손익·기간 매수·기간 배당)은 고른 기간의 값이고, 나머지 다섯(수량·평균 단가·
	     현재가·평가 금액·평가 손익)은 지금 보유분이다. 라벨이 축을 말하지 않으면 나란히 놓인 값끼리 검산이 안 된다 -
	     실측 2026-09-11 삼성전자: "매수 원가 466,248,078" 옆의 평가 손익은 996,563,421 인데
	     1,359,088,500 - 466,248,078 = 892,840,422 이라 맞지 않았고(평가 손익은 보유원가 362,525,079 기준),
	     기간을 올해로 좁히면 "매수 원가 0원" 옆에 평가 손익 +9.9억이 그대로 섰다. "누적 배당" 도 한 달 기간에서
	     1,595,632 를 누적이라고 적었다. 값이 아니라 이름을 고친다 - 기간 값이라는 정보를 잃지 않는다. --%>
	<div class="grid stat-card-grid grid-cols-1 min-[360px]:grid-cols-2 lg:grid-cols-4 gap-3">
		@template._components.ui.statCard(
			label = MessageUtil.getMessage("common.label.quantity"),
			value = net.luversof.web.gate.stock.util.StockMessageSplitUtil.beforePlaceholder(MessageUtil.getMessage("stock.activity.label.shares"))
				+ String.format("%,d", holdingQuantity)
				+ (holdingQuantity == 1
					? net.luversof.web.gate.stock.util.StockMessageSplitUtil.afterPlaceholder(MessageUtil.getMessage("stock.activity.label.shares.one"))
					: net.luversof.web.gate.stock.util.StockMessageSplitUtil.afterPlaceholder(MessageUtil.getMessage("stock.activity.label.shares"))))
		<%-- 수량도 금액 숨김 대상이다 - 활동 목록·자산 현황은 수량을 amount-value 로 감싼다(공개 가격 x 수량 = 평가금액).
		     실측 2026-09-11: 이 카드만 amount = false 라 숨김을 켜도 "5,043주" 가 그대로 보였다. --%>
		<%-- 보유가 없으면 평균 단가라는 값 자체가 없다 - 컨트롤러가 0 으로 떨어뜨린다. 실측 2026-09-13: 86 종목 중 77 개가
	     "평균 단가 0" 을 찍었고, 바로 옆 현재가는 210,000(NAVER) 이라 0 원에 산 것처럼 읽혔다. --%>
		@if(holdingQuantity <= 0)
		@template._components.ui.statCard(
			label = MessageUtil.getMessage("stock.column.average.cost"),
			value = MessageUtil.getMessage("stock.detail.value.no.holding"),
			amount = false)
		@else
		@template._components.ui.statCard(
			label = MessageUtil.getMessage("stock.column.average.cost"),
			value = String.format("%,d", StockFormatUtil.displayWon(averageBuyPrice)),
			valueTitle = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(averageBuyPrice)))
		@endif
		<%-- 시세를 모르는 종목이 있다 - 실측 2026-09-13: 86 종목 중 4 종목(0177R0 · 0190G0 · 0210E0 · 0219E0)은
		     가격 이력 자체가 없다. 그때 "0" 은 잰 값처럼 읽힌다(같은 화면의 매매 · 배당 · 주가 구역은 모두
		     "…없습니다" 라고 말하는데 이 카드만 0 을 적었다). 값 대신 없다고 말한다 -
		     금액이 아니므로 금액 숨김(흐리게)도 걸지 않는다. --%>
		@if(currentPriceUnknown)
		@template._components.ui.statCard(
			label = MessageUtil.getMessage("stock.column.current.price"),
			value = MessageUtil.getMessage("stock.item.detail.current.price.unknown"),
			amount = false)
		@else
		@template._components.ui.statCard(
			label = MessageUtil.getMessage("stock.column.current.price"),
			value = String.format("%,d", StockFormatUtil.displayWon(currentPrice)),
			valueTitle = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(currentPrice)))
		@endif
		@template._components.ui.statCard(
			label = MessageUtil.getMessage("stock.profit.evaluation.amount"),
			value = String.format("%,d", StockFormatUtil.displayWon(evaluationAmount)),
			valueTitle = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(evaluationAmount)),
			tone = "text-principal")
		@template._components.ui.statCard(
			label = MessageUtil.getMessage("stock.profit.unrealized"),
			value = StockFormatUtil.signedWon(StockFormatUtil.displayWon(evaluationProfit)),
			valueTitle = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(evaluationProfit)),
			sub = evalRateKnown ? StockFormatUtil.signedPct(evalProfitRatePct, 1) + " · " + holdingBasisLabel : "",
			tone = evalColor)
		<%-- 판 적이 없으면 비율이 없다(0 / 0) - 그 사실을 적고, 0 을 이득처럼 칠하지 않는다. --%>
		@template._components.ui.statCard(
			label = MessageUtil.getMessage("stock.profit.realized"),
			value = StockFormatUtil.signedWon(StockFormatUtil.displayWon(realizedProfit)),
			valueTitle = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(realizedProfit)),
			sub = realizedRateKnown
				? StockFormatUtil.signedPct(realizedCostRatePct, 1) + " · " + MessageUtil.getMessage("stock.item.detail.rate.basis.sold")
				: MessageUtil.getMessage("stock.item.detail.rate.none.sold"),
			tone = realizedRateKnown ? realizedColor : "")
		@template._components.ui.statCard(
			label = MessageUtil.getMessage("stock.item.detail.principal"),
			value = String.format("%,d", StockFormatUtil.displayWon(totalBuyCost)),
			valueTitle = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(totalBuyCost)))
		<%-- 배당은 음수가 없어 부호를 붙이지 않는다. 지금 보유하지 않으면 보유 원가가 없어 비율도 없다. --%>
		@template._components.ui.statCard(
			label = MessageUtil.getMessage("stock.item.detail.period.dividend"),
			value = String.format("%,d", StockFormatUtil.displayWon(totalDividend)),
			valueTitle = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(totalDividend)),
			sub = dividendRateKnown
				? StockFormatUtil.pct(dividendCostRatePct, 1) + " · " + holdingBasisLabel
				: MessageUtil.getMessage("stock.item.detail.rate.none.holding"),
			tone = "text-dividend")
		<%-- 보유 전체 기준 연평균 수익률(XIRR) - 자산 현황 종목 줄과 같은 계산 · 같은 입력. 기간 선택과 무관하다(scopeNote 로 밝힘).
		     사용자 선택 2026-09-17: 합산 상자 아래 작은 줄은 눈에 띄지 않아 카드로 올렸다. 지금 보유하지 않거나 최초 매수일을 모르면 그리지 않는다.
		     1 년이 안 된 보유는 확대된 값이라 무엇을 환산했는지 적는다(실측 2026-09-14: 117 일 -6.0% -> 연 -17.5%). --%>
		@if(holdingReturnShown)
		!{String holdingSinceMessage = MessageUtil.getMessage("stock.asset.status.cell.holding.since"); int holdingSinceAt = holdingSinceMessage.indexOf("{0}");}
		<div class="contents" data-holding-return data-short-term="${String.valueOf(holdingReturn.shortTerm())}" data-holding-days="${holdingReturn.holdingDays()}">
		@template._components.ui.statCard(
			label = MessageUtil.getMessage("stock.asset.status.cell.annualized.name"),
			value = holdingAnnualizedText.isEmpty() ? "-" : holdingAnnualizedText,
			valueTitle = holdingAnnualizedText.isEmpty() ? MessageUtil.getMessage("stock.item.detail.annualized.none") : "",
			scopeNote = MessageUtil.getMessage("stock.item.detail.holding.basis"),
			sub = holdingPeriodText + " · " + (holdingReturn.shortTerm()
				? java.text.MessageFormat.format(MessageUtil.getMessage("stock.asset.status.cell.annualized.short"), String.valueOf(holdingReturn.holdingDays()))
				: (holdingSinceAt >= 0 ? holdingSinceMessage.substring(0, holdingSinceAt) : holdingSinceMessage + " ")),
			subDate = holdingReturn.shortTerm() ? "" : holdingFirstBuyDate.toString(),
			subDateAfter = holdingReturn.shortTerm() || holdingSinceAt < 0 ? "" : holdingSinceMessage.substring(holdingSinceAt + 3),
			tone = holdingReturn.annualizedPct() == null ? "" : (holdingReturn.annualizedPct().signum() >= 0 ? "text-profit" : "text-loss"),
			amount = false)
		</div>
		@endif
	</div>

	<%-- 기간별 손익 - 고른 기간을 달(또는 해)로 쪼개 언제 벌고 언제 잃었는지 보여준다.
	     위의 합산 손익과 같은 쪼갬(평가 변동 / 실현+배당)을 써서 두 자리가 같은 뜻을 말한다. --%>
	@template.stock.htmx.fragments.periodBreakdownTable(periodBreakdown = periodBreakdown, periodBreakdownNote = periodBreakdownNote)

	<%-- 주가 추이 - 보유 평가액 추이만으로는 <b>주가 자체</b>를 볼 수 없다. 평가액은 수량이 바뀌면 같이
	     움직이므로, 산 뒤로 주가가 어떻게 됐는지는 그 선에서 읽어낼 수 없다.
	     점선은 <b>지금</b> 평균단가라 과거 구간에도 같은 값이 그어진다(평단은 매매마다 달라졌다).
	     그래도 "지금 내 단가가 이 구간 어디쯤인지" 를 한눈에 보여주므로 함께 긋고, 이름으로 밝힌다. --%>
	@if(priceHistory != null && !priceHistory.isEmpty())
		<div>
			<h2 class="text-lg font-semibold mb-2 text-base-content/70">${MessageUtil.getMessage("stock.item.detail.price.chart.title")}</h2>
			<div class="bg-base-100 rounded-box border border-base-200 p-4">
				<div class="h-64"><canvas id="stockPriceChart" role="img" aria-label="${MessageUtil.getMessage("stock.item.detail.price.chart.title")}" class="amount-chart"></canvas></div>
			</div>
			@if(priceChart != null && !priceChart.isEmpty())
			<%-- 캔들 + 그 시점 평균 단가(사용자 요청 2026-10-02). 점선은 그 날 보유분의 평단이라 보유가 없던 기간에는 끊긴다 -
			     이 기간에 보유가 한 번도 없었으면 점선을 설명하지 않는다(없는 선을 찾게 된다). 봉 단위(일 · 주 · 월)는 브라우저가 정해 앞에 적는다. --%>
			!{boolean candleHasAverage = priceChart.stream().anyMatch(p -> p.averageCost() != null && p.averageCost().signum() != 0);}
			<p class="text-xs text-base-content/60 mt-1"><span data-candle-unit></span>${MessageUtil.getMessage(candleHasAverage ? "stock.item.detail.price.chart.candle.desc" : "stock.item.detail.price.chart.candle.desc.nocost")}</p>
		</div>
		<script nonce="${CspNonceHolder.getNonce()}">
		(function() {
			if (typeof window.ensureStockCharts !== 'function') return;
			var series = $unsafe{ChartSeriesJs.candleSeries(priceChart, trades, zone, timeSeries)};
			window.ensureStockCharts(function() {
				if (!window.StockCharts || !window.StockCharts.createCandleChart) return;
				var __priceChart = window.StockCharts.createCandleChart('stockPriceChart', series, {
					openLabel: '${MessageUtil.getMessage("stock.item.detail.price.chart.open")}',
					highLabel: '${MessageUtil.getMessage("stock.item.detail.price.chart.high")}',
					lowLabel: '${MessageUtil.getMessage("stock.item.detail.price.chart.low")}',
					closeLabel: '${MessageUtil.getMessage("stock.item.detail.price.chart.close")}',
					avgLabel: '${MessageUtil.getMessage("stock.item.detail.price.chart.average.at")}',
					avgBelowLabel: '${MessageUtil.getMessage("stock.item.detail.price.chart.average.below")}',
					avgAboveLabel: '${MessageUtil.getMessage("stock.item.detail.price.chart.average.above")}',
					buyLabel: '${MessageUtil.getMessage("stock.trade.label.buy")}',
					sellLabel: '${MessageUtil.getMessage("stock.trade.label.sell")}',
					sharePattern: '${MessageUtil.getMessage("stock.activity.label.shares")}',
					shareOnePattern: '${MessageUtil.getMessage("stock.activity.label.shares.one")}',
					distLabel: '${MessageUtil.getMessage("stock.item.detail.price.chart.distribution")}',
					holdingValueLabel: '${MessageUtil.getMessage("stock.item.detail.chart.value")}',
					holdingCostLabel: '${MessageUtil.getMessage("stock.item.detail.chart.cost")}',
					holdingProfitLabel: '${MessageUtil.getMessage("stock.profit.unrealized")}',
					maxLabel: '${MessageUtil.getMessage("stock.chart.range.max")}',
					minLabel: '${MessageUtil.getMessage("stock.chart.range.min")}'
				});
				var unitEl = document.querySelector('[data-candle-unit]');
				var unitText = {
					day: '${MessageUtil.getMessage("stock.item.detail.price.chart.unit.day")}',
					week: '${MessageUtil.getMessage("stock.item.detail.price.chart.unit.week")}',
					month: '${MessageUtil.getMessage("stock.item.detail.price.chart.unit.month")}'
				};
				if (unitEl && __priceChart && unitText[__priceChart.$candleUnit]) unitEl.textContent = unitText[__priceChart.$candleUnit] + ' · ';
				setTimeout(function() { try { if (__priceChart) window.StockCharts.resizeIfChanged(__priceChart); } catch (e) {} }, 60);
			});
		})();
		</script>
			@else
			<%-- 점선을 안 그은 경우에는 점선을 설명하면 안 된다 - 없는 선을 찾게 된다. --%>
			!{String priceChartDescKey = averageBuyPrice != null && averageBuyPrice.signum() != 0
				? "stock.item.detail.price.chart.desc"
				: "stock.item.detail.price.chart.desc.nocost";}
			<p class="text-xs text-base-content/60 mt-1">${MessageUtil.getMessage(priceChartDescKey)}</p>
		</div>
		<script nonce="${CspNonceHolder.getNonce()}">
		(function() {
			if (typeof window.ensureStockCharts !== 'function') return;
			var series = $unsafe{ChartSeriesJs.priceSeries(priceHistory, averageBuyPrice)};
			window.ensureStockCharts(function() {
				if (!window.StockCharts || !window.StockCharts.createHoldingsChart) return;
				var __priceChart = window.StockCharts.createHoldingsChart('stockPriceChart', series, {
					valueLabel: '${MessageUtil.getMessage("stock.item.detail.price.chart.close")}',
					costLabel: '${MessageUtil.getMessage("stock.item.detail.price.chart.average")}',
					maxLabel: '${MessageUtil.getMessage("stock.chart.range.max")}',
					minLabel: '${MessageUtil.getMessage("stock.chart.range.min")}'
				}, { animate: true, showMarkers: false });
				setTimeout(function() { try { if (__priceChart) window.StockCharts.resizeIfChanged(__priceChart); } catch (e) {} }, 60);
			});
		})();
		</script>
			@endif
	@else
		<%-- 자료가 없으면 구역째 사라지고 있었다. 실측 2026-09-12: 이 종목은 시세가 2026-04-01 에 끝나
		     최근 3개월 점이 0 인데, 같은 화면의 매매·배당은 "없습니다" 라고 말하는 반면 주가 추이만 말없이 빠졌다
		     - 화면만 봐서는 자료가 없는 것인지 이 화면에 원래 없는 것인지 알 수 없다. --%>
		<div>
			<h2 class="text-lg font-semibold mb-2 text-base-content/70">${MessageUtil.getMessage("stock.item.detail.price.chart.title")}</h2>
			<%-- 빈 기간에서 다음 행동을 준다 - 실측 2026-09-13: 목록 네 화면은 "전체 기간으로 보기" 를 띄우는데
			     상세 두 화면의 기간 안내 다섯 자리만 평문 상자라 막다른 길이었다(프리셋 '전체' 단추는 있었다). --%>
			@template._components.ui.emptyState(message = MessageUtil.getMessage("stock.item.detail.empty.price.history"), boxed = true, padding = "py-6", widenRangeAction = !"all".equals(rangeMode))
		</div>
	@endif

	<!-- 보유 평가액·원가 추이 -->
	<%-- 캔들이 있으면 따로 두지 않는다(사용자 요청 2026-10-07: "굳이 2개가 나뉘어있을 필요가 없을거 같아") - 그 날 보유 평가액 · 원가 ·
	     평가손익은 주가 추이 캔들의 툴팁에 있다. 시세 봉(OHLC)이 없어 선 차트로 그리는 종목에서만 이 차트를 그대로 둔다. --%>
	!{boolean holdingsMergedIntoCandles = priceChart != null && !priceChart.isEmpty();}
	@if(!holdingsMergedIntoCandles)
	@if(timeSeries != null && !timeSeries.isEmpty() && chartFormatter != null)
		<div>
			<h2 class="text-lg font-semibold mb-2 text-base-content/70">${MessageUtil.getMessage("stock.item.detail.chart.title")}</h2>
			<div class="bg-base-100 rounded-box border border-base-200 p-4">
				<div class="h-64"><canvas id="stockDetailChart" role="img" aria-label="${MessageUtil.getMessage("stock.item.detail.chart.title")}" class="amount-chart"></canvas></div>
			</div>
		</div>
		<script nonce="${CspNonceHolder.getNonce()}">
		(function() {
			if (typeof window.ensureStockCharts !== 'function') return;
			var series = $unsafe{ChartSeriesJs.holdingsSeries(timeSeries, chartFormatter)};
			window.ensureStockCharts(function() {
				if (!window.StockCharts || !window.StockCharts.createHoldingsChart) return;
				var __detailChart = window.StockCharts.createHoldingsChart('stockDetailChart', series, {
					valueLabel: '${MessageUtil.getMessage("stock.item.detail.chart.value")}',
					costLabel: '${MessageUtil.getMessage("stock.item.detail.chart.cost")}',
					profitLabel: '${MessageUtil.getMessage("stock.profit.unrealized")}',
					maxLabel: '${MessageUtil.getMessage("stock.chart.range.max")}',
					minLabel: '${MessageUtil.getMessage("stock.chart.range.min")}'
				}, { animate: true, showMarkers: true });
				setTimeout(function() { try { if (__detailChart) window.StockCharts.resizeIfChanged(__detailChart); } catch (e) {} }, 60);
				setTimeout(function() { try { if (__detailChart) window.StockCharts.resizeIfChanged(__detailChart); } catch (e) {} }, 250);
			});
		})();
		</script>
	@else
		<%-- 자료가 없으면 구역째 사라지고 있었다.
		     실측 2026-09-16: 거래가 없는 43 종목(86 중)에서 이 제목이 통째로 빠졌다.
		     형제 구역(매매·배당·보유·주가 추이)은 모두 "없습니다" 라고 말한다. --%>
		<div>
			<h2 class="text-lg font-semibold mb-2 text-base-content/70">${MessageUtil.getMessage("stock.item.detail.chart.title")}</h2>
			@template._components.ui.emptyState(message = MessageUtil.getMessage("stock.item.detail.empty.valuation.series"), boxed = true, padding = "py-6", widenRangeAction = !"all".equals(rangeMode))
		</div>
	@endif
	@endif

	<!-- 계좌별 보유 현황 -->
	<div>
		<h2 class="text-lg font-semibold mb-2 text-base-content/70">${MessageUtil.getMessage("stock.item.detail.account.holdings.title")}</h2>
		<%-- 이 구역만 범용 "데이터가 없습니다" 를 썼다 - 실측 2026-09-13: 86 종목 중 77 종목(전량 매도 34 · 무거래 43)이
	     그 문구를 냈고,
	     같은 성격인 계좌 상세의 '보유 종목' 은 이미 "이 계좌의 보유 종목이 없습니다" 라고 말한다.
	     자료가 유실된 것과 보유가 없는 것은 다르다.
	     형제 문구(매매 · 배당 · 주가)와 달리 "이 기간에는" 을 붙이지 않는다 - 이 표는 기간과 무관한 현재 보유 스냅샷이다. --%>
		@if(accountHoldings == null || accountHoldings.isEmpty())
			<div class="bg-base-200 rounded-box p-6 text-center text-sm text-base-content/60">${MessageUtil.getMessage("stock.item.detail.empty.account.holdings")}</div>
		@else
			@template._components.ui.tableCard(body = @`
				<div class="overflow-x-auto">
				<table class="table table-sm w-full table-pin-rows pin-top-nav" data-sortable aria-label="${MessageUtil.getMessage("stock.item.detail.account.holdings.title")}">
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
								<th scope="row" class="font-semibold">
									@if(holding.accountId() != null)
										<a href="${StockDetailLinkUtil.account(holding.accountId().toString())}" class="link link-hover">${holding.accountName() != null ? holding.accountName() : "-"}</a>
									@else
										${holding.accountName() != null ? holding.accountName() : "-"}
									@endif
								</th>
								<td class="text-right font-mono amount-value">${String.format("%,d", holding.holdingQuantity())}</td>
								<td class="text-right font-mono amount-value">${String.format("%,d", StockFormatUtil.displayWon(holding.evaluationAmount()))}</td>
								<td class="text-right font-mono amount-value ${acctEvalProfit >= 0 ? "text-profit" : "text-loss"}">${StockFormatUtil.signedWon(acctEvalProfit)} <span class="text-xs">(${StockFormatUtil.signedPct(acctEvalRatePct, 1)})</span></td>
								<td class="text-right font-mono text-base-content/70">${StockFormatUtil.pct(acctWeightPct, 1)}</td>
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
		<h2 class="text-lg font-semibold mb-2 text-base-content/70">${MessageUtil.getMessage("stock.page.trade.title")}</h2>
		@if(trades == null || trades.isEmpty())
			<%-- 빈 기간에서 다음 행동을 준다 - 실측 2026-09-13: 목록 네 화면은 "전체 기간으로 보기" 를 띄우는데
			     상세 두 화면의 기간 안내 다섯 자리만 평문 상자라 막다른 길이었다(프리셋 '전체' 단추는 있었다). --%>
			@template._components.ui.emptyState(message = MessageUtil.getMessage("stock.item.detail.empty.trades"), boxed = true, padding = "py-6", widenRangeAction = !"all".equals(rangeMode))
		@else
			@template._components.ui.tableCard(body = @`
				<div class="overflow-x-auto table-head-sticky-xl">
				<table class="table table-sm w-full table-pin-rows pin-top-nav" data-sortable aria-label="${MessageUtil.getMessage("stock.page.trade.title")}">
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
								<td class="tabular-nums whitespace-nowrap date-whole">${trade.tradeDate() != null ? trade.tradeDate().atZone(zone).toLocalDate().toString() : "-"}</td>
								<td><span class="badge badge-sm ${isBuy ? "badge-error badge-outline" : "badge-info badge-outline"}">${isBuy ? MessageUtil.getMessage("stock.trade.label.buy") : MessageUtil.getMessage("stock.trade.label.sell")}</span></td>
								<td class="text-right font-mono amount-value">${String.format("%,d", trade.quantity())}</td>
								<td class="text-right font-mono amount-value">${String.format("%,d", StockFormatUtil.displayWon(trade.price()))}</td>
								<td class="text-right font-mono amount-value">${String.format("%,d", StockFormatUtil.displayWon(trade.amount()))}</td>
								<td class="text-right font-mono amount-value hidden sm:table-cell opacity-60">${String.format("%,d", fees)}</td>
								<td class="text-right font-mono amount-value ${!isBuy && trade.realizedProfit() != null && trade.realizedProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : (!isBuy && trade.realizedProfit() != null ? "text-loss" : "opacity-40")}" title="${!isBuy && trade.realizedProfit() != null ? null : MessageUtil.getMessage("stock.trade.realized.none.buy")}" aria-label="${!isBuy && trade.realizedProfit() != null ? null : MessageUtil.getMessage("stock.trade.realized.none.buy")}">${!isBuy && trade.realizedProfit() != null ? StockFormatUtil.signedWon(StockFormatUtil.displayWon(trade.realizedProfit())) : "-"}</td>
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
		<h2 class="text-lg font-semibold mb-2 text-base-content/70">${MessageUtil.getMessage("stock.page.dividend.title")}</h2>
		@if(dividends == null || dividends.isEmpty())
			<%-- 빈 기간에서 다음 행동을 준다 - 실측 2026-09-13: 목록 네 화면은 "전체 기간으로 보기" 를 띄우는데
			     상세 두 화면의 기간 안내 다섯 자리만 평문 상자라 막다른 길이었다(프리셋 '전체' 단추는 있었다). --%>
			@template._components.ui.emptyState(message = MessageUtil.getMessage("stock.item.detail.empty.dividends"), boxed = true, padding = "py-6", widenRangeAction = !"all".equals(rangeMode))
		@else
			@template._components.ui.tableCard(body = @`
				<div class="overflow-x-auto table-head-sticky-xl">
				<table class="table table-sm w-full table-pin-rows pin-top-nav" data-sortable aria-label="${MessageUtil.getMessage("stock.page.dividend.title")}">
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
							!{ boolean dividendTaxZero = dividend.tax() == null || dividend.tax().signum() == 0; }
							<tr>
								<td class="tabular-nums whitespace-nowrap date-whole">${dividend.payDate() != null ? dividend.payDate().atZone(zone).toLocalDate().toString() : "-"}</td>
								<td class="text-right font-mono amount-value hidden sm:table-cell opacity-70">${String.format("%,d", StockFormatUtil.displayWon(dividend.grossAmount()))}</td>
								<td class="text-right font-mono amount-value hidden sm:table-cell opacity-60" title="${dividendTaxZero ? zeroAmountTitle : null}" aria-label="${dividendTaxZero ? zeroAmountTitle : null}">${dividendTaxZero ? "-" : String.format("%,d", StockFormatUtil.displayWon(dividend.tax()))}</td>
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
