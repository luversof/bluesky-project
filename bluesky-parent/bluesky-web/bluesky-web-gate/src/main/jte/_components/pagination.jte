@import net.luversof.web.gate.interceptor.PaginationInterceptor.Pagination

@param Pagination pagination

<div class="w-full join place-content-center">
	<button 
		class="join-item btn btn-sm btn-ghost navButton ${!pagination.getFirstNav().isActive() ? "btn-disabled" : ""}"
		data-page="${pagination.getFirstNav().page()}"
	>
		≪
	</button>
	<button 
		class="join-item btn btn-sm btn-ghost navButton ${!pagination.getPrevNav().isActive() ? "btn-disabled" : ""}" 
		data-page="${pagination.getPrevNav().page()}"
	>
		＜
	</button>
	
	@for(var nav : pagination.getNavList())
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
		＞
	</button>
	
	<button 
		class="join-item btn btn-sm btn-ghost navButton ${!pagination.getLastNav().isActive() ? "btn-disabled" : ""}"
 		data-page="${pagination.getLastNav().page()}"
	>
		≫
	</button>
</div>
