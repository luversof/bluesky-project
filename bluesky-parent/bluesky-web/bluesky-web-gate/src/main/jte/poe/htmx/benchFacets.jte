@import net.luversof.web.gate.poe.dto.ArchetypeBenchmark
@import net.luversof.web.gate.poe.PoeText

<%-- poe.ninja 검색 사이드바 집계(패싯) 패널 — 필터 매칭 전체 모집단 기준 사용률(%).
     마스터리/룬크래프트/문신/무기 구성/판테온/아틀라스 패시브 등(사용자 요청 차원 전부). --%>
@param ArchetypeBenchmark bench = null
@param int rows = 5

@if(bench != null && bench.facets() != null && !bench.facets().isEmpty() && bench.facetTotal() > 0)
	!{
		boolean ko = PoeText.isKorean();
		// {패싯키, ko라벨, en라벨} — 사용자 요청 차원 우선 순서
		String[][] order = {
			{"masteries", "마스터리", "Masteries"},
			{"runegrafts", "룬크래프트", "Runegrafts"},
			{"tattoos", "문신", "Tattoos"},
			{"weaponmode", "무기 구성", "Weapons"},
			{"pantheon", "판테온", "Pantheon"},
			{"atlasskills", "아틀라스 패시브", "Atlas passives"},
			{"anointed", "도유", "Anoints"},
			{"secondascendancy", "혈맹", "Bloodline"},
			{"bandit", "산적", "Bandit"},
			{"keypassives", "키스톤", "Keystones"},
			{"items", "장비", "Items"},
			{"shrinebeltbuffs", "성소 벨트", "Shrine belt"},
		};
	}
	<div class="space-y-1">
		<div class="flex items-baseline gap-2">
			<span class="text-xs font-bold text-info">${ko ? "실빌드 구성 분포" : "Real-build loadout"}</span>
			<span class="text-[10px] text-base-content/40">${ko ? "poe.ninja 전체 모집단" : "poe.ninja full population"} n=${String.format("%,d", bench.facetTotal())}</span>
		</div>
		<div class="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
			@for(String[] o : order)
				!{
					java.util.List<ArchetypeBenchmark.FacetEntry> list = bench.facets().get(o[0]);
					// "None"(혈맹 없음 등)은 신호가 아니라 소음 — 제외
					java.util.List<ArchetypeBenchmark.FacetEntry> shown = list == null ? null
						: list.stream().filter(e -> !"None".equals(e.name())).limit(rows).toList();
				}
				@if(shown != null && !shown.isEmpty())
					<div class="rounded border border-base-200 bg-base-100 px-2 py-1.5 min-w-0">
						<div class="mb-1 text-[10px] font-bold uppercase tracking-wide text-base-content/40">${ko ? o[1] : o[2]}</div>
						@for(ArchetypeBenchmark.FacetEntry e : shown)
							<div class="flex items-baseline gap-2 text-[11px] leading-5">
								<span class="truncate text-base-content/70" title="${e.name()}">${e.name()}</span>
								<span class="ml-auto shrink-0 font-mono text-base-content/50">${Math.round(e.count() * 100.0 / bench.facetTotal())}%</span>
							</div>
						@endfor
					</div>
				@endif
			@endfor
		</div>
	</div>
@endif
