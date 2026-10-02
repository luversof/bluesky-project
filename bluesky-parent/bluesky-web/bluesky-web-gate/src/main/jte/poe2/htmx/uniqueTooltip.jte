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
	String itemClass = net.luversof.web.gate.poe.PoeClassNames.poe2(item.itemClassKo(), item.itemClass());
	java.util.List<String> flavourLines = PoeText.lines(item.flavourKo(), item.flavour());
}

<div class="poe-tooltip poe-rar-unique w-80 max-h-[80vh] overflow-y-auto shadow-2xl" style="border-color:${rarity}">
	<div class="poe-tt-band poe-tt-band-2">
		<div class="text-center font-bold text-[15px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${rarity}">${PoeText.name(item.nameKo(), item.name())}</div>
		<div class="text-center text-[13px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${rarity}">${PoeText.name(item.baseTypeKo(), item.baseType())}</div>
	</div>
	@if(itemClass != null || PoeText.isKorean())
		<div class="px-4 pt-1.5 text-center text-[11px]">
			@if(itemClass != null)<span class="text-white/70">${itemClass}</span>@endif
			@if(PoeText.isKorean())<div class="text-[10px] text-white/30">${item.name()} · ${item.baseType()}</div>@endif
		</div>
	@endif
	@if(base != null)
		@template.poe2.htmx.baseProps(base = base)
	@endif
	@template.poe2.htmx.requirements(level = item.requiredLevel() != null ? item.requiredLevel() : (base == null ? null : base.reqLevel()), base = base)
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
				<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(line)}</div>
			@endfor
		</div>
	@endif

	<%-- 로어(플레이버) — 인게임 고유 툴팁 맨 아래 주황 기울임(10-01, 위키 + 게임 FlavourText 원문) --%>
	@if(flavourLines != null && !flavourLines.isEmpty())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-2 text-center space-y-0.5" data-tt-flavour>
			@for(String line : flavourLines)
				<div class="text-[12px] leading-snug italic text-[#af6025]">${line}</div>
			@endfor
		</div>
	@endif
</div>
