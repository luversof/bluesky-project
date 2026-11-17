@import net.luversof.web.dynamiccrud2.domain.InputType
@import net.luversof.web.dynamiccrud2.menu.service.MenuPageService.UsePage

@param UsePage usePage
@param String searchQuery
@param String menuPath
@param net.luversof.web.dynamiccrud2.admin.service.MenuService.ProjectNode nav = null

!{var menu = usePage.menu();}
!{var base = menuPath + "?" + searchQuery;}
!{var sortPart = usePage.sort() == null ? "" : "&sort=" + java.net.URLEncoder.encode(usePage.sort(), java.nio.charset.StandardCharsets.UTF_8) + "&desc=" + usePage.descending();}
!{var lastPage = usePage.total() <= 0 ? 0 : (int) ((usePage.total() - 1) / usePage.pageSize());}

@template._layout.layout(title = menu.getName(), brand = nav == null ? null : nav.project().getName(), brandUrl = nav == null ? "/" : "/" + nav.project().getCode(), sidebar = nav == null ? null : @`
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
`, content = @`

	@if(!usePage.searchFields().isEmpty())
		<form method="get" action="${menuPath}" class="card mb-4 flex flex-wrap items-end gap-3">
			@for(var field : usePage.searchFields())
				<div>
					<label class="label" for="s-${field.name()}">${field.header()}@if(field.required()) <span class="text-error">*</span>@endif</label>
					<input id="s-${field.name()}" class="input input-sm" name="s.${field.name()}" value="${field.value()}"
						type="${field.inputType() == InputType.NUMBER ? "number" : field.inputType() == InputType.DATE ? "date" : field.inputType() == InputType.DATETIME ? "datetime-local" : "text"}"
						step="${field.inputType() == InputType.NUMBER ? "any" : field.inputType() == InputType.DATETIME ? "1" : null}" />
				</div>
			@endfor
			<button class="btn btn-sm btn-primary">검색</button>
		</form>
	@endif

	@if(usePage.message() != null)
		<div class="alert alert-warning mb-4" role="alert">${usePage.message()}</div>
	@endif

	<div class="flex flex-wrap items-center gap-2 mb-2">
		<span class="text-sm">전체 ${usePage.total()}건</span>
		<span class="ml-auto flex flex-wrap gap-2">
			@for(var action : usePage.insertActions())
				<button class="btn btn-sm" hx-post="${menuPath}/action/${action.getId().toString()}/form" hx-target="#actionPanel">${action.getName()}</button>
			@endfor
			@for(var action : usePage.deleteActions())
				<button class="btn btn-sm btn-error" hx-post="${menuPath}/action/${action.getId().toString()}/form" hx-include="[name='rows']:checked" hx-target="#actionPanel">선택 ${action.getName()}</button>
			@endfor
			@if(menu.isEnableExcel())
				<a class="btn btn-sm" href="${menuPath}/excel?${searchQuery}${sortPart}">엑셀</a>
			@endif
		</span>
	</div>

	<div id="actionPanel" class="mb-3"></div>

	<div class="overflow-x-auto">
		<table class="table">
			<thead>
				<tr>
					@if(!usePage.deleteActions().isEmpty())<th class="w-8"><span class="sr-only">선택</span></th>@endif
					@for(var column : usePage.columns())
						<th>
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
					@if(!usePage.updateActions().isEmpty())<th></th>@endif
				</tr>
			</thead>
			<tbody>
				@for(var row : usePage.rows())
					<tr>
						@if(!usePage.deleteActions().isEmpty())
							<td><input class="checkbox" type="checkbox" name="rows" value="${row.json()}" aria-label="행 선택" /></td>
						@endif
						@for(var value : row.values())
							<td class="max-w-xs truncate" title="${value}">${value}</td>
						@endfor
						@if(!usePage.updateActions().isEmpty())
							<td class="whitespace-nowrap text-right">
								<input type="hidden" name="row" value="${row.json()}" />
								@for(var action : usePage.updateActions())
									<button class="btn btn-sm" hx-post="${menuPath}/action/${action.getId().toString()}/form" hx-include="closest td" hx-target="#actionPanel">${action.getName()}</button>
								@endfor
							</td>
						@endif
					</tr>
				@endfor
				@if(usePage.rows().isEmpty())
					<tr><td class="hint py-6" colspan="${usePage.columns().size() + (usePage.deleteActions().isEmpty() ? 0 : 1) + (usePage.updateActions().isEmpty() ? 0 : 1)}">조회 결과가 없습니다.</td></tr>
				@endif
			</tbody>
		</table>
	</div>

	@if(lastPage > 0)
		<nav class="mt-4 flex items-center gap-2 text-sm" aria-label="페이지">
			@if(usePage.page() > 0)<a class="btn btn-sm" href="${base}${sortPart}&page=${usePage.page() - 1}">이전</a>@endif
			<span>${usePage.page() + 1} / ${lastPage + 1}</span>
			@if(usePage.page() < lastPage)<a class="btn btn-sm" href="${base}${sortPart}&page=${usePage.page() + 1}">다음</a>@endif
		</nav>
	@endif
`)
