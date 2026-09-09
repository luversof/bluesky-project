@param gg.jte.Content body
@param String title = ""
@param String id = ""
@param String cssClass = ""
@param String bodyClass = "p-4"

<%-- id 를 안 주면 속성을 아예 내지 않는다. JTE 는 null 만 빼고 "" 는 id="" 로 내는데, 빈 id 는 HTML 에서 무효이고
     13화면 실측(2026-09-09)에서 화면마다 2~9개씩 겹치는 id="" 가 이 카드에서 나왔다. --%>
<div id="${id == null || id.isEmpty() ? null : id}" class="card bg-base-100 border border-base-200 rounded-box shadow-sm ${cssClass}">
	<div class="card-body ${bodyClass}">
		@if(title != null && !title.isEmpty())
		<h2 class="card-title text-base mb-2">${title}</h2>
		@endif
		${body}
	</div>
</div>
