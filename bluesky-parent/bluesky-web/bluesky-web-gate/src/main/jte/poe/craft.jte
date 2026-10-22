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
@param java.util.List<net.luversof.web.gate.poe.CraftPoolGroups.Family<PoeCraft.PoolLine>> poolGroups = java.util.List.of()
@param java.util.List<PoeCraft.Essence> essences = java.util.List.of()
@param java.util.List<PoeCraft.PoolLine> calcPool = java.util.List.of()
@param java.util.List<PoeCraft.VeiledLine> calcVeiled = java.util.List.of()
@param PoeCraft.CalcExtras calcExtras = null
@param PoeCraft.Options options = null
@param String rollsJson = "{}"
@param java.util.Map<String, Long> rolls = java.util.Map.of()
@param String importNote = null
@param boolean imported = false
@param java.util.List<net.luversof.web.gate.poe.dto.PoeBaseItem> bases = java.util.List.of()
@param String baseName = ""
@param String baseNameKo = ""
@param String itemName = ""
@param String itemNameKo = ""
@param net.luversof.web.gate.poe.dto.PoeBaseItem craftBase = null
@param java.util.List<String> craftEnchant = null
@param Integer passives = null
@param java.util.List<String[]> craftPriceList = java.util.List.of()

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

			<%-- 제작할 아이템 검색(10-21 C527, craftofexile 'Search for a craftable item') — 베이스 이름(영 · 한)으로 종류 · 변형 · 베이스를 한 번에 --%>
			<div class="max-w-xl space-y-1" data-craft-base-search>
				<input type="search" name="q" class="input input-sm input-bordered w-full" autocomplete="off" placeholder="${ko ? "제작할 아이템 검색 — 베이스 이름(예: 거신 건틀릿, Titan Gauntlets, 대형 스킬 군 주얼)" : "Search a craftable item — base name (e.g. Titan Gauntlets, Large Cluster Jewel)"}" aria-label="${ko ? "제작할 아이템 검색" : "Search a craftable item"}"
				       hx-get="/poe/htmx/craft/base-search" hx-trigger="input changed delay:300ms, search" hx-target="#craft-base-hits" hx-swap="innerHTML">
				<div id="craft-base-hits" aria-live="polite"></div>
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
						<span class="label-text text-xs">${"Jewel".equals(activeClass) || "AbyssJewel".equals(activeClass) ? (ko ? "주얼 종류" : "Jewel type") : (ko ? "속성 변형" : "Attribute variant")}</span>
						<select name="variant" class="select select-sm select-bordered">
							@for(PoeModItemClass.Variant v : active.variants())
								<option value="${v.key()}" selected="${v.key().equals(activeVariant)}">${ko && v.nameKo() != null ? v.nameKo() : v.name()}</option>
							@endfor
						</select>
					</label>
				@endif
				@if(net.luversof.web.gate.poe.controller.PoeCraftController.clusterRange(activeVariant) != null)
					<%-- 군집 주얼 패시브 수(10-21 C531) — 카드 인챈트 첫 줄 · 붙여넣기 · 공유 링크, 옵션 확률엔 영향 없음 --%>
					!{ int[] passRange = net.luversof.web.gate.poe.controller.PoeCraftController.clusterRange(activeVariant); }
					<label class="form-control min-w-0">
						<span class="label-text text-xs">${ko ? "패시브 수" : "Passive skills"}</span>
						<select name="passives" class="select select-sm select-bordered" data-craft-passives>
							@for(int pn = passRange[0]; pn <= passRange[1]; pn++)
								<option value="${pn}" selected="${passives != null && passives == pn}">${pn}</option>
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
				@if(!bases.isEmpty())
					<%-- 베이스(C487) — craftofexile 처럼 베이스로 고른다. 옵션 풀은 종류 · 변형이 같으면 같고, 카드 이름이 이 베이스 --%>
					<label class="form-control min-w-0 max-w-full">
						<span class="label-text text-xs">${ko ? "베이스" : "Base"}</span>
						<select name="base" class="select select-sm select-bordered max-w-full" data-craft-base-select>
							@for(var b : bases)
								<option value="${b.name()}" selected="${b.name().equals(baseName)}">${ko && b.nameKo() != null && !b.nameKo().isBlank() ? b.nameKo() : b.name()} (${ko ? "레벨 " : "lvl "}${b.dropLevel()})</option>
							@endfor
						</select>
					</label>
				@endif
				<label class="form-control w-28">
					<span class="label-text text-xs">${ko ? "아이템 레벨" : "Item level"}</span>
					<input type="number" name="ilvl" min="1" max="100" value="${ilvl}" class="input input-sm input-bordered">
				</label>
			</form>

			<%-- 게임 아이템 붙여넣기(10-20 C485, craftofexile 의 Import item) — 게임에서 Ctrl+C(상세 Ctrl+Alt+C)한 글. 이 화면 아무 데서나 Ctrl+V 해도 연다(common.ts) --%>
			<details class="rounded-box border border-base-300 bg-base-100 px-3 py-2" data-craft-import>
				<summary class="cursor-pointer text-sm font-medium">${ko ? "게임 아이템 붙여넣기" : "Paste an in-game item"} <span class="text-xs font-normal text-base-content/60">${ko ? "— 게임에서 아이템에 마우스를 올리고 Ctrl+C(옵션 이름 · 등급까지는 Ctrl+Alt+C), 이 화면에서 Ctrl+V · Path of Building 아이템 글 · 빌드 코드(빌드 아이템 중 고르기)도" : "— hover the item in game, press Ctrl+C (Ctrl+Alt+C for names and tiers), then Ctrl+V on this page · Path of Building item text or a build code (pick a build item) works too"}</span></summary>
				<form class="mt-2 space-y-2" hx-post="/poe/htmx/craft/import" hx-target="[data-craft-import-msg]" hx-swap="innerHTML" data-craft-import-form>
					<textarea name="text" rows="8" class="textarea textarea-bordered w-full font-mono text-xs" aria-label="${ko ? "게임에서 복사한 아이템 글" : "Item text copied from the game"}" placeholder="Item Class: Gloves&#10;Rarity: Rare&#10;Doom Grip&#10;Titan Gauntlets&#10;--------&#10;Item Level: 84&#10;--------&#10;+120 to maximum Life" data-craft-import-text></textarea>
					<div class="flex flex-wrap items-center gap-2">
						<button type="submit" class="btn btn-sm btn-primary" data-craft-import-submit>${ko ? "이 아이템으로 시작" : "Start from this item"}</button>
						<span class="min-w-0" data-craft-import-msg aria-live="polite"></span>
					</div>
				</form>
			</details>
			<%-- 보관함(10-20 C518, craftofexile beta 의 Inventory) — 이 브라우저에 담아 둔 아이템(공유 링크)을 다시 연다, 목록은 common.ts 가 그린다 --%>
			<details class="rounded-box border border-base-300 bg-base-100 px-3 py-2" data-craft-inventory data-inv-lang="${ko ? "ko" : "en"}">
				<summary class="cursor-pointer text-sm font-medium">${ko ? "보관함" : "Inventory"} <span class="text-xs font-normal text-base-content/60" data-craft-inv-count></span> <span class="text-xs font-normal text-base-content/60">${ko ? "— 저장한 아이템을 그대로 다시 엽니다(이 브라우저에만)" : "— reopen saved items as they were (this browser only)"}</span></summary>
				<div class="mt-2 space-y-1 min-w-0" data-craft-inv-list></div>
				<%-- 보관함 옮기기(10-21 C535) — 목록을 글로 복사해 다른 브라우저 · PC 의 이 칸에 붙여넣으면 합친다(같은 사이트 제작 화면 주소만) --%>
				<details class="mt-2 text-xs" data-craft-inv-move>
					<summary class="cursor-pointer select-none text-base-content/70">${ko ? "다른 브라우저로 옮기기" : "Move to another browser"}</summary>
					<div class="mt-1 space-y-1">
						<div class="flex flex-wrap items-center gap-2">
							<button type="button" class="btn btn-xs btn-ghost border border-base-300" data-craft-inv-export>${ko ? "목록 글로 복사" : "Copy list as text"}</button>
							<button type="button" class="btn btn-xs btn-ghost border border-base-300" data-craft-inv-import>${ko ? "붙여넣은 글 가져오기" : "Import pasted text"}</button>
							<span class="text-success" data-craft-inv-msg aria-live="polite"></span>
						</div>
						<textarea rows="3" class="textarea textarea-bordered textarea-xs w-full font-mono" aria-label="${ko ? "가져올 보관함 글" : "Inventory text to import"}" placeholder="${ko ? "다른 브라우저에서 '목록 글로 복사'한 글을 여기에" : "Paste text from 'Copy list as text' here"}" data-craft-inv-import-text></textarea>
					</div>
				</details>
			</details>
			@if(imported)
				<div role="status" class="alert ${importNote != null && !importNote.isBlank() ? "alert-warning" : "alert-success"} py-2 text-sm" data-craft-import-note>
					<span class="min-w-0 wrap-anywhere">${ko ? "게임 아이템을 가져왔습니다." : "Imported the in-game item."}@if(importNote != null && !importNote.isBlank()) ${ko ? "확인할 것: " : "Check: "}${importNote}@endif</span>
				</div>
			@endif

			@if(view != null)
				@template.poe.htmx.craftEmu(view = view, error = error, lastAction = lastAction, added = added, stateJson = stateJson, historyJson = historyJson, historySize = historySize, pool = pool, poolGroups = poolGroups, essences = essences, classLabel = classLabel, baseName = baseName, baseNameKo = baseNameKo, itemName = itemName, itemNameKo = itemNameKo, craftBase = craftBase, craftEnchant = craftEnchant, passives = passives, spamNote = null, options = options, imprintJson = "", foresee = "", preview = null, previewAction = null, previewArg = null, spentJson = "[]", spent = null, rollsJson = rollsJson, rolls = rolls, rollHistJson = "[]", imprintRollsJson = "{}", historyRows = java.util.List.of(), historyStartUndo = null, historyDropped = 0, recommend = null, optJob = null, ghosts = null, ghostSeeds = null, ghostAction = null, ghost = 0)
			@else
				<p class="text-sm text-base-content/60">${ko ? "제작 데이터를 불러오지 못했습니다." : "Crafting data is not available."}</p>
			@endif

			@if(!calcPool.isEmpty())
				<%-- 계산기(10-15 C336) — 계열마다 최소 등급을 고르면 방법별 기대 비용(가장 적은 비용 순). 목표는 일반 → 레어 기준 전체 풀에서 고른다 --%>
				<section id="craft-calc" class="space-y-3 border-t border-base-300 pt-4 scroll-mt-24" data-craft-calc data-no-success-toast>
					<h2 class="text-lg font-semibold">${ko ? "제작 계산기 — 가장 적은 비용" : "Crafting calculator — cheapest method"}</h2>
					<p class="text-sm text-base-content/60">${ko ? "목표 등급을 고르면 에센스 · 화석 · 수확 · 제작대(메타) · 짐승 · 화폐를 모두 섞어 지금 아이템에서 가장 싼 길을 찾습니다(같이 못 쓰는 조합은 제작 규칙대로 막음)." : "Pick target tiers — the cheapest path from the current item mixing essences, fossils, harvest, bench (meta), beasts and orbs (invalid combinations blocked by the crafting rules)."}</p>
					<%-- 한 화면(C458): 왼쪽 목표 · 시작 · 실행, 오른쪽 결과 --%>
					<%-- 가격 직접 입력(10-21 C529, craftofexile 'custom pricing') — 비우면 시세. 이 브라우저에 저장, 계산 · 쓴 화폐 · 최적 조합 비용에 쓴다(common.ts 가 요청마다 prices 로) --%>
					@if(!craftPriceList.isEmpty())
						<details class="rounded-box border border-base-300 bg-base-100 px-3 py-2" data-craft-prices>
							<summary class="cursor-pointer text-sm font-medium">${ko ? "가격 직접 입력" : "Custom prices"} <span class="text-xs font-normal text-base-content/60" data-craft-prices-count></span> <span class="text-xs font-normal text-base-content/60">${ko ? "— 카오스 기준, 비우면 시세(회색 글씨) · 이 브라우저에 저장" : "— in chaos, blank uses the market price (grey) · saved in this browser"}</span></summary>
							<div class="mt-2 grid gap-1 sm:grid-cols-2 lg:grid-cols-3 text-xs">
								@for(String[] pr : craftPriceList)
									<label class="flex items-center gap-2 min-w-0"><span class="min-w-0 flex-1 wrap-anywhere">${ko ? pr[1] : pr[0]}</span><input type="number" min="0" step="any" inputmode="decimal" class="input input-xs input-bordered w-24 shrink-0" placeholder="${pr[2]}" aria-label="${(ko ? pr[1] : pr[0]) + (ko ? " 가격(카오스)" : " price (chaos)")}" data-craft-price-name="${pr[0]}"></label>
								@endfor
							</div>
							<button type="button" class="btn btn-xs btn-ghost border border-base-300 mt-2" data-craft-prices-reset>${ko ? "시세로 되돌리기" : "Reset to market prices"}</button>
						</details>
					@endif
					<div class="grid gap-4 items-start lg:grid-cols-[minmax(0,26rem)_minmax(0,1fr)]" data-craft-calc-workspace>
					<form hx-post="/poe/htmx/craft/calc" hx-target="#craft-calc-result" hx-swap="outerHTML" hx-include="#craft-emu input[name=state]" hx-disabled-elt="find [data-craft-calc-run], find [data-craft-strategy-run]" hx-indicator="[data-craft-calc-busy]" class="space-y-2 min-w-0 lg:sticky lg:top-20 lg:max-h-[calc(100vh-6rem)] lg:flex lg:flex-col lg:overflow-y-auto lg:pr-1" data-craft-calc-form>
						<input type="hidden" name="itemClass" value="${activeClass}">
						<input type="hidden" name="variant" value="${activeVariant}">
						<input type="hidden" name="influence" value="${activeInfluence}">
						<input type="hidden" name="ilvl" value="${ilvl}">
						<%-- 목표 고르기(C458 — 한 화면): 검색 · 고른 줄은 맨 위로(공용 data-picked-list) · 칸 안 스크롤. 최소 등급 = 그 등급 이상이면 충족 --%>
						<%-- 최소 높이 16rem · 폼 칸 스크롤(10-21 C549 — 장막 · 탐광 · 엘드리치 칸을 펼치면 화면 높이 고정 칸에서 목표 목록이 0 높이로 눌려 글자가 겹쳤다) --%>
						<div class="space-y-1 lg:flex lg:flex-col lg:min-h-[16rem] lg:flex-1" data-craft-calc-targets>
							<input type="search" class="input input-sm input-bordered w-full" placeholder="${ko ? "옵션 검색 — 생명력, 저항, 공격 속도…" : "Search modifiers — life, resistance, attack speed…"}" data-filter-input="#craft-calc-mods [data-craft-calc-family]" aria-label="${ko ? "옵션 검색" : "Search modifiers"}" autocomplete="off">
							<div class="text-xs text-base-content/70">${ko ? "고른 목표 " : "Targets "}<span class="font-semibold" data-picked-count>0</span>${ko ? "개 · 고른 줄은 맨 위로 · 최소 등급 이상이면 충족" : " · picked rows move to the top · any tier at or above the minimum counts"}</div>
							<%-- 묶음(10-20 C522, craftofexile 계산기 '일부 맞춤') — 줄 끝을 체크한 옵션 중 고른 개수 이상이면 충족(예: 저항 넷 중 둘) --%>
							<div class="flex flex-wrap items-center gap-1 text-xs text-base-content/70" data-craft-calc-group title="${ko ? "묶음 옵션의 등급 칸을 고르면 그 등급 이상, 안 고르면 모든 등급 — 예: 저항 넷을 체크하고 2 = 저항 둘 이상" : "A group modifier counts at its picked tier or better, any tier if none — e.g. check four resistances and pick 2"}">
								<span>${ko ? "묶음: 줄 끝 체크한 옵션 중" : "Group: at least"}</span>
								<select name="t_grpk" class="select select-xs select-bordered h-7 min-h-0 w-14" aria-label="${ko ? "묶음에서 채울 개수" : "How many of the group"}">
									<option value="1">1</option>
									<option value="2">2</option>
									<option value="3">3</option>
									<option value="4">4</option>
									<option value="5">5</option>
									<option value="6">6</option>
								</select>
								<span>${ko ? "개 이상" : "of the checked rows"}</span>
							</div>
							<div id="craft-calc-mods" class="flex flex-col gap-0.5 max-h-[22rem] lg:max-h-none lg:flex-1 lg:min-h-0 overflow-y-auto rounded-box border border-base-300 p-1.5" data-picked-list>
								@for(String gen : java.util.List.of("prefix", "suffix"))
									!{
										java.util.Map<String, java.util.List<PoeCraft.PoolLine>> fams = new java.util.LinkedHashMap<>();
										for (PoeCraft.PoolLine p : calcPool) if (gen.equals(p.gen())) fams.computeIfAbsent(p.family(), k -> new java.util.ArrayList<>()).add(p);
									}
									@for(var fe : fams.entrySet())
										!{
											java.util.List<PoeCraft.PoolLine> tiers = fe.getValue().stream().sorted(java.util.Comparator.comparing(p -> p.tierRank() == null ? 99 : p.tierRank())).toList();
											PoeCraft.PoolLine top = tiers.get(0);
											java.util.List<String> mx = ko && top.ko() != null && !top.ko().isEmpty() ? top.ko() : top.en();
											java.util.List<String> mn = ko && top.koMin() != null && !top.koMin().isEmpty() ? top.koMin() : top.enMin();
										}
										<label class="flex items-center gap-2 text-xs min-w-0 rounded px-1 py-0.5" data-craft-calc-family="${gen}:${fe.getKey()}" data-craft-calc-ids="${String.join(" ", tiers.stream().filter(t -> t.tierRank() != null).map(t -> t.id() + "=" + t.tierRank()).toList())}" data-picked-row>
											<span class="text-[10px] font-mono text-base-content/60 w-6 shrink-0">${"prefix".equals(gen) ? (ko ? "접두" : "P") : (ko ? "접미" : "S")}</span>
											<select name="t_${gen}:${fe.getKey()}" class="select select-xs select-bordered w-24 shrink-0 h-7 min-h-0" aria-label="${ko ? "최소 등급" : "Minimum tier"}">
												<option value="">-</option>
												@for(PoeCraft.PoolLine t : tiers)
													@if(t.tierRank() != null)<option value="${t.tierRank()}">T1@if(t.tierRank() > 1)~T${t.tierRank()}@endif</option>@endif
												@endfor
											</select>
											<span class="min-w-0 wrap-anywhere">@for(int i = 0; i < mx.size(); i++)$unsafe{PoeText.highlightValues(net.luversof.web.gate.poe.PoeModRange.range(mn != null && i < mn.size() ? mn.get(i) : null, mx.get(i)))}@if(i < mx.size() - 1), @endif@endfor@template.poe.htmx.craftCalcRowExtra(top = top, tiers = tiers, key = gen + ":" + fe.getKey(), extras = calcExtras, infName = active == null || active.influences() == null ? activeInfluence : active.influences().stream().filter(x -> x.key().equals(activeInfluence)).map(x -> ko && x.nameKo() != null ? x.nameKo() : x.name()).findFirst().orElse(activeInfluence))</span>
											<input type="checkbox" name="t_grp:${gen}:${fe.getKey()}" value="1" class="checkbox checkbox-xs ml-auto shrink-0" title="${ko ? "묶음에 넣기 — 위 '묶음'에서 고른 개수 이상 채우면 충족(등급을 고르면 그 등급 이상)" : "Add to the group — met when at least the chosen number of the group is hit (picked tier or better)"}" aria-label="${ko ? "묶음에 넣기" : "Add to group"}" data-craft-calc-grp>
										</label>
									@endfor
								@endfor
							</div>
						</div>
						<%-- 정복자 · 쉐이퍼 · 엘더 바로가기 · 제작대 안내 · 바알 타락 임플리싯(10-21 C548) — 조각(craft.jte 생성 메서드 한도) --%>
						@template.poe.htmx.craftCalcMore(extras = calcExtras, influences = active == null || active.influences() == null ? java.util.List.of() : active.influences(), part = "top", activeInfluence = activeInfluence, infFamilies = calcExtras == null || calcExtras.influenceIds() == null ? 0 : calcPool.stream().filter(p -> calcExtras.influenceIds().contains(p.id())).map(p -> p.gen() + ":" + p.family()).distinct().count())
						<%-- 빈 자리 남기기(10-20 C519, craftofexile 계산기 'open affix') — 목표 옵션을 채운 뒤에도 그 쪽에 남아야 할 빈 자리(제작대 옵션 · 다음 제작용).
						     방법 비교 표 · 적중 확률 · 연타 · 전략 모의 · 최적 조합(C520) 모두 반영 --%>
						<div class="flex flex-wrap items-center gap-2 text-xs" data-craft-calc-open>
							<span class="text-base-content/70" title="${ko ? "목표 옵션을 다 채운 뒤에도 남아 있어야 할 빈 자리 — 제작대 옵션 · 다음 제작을 위해(제작대 옵션도 자리를 차지)" : "Open slots that must remain after the targets are met — for a bench craft or the next step (bench mods take a slot too)"}">${ko ? "빈 자리 남기기" : "Keep open"}</span>
							@for(String side : java.util.List.of("prefix", "suffix"))
								<label class="flex items-center gap-1">${"prefix".equals(side) ? (ko ? "접두" : "Prefix") : (ko ? "접미" : "Suffix")}
									<select name="t_open:${side}" class="select select-xs select-bordered h-7 min-h-0 w-16" aria-label="${"prefix".equals(side) ? (ko ? "남길 빈 접두 자리" : "Open prefixes to keep") : (ko ? "남길 빈 접미 자리" : "Open suffixes to keep")}">
										<option value="">-</option>
										<option value="1">1</option>
										<option value="2">2</option>
										<option value="3">3</option>
									</select>
								</label>
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
						<%-- 탐광 · 엘드리치 목표(10-21 C549) — 조각 --%>
						@template.poe.htmx.craftCalcTargetsMore(extras = calcExtras)
						<div class="flex flex-wrap items-center gap-2" data-craft-calc-run-row>
							<label class="flex items-center gap-1 text-xs" title="${ko ? "최적 조합은 지금 아이템 그대로(이미 맞은 목표 · 옵션을 살린 채)에서, 아래 방법별 비교 표는 분열 옵션과 엘드리치 임플리싯만 남기고 계산합니다(C494 — 붙여넣은 게임 아이템을 마저 완성하는 비용)" : "The optimal plan starts from the current item as is (keeping matched targets); the method table keeps only fractured modifiers and eldritch implicits"}"><input type="checkbox" name="fromState" value="1" class="checkbox checkbox-xs" data-craft-from-state checked>${ko ? "에뮬레이터의 지금 아이템에서 시작" : "Start from the emulator item"}</label>
							<button type="submit" class="btn btn-sm btn-primary" data-craft-calc-run>${ko ? "최소 비용 찾기" : "Find the cheapest way"}</button>
							<span class="htmx-indicator text-xs text-base-content/70" data-craft-calc-busy role="status"><span class="loading loading-spinner loading-xs align-middle"></span> ${ko ? "계산 중(최대 10초)…" : "Calculating (up to 10 s)…"}</span>
						</div>
						<%-- 최적 조합은 표와 동시에 시작(C458 — 표가 끝나길 기다리면 6~9초 늦었다). 백그라운드 작업이라 게이트 읽기 제한과 무관 --%>
						<div hidden hx-post="/poe/htmx/craft/optimize" hx-trigger="submit from:closest form" hx-include="closest form, #craft-emu input[name=state]" hx-target="#craft-opt-result" hx-swap="outerHTML" data-craft-optimize-trigger></div>
						<details class="rounded-box border border-base-300 bg-base-100" data-craft-calc-advanced>
							<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "고급 — 에센스 하나 비교 · 다단계 전략 직접 모의" : "Advanced — compare one essence · simulate your own strategy"}</summary>
							<div class="px-3 pb-3 space-y-3">
								@if(!essences.isEmpty())
								<%-- 좁은 폭에선 한 줄 전체(C422 UX QA — 375px 한국어에서 옆 체크 문구에 밀려 62px 로 눌렸다) --%>
								<div class="basis-full space-y-1" data-craft-essence>
									<%-- 종류 아이콘 · 등급 단추로 고르기(10-21 C547 — 105개 셀렉트), '비교 안 함' 단추 --%>
									@template.poe.htmx.craftEssencePicker(essences = essences, selectLabel = ko ? "비교할 에센스" : "Essence to compare", noneLabel = ko ? "에센스 비교 안 함" : "No essence")
								</div>
							@endif
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
								for (PoeCraft.Fossil f : options.fossils()) acts.add(new String[] {ko ? "화석(하나)" : "Fossil (one)", "FOSSIL:" + f.slug(), ko ? f.nameKo() : f.name()});
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
							</div>
						</details>
					</form>
					<div class="min-w-0 space-y-3 lg:sticky lg:top-20 lg:max-h-[calc(100vh-6rem)] lg:overflow-y-auto lg:pr-1" data-craft-calc-results>
						<div id="craft-opt-result" class="rounded-box border border-dashed border-base-300 p-4 text-sm text-base-content/70" data-craft-optimize-empty>${ko ? "목표를 고르고 '최소 비용 찾기'를 누르면 여기에 가장 싼 제작 흐름(상태마다 쓸 수단)이, 아래에 방법별 비교 표가 나옵니다." : "Pick targets and press 'Find the cheapest way' — the cheapest crafting flow appears here and the per-method table below."}</div>
						@template.poe.htmx.craftCalc(calc = null, targetLabels = java.util.List.of(), ghost = 0)
						@template.poe.htmx.craftStrategy(result = null, presetLabel = "", targetLabels = java.util.List.of())
					</div>
					</div>
					<%-- 바알 타락 임플리싯(10-21 C548) — 작업 영역 아래 전체 폭(폼은 화면 높이 고정 칸) --%>
					@template.poe.htmx.craftCalcMore(extras = calcExtras, part = "corrupted")
				</section>
			@endif

		</div>
	`
)
