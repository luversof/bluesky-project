@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText

@param Poe2.Unique item
@param Poe2.BaseItem base = null
<%-- 보일 변형(1-base). null 이면 기본. 인게임엔 변형 라벨이 없어 툴팁 안엔 넣지 않는다(선택은 바깥 칩). --%>
@param Integer variant = null

!{
	Poe2.UniqueVariant picked = null;
	if (variant != null && item.variants() != null) {
		for (Poe2.UniqueVariant v : item.variants()) {
			if (v.index() != null && v.index().intValue() == variant.intValue()) picked = v;
		}
	}
	java.util.List<String> implicits = picked != null ? PoeText.lines(picked.implicitsKo(), picked.implicits()) : PoeText.lines(item.implicitsKo(), item.implicits());
	java.util.List<String> explicits = picked != null ? PoeText.lines(picked.explicitsKo(), picked.explicits()) : PoeText.lines(item.explicitsKo(), item.explicits());
	// PoE 고유(주황) 색 — PoE1 과 같은 인게임 색
	String rarity = "#af6025";
	// 툴팁 종류 줄 = 단수 분류(10-23 Q39 — Bows · 쇠뇌 → Bow · 석궁)
	String itemClass = net.luversof.web.gate.poe.PoeClassNames.poe2Tip(item.itemClassKo(), item.itemClass());
	// 무기는 종류 줄을 속성 칸 첫 줄로(10-23 Q62) — 머리 아래 칸엔 안 쓴다
	boolean weaponBase = base != null && base.weapon() != null && base.weapon().damageMax() != null && base.weapon().damageMax() > 0;
	java.util.List<String> flavourLines = PoeText.lines(item.flavourKo(), item.flavour());
	// 데이터의 "Corrupted"/"타락" 옵션 줄(고유 13) — 인게임은 옵션이 아니라 맨 아래 붉은 한 줄(10-05 C157, PoE1 uniqueDetail 짝)
	java.util.Set<String> corruptedWords = java.util.Set.of("Corrupted", "타락");
	boolean corruptedItem = explicits != null && explicits.stream().anyMatch(corruptedWords::contains);
}

<div class="poe-tooltip poe-rar-unique w-80 max-w-[calc(100vw-2rem)] max-h-[80vh] overflow-y-auto shadow-2xl" style="border-color:${rarity}">
	<div class="poe-tt-band poe-tt-band-2">
		<div class="text-center font-bold text-[15px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${rarity}">${PoeText.name(item.nameKo(), item.name())}</div>
		<div class="text-center text-[13px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${rarity}">${PoeText.name(item.baseTypeKo(), item.baseType())}</div>
	</div>
	@if((itemClass != null && !weaponBase) || PoeText.isKorean())
		<div class="px-4 pt-1.5 text-center text-[11px]">
			@if(itemClass != null && !weaponBase)<span class="text-white/70">${itemClass}</span>@endif
			@if(PoeText.isKorean())<div class="text-[10px] text-white/30">${item.name()} · ${item.baseType()}</div>@endif
		</div>
	@endif
	@if(base != null)
		@template.poe2.htmx.baseProps(base = base, lines = net.luversof.web.gate.poe.PoeLocalProps.join(picked != null ? picked.implicits() : item.implicits(), picked != null ? picked.explicits() : item.explicits()), classLine = weaponBase ? itemClass : null, leadSep = !weaponBase)
	@endif
	<%-- 적용 반경(주얼, 10-08 C191 — 게임 ClientStrings JewelRadiusLabel "적용 반경" · JewelRadiusSmall "작게" 등, PoE1 고유 상세의 반경 줄 짝) --%>
	<%-- 중복 사용 제한(10-08 C197, PoE1 짝 — 게임 ClientStrings JewelItemLimit) --%>
	@if((item.limit() != null && !item.limit().isBlank()) || (item.radius() != null && !item.radius().isBlank()))
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center text-[12px] space-y-0.5" data-tt-jewel-props>
	@endif
	@if(item.limit() != null && !item.limit().isBlank())
		<div data-tt-limit><span class="text-[#7f7f7f]">${io.github.luversof.boot.context.support.MessageUtil.getMessage("poe.items.detail.limit")}: </span><span class="text-[#e0e0e0]">${io.github.luversof.boot.context.support.MessageUtil.getMessage("poe.limit." + item.limit().replace(" ", ""), item.limit())}</span></div>
	@endif
	@if(item.radius() != null && !item.radius().isBlank())
		<div data-tt-radius><span class="text-[#7f7f7f]">${io.github.luversof.boot.context.support.MessageUtil.getMessage("poe2.radius.label")}: </span><span class="text-[#e0e0e0]">${io.github.luversof.boot.context.support.MessageUtil.getMessage("poe2.radius." + item.radius().replace(" ", ""), item.radius())}</span></div>
	@endif
	@if((item.limit() != null && !item.limit().isBlank()) || (item.radius() != null && !item.radius().isBlank()))
		</div>
	@endif
	@template.poe2.htmx.requirements(level = item.requiredLevel() != null ? item.requiredLevel() : (base == null ? null : base.reqLevel()), base = base, implicits = picked != null ? picked.implicits() : item.implicits(), explicits = picked != null ? picked.explicits() : item.explicits())
	@if(implicits != null && !implicits.isEmpty())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center space-y-0.5 bg-white/[0.03]">
			@for(String line : implicits)
				<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(line)}</div>
			@endfor
		</div>
	@endif
	@if(explicits != null && !explicits.isEmpty())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-2 text-center space-y-0.5">
			@for(String line : explicits)
				@if(!corruptedWords.contains(line))
				<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(line)}</div>
				@endif
			@endfor
		</div>
	@endif

	<%-- 로어(플레이버) — 인게임 고유 툴팁 맨 아래 주황 기울임(10-01, 위키 + 게임 FlavourText 원문) --%>
	<%-- 타락 줄 = 옵션 바로 아래 · 로어 위(10-09 C216 — poedb 목소리 · poe2db 신념의 프리즘 툴팁 순서: 옵션 → Corrupted → 로어 → 사용 설명. 예전(C157)엔 맨 아래) --%>
	@if(corruptedItem)
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center text-[12px] font-bold text-[#d20000]" data-tt-corrupted>${MessageUtil.getMessage("poe.common.corrupted")}</div>
	@endif
	@if(flavourLines != null && !flavourLines.isEmpty())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-2 text-center space-y-0.5" data-tt-flavour>
			@for(String line : flavourLines)
				<div class="text-[12px] leading-snug italic text-[#af6025]">${line}</div>
			@endfor
		</div>
	@endif
	<%-- 사용 설명(10-09 C213 — PoE2 게임 ClientStrings ItemDescriptionFlask · FlaskUtility1(부적) · PassiveJewel, 마크업 벗김. 맨 아래 회색 문단) --%>
	!{ String helpKey = net.luversof.web.gate.poe.PoeItemDescription.poe2Key(item.itemClass()); }
	@if(helpKey != null)
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-2 text-center text-[12px] leading-snug text-[#7f7f7f]" data-tt-help>${io.github.luversof.boot.context.support.MessageUtil.getMessage(helpKey)}</div>
	@endif
	<%-- 키워드 설명(10-04 C112, 젬 C111 짝) — 변형을 골랐으면 그 변형 줄의 용어(기본과 다른 변형만 자기 목록을 갖고, 없으면 고유 것과 같다 — C117) --%>
	@template.poe2.htmx.keywordBox(keywords = picked != null && picked.keywords() != null ? picked.keywords() : item.keywords())
</div>
