@param String title
@param String description = ""
@param String size = "text-xl"
@param gg.jte.Content actions = null

<div class="flex items-start justify-between gap-4 mb-3">
	<div class="min-w-0">
		<h1 class="${size} font-bold">${title}</h1>
		@if(description != null && !description.isEmpty())
		<p class="text-sm text-base-content/60 mt-0.5">${description}</p>
		@endif
	</div>
	@if(actions != null)
	<div class="shrink-0">${actions}</div>
	@endif
</div>
