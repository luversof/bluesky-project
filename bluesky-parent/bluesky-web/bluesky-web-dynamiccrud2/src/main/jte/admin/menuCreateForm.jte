<%-- 메인 메뉴 아래에 펼치는 메뉴 추가 폼(관리 화면 안). connection 을 고르면 그 DB 의 테이블 목록을 받아 거르며 고른다.
     추가하면 조회 쿼리 · 필드가 만들어지고 관리 화면에 머문다. 검색 · 작업은 메뉴 이름을 눌러 메뉴 설정 화면에서 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@param String mainMenuId
@param String mainMenuName

!{var prefix = "nm-" + mainMenuId;}

<form id="add-menu-${mainMenuId}" method="post" action="/admin/mainMenu/${mainMenuId}/menu" class="card mt-2 ml-6 max-w-5xl grid gap-3 sm:grid-cols-2 xl:grid-cols-4" hidden
	aria-label="${t("admin.menuCreate.label", mainMenuName)}">
	@template._components.field(label = t("admin.menuCreate.name"), forId = prefix + "-name", content = @`
		<input id="${prefix}-name" class="input input-sm w-full" name="name" required maxlength="100" />
	`)
	@template._components.field(label = t("field.code"), forId = prefix + "-code", content = @`
		@template._components.codeInput(id = prefix + "-code", placeholder = t("admin.menuCreate.codePlaceholder"), small = true, cssClass = "w-full")
	`)
	@template._components.field(label = "connection", forId = prefix + "-conn-input", content = @`
		@template._components.combobox(id = prefix + "-conn", name = "connectionName", placeholder = t("admin.menuCreate.connPlaceholder"),
			searchUrl = "/admin/connection/search", emptyText = t("admin.menuCreate.connEmpty"))
	`)
	@template._components.field(label = t("target.table"), forId = prefix + "-table-input", content = @`
		@template._components.combobox(id = prefix + "-table", name = "table", placeholder = t("admin.menuCreate.tablePlaceholder"),
			loadUrl = "/admin/connection/tables", loadTrigger = "change from:#" + prefix + "-conn", loadInclude = "#" + prefix + "-conn",
			emptyText = t("admin.menuCreate.tableEmpty"))
	`)
	<%-- 안내는 단추와 한 줄(왼쪽 안내 · 오른쪽 단추, 좁으면 줄바꿈) --%>
	@template._components.formActions(cssClass = "sm:col-span-2 xl:col-span-4", content = @`
		<p class="hint mr-auto">${t("admin.menuCreate.hint")}</p>
		<button type="button" class="btn btn-sm btn-ghost" data-toggle="add-menu-${mainMenuId}">${t("common.cancel")}</button>
		<button class="btn btn-sm btn-primary">${t("common.add")}</button>
	`)
</form>
