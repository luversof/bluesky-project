@import org.springframework.data.domain.Page
@import net.luversof.web.dynamiccrud.util.TemplateUtil
@import net.luversof.web.common.menu.domain.Pagination

@param Page<?> page

<%-- pagination component --%>

@if(page.getTotalPages() > 1)
!{
        Pagination pagination = TemplateUtil.getPagination(page);
}
<nav class="join" aria-label="페이지">
        <button
                class="join-item btn btn-sm btn-ghost btn-square navButton ${!pagination.getFirstNav().isActive() ? "btn-disabled" : ""}"
                data-page="${pagination.getFirstNav().page()}"
                aria-label="첫 페이지"
        >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-4" aria-hidden="true">
                        <path stroke-linecap="round" stroke-linejoin="round" d="m18.75 4.5-7.5 7.5 7.5 7.5m-6-15L5.25 12l7.5 7.5" />
                </svg>
        </button>
        <button
                class="join-item btn btn-sm btn-ghost btn-square navButton ${!pagination.getPrevNav().isActive() ? "btn-disabled" : ""}"
                data-page="${pagination.getPrevNav().page()}"
                aria-label="이전 페이지"
        >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-4" aria-hidden="true">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5 8.25 12l7.5-7.5" />
                </svg>
        </button>

        @for(Pagination.Nav nav : pagination.getNavList())
                <button
                        class="join-item btn btn-sm btn-ghost min-w-8 px-2 font-normal tabular-nums navButton ${nav.isActive() ? "btn-active" : ""}"
                        data-page="${nav.page()}"
                        aria-current="${nav.isActive() ? "page" : null}"
                >
                        ${nav.page()}
                </button>
        @endfor

        <button
                class="join-item btn btn-sm btn-ghost btn-square navButton ${!pagination.getNextNav().isActive() ? "btn-disabled" : ""}"
                data-page="${pagination.getNextNav().page()}"
                aria-label="다음 페이지"
        >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-4" aria-hidden="true">
                        <path stroke-linecap="round" stroke-linejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
                </svg>
        </button>

        <button
                class="join-item btn btn-sm btn-ghost btn-square navButton ${!pagination.getLastNav().isActive() ? "btn-disabled" : ""}"
                data-page="${pagination.getLastNav().page()}"
                aria-label="마지막 페이지"
        >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-4" aria-hidden="true">
                        <path stroke-linecap="round" stroke-linejoin="round" d="m5.25 4.5 7.5 7.5-7.5 7.5m6-15 7.5 7.5-7.5 7.5" />
                </svg>
        </button>
</nav>
@endif
