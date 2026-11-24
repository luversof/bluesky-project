@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import java.util.List
@import net.luversof.web.dynamiccrud2.admin.service.MenuService.ProjectNode

@param List<ProjectNode> tree
@param ProjectNode selected = null
@param String error = null

<%-- 전체 설정 변경 이력은 화면 제목(프로젝트 관리) 줄 오른쪽 끝, 그 프로젝트의 이력은 프로젝트 줄 오른쪽 끝(사용자 결정 11-24) --%>
@template._layout.layout(title = t("admin.index.title"), titleActions = @`
	<a class="btn btn-sm" href="/admin/history">${t("admin.history.allTitle")}</a>
`, content = @`
	@template._components.alert(message = error)

	<div class="grid gap-6 md:grid-cols-[16rem_1fr] items-start">
		<nav aria-label="${t("admin.projects")}" class="grid gap-2">
			<div class="flex items-center justify-between">
				<h2 class="font-semibold">${t("admin.projects")}</h2>
				@template._components.iconToggle(target = "add-project", icon = "+", label = t("admin.project.new"))
			</div>
			@template._components.codeNameForm(formId = "add-project", action = "/admin/project", noun = t("noun.project"), stacked = true)
			@if(tree.isEmpty())
				@template._components.emptyState(message = t("admin.project.empty"))
			@endif
			<ul class="grid gap-0.5" data-reorder-url="/admin/project/order">
				@for(var projectNode : tree)
					!{var project = projectNode.project();}
					!{var current = selected != null && selected.project().getId().equals(project.getId());}
					<%-- reveal-row · reveal-icons: 아이콘은 줄에 올리거나 초점이 들어올 때만 스르륵(숨김 상태 빗금 눈은 늘 보임, 사용자 결정 11-24, main.css) --%>
					<li class="reveal-row flex items-center gap-1 rounded ${current ? "bg-base-200" : ""}" data-id="${project.getId().toString()}">
						@template._components.dragHandle(name = project.getName())
						<a class="min-w-0 py-1 truncate hover:underline ${current ? "font-semibold" : ""}" href="/admin?project=${project.getId().toString()}" aria-current="${current ? "page" : null}">${project.getName()}</a>
						<%-- 주소는 이름 바로 옆에(사용자 요청 11-24), 보이기 토글만 오른쪽 끝 --%>
						<span class="hint font-mono text-xs">/${project.getCode()}</span>
						<%-- 보이기 · 숨기기를 목록에서 바로(사용자 요청 11-24). 끝나면 보던 프로젝트로(back) --%>
						<span class="reveal-icons ml-auto pr-1">@template._components.displayToggle(action = "/admin/project/" + project.getId() + "/display", name = project.getName(),
							shown = project.isEnableDisplay(), back = selected == null ? null : selected.project().getId().toString())</span>
					</li>
				@endfor
			</ul>
			@if(tree.size() > 1)
				<p class="hint text-xs">${t("admin.reorderHint")}</p>
			@endif
		</nav>

		@if(selected != null)
			!{var project = selected.project();}
			!{var projectId = project.getId().toString();}
			<section class="min-w-0">
				<%-- ✎ 를 누르면 이 줄(이름 /주소)이 그 자리에서 입력칸으로 바뀐다(data-hidden-by, js/toggle.js) --%>
				<div class="flex flex-wrap items-center gap-1 min-h-9">
					<div class="reveal-row flex items-center gap-1" data-hidden-by="rename-p-${projectId}">
						<h2 class="text-lg font-semibold">${project.getName()}</h2>
						<a class="hint font-mono mr-1 hover:underline" href="/${project.getCode()}" title="${t("admin.openUse")}">/${project.getCode()}</a>
						<span class="icon-group reveal-icons">
							@template._components.iconToggle(target = "rename-p-" + projectId, icon = "✎", label = t("admin.rename", project.getName()))
							@template._components.displayToggle(action = "/admin/project/" + projectId + "/display", name = project.getName(), shown = project.isEnableDisplay())
							@template._components.iconDelete(action = "/admin/project/" + projectId + "/delete", label = t("admin.deleteItem", project.getName()), confirm = t("admin.project.deleteConfirm"),
								blockedReason = selected.mainMenus().isEmpty() ? null : t("admin.project.deleteBlocked", selected.mainMenus().size()))
						</span>
					</div>
					@template._components.codeNameForm(formId = "rename-p-" + projectId, action = "/admin/project", noun = t("noun.project"),
						itemId = projectId, code = project.getCode(), name = project.getName(), submitText = t("common.save"), inline = true, nameClass = "text-lg font-semibold")
					<%-- 이 프로젝트의 설정 변경 이력(프로젝트 · 메인 메뉴 · 메뉴 · 쿼리 · 검색, 지운 메뉴 포함)은 이 줄 오른쪽 끝 --%>
					<a class="btn btn-sm ml-auto" href="/admin/project/${projectId}/history">${t("admin.history.settingsTitle")}</a>
				</div>

				@if(selected.mainMenus().isEmpty())
					@template._components.emptyState(message = t("admin.mainMenu.empty"), cssClass = "mt-3")
				@endif
				<div class="mt-3 ml-1 pl-4 border-l border-base-300" data-reorder-url="/admin/project/${projectId}/mainMenu/order">
					@for(var mainMenuNode : selected.mainMenus())
						!{var mainMenu = mainMenuNode.mainMenu();}
						!{var mainMenuId = mainMenu.getId().toString();}
						<div class="py-1.5 bg-base-100" data-id="${mainMenuId}">
							<div class="reveal-row flex flex-wrap items-center gap-1 min-h-9">
								@template._components.dragHandle(name = mainMenu.getName())
								<div class="flex items-center gap-1" data-hidden-by="rename-mm-${mainMenuId}">
									<h3 class="font-semibold">${mainMenu.getName()}</h3>
									<a class="hint font-mono mr-1 hover:underline" href="/${project.getCode()}/${mainMenu.getCode()}" title="${t("admin.openUse")}">/${mainMenu.getCode()}</a>
									<span class="icon-group reveal-icons">
										@template._components.iconToggle(target = "rename-mm-" + mainMenuId, icon = "✎", label = t("admin.rename", mainMenu.getName()))
										@template._components.displayToggle(action = "/admin/mainMenu/" + mainMenuId + "/display", name = mainMenu.getName(), shown = mainMenu.isEnableDisplay())
										@template._components.iconDelete(action = "/admin/mainMenu/" + mainMenuId + "/delete", label = t("admin.deleteItem", mainMenu.getName()), confirm = t("admin.mainMenu.deleteConfirm"),
											blockedReason = mainMenuNode.menus().isEmpty() ? null : t("admin.mainMenu.deleteBlocked", mainMenuNode.menus().size()))
									</span>
								</div>
								@template._components.codeNameForm(formId = "rename-mm-" + mainMenuId, action = "/admin/project/" + projectId + "/mainMenu",
									noun = t("noun.mainMenu"), itemId = mainMenuId, code = mainMenu.getCode(), name = mainMenu.getName(), submitText = t("common.save"), inline = true, nameClass = "font-semibold")
							</div>
							<%-- 메뉴 목록은 메뉴가 없어도 둔다: 다른 메인 메뉴의 메뉴를 끌어 놓는 자리(data-reorder-group, 사용자 결정 11-24) --%>
								<ul class="ml-6 mt-1 grid gap-0.5 text-sm" data-reorder-url="/admin/mainMenu/${mainMenuId}/menu/order" data-reorder-group="menu">
									@for(var menu : mainMenuNode.menus())
										!{var menuId = menu.getId().toString();}
										<li class="reveal-row flex flex-wrap items-center gap-1 bg-base-100 min-h-8" data-id="${menuId}">
											@template._components.dragHandle(name = menu.getName())
											<%-- 프로젝트 · 메인 메뉴와 같게 ✎ 로 그 자리에서 이름 · 주소 이름을 고친다(나머지 설정은 이름을 눌러 메뉴 설정 화면) --%>
											<div class="flex items-center gap-2" data-hidden-by="rename-m-${menuId}">
												<a class="link" href="/admin/menu/${menuId}" title="${t("admin.openMenu")}">${menu.getName()}</a>
												<a class="hint font-mono hover:underline" href="/${project.getCode()}/${mainMenu.getCode()}/${menu.getCode()}" title="${t("admin.openUse")}">/${menu.getCode()}</a>
												@if(menu.getAuthority() != null)
													<span class="badge">${menu.getAuthority()}</span>
												@endif
												<%-- 메인 메뉴와 같게 ✎ · 눈(보이기 · 숨기기) · 휴지통(삭제)(사용자 요청 11-24).
												     복사: 이름을 묻지 않고 "이름 복사본" · code-copy 로 숨김 메뉴를 만들고 이 화면에 머문다(MenuService.copyMenu(id), 사용자 결정 11-24) --%>
												<span class="icon-group reveal-icons">
													@template._components.iconToggle(target = "rename-m-" + menuId, icon = "✎", label = t("admin.rename", menu.getName()))
													<form method="post" action="/admin/menu/${menuId}/copy" class="inline-flex">
														<button class="icon-btn" aria-label="${t("admin.menu.copy.item", menu.getName())}" data-tip="${t("admin.menu.copy.tip")}">
															<svg viewBox="0 0 16 16" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><rect x="5.5" y="5.5" width="8" height="8" rx="1.5"/><path d="M10.5 3.5v-1a1 1 0 0 0-1-1h-6a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h1"/></svg>
														</button>
													</form>
													@template._components.displayToggle(action = "/admin/menu/" + menuId + "/display", name = menu.getName(), shown = menu.isEnableDisplay(), opensByAddress = true)
													@template._components.iconDelete(action = "/admin/menu/" + menuId + "/delete", label = t("admin.deleteItem", menu.getName()), confirm = t("admin.menu.deleteConfirm"))
												</span>
											</div>
											@template._components.codeNameForm(formId = "rename-m-" + menuId, action = "/admin/menu/" + menuId + "/rename",
												noun = t("noun.menu"), itemId = menuId, code = menu.getCode(), name = menu.getName(), submitText = t("common.save"), inline = true)
										</li>
									@endfor
								</ul>
							<%-- 메뉴 추가는 그 메인 메뉴의 메뉴 목록 맨 아래, 메뉴와 같은 깊이(ml-6)(이름 옆 + 는 메인 메뉴를 만드는 것으로 읽힌다) --%>
							@template._components.addToggle(target = "add-menu-" + mainMenuId, label = t("admin.menu.add"), cssClass = "ml-6 mt-0.5")
							@template.admin.menuCreateForm(mainMenuId = mainMenuId, mainMenuName = mainMenu.getName())
						</div>
					@endfor
					<%-- 메인 메뉴 추가는 메인 메뉴 목록(세로줄) 안 맨 아래, 메인 메뉴와 같은 깊이. data-id 가 없어 순서 바꾸기 대상은 아니다
					     (프로젝트 이름 옆 + 는 왼쪽 프로젝트 + 와 같은 자리라 프로젝트 추가로 읽힌다) --%>
					@template._components.addToggle(target = "add-mm-" + projectId, label = t("admin.mainMenu.add"), cssClass = "mt-1")
					@template._components.codeNameForm(formId = "add-mm-" + projectId, action = "/admin/project/" + projectId + "/mainMenu",
						noun = t("noun.mainMenu"), cssClass = "mt-2")
				</div>
			</section>
		@endif
	</div>
`)
