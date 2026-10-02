@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe2.Poe2View
@import net.luversof.web.gate.poe.PoeText

@param Poe2.BaseItem base

!{
	// 일반(노멀) 아이템 — 이름 흰색(인게임 노멀 희귀도)
	java.util.List<String> implicits = Poe2View.implicitLines(base);
}

<div class="poe-tooltip poe-rar-white w-80 max-h-[80vh] overflow-y-auto shadow-2xl">
	<div class="poe-tt-band">
		<div class="text-center font-bold text-[15px] leading-tight text-[#c8c8c8] drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]">${PoeText.name(base.nameKo(), base.name())}</div>
	</div>
	<div class="px-4 pt-1.5 text-center text-[11px]">
		<span class="text-white/70">${net.luversof.web.gate.poe.PoeClassNames.poe2(base.itemClassKo(), base.itemClass())}</span>
		@if(PoeText.isKorean())<div class="text-[10px] text-white/30">${base.name()}</div>@endif
	</div>
	@template.poe2.htmx.baseProps(base = base)
	@template.poe2.htmx.requirements(level = base.reqLevel(), base = base)
	@if(!implicits.isEmpty())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center space-y-0.5 bg-white/[0.03]">
			@for(String line : implicits)
				<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(line)}</div>
			@endfor
		</div>
	@endif
</div>
