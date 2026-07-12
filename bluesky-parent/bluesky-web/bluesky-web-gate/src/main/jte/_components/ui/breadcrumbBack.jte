@import net.luversof.web.gate.util.CspNonceHolder
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
	<button type="button" class="btn btn-ghost btn-xs gap-1 shrink-0" data-breadcrumb-back>
		<svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
			<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
		</svg>
		${backLabel}
	</button>
</div>

<script nonce="${CspNonceHolder.getNonce()}">
	// 이 컴포넌트는 여러 페이지에서 include되고 htmx 스왑으로 재렌더될 수 있으므로 document 위임 + 전역 1회 가드로 바인딩한다.
	if (!window.__breadcrumbBackBound) {
		window.__breadcrumbBackBound = true;
		document.addEventListener('click', function(event) {
			var button = event.target && event.target.closest ? event.target.closest('[data-breadcrumb-back]') : null;
			if (button) {
				history.back();
			}
		});
	}
</script>
