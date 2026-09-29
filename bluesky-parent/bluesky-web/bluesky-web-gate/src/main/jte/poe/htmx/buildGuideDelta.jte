@param String label
@param Double pct = null

<%-- 업그레이드 가이드 증감 배지 — 0.05% 미만은 부호 없는 0.0% 중립색(부호 붙은 0 금지), 오르면 초록 · 내리면 빨강(PoB 스탯 비교와 같은 색). --%>
@if(pct != null)
	<span class="whitespace-nowrap font-mono text-xs ${Math.abs(pct) < 0.05 ? "text-base-content/60" : (pct > 0 ? "text-success" : "text-error")}">${label} ${Math.abs(pct) < 0.05 ? "0.0%" : String.format("%+.1f%%", pct)}</span>
@endif
