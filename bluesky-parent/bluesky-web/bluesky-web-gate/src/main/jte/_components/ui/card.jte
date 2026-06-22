@param gg.jte.Content body
@param String title = ""
@param String id = ""
@param String cssClass = ""
@param String bodyClass = "p-4"

<div id="${id}" class="card bg-base-100 border border-base-200 rounded-box shadow-sm ${cssClass}">
	<div class="card-body ${bodyClass}">
		@if(title != null && !title.isEmpty())
		<h2 class="card-title text-base mb-2">${title}</h2>
		@endif
		${body}
	</div>
</div>
