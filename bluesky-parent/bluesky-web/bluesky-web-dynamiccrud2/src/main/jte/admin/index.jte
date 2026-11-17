@import java.util.List
@import net.luversof.web.dynamiccrud2.admin.service.MenuService.ProjectNode

@param List<ProjectNode> tree
@param ProjectNode selected = null
@param String error = null

@template._layout.layout(title = "관리", content = @`
	@if(error != null)
		<div class="alert alert-error mb-4" role="alert">${error}</div>
	@endif

	<div class="grid gap-6 md:grid-cols-[16rem_1fr] items-start">
		<nav aria-label="프로젝트" class="grid gap-2">
			<div class="flex items-center justify-between">
				<h2 class="font-semibold">프로젝트</h2>
				<button type="button" class="icon-btn" data-toggle="add-project" aria-expanded="false" aria-label="새 프로젝트" title="새 프로젝트">+</button>
			</div>
			<form id="add-project" method="post" action="/admin/project" class="card grid gap-2" hidden>
				<input class="input input-sm font-mono" name="code" required maxlength="50" pattern="[a-z0-9][a-z0-9\-]{0,49}" title="영문 소문자 · 숫자 · -" placeholder="주소 이름(예: event)" aria-label="새 프로젝트 주소 이름" />
				<input class="input input-sm" name="name" required maxlength="100" placeholder="프로젝트 이름" aria-label="새 프로젝트 이름" />
				<div class="flex gap-2">
					<button class="btn btn-sm btn-primary">추가</button>
					<button type="button" class="btn btn-sm btn-ghost" data-toggle="add-project">취소</button>
				</div>
			</form>
			@if(tree.isEmpty())
				<p class="hint">프로젝트가 없습니다. + 로 추가하세요.</p>
			@endif
			<ul class="grid gap-0.5" data-reorder-url="/admin/project/order">
				@for(var projectNode : tree)
					!{var project = projectNode.project();}
					!{var current = selected != null && selected.project().getId().equals(project.getId());}
					<li class="flex items-center gap-1 rounded ${current ? "bg-base-200" : ""}" data-id="${project.getId().toString()}">
						<button type="button" class="drag-handle" aria-label="${project.getName()} 순서 바꾸기(끌기 또는 위 · 아래 화살표)">⠿</button>
						<a class="flex-1 py-1 truncate hover:underline ${current ? "font-semibold" : ""}" href="/admin?project=${project.getId().toString()}" aria-current="${current ? "page" : null}">${project.getName()}</a>
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
				<div class="flex items-center gap-1">
					<h2 class="text-lg font-semibold">${project.getName()}</h2>
					<a class="hint font-mono mr-1 hover:underline" href="/${project.getCode()}" title="사용 화면 열기">/${project.getCode()}</a>
					<button type="button" class="icon-btn" data-toggle="rename-p-${projectId}" aria-expanded="false" aria-label="${project.getName()} 이름 바꾸기" title="이름 바꾸기">✎</button>
					<form method="post" action="/admin/project/${projectId}/delete" class="inline-flex">
						<button class="icon-btn icon-btn-danger" aria-label="${project.getName()} 삭제" title="삭제" onclick="return confirm('프로젝트를 지울까요?')">×</button>
					</form>
					<button type="button" class="icon-btn" data-toggle="add-mm-${projectId}" aria-expanded="false" aria-label="${project.getName()} 에 메인 메뉴 추가" title="메인 메뉴 추가">+</button>
				</div>
				<form id="rename-p-${projectId}" method="post" action="/admin/project" class="mt-2 flex flex-wrap items-center gap-2" hidden>
					<input type="hidden" name="id" value="${projectId}" />
					<input class="input input-sm w-40 font-mono" name="code" value="${project.getCode()}" required maxlength="50" pattern="[a-z0-9][a-z0-9\-]{0,49}" title="영문 소문자 · 숫자 · -" aria-label="프로젝트 주소 이름" />
					<input class="input input-sm" name="name" value="${project.getName()}" required maxlength="100" aria-label="프로젝트 새 이름" />
					<button class="btn btn-sm btn-primary">저장</button>
					<button type="button" class="btn btn-sm btn-ghost" data-toggle="rename-p-${projectId}">취소</button>
				</form>
				<form id="add-mm-${projectId}" method="post" action="/admin/project/${projectId}/mainMenu" class="mt-2 flex flex-wrap items-center gap-2" hidden>
					<input class="input input-sm w-40 font-mono" name="code" required maxlength="50" pattern="[a-z0-9][a-z0-9\-]{0,49}" title="영문 소문자 · 숫자 · -" placeholder="주소 이름" aria-label="새 메인 메뉴 주소 이름" />
					<input class="input input-sm" name="name" required maxlength="100" placeholder="메인 메뉴 이름" aria-label="새 메인 메뉴 이름" />
					<button class="btn btn-sm btn-primary">추가</button>
					<button type="button" class="btn btn-sm btn-ghost" data-toggle="add-mm-${projectId}">취소</button>
				</form>

				@if(selected.mainMenus().isEmpty())
					<p class="hint mt-3">메인 메뉴가 없습니다. + 로 추가하세요.</p>
				@endif
				<div class="mt-3 ml-1 pl-4 border-l border-base-300" data-reorder-url="/admin/project/${projectId}/mainMenu/order">
					@for(var mainMenuNode : selected.mainMenus())
						!{var mainMenu = mainMenuNode.mainMenu();}
						!{var mainMenuId = mainMenu.getId().toString();}
						<div class="py-1.5 bg-base-100" data-id="${mainMenuId}">
							<div class="flex items-center gap-1">
								<button type="button" class="drag-handle" aria-label="${mainMenu.getName()} 순서 바꾸기(끌기 또는 위 · 아래 화살표)">⠿</button>
								<h3 class="font-semibold">${mainMenu.getName()}</h3>
								<span class="hint font-mono mr-1">/${mainMenu.getCode()}</span>
								<button type="button" class="icon-btn" data-toggle="rename-mm-${mainMenuId}" aria-expanded="false" aria-label="${mainMenu.getName()} 이름 바꾸기" title="이름 바꾸기">✎</button>
								<form method="post" action="/admin/mainMenu/${mainMenuId}/delete" class="inline-flex">
									<button class="icon-btn icon-btn-danger" aria-label="${mainMenu.getName()} 삭제" title="삭제" onclick="return confirm('메인 메뉴를 지울까요?')">×</button>
								</form>
								<a class="icon-btn" href="/admin/mainMenu/${mainMenuId}/menu/new" aria-label="${mainMenu.getName()} 에 메뉴 추가" title="메뉴 추가">+</a>
							</div>
							<form id="rename-mm-${mainMenuId}" method="post" action="/admin/project/${projectId}/mainMenu" class="mt-1 ml-8 flex flex-wrap items-center gap-2" hidden>
								<input type="hidden" name="id" value="${mainMenuId}" />
								<input class="input input-sm w-40 font-mono" name="code" value="${mainMenu.getCode()}" required maxlength="50" pattern="[a-z0-9][a-z0-9\-]{0,49}" title="영문 소문자 · 숫자 · -" aria-label="메인 메뉴 주소 이름" />
								<input class="input input-sm" name="name" value="${mainMenu.getName()}" required maxlength="100" aria-label="메인 메뉴 새 이름" />
								<button class="btn btn-sm btn-primary">저장</button>
								<button type="button" class="btn btn-sm btn-ghost" data-toggle="rename-mm-${mainMenuId}">취소</button>
							</form>
							@if(!mainMenuNode.menus().isEmpty())
								<ul class="ml-6 mt-1 grid gap-0.5 text-sm" data-reorder-url="/admin/mainMenu/${mainMenuId}/menu/order">
									@for(var menu : mainMenuNode.menus())
										<li class="flex items-center gap-2 bg-base-100" data-id="${menu.getId().toString()}">
											<button type="button" class="drag-handle" aria-label="${menu.getName()} 순서 바꾸기(끌기 또는 위 · 아래 화살표)">⠿</button>
											<a class="hover:underline" href="/admin/menu/${menu.getId().toString()}">${menu.getName()}</a>
											<a class="hint font-mono hover:underline" href="/${project.getCode()}/${mainMenu.getCode()}/${menu.getCode()}" title="사용 화면 열기">/${menu.getCode()}</a>
											@if(menu.getAuthority() != null)
												<span class="badge">${menu.getAuthority()}</span>
											@endif
											@if(!menu.isEnableDisplay())
												<span class="badge">숨김</span>
											@endif
										</li>
									@endfor
								</ul>
							@endif
						</div>
					@endfor
				</div>
			</section>
		@endif
	</div>
	<script src="/js/reorder.js"></script>
	<script src="/js/toggle.js"></script>
`)
