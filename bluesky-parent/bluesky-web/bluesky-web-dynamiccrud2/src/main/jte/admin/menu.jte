@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import java.util.List
@import net.luversof.web.dynamiccrud2.domain.DbTable
@import net.luversof.web.dynamiccrud2.domain.Menu
@import net.luversof.web.dynamiccrud2.domain.MenuQuery
@import net.luversof.web.dynamiccrud2.domain.QueryKind

@param Menu menu
@param DbTable dbTable
@param List<MenuQuery> queries
@param java.util.UUID projectId
@param net.luversof.web.dynamiccrud2.admin.service.SearchService.SearchSetup search
@param String databaseType
@param String targetNoun = net.luversof.web.dynamiccrud2.config.Msg.t("target.table")
@param List<net.luversof.web.dynamiccrud2.admin.controller.Crumb> breadcrumb = null
@param String error = null
<%-- menuPath: 제목에 보일 "프로젝트 › 메인 메뉴 › 메뉴"(없으면 메뉴 이름) --%>
@param String menuPath = null
@param String apiBase = null
@param List<net.luversof.web.dynamiccrud2.menu.service.MenuPageService.ApiCall> apiCalls = java.util.List.of()

!{var menuId = menu.getId().toString();}

<%-- 순서: 기본 정보 → 쿼리 설정 → 테이블 정보(접힘) → 검색 설정(쿼리를 먼저 정하고 검색은 그 뒤에 다듬는다). 테이블 정보는 쿼리 작성 화면에도(펼침). 메뉴 삭제는 제목 줄 오른쪽 --%>
<%-- 변경 이력 · 메뉴 복사 · 메뉴 삭제는 화면 제목(메뉴 관리) 줄 오른쪽 끝(사용자 결정 11-24 - 예전엔 제목 아래 따로 한 줄) --%>
@template._layout.layout(title = t("admin.menu.title", menuPath == null ? menu.getName() : menuPath), breadcrumb = breadcrumb, titleActions = @`
	<a class="btn btn-sm" href="/admin/menu/${menuId}/history">${t("admin.history.title")}</a>
	<%-- 복사: 같은 메인 메뉴에 쿼리 · 검색 설정까지 그대로, 숨김으로 만든다(MenuService.copyMenu) --%>
	<span class="inline-flex items-center gap-1">
		<button type="button" class="btn btn-sm" data-toggle="copy-menu" aria-controls="copy-menu" aria-expanded="false">${t("admin.menu.copy")}</button>
		@template._components.help(text = t("admin.menu.copy.hint"), label = t("admin.menu.copy.helpLabel"), alignEnd = true)
	</span>
	<form method="post" action="/admin/menu/${menuId}/delete">
		<button class="btn btn-sm btn-error" data-confirm="${t("admin.menu.deleteConfirm")}">${t("admin.menu.delete")}</button>
	</form>
`, content = @`
	@template._components.codeNameForm(formId = "copy-menu", action = "/admin/menu/" + menuId + "/copy", noun = t("admin.menu.copy.noun"),
		name = t("admin.menu.copy.name", menu.getName()), code = menu.getCode() + "-copy", submitText = t("admin.menu.copy.submit"),
		cssClass = "justify-end mb-4")
	@template._components.alert(message = error)

	@template._components.guide(title = t("admin.menu.guide.title"), content = @`
		<p>${t("admin.menu.guide.intro")}</p>
		<ol class="mt-1">
			<li>$unsafe{t("admin.menu.guide.basic", targetNoun)}</li>
			<li>$unsafe{t("admin.menu.guide.query")}</li>
			<li>$unsafe{t("admin.menu.guide.search", targetNoun)}</li>
		</ol>
	`)

	@template._components.card(title = t("admin.menu.basic"), actions = @`
		<%-- 컬럼 라벨 · 입력 형식 · 정렬 허용 · 수정 가능은 테이블 단위 필드 화면에서 정한다(같은 테이블을 쓰는 메뉴가 공유) --%>
		<a class="btn btn-sm" href="/admin/table/${dbTable.getId().toString()}?menu=${menuId}">${t("admin.table.name", targetNoun)}</a>
	`, body = @`
		<%-- 메뉴를 만들 때 고른 데이터 출처: 메뉴의 정체라 부연(우상단 작은 글씨)이 아니라 맨 위 첫 줄에 --%>
		@template._components.sourceInfo(dbTable = dbTable, databaseType = databaseType, targetNoun = targetNoun, cssClass = "mb-3 pb-3 border-b border-base-300")
		<%-- 넓은 화면: 이름(넓게) · 주소 이름 · 권한 · 페이지 크기(숫자만큼) 한 줄, 아래 줄에 체크 둘 + 저장 --%>
		<form method="post" action="/admin/menu/${menuId}" class="grid gap-3 sm:grid-cols-2 lg:grid-cols-[minmax(0,2fr)_minmax(0,1.5fr)_minmax(0,1.5fr)_6rem]">
			@template._components.field(label = t("field.name"), forId = "name", content = @`
				<input id="name" class="input w-full" name="name" value="${menu.getName()}" required maxlength="100" />
			`)
			@template._components.field(label = t("field.code"), forId = "code", help = t("field.code.help"), content = @`
				@template._components.codeInput(id = "code", value = menu.getCode(), cssClass = "w-full")
			`)
			@template._components.field(label = t("field.authority"), forId = "authority", help = t("field.authority.help"), content = @`
				<input id="authority" class="input w-full" name="authority" value="${menu.getAuthority()}" maxlength="50"
					placeholder="${t("field.authority.placeholder")}" />
			`)
			@template._components.field(label = t("field.pageSize"), forId = "pageSize", help = t("field.pageSize.help"), content = @`
				<input id="pageSize" class="input w-full" name="pageSize" type="number" min="1" value="${menu.getPageSize()}" />
			`)
			<div class="sm:col-span-2 lg:col-span-4 flex flex-wrap items-center gap-x-5 gap-y-2">
				<label class="flex items-center gap-2 text-sm">
					<input class="checkbox" type="checkbox" name="enableDisplay" value="true" checked="${menu.isEnableDisplay()}" /> ${t("field.enableDisplay")}
				</label>
				<label class="flex items-center gap-2 text-sm">
					<input class="checkbox" type="checkbox" name="enableExcel" value="true" checked="${menu.isEnableExcel()}" /> ${t("field.enableExcel")}
				</label>
				@template._components.help(text = t("field.enableDisplay.help"), label = t("common.helpFor", t("field.enableDisplay")))
				<button class="btn btn-sm btn-primary ml-auto">${t("common.save")}</button>
			</div>
		</form>
	`)

	@template._components.card(title = t("admin.menu.queries"), help = t("admin.menu.queries.help"), actions = @`
		<%-- 쿼리 입력 · 수정은 이 화면의 레이어(queryDialog)로 연다. href 는 새 탭으로 열 때(단독 화면) --%>
		<a class="btn btn-sm" href="/admin/menu/${menuId}/query/new?kind=INSERT" hx-get="/admin/menu/${menuId}/query/new?kind=INSERT" hx-target="#queryPanel">${t("admin.query.addInsert")}</a>
		<a class="btn btn-sm" href="/admin/menu/${menuId}/query/new?kind=UPDATE" hx-get="/admin/menu/${menuId}/query/new?kind=UPDATE" hx-target="#queryPanel">${t("admin.query.addUpdate")}</a>
		<a class="btn btn-sm" href="/admin/menu/${menuId}/query/new?kind=DELETE" hx-get="/admin/menu/${menuId}/query/new?kind=DELETE" hx-target="#queryPanel">${t("admin.query.addDelete")}</a>
	`, body = @`
		<table class="table" aria-label="${t("admin.menu.queries")}">
			<thead><tr><th>${t("index.col.kind")}</th><th>${t("field.name")}</th><th>${t("field.authority")}</th><th>${t("admin.query.col.query")}</th><th></th></tr></thead>
			<tbody>
				@for(var query : queries)
					<tr>
						<td class="w-px"><span class="badge ${query.getKind() == QueryKind.READ ? "badge-primary" : ""}">${query.getKind().name()}</span></td>
						!{var editUrl = "/admin/menu/" + menuId + "/query/" + query.getId();}
						<td class="whitespace-nowrap"><a class="link" href="${editUrl}" hx-get="${editUrl}" hx-target="#queryPanel">${query.getName()}</a></td>
						<td class="whitespace-nowrap">${query.getAuthority() == null ? t("admin.query.menuAuthority") : query.getAuthority()}</td>
						<td class="font-mono text-xs max-w-0 w-full truncate" title="${query.getQueryText()}">${query.getQueryText()}</td>
						<td class="w-px">
							<div class="flex items-center justify-end gap-1">
								<a class="btn btn-sm" href="${editUrl}" hx-get="${editUrl}" hx-target="#queryPanel" aria-label="${t("admin.query.editItem", query.getName())}">✎ ${t("common.edit")}</a>
								@if(query.getKind() != QueryKind.READ)
									<form method="post" action="${editUrl}/delete">
										<button class="btn btn-sm btn-ghost text-error" data-confirm="${t("admin.query.deleteConfirm")}" aria-label="${t("admin.deleteItem", query.getName())}">${t("common.delete")}</button>
									</form>
								@endif
							</div>
						</td>
					</tr>
				@endfor
			</tbody>
		</table>
		@template._components.modal(id = "queryDialog", panelId = "queryPanel", label = t("admin.menu.queries"), wide = true)
	`)

	<%-- JSON 호출 설명: 위 쿼리 · 아래 검색 설정에서 만든다(MenuPageService.apiDoc). 처음엔 접어 둔다 --%>
	@if(apiBase != null && !apiCalls.isEmpty())
		@template._components.card(title = t("admin.menu.api"), help = t("admin.menu.api.help"), collapsible = true, open = false, body = @`
			<div class="grid gap-5">
				@for(var call : apiCalls)
					<section class="grid gap-2">
						<p class="flex flex-wrap items-center gap-2">
							<%-- 쿼리 설정 표와 같은 모양: 종류 배지 · 이름 먼저, 그 뒤 메서드 · 주소 --%>
							<span class="badge ${call.kind() == net.luversof.web.dynamiccrud2.domain.QueryKind.READ ? "badge-primary" : ""}">${call.kind().name()}</span>
							<span class="font-semibold">${call.name()}</span>
							<span class="hint font-mono">${call.method()}</span>
							<code class="code text-sm break-all">${apiBase}${call.path()}</code>
						</p>
						@if(call.rows())
							<p class="hint">$unsafe{t("admin.menu.api.rows")}</p>
						@endif
						@if(!call.params().isEmpty())
							<div class="overflow-x-auto">
								<table class="table" aria-label="${t("admin.menu.api.params", call.name())}">
									<thead><tr><th>${call.method().equals("GET") ? t("admin.menu.api.col.query") : t("admin.menu.api.col.input")}</th><th>${t("admin.table.col.required")}</th><th>${t("admin.menu.api.col.desc")}</th></tr></thead>
									<tbody>
										@for(var param : call.params())
											<tr><td class="font-mono text-xs whitespace-nowrap">${param.name()}</td><td>${param.required() ? "✓" : ""}</td><td>${param.description()}</td></tr>
										@endfor
									</tbody>
								</table>
							</div>
						@endif
					</section>
				@endfor
				<div class="grid gap-2 sm:grid-cols-2">
					<div><p class="text-sm font-semibold mb-1">${t("admin.menu.api.example.list")}</p><pre class="code text-xs overflow-x-auto">{"total": 1, "page": 0, "pageSize": 20, "sort": null, "descending": false, "message": null,
 "columns": [{"label": "id", "header": "id", "inputType": "TEXT", "sortable": true}],
 "rows": [{"values": ["…"], "row": "…"}]}</pre></div>
					<div><p class="text-sm font-semibold mb-1">${t("admin.menu.api.example.action")}</p><pre class="code text-xs overflow-x-auto">POST  {"inputs": {"name": "…"}, "rows": ["…"]}
200   {"affected": 1}
400   {"error": "…"}</pre></div>
				</div>
			</div>
		`)
	@endif

	<%-- 검색 설정 위 테이블 정보(접힘): 검색칸마다 인덱스 사용 여부가 이미 보여, 전체 구조가 필요할 때만 펼친다(2026-11-23 사용자 결정) --%>
	@template._components.tableInfo(tables = search.tables(), open = false, targetNoun = targetNoun, rememberKey = "tableInfo")
	@template._components.card(title = t("admin.menu.search"), help = t("admin.menu.search.help"), body = @`
		@if(!search.parsed())
			@template._components.alert(message = t("admin.menu.search.unparsed"), tone = "info", cssClass = "")
		@elseif(search.candidates().isEmpty())
			@template._components.emptyState(message = t("admin.menu.search.noColumns"))
		@else
			@template.admin.searchSettings(search = search, menuId = menuId)
		@endif
	`)
`)
