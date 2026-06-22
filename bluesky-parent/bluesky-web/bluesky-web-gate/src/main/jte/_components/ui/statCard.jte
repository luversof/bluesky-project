@param String label
@param String value
@param String valueTitle = ""
@param String sub = ""
@param String tone = ""
@param boolean amount = true

<div class="bg-base-200 rounded-box p-3">
	<div class="text-xs text-base-content/60">${label}</div>
	<div class="text-lg font-bold font-mono ${amount ? "amount-value" : ""} ${tone}" title="${valueTitle}">${value}</div>
	@if(sub != null && !sub.isEmpty())
	<div class="text-xs ${tone.isEmpty() ? "text-base-content/50" : tone}">${sub}</div>
	@endif
</div>
