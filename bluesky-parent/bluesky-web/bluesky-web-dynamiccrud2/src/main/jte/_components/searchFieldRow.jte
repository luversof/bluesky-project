<%-- 검색칸 목록의 한 줄: ⠿ 컬럼 · 검색 방식 [▾] · 인덱스 사용 여부 · ×.
     고르지 않은 컬럼도 숨긴 채(입력 disabled - 폼으로 안 감) 그려 두고, "+ 검색칸 추가" 로 고르면 보인다(js/ui.js picklist).
     인덱스 문구는 방식마다 서버가 정해 option 의 data-* 에 싣고, 방식을 바꾸면 바로 바뀐다.
     폼 값: on = 결과 이름(화면 순서가 검색 폼 순서), op.결과이름 = 방식 --%>
@import net.luversof.web.dynamiccrud2.admin.service.SearchService.Candidate

@param Candidate candidate

!{var use = candidate.indexUse().get(candidate.operator());}
!{var selectId = "op-" + Integer.toHexString(candidate.label().hashCode());}

<li class="pick-row" data-id="${candidate.label()}" data-pick-key="${candidate.label()}" data-pick-label="${candidate.header()}"
	data-pick-sub="${candidate.indexUse().get(candidate.operators().get(0)).text()}" hidden="${!candidate.searchOn()}">
	@template._components.dragHandle(name = candidate.header())
	<span class="min-w-0 flex-1 truncate text-sm font-medium" title="${candidate.header()} (${candidate.label()})">${candidate.header()}
		@if(!candidate.header().equals(candidate.label()))<span class="hint font-mono font-normal">${candidate.label()}</span>@endif
	</span>
	<input type="hidden" name="on" value="${candidate.label()}" disabled="${!candidate.searchOn()}" />
	<label class="hint text-xs shrink-0" for="${selectId}">검색 방식</label>
	<select id="${selectId}" class="select select-sm shrink-0" name="op.${candidate.label()}" data-index-select disabled="${!candidate.searchOn()}">
		@for(var operator : candidate.operators())
			!{var u = candidate.indexUse().get(operator);}
			<option value="${operator.name()}" selected="${operator == candidate.operator()}"
				data-index-ok="${String.valueOf(u.ok())}" data-index-text="${u.text()}">${operator.title()}</option>
		@endfor
	</select>
	<span class="index-badge ${use.ok() ? "index-ok" : "index-warn"} w-56 truncate" data-index-badge title="${use.text()}">${use.ok() ? "✓ " : "⚠ "}${use.text()}</span>
	<button type="button" class="icon-btn icon-btn-danger shrink-0" data-pick-remove aria-label="${candidate.header()} 검색칸에서 빼기" title="빼기">×</button>
</li>
