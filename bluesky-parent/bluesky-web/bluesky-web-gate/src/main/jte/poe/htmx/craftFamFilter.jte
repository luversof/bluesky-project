@import net.luversof.web.gate.poe.PoeText
@param java.util.List<String> tags
<%-- 계열 표 거르기(10-22 P1, poedb 속성 부여의 태그 칩 · 검색 — 계열이 쪽마다 수십 개라 원하는 옵션 찾기가 불편했다). PoE1 · PoE2 craftPoolGroups 공용.
     브라우저 안에서만 거른다(common.ts applyFamFilter — 이름 없는 입력이라 제작 요청에 섞이지 않는다), 화폐를 써서 표가 바뀌어도 같은 거르기(restoreFamFilter).
     태그 여러 개 = 모두 가진 계열 --%>
!{ boolean ko = PoeText.isKorean(); }
<div class="space-y-1 rounded-box border border-base-300 bg-base-100 px-2 py-1.5 text-xs" data-craft-fam-filter>
	<div class="flex flex-wrap items-center gap-2">
		<input type="search" class="input input-xs input-bordered w-40 min-w-0" placeholder="${ko ? "옵션 찾기" : "Find a modifier"}" aria-label="${ko ? "옵션 찾기" : "Find a modifier"}" data-craft-fam-q>
		<label class="flex items-center gap-1 cursor-pointer"><input type="checkbox" class="checkbox checkbox-xs" data-craft-fam-rollable>${ko ? "붙을 수 있는 것만" : "Rollable only"}</label>
		<span class="font-mono text-base-content/70" aria-live="polite" data-craft-fam-count></span>
		<button type="button" class="btn btn-xs btn-ghost" data-craft-fam-clear>${ko ? "지우기" : "Clear"}</button>
	</div>
	@if(!tags.isEmpty())
		<div class="flex flex-wrap gap-1" role="group" aria-label="${ko ? "태그로 거르기" : "Filter by tag"}">
			@for(String t : tags)<button type="button" class="btn btn-xs btn-ghost border border-base-300" aria-pressed="false" data-craft-fam-tag="${t}">${t}</button>@endfor
		</div>
	@endif
</div>
