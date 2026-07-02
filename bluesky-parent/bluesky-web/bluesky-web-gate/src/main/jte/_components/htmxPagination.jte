@import net.luversof.web.common.menu.domain.Pagination

@param Pagination pagination
@param String url
@param String includeSelector
@param String targetSelector

<div class="flex justify-center p-2 border-t border-base-200 bg-base-100">
	<nav class="join" aria-label="pagination">
		<button class="join-item btn btn-sm ${!pagination.getFirstNav().isActive() ? "btn-disabled" : ""}"
				disabled="${!pagination.getFirstNav().isActive()}"
				hx-get="${url}" hx-include="${includeSelector}"
				hx-vals='{"page": ${pagination.getFirstNav().page()}}'
				hx-target="${targetSelector}" hx-select="${targetSelector}" hx-swap="outerHTML">&laquo;</button>
		<button class="join-item btn btn-sm ${!pagination.getPrevNav().isActive() ? "btn-disabled" : ""}"
				disabled="${!pagination.getPrevNav().isActive()}"
				hx-get="${url}" hx-include="${includeSelector}"
				hx-vals='{"page": ${pagination.getPrevNav().page()}}'
				hx-target="${targetSelector}" hx-select="${targetSelector}" hx-swap="outerHTML">&lsaquo;</button>
		@for(Pagination.Nav nav : pagination.getNavList())
		<button class="join-item btn btn-sm ${nav.isActive() ? "btn-active" : ""}"
				aria-current="${nav.isActive() ? "page" : "false"}"
				hx-get="${url}" hx-include="${includeSelector}"
				hx-vals='{"page": ${nav.page()}}'
				hx-target="${targetSelector}" hx-select="${targetSelector}" hx-swap="outerHTML">${nav.page()}</button>
		@endfor
		<button class="join-item btn btn-sm ${!pagination.getNextNav().isActive() ? "btn-disabled" : ""}"
				disabled="${!pagination.getNextNav().isActive()}"
				hx-get="${url}" hx-include="${includeSelector}"
				hx-vals='{"page": ${pagination.getNextNav().page()}}'
				hx-target="${targetSelector}" hx-select="${targetSelector}" hx-swap="outerHTML">&rsaquo;</button>
		<button class="join-item btn btn-sm ${!pagination.getLastNav().isActive() ? "btn-disabled" : ""}"
				disabled="${!pagination.getLastNav().isActive()}"
				hx-get="${url}" hx-include="${includeSelector}"
				hx-vals='{"page": ${pagination.getLastNav().page()}}'
				hx-target="${targetSelector}" hx-select="${targetSelector}" hx-swap="outerHTML">&raquo;</button>
	</nav>
</div>
