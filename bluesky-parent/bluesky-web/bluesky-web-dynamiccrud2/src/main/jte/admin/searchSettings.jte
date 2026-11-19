<%-- 메뉴 검색 설정(추가형): 고른 것만 목록으로 보이고 "+ 추가" 로 고른다. 저장 단추는 다른 카드처럼 아래 오른쪽.
     - 검색칸: 사용 화면 검색 폼에 보일 칸 · 검색 방식 · 인덱스 사용 여부, ⠿ 로 순서(그 순서대로 폼에 나온다)
     - 통합 검색 대상: 검색어 하나로 함께 찾을 글자 컬럼 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import net.luversof.web.dynamiccrud2.admin.service.SearchService.SearchSetup

@param SearchSetup search
@param String menuId

<form id="search-form" method="post" action="/admin/menu/${menuId}/search" class="grid gap-5" aria-label="${t("admin.menu.search")}">
	<section class="grid gap-2" data-picklist>
		<div class="flex flex-wrap items-center gap-2">
			<h3 class="text-sm font-semibold">${t("admin.search.fields")}</h3>
			<span class="hint">${t("admin.search.fields.hint")}</span>
			@template._components.combobox(id = "add-search-field", placeholder = t("admin.search.fields.add"), required = false,
				ariaLabel = t("admin.search.fields.addLabel"), emptyText = t("admin.search.fields.none"), cssClass = "ml-auto w-72", options = @`
					@for(var candidate : search.candidates())
						@if(!candidate.searchOn())
							@template._components.pickOption(value = candidate.label(), label = candidate.header(),
								sub = candidate.indexUse().get(candidate.operators().get(0)).text())
						@endif
					@endfor
				`)
		</div>
		<%-- 줄이 화면 끝까지 늘어나면 이름과 방식 사이가 멀어진다 - 폭을 묶는다 --%>
		<ul class="grid gap-1 max-w-4xl" data-picklist-items data-reorder-url="">
			@for(var candidate : search.candidates())
				@template._components.searchFieldRow(candidate = candidate)
			@endfor
		</ul>
		<p class="hint" data-picklist-empty hidden="${search.candidates().stream().anyMatch(c -> c.searchOn())}">${t("admin.search.fields.empty")}</p>
	</section>

	<section class="grid gap-2" data-picklist>
		<div class="flex flex-wrap items-center gap-2">
			<h3 class="text-sm font-semibold">${t("admin.search.keyword")}</h3>
			<span class="hint">${t("admin.search.keyword.hint")}</span>
			@template._components.combobox(id = "add-keyword", placeholder = t("admin.search.keyword.add"), required = false,
				ariaLabel = t("admin.search.keyword.addLabel"), emptyText = t("admin.search.keyword.none"), cssClass = "ml-auto w-72", options = @`
					@for(var candidate : search.keywordCandidates())
						@if(!candidate.keywordOn())
							@template._components.pickOption(value = candidate.label(), label = candidate.header())
						@endif
					@endfor
				`)
		</div>
		<div class="flex flex-wrap gap-1.5" data-picklist-items>
			@for(var candidate : search.keywordCandidates())
				@template._components.pickChip(key = candidate.label(), label = candidate.header(), name = "kw", picked = candidate.keywordOn())
			@endfor
		</div>
		<p class="hint" data-picklist-empty hidden="${search.keywordCandidates().stream().anyMatch(c -> c.keywordOn())}">${t("admin.search.keyword.empty")}</p>
	</section>
	@template._components.formActions(content = @`
		<button class="btn btn-sm btn-primary">${t("admin.search.save")}</button>
	`)
</form>
