@param String classAppend = ""
@param String menu = "main"

<ul class="menu p-2 gap-1 ${classAppend}">
	@for(var menuItem : net.luversof.web.gate.util.TemplateUtil.getMenuList(menu))
		@if(menuItem instanceof net.luversof.web.common.menu.domain.Menu m)
			@if(m.isDisplay())
				<li>
					<a class="font-medium rounded-lg flex items-center gap-3 px-4 py-3 transition-all duration-200 ${m.isCurrentMenu() ? "bg-primary/10 text-primary border-l-4 border-primary font-bold" : "hover:bg-base-200 text-base-content/80 hover:text-base-content"}" href="${m.getUrl()}">
						@template._components.menuIcon(messageCode = m.getMessageCode(), name = m.getName())
						${m.getName()}
					</a>
				</li>
			@endif
		@endif
	@endfor
</ul>