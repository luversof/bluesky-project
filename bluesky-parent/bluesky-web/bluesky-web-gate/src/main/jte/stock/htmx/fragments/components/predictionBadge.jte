@import java.math.BigDecimal
@import java.util.function.Function
@import io.github.luversof.boot.context.support.MessageUtil

<%-- 시황 분석 예측 한 칸: 맞음 · 틀림 · 채점 전을 글자와 기호로 먼저(사용자 요청 2026-11-22: 초록 · 빨강 색만으로는 알기 어렵다),
     그 아래 "예상 방향 확신도 → 실제 방향 변화율". 마우스를 올리면 근거. 회차 비교 · 일자별 적중 · 같은 날짜 버전 비교가 같이 쓴다. --%>
@param String direction
@param int confidence
@param String rationale = null
@param Boolean hit = null
@param String actualDirection = null
@param BigDecimal actualChangePct = null
@param Function<BigDecimal, String> pct

!{Function<String, String> dir = d -> d == null ? "-" : switch (d) { case "UP", "DOWN", "FLAT" -> MessageUtil.getMessage("stock.analysis.dir." + d); default -> d; };}
@if(hit == null)
	<span class="text-xs text-base-content/60">${MessageUtil.getMessage("stock.analysis.hit.pending")}</span>
@elseif(hit)
	<span class="text-sm font-bold text-success">✓ ${MessageUtil.getMessage("stock.analysis.hit.yes")}</span>
@else
	<span class="text-sm font-bold text-error">✗ ${MessageUtil.getMessage("stock.analysis.hit.no")}</span>
@endif
<div class="text-xs">
	@template._components.ui.tooltip(text = rationale == null ? direction : rationale, placement = "top", content = @`<span>${MessageUtil.getMessage("stock.analysis.hit.expected")} <b>${dir.apply(direction)}</b> <span class="opacity-60">${confidence}</span></span>`)
	@if(actualDirection != null)
		<span class="opacity-80">→ ${MessageUtil.getMessage("stock.analysis.hit.actual")} <b>${dir.apply(actualDirection)}</b> ${pct.apply(actualChangePct)}</span>
	@endif
</div>
