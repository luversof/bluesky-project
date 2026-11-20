@import java.math.BigDecimal
@import java.util.function.Function

<%-- 시황 분석 예측 한 칸: 방향 · 확신도 배지(적중 초록 · 실패 빨강 · 채점 전 색 없음, 마우스를 올리면 근거)와 실제 방향 · 변화율. 회차 비교 · 일자별 적중 표가 같이 쓴다. --%>
@param String direction
@param int confidence
@param String rationale = null
@param Boolean hit = null
@param String actualDirection = null
@param BigDecimal actualChangePct = null
@param Function<BigDecimal, String> pct

@template._components.ui.tooltip(text = rationale == null ? direction : rationale, placement = "top", content = @`<span class="badge badge-sm badge-outline${hit == null ? "" : (hit ? " badge-success" : " badge-error")}">${direction} ${confidence}</span>`)
@if(actualDirection != null)
	<span class="text-xs opacity-70">${actualDirection} ${pct.apply(actualChangePct)}</span>
@endif
