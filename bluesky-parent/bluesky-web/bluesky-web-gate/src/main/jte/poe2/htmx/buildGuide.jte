@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe2.Poe2View
@import net.luversof.web.gate.poe.PoeText

@param Poe2.BuildGuide guide = null
@param String error = null
<%-- 무기 세트별 가이드(10-01) — 주 세트(DPS 큰 쪽). 다른 세트로 바꿔 볼 때도 표시를 유지하려고 게이트가 넘긴다. --%>
@param Integer mainSet = null
<%-- 거래소 주소 경로의 리그(10-08 C172) --%>
@param String tradeLeague = null

<%-- 업그레이드 가이드(PoB-PoE2 비교 계산기 — PoB 아이템 툴팁의 "끼우면/빼면" 과 같은 계산) — PoE1 빌드 화면의 가이드 자리.
     ① 고유 아이템으로 바꾸면(DPS 상위 · 생존 상위) ② 장비 칸 기여(빼면) ③ 보조젬 기여(끄면) ④ 특화·핵심 패시브 기여(빼면, 0 = 재분배 후보).
     DPS·EHP 를 따로 보인다(합친 점수 하나면 "점수는 올랐는데 DPS 가 내려간" 착시가 생긴다 — PoE1 가이드 교훈). --%>
!{
	boolean korean = PoeText.isKorean();
}
@if(error != null || guide == null || !Boolean.TRUE.equals(guide.available()) || guide.error() != null)
	<section class="space-y-1" data-poe2-guide>
		<h3 class="text-sm font-semibold">${korean ? "업그레이드 가이드" : "Upgrade guide"}</h3>
		@if(error != null)
			<div class="text-sm text-warning">${korean ? "코드를 읽지 못해 가이드를 만들지 못했습니다." : "Could not read the code."}</div>
		@elseif(guide == null || !Boolean.TRUE.equals(guide.available()))
			<div class="text-sm text-base-content/60">${korean ? "이 서버에는 PoB-PoE2 엔진이 없습니다." : "The PoB-PoE2 engine is not installed on this server."}</div>
		@else
			<div class="text-sm text-warning">${korean ? "가이드 계산에 실패했습니다: " : "Guide failed: "}<span class="font-mono text-xs">${guide.error().length() > 160 ? guide.error().substring(0, 160) + "…" : guide.error()}</span></div>
		@endif
	</section>
@else
	!{
		java.util.function.Function<Double, String> pct = v -> v == null ? "-" : Math.abs(v) < 0.05 ? "0" : String.format("%+.1f%%", v);
		java.util.function.Function<Double, String> tone = v -> v == null || Math.abs(v) < 0.05 ? "text-base-content/60" : v > 0 ? "text-success" : "text-error";
		java.util.List<Poe2.GuideNode> strong = new java.util.ArrayList<>();
		java.util.List<Poe2.GuideNode> zero = new java.util.ArrayList<>();
		for (Poe2.GuideNode n : guide.nodes()) {
			if (Math.abs(n.dps() == null ? 0 : n.dps()) < 0.05 && Math.abs(n.ehp() == null ? 0 : n.ehp()) < 0.05) zero.add(n);
			else strong.add(n);
		}
	}
	<section class="space-y-3" data-poe2-guide>
		<div class="flex flex-wrap items-baseline gap-2">
			<h3 class="text-sm font-semibold">${korean ? "업그레이드 가이드 (PoB-PoE2 계산)" : "Upgrade guide (PoB-PoE2)"}</h3>
			<span class="text-xs text-base-content/60" data-guide-tried>${korean ? "고유 아이템 " + guide.tried() + "가지를 끼워 보고 오르는 것만(PoB 고유 목록처럼 품질 20% 기준) · 요구 능력치가 모자라게 되거나 다른 축을 5% 넘게 깎는 교체는 뺐습니다" : guide.tried() + " unique swaps tried (20% quality, as in PoB's unique list) · excludes swaps that break attribute requirements or cost over 5% on the other axis"} (${guide.elapsedMs()}ms)</span>
		</div>
		<%-- 무기 세트별 추천(10-01 사용자 요청) — 세트마다 패시브·무기가 달라 추천도 달라진다. 이 가이드는 weaponSet 을 켠 채 계산했다(처음엔 주 세트).
		     다른 세트 단추는 그 세트로 가이드를 다시 계산해 이 구획을 통째로 바꾼다(가이드는 수 초~20초 — 단추에 로딩 표시). --%>
		@if(guide.weaponSet() != null)
			<div class="flex flex-wrap items-center gap-2 text-xs" data-guide-sets>
				<span class="text-base-content/60">${korean ? "무기 세트별 추천:" : "Recommendations by weapon set:"}</span>
				@for(int ws = 1; ws <= 2; ws++)
					!{ boolean on = guide.weaponSet() == ws; boolean isMain = mainSet != null && mainSet == ws; }
					@if(on)
						<span class="btn btn-xs btn-primary no-animation" aria-current="true" data-guide-set="${ws}">${korean ? "세트 " + ws : "Set " + ws}@if(isMain) · ${korean ? "주" : "main"}@endif</span>
					@else
						<button type="button" class="btn btn-xs btn-outline" data-guide-set="${ws}"
						        hx-post="/poe2/htmx/build/guide" hx-include="#poe2BuildImport [name=code]"
						        hx-vals='{"set": "${ws}", "main": "${mainSet == null ? "" : String.valueOf(mainSet)}"}'
						        hx-target="closest [data-poe2-guide]" hx-swap="outerHTML" hx-indicator="this">
							${korean ? "세트 " + ws : "Set " + ws}@if(isMain) · ${korean ? "주" : "main"}@endif
							<span class="loading loading-spinner loading-xs htmx-indicator"></span>
						</button>
					@endif
				@endfor
				<span class="text-base-content/60">${korean ? "— 세트마다 패시브·무기가 달라 그 세트를 켠 채 계산했습니다" : "— computed with that set active (passives and weapons differ per set)"}</span>
			</div>
		@endif
		<%-- 증감의 기준 — DPS 는 PoB 주 스킬 하나의 값이다. 주 스킬이 보조용(의식·오라 등)이면 DPS 추천이 그 스킬 기준이 되므로 밝힌다(09-30 실빌드 리치: 78 DPS) --%>
		@if(guide.base() != null)
			!{ String skillLabel = guide.base().skill() == null ? "-" : korean && guide.base().skillKo() != null ? guide.base().skillKo() : guide.base().skill(); }
			<div class="text-xs text-base-content/70" data-guide-base>
				${korean ? "기준: 주 스킬 " : "Baseline: main skill "}<span class="font-semibold">${skillLabel}</span>
				· DPS <span class="font-mono">${String.format("%,.0f", guide.base().dps() == null ? 0 : guide.base().dps())}</span>
				· EHP <span class="font-mono">${String.format("%,.0f", guide.base().ehp() == null ? 0 : guide.base().ehp())}</span>
				${korean ? " — 모든 증감(%)은 이 값 대비" : " — all % changes are relative to this"}
			</div>
			@if(guide.base().dps() != null && guide.base().dps() < 1000)
				<div role="alert" class="alert alert-warning py-1.5 text-xs" data-guide-lowdps>${korean ? "주 스킬 DPS 가 매우 낮습니다 — PoB 에서 이 빌드의 주 스킬(피해를 주는 스킬)로 바꿔 내보내면 DPS 추천이 그 스킬 기준이 됩니다." : "The main skill's DPS is very low — set the damaging skill as the main skill in PoB and re-export for meaningful DPS suggestions."}</div>
			@endif
		@endif

		<%-- ① 고유 아이템 교체 — DPS 상위 · 생존 상위 --%>
		<div class="grid gap-3 lg:grid-cols-2">
			@for(int k = 0; k < 2; k++)
				!{
					java.util.List<Poe2.GuideSwap> list = k == 0 ? guide.swapsDps() : guide.swapsEhp();
					String title = k == 0 ? (korean ? "피해(DPS)를 가장 올리는 고유 아이템" : "Best uniques for DPS") : (korean ? "생존(EHP)을 가장 올리는 고유 아이템" : "Best uniques for survival (EHP)");
				}
				<div class="rounded-box border border-base-200 bg-base-100 p-2" data-guide-swaps="${k == 0 ? "dps" : "ehp"}">
					<div class="text-xs font-semibold mb-1">${title}</div>
					@if(list.isEmpty())
						<div class="text-xs text-base-content/60">${korean ? "오르는 교체가 없습니다." : "No improving swap."}</div>
					@else
						<div class="overflow-x-auto">
							<table class="table table-xs">
								<thead><tr><th>${korean ? "칸" : "Slot"}</th><th>${korean ? "아이템" : "Item"}</th><th class="text-right">DPS</th><th class="text-right">EHP</th><th class="text-right">${korean ? "최대 피격" : "Max hit"}</th></tr></thead>
								<tbody>
									@for(Poe2.GuideSwap s : list)
										<tr>
											<td class="whitespace-nowrap">${Poe2View.slotName(s.slot())}</td>
											<td>
												<span class="inline-flex items-center gap-1.5">
													@if(s.image() != null)<img src="/poe-data/poe2/icons/${s.image()}" alt="" class="h-6 w-6 object-contain" loading="lazy">@endif
													@if(s.slug() != null)
														<%-- 마우스를 올리면 인게임 툴팁(PoE1 가이드와 같은 .poe-hover → #poePreview) --%>
														<a class="poe-hover link-hover font-semibold poe-name-unique cursor-pointer"
														   href="/poe2/uniques/${s.slug()}" target="_blank" rel="noopener"
														   hx-get="/poe2/htmx/uniques/detail?slug=${s.slug()}"
														   hx-target="#poePreview"
														   hx-trigger="mouseenter delay:100ms"
														   hx-swap="innerHTML">${korean && s.itemKo() != null ? s.itemKo() : s.item()}</a>
														@if(PoeText.trade2Url(tradeLeague, s.tradeQuery()) != null)
															<a class="badge badge-xs badge-outline badge-info" target="_blank" rel="noopener" href="${PoeText.trade2Url(tradeLeague, s.tradeQuery())}" data-guide-trade title="${korean ? "이 고유 아이템을 거래소에서 검색(즉시 구입 · 가격순)" : "Search trade for this unique"}">${korean ? "거래소" : "Trade"}</a>
														@endif
													@else
														<span class="text-[#af6025]">${s.item()}</span>
													@endif
												</span>
											</td>
											<td class="text-right font-mono ${tone.apply(s.dps())}">${pct.apply(s.dps())}</td>
											<td class="text-right font-mono ${tone.apply(s.ehp())}">${pct.apply(s.ehp())}</td>
											<td class="text-right font-mono ${tone.apply(s.maxHit())}">${pct.apply(s.maxHit())}</td>
										</tr>
									@endfor
								</tbody>
							</table>
						</div>
					@endif
				</div>
			@endfor
		</div>

		<%-- ⑧ 레어 목표 — 가이드가 붙은 뒤 따로 불러온다(10-02: 본 가이드가 4~6초 느려지지 않게). 코드 칸(#poe2BuildImport [name=code] — 본 가이드와 같은 선택자: 시뮬 결과 화면엔 #poe2BuildCode 가 없다, 10-02)과 이 가이드의 세트로 --%>
		<div data-guide-rares-lazy data-no-success-toast
		     hx-post="/poe2/htmx/build/guide/rares" hx-trigger="load" hx-swap="outerHTML"
		     hx-include="#poe2BuildImport [name=code]"
		     hx-vals='{"set": "${guide.weaponSet() == null ? "" : String.valueOf(guide.weaponSet())}"}'
		     class="rounded-box border border-base-200 bg-base-100 p-2 text-xs text-base-content/60">
			<span class="loading loading-spinner loading-xs align-middle mr-1"></span>${korean ? "좋은 레어로 바꾸면 — 계산 중(칸마다 옵션을 하나씩 재 봅니다)…" : "If you swap to a good rare — calculating…"}
		</div>

		<%-- ⑤ 보조젬 교체 — 가장 약한 보조 자리(미적용 우선)에 이 스킬에 적용되는 보조를 끼워 본 결과 --%>
		@if(guide.gemWeakest() != null)
			<div class="rounded-box border border-base-200 bg-base-100 p-2" data-guide-gemswaps>
				<div class="text-xs font-semibold mb-1">
					${korean ? "보조젬 교체: 「" + (guide.gemWeakestKo() != null ? guide.gemWeakestKo() : guide.gemWeakest()) + "」 자리에 끼우면" : "Support swap: replacing " + guide.gemWeakest()}
					<span class="font-normal text-base-content/60">${korean ? "(기여가 가장 작은 보조 · 이 스킬에 적용되는 보조 " + guide.gemTried() + "가지 중 · 같은 계열은 가장 좋은 하나만)" : "(weakest support · " + guide.gemTried() + " compatible supports tried)"}</span>
				</div>
				@if(guide.gemSwaps().isEmpty())
					<div class="text-xs text-base-content/60">${korean ? "오르는 교체가 없습니다." : "No improving swap."}</div>
				@else
					<div class="flex flex-wrap gap-1.5">
						@for(Poe2.GuideSupport s : guide.gemSwaps())
							<span class="inline-flex items-center gap-1 rounded-lg border border-base-200 px-2 py-0.5 text-xs">
								@if(s.slug() != null)<a class="poe-hover link link-hover" href="/poe2/gems/${s.slug()}" target="_blank" rel="noopener" hx-get="/poe2/htmx/gems/detail?slug=${s.slug()}" hx-target="#poePreview" hx-trigger="mouseenter delay:100ms" hx-swap="innerHTML">${korean && s.nameKo() != null ? s.nameKo() : s.name()}</a>@else${s.name()}@endif
								<span class="font-mono ${tone.apply(s.dps())}">DPS ${pct.apply(s.dps())}</span>
								@if(s.ehp() != null && Math.abs(s.ehp()) >= 0.05)<span class="font-mono ${tone.apply(s.ehp())}">EHP ${pct.apply(s.ehp())}</span>@endif
								<%-- 계보(lineage) 젬 — 드물게 얻는다(자동 다듬기 단계 이름의 "계보 젬(드묾)"과 같은 표시) --%>
								@if(Boolean.TRUE.equals(s.lineage()))
									<span class="badge badge-ghost badge-xs" data-guide-lineage title="${korean ? "계보 젬 — 드물게 얻는 젬이라 바로 구하기 어려울 수 있습니다" : "Lineage gem — rare drop, may be hard to obtain"}">${korean ? "계보" : "lineage"}</span>
								@endif
								<%-- 조건부 보조젬 — PoB 가 조건을 켠 채로 재서 부풀린다(API Poe2RefineService.CONDITIONAL_SUPPORTS 와 같은 목록, 자동 다듬기에선 뺀다) --%>
								@if(s.name() != null && (s.name().startsWith("Inexorable Critical") || s.name().startsWith("Minion Pact")))
									<span class="badge badge-warning badge-xs" data-guide-conditional
									      title="${korean ? (s.name().startsWith("Minion Pact") ? "주변 소환수를 희생해야 효과가 납니다 — 소환수가 없으면 인게임에선 이득이 없습니다" : "치명타 없이 흐른 시간만큼 쌓이는 효과라 PoB 는 최대 누적을 가정합니다 — 실제로는 치명타마다 초기화됩니다") : (s.name().startsWith("Minion Pact") ? "Needs a nearby minion to sacrifice — no gain in game without minions" : "Stacks while not critting; PoB assumes the maximum — it resets on every crit in game")}">${korean ? "조건부" : "conditional"}</span>
								@endif
							</span>
						@endfor
					</div>
				@endif
			</div>
		@endif

		<%-- ⑦ 다음에 찍을 패시브 — 8점 안에 닿는 특화·핵심마다 가는 길 전체를 더해 잰 증감, 점수당 순 --%>
		@if(guide.nextDps() != null && (!guide.nextDps().isEmpty() || !guide.nextEhp().isEmpty()))
			<div class="grid gap-3 lg:grid-cols-2" data-guide-next>
				@for(int k = 0; k < 2; k++)
					!{
						java.util.List<Poe2.GuideNextNode> list = k == 0 ? guide.nextDps() : guide.nextEhp();
						String title = k == 0 ? (korean ? "다음에 찍을 패시브 — 피해(DPS)" : "Next passives — DPS") : (korean ? "다음에 찍을 패시브 — 생존(EHP)" : "Next passives — EHP");
					}
					<div class="rounded-box border border-base-200 bg-base-100 p-2" data-guide-next-list="${k == 0 ? "dps" : "ehp"}">
						<div class="text-xs font-semibold mb-1">${title} <span class="font-normal text-base-content/60">${korean ? "(가는 길 포함 점수 · 점수당 순 · 안 찍은 특화·핵심 " + guide.nextTried() + "개 중)" : "(path points included · per point · " + guide.nextTried() + " tried)"}</span></div>
						@if(list.isEmpty())
							<div class="text-xs text-base-content/60">${korean ? "오르는 패시브가 없습니다." : "None."}</div>
						@else
							<table class="table table-xs">
								<thead><tr><th>${korean ? "패시브" : "Passive"}</th><th class="text-right">${korean ? "점수" : "Pts"}</th><th class="text-right">DPS</th><th class="text-right">EHP</th><th class="text-right">${korean ? "점당" : "Per pt"}</th></tr></thead>
								<tbody>
									@for(Poe2.GuideNextNode n : list)
										!{ Double main = k == 0 ? n.dps() : n.ehp(); }
										<tr>
											<td>
												<%-- 트리 화면에서 가는 길까지 찍힌 상태로 열린다 --%>
												@if(n.treeLink() != null)<a class="link link-hover" href="${n.treeLink()}" title="${korean ? "트리에서 가는 길 보기" : "Show the path on the tree"}">${korean && n.nameKo() != null ? n.nameKo() : n.name()}</a>@else${korean && n.nameKo() != null ? n.nameKo() : n.name()}@endif
												@if("Keystone".equals(n.type()))<span class="badge badge-ghost badge-xs ml-1">${korean ? "핵심" : "Keystone"}</span>@endif
											</td>
											<td class="text-right font-mono">${n.points()}</td>
											<td class="text-right font-mono ${tone.apply(n.dps())}">${pct.apply(n.dps())}</td>
											<td class="text-right font-mono ${tone.apply(n.ehp())}">${pct.apply(n.ehp())}</td>
											<td class="text-right font-mono ${tone.apply(main)}">${main == null || n.points() == null || n.points() == 0 ? "-" : pct.apply(main / n.points())}</td>
										</tr>
									@endfor
								</tbody>
							</table>
						@endif
					</div>
				@endfor
			</div>
		@endif

		<%-- ⑥ 칸별 옵션 목표 — 희귀·마법·일반 장비에 그 베이스 옵션 풀의 계열별 최고 등급(최대 롤)을 더해 본 결과(제작·구매 때 노릴 옵션) --%>
		@if(guide.modTargets() != null && guide.modTargets().stream().anyMatch(t -> !t.dps().isEmpty() || !t.ehp().isEmpty()))
			<div class="rounded-box border border-base-200 bg-base-100 p-2" data-guide-modtargets>
				<div class="text-xs font-semibold mb-1">
					${korean ? "칸별로 노릴 옵션" : "Mods to aim for, per slot"}
					<span class="font-normal text-base-content/60">${korean ? "(지금 아이템에 그 옵션의 최고 등급·최대 롤을 더하면 — 이미 있는 옵션은 제외, 인게임에선 접두·접미 칸이 비어야 붙습니다)" : "(adding the top tier at max roll to the current item — existing mods excluded)"}</span>
				</div>
				<div class="grid gap-2 md:grid-cols-2 xl:grid-cols-3">
					@for(Poe2.GuideModTarget t : guide.modTargets())
						@if(!t.dps().isEmpty() || !t.ehp().isEmpty())
							<div class="rounded-lg border border-base-200 px-2 py-1.5 text-xs" data-guide-modtarget="${t.slot()}">
								<div class="font-semibold">${Poe2View.slotName(t.slot())} <span class="font-normal text-base-content/60">${korean && t.itemKo() != null ? t.itemKo() : t.item()}</span></div>
								@for(int k = 0; k < 2; k++)
									!{ java.util.List<Poe2.GuideMod> mods = k == 0 ? t.dps() : t.ehp(); }
									@if(!mods.isEmpty())
										<div class="mt-1 text-[11px] text-base-content/60">${k == 0 ? (korean ? "피해(DPS)" : "DPS") : (korean ? "생존(EHP)" : "EHP")}</div>
										@for(Poe2.GuideMod m : mods)
											<div class="flex items-baseline gap-2">
												<span class="flex-1 text-[#8888ff]">${String.join(" / ", korean && m.linesKo() != null && !m.linesKo().isEmpty() ? m.linesKo() : m.lines())}</span>
												<span class="font-mono whitespace-nowrap ${tone.apply(k == 0 ? m.dps() : m.ehp())}">${pct.apply(k == 0 ? m.dps() : m.ehp())}</span>
											</div>
										@endfor
									@endif
								@endfor
							</div>
						@endif
					@endfor
				</div>
			</div>
		@endif

		<div class="grid gap-3 lg:grid-cols-3">
			<%-- ② 장비 칸 기여(빼면) --%>
			<div class="rounded-box border border-base-200 bg-base-100 p-2" data-guide-slots>
				<div class="text-xs font-semibold mb-1">${korean ? "장비 칸 기여 (빼면)" : "Slot contribution (if removed)"}</div>
				<table class="table table-xs">
					<thead><tr><th>${korean ? "칸" : "Slot"}</th><th class="text-right">DPS</th><th class="text-right">EHP</th></tr></thead>
					<tbody>
						@for(Poe2.GuideSlot s : guide.slots())
							<tr title="${korean && s.itemKo() != null ? s.itemKo() : s.item()}">
								<td class="whitespace-nowrap">${Poe2View.slotName(s.slot())}<div class="text-[10px] text-base-content/60 truncate max-w-[10rem]">${korean && s.itemKo() != null ? s.itemKo() : s.item()}</div></td>
								<td class="text-right font-mono ${tone.apply(s.dps())}">${pct.apply(s.dps())}</td>
								<td class="text-right font-mono ${tone.apply(s.ehp())}">${pct.apply(s.ehp())}</td>
							</tr>
						@endfor
					</tbody>
				</table>
			</div>
			<%-- ③ 보조젬 기여(끄면) --%>
			<div class="rounded-box border border-base-200 bg-base-100 p-2" data-guide-supports>
				<div class="text-xs font-semibold mb-1">${korean ? "주 스킬 보조젬 기여 (끄면 잃는 몫)" : "Support gem contribution"}</div>
				@if(guide.supports().isEmpty())
					<div class="text-xs text-base-content/60">${korean ? "주 스킬 묶음에 보조젬이 없습니다." : "No supports in the main group."}</div>
				@else
					<table class="table table-xs">
						<thead><tr><th>${korean ? "보조젬" : "Support"}</th><th class="text-right">DPS</th><th class="text-right">EHP</th></tr></thead>
						<tbody>
							@for(Poe2.GuideSupport s : guide.supports())
								<tr>
									<td>
										@if(s.slug() != null)<a class="poe-hover link link-hover" href="/poe2/gems/${s.slug()}" target="_blank" rel="noopener" hx-get="/poe2/htmx/gems/detail?slug=${s.slug()}" hx-target="#poePreview" hx-trigger="mouseenter delay:100ms" hx-swap="innerHTML">${korean && s.nameKo() != null ? s.nameKo() : s.name()}</a>@else${s.name()}@endif
										@if(!Boolean.TRUE.equals(s.applied()))<span class="badge badge-warning badge-xs ml-1" title="${korean ? "이 스킬에는 인게임에서 적용되지 않는 보조젬" : "Does not apply to this skill in game"}">${korean ? "미적용" : "not applied"}</span>@endif
									</td>
									<td class="text-right font-mono ${tone.apply(s.dps())}">${pct.apply(s.dps())}</td>
									<td class="text-right font-mono ${tone.apply(s.ehp())}">${pct.apply(s.ehp())}</td>
								</tr>
							@endfor
						</tbody>
					</table>
				@endif
			</div>
			<%-- ④ 특화·핵심 패시브 기여(빼면) --%>
			<div class="rounded-box border border-base-200 bg-base-100 p-2" data-guide-nodes>
				<div class="text-xs font-semibold mb-1">${korean ? "특화·핵심 패시브 기여 (빼면 잃는 몫)" : "Notable/keystone contribution"}</div>
				@if(!strong.isEmpty())
					<table class="table table-xs">
						<thead><tr><th>${korean ? "패시브" : "Passive"}</th><th class="text-right">DPS</th><th class="text-right">EHP</th></tr></thead>
						<tbody>
							@for(Poe2.GuideNode n : strong)
								<tr>
									<td>${korean && n.nameKo() != null ? n.nameKo() : n.name()}@if("Keystone".equals(n.type()))<span class="badge badge-ghost badge-xs ml-1">${korean ? "핵심" : "Keystone"}</span>@endif</td>
									<td class="text-right font-mono ${tone.apply(n.dps())}">${pct.apply(n.dps())}</td>
									<td class="text-right font-mono ${tone.apply(n.ehp())}">${pct.apply(n.ehp())}</td>
								</tr>
							@endfor
						</tbody>
					</table>
				@endif
				@if(!zero.isEmpty())
					<div class="mt-1 text-xs">
						<span class="text-base-content/60">${korean ? "DPS·EHP 기여 0 (재분배 후보 — 계산에 안 잡히는 효과일 수 있음):" : "No DPS/EHP effect (respec candidates — may be unmodelled):"}</span>
						@for(Poe2.GuideNode n : zero)<span class="badge badge-ghost badge-xs mr-1">${korean && n.nameKo() != null ? n.nameKo() : n.name()}</span>@endfor
					</div>
				@endif
			</div>
		</div>
	</section>
@endif
