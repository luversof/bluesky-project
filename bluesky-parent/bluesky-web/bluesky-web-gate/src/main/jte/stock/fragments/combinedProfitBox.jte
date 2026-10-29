@import net.luversof.web.gate.stock.util.StockFormatUtil
@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal

<%-- 합산 손익 상자 - 종목 상세 · 계좌 상세 공용(계좌 상세는 2026-10-29 사용자 결정 "1. a": 실현은 카드 금액과 같은 기록값).
     평가 몫은 현재 시점 평가손익이 아니라 기간 평가 변동(기말 - 기초)이다 - 현재 시점 값을 더하면 아래 '기간별 손익' 표의 합과
     맞지 않았다(실측 2026-09-01 삼성전자 '올해' 2.9 억 차이). 평가 변동 + 실현 + 배당은 api-stock 요약의 periodProfit 과 같다
     (종목: 실측 2026-09-01 차이 0 · 계좌: 실측 2026-10-29 여섯 계좌 모두 차이 0). --%>
@param BigDecimal periodUnrealizedDelta = null
@param BigDecimal realizedProfit = null
@param BigDecimal totalDividend = null
<%-- 비율은 자산 성장 화면과 같은 정의(기초 평가액 + 기간 중 순유입 원금 대비). 없으면 금액만. --%>
@param Double periodProfitRatePct = null
<%-- 세 몫(평가 변동 · 실현 · 배당)의 비율 분모(사용자 요청 2026-09-17 "수익률 퍼센트도") - 합산 비율과 같은 분모라야 셋을 더한 값이 합산 비율이 된다. --%>
@param BigDecimal periodCapitalBase = null
<%-- 상자 아래에 붙일 화면별 줄(종목 상세의 배당 상쇄 줄). --%>
@param gg.jte.Content extra = null

!{
	BigDecimal unrealizedDelta = periodUnrealizedDelta != null ? periodUnrealizedDelta : BigDecimal.ZERO;
	BigDecimal combinedProfit = unrealizedDelta
		.add(realizedProfit != null ? realizedProfit : BigDecimal.ZERO)
		.add(totalDividend != null ? totalDividend : BigDecimal.ZERO);
	boolean combinedPositive = combinedProfit.signum() >= 0;
	String deltaColor = unrealizedDelta.signum() >= 0 ? "text-profit" : "text-loss";
	String realizedColor = realizedProfit != null && realizedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss";
	boolean combinedRateAvailable = periodProfitRatePct != null;
	double combinedRatePct = combinedRateAvailable ? periodProfitRatePct : 0.0;
	boolean componentRateAvailable = combinedRateAvailable && periodCapitalBase != null && periodCapitalBase.signum() > 0;
	double unrealizedDeltaRatePct = componentRateAvailable ? unrealizedDelta.doubleValue() * 100.0 / periodCapitalBase.doubleValue() : 0.0;
	double realizedRatePct = componentRateAvailable && realizedProfit != null ? realizedProfit.doubleValue() * 100.0 / periodCapitalBase.doubleValue() : 0.0;
	double dividendRatePct = componentRateAvailable && totalDividend != null ? totalDividend.doubleValue() * 100.0 / periodCapitalBase.doubleValue() : 0.0;
}
<%-- 평가손익·실현손익·배당이 카드로 흩어져 있어 사람이 더해야 알 수 있었다.
     아래 격자의 세 값을 그대로 더한 것이라, 화면에서 눈으로 검산할 수 있게 셋을 함께 적는다. --%>
<div class="mb-3 rounded-2xl border border-base-300/60 bg-base-100/80 px-4 py-3" data-combined-profit-box>
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
	@if(extra != null)${extra}@endif
</div>
