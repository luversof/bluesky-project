@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import java.util.List
@import net.luversof.web.dynamiccrud2.admin.service.MenuService.ProjectNode

@param List<ProjectNode> tree

@template._layout.layout(title = t("menu.index.title"), content = @`
	@if(tree.isEmpty())
		@template._components.emptyState(message = t("menu.index.empty"))
	@endif
	<div class="grid gap-4 sm:grid-cols-2">
		@for(var project : tree)
			<section class="card">
				<h2 class="text-lg font-semibold mb-2"><a class="hover:underline" href="/${project.project().getCode()}">${project.project().getName()}</a></h2>
				@for(var mainMenu : project.mainMenus())
					<h3 class="font-semibold mt-3 text-sm">${mainMenu.mainMenu().getName()}</h3>
					<ul class="mt-1 ml-4 list-disc text-sm">
						@for(var menu : mainMenu.menus())
							<li><a class="hover:underline" href="/${project.project().getCode()}/${mainMenu.mainMenu().getCode()}/${menu.getCode()}">${menu.getName()}</a></li>
						@endfor
					</ul>
				@endfor
			</section>
		@endfor
	</div>
`)
