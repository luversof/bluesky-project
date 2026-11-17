@import java.util.List
@import net.luversof.web.dynamiccrud2.setting.domain.DbTable
@import net.luversof.web.dynamiccrud2.setting.domain.Menu
@import net.luversof.web.dynamiccrud2.setting.domain.MenuQuery
@import net.luversof.web.dynamiccrud2.setting.domain.QueryKind

@param Menu menu
@param DbTable dbTable
@param List<MenuQuery> queries
@param String error = null

!{var menuId = menu.getId().toString();}

@template._layout.layout(title = "메뉴: " + menu.getName(), content = @`
	<p class="mb-4 text-sm"><a class="hover:underline" href="/setting">← 설정</a></p>
	@if(error != null)
		<div class="alert alert-error mb-4" role="alert">${error}</div>
	@endif

	<section class="card mb-6">
		<h2 class="font-semibold mb-3">기본 정보</h2>
		<form method="post" action="/setting/menu/${menuId}" class="grid gap-4 sm:grid-cols-2">
			<div>
				<label class="label" for="name">이름</label>
				<input id="name" class="input w-full" name="name" value="${menu.getName()}" required maxlength="100" />
			</div>
			<div>
				<label class="label" for="authority">권한</label>
				<input id="authority" class="input w-full" name="authority" value="${menu.getAuthority()}" placeholder="비우면 로그인한 사용자 누구나" maxlength="50" />
				<p class="hint mt-1">역할 하나(예: ROLE_ADMIN). ROLE_MASTER &gt; ROLE_ADMIN &gt; ROLE_USER 계층을 따릅니다.</p>
			</div>
			<div>
				<label class="label" for="pageSize">페이지 크기</label>
				<input id="pageSize" class="input w-32" name="pageSize" type="number" min="1" value="${menu.getPageSize()}" />
			</div>
			<div>
				<label class="label" for="displayOrder">순서</label>
				<input id="displayOrder" class="input w-32" name="displayOrder" type="number" value="${menu.getDisplayOrder()}" />
			</div>
			<label class="flex items-center gap-2 text-sm">
				<input class="checkbox" type="checkbox" name="enableDisplay" value="true" checked="${menu.isEnableDisplay()}" /> 메뉴 목록에 표시
			</label>
			<label class="flex items-center gap-2 text-sm">
				<input class="checkbox" type="checkbox" name="enableExcel" value="true" checked="${menu.isEnableExcel()}" /> 엑셀 내려받기
			</label>
			<div class="sm:col-span-2 flex items-center gap-2">
				<button class="btn btn-primary">저장</button>
				<span class="hint ml-auto">
					기준 테이블:
					<a class="hover:underline" href="/setting/table/${dbTable.getId().toString()}">${dbTable.getConnectionName()} · ${dbTable.getSchemaName() == null ? "" : dbTable.getSchemaName() + "."}${dbTable.getTableName()}</a>
					(필드 설정)
				</span>
			</div>
		</form>
	</section>

	<section class="card mb-6">
		<div class="flex flex-wrap items-center gap-2 mb-3">
			<h2 class="font-semibold">작업</h2>
			<span class="hint">조회는 하나, 입력 · 수정 · 삭제는 여러 개 둘 수 있습니다.</span>
			<span class="ml-auto flex gap-2">
				<a class="btn btn-sm" href="/setting/menu/${menuId}/query/new?kind=INSERT">입력 추가</a>
				<a class="btn btn-sm" href="/setting/menu/${menuId}/query/new?kind=UPDATE">수정 추가</a>
				<a class="btn btn-sm" href="/setting/menu/${menuId}/query/new?kind=DELETE">삭제 추가</a>
			</span>
		</div>
		<table class="table">
			<thead><tr><th>종류</th><th>이름</th><th>권한</th><th>쿼리</th><th></th></tr></thead>
			<tbody>
				@for(var query : queries)
					<tr>
						<td><span class="badge ${query.getKind() == QueryKind.READ ? "badge-primary" : ""}">${query.getKind().name()}</span></td>
						<td><a class="hover:underline" href="/setting/menu/${menuId}/query/${query.getId().toString()}">${query.getName()}</a></td>
						<td>${query.getAuthority() == null ? "메뉴 권한" : query.getAuthority()}</td>
						<td class="font-mono text-xs max-w-md truncate" title="${query.getQueryText()}">${query.getQueryText()}</td>
						<td class="text-right">
							@if(query.getKind() != QueryKind.READ)
								<form method="post" action="/setting/menu/${menuId}/query/${query.getId().toString()}/delete">
									<button class="btn btn-sm btn-ghost" onclick="return confirm('작업을 지울까요?')">삭제</button>
								</form>
							@endif
						</td>
					</tr>
				@endfor
			</tbody>
		</table>
	</section>

	<form method="post" action="/setting/menu/${menuId}/delete">
		<button class="btn btn-error" onclick="return confirm('메뉴를 지울까요? 필드 설정은 다른 메뉴와 공유하므로 남습니다.')">메뉴 삭제</button>
	</form>
`)
