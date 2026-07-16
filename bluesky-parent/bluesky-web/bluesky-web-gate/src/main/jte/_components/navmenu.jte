@param String classAppend = ""
@param String menu = "main"

<ul class="menu p-2 gap-1 ${classAppend}">
	@for(var menuItem : net.luversof.web.gate.util.TemplateUtil.getMenuList(menu))
		@if(menuItem instanceof net.luversof.web.common.menu.domain.Menu m)
			@if(m.isDisplay())
				<li>
					<%-- 라벨은 absolute(흐름 밖)라 접힐 때 아이콘 x 가 라벨 길이에 흔들리지 않는다. 아이콘은 패딩 트랜지션으로만 이동. --%>
					<a class="sidebar-link relative overflow-hidden font-medium rounded-box flex items-center px-4 py-3 border-l-4 transition-[background-color,color,border-color,padding] duration-300 hover:duration-75 ${m.isCurrentMenu() ? "bg-primary/10 text-primary border-primary font-bold" : "border-transparent hover:bg-base-200 text-base-content/80 hover:text-base-content"}" href="${m.getUrl()}" title="${m.getName()}">
						@template._components.menuIcon(messageCode = m.getMessageCode(), name = m.getName())
						<span class="sidebar-label absolute inset-y-0 left-12 right-3 flex items-center truncate">${m.getName()}</span>
					</a>
				</li>
			@endif
		@endif
	@endfor
</ul>