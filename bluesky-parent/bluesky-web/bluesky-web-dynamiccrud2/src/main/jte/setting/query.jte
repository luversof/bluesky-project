@import java.util.UUID
@import net.luversof.web.dynamiccrud2.setting.domain.Menu
@import net.luversof.web.dynamiccrud2.setting.domain.QueryKind
@import net.luversof.web.dynamiccrud2.setting.service.QueryService.QueryInspection

@param Menu menu
@param UUID queryId
@param QueryKind kind
@param String name
@param String queryText
@param String authority
@param String error
@param QueryInspection inspection

!{var menuId = menu.getId().toString();}

@template._layout.layout(title = (queryId == null ? "작업 추가: " : "작업 편집: ") + kind.name(), content = @`
	<p class="mb-4 text-sm"><a class="hover:underline" href="/setting/menu/${menuId}">← ${menu.getName()}</a></p>
	@if(error != null)
		<div class="alert alert-error mb-4" role="alert">${error}</div>
	@endif

	<form method="post" action="/setting/menu/${menuId}/query" class="grid gap-4">
		<input type="hidden" name="kind" value="${kind.name()}" />
		@if(queryId != null)
			<input type="hidden" name="queryId" value="${queryId.toString()}" />
		@endif
		<div class="grid gap-4 sm:grid-cols-2">
			<div>
				<label class="label" for="name">이름</label>
				<input id="name" class="input w-full" name="name" value="${name}" maxlength="100" placeholder="비우면 종류 이름" />
			</div>
			@if(kind != QueryKind.READ)
				<div>
					<label class="label" for="authority">실행 권한</label>
					<input id="authority" class="input w-full" name="authority" value="${authority}" maxlength="50" placeholder="비우면 메뉴 권한을 따릅니다" />
				</div>
			@endif
		</div>
		<div>
			<label class="label" for="queryText">쿼리</label>
			<textarea id="queryText" class="textarea code w-full" name="queryText" rows="12" spellcheck="false" required>${queryText}</textarea>
			<p class="hint mt-1">
				파라미터는 <code>:이름</code> 으로 씁니다. 쓰던 쿼리를 그대로 붙여 넣으면 아래 표가 쿼리에서 만들어집니다.
				@if(kind == QueryKind.READ)
					WHERE 에 쓴 파라미터는 검색 조건이 되고, 기본은 필수입니다. 페이징이 없으면 자동으로 붙습니다(직접 하려면 <code>:_limit</code> · <code>:_offset</code>).
				@endif
			</p>
		</div>
		<div>
			<button type="button" class="btn btn-sm" hx-post="/setting/menu/${menuId}/query/inspect" hx-include="closest form" hx-target="#inspection" hx-swap="innerHTML">쿼리 다시 읽기</button>
		</div>
		<div id="inspection">
			@template.setting.queryInspection(kind = kind, inspection = inspection)
		</div>
		<div class="flex gap-2">
			<button class="btn btn-primary">저장</button>
			<a class="btn btn-ghost" href="/setting/menu/${menuId}">취소</a>
		</div>
	</form>
`)
