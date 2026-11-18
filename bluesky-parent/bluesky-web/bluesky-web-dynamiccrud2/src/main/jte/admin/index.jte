@import java.util.List
@import net.luversof.web.dynamiccrud2.admin.service.MenuService.ProjectNode

@param List<ProjectNode> tree
@param ProjectNode selected = null
@param String error = null

@template._layout.layout(title = "프로젝트 관리", content = @`
	@template._components.alert(message = error)

	<div class="grid gap-6 md:grid-cols-[16rem_1fr] items-start">
		<nav aria-label="프로젝트" class="grid gap-2">
			<div class="flex items-center justify-between">
				<h2 class="font-semibold">프로젝트</h2>
				@template._components.iconToggle(target = "add-project", icon = "+", label = "새 프로젝트")
			</div>
			@template._components.codeNameForm(formId = "add-project", action = "/admin/project", noun = "프로젝트", stacked = true)
			@if(tree.isEmpty())
				@template._components.emptyState(message = "프로젝트가 없습니다. + 로 추가하세요.")
			@endif
			<ul class="grid gap-0.5" data-reorder-url="/admin/project/order">
				@for(var projectNode : tree)
					!{var project = projectNode.project();}
					!{var current = selected != null && selected.project().getId().equals(project.getId());}
					<li class="flex items-center gap-1 rounded ${current ? "bg-base-200" : ""}" data-id="${project.getId().toString()}">
						@template._components.dragHandle(name = project.getName())
						<a class="flex-1 py-1 truncate hover:underline ${current ? "font-semibold" : ""}" href="/admin?project=${project.getId().toString()}" aria-current="${current ? "page" : null}">${project.getName()}</a>
						@if(!project.isEnableDisplay())<span class="hint inline-flex">@template._components.eyeIcon(slashed = true, label = "메뉴 목록에서 숨김")</span>@endif
						<span class="hint font-mono text-xs pr-2">/${project.getCode()}</span>
					</li>
				@endfor
			</ul>
			@if(tree.size() > 1)
				<p class="hint text-xs">순서는 ⠿ 를 끌거나 ⠿ 에서 ↑ ↓ 키로 바꿉니다.</p>
			@endif
		</nav>

		@if(selected != null)
			!{var project = selected.project();}
			!{var projectId = project.getId().toString();}
			<section class="min-w-0">
				<%-- ✎ 를 누르면 이 줄(이름 /주소)이 그 자리에서 입력칸으로 바뀐다(data-hidden-by, js/toggle.js) --%>
				<div class="flex flex-wrap items-center gap-1 min-h-9">
					<div class="flex items-center gap-1" data-hidden-by="rename-p-${projectId}">
						<h2 class="text-lg font-semibold">${project.getName()}</h2>
						<a class="hint font-mono mr-1 hover:underline" href="/${project.getCode()}" title="사용 화면 열기">/${project.getCode()}</a>
						<span class="icon-group">
							@template._components.iconToggle(target = "rename-p-" + projectId, icon = "✎", label = project.getName() + " 이름 바꾸기")
							@template._components.displayToggle(action = "/admin/project/" + projectId + "/display", name = project.getName(), shown = project.isEnableDisplay())
							@template._components.iconDelete(action = "/admin/project/" + projectId + "/delete", label = project.getName() + " 삭제", confirm = "프로젝트를 지울까요?")
						</span>
					</div>
					@template._components.codeNameForm(formId = "rename-p-" + projectId, action = "/admin/project", noun = "프로젝트",
						itemId = projectId, code = project.getCode(), name = project.getName(), submitText = "저장", inline = true, nameClass = "text-lg font-semibold")
				</div>

				@if(selected.mainMenus().isEmpty())
					@template._components.emptyState(message = "메인 메뉴가 없습니다. 아래 \"+ 메인 메뉴 추가\" 로 추가하세요.", cssClass = "mt-3")
				@endif
				<div class="mt-3 ml-1 pl-4 border-l border-base-300" data-reorder-url="/admin/project/${projectId}/mainMenu/order">
					@for(var mainMenuNode : selected.mainMenus())
						!{var mainMenu = mainMenuNode.mainMenu();}
						!{var mainMenuId = mainMenu.getId().toString();}
						<div class="py-1.5 bg-base-100" data-id="${mainMenuId}">
							<div class="flex flex-wrap items-center gap-1 min-h-9">
								@template._components.dragHandle(name = mainMenu.getName())
								<div class="flex items-center gap-1" data-hidden-by="rename-mm-${mainMenuId}">
									<h3 class="font-semibold">${mainMenu.getName()}</h3>
									<a class="hint font-mono mr-1 hover:underline" href="/${project.getCode()}/${mainMenu.getCode()}" title="사용 화면 열기">/${mainMenu.getCode()}</a>
									<span class="icon-group">
										@template._components.iconToggle(target = "rename-mm-" + mainMenuId, icon = "✎", label = mainMenu.getName() + " 이름 바꾸기")
										@template._components.displayToggle(action = "/admin/mainMenu/" + mainMenuId + "/display", name = mainMenu.getName(), shown = mainMenu.isEnableDisplay())
										@template._components.iconDelete(action = "/admin/mainMenu/" + mainMenuId + "/delete", label = mainMenu.getName() + " 삭제", confirm = "메인 메뉴를 지울까요?")
									</span>
								</div>
								@template._components.codeNameForm(formId = "rename-mm-" + mainMenuId, action = "/admin/project/" + projectId + "/mainMenu",
									noun = "메인 메뉴", itemId = mainMenuId, code = mainMenu.getCode(), name = mainMenu.getName(), submitText = "저장", inline = true, nameClass = "font-semibold")
							</div>
							@if(!mainMenuNode.menus().isEmpty())
								<ul class="ml-6 mt-1 grid gap-0.5 text-sm" data-reorder-url="/admin/mainMenu/${mainMenuId}/menu/order">
									@for(var menu : mainMenuNode.menus())
										!{var menuId = menu.getId().toString();}
										<li class="flex flex-wrap items-center gap-1 bg-base-100 min-h-8" data-id="${menuId}">
											@template._components.dragHandle(name = menu.getName())
											<%-- 프로젝트 · 메인 메뉴와 같게 ✎ 로 그 자리에서 이름 · 주소 이름을 고친다(나머지 설정은 이름을 눌러 메뉴 설정 화면) --%>
											<div class="flex items-center gap-2" data-hidden-by="rename-m-${menuId}">
												<a class="link" href="/admin/menu/${menuId}" title="메뉴 설정 열기">${menu.getName()}</a>
												<a class="hint font-mono hover:underline" href="/${project.getCode()}/${mainMenu.getCode()}/${menu.getCode()}" title="사용 화면 열기">/${menu.getCode()}</a>
												@if(menu.getAuthority() != null)
													<span class="badge">${menu.getAuthority()}</span>
												@endif
												@if(!menu.isEnableDisplay())
													<span class="hint inline-flex">@template._components.eyeIcon(slashed = true, label = "메뉴 목록에서 숨김")</span>
												@endif
												@template._components.iconToggle(target = "rename-m-" + menuId, icon = "✎", label = menu.getName() + " 이름 바꾸기")
											</div>
											@template._components.codeNameForm(formId = "rename-m-" + menuId, action = "/admin/menu/" + menuId + "/rename",
												noun = "메뉴", itemId = menuId, code = menu.getCode(), name = menu.getName(), submitText = "저장", inline = true)
										</li>
									@endfor
								</ul>
							@endif
							<%-- 메뉴 추가는 그 메인 메뉴의 메뉴 목록 맨 아래, 메뉴와 같은 깊이(ml-6)(이름 옆 + 는 메인 메뉴를 만드는 것으로 읽힌다) --%>
							@template._components.addToggle(target = "add-menu-" + mainMenuId, label = "메뉴 추가", cssClass = "ml-6 mt-0.5")
							@template.admin.menuCreateForm(mainMenuId = mainMenuId, mainMenuName = mainMenu.getName())
						</div>
					@endfor
					<%-- 메인 메뉴 추가는 메인 메뉴 목록(세로줄) 안 맨 아래, 메인 메뉴와 같은 깊이. data-id 가 없어 순서 바꾸기 대상은 아니다
					     (프로젝트 이름 옆 + 는 왼쪽 프로젝트 + 와 같은 자리라 프로젝트 추가로 읽힌다) --%>
					@template._components.addToggle(target = "add-mm-" + projectId, label = "메인 메뉴 추가", cssClass = "mt-1")
					@template._components.codeNameForm(formId = "add-mm-" + projectId, action = "/admin/project/" + projectId + "/mainMenu",
						noun = "메인 메뉴", cssClass = "mt-2")
				</div>
			</section>
		@endif
	</div>
`)
