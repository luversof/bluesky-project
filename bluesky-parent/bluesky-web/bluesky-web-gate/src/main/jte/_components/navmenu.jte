@param String classAppend = ""
@param String menu = "main"

<ul class="menu p-0 ${classAppend}">
	@for(var menuItem : net.luversof.web.gate.util.ThymeleafUtil.getMenuList(menu))
		@if(menuItem instanceof net.luversof.web.common.menu.domain.Menu m)
			@if(m.isDisplay())
				<li>
					<a class="font-bold h-full rounded-none flex ${m.isCurrentMenu() ? "menu-active" : ""}" href="${m.getUrl()}">
						${m.getName()}
					</a>
				</li>
			@endif
		@endif
	@endfor
</ul>