@import java.util.List
@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param List<Poe2.ItemClass> classes = java.util.List.of()
@param String target
@param String activeClass = "all"

<%-- 아이템 분류 칩 — PoE1 groupChips(poe/groupChips.jte)와 같은 모양: 분류(무기 · 보조 · 방어구 · 장신구 · 플라스크 · 주얼)별 줄 + 왼쪽 줄 이름.
     (10-01: PoE2 베이스 목록은 줄 이름이 없었고, 고유 목록은 한 줄에 전부 늘어놓았다.) 줄 이름은 PoE1 과 같은 메시지 키 poe.itemgroup.* --%>
!{
	java.util.Map<String, List<Poe2.ItemClass>> byCategory = new java.util.LinkedHashMap<>();
	for (String c : List.of("weapon", "offhand", "armour", "jewellery", "flask", "jewel", "other")) byCategory.put(c, new java.util.ArrayList<>());
	for (Poe2.ItemClass c : classes) byCategory.getOrDefault(c.category(), byCategory.get("other")).add(c);
}
<div class="space-y-1.5" data-chip-group data-chip-target="${target}" data-chip-field="itemClass">
	<div class="flex flex-wrap items-center gap-1.5">
		<span class="w-16 shrink-0"></span>
		<button type="button" class="poe-chip ${"all".equals(activeClass) ? "poe-chip-active" : ""}" data-chip-value="all">${MessageUtil.getMessage("poe.uniques.category.all")}</button>
	</div>
	@for(java.util.Map.Entry<String, List<Poe2.ItemClass>> row : byCategory.entrySet())
		@if(!row.getValue().isEmpty())
			<div class="flex flex-wrap items-center gap-1.5" data-chip-row="${row.getKey()}">
				<span class="w-16 shrink-0 text-xs font-semibold text-base-content/60">${MessageUtil.getMessage("poe.itemgroup." + row.getKey())}</span>
				@for(Poe2.ItemClass c : row.getValue())
					<button type="button" class="poe-chip ${c.key().equals(activeClass) ? "poe-chip-active" : ""}" data-chip-value="${c.key()}" data-slot="${c.key()}">${PoeText.name(c.ko(), c.enName())}</button>
				@endfor
			</div>
		@endif
	@endfor
</div>
