<%-- 메인 메뉴 아래에 펼치는 메뉴 추가 폼(관리 화면 안). connection 을 고르면 그 DB 의 테이블 목록을 받아 거르며 고른다.
     추가하면 조회 쿼리 · 필드가 만들어지고 관리 화면에 머문다. 검색 · 작업은 메뉴 이름을 눌러 메뉴 설정 화면에서 --%>
@param String mainMenuId
@param String mainMenuName

!{var prefix = "nm-" + mainMenuId;}

<form id="add-menu-${mainMenuId}" method="post" action="/admin/mainMenu/${mainMenuId}/menu" class="card mt-2 ml-6 max-w-5xl grid gap-3 sm:grid-cols-2 xl:grid-cols-4" hidden
	aria-label="${mainMenuName} 에 메뉴 추가">
	@template._components.field(label = "메뉴 이름", forId = prefix + "-name", content = @`
		<input id="${prefix}-name" class="input input-sm w-full" name="name" required maxlength="100" />
	`)
	@template._components.field(label = "주소 이름", forId = prefix + "-code", content = @`
		@template._components.codeInput(id = prefix + "-code", placeholder = "예: coupon-list", small = true, cssClass = "w-full")
	`)
	@template._components.field(label = "connection", forId = prefix + "-conn-input", content = @`
		@template._components.combobox(id = prefix + "-conn", name = "connectionName", placeholder = "이름 일부를 입력하세요",
			searchUrl = "/admin/connection/search", emptyText = "이름에 그 글자가 든 connection 이 없습니다")
	`)
	@template._components.field(label = "테이블", forId = prefix + "-table-input", content = @`
		@template._components.combobox(id = prefix + "-table", name = "table", placeholder = "connection 을 고른 뒤 이름 일부를 입력하세요",
			loadUrl = "/admin/connection/tables", loadTrigger = "change from:#" + prefix + "-conn", loadInclude = "#" + prefix + "-conn",
			emptyText = "이름에 그 글자가 든 테이블이 없습니다")
	`)
	<%-- 안내는 단추와 한 줄(왼쪽 안내 · 오른쪽 단추, 좁으면 줄바꿈) --%>
	@template._components.formActions(cssClass = "sm:col-span-2 xl:col-span-4", content = @`
		<p class="hint mr-auto">고른 테이블의 컬럼을 필드로 등록하고(이미 등록된 테이블이면 공유) 조회 쿼리를 자동으로 만듭니다. 만든 뒤 메뉴 이름을 눌러 쿼리 · 검색을 설정합니다.</p>
		<button type="button" class="btn btn-sm btn-ghost" data-toggle="add-menu-${mainMenuId}">취소</button>
		<button class="btn btn-sm btn-primary">추가</button>
	`)
</form>
