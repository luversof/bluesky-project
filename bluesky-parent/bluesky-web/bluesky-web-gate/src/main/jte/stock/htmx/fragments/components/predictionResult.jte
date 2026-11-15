@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.util.function.Function
@import net.luversof.web.gate.stock.httpexchange.StockAnalysisClient

<%-- 시황 분석 예측 한 줄의 결과(대기 / 적중 · 실패 + 실제 방향 · 변화율, 늦은 예측 표시). 최근 예측 · 종목별 예측 표가 같이 쓴다. --%>
@param StockAnalysisClient.RecentPrediction p
@param Function<BigDecimal, String> pct

@if(p.hit() == null)
	<span class="badge badge-sm badge-outline">${MessageUtil.getMessage("stock.analysis.result.pending")}</span>
@else
	<span class="badge badge-sm badge-outline ${p.hit() ? "badge-success" : "badge-error"}">${MessageUtil.getMessage(p.hit() ? "stock.analysis.result.hit" : "stock.analysis.result.miss")}</span>
	<span class="text-xs">${p.actualDirection()} ${pct.apply(p.actualChangePct())}</span>
	@if(Boolean.TRUE.equals(p.late()))
		<span class="badge badge-warning badge-sm badge-outline">${MessageUtil.getMessage("stock.analysis.result.late")}</span>
	@endif
@endif
