@import java.util.List
@param List<String[]> buildItems = List.of()
@param String importUrl = "/poe/htmx/craft/import"
<%-- PoB 빌드 코드 붙여넣기(10-21 C525) — 빌드의 일반 · 매직 · 희귀 아이템 목록(고유 제외), '이 아이템으로'는 PoB 글로 되살린 아이템을 같은 가져오기로 보낸다 --%>
!{ boolean ko = net.luversof.web.gate.poe.PoeText.isKorean(); }
<span class="block space-y-1" data-craft-import-build="${buildItems.size()}">
	@if(buildItems.isEmpty())
		<span class="block text-sm text-error" data-craft-import-error>${ko ? "이 빌드엔 제작할 수 있는 아이템(일반 · 매직 · 희귀)이 없습니다" : "This build has no craftable (normal, magic or rare) items"}</span>
	@else
		<span class="block text-xs text-base-content/70">${ko ? "빌드 아이템 " + buildItems.size() + "개 — 고르면 그 아이템으로 시작합니다(고유 제외 · 아이템 레벨은 84 가정)" : buildItems.size() + " build items — pick one to start from it (uniques skipped, item level 84 assumed)"}</span>
		@for(String[] it : buildItems)
			<span class="flex flex-wrap items-center gap-2 text-xs min-w-0" data-craft-import-build-item>
				<button type="button" class="btn btn-xs btn-ghost border border-base-300 shrink-0" hx-post="${importUrl}" hx-vals="${it[2]}" hx-target="[data-craft-import-msg]" hx-swap="innerHTML" data-craft-import-build-pick>${ko ? "이 아이템으로" : "Use this"}</button>
				<span class="badge badge-ghost badge-sm shrink-0">${"RARE".equals(it[1]) ? (ko ? "희귀" : "Rare") : "MAGIC".equals(it[1]) ? (ko ? "마법" : "Magic") : (ko ? "일반" : "Normal")}</span>
				<span class="min-w-0 wrap-anywhere">${it[0]}</span>
			</span>
		@endfor
	@endif
</span>
