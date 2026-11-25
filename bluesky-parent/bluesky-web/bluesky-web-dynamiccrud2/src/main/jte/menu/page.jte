@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import net.luversof.web.dynamiccrud2.menu.service.MenuPageService.UsePage

@param UsePage usePage
@param String searchQuery
@param String menuPath
@param net.luversof.web.dynamiccrud2.admin.service.MenuService.ProjectNode nav = null
@param java.util.List<net.luversof.web.dynamiccrud2.domain.Project> projects = java.util.List.of()

!{var menu = usePage.menu();}
!{var base = menuPath + "?" + searchQuery;}
!{var sortPart = usePage.sort() == null ? "" : "&sort=" + java.net.URLEncoder.encode(usePage.sort(), java.nio.charset.StandardCharsets.UTF_8) + "&desc=" + usePage.descending();}
!{var lastPage = usePage.total() <= 0 ? 0 : (int) ((usePage.total() - 1) / usePage.pageSize());}

@template._layout.layout(title = menu.getName(), brand = nav == null ? null : nav.project().getName(), brandUrl = nav == null ? "/" : "/" + nav.project().getCode(),
	brandMenu = nav == null || projects.size() < 2 ? null : @`@template._components.projectSwitcher(current = nav.project(), projects = projects)`, sidebar = nav == null ? null : @`
	<nav class="grid gap-4 text-sm">
		@for(var mainMenuNode : nav.mainMenus())
			<div>
				<p class="font-semibold mb-1">${mainMenuNode.mainMenu().getName()}</p>
				<ul class="grid gap-0.5 border-l border-base-300">
					@for(var item : mainMenuNode.menus())
						!{var current = item.getId().equals(menu.getId());}
						<li>
							<a class="block pl-3 py-1 -ml-px border-l-2 ${current ? "border-primary text-primary font-semibold" : "border-transparent hover:border-base-300 hover:bg-base-200"}"
								href="/${nav.project().getCode()}/${mainMenuNode.mainMenu().getCode()}/${item.getCode()}"
								aria-current="${current ? "page" : null}">${item.getName()}</a>
						</li>
					@endfor
				</ul>
			</div>
		@endfor
	</nav>
`, stickyHead = @`
	<%-- 검색 폼 · 총 건수 줄은 제목과 함께 화면 위에 붙어 있다(layout stickyHead, 사용자 요청 11-24) --%>
	<%-- 검색: 통합 검색 · 메뉴 검색 설정 칸 · 쿼리 :파라미터 칸을 한 폼으로. 페이지 · 정렬은 새 검색에서 처음으로 돌린다 --%>
	@if(usePage.keyword() != null || !usePage.filterFields().isEmpty() || !usePage.searchFields().isEmpty())
		<form method="get" action="${menuPath}" class="card mb-3 flex flex-wrap items-end gap-3" role="search" aria-label="${t("menu.page.searchLabel", menu.getName())}">
			@if(usePage.keyword() != null)
				@template._components.field(label = t("menu.page.keyword"), forId = "q", content = @`
					<input id="q" class="input input-sm w-64" type="search" name="q" value="${usePage.keyword().value()}"
						placeholder="${String.join(" · ", usePage.keyword().headers())}" aria-describedby="q-hint" />
					<span id="q-hint" class="sr-only">${t("menu.page.keyword.hint", String.join(", ", usePage.keyword().headers()))}</span>
				`)
			@endif
			@for(var i = 0; i < usePage.filterFields().size(); i++)
				@template._components.searchFilter(field = usePage.filterFields().get(i), index = i)
			@endfor
			@for(var field : usePage.searchFields())
				@template._components.field(label = field.header(), forId = "s-" + field.name(), required = field.required(), content = @`
					@template._components.typedInput(id = "s-" + field.name(), name = "s." + field.name(), value = field.value(), inputType = field.inputType(), small = true, options = field.options())
				`)
			@endfor
			<div class="flex gap-2">
				<button class="btn btn-sm btn-primary">${t("menu.page.search")}</button>
				<a class="btn btn-sm btn-ghost" href="${menuPath}">${t("menu.page.reset")}</a>
			</div>
		</form>
	@endif

	<div class="flex flex-wrap items-center gap-2">
		<span class="text-sm">${t("menu.page.total", usePage.total())}</span>
		<span class="ml-auto flex flex-wrap gap-2">
			@for(var action : usePage.insertActions())
				<button class="btn btn-sm" hx-post="${menuPath}/action/${action.getId().toString()}/form" hx-target="#actionPanel">${action.getName()}</button>
			@endfor
			@for(var action : usePage.deleteActions())
				<button class="btn btn-sm btn-error" hx-post="${menuPath}/action/${action.getId().toString()}/form" hx-include="[name='rows']:checked" hx-target="#actionPanel">${t("menu.page.selected", action.getName())}</button>
			@endfor
			@if(menu.isEnableExcel())
				<a class="btn btn-sm" href="${menuPath}/excel?${searchQuery}${sortPart}">${t("menu.page.excel")}</a>
			@endif
		</span>
	</div>
`, content = @`

	@if(usePage.message() != null)
		@template._components.alert(message = usePage.message(), tone = "warning")
	@endif

	<%-- 작업 폼은 목록 위 레이어로 띄워 스크롤 위치를 바꾸지 않는다 --%>
	@template._components.modal(id = "actionDialog", panelId = "actionPanel", label = t("menu.page.actionLabel"))

	<%-- .table-scroll: 가로 스크롤 상자. 화면을 내리면 표 머리(thead)가 위에 붙은 제목 · 검색 밑에 따라 붙는다(사용자 요청 11-24) --%>
	<div class="relative table-scroll">
		<table class="table" aria-label="${menu.getName()}">
			<thead>
				<tr>
					@if(!usePage.deleteActions().isEmpty())<th class="w-8"><input class="checkbox" type="checkbox" data-check-all="rows" aria-label="${t("menu.page.checkAll")}" /></th>@endif
					@for(var column : usePage.columns())
						<%-- 제목도 칸과 같은 쪽으로(숫자 오른쪽 · 참거짓 가운데). 값 목록이 있으면 칸이 글자로 보여 왼쪽 --%>
						<th class="${column.options().isEmpty() ? column.inputType().align() : null}">
							@if(column.sortable())
								!{var active = column.label().equals(usePage.sort());}
								!{var nextDesc = active && !usePage.descending();}
								<a class="hover:underline" href="${base}&sort=${java.net.URLEncoder.encode(column.label(), java.nio.charset.StandardCharsets.UTF_8)}&desc=${nextDesc}">
									${column.header()}${active ? (usePage.descending() ? " ▼" : " ▲") : ""}
								</a>
							@else
								${column.header()}
							@endif
						</th>
					@endfor
					<%-- 수정 단추 칸은 가로 스크롤과 상관없이 오른쪽 끝에 붙어 보인다(.sticky-end, 사용자 요청 11-24) --%>
					@if(!usePage.updateActions().isEmpty())<th class="sticky-end"></th>@endif
				</tr>
			</thead>
			<tbody>
				@for(var row : usePage.rows())
					<tr>
						@if(!usePage.deleteActions().isEmpty())
							<td><input class="checkbox" type="checkbox" name="rows" value="${row.json()}" aria-label="${t("menu.page.checkRow")}" /></td>
						@endif
						@for(var i = 0; i < row.values().size(); i++)
							@template._components.cellValue(value = row.values().get(i),
								inputType = i < usePage.columns().size() ? usePage.columns().get(i).inputType() : null,
								options = i < usePage.columns().size() ? usePage.columns().get(i).options() : java.util.Map.of(),
								dateFormat = i < usePage.columns().size() ? usePage.columns().get(i).dateFormat() : null,
								json = i < usePage.columns().size() && usePage.columns().get(i).json())
						@endfor
						@if(!usePage.updateActions().isEmpty())
							<td class="sticky-end whitespace-nowrap text-right">
								<input type="hidden" name="row" value="${row.json()}" />
								@for(var action : usePage.updateActions())
									<button class="btn btn-sm" hx-post="${menuPath}/action/${action.getId().toString()}/form" hx-include="closest td" hx-target="#actionPanel">${action.getName()}</button>
								@endfor
							</td>
						@endif
					</tr>
				@endfor
				@if(usePage.rows().isEmpty())
					@template._components.emptyState(message = t("menu.page.empty"), colspan = usePage.columns().size() + (usePage.deleteActions().isEmpty() ? 0 : 1) + (usePage.updateActions().isEmpty() ? 0 : 1))
				@endif
			</tbody>
		</table>
	</div>

	@template._components.pagination(current = usePage.page(), lastPage = lastPage, pageUrl = base + sortPart + "&page=")
`)
