@import net.luversof.web.gate.poe.dto.PoeModClass
@import net.luversof.web.gate.poe.dto.PoeModTier
@import net.luversof.web.gate.poe.PoeText

@param String key
@param PoeModClass.Family family
@param boolean ko = false

!{
	java.util.List<PoeModTier> tiers = family.tiers();
	// 대표 문장(최상위 티어의 스탯 줄, 수치 없이 라벨 느낌) — 패밀리 제목
	PoeModTier top = tiers.isEmpty() ? null : tiers.get(0);
	// 스폰웨이트 최대 — 티어별 상대 확률(%)의 분모. 같은 패밀리 안 상대값이라 대략의 등장 빈도 감을 준다.
	int maxW = 1;
	for (PoeModTier t : tiers) maxW = Math.max(maxW, t.weight());
	// 티어 라벨: T1 = 최상위(맨 위). 인게임 표기와 반대(게임은 낮은 숫자가 상위)라 위→아래로 T1..Tn.
}

<details class="rounded-lg border border-base-200 bg-base-100 overflow-hidden" data-mod-family="${key}">
	<summary class="cursor-pointer select-none px-3 py-2 hover:bg-base-200/50 list-none flex items-center gap-2">
		<span class="text-xs text-base-content/30 font-mono shrink-0">${tiers.size()}T</span>
		<span class="min-w-0 flex-1">
			@if(top != null)
				@for(String line : (ko ? top.ko() : top.en()))
					<span class="text-[13px] text-[#8888ff] block truncate">$unsafe{PoeText.highlightValues(line)}</span>
				@endfor
			@endif
		</span>
		<span class="text-[10px] text-base-content/25 shrink-0">${key}</span>
	</summary>
	<div class="border-t border-base-200 overflow-x-auto">
		<table class="table table-xs w-full">
			<thead>
				<tr class="text-base-content/40">
					<th class="w-10">T</th>
					<th class="w-12">iLvl</th>
					<th>${ko ? "효과 (범위)" : "Effect (range)"}</th>
					<th class="w-16 text-right">${ko ? "가중치" : "Weight"}</th>
				</tr>
			</thead>
			<tbody>
				!{int idx = 0;}
				@for(PoeModTier t : tiers)
					!{idx++;}
					<tr data-tier-ilvl="${t.ilvl()}">
						<td class="font-mono text-base-content/50">T${idx}</td>
						<td class="font-mono text-base-content/50">${t.ilvl()}</td>
						<td>
							<div class="text-[11px] text-base-content/40">${ko ? t.nameKo() : t.name()}</div>
							!{java.util.List<String> mx = ko ? t.ko() : t.en();}
							!{java.util.List<String> mn = ko ? t.koMin() : t.enMin();}
							@for(int i = 0; i < mx.size(); i++)
								<div class="text-[12px] whitespace-pre-line">
									$unsafe{PoeText.highlightValues(net.luversof.web.gate.poe.PoeModRange.range(mn != null && i < mn.size() ? mn.get(i) : null, mx.get(i)))}
								</div>
							@endfor
						</td>
						<td class="text-right font-mono text-[11px] text-base-content/40">${t.weight()}</td>
					</tr>
				@endfor
			</tbody>
		</table>
	</div>
</details>
