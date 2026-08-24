@import net.luversof.web.common.menu.domain.Pagination

@param Pagination pagination
@param String url
@param String includeSelector
@param String targetSelector

<%-- 페이지 수가 많으면 버튼 줄이 화면보다 넓어진다. 가운데 정렬만 걸어두면 그 줄이
     문서를 좌우로 밀어 페이지 전체에 가로 스크롤이 생긴다(실측: 375px 에서 41px).
     안쪽 줄에 min-w-max 를 두면, 들어갈 때는 폭을 다 써서 가운데 정렬되고
     넘칠 때는 내용 폭이 되어 이 줄 안에서만 가로로 스크롤된다.
     (nav.join 은 inline-flex 라 mx-auto 로는 가운데 정렬이 되지 않는다.) --%>
<div class="overflow-x-auto border-t border-base-200 bg-base-100">
	<div class="flex justify-center min-w-max p-2">
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
</div>
