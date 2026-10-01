@param String title
@param String description = ""
@param String size = "text-xl"
@param gg.jte.Content actions = null

<%-- 자리가 모자라면 오른쪽 동작(다른 계좌 등)을 제목 아래로 내린다 - 줄바꿈이 없으면 제목이 0 폭으로 눌리고 동작이 화면을 밀었다(2026-09-30). --%>
<div class="flex flex-wrap items-start justify-between gap-x-4 gap-y-2 mb-3">
	<div class="min-w-0">
		<%-- 제목에 끊을 자리가 없는 긴 낱말(영어 계좌 이름 등)이 오면 min-w-0 이어도 글자가 상자 밖으로 나갔다(실측 2026-09-30: 영어 320px ·
		     글꼴 200% 계좌 상세 12px). 더 못 줄일 때만 끊는다. --%>
		<h1 class="${size} font-bold wrap-anywhere">${title}</h1>
		@if(description != null && !description.isEmpty())
		<p class="text-sm text-base-content/60 mt-0.5 wrap-anywhere">${description}</p>
		@endif
	</div>
	@if(actions != null)
	<div class="shrink-0">${actions}</div>
	@endif
</div>
