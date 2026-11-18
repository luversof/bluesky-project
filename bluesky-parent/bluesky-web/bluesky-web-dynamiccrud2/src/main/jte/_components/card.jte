<%-- 구역 카드. 제목 줄 오른쪽에 actions(단추 · 안내)를 둔다. 제목이 없으면 제목 줄도 없다 --%>
@param gg.jte.Content body
@param String title = null
@param gg.jte.Content actions = null
@param String cssClass = "mb-4"

<section class="card ${cssClass}">
	@if(title != null || actions != null)
		<div class="flex flex-wrap items-center gap-2 mb-2">
			@if(title != null)<h2 class="font-semibold">${title}</h2>@endif
			@if(actions != null)<div class="ml-auto flex flex-wrap items-center gap-2">${actions}</div>@endif
		</div>
	@endif
	${body}
</section>
