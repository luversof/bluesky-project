@import java.util.List
@param List<String[]> baseHits = List.of()
@param String baseQuery = ""
<%-- 제작할 아이템 검색 결과(10-21 C527) — 누르면 그 종류 · 변형 · 베이스로 연다 --%>
!{ boolean ko = net.luversof.web.gate.poe.PoeText.isKorean(); }
@if(baseQuery.length() >= 2)
	@if(baseHits.isEmpty())
		<p class="text-xs text-base-content/60 px-1 py-1" data-craft-base-none>${ko ? "제작할 수 있는 베이스가 없습니다" : "No craftable base found"}</p>
	@else
		<ul class="menu menu-sm bg-base-100 rounded-box border border-base-300 p-1 max-h-72 overflow-y-auto" data-craft-base-hits="${baseHits.size()}">
			@for(String[] h : baseHits)
				<li><a href="${h[0]}" class="flex flex-wrap gap-x-2 min-w-0" data-craft-base-hit><span class="font-medium wrap-anywhere">${h[1]}</span><span class="text-xs text-base-content/60 wrap-anywhere">${h[2]}</span></a></li>
			@endfor
		</ul>
	@endif
@endif
