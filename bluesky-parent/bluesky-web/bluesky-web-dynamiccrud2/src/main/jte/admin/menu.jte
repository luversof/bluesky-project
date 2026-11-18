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
@param String targetNoun = "테이블"
@param List<net.luversof.web.dynamiccrud2.admin.controller.Crumb> breadcrumb = null
@param String error = null

!{var menuId = menu.getId().toString();}

<%-- 순서: 기본 정보 → 쿼리 설정 → 테이블 인덱스 → 검색 설정(쿼리를 먼저 정하고 검색은 그 뒤에 다듬는다). 메뉴 삭제는 맨 위 오른쪽 --%>
@template._layout.layout(title = "메뉴 관리: " + menu.getName(), breadcrumb = breadcrumb, content = @`
	<%-- 메뉴 삭제는 이 화면의 주요 동작이라 스크롤 없이 보이게 맨 위 오른쪽(돌아가기는 위 위치 표시) --%>
	<div class="flex items-center gap-2 mb-4">
		<form method="post" action="/admin/menu/${menuId}/delete" class="ml-auto">
			<button class="btn btn-sm btn-error" data-confirm="메뉴를 지울까요? 필드 설정은 다른 메뉴와 공유하므로 남습니다.">메뉴 삭제</button>
		</form>
	</div>
	@template._components.alert(message = error)

	@template._components.card(title = "기본 정보", actions = @`
		<%-- 컬럼 라벨 · 입력 형식 · 정렬 허용 · 수정 가능은 테이블 단위 필드 화면에서 정한다(같은 테이블을 쓰는 메뉴가 공유) --%>
		<a class="btn btn-sm" href="/admin/table/${dbTable.getId().toString()}?menu=${menuId}">${targetNoun} 필드 관리</a>
	`, body = @`
		<%-- 메뉴를 만들 때 고른 데이터 출처: 메뉴의 정체라 부연(우상단 작은 글씨)이 아니라 맨 위 첫 줄에 --%>
		@template._components.sourceInfo(dbTable = dbTable, databaseType = databaseType, targetNoun = targetNoun, cssClass = "mb-3 pb-3 border-b border-base-300")
		<%-- 넓은 화면: 이름(넓게) · 주소 이름 · 권한 · 페이지 크기(숫자만큼) 한 줄, 아래 줄에 체크 둘 + 저장 --%>
		<form method="post" action="/admin/menu/${menuId}" class="grid gap-3 sm:grid-cols-2 lg:grid-cols-[minmax(0,2fr)_minmax(0,1.5fr)_minmax(0,1.5fr)_6rem]">
			@template._components.field(label = "이름", forId = "name", content = @`
				<input id="name" class="input w-full" name="name" value="${menu.getName()}" required maxlength="100" />
			`)
			@template._components.field(label = "주소 이름", forId = "code", content = @`
				@template._components.codeInput(id = "code", value = menu.getCode(), cssClass = "w-full")
			`)
			@template._components.field(label = "권한", forId = "authority", content = @`
				<input id="authority" class="input w-full" name="authority" value="${menu.getAuthority()}" maxlength="50"
					placeholder="비우면 로그인 사용자 누구나" title="역할 하나(예: ROLE_ADMIN). ROLE_MASTER > ROLE_ADMIN > ROLE_USER 계층을 따릅니다." />
			`)
			@template._components.field(label = "페이지 크기", forId = "pageSize", content = @`
				<input id="pageSize" class="input w-full" name="pageSize" type="number" min="1" value="${menu.getPageSize()}" />
			`)
			<div class="sm:col-span-2 lg:col-span-4 flex flex-wrap items-center gap-x-5 gap-y-2">
				<label class="flex items-center gap-2 text-sm">
					<input class="checkbox" type="checkbox" name="enableDisplay" value="true" checked="${menu.isEnableDisplay()}" /> 메뉴 목록에 표시
				</label>
				<label class="flex items-center gap-2 text-sm">
					<input class="checkbox" type="checkbox" name="enableExcel" value="true" checked="${menu.isEnableExcel()}" /> 엑셀 내려받기
				</label>
				<span class="hint">주소 이름을 바꾸면 예전 주소는 열리지 않습니다.</span>
				<button class="btn btn-sm btn-primary ml-auto">저장</button>
			</div>
		</form>
	`)

	@template._components.card(title = "쿼리 설정", actions = @`
		<span class="hint">목록 조회 쿼리는 하나, 입력 · 수정 · 삭제 쿼리는 여러 개(사용 화면의 단추가 됩니다)</span>
		<a class="btn btn-sm" href="/admin/menu/${menuId}/query/new?kind=INSERT">입력 추가</a>
		<a class="btn btn-sm" href="/admin/menu/${menuId}/query/new?kind=UPDATE">수정 추가</a>
		<a class="btn btn-sm" href="/admin/menu/${menuId}/query/new?kind=DELETE">삭제 추가</a>
	`, body = @`
		<table class="table" aria-label="쿼리">
			<thead><tr><th>종류</th><th>이름</th><th>권한</th><th>쿼리</th><th></th></tr></thead>
			<tbody>
				@for(var query : queries)
					<tr>
						<td class="w-px"><span class="badge ${query.getKind() == QueryKind.READ ? "badge-primary" : ""}">${query.getKind().name()}</span></td>
						!{var editUrl = "/admin/menu/" + menuId + "/query/" + query.getId();}
						<td class="whitespace-nowrap"><a class="link" href="${editUrl}">${query.getName()}</a></td>
						<td class="whitespace-nowrap">${query.getAuthority() == null ? "메뉴 권한" : query.getAuthority()}</td>
						<td class="font-mono text-xs max-w-0 w-full truncate" title="${query.getQueryText()}">${query.getQueryText()}</td>
						<td class="w-px">
							<div class="flex items-center justify-end gap-1">
								<a class="btn btn-sm" href="${editUrl}" aria-label="${query.getName()} 편집">✎ 편집</a>
								@if(query.getKind() != QueryKind.READ)
									<form method="post" action="${editUrl}/delete">
										<button class="btn btn-sm btn-ghost text-error" data-confirm="쿼리를 지울까요? 사용 화면에서 이 단추가 없어집니다." aria-label="${query.getName()} 삭제">삭제</button>
									</form>
								@endif
							</div>
						</td>
					</tr>
				@endfor
			</tbody>
		</table>
	`)

	@template._components.card(title = targetNoun + " 인덱스", actions = @`
		<span class="hint">검색칸은 인덱스의 첫 컬럼(굵게)을 완전 일치 · 범위로 찾을 때 인덱스를 씁니다. 실제 사용 여부는 DB 가 데이터 분포를 보고 정합니다.</span>
	`, body = @`
		<div class="grid gap-4 xl:grid-cols-2">
			@for(var table : search.tables())
				@template._components.indexTable(table = table)
			@endfor
		</div>
	`)

	@template._components.card(title = "검색 설정", actions = @`
		<span class="hint">조회 결과의 실제 컬럼 중 검색칸 · 통합 검색에 쓸 것을 고릅니다. 칸마다 고른 방식이 인덱스를 쓰는지 보입니다(⚠ 는 큰 테이블에서 느림).</span>
	`, body = @`
		@if(!search.parsed())
			@template._components.alert(message = "조회 쿼리 구조를 해석하지 못해 결과 컬럼을 고를 수 없습니다. 검색은 쿼리의 :파라미터 조건으로 하세요.", tone = "info", cssClass = "")
		@elseif(search.candidates().isEmpty())
			@template._components.emptyState(message = "고를 수 있는 컬럼이 없습니다(계산 결과 · * 만 있는 조회).")
		@else
			@template.admin.searchSettings(search = search, menuId = menuId)
		@endif
	`)
`)
