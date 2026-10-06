@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText

<%-- PoE2 빌드 요약의 레어 · 마법 · 노멀 아이템 툴팁(10-04 C132, PoE1 buildSummary 의 itemTooltip 짝) — 예전엔 "옵션 N개" 접이식 목록뿐이었다.
     인게임 배치: 이름(레어는 이름 + 베이스 2줄) → 암시(구분선 위 별도 칸) → 옵션 → 키워드 Alt 설명. 고유는 자체 상세 툴팁을 쓴다. --%>
@param Poe2.BuildItem item

!{
	boolean rare = "RARE".equals(item.rarity());
	boolean magic = "MAGIC".equals(item.rarity());
	// 고유도 이 툴팁으로(10-04 C140 — 예전엔 고유 일반 상세라 이 빌드가 굴린 값이 안 보였다)
	boolean unique = "UNIQUE".equals(item.rarity());
	String rarityClass = unique ? "poe-rar-unique" : rare ? "poe-rar-rare" : magic ? "poe-rar-magic" : "poe-rar-white";
	String color = unique ? "#af6025" : rare ? "#ffff77" : magic ? "#8888ff" : "#c8c8c8";
	java.util.List<String> flavourLines = PoeText.lines(item.flavourKo(), item.flavour());
	java.util.List<String> lines = PoeText.isKorean() && item.modsKo() != null && item.modsKo().size() == item.mods().size() ? item.modsKo() : item.mods();
	int split = Math.min(item.implicitCount() == null ? 0 : item.implicitCount(), lines.size());
	java.util.List<String> implicits = lines.subList(0, split);
	java.util.List<String> explicits = lines.subList(split, lines.size());
	String base = PoeText.name(item.baseTypeKo(), item.baseType());
	String name = PoeText.name(item.nameKo(), item.name());
	boolean twoLine = (rare || unique) && base != null && !base.equals(name);
	// 타락은 옵션이 아니라 맨 아래 붉은 한 줄(10-05 C158) — 예전엔 옵션 칸 안에서 붉게
	boolean corruptedItem = explicits.stream().anyMatch(l -> "타락".equals(l) || "Corrupted".equals(l));
}

<div class="poe-tooltip ${rarityClass} w-80 max-w-[calc(100vw-2rem)] max-h-[80vh] overflow-y-auto shadow-2xl" style="border-color:${color}" data-build-item-tip>
	<div class="poe-tt-band ${twoLine ? "poe-tt-band-2" : ""}">
		<div class="text-center font-bold text-[15px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${color}">${name}</div>
		@if(twoLine)
			<div class="text-center text-[13px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${color}">${base}</div>
		@endif
	</div>
	<%-- 베이스 속성 칸 · 요구 사항(10-04 C147, PoE1 빌드 툴팁 짝 — 고유 · 베이스 툴팁과 같은 조각) --%>
	@if(item.base() != null)
		@template.poe2.htmx.baseProps(base = item.base(), quality = item.quality() == null ? 0 : item.quality())
		@template.poe2.htmx.requirements(level = item.base().reqLevel(), base = item.base())
	@endif
	@if(!implicits.isEmpty())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center space-y-0.5 bg-white/[0.03]">
			@for(String line : implicits)
				<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(line)}</div>
			@endfor
		</div>
	@endif
	@if(!explicits.isEmpty())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-2 text-center space-y-0.5">
			@for(String line : explicits)
				@if("타락".equals(line) || "Corrupted".equals(line))
					<%-- 타락은 인게임처럼 붉게(다른 툴팁과 같은 색) --%>
					<%-- 맨 아래 한 줄로 옮김(10-05 C158, 고유 툴팁 C157 과 같은 자리) --%>
				@else
					<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(line)}</div>
				@endif
			@endfor
		</div>
	@endif
	@if(flavourLines != null && !flavourLines.isEmpty())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-2 text-center space-y-0.5" data-tt-flavour>
			@for(String fl : flavourLines)
				<div class="text-[12px] leading-snug italic text-[#af6025]">${fl}</div>
			@endfor
		</div>
	@endif
	@if(corruptedItem)
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center text-[12px] font-bold text-[#d20000]" data-tt-corrupted>${PoeText.isKorean() ? "타락" : "Corrupted"}</div>
	@endif
	<div class="pt-1.5"></div>
	@template.poe2.htmx.keywordBox(keywords = item.keywords())
</div>
