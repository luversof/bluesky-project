@import net.luversof.web.gate.poe.PoeText
@import net.luversof.web.gate.poe.PoeModRange
@import net.luversof.web.gate.poe.dto.PoeCraft
@import net.luversof.web.gate.poe.dto.PoeModItemClass

@param PoeCraft.CalcExtras extras = null
@param java.util.List<PoeModItemClass.Influence> influences = java.util.List.of()
@param String activeInfluence = ""
@param long infFamilies = 0
@param String part = "all"

<%-- 계산기 목표 칸 아래(10-21 C548 사용자 요청 "제작 계산기에 타락 · 정복자 · 제작 옵션이 안 보인다") — ① 영향력(정복자 · 쉐이퍼 · 엘더) 옵션은 베이스
     영향력이 있어야 붙어, 고르면 위쪽 '영향력'을 바꿔 그 옵션을 목표 목록에 더한다(바로가기) ② 바알 오브 타락 임플리싯 — 타락하면 더 못 바꿔 계산기
     목표는 아니고, 바알 한 번에 붙을 확률과 태그를 보인다(제작대 옵션은 목표 줄의 '제작대 ≈ Tn'). part = top(폼 안 — 짧은 두 줄) · corrupted(작업 영역 아래 —
     화면 높이 고정 칸 안에 긴 표를 넣으면 목표 목록이 0 높이로 눌려 겹쳤다) --%>
!{
	boolean ko = PoeText.isKorean();
	java.util.function.Function<PoeModItemClass.Influence, String> nameOf = i -> ko && i.nameKo() != null ? i.nameKo() : i.name();
	String curName = influences.stream().filter(i -> i.key().equals(activeInfluence)).map(nameOf).findFirst().orElse(activeInfluence);
	int benchCount = extras == null || extras.bench() == null ? 0 : extras.bench().size();
}
@if(!"corrupted".equals(part) && !influences.isEmpty())
	<div class="flex flex-wrap items-center gap-1 text-xs shrink-0" data-craft-calc-influence="${activeInfluence}">
		<span class="text-base-content/70">${ko ? "정복자 · 쉐이퍼 · 엘더 옵션" : "Conqueror · Shaper · Elder modifiers"}</span>
		@if(activeInfluence == null || activeInfluence.isEmpty())
			<span class="text-base-content/70">— ${ko ? "영향력을 고르면 그 옵션이 위 목록에 더해집니다:" : "pick an influence to add its modifiers to the list above:"}</span>
			@for(PoeModItemClass.Influence inf : influences)
				<button type="button" class="btn btn-xs btn-ghost border border-base-300" data-craft-set-influence="${inf.key()}" title="${ko ? "위쪽 '영향력'을 " + nameOf.apply(inf) + "(으)로 — 만들던 아이템은 처음부터" : "Set the influence above to " + nameOf.apply(inf) + " — the current item starts over"}">${nameOf.apply(inf)}</button>
			@endfor
		@else
			<span class="text-base-content/70">— ${ko ? "지금 " + curName + " 베이스 · 위 목록의 '" + curName + "' 표시 " + infFamilies + "줄" : "now a " + curName + " base · " + infFamilies + " rows marked '" + curName + "' above"}</span>
			<button type="button" class="btn btn-xs btn-ghost border border-base-300" data-craft-set-influence="">${ko ? "영향력 없음으로" : "No influence"}</button>
		@endif
	</div>
@endif
@if(!"corrupted".equals(part) && benchCount > 0)
	<p class="text-xs text-base-content/70 shrink-0" data-craft-calc-bench-note>${ko ? "제작대 옵션 — 위 목록의 '제작대 ≈ Tn' " + benchCount + "줄: 그 계열 목표를 T1~Tn 이하로 고르면 제작대로도 채웁니다(마우스를 올리면 제작대 옵션 문구)" : "Bench crafts — " + benchCount + " rows marked 'Bench ≈ Tn' above: with that family's target at T1~Tn or looser, the bench can finish it (hover for the craft)"}</p>
@endif
@if(!"top".equals(part) && extras != null && extras.corrupted() != null && !extras.corrupted().isEmpty())
	<details class="rounded-box border border-base-300 bg-base-100" data-craft-calc-corrupted>
		<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "타락 옵션(바알 오브)" : "Corrupted implicits (Vaal Orb)"} <span class="text-xs font-normal text-base-content/70">· ${extras.corrupted().size()}${ko ? "종 · 바알 한 번에 그 임플리싯이 붙을 확률(결과 넷 중 하나가 '임플리싯'). 타락하면 더 못 바꿔 계산기 목표로는 고르지 않습니다 — 위 에뮬레이터의 바알 오브로 시험" : " · chance per Vaal Orb (one of four outcomes is an implicit). Corrupted items can't be changed, so these aren't calculator targets — try the Vaal Orb in the emulator above"}</span></summary>
		<div class="px-3 pb-3 overflow-x-auto">
			<table class="table table-xs">
				<thead><tr><th>${ko ? "옵션" : "Modifier"}</th><th class="text-right">${ko ? "레벨" : "Level"}</th><th class="text-right">${ko ? "바알 한 번" : "Per Vaal"}</th></tr></thead>
				<tbody>
					@for(PoeCraft.CorruptedLine c : extras.corrupted())
						!{
							java.util.List<String> mx = ko && c.ko() != null && !c.ko().isEmpty() ? c.ko() : (c.en() == null ? java.util.List.<String>of() : c.en());
							java.util.List<String> mn = ko && c.koMin() != null && !c.koMin().isEmpty() ? c.koMin() : (c.enMin() == null ? java.util.List.<String>of() : c.enMin());
							java.util.List<String> tg = ko ? (c.tagsKo() == null ? java.util.List.<String>of() : c.tagsKo()) : (c.tags() == null ? java.util.List.<String>of() : c.tags());
						}
						<tr data-craft-calc-corrupt="${c.id()}">
							<td class="min-w-0 wrap-anywhere">
								@for(int i = 0; i < mx.size(); i++)<div>$unsafe{PoeText.highlightValues(PoeModRange.range(i < mn.size() ? mn.get(i) : null, mx.get(i)))}</div>@endfor
								@if(!tg.isEmpty())<div class="text-[10px] text-base-content/60" data-craft-calc-tags>— ${String.join(", ", tg)}</div>@endif
							</td>
							<td class="text-right font-mono">${c.level()}</td>
							<td class="text-right font-mono">${String.format("%.2f%%", c.chance() * 100)}</td>
						</tr>
					@endfor
				</tbody>
			</table>
		</div>
	</details>
@endif
