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
@param java.util.List<net.luversof.web.dynamiccrud2.admin.controller.Crumb> breadcrumb = null

!{var menuId = menu.getId().toString();}

@template._layout.layout(title = (queryId == null ? "쿼리 추가: " : "쿼리 편집: ") + kind.name(), breadcrumb = breadcrumb, content = @`
	@template._components.alert(message = error)

	<form method="post" action="/admin/menu/${menuId}/query">
		<input type="hidden" name="kind" value="${kind.name()}" />
		@if(queryId != null)
			<input type="hidden" name="queryId" value="${queryId.toString()}" />
		@endif
		@template._components.card(title = "기본 정보", body = @`
		<div class="grid gap-4 sm:grid-cols-2">
			@template._components.field(label = "이름", forId = "name", content = @`
				<input id="name" class="input w-full" name="name" value="${name}" maxlength="100" placeholder="비우면 종류 이름" />
			`)
			@if(kind != QueryKind.READ)
				@template._components.field(label = "실행 권한", forId = "authority", content = @`
					<input id="authority" class="input w-full" name="authority" value="${authority}" maxlength="50" placeholder="비우면 메뉴 권한을 따릅니다" />
				`)
			@endif
		</div>
		`)
		@template._components.card(title = "쿼리", actions = @`
			<span class="hint">DB 에서 쿼리를 확인하고 아래 표를 새로 만듭니다. 저장할 때도 같은 확인을 하고, 실패하면 저장하지 않습니다.</span>
			<button type="button" class="btn btn-sm" hx-post="/admin/menu/${menuId}/query/inspect" hx-include="closest form" hx-target="#inspection" hx-swap="innerHTML">쿼리 테스트</button>
		`, body = @`
		<div>
			<textarea aria-label="쿼리" id="queryText" class="textarea code w-full" name="queryText" rows="12" spellcheck="false" required>${queryText}</textarea>
			<p class="hint mt-1">
				파라미터는 <code>:이름</code> 으로 씁니다. 쓰던 쿼리를 그대로 붙여 넣으면 아래 표가 쿼리에서 만들어집니다.
				@if(kind == QueryKind.READ)
					WHERE 에 쓴 파라미터는 검색 조건이 되고, 기본은 필수입니다. 페이징이 없으면 자동으로 붙습니다(직접 하려면 <code>:_limit</code> · <code>:_offset</code>).
				@endif
			</p>
		</div>
		<div id="inspection" class="mt-4">
			@template.admin.queryInspection(kind = kind, inspection = inspection)
		</div>
		`)
		@template._components.formActions(content = @`
			<a class="btn btn-ghost" href="/admin/menu/${menuId}">취소</a>
			<button class="btn btn-primary">저장</button>
		`)
	</form>
`)
