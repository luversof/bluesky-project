@param String homeUrl = "/stock"
@param String homeLabel = ""
@param String categoryLabel = ""
@param String backLabel = ""

<div class="flex items-center justify-between gap-2 mb-1">
	<div class="breadcrumbs text-sm py-0 min-w-0">
		<ul>
			<li><a href="${homeUrl}" class="link link-hover">${homeLabel}</a></li>
			<li class="text-base-content/60">${categoryLabel}</li>
		</ul>
	</div>
	<button type="button" class="btn btn-ghost btn-xs gap-1 shrink-0" onclick="history.back()">
		<svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
			<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
		</svg>
		${backLabel}
	</button>
</div>
