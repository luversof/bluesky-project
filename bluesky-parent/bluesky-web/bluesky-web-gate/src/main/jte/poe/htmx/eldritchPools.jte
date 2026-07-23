@import net.luversof.web.gate.poe.dto.PoeEldritch
@import net.luversof.web.gate.poe.PoeText

@param PoeEldritch eldritch
@param boolean ko = false

<%-- 총주교/포식자 각 팩션의 계열 목록 — 접이식 카드, 티어 사다리(강→약) --%>
<div class="grid gap-4 md:grid-cols-2">
	@for(var pair : java.util.List.of(
			java.util.Map.entry(eldritch.exarchFaction(), eldritch.exarch()),
			java.util.Map.entry(eldritch.eaterFaction(), eldritch.eater())))
		!{
			PoeEldritch.Faction faction = pair.getKey();
			java.util.List<PoeEldritch.Family> fams = pair.getValue();
		}
		<div>
			<h3 class="mb-2 flex items-baseline gap-2 font-bold">
				<span class="text-amber-500">${ko ? faction.nameKo() : faction.name()}</span>
				<span class="text-xs text-base-content/40">${fams.size()}</span>
			</h3>
			<div class="space-y-2">
				@for(PoeEldritch.Family fam : fams)
					!{
						PoeEldritch.Tier top = fam.tiers().isEmpty() ? null : fam.tiers().get(0);
					}
					<details class="rounded-lg border border-base-200 bg-base-100 overflow-hidden" data-eldritch-family="${fam.key()}">
						<summary class="cursor-pointer select-none px-3 py-2 hover:bg-base-200/50 list-none flex items-center gap-2">
							<span class="text-xs text-base-content/30 font-mono shrink-0">${fam.tiers().size()}T</span>
							<span class="min-w-0 flex-1">
								@if(top != null)
									@for(String line : (ko ? top.ko() : top.en()))
										<span class="text-[13px] text-amber-300 block truncate">$unsafe{PoeText.highlightValues(line)}</span>
									@endfor
								@endif
							</span>
						</summary>
						<div class="border-t border-base-200 px-3 py-1.5 space-y-0.5">
							@for(PoeEldritch.Tier t : fam.tiers())
								<div class="flex items-baseline gap-2 text-[12px]">
									<span class="shrink-0 font-mono text-base-content/40 w-6">T${t.tier()}</span>
									<span class="whitespace-pre-line">$unsafe{PoeText.highlightValues(String.join(", ", ko ? t.ko() : t.en()))}</span>
								</div>
							@endfor
						</div>
					</details>
				@endfor
			</div>
		</div>
	@endfor
</div>
