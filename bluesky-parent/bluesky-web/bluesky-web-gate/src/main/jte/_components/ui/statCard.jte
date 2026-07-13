@param String label
@param String value
@param String valueTitle = ""
@param String sub = ""
@param String tone = ""
@param boolean amount = true
@param String href = null

<%-- href 가 있으면 카드 전체가 해당 화면으로 이동하는 링크가 된다 (대시보드 → 상세 drill-down) --%>
@if(href != null)
<a href="${href}" class="block bg-base-200 rounded-box p-3 transition-colors duration-300 hover:bg-base-300 hover:duration-75 group">
	<div class="flex items-center justify-between text-xs text-base-content/60">
		<span>${label}</span>
		<span class="opacity-0 group-hover:opacity-60 transition-opacity">→</span>
	</div>
	<div class="text-lg font-bold font-mono ${amount ? "amount-value" : ""} ${tone}" title="${valueTitle}">${value}</div>
	@if(sub != null && !sub.isEmpty())
	<div class="text-xs ${tone.isEmpty() ? "text-base-content/50" : tone}">${sub}</div>
	@endif
</a>
@else
<div class="bg-base-200 rounded-box p-3">
	<div class="text-xs text-base-content/60">${label}</div>
	<div class="text-lg font-bold font-mono ${amount ? "amount-value" : ""} ${tone}" title="${valueTitle}">${value}</div>
	@if(sub != null && !sub.isEmpty())
	<div class="text-xs ${tone.isEmpty() ? "text-base-content/50" : tone}">${sub}</div>
	@endif
</div>
@endif
