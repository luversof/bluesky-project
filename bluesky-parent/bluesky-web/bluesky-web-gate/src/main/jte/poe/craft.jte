@import java.util.List
@import net.luversof.web.gate.poe.dto.PoeModItemClass
@import net.luversof.web.gate.poe.dto.PoeCraft
@import net.luversof.web.gate.poe.PoeText

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param List<PoeModItemClass> modClasses = java.util.List.of()
@param String activeClass = ""
@param String activeVariant = ""
@param String activeInfluence = ""
@param int ilvl = 84
@param String classLabel = ""
@param PoeCraft.View view = null
@param String error = null
@param String lastAction = null
@param java.util.List<String> added = java.util.List.of()
@param String stateJson = "{}"
@param String historyJson = "[]"
@param int historySize = 0
@param java.util.List<PoeCraft.PoolLine> pool = java.util.List.of()
@param java.util.List<PoeCraft.Essence> essences = java.util.List.of()
@param java.util.List<PoeCraft.PoolLine> calcPool = java.util.List.of()
@param java.util.List<PoeCraft.VeiledLine> calcVeiled = java.util.List.of()
@param PoeCraft.Options options = null

<%-- PoE1 제작 에뮬레이터(10-15 C335 — 사용자 요청 "craftofexile 처럼 직접 아이템을 제작해 보는 에뮬레이터"). 종류 · 변형 · 영향력 · 아이템 레벨을
     고르면 새 일반 아이템, 화폐를 누르면 그 결과(옵션 확률은 게임 데이터의 풀별 가중치). --%>
!{
	boolean ko = PoeText.isKorean();
	String pageTitle = ko ? "제작 에뮬레이터" : "Crafting Emulator";
	PoeModItemClass active = modClasses.stream().filter(c -> c.itemClass().equals(activeClass)).findFirst().orElse(null);
}

@template._layout.poeLayout(
	title = pageTitle,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentPoe = @`
		<div class="space-y-4" data-craft-page>
			<div class="flex flex-wrap items-baseline gap-3">
				<h1 class="text-2xl font-bold">${pageTitle}</h1>
				@if(!patch.isEmpty())<span class="badge badge-ghost">${patch}</span>@endif
				<span class="text-sm text-base-content/60">${ko ? "화폐를 하나씩 써 보며 결과와 다음 옵션 확률을 봅니다. 확률은 게임 데이터의 옵션 가중치로 계산합니다." : "Apply currency one step at a time and see the result and the odds of the next modifier, computed from the game's modifier weights."} <a href="#craft-calc" class="link link-primary" data-craft-calc-jump>${ko ? "→ 제작 계산기(가장 적은 비용) · 다단계 전략" : "→ Crafting calculator (cheapest) · multi-step strategy"}</a></span>
			</div>

			<%-- 바꾸면 만들던 아이템 · 되돌리기 이력이 초기화된다 — 이력이 있으면 확인, 취소하면 되돌림(C449 UX QA, PoE2 C442 와 같은 공용 장치) --%>
			<form method="get" action="/poe/craft" class="flex flex-wrap items-end gap-3" data-craft-picker data-confirm-dirty="#craft-emu input[name=history]" data-confirm-text="${ko ? "아이템 종류 · 변형 · 영향력 · 아이템 레벨을 바꾸면 지금 만들던 아이템과 되돌리기 이력이 초기화됩니다. 계속할까요?" : "Changing the item class, variant, influence or item level resets the item you are crafting and its undo history. Continue?"}"
			      hx-get="/poe/craft" hx-trigger="change" hx-target="[data-craft-page]" hx-select="[data-craft-page]" hx-swap="outerHTML" hx-push-url="true">
				<label class="form-control min-w-0">
					<span class="label-text text-xs">${ko ? "아이템 종류" : "Item class"}</span>
					<select name="itemClass" class="select select-sm select-bordered">
						@for(PoeModItemClass c : modClasses)
							<option value="${c.itemClass()}" selected="${c.itemClass().equals(activeClass)}">${ko && c.nameKo() != null ? c.nameKo() : c.name()}</option>
						@endfor
					</select>
				</label>
				@if(active != null && !active.variants().isEmpty())
					<label class="form-control min-w-0">
						<span class="label-text text-xs">${ko ? "속성 변형" : "Attribute variant"}</span>
						<select name="variant" class="select select-sm select-bordered">
							@for(PoeModItemClass.Variant v : active.variants())
								<option value="${v.key()}" selected="${v.key().equals(activeVariant)}">${ko && v.nameKo() != null ? v.nameKo() : v.name()}</option>
							@endfor
						</select>
					</label>
				@endif
				@if(active != null && active.influences() != null && !active.influences().isEmpty())
					<label class="form-control min-w-0">
						<span class="label-text text-xs">${ko ? "영향력" : "Influence"}</span>
						<select name="influence" class="select select-sm select-bordered">
							<option value="" selected="${activeInfluence.isEmpty()}">${ko ? "없음" : "None"}</option>
							@for(PoeModItemClass.Influence inf : active.influences())
								<option value="${inf.key()}" selected="${inf.key().equals(activeInfluence)}">${ko && inf.nameKo() != null ? inf.nameKo() : inf.name()}</option>
							@endfor
						</select>
					</label>
				@endif
				<label class="form-control w-28">
					<span class="label-text text-xs">${ko ? "아이템 레벨" : "Item level"}</span>
					<input type="number" name="ilvl" min="1" max="100" value="${ilvl}" class="input input-sm input-bordered">
				</label>
			</form>

			@if(view != null)
				@template.poe.htmx.craftEmu(view = view, error = error, lastAction = lastAction, added = added, stateJson = stateJson, historyJson = historyJson, historySize = historySize, pool = pool, essences = essences, classLabel = classLabel, options = options, imprintJson = "", foresee = "", preview = null, previewAction = null, previewArg = null, spentJson = "[]", spent = null, rollsJson = "{}", rolls = java.util.Map.of(), rollHistJson = "[]", imprintRollsJson = "{}", historyRows = java.util.List.of(), historyStartUndo = null, historyDropped = 0)
			@else
				<p class="text-sm text-base-content/60">${ko ? "제작 데이터를 불러오지 못했습니다." : "Crafting data is not available."}</p>
			@endif

			@if(!calcPool.isEmpty())
				<%-- 계산기(10-15 C336) — 계열마다 최소 등급을 고르면 방법별 기대 비용(가장 적은 비용 순). 목표는 일반 → 레어 기준 전체 풀에서 고른다 --%>
				<section id="craft-calc" class="space-y-3 border-t border-base-300 pt-4 scroll-mt-24" data-craft-calc data-no-success-toast>
					<h2 class="text-lg font-semibold">${ko ? "제작 계산기 — 가장 적은 비용" : "Crafting calculator — cheapest method"}</h2>
					<p class="text-sm text-base-content/60">${ko ? "원하는 옵션의 최소 등급을 고르면 카오스 · 변화 · 에센스 · 화석 · 수확 연타와, 그 연타로 목표 일부를 맞춘 뒤 제왕 · 엑잘티드 · 제작대로 마무리하는 조합까지 성공 확률과 기대 비용(poe.ninja 시세)을 비교합니다." : "Pick the minimum tier of the modifiers you want to compare chaos, alteration, essence, fossil and harvest spam, plus combinations that roll part of the targets and finish with regal, exalted or bench crafts, by chance and expected cost (poe.ninja prices)."}</p>
					<%-- 계산 중 표시(C422 UX QA — 6~10초 동안 아무 변화가 없어 다시 눌렀다): 버튼을 끄고 진행 표시 --%>
					<form hx-post="/poe/htmx/craft/calc" hx-target="#craft-calc-result" hx-swap="outerHTML" hx-include="#craft-emu input[name=state]" hx-disabled-elt="find [data-craft-calc-run], find [data-craft-strategy-run]" hx-indicator="[data-craft-calc-busy]" class="space-y-3">
						<input type="hidden" name="itemClass" value="${activeClass}">
						<input type="hidden" name="variant" value="${activeVariant}">
						<input type="hidden" name="influence" value="${activeInfluence}">
						<input type="hidden" name="ilvl" value="${ilvl}">
						<div class="grid gap-3 lg:grid-cols-2">
							@for(String gen : java.util.List.of("prefix", "suffix"))
								!{
									java.util.Map<String, java.util.List<PoeCraft.PoolLine>> fams = new java.util.LinkedHashMap<>();
									for (PoeCraft.PoolLine p : calcPool) if (gen.equals(p.gen())) fams.computeIfAbsent(p.family(), k -> new java.util.ArrayList<>()).add(p);
								}
								<div class="space-y-1 min-w-0">
									<div class="text-sm font-medium">${"prefix".equals(gen) ? (ko ? "접두" : "Prefixes") : (ko ? "접미" : "Suffixes")}</div>
									@for(var fe : fams.entrySet())
										!{
											java.util.List<PoeCraft.PoolLine> tiers = fe.getValue().stream().sorted(java.util.Comparator.comparing(p -> p.tierRank() == null ? 99 : p.tierRank())).toList();
											PoeCraft.PoolLine top = tiers.get(0);
											java.util.List<String> mx = ko && top.ko() != null && !top.ko().isEmpty() ? top.ko() : top.en();
											java.util.List<String> mn = ko && top.koMin() != null && !top.koMin().isEmpty() ? top.koMin() : top.enMin();
										}
										<label class="flex items-center gap-2 text-xs min-w-0" data-craft-calc-family="${gen}:${fe.getKey()}">
											<select name="t_${gen}:${fe.getKey()}" class="select select-xs select-bordered w-24 shrink-0" aria-label="${ko ? "최소 등급" : "Minimum tier"}">
												<option value="">-</option>
												@for(PoeCraft.PoolLine t : tiers)
													@if(t.tierRank() != null)<option value="${t.tierRank()}">T1@if(t.tierRank() > 1)~T${t.tierRank()}@endif</option>@endif
												@endfor
											</select>
											<span class="min-w-0 wrap-anywhere">@for(int i = 0; i < mx.size(); i++)$unsafe{PoeText.highlightValues(net.luversof.web.gate.poe.PoeModRange.range(mn != null && i < mn.size() ? mn.get(i) : null, mx.get(i)))}@if(i < mx.size() - 1), @endif@endfor</span>
										</label>
									@endfor
								</div>
							@endfor
						</div>
						@if(!calcVeiled.isEmpty())
							<%-- 장막 해제 옵션 목표(C421) — 체크하면 '장막의 카오스 오브 → 해제(목표 먼저)' 방법도 비교 --%>
							<details class="rounded-box border border-base-300 bg-base-100" data-craft-calc-veil>
								<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "장막 해제 옵션 목표" : "Unveiled modifier targets"} <span class="text-xs font-normal text-base-content/70">· ${calcVeiled.size()}${ko ? "종 · 고르면 장막의 카오스 오브 → 해제 방법도 비교" : " · picking one adds Veiled Chaos Orb → unveil"}</span></summary>
								<div class="px-3 pb-3 grid gap-1 md:grid-cols-2">
									@for(PoeCraft.VeiledLine vl : calcVeiled)
										!{ java.util.List<String> vtx = ko && vl.ko() != null && !vl.ko().isEmpty() ? vl.ko() : vl.en(); }
										<label class="flex items-start gap-2 text-xs min-w-0" data-craft-calc-veil-mod="${vl.id()}">
											<input type="checkbox" name="t_veil:${vl.id()}" value="1" class="checkbox checkbox-xs mt-0.5">
											<span class="min-w-0 wrap-anywhere">${"prefix".equals(vl.gen()) ? (ko ? "접두 " : "P ") : (ko ? "접미 " : "S ")}${String.join(", ", vtx == null ? java.util.List.of() : vtx)}</span>
										</label>
									@endfor
								</div>
							</details>
						@endif
						<div class="flex flex-wrap items-center gap-2">
							@if(!essences.isEmpty())
								<%-- 좁은 폭에선 한 줄 전체(C422 UX QA — 375px 한국어에서 옆 체크 문구에 밀려 62px 로 눌렸다) --%>
								<select name="essence" class="select select-sm select-bordered min-w-0 flex-1 basis-full sm:basis-0" aria-label="${ko ? "비교할 에센스" : "Essence to compare"}">
									<option value="">${ko ? "에센스 비교 안 함" : "No essence"}</option>
									@for(PoeCraft.Essence e : essences)
										<option value="${e.slug()}">${ko ? e.nameKo() : e.name()} — ${String.join(", ", ko && e.ko() != null && !e.ko().isEmpty() ? e.ko() : e.en())}</option>
									@endfor
								</select>
							@endif
							<%-- 지금 아이템에서 시작(10-15 C349) — 에뮬레이터의 분열 옵션 · 임플리싯을 남긴 채(분열 베이스 · 엘드리치 베이스). "직접 편집"으로 만든 아이템 --%>
							<label class="flex items-center gap-1 text-xs" title="${ko ? "분열 옵션과 엘드리치 임플리싯은 남기고 나머지는 비운 채 시작합니다(분열 옵션이 있으면 레어)" : "Keeps fractured modifiers and eldritch implicits, clears the rest (rare if anything is fractured)"}"><input type="checkbox" name="fromState" value="1" class="checkbox checkbox-xs" data-craft-from-state>${ko ? "지금 아이템(분열 · 임플리싯)에서 시작" : "Start from the current item (fractured · implicits)"}</label>
							<button type="submit" class="btn btn-sm btn-primary" data-craft-calc-run>${ko ? "계산" : "Calculate"}</button>
							<span class="htmx-indicator text-xs text-base-content/70" data-craft-calc-busy role="status"><span class="loading loading-spinner loading-xs align-middle"></span> ${ko ? "계산 중(최대 10초)…" : "Calculating (up to 10 s)…"}</span>
						</div>
						<%-- 다단계 전략(10-15 C339) — 같은 목표로 프리셋 전략을 모의(craftofexile 시뮬레이터처럼 단계 · 조건) --%>
						<div class="flex flex-wrap items-center gap-2 border-t border-base-300 pt-3" data-craft-strategy>
							<span class="text-sm font-medium">${ko ? "다단계 전략" : "Multi-step strategy"}</span>
							<select name="preset" class="select select-sm select-bordered min-w-0" aria-label="${ko ? "전략" : "Strategy"}">
								<option value="chaos">${ko ? "연금 → 카오스 반복" : "Alchemy → chaos spam"}</option>
								<option value="alt-regal">${ko ? "변화(+확장) → 제왕 → 엑잘 ×3" : "Alteration (+aug) → Regal → Exalt ×3"}</option>
								<option value="essence">${ko ? "에센스 반복(위에서 고른 에센스)" : "Essence spam (essence chosen above)"}</option>
								<option value="veiled">${ko ? "연금 → [장막의 카오스 → 해제(목표 먼저)] 반복" : "Alchemy → [Veiled Chaos → unveil (target first)] repeat"}</option>
								<option value="prefix-lock">${ko ? "카오스로 접두 확보 → [접두 잠금 + 카오스] 반복" : "Chaos for prefixes → [prefix lock + chaos]"}</option>
								<option value="suffix-lock">${ko ? "카오스로 접미 확보 → [접미 잠금 + 카오스] 반복" : "Chaos for suffixes → [suffix lock + chaos]"}</option>
								<option value="custom">${ko ? "직접 엮기(아래 단계 표)" : "Custom (steps below)"}</option>
							</select>
							<button type="submit" class="btn btn-sm" hx-post="/poe/htmx/craft/strategy" hx-target="#craft-strategy-result" hx-swap="outerHTML" hx-disabled-elt="this" hx-indicator="[data-craft-calc-busy]" data-craft-strategy-run>${ko ? "전략 모의" : "Simulate strategy"}</button>
						</div>
						<%-- 전략 직접 엮기(10-15 C344 — C339 남은 것). 줄마다 화폐 · 언제까지(한 번 / 목표 전부 · 접두 목표 · 접미 목표가 맞을 때까지) · 최대 횟수.
						     "다음 줄과 묶음"이면 이어지는 줄들을 한 묶음으로 반복한다(예: [접두 잠금 → 카오스] 반복). 전략 칸에서 "직접 엮기"를 골라야 쓴다 --%>
						!{
							java.util.List<String[]> acts = new java.util.ArrayList<>();
							for (String[] b : java.util.List.of(
									new String[] {"TRANSMUTE", "진화의 오브", "Orb of Transmutation"}, new String[] {"ALTERATION", "변화의 오브", "Orb of Alteration"},
									new String[] {"AUGMENTATION", "확장의 오브", "Orb of Augmentation"}, new String[] {"REGAL", "제왕의 오브", "Regal Orb"},
									new String[] {"ALCHEMY", "연금술의 오브", "Orb of Alchemy"}, new String[] {"CHAOS", "카오스 오브", "Chaos Orb"},
									new String[] {"EXALT", "엑잘티드 오브", "Exalted Orb"}, new String[] {"ANNUL", "소멸의 오브", "Orb of Annulment"},
									new String[] {"SCOUR", "정제의 오브", "Orb of Scouring"}, new String[] {"VEILED_CHAOS", "장막의 카오스 오브", "Veiled Chaos Orb"},
									new String[] {"BENCH_REMOVE", "제작대 옵션 제거", "Remove crafted mods"})) acts.add(new String[] {ko ? "기본 화폐" : "Basic currency", b[0], ko ? b[1] : b[2]});
							for (PoeCraft.Essence e : essences) acts.add(new String[] {ko ? "에센스" : "Essence", "ESSENCE:" + e.slug(), ko ? e.nameKo() : e.name()});
							if (options != null) {
								for (PoeCraft.Fossil f : options.fossils()) if (f.supported()) acts.add(new String[] {ko ? "화석(하나)" : "Fossil (one)", "FOSSIL:" + f.slug(), ko ? f.nameKo() : f.name()});
								for (PoeCraft.BenchCraft b : options.bench()) if (b.modId() != null && b.modId().contains("MasterItemGeneration")) acts.add(new String[] {ko ? "제작대 메타" : "Bench meta", "BENCH:" + b.modId(), String.join(" ", ko && b.ko() != null && !b.ko().isEmpty() ? b.ko() : b.en())});
								for (PoeCraft.Harvest h : options.harvest()) if ("reroll_with_mod".equals(h.command())) acts.add(new String[] {ko ? "수확" : "Harvest", "HARVEST:" + h.id(), ko && h.textKo() != null ? h.textKo() : h.text()});
							}
							// 섞어 쓰는 화폐(C340~C346) — 분열 · 장막 엑잘 · 엘드리치 · 지배 · 각성자
							acts.add(new String[] {ko ? "기본 화폐" : "Basic currency", "FRACTURE", ko ? "분열의 오브" : "Fracturing Orb"});
							acts.add(new String[] {ko ? "기본 화폐" : "Basic currency", "VEILED_EXALT", ko ? "장막의 엑잘티드 오브" : "Veiled Exalted Orb"});
							// 장막 해제(C427) — 셋 중 목표에 드는 것 먼저(실행기 unveilChoice), 없으면 무작위
							acts.add(new String[] {ko ? "기본 화폐" : "Basic currency", "UNVEIL", ko ? "장막 해제(목표 먼저)" : "Unveil (target first)"});
							// 삿된 화폐(C412 — C401 은 에뮬레이터에만 넣고 전략 목록엔 빠졌다)
							acts.add(new String[] {ko ? "기본 화폐" : "Basic currency", "FOULBORN_AUG", ko ? "삿된 확장의 오브" : "Foulborn Orb of Augmentation"});
							acts.add(new String[] {ko ? "기본 화폐" : "Basic currency", "FOULBORN_REGAL", ko ? "삿된 제왕의 오브" : "Foulborn Regal Orb"});
							acts.add(new String[] {ko ? "기본 화폐" : "Basic currency", "FOULBORN_EXALT", ko ? "삿된 엑잘티드 오브" : "Foulborn Exalted Orb"});
							if (java.util.Set.of("Body Armour", "Boots", "Gloves", "Helmet").contains(activeClass)) {
								String[] tk = {"하급", "상급", "우수한", "특출난"};
								String[] te = {"Lesser", "Greater", "Grand", "Exceptional"};
								for (int t = 1; t <= 4; t++) acts.add(new String[] {ko ? "엘드리치" : "Eldritch", "EMBER:" + t, ko ? tk[t - 1] + " 섬뜩한 불씨" : te[t - 1] + " Eldritch Ember"});
								for (int t = 1; t <= 4; t++) acts.add(new String[] {ko ? "엘드리치" : "Eldritch", "ICHOR:" + t, ko ? tk[t - 1] + " 섬뜩한 영액" : te[t - 1] + " Eldritch Ichor"});
								acts.add(new String[] {ko ? "엘드리치" : "Eldritch", "ELDRITCH_CHAOS", ko ? "섬뜩한 카오스 오브" : "Eldritch Chaos Orb"});
								acts.add(new String[] {ko ? "엘드리치" : "Eldritch", "ELDRITCH_EXALT", ko ? "섬뜩한 엑잘티드 오브" : "Eldritch Exalted Orb"});
								acts.add(new String[] {ko ? "엘드리치" : "Eldritch", "ELDRITCH_ANNUL", ko ? "섬뜩한 소멸의 오브" : "Eldritch Orb of Annulment"});
								// 갈등의 오브(C412 — C398 은 에뮬레이터에만)
								acts.add(new String[] {ko ? "엘드리치" : "Eldritch", "CONFLICT", ko ? "갈등의 오브" : "Orb of Conflict"});
								acts.add(new String[] {ko ? "영향력" : "Influence", "DOMINANCE", ko ? "지배의 오브" : "Orb of Dominance"});
							}
							if (options != null) {
								java.util.Map<String, String[]> cq = java.util.Map.of("shaper", new String[] {"쉐이퍼", "Shaper"}, "elder", new String[] {"엘더", "Elder"}, "crusader", new String[] {"십자군", "Crusader"}, "eyrie", new String[] {"대속자", "Redeemer"}, "basilisk", new String[] {"사냥꾼", "Hunter"}, "adjudicator", new String[] {"전쟁군주", "Warlord"});
								for (String c : options.conquerors()) {
									String[] n = cq.getOrDefault(c, new String[] {c, c});
									acts.add(new String[] {ko ? "영향력" : "Influence", "CONQUEROR:" + c, ko ? n[0] + "의 엑잘티드 오브" : n[1] + "'s Exalted Orb"});
									acts.add(new String[] {ko ? "영향력" : "Influence", "AWAKENER:" + c, ko ? "각성자의 오브 + " + n[0] : "Awakener's Orb + " + n[1]});
								}
								// 짐승 제작(C395 — C387 · C388): 맞바꾸기 · 그 영향력 아이템에 영향력 옵션 · 위상 스킬
								acts.add(new String[] {ko ? "짐승 제작" : "Beastcraft", "BEAST:swap-suffix", ko ? "접미어 추가, 무작위 접두어 제거" : "Add a Suffix, Remove a Random Prefix"});
								acts.add(new String[] {ko ? "짐승 제작" : "Beastcraft", "BEAST:swap-prefix", ko ? "접두어 추가, 무작위 접미어 제거" : "Add a Prefix, Remove a Random Suffix"});
								for (String c : options.conquerors()) {
									String[] n = cq.getOrDefault(c, new String[] {c, c});
									acts.add(new String[] {ko ? "짐승 제작" : "Beastcraft", "BEAST:influence:" + c, ko ? n[0] + " 아이템에 속성 부여 추가" : "Add a Mod to a " + n[1] + " Item"});
								}
								if (!activeClass.contains("Flask") && !activeClass.contains("Jewel")) {
									for (String[] asp : java.util.List.of(new String[] {"cat", "고양이", "Cat"}, new String[] {"avian", "새", "Avian"}, new String[] {"spider", "거미", "Spider"}, new String[] {"crab", "게", "Crab"})) acts.add(new String[] {ko ? "짐승 제작" : "Beastcraft", "BEAST:aspect:" + asp[0], ko ? "30레벨 " + asp[1] + "의 위상 스킬" : "Level 30 Aspect of the " + asp[2] + " skill"});
								}
								// 두카트(C395 — C391~C394): 이 종류에 맞는 것만
								acts.add(new String[] {ko ? "두카트" : "Ducat", "DUCAT:genteel", ko ? "고상한 자의 두카트" : "The Genteel's Ducat"});
								if ("Belt".equals(activeClass)) acts.add(new String[] {ko ? "두카트" : "Ducat", "DUCAT:rotmother", ko ? "부패모의 두카트" : "Rotmother's Ducat"});
								if (!activeClass.contains("Flask") && !activeClass.contains("Jewel")) acts.add(new String[] {ko ? "두카트" : "Ducat", "DUCAT:brinehook", ko ? "브라인훅의 두카트" : "Brinehook's Ducat"});
								if (java.util.Set.of("Body Armour", "Boots", "Gloves", "Helmet", "Amulet", "Belt", "Ring", "Quiver", "Bow", "Staff", "Warstaff", "Wand").contains(activeClass)) acts.add(new String[] {ko ? "두카트" : "Ducat", "DUCAT:cyaxan", ko ? "사이악산의 두카트" : "Cyaxan's Ducat"});
								if ("Amulet".equals(activeClass)) acts.add(new String[] {ko ? "두카트" : "Ducat", "DUCAT:ukatoa", ko ? "우카토아의 두카트" : "Ukatoa's Ducat"});
								if ("AbyssJewel".equals(activeClass)) acts.add(new String[] {ko ? "두카트" : "Ducat", "DUCAT:merrick", ko ? "메릭의 두카트" : "Merrick's Ducat"});
								// 기폭제(C395 — C385): 장신구, 한 단계 = 20%까지(쓴 개수만큼)
								if (java.util.Set.of("Ring", "Amulet", "Belt").contains(activeClass) && options.currencies() != null) {
									for (PoeCraft.CurrencyInfo ci : options.currencies()) if (ci.catalyst() != null && !"vaal".equals(ci.catalyst())) acts.add(new String[] {ko ? "기폭제" : "Catalyst", "CATALYST:" + ci.catalyst() + "*20", (ko ? ci.nameKo() + " — 20%까지" : ci.name() + " — to 20%")});
								}
							}
							java.util.Map<String, java.util.List<String[]>> actGroups = new java.util.LinkedHashMap<>();
							for (String[] a : acts) actGroups.computeIfAbsent(a[0], k -> new java.util.ArrayList<>()).add(a);
						}
						<details class="rounded-box border border-base-300 bg-base-100" data-craft-strategy-editor>
							<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "단계 직접 엮기" : "Build your own steps"} <span class="text-xs font-normal text-base-content/60">· ${ko ? "전략에서 \"직접 엮기\"를 고르면 이 표대로 모의" : "pick \"Custom\" above to simulate these steps"}</span></summary>
							<div class="px-3 pb-3 space-y-2">
								@for(int i = 1; i <= 6; i++)
									<div class="flex flex-wrap items-center gap-2" data-craft-step-row="${i}">
										<span class="text-xs text-base-content/70 w-4 shrink-0">${i}</span>
										<select name="s${i}" class="select select-xs select-bordered min-w-0 flex-1" aria-label="${ko ? "화폐" : "Currency"} ${i}">
											<option value="">${ko ? "(안 씀)" : "(none)"}</option>
											@for(var g : actGroups.entrySet())
												<optgroup label="${g.getKey()}">
													@for(String[] a : g.getValue())
														<option value="${a[1]}">${a[2]}</option>
													@endfor
												</optgroup>
											@endfor
										</select>
										<select name="u${i}" class="select select-xs select-bordered min-w-0" aria-label="${ko ? "언제까지" : "Until"} ${i}">
											<option value="">${ko ? "한 번" : "Once"}</option>
											<option value="all">${ko ? "목표 전부 맞을 때까지" : "Until all targets"}</option>
											<option value="pre">${ko ? "접두 목표 맞을 때까지" : "Until prefix targets"}</option>
											<option value="suf">${ko ? "접미 목표 맞을 때까지" : "Until suffix targets"}</option>
										</select>
										<input type="number" name="m${i}" min="1" max="20000" placeholder="${ko ? "최대 횟수" : "max uses"}" class="input input-xs input-bordered w-24" aria-label="${ko ? "최대 횟수" : "Max uses"} ${i}">
										<label class="flex items-center gap-1 text-xs"><input type="checkbox" name="g${i}" value="1" class="checkbox checkbox-xs">${ko ? "다음 줄과 묶음" : "Group with next"}</label>
									</div>
								@endfor
								<p class="text-xs text-base-content/70">${ko ? "묶음은 마지막 줄의 \"언제까지 · 최대\"로 통째 반복합니다. 최대를 비우면 5,000번." : "A group repeats as a whole using its last row's until / max. Empty max = 5,000."}</p>
							</div>
						</details>
					</form>
					@template.poe.htmx.craftCalc(calc = null, targetLabels = java.util.List.of())
					@template.poe.htmx.craftStrategy(result = null, presetLabel = "", targetLabels = java.util.List.of())
				</section>
			@endif

		</div>
	`
)
