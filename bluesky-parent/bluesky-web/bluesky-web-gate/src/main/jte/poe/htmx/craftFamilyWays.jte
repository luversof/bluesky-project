@import net.luversof.web.gate.poe.PoeText
@import net.luversof.web.gate.poe.dto.PoeCraft
@param PoeCraft.FamilyWays ways = null
<%-- 같은 옵션을 얻는 다른 길(10-22 P4, poedb 속성 부여의 '같은 문구 다른 출처') — 계열을 펼치면 불러온다(craftPoolGroups 의 intersect once).
     에센스(그 에센스가 보장하는 자연 등급 T) · 작업대(같은 옵션 · 등급 상당) · 화석(그 계열 태그 배수 — 늘림은 단추, 막음은 글) · 수확(그 태그 포함 재련).
     단추를 누르면 그 화폐를 고른 상태로(common.ts [data-craft-way] — 옵션 표가 그 기준으로 바뀐다) --%>
!{
	boolean ko = PoeText.isKorean();
	boolean any = ways != null && (!ways.essences().isEmpty() || ways.bench() != null || !ways.fossils().isEmpty() || !ways.harvests().isEmpty());
	java.util.List<String> blockedBy = ways == null ? java.util.List.of() : ways.fossils().stream().filter(f -> f.mult() < 1).map(f -> (ko && f.nameKo() != null ? f.nameKo() : f.name()) + " " + net.luversof.web.gate.poe.CraftPoolGroups.multText(f.mult())).toList();
}
<div class="mt-2 space-y-1 text-xs" data-craft-ways="${any ? "1" : "0"}">
	<div class="text-base-content/70">${ko ? "같은 옵션을 얻는 다른 길 — 누르면 그 화폐를 고른 상태로" : "Other ways to get this — click to arm that currency"}</div>
	@if(!any)
		<div class="text-base-content/60">${ko ? "에센스 · 작업대 · 화석 · 수확으로 따로 얻는 길이 없습니다." : "No essence, bench, fossil or harvest route for this modifier."}</div>
	@else
		<div class="flex flex-wrap gap-1">
			@for(var e : ways.essences())
				<button type="button" class="btn btn-xs btn-ghost border border-base-300 gap-1 h-auto min-h-6 py-0.5" data-craft-way="ESSENCE" data-craft-way-value="${e.slug()}" title="${ko ? "이 에센스는 이 계열 T" + e.rank() + " 이상을 보장합니다" : "This essence guarantees T" + e.rank() + " or better"}"><img src="/poe-assets/${e.icon()}" alt="" class="w-4 h-4 object-contain" loading="lazy">${ko && e.nameKo() != null ? e.nameKo() : e.name()} <span class="text-base-content/60">T${e.rank()}</span></button>
			@endfor
			@if(ways.bench() != null)
				<button type="button" class="btn btn-xs btn-ghost border border-base-300 h-auto min-h-6 py-0.5" data-craft-way="BENCH" data-craft-way-value="${ways.bench().modId()}" title="${ko ? "작업대 옵션 — 이 계열 T" + ways.bench().tier() + " 상당" : "Bench craft — equal to T" + ways.bench().tier()}">${ko ? "작업대: " : "Bench: "}${String.join(", ", ko && ways.bench().ko() != null && !ways.bench().ko().isEmpty() ? ways.bench().ko() : ways.bench().en())} <span class="text-base-content/60">T${ways.bench().tier()}</span></button>
			@endif
			@for(var f : ways.fossils())
				@if(f.mult() > 1)
					<button type="button" class="btn btn-xs btn-ghost border border-base-300 gap-1 h-auto min-h-6 py-0.5" data-craft-way="FOSSIL" data-craft-way-value="${f.slug()}" title="${ko ? "이 화석은 이 계열 가중치를 " + net.luversof.web.gate.poe.CraftPoolGroups.multText(f.mult()) + " 합니다" : "This fossil multiplies this family's weight " + net.luversof.web.gate.poe.CraftPoolGroups.multText(f.mult())}"><img src="/poe-assets/${f.icon()}" alt="" class="w-4 h-4 object-contain" loading="lazy">${ko && f.nameKo() != null ? f.nameKo() : f.name()} <span class="badge badge-info badge-xs">${net.luversof.web.gate.poe.CraftPoolGroups.multText(f.mult())}</span></button>
				@endif
			@endfor
			@for(var h : ways.harvests())
				<button type="button" class="btn btn-xs btn-ghost border border-base-300 h-auto min-h-6 py-0.5 text-left" data-craft-way="HARVEST" data-craft-way-value="${h.id()}">${ko ? "수확: " : "Harvest: "}${ko && h.textKo() != null ? h.textKo() : h.text()}</button>
			@endfor
		</div>
		@if(!blockedBy.isEmpty())
			<div class="text-base-content/60" data-craft-ways-blocked>${ko ? "이 화석을 쓰면 안 붙음: " : "Can't roll with: "}${String.join(", ", blockedBy)}</div>
		@endif
	@endif
	<div class="text-warning" hidden aria-live="polite" data-craft-way-msg></div>
</div>
