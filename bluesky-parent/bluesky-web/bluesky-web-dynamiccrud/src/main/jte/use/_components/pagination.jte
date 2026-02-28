@import org.springframework.data.domain.Page
@import net.luversof.web.dynamiccrud.thymeleaf.util.ThymeleafUtil
@import net.luversof.web.common.menu.domain.Pagination

@param Page<?> page

<%-- pagination component --%>

@if(page.getTotalPages() > 1)
!{
        Pagination pagination = ThymeleafUtil.getPagination(page);
}
<div class="w-full join place-content-center">
        <button
                class="join-item btn btn-sm btn-ghost navButton ${!pagination.getFirstNav().isActive() ? "btn-disabled" : ""}"
                data-page="${pagination.getFirstNav().page()}"
        >
                &laquo;
        </button>
        <button
                class="join-item btn btn-sm btn-ghost navButton ${!pagination.getPrevNav().isActive() ? "btn-disabled" : ""}"
                data-page="${pagination.getPrevNav().page()}"
        >
                &lsaquo;
        </button>

        @for(Pagination.Nav nav : pagination.getNavList())
                <button
                        class="join-item btn btn-sm btn-ghost navButton ${nav.isActive() ? "btn-active" : ""}"
                        data-page="${nav.page()}"
                >
                        ${nav.page()}
                </button>
        @endfor

        <button
                class="join-item btn btn-sm btn-ghost navButton ${!pagination.getNextNav().isActive() ? "btn-disabled" : ""}"
                data-page="${pagination.getNextNav().page()}"
        >
                &rsaquo;
        </button>

        <button
                class="join-item btn btn-sm btn-ghost navButton ${!pagination.getLastNav().isActive() ? "btn-disabled" : ""}"
                data-page="${pagination.getLastNav().page()}"
        >
                &raquo;
        </button>
</div>
@endif
