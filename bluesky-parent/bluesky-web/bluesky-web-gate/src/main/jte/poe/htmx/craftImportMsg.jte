@import java.util.List
@param String importError = null
@param List<String> importWarnings = List.of()
<%-- 게임 아이템 붙여넣기 결과(C485) — 못 읽으면 이유, 읽었으면 HX-Redirect 로 넘어가는 동안의 안내 --%>
!{ boolean ko = net.luversof.web.gate.poe.PoeText.isKorean(); }
@if(importError != null)
	<span class="text-sm text-error wrap-anywhere" data-craft-import-error>${importError}</span>
	@for(String w : importWarnings)
		<span class="block text-xs text-base-content/60 wrap-anywhere">${w}</span>
	@endfor
@else
	<span class="text-sm text-base-content/70" data-craft-import-ok>${ko ? "아이템을 여는 중…" : "Opening the item…"}</span>
@endif
