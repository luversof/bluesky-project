<%-- 작업 쿼리 입력 · 수정 폼. 메뉴 관리 화면의 레이어(layer)로도, 단독 화면(admin/query)으로도 쓴다 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import java.util.UUID
@import net.luversof.web.dynamiccrud2.domain.Menu
@import net.luversof.web.dynamiccrud2.domain.QueryKind
@import net.luversof.web.dynamiccrud2.admin.service.QueryService.QueryInspection

@param Menu menu
@param UUID queryId
@param QueryKind kind
@param String name
@param String queryText
@param String authority
@param String error
@param QueryInspection inspection
@param boolean layer = false
@param java.util.List<net.luversof.web.dynamiccrud2.admin.service.SearchService.TableIndexes> indexTables = java.util.List.of()

!{var menuId = menu.getId().toString();}

<%-- id 는 q- 로 시작한다(레이어로 뜨면 메뉴 관리 화면의 기본 정보 칸과 같은 화면에 있다).
     layer: 메뉴 관리 화면의 레이어(_components/modal)에 htmx 로 뜬다 - 저장은 htmx(성공하면 메뉴 관리 화면을 새로 고침, 실패하면 이 조각을 사유와 함께 다시),
     취소는 레이어 닫기. 아니면 단독 화면(주소로 바로 열었을 때) --%>
<form method="post" action="/admin/menu/${menuId}/query" class="${layer ? "card grid gap-4" : null}"
	hx-post="${layer ? "/admin/menu/" + menuId + "/query" : null}" hx-target="${layer ? "#queryPanel" : null}">
	@if(layer)
		<h2 class="font-semibold">${t(queryId == null ? "admin.query.title.add" : "admin.query.title.edit", kind.name())}</h2>
	@endif
	@template._components.alert(message = error, cssClass = layer ? "" : "mb-4")
	<input type="hidden" name="kind" value="${kind.name()}" />
	@if(queryId != null)
		<input type="hidden" name="queryId" value="${queryId.toString()}" />
	@endif
	@template._components.card(title = t("admin.menu.basic"), body = @`
	<div class="grid gap-4 sm:grid-cols-2">
		@template._components.field(label = t("field.name"), forId = "q-name", content = @`
			<input id="q-name" class="input w-full" name="name" value="${name}" maxlength="100" placeholder="${t("admin.query.name.placeholder")}" />
		`)
		@if(kind != QueryKind.READ)
			@template._components.field(label = t("admin.query.authority"), forId = "q-authority", content = @`
				<input id="q-authority" class="input w-full" name="authority" value="${authority}" maxlength="50" placeholder="${t("admin.query.authority.placeholder")}" />
			`)
		@endif
	</div>
	`)
	<%-- 테이블 정보: 쿼리를 쓰기 바로 전에 본다 - 컬럼(입력 · 수정 쿼리에 넣을 값 · NOT NULL · 기본값 · 자동 증가)과
	     인덱스(WHERE · ORDER BY 가 인덱스를 타는지). 인덱스는 메뉴 관리 화면에서 옮겨 왔다(2026-11-23 사용자 결정) --%>
	@template._components.tableInfo(tables = indexTables, rememberKey = "tableInfo")
	@template._components.card(title = t("admin.query.col.query"), actions = @`
		<span class="hint">${t("admin.query.test.hint")}</span>
		<button type="button" class="btn btn-sm" hx-post="/admin/menu/${menuId}/query/inspect" hx-include="closest form" hx-target="#q-inspection" hx-swap="innerHTML">${t("admin.query.test")}</button>
	`, body = @`
	<div>
		<%-- 처음엔 작게(두세 줄), 줄이 늘면 내용만큼 커진다(.textarea-autogrow: CSS field-sizing, 안 되는 브라우저는 js/ui.js) --%>
		<textarea aria-label="${t("admin.query.col.query")}" id="q-queryText" class="textarea textarea-autogrow code w-full" name="queryText" rows="3" spellcheck="false" required>${queryText}</textarea>
		<p class="hint mt-1">
			$unsafe{t("admin.query.paramHint")}
			@if(kind == QueryKind.READ)
				$unsafe{t("admin.query.readHint")}
			@endif
		</p>
	</div>
	<div id="q-inspection" class="mt-4">
		@template.admin.queryInspection(kind = kind, inspection = inspection, menuId = menu.getId())
	</div>
	`)
	@template._components.formActions(content = @`
		@if(layer)
			<button type="button" class="btn btn-ghost" data-modal-close>${t("common.cancel")}</button>
		@else
			<a class="btn btn-ghost" href="/admin/menu/${menuId}">${t("common.cancel")}</a>
		@endif
		<button class="btn btn-primary">${t("common.save")}</button>
	`)
</form>
