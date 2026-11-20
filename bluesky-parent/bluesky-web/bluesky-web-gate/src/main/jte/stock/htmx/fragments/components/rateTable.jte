@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.util.List
@import java.util.function.Function
@import net.luversof.web.gate.stock.httpexchange.StockAnalysisClient
@import net.luversof.web.gate.stock.support.StockViewSupport

<%-- 시황 분석 적중률 표(묶음 · 기간 · 채점 수 · 적중률 · 기준선 · edge). 분석 화면의 적중률 카드들과 종목별 예측 카드가 같이 쓴다. --%>
@param List<StockAnalysisClient.RateRow> rows
@param String label
@param Function<BigDecimal, String> rate

@if(rows == null || rows.isEmpty())
	<p class="text-sm opacity-70">${MessageUtil.getMessage("stock.analysis.empty")}</p>
@else
	<div class="overflow-x-auto">
		<table class="table data-table w-full" aria-label="${label}">
			<thead><tr>
				<th scope="col">${MessageUtil.getMessage("stock.analysis.col.group")}</th>
				<th scope="col">${MessageUtil.getMessage("stock.analysis.col.horizon")}</th>
				<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.col.graded")}</th>
				<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.col.hitRate")}</th>
				<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.col.alwaysUp")}</th>
				<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.col.trend")}</th>
				<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.col.majority")}</th>
				<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.col.edge")}</th>
			</tr></thead>
			<tbody>
				@for(var r : rows)
					<%-- 표본이 적으면 흐리게: 적중률이 우연에 크게 흔들린다. --%>
					<tr class="${r.graded() < 20 ? "opacity-60" : null}">
						<td>${r.group()}</td>
						<td>D+${r.horizonDays()}</td>
						<td class="text-right whitespace-nowrap">${r.graded()}
							@if(r.reanalysisGraded() != null && r.reanalysisGraded() > 0)
								<span class="text-xs opacity-60">${StockViewSupport.msg("stock.analysis.rate.reanalysis", String.valueOf(r.reanalysisGraded()))}</span>
							@endif
						</td>
						<td class="text-right font-semibold">${rate.apply(r.hitRatePct())}</td>
						<td class="text-right">${rate.apply(r.alwaysUpRatePct())}</td>
						<td class="text-right">${rate.apply(r.trendRatePct())}</td>
						<td class="text-right">${rate.apply(r.majorityRatePct())}</td>
						<td class="text-right font-semibold ${r.edgePct() == null ? "" : (r.edgePct().signum() > 0 ? "text-profit" : "text-loss")}">${r.edgePct() == null ? "-" : (r.edgePct().signum() > 0 ? "+" : "") + r.edgePct().toPlainString() + "%p"}</td>
					</tr>
				@endfor
			</tbody>
		</table>
	</div>
	<p class="mt-2 text-xs text-base-content/60">${MessageUtil.getMessage("stock.analysis.rate.note")}</p>
@endif
