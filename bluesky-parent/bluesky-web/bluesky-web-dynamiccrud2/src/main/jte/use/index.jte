@import java.util.List
@import net.luversof.web.dynamiccrud2.setting.service.MenuService.ProjectNode

@param List<ProjectNode> tree

@template._layout.layout(title = "메뉴", content = @`
	@if(tree.isEmpty())
		<p class="hint">쓸 수 있는 메뉴가 없습니다.</p>
	@endif
	<div class="grid gap-4 sm:grid-cols-2">
		@for(var project : tree)
			<section class="card">
				<h2 class="text-lg font-semibold mb-2">${project.project().getName()}</h2>
				@for(var mainMenu : project.mainMenus())
					<h3 class="font-semibold mt-3 text-sm">${mainMenu.mainMenu().getName()}</h3>
					<ul class="mt-1 ml-4 list-disc text-sm">
						@for(var menu : mainMenu.menus())
							<li><a class="hover:underline" href="/use/menu/${menu.getId().toString()}">${menu.getName()}</a></li>
						@endfor
					</ul>
				@endfor
			</section>
		@endfor
	</div>
`)
