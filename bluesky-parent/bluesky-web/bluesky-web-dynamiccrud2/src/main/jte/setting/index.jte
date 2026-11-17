@import java.util.List
@import net.luversof.web.dynamiccrud2.setting.service.MenuService.ProjectNode

@param List<ProjectNode> tree
@param String error = null

@template._layout.layout(title = "설정", content = @`
	@if(error != null)
		<div class="alert alert-error mb-4" role="alert">${error}</div>
	@endif

	<form method="post" action="/setting/project" class="card mb-6 flex flex-wrap items-end gap-3">
		<div>
			<label class="label" for="newProjectName">새 프로젝트</label>
			<input id="newProjectName" class="input" name="name" required maxlength="100" />
		</div>
		<div>
			<label class="label" for="newProjectOrder">순서</label>
			<input id="newProjectOrder" class="input w-24" name="displayOrder" type="number" value="0" />
		</div>
		<button class="btn btn-primary">추가</button>
	</form>

	@if(tree.isEmpty())
		<p class="hint">프로젝트가 없습니다. 위에서 추가하세요.</p>
	@endif

	@for(var projectNode : tree)
		!{var project = projectNode.project();}
		<section class="card mb-4">
			<div class="flex items-center gap-3 mb-3">
				<h2 class="text-lg font-semibold">${project.getName()}</h2>
				<form method="post" action="/setting/project/${project.getId().toString()}/delete" class="ml-auto">
					<button class="btn btn-sm btn-ghost" onclick="return confirm('프로젝트를 지울까요?')">프로젝트 삭제</button>
				</form>
			</div>

			@for(var mainMenuNode : projectNode.mainMenus())
				!{var mainMenu = mainMenuNode.mainMenu();}
				<div class="border-t border-base-300 py-3">
					<div class="flex items-center gap-3">
						<h3 class="font-semibold">${mainMenu.getName()}</h3>
						<a class="btn btn-sm" href="/setting/mainMenu/${mainMenu.getId().toString()}/menu/new">메뉴 추가</a>
						<form method="post" action="/setting/mainMenu/${mainMenu.getId().toString()}/delete" class="ml-auto">
							<button class="btn btn-sm btn-ghost" onclick="return confirm('메인 메뉴를 지울까요?')">메인 메뉴 삭제</button>
						</form>
					</div>
					@if(!mainMenuNode.menus().isEmpty())
						<ul class="mt-2 ml-4 list-disc text-sm">
							@for(var menu : mainMenuNode.menus())
								<li>
									<a class="hover:underline" href="/setting/menu/${menu.getId().toString()}">${menu.getName()}</a>
									@if(menu.getAuthority() != null)
										<span class="badge ml-1">${menu.getAuthority()}</span>
									@endif
									@if(!menu.isEnableDisplay())
										<span class="badge ml-1">숨김</span>
									@endif
								</li>
							@endfor
						</ul>
					@endif
				</div>
			@endfor

			<form method="post" action="/setting/project/${project.getId().toString()}/mainMenu" class="border-t border-base-300 pt-3 flex flex-wrap items-end gap-3">
				<div>
					<label class="label" for="mm-${project.getId().toString()}">새 메인 메뉴</label>
					<input id="mm-${project.getId().toString()}" class="input input-sm" name="name" required maxlength="100" />
				</div>
				<input class="input input-sm w-20" name="displayOrder" type="number" value="0" aria-label="순서" />
				<button class="btn btn-sm">추가</button>
			</form>
		</section>
	@endfor
`)
