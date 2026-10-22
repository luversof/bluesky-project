@import net.luversof.web.gate.poe.PoeText
@import net.luversof.web.gate.poe2.dto.Poe2Craft

@param java.util.List<net.luversof.web.gate.poe.CraftPoolGroups.Family<Poe2Craft.PoolLine>> poolGroups
@param java.util.List<Poe2Craft.Line> lines
@param java.util.List<String> added = java.util.List.of()
@param int max
@param long prefixes
@param long suffixes
@param java.util.Map<String, java.util.List<String>> lineTags = java.util.Map.of()
@param java.util.List<Poe2Craft.EssenceOption> essences = java.util.List.of()

!{
	boolean ko = PoeText.isKorean();
	java.util.function.Function<java.util.List<String>, java.util.List<String>> nz = l -> l == null ? java.util.List.of() : l;
	java.util.Set<String> famIds = new java.util.HashSet<>();
	for (var f : poolGroups) for (var t : f.tiers()) famIds.add(t.line().id());
}
<%-- 계열 묶음(10-21 C543, PoE1 C542 짝 — 사용자 요청 "옵션별로 묶어 줄을 줄이고, 누르면 등급 상세, 제작으로 붙은 옵션은 활성화 표시") — 빈 레어 기준
     전체 계열을 고정 순서로 한 줄씩(최고 등급 문구 · 지금 붙을 확률 합 · 최고 등급 확률 · ☆ = 그 계열 최고 등급 목표), 누르면 등급 표(예전 줄 그대로).
     PoE2 는 균등 확률이라 확률은 참고용, 고른 화폐 등급 · 징조에 걸린 줄은 0%(직접 편집 넣기는 그대로) --%>
<%-- 거르기(10-22 P1) — 태그 칩은 이 표 계열 태그를 많은 순으로 --%>
!{
	java.util.Map<String, Integer> famTagCount = new java.util.HashMap<>();
	for (var f : poolGroups) for (String t : ko ? nz.apply(f.top().tagsKo()) : nz.apply(f.top().tags())) famTagCount.merge(t, 1, Integer::sum);
	java.util.List<String> famTags = famTagCount.keySet().stream().sorted(java.util.Comparator.comparing((String t) -> -famTagCount.get(t)).thenComparing(t -> t)).toList();
}
@if(!poolGroups.isEmpty())@template.poe.htmx.craftFamFilter(tags = famTags)@endif
@for(String gen : java.util.List.of("prefix", "suffix"))
	!{
		java.util.List<net.luversof.web.gate.poe.CraftPoolGroups.Family<Poe2Craft.PoolLine>> gf = poolGroups.stream().filter(f -> gen.equals(f.gen())).toList();
		double gsum = gf.stream().mapToDouble(net.luversof.web.gate.poe.CraftPoolGroups.Family::sum).sum();
		long gon = "prefix".equals(gen) ? prefixes : suffixes;
		java.util.List<Poe2Craft.Line> outside = lines.stream().filter(l -> gen.equals(l.gen()) && !famIds.contains(l.id())).toList();
	}
	@if(!gf.isEmpty() || !outside.isEmpty())
		<details class="rounded-box border border-base-300 bg-base-100" open data-craft-pool-gen="${gen}">
			<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">
				${"prefix".equals(gen) ? (ko ? "접두" : "Prefixes") : (ko ? "접미" : "Suffixes")} · ${ko ? "붙음 " : ""}${gon}/${max}${ko ? "" : " on"} · ${ko ? "계열 " : ""}${gf.size()}${ko ? "개" : " families"} · ${String.format("%.1f%%", gsum * 100)}
			</summary>
			<div class="px-2 pb-2 space-y-1" data-craft-fam-list>
				<p class="text-xs text-base-content/60" hidden data-craft-fam-none>${ko ? "거르기에 맞는 계열이 없습니다." : "No family matches the filter."}</p>
				@for(Poe2Craft.Line l : outside)
					!{ java.util.List<String> ox = ko && !nz.apply(l.ko()).isEmpty() ? l.ko() : nz.apply(l.en()); }
					<div class="rounded border-l-4 border-primary bg-primary/10 px-2 py-1.5 text-xs min-w-0 wrap-anywhere" data-craft-fam-outside="${l.id()}">
						@for(String t : ox)<div>$unsafe{PoeText.highlightValues(t)}</div>@endfor
						<div class="text-[10px] text-base-content/70"><span class="badge badge-primary badge-xs">${ko ? "적용" : "on"}@if(l.tierRank() != null) T${l.tierRank()}@endif</span> ${ko ? "이 표 밖 옵션" : "outside this table"}@if(lineTags.get(l.id()) != null) — ${String.join(", ", lineTags.get(l.id()))}@endif@if(l.fractured()) · ${ko ? "분열" : "fractured"}@endif@if(added != null && added.contains(l.id())) <span class="badge badge-success badge-xs">${ko ? "방금" : "new"}</span>@endif</div>
					</div>
				@endfor
				@for(var f : gf)
					!{
						Poe2Craft.PoolLine top = f.top();
						java.util.List<String> fx = ko && !nz.apply(top.ko()).isEmpty() ? top.ko() : nz.apply(top.en());
						Integer topRank = top.tierRank();
						// 계열 태그(C544 사용자 요청 "옵션에 태그가 보여야") — 최고 등급 줄의 태그(계열 안 등급은 같은 태그)
						java.util.List<String> ftags = ko ? nz.apply(top.tagsKo()) : nz.apply(top.tags());
						Integer lastRank = f.tiers().get(f.tiers().size() - 1).line().tierRank();
						var on = f.applied();
						boolean live = f.rollable() && f.sum() > 0;
					}
					<details class="rounded border ${on != null ? "border-l-4 border-primary bg-primary/10" : "border-base-300"}" data-craft-group="fam:${f.key()}" data-swap-anchor-key="fam:${f.key()}" data-craft-fam="${f.key()}" data-craft-fam-tagset="${String.join("|", ftags)}" data-craft-fam-rollable="${f.rollable() ? "1" : null}" data-craft-fam-on="${on == null ? null : on.line().id()}">
						<summary class="cursor-pointer select-none grid grid-cols-[minmax(0,1fr)_auto_auto] items-center gap-2 px-2 py-1.5" title="${ko ? "누르면 등급별 상세" : "Click for every tier"}">
							<div class="min-w-0 text-xs wrap-anywhere ${!live && on == null ? "text-base-content/60" : null}">
								@for(String t : fx)<div>$unsafe{PoeText.highlightValues(t)}</div>@endfor
								<div class="text-[10px] leading-tight text-base-content/60">@if(topRank != null)T${topRank}@if(lastRank != null && !lastRank.equals(topRank))~T${lastRank}@endif · @endif${f.tiers().size()}${ko ? "단계" : (f.tiers().size() == 1 ? " tier" : " tiers")}@if(!ftags.isEmpty())<span data-craft-fam-tags> — ${String.join(", ", ftags)}</span>@endif
									@if(on != null) <span class="badge badge-primary badge-xs" data-craft-fam-applied="${on.line().tierRank()}">${ko ? "적용" : "on"}@if(on.line().tierRank() != null) T${on.line().tierRank()}@endif</span>@endif
									@if(on != null && on.fresh()) <span class="badge badge-success badge-xs" data-craft-fam-fresh>${ko ? "방금" : "new"}</span>@endif
								</div>
							</div>
							<div class="text-right font-mono text-xs leading-tight whitespace-nowrap" data-craft-fam-chance="${String.format("%.6f", f.sum())}">
								@if(live)
									${String.format("%.2f%%", f.sum() * 100)}
									@if(topRank != null)<div class="text-[10px] text-base-content/60" data-craft-fam-top>T${topRank} ${String.format("%.2f%%", f.topChance() * 100)}</div>@endif
								@else
									<span class="text-base-content/60" title="${on != null ? (ko ? "이미 붙은 계열은 다시 붙지 않습니다" : "Already on the item — can't roll again") : f.rollable() ? (ko ? "고른 화폐 등급 · 징조에 걸려 지금은 안 붙습니다(펼쳐서 직접 편집 넣기는 됨)" : "Blocked by the picked currency tier or omens (manual add still works)") : (ko ? "지금 아이템에는 붙지 않습니다(자리 · 같은 그룹 · 희귀도)" : "Can't roll on this item now (slots, same group or rarity)")}">—</span>
								@endif
							</div>
							<div>@if(topRank != null && top.family() != null)<button type="button" class="btn btn-xs btn-ghost border border-base-300" data-craft-pool-target="${f.key()}" data-tier="${topRank}" aria-pressed="false" title="${ko ? "시뮬레이터 목표로 — 이 계열 최고 등급 T" + topRank + "(다시 누르면 끔). 낮은 등급까지 허용하려면 펼쳐서 그 등급의 ☆" : "Simulator target — this family's top tier T" + topRank + " (click again to clear). To allow lower tiers, expand and use that tier's ☆"}">☆</button>@endif</div>
						</summary>
						<div class="px-2 pb-2 overflow-x-auto">
							<table class="table table-xs">
								<thead><tr><th>${ko ? "등급" : "Tier"}</th><th>${ko ? "옵션" : "Modifier"}</th><th class="text-right">${ko ? "레벨" : "Level"}</th><th class="text-right">${ko ? "확률" : "Chance"}</th><th class="text-right">${ko ? "넣기" : "Add"}</th></tr></thead>
								<tbody>
									@for(var t : f.tiers())
										!{
											Poe2Craft.PoolLine p = t.line();
											java.util.List<String> mx = ko && !nz.apply(p.ko()).isEmpty() ? p.ko() : nz.apply(p.en());
											boolean zero = !t.rollable() || t.chance() <= 0;
											// 접사 이름(10-22 P4, PoE1 짝) — 이름이 없거나 id 그대로면 안 보인다
											String affix = ko && p.nameKo() != null && !p.nameKo().isBlank() ? p.nameKo() : p.name();
											if (affix != null && (affix.isBlank() || affix.equals(p.id()))) affix = null;
										}
										<%-- 고른 등급에 걸린 줄(C445) = 확률 0 · 흐리게, 직접 편집 넣기는 그대로 --%>
										<tr data-craft-pool-line="${t.rollable() ? p.id() : null}" data-craft-pool-tier="${p.id()}" data-craft-pool-zero="${t.rollable() && t.chance() <= 0 ? "1" : null}" data-craft-pool-on="${t.applied() ? "1" : null}" class="${t.applied() ? "bg-primary/10" : zero ? "text-base-content/70" : null}">
											<td class="font-mono text-xs">@if(p.tierRank() != null)T${p.tierRank()}@endif@if(affix != null)<div class="font-sans text-[11px] leading-tight text-base-content/80" data-craft-affix-name>${affix}</div>@endif</td>
											<td class="min-w-0 wrap-anywhere">
												@for(String x : mx)
													<div>$unsafe{PoeText.highlightValues(x)}</div>
												@endfor
												@if(t.applied())<span class="badge badge-primary badge-xs">${ko ? "적용" : "on"}</span>@if(t.fresh()) <span class="badge badge-success badge-xs">${ko ? "방금" : "new"}</span>@endif@endif
											</td>
											<td class="text-right font-mono">${p.level()}</td>
											<td class="text-right font-mono">@if(zero)-@else${String.format("%.2f%%", t.chance() * 100)}@endif</td>
											<td class="text-right whitespace-nowrap">
												@if(p.tierRank() != null && p.family() != null)<button type="button" class="btn btn-xs btn-ghost border border-base-300" data-craft-pool-target="${p.gen()}:${p.family()}" data-tier="${p.tierRank()}" aria-pressed="false" title="${ko ? "시뮬레이터 목표로 — 이 계열 T1~T" + p.tierRank() + "(다시 누르면 끔). 고르면 화폐 단추에 목표 적중 확률" : "Simulator target — this family T1~T" + p.tierRank() + " (click again to clear). Shows hit chance on currency buttons"}">☆</button>@endif
												@if(t.rollable())
													<button type="button" name="action" value="MANUAL_ADD:${p.id()}" class="btn btn-xs btn-ghost border border-base-300" hx-post="/poe2/htmx/craft" hx-target="#craft2-emu" hx-swap="outerHTML" hx-include="#craft2-emu > form" data-craft-action="MANUAL_ADD:${p.id()}" title="${ko ? "이 옵션을 넣기" : "Add this modifier"}">+</button>
													<button type="button" name="action" value="MANUAL_ADD:${p.id()}:F" class="btn btn-xs btn-ghost border border-base-300" hx-post="/poe2/htmx/craft" hx-target="#craft2-emu" hx-swap="outerHTML" hx-include="#craft2-emu > form" data-craft-action="MANUAL_ADD:${p.id()}:F" title="${ko ? "분열 옵션으로 넣기" : "Add as fractured"}">${ko ? "+분열" : "+frac"}</button>
												@endif
											</td>
										</tr>
									@endfor
								</tbody>
							</table>
							<%-- 같은 옵션을 얻는 다른 길(10-22 P4, PoE1 짝) — PoE2 는 에센스가 풀 옵션 하나를 그대로 보장한다(제작대 · 화석 · 수확 없음). 누르면 그 에센스를 고른 상태로(common.ts [data-craft-way]) --%>
							!{
								java.util.Map<String, Integer> famRank = new java.util.HashMap<>();
								for (var tt : f.tiers()) if (tt.line().tierRank() != null) famRank.put(tt.line().id(), tt.line().tierRank());
								java.util.List<Poe2Craft.EssenceOption> famEss = essences.stream().filter(e -> famRank.containsKey(e.modId())).sorted(java.util.Comparator.comparing((Poe2Craft.EssenceOption e) -> famRank.get(e.modId()))).toList();
							}
							@if(!famEss.isEmpty())
								<div class="mt-2 space-y-1 text-xs" data-craft-ways="1">
									<div class="text-base-content/70">${ko ? "이 옵션을 보장하는 에센스 — 누르면 그 에센스를 고른 상태로" : "Essences that guarantee this — click to arm"}</div>
									<div class="flex flex-wrap gap-1">
										@for(var e : famEss)
											<button type="button" class="btn btn-xs btn-ghost border border-base-300 h-auto min-h-6 py-0.5" data-craft-way="ESSENCE" data-craft-way-value="${e.slug()}">${ko && e.nameKo() != null ? e.nameKo() : e.name()} <span class="text-base-content/60">T${famRank.get(e.modId())}</span></button>
										@endfor
									</div>
									<div class="text-warning" hidden aria-live="polite" data-craft-way-msg></div>
								</div>
							@endif
						</div>
					</details>
				@endfor
			</div>
		</details>
	@endif
@endfor
