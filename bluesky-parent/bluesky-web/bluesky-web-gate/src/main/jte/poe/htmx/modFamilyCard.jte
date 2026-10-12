@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe.dto.PoeModClass
@import net.luversof.web.gate.poe.dto.PoeModTier
@import net.luversof.web.gate.poe.PoeText

@param String key
@param PoeModClass.Family family
@param boolean ko = false
@param long poolWeight = 0
<%-- 제작 계산기 선택 체크박스(접두/접미 섹션에서만 true) --%>
@param boolean selectable = false

!{
	java.util.List<PoeModTier> tiers = family.tiers();
	// 대표 문장(최상위 티어의 스탯 줄, 수치 없이 라벨 느낌) — 패밀리 제목
	PoeModTier top = tiers.isEmpty() ? null : tiers.get(0);
	// 스폰웨이트 최대 — 티어별 상대 확률(%)의 분모. 같은 패밀리 안 상대값이라 대략의 등장 빈도 감을 준다.
	int maxW = 1;
	for (PoeModTier t : tiers) maxW = Math.max(maxW, t.weight());
	// 티어 라벨: T1 = 최상위(맨 위). 인게임 표기와 반대(게임은 낮은 숫자가 상위)라 위→아래로 T1..Tn.
	// 패밀리 전체 등장 확률(임의 티어) = 티어 가중치 합 / 젠 풀 합 — 제작 계산기 곱셈용
	long famWeightSum = 0;
	for (PoeModTier t : tiers) famWeightSum += t.weight();
	double famOdds = poolWeight > 0 ? famWeightSum * 100.0 / poolWeight : 0;
}

<details class="rounded-lg border border-base-200 bg-base-100 overflow-hidden" data-mod-family="${key}" data-family-odds="${selectable && poolWeight > 0 ? String.format("%.4f", famOdds) : null}" data-family-gen="${selectable ? family.gen() : null}">
	<summary class="cursor-pointer select-none px-3 py-2 hover:bg-base-200/50 list-none flex items-center gap-2">
		@if(selectable && poolWeight > 0)
			<%-- 계산기 선택 — 클릭이 details 토글로 번지지 않게 onclick 전파 차단 --%>
			<input type="checkbox" class="checkbox checkbox-xs poe-craft-pick shrink-0" onclick="event.stopPropagation()" title="${MessageUtil.getMessage("poe.mods.odds.include")}">
		@endif
		<%-- 티어 "개수"(펼치면 T1..Tn). 접힌 배지가 "티어5"로 오해되지 않게 title 로 명시. --%>
		<span class="text-xs text-base-content/60 font-mono shrink-0" title="${MessageUtil.getMessage("poe.mods.tiers.title")}" data-family-tiers data-tiers-format="${java.text.MessageFormat.format(MessageUtil.getMessage("poe.mods.tiers"), "{0}")}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.mods.tiers"), tiers.size())}</span>
		<%-- 최상위 티어 요구 아이템 레벨 — 접힌 상태에서도 "이 옵션 T1 을 뜨게 하려면 몇 렙 아이템이 필요한지" 바로 보이게 --%>
		@if(top != null)
			<span class="text-[10px] font-mono text-base-content/60 shrink-0" data-top-ilvl title="${MessageUtil.getMessage("poe.mods.toptier.ilvl")}">i${top.ilvl()}</span>
		@endif
		<span class="min-w-0 flex-1">
			@if(top != null)
				@for(String line : (ko ? top.ko() : top.en()))
					<span class="text-[13px] text-[#8888ff] block truncate">$unsafe{PoeText.highlightValues(line)}</span>
				@endfor
			@endif
		</span>
		<span class="text-[10px] text-base-content/60 shrink-0">${key}</span>
	</summary>
	<div class="border-t border-base-200 overflow-x-auto">
		<table class="table table-xs w-full">
			<thead>
				<tr class="text-base-content/60">
					<%-- 머리글은 PoE2 옵션 풀(poe2.mods.tier/level "티어 · 레벨")과 같은 말 — 한국어 화면에 "T · iLvl" 이 남아 있었다(10-02 C30) --%>
					<th class="w-10">${MessageUtil.getMessage("poe.mods.col.tier")}</th>
					<th class="w-12">${MessageUtil.getMessage("poe.mods.col.level")}</th>
					<th>${MessageUtil.getMessage("poe.mods.col.effect")}</th>
					<th class="w-16 text-right">${MessageUtil.getMessage("poe.mods.col.weight")}</th>
					@if(poolWeight > 0)
						<th class="w-16 text-right" title="${MessageUtil.getMessage("poe.mods.col.odds.title")}">${MessageUtil.getMessage("poe.mods.col.odds")}</th>
					@endif
				</tr>
			</thead>
			<tbody>
				!{int idx = 0;}
				@for(PoeModTier t : tiers)
					!{idx++;}
					<tr data-tier-ilvl="${t.ilvl()}">
						<td class="font-mono text-base-content/60">T${idx}</td>
						<td class="font-mono text-base-content/60">${t.ilvl()}</td>
						<td>
							<div class="text-[11px] text-base-content/60">${ko ? t.nameKo() : t.name()}</div>
							!{java.util.List<String> mx = ko ? t.ko() : t.en();}
							!{java.util.List<String> mn = ko ? t.koMin() : t.enMin();}
							@for(int i = 0; i < mx.size(); i++)
								<div class="text-[12px] whitespace-pre-line">
									$unsafe{PoeText.highlightValues(net.luversof.web.gate.poe.PoeModRange.range(mn != null && i < mn.size() ? mn.get(i) : null, mx.get(i)))}
								</div>
							@endfor
						</td>
						<td class="text-right font-mono text-[11px] text-base-content/60">${t.weight()}</td>
						@if(poolWeight > 0)
							!{double pct = t.weight() * 100.0 / poolWeight;}
							<td class="text-right font-mono text-[11px] text-base-content/60"
							    title="${MessageUtil.getMessage("poe.mods.odds.avg")}${pct > 0 ? String.valueOf(Math.round(100.0 / pct)) : "∞"}">
								${pct >= 1 ? String.format("%.1f%%", pct) : String.format("%.2f%%", pct)}
							</td>
						@endif
					</tr>
				@endfor
			</tbody>
		</table>
	</div>
</details>
