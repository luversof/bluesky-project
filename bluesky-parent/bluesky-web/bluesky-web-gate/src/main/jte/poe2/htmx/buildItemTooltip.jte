@import io.github.luversof.boot.context.support.MessageUtil
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
	// 줄 종류(10-23 Q117 — API modKinds, 옛 응답이면 모두 빈 값): 룬 · 인챈트는 암시 칸 위 따로(인챈트 색), 분열 · 제작은 제 색
	java.util.List<String> kinds = item.modKinds() != null && item.modKinds().size() == lines.size() ? item.modKinds() : java.util.Collections.nCopies(lines.size(), "");
	java.util.List<Integer> enchantIdx = new java.util.ArrayList<>();
	java.util.List<Integer> implicitIdx = new java.util.ArrayList<>();
	for (int k = 0; k < split; k++) {
		if ("rune".equals(kinds.get(k)) || "enchant".equals(kinds.get(k))) enchantIdx.add(k); else implicitIdx.add(k);
	}
	java.util.List<String> implicits = implicitIdx.stream().map(lines::get).toList();
	java.util.List<String> explicits = lines.subList(split, lines.size());
	java.util.List<String> explicitKinds = kinds.subList(split, kinds.size());
	// 무기는 종류 줄을 속성 칸 첫 줄로, 그 밖엔 머리 아래(itemTooltip · uniqueTooltip 과 같게, Q117)
	boolean weaponBase = item.base() != null && item.base().weapon() != null && item.base().weapon().damageMax() != null && item.base().weapon().damageMax() > 0;
	String itemClass = item.base() == null ? null : net.luversof.web.gate.poe.PoeClassNames.poe2Tip(item.base().itemClassKo(), item.base().itemClass());
	// 두 번 타락(PoB "Twice Corrupted" — 게임 ItemPopupDoubleCorrupted '두 번 타락')은 타락 줄 자리에
	boolean twiceCorrupted = item.mods().stream().anyMatch("Twice Corrupted"::equals);
	// 축성(PoB "Sanctified" — 게임 ItemPopupSanctified '축성')도 옵션이 아니라 상태 줄(10-24 Q157, 제작 카드 C390 과 같게)
	boolean sanctifiedItem = item.mods().stream().anyMatch("Sanctified"::equals);
	String base = PoeText.name(item.baseTypeKo(), item.baseType());
	String name = PoeText.name(item.nameKo(), item.name());
	boolean twoLine = (rare || unique) && base != null && !base.equals(name);
	// 타락은 옵션이 아니라 맨 아래 붉은 한 줄(10-05 C158) — 예전엔 옵션 칸 안에서 붉게
	boolean corruptedItem = twiceCorrupted || explicits.stream().anyMatch(l -> "타락".equals(l) || "Corrupted".equals(l));
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
		@if(!weaponBase && itemClass != null)<div class="px-4 pt-1.5 text-center text-[11px]"><span class="text-white/70" data-tt-item-class>${itemClass}</span></div>@endif
		@template.poe2.htmx.baseProps(base = item.base(), quality = item.quality() == null ? 0 : item.quality(), lines = item.mods(), defences = item.defences(), classLine = weaponBase ? itemClass : null, leadSep = !weaponBase)
		<%-- 요구 능력치에 이 아이템 옵션 반영(10-08 C178, mods = 영문 원문 — 암시 포함) --%>
		@template.poe2.htmx.requirements(level = item.levelReq() != null ? item.levelReq() : item.base().reqLevel(), base = item.base(), explicits = item.mods())
	@endif
	@if(item.itemLevel() != null)
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center text-[12px] text-white/60" data-tt-ilvl>${PoeText.isKorean() ? "아이템 레벨 " : "Item Level "}<span class="text-white">${item.itemLevel()}</span></div>
	@endif
	@if(!enchantIdx.isEmpty())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center space-y-0.5" data-tt-enchants>
			@for(int ei : enchantIdx)
				<div class="text-[12px] leading-snug text-[#b8daf2] whitespace-pre-line" data-tt-kind="${kinds.get(ei)}">$unsafe{PoeText.highlightValues(lines.get(ei))}</div>
			@endfor
		</div>
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
			@for(int xi = 0; xi < explicits.size(); xi++)
				!{ String line = explicits.get(xi); }
				@if("타락".equals(line) || "Corrupted".equals(line) || "Twice Corrupted".equals(item.mods().get(split + xi)) || "Sanctified".equals(item.mods().get(split + xi)))
					<%-- 타락은 인게임처럼 붉게(다른 툴팁과 같은 색) --%>
					<%-- 맨 아래 한 줄로 옮김(10-05 C158, 고유 툴팁 C157 과 같은 자리) --%>
				@else
					<div class="text-[12px] leading-snug ${"rune".equals(explicitKinds.get(xi)) ? "text-[#b8daf2]" : PoeText.modKindClass(explicitKinds, explicits.size(), xi)} whitespace-pre-line" data-tt-kind="${explicitKinds.get(xi)}">$unsafe{PoeText.highlightValues(line)}</div>
				@endif
			@endfor
		</div>
	@endif
	<%-- 타락 줄 = 옵션 바로 아래 · 로어 위(10-09 C216 — poedb 목소리 · poe2db 신념의 프리즘 툴팁 순서: 옵션 → Corrupted → 로어 → 사용 설명. 예전(C157)엔 맨 아래) --%>
	@if(sanctifiedItem)
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center text-[12px] font-bold text-[#e8d08a]" data-tt-sanctified>${PoeText.isKorean() ? "축성" : "Sanctified"}</div>
	@endif
	@if(corruptedItem)
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center text-[12px] font-bold text-[#d20000]" data-tt-corrupted>${twiceCorrupted ? (PoeText.isKorean() ? "두 번 타락" : "Twice Corrupted") : MessageUtil.getMessage("poe.common.corrupted")}</div>
	@endif
	@if(flavourLines != null && !flavourLines.isEmpty())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-2 text-center space-y-0.5" data-tt-flavour>
			@for(String fl : flavourLines)
				<div class="text-[12px] leading-snug italic text-[#af6025]">${fl}</div>
			@endfor
		</div>
	@endif
	<%-- 사용 설명(10-09 C213 — PoE2 게임 ClientStrings ItemDescriptionFlask · FlaskUtility1(부적) · PassiveJewel, 마크업 벗김. 맨 아래 회색 문단) --%>
	!{ String helpKey = net.luversof.web.gate.poe.PoeItemDescription.poe2Key(item.base() == null ? null : item.base().itemClass()); }
	@if(helpKey != null)
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-2 text-center text-[12px] leading-snug text-[#7f7f7f]" data-tt-help>${io.github.luversof.boot.context.support.MessageUtil.getMessage(helpKey)}</div>
	@endif
	<div class="pt-1.5"></div>
	@template.poe2.htmx.keywordBox(keywords = item.keywords())
</div>
