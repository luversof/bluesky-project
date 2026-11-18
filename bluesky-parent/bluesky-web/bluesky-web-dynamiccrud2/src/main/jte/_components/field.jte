<%-- 입력 한 칸: 라벨 + 입력(content) + 도움말. 입력의 id 를 forId 로 넘겨 라벨과 잇는다 --%>
@param String label
@param String forId
@param gg.jte.Content content
@param String hint = null
@param boolean required = false
@param String cssClass = ""

<div class="${cssClass}">
	<label class="label" for="${forId}">${label}@if(required) <span class="text-error">*</span>@endif</label>
	${content}
	@if(hint != null)<p class="hint mt-1">${hint}</p>@endif
</div>
