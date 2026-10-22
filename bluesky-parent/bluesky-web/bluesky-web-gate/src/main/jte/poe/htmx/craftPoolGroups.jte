@import net.luversof.web.gate.poe.PoeText
@import net.luversof.web.gate.poe.PoeModRange
@import net.luversof.web.gate.poe.dto.PoeCraft

@param java.util.List<net.luversof.web.gate.poe.CraftPoolGroups.Family<PoeCraft.PoolLine>> poolGroups
@param java.util.List<PoeCraft.Line> lines
@param java.util.List<String> added = java.util.List.of()
@param int max
@param long prefixes
@param long suffixes
@param java.util.Map<String, java.util.List<String>> lineTags = java.util.Map.of()
@param String actionLabel = null
@param boolean actionPicked = false
@param String actionRarity = null
@param java.util.Map<String, Double> actionMult = java.util.Map.of()
@param String actionError = null
@param String waysUrl = null

!{
	boolean ko = PoeText.isKorean();
	java.util.function.Function<java.util.List<String>, java.util.List<String>> nz = l -> l == null ? java.util.List.of() : l;
}
<%-- 계열 묶음(10-21 C542 사용자 요청 "접두 · 접미를 등급별로 한 줄씩 무수히 — 보통 T1 을 원하니 옵션별로 묶어 줄을 줄이고, 누르면 등급 상세,
     제작으로 붙은 옵션은 활성화 표시") — 이 종류의 전체 계열을 고정 순서로 한 줄씩(최고 등급 문구 · 지금 붙을 확률 합 · 최고 등급 확률 · ☆ = 그 계열
     최고 등급 목표), 누르면 등급 표(예전 줄 그대로 — ☆ T1~Tn · + · +분열). 붙은 계열은 제자리에서 강조 · '적용 Tn', 방금 붙은 것은 '방금'.
     펼친 계열은 화폐를 써도 열린 채(data-craft-group → #craft-emu 의 data-keep-open), 누른 줄 자리 유지(data-swap-anchor-key) --%>
!{
	java.util.Set<String> famIds = new java.util.HashSet<>();
	for (var f : poolGroups) for (var t : f.tiers()) famIds.add(t.line().id());
}
<%-- 거르기(10-22 P1) — 태그 칩은 이 표 계열 태그를 많은 순으로 --%>
!{
	java.util.Map<String, Integer> famTagCount = new java.util.HashMap<>();
	for (var f : poolGroups) for (String t : ko ? nz.apply(f.top().tagsKo()) : nz.apply(f.top().tags())) famTagCount.merge(t, 1, Integer::sum);
	// 가중치 합(10-22 P2, poedb 속성 부여의 '가중치 합 · 쪽 합계') — 확률 = 줄 가중치 ÷ 양쪽(접두 + 접미) 붙을 수 있는 가중치 합(API pool 과 같은 식)
	long famAllW = poolGroups.stream().flatMap(f -> f.tiers().stream()).filter(t -> t.rollable()).mapToLong(t -> t.line().weight()).sum();
	java.util.List<String> famTags = famTagCount.keySet().stream().sorted(java.util.Comparator.comparing((String t) -> -famTagCount.get(t)).thenComparing(t -> t)).toList();
}
<%-- 고른 화폐 기준(10-22 P3) — 확률 = 그 수단이 처음 뽑는 옵션의 가중치 ÷ 합, ×n = 화폐가 바꾼 배수 --%>
!{ java.util.function.Function<Double, String> multText = x -> x == Math.rint(x) ? String.format(java.util.Locale.ROOT, "×%d", Math.round(x)) : String.format(java.util.Locale.ROOT, "×%.2f", x); }
@if(actionLabel != null)
	<div class="rounded-box border border-warning/40 bg-warning/10 px-2 py-1.5 text-xs" data-craft-action-pool="${actionPicked ? "1" : "0"}">
		@if(actionPicked)
			<span class="font-medium">${ko ? "고른 화폐 기준" : "Armed currency"}</span> — <span data-craft-action-pool-label>${actionLabel}</span>: ${ko ? "확률 = 이 수단이 처음 뽑는 옵션 하나의 가중치 ÷ 합" : "chance = weight of the first modifier this rolls ÷ total"}@if("RARE".equals(actionRarity))${ko ? " (레어 자리 기준)" : " (as a rare)"}@endif@if(!actionMult.isEmpty()) · ${ko ? "노란 ×n = 이 화폐가 바꾼 가중치 배수" : "yellow ×n = weight multiplier from this currency"}@endif
		@else
			<span class="font-medium" data-craft-action-pool-label>${actionLabel}</span> — ${actionError != null ? actionError : (ko ? "이 수단은 표의 옵션을 무작위로 뽑지 않습니다 — 지금 아이템 기준 표" : "This doesn't roll a random modifier from the table — showing odds for the current item")}
		@endif
	</div>
@endif
@if(!poolGroups.isEmpty())@template.poe.htmx.craftFamFilter(tags = famTags)@endif
@for(String gen : java.util.List.of("prefix", "suffix"))
	!{
		java.util.List<net.luversof.web.gate.poe.CraftPoolGroups.Family<PoeCraft.PoolLine>> gf = poolGroups.stream().filter(f -> gen.equals(f.gen())).toList();
		double gsum = gf.stream().mapToDouble(net.luversof.web.gate.poe.CraftPoolGroups.Family::sum).sum();
		long gw = gf.stream().flatMap(f -> f.tiers().stream()).filter(t -> t.rollable()).mapToLong(t -> t.line().weight()).sum();
		long gon = "prefix".equals(gen) ? prefixes : suffixes;
		java.util.List<PoeCraft.Line> outside = lines.stream().filter(l -> gen.equals(l.gen()) && !famIds.contains(l.id())).toList();
	}
	@if(!gf.isEmpty() || !outside.isEmpty())
		<details class="rounded-box border border-base-300 bg-base-100" open data-craft-pool-gen="${gen}">
			<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">
				${"prefix".equals(gen) ? (ko ? "접두" : "Prefixes") : (ko ? "접미" : "Suffixes")} · ${ko ? "붙음 " : ""}${gon}/${max}${ko ? "" : " on"} · ${ko ? "계열 " : ""}${gf.size()}${ko ? "개" : " families"} · ${String.format("%.1f%%", gsum * 100)}@if(famAllW > 0) · <span class="font-normal text-base-content/70" title="${ko ? "확률 = 가중치 ÷ 양쪽(접두 + 접미) 붙을 수 있는 가중치 합" : "Chance = weight ÷ the rollable weight of both sides"}" data-craft-gen-weight="${gw}" data-craft-all-weight="${famAllW}">${ko ? "가중 " : "weight "}${String.format(java.util.Locale.ROOT, "%,d", gw)} / ${String.format(java.util.Locale.ROOT, "%,d", famAllW)}</span>@endif
			</summary>
			<div class="px-2 pb-2 space-y-1" data-craft-fam-list>
				<p class="text-xs text-base-content/60" hidden data-craft-fam-none>${ko ? "거르기에 맞는 계열이 없습니다." : "No family matches the filter."}</p>
				@for(PoeCraft.Line l : outside)
					!{ java.util.List<String> ox = ko && !nz.apply(l.ko()).isEmpty() ? l.ko() : nz.apply(l.en()); java.util.List<String> on2 = ko && !nz.apply(l.koMin()).isEmpty() ? l.koMin() : nz.apply(l.enMin()); }
					<div class="rounded border-l-4 border-primary bg-primary/10 px-2 py-1.5 text-xs min-w-0 wrap-anywhere" data-craft-fam-outside="${l.id()}">
						@for(int i = 0; i < ox.size(); i++)<div>$unsafe{PoeText.highlightValues(PoeModRange.range(i < on2.size() ? on2.get(i) : null, ox.get(i)))}</div>@endfor
						<div class="text-[10px] text-base-content/70"><span class="badge badge-primary badge-xs">${ko ? "적용" : "on"}@if(l.tierRank() != null) T${l.tierRank()}@endif</span> ${l.crafted() ? (ko ? "제작대 옵션" : "bench craft") : l.essence() ? (ko ? "에센스 옵션" : "essence mod") : (ko ? "이 표 밖 옵션" : "outside this table")}@if(lineTags.get(l.id()) != null) — ${String.join(", ", lineTags.get(l.id()))}@endif@if(l.fractured()) · ${ko ? "분열" : "fractured"}@endif@if(added != null && added.contains(l.id())) <span class="badge badge-success badge-xs">${ko ? "방금" : "new"}</span>@endif</div>
					</div>
				@endfor
				@for(var f : gf)
					!{
						PoeCraft.PoolLine top = f.top();
						java.util.List<String> fx = ko && !nz.apply(top.ko()).isEmpty() ? top.ko() : nz.apply(top.en());
						java.util.List<String> fn = ko && !nz.apply(top.koMin()).isEmpty() ? top.koMin() : nz.apply(top.enMin());
						Integer topRank = top.tierRank();
						// 계열 태그(C544 사용자 요청 "옵션에 태그가 보여야") — 최고 등급 줄의 태그(계열 안 등급은 같은 태그)
						java.util.List<String> ftags = ko ? nz.apply(top.tagsKo()) : nz.apply(top.tags());
						Integer lastRank = f.tiers().get(f.tiers().size() - 1).line().tierRank();
						var on = f.applied();
						long fw = f.tiers().stream().filter(t -> t.rollable()).mapToLong(t -> t.line().weight()).sum();
						int fl = f.tiers().stream().mapToInt(t -> t.line().ilvl()).max().orElse(0);
						Double fm = f.tiers().stream().filter(t -> t.rollable()).map(t -> actionMult.get(t.line().id())).filter(java.util.Objects::nonNull).findFirst().orElse(null);
					}
					<details class="rounded border ${on != null ? "border-l-4 border-primary bg-primary/10" : "border-base-300"}" data-craft-group="fam:${f.key()}" data-swap-anchor-key="fam:${f.key()}" data-craft-fam="${f.key()}" data-craft-fam-tagset="${String.join("|", ftags)}" data-craft-fam-rollable="${f.rollable() ? "1" : null}" data-craft-fam-on="${on == null ? null : on.line().id()}">
						<summary class="cursor-pointer select-none grid grid-cols-[minmax(0,1fr)_auto_auto] items-center gap-2 px-2 py-1.5" title="${ko ? "누르면 등급별 상세" : "Click for every tier"}">
							<div class="min-w-0 text-xs wrap-anywhere ${!f.rollable() && on == null ? "text-base-content/60" : null}">
								@for(int i = 0; i < fx.size(); i++)<div>$unsafe{PoeText.highlightValues(PoeModRange.range(i < fn.size() ? fn.get(i) : null, fx.get(i)))}</div>@endfor
								<div class="text-[10px] leading-tight text-base-content/60">@if(topRank != null)T${topRank}@if(lastRank != null && !lastRank.equals(topRank))~T${lastRank}@endif · @endif${f.tiers().size()}${ko ? "단계" : (f.tiers().size() == 1 ? " tier" : " tiers")} · ${ko ? "레벨 " : "iLvl "}${fl}@if(fw > 0) · <span data-craft-fam-weight="${fw}">${ko ? "가중 " : "weight "}${String.format(java.util.Locale.ROOT, "%,d", fw)}</span>@endif@if(!ftags.isEmpty())<span data-craft-fam-tags> — ${String.join(", ", ftags)}</span>@endif
									@if(on != null) <span class="badge badge-primary badge-xs" data-craft-fam-applied="${on.line().tierRank()}">${ko ? "적용" : "on"}@if(on.line().tierRank() != null) T${on.line().tierRank()}@endif</span>@endif
									@if(on != null && on.fresh()) <span class="badge badge-success badge-xs" data-craft-fam-fresh>${ko ? "방금" : "new"}</span>@endif
								</div>
							</div>
							<div class="text-right font-mono text-xs leading-tight whitespace-nowrap" data-craft-fam-chance="${String.format("%.6f", f.sum())}">
								@if(fm != null)<span class="badge badge-warning badge-xs" data-craft-fam-mult="${fm}">${multText.apply(fm)}</span>@endif
								@if(f.rollable())
									${String.format("%.2f%%", f.sum() * 100)}
									@if(topRank != null)<div class="text-[10px] text-base-content/60" data-craft-fam-top>T${topRank} ${String.format("%.2f%%", f.topChance() * 100)}</div>@endif
								@else
									<span class="text-base-content/60" title="${on != null ? (ko ? "이미 붙은 계열은 다시 붙지 않습니다" : "Already on the item — can't roll again") : (ko ? "지금 아이템에는 붙지 않습니다(자리 · 같은 그룹 · 희귀도)" : "Can't roll on this item now (slots, same group or rarity)")}">—</span>
								@endif
							</div>
							<div>@if(topRank != null && top.family() != null)<button type="button" class="btn btn-xs btn-ghost border border-base-300" data-craft-pool-target="${f.key()}" data-tier="${topRank}" aria-pressed="false" title="${ko ? "계산기 목표로 — 이 계열 최고 등급 T" + topRank + "(다시 누르면 끔). 낮은 등급까지 허용하려면 펼쳐서 그 등급의 ☆" : "Calculator target — this family's top tier T" + topRank + " (click again to clear). To allow lower tiers, expand and use that tier's ☆"}">☆</button>@endif</div>
						</summary>
						<div class="px-2 pb-2 overflow-x-auto">
							<table class="table table-xs">
								<thead><tr><th title="${ko ? "등급 · 아래 작은 글씨 = 요구 아이템 레벨 · 가중치" : "Tier · item level and weight below"}">${ko ? "등급" : "Tier"}</th><th>${ko ? "옵션" : "Modifier"}</th><th class="text-right">${ko ? "확률" : "Chance"}</th><th class="text-right">${ko ? "넣기" : "Add"}</th></tr></thead>
								<tbody>
									@for(var t : f.tiers())
										!{
											PoeCraft.PoolLine p = t.line();
											java.util.List<String> mx = ko && !nz.apply(p.ko()).isEmpty() ? p.ko() : nz.apply(p.en());
											java.util.List<String> mn = ko && !nz.apply(p.koMin()).isEmpty() ? p.koMin() : nz.apply(p.enMin());
											// 접사 이름(10-22 P4, poedb · 인게임 상세 보기의 접두어 속성 이름) — 이름이 없거나 id 그대로면 안 보인다
											String affix = ko && p.nameKo() != null && !p.nameKo().isBlank() ? p.nameKo() : p.name();
											if (affix != null && (affix.isBlank() || affix.equals(p.id()))) affix = null;
										}
										<tr data-craft-pool-line="${t.rollable() ? p.id() : null}" data-craft-pool-tier="${p.id()}" data-craft-pool-on="${t.applied() ? "1" : null}" class="${t.applied() ? "bg-primary/10" : !t.rollable() ? "text-base-content/70" : null}">
											<td class="font-mono text-xs whitespace-nowrap">@if(p.tierRank() != null)T${p.tierRank()}@endif@if(affix != null)<div class="font-sans text-[11px] leading-tight text-base-content/80" data-craft-affix-name>${affix}</div>@endif
												<div class="text-[10px] leading-tight text-base-content/60" data-craft-pool-meta>${ko ? "레벨 " : "iLvl "}${p.ilvl()}<br>${ko ? "가중 " : "w "}${p.weight()}@if(t.rollable() && actionMult.get(p.id()) != null) <span class="badge badge-warning badge-xs" data-craft-tier-mult>${multText.apply(actionMult.get(p.id()))}</span>@endif</div>
											</td>
											<td class="min-w-0 wrap-anywhere">
												@for(int i = 0; i < mx.size(); i++)
													<div>$unsafe{PoeText.highlightValues(PoeModRange.range(i < mn.size() ? mn.get(i) : null, mx.get(i)))}</div>
												@endfor
												@if(t.applied())<span class="badge badge-primary badge-xs">${ko ? "적용" : "on"}</span>@if(t.fresh()) <span class="badge badge-success badge-xs">${ko ? "방금" : "new"}</span>@endif@endif
											</td>
											<td class="text-right font-mono" data-craft-chance="${t.rollable() ? "1" : null}">@if(t.rollable())${String.format("%.2f%%", t.chance() * 100)}@else<span class="text-base-content/60">—</span>@endif</td>
											<td class="text-right whitespace-nowrap">
												@if(p.tierRank() != null && p.family() != null)<button type="button" class="btn btn-xs btn-ghost border border-base-300" data-craft-pool-target="${p.gen()}:${p.family()}" data-tier="${p.tierRank()}" aria-pressed="false" title="${ko ? "계산기 목표로 — 이 계열 T1~T" + p.tierRank() + "(다시 누르면 끔). 고르면 화폐 단추에 목표 적중 확률" : "Calculator target — this family T1~T" + p.tierRank() + " (click again to clear). Shows hit chance on currency buttons"}">☆</button>@endif
												@if(t.rollable())
													<button type="button" name="action" value="MANUAL_ADD:${p.id()}" class="btn btn-xs btn-ghost border border-base-300" hx-post="/poe/htmx/craft" hx-target="#craft-emu" hx-swap="outerHTML" hx-include="#craft-emu > form" data-craft-action="MANUAL_ADD:${p.id()}" title="${ko ? "이 옵션을 넣기" : "Add this modifier"}">+</button>
													<button type="button" name="action" value="MANUAL_ADD:${p.id()}:F" class="btn btn-xs btn-ghost border border-base-300" hx-post="/poe/htmx/craft" hx-target="#craft-emu" hx-swap="outerHTML" hx-include="#craft-emu > form" data-craft-action="MANUAL_ADD:${p.id()}:F" title="${ko ? "분열 옵션으로 넣기" : "Add as fractured"}">${ko ? "+분열" : "+frac"}</button>
												@endif
											</td>
										</tr>
									@endfor
								</tbody>
							</table>
							@if(waysUrl != null)<div class="mt-1 text-xs text-base-content/60" data-craft-ways-url="${waysUrl}&family=${net.luversof.web.gate.poe.CraftPoolGroups.enc(f.key())}" data-craft-ways-slot>${ko ? "같은 옵션을 얻는 다른 길 불러오는 중…" : "Loading other ways to get this…"}</div>@endif
						</div>
					</details>
				@endfor
			</div>
		</details>
	@endif
@endfor
