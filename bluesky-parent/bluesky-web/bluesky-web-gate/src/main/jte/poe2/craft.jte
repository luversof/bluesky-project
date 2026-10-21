@import java.util.List
@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe2.dto.Poe2Craft
@import net.luversof.web.gate.poe.PoeText

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param Poe2.Meta meta = null
@param List<Poe2.ModPoolSummary> pools = java.util.List.of()
@param String activeKey = null
@param int ilvl = 82
@param java.util.List<Poe2.BaseItem> bases = java.util.List.of()
@param String baseName = ""
@param String baseNameKo = ""
@param Poe2.BaseItem craftBase = null
@param java.util.List<String[]> craftPriceList = java.util.List.of()
@param String itemName = ""
@param String itemNameKo = ""
@param Poe2Craft.View view = null
@param String error = null
@param String lastAction = null
@param java.util.List<String> added = java.util.List.of()
@param String stateJson = "{}"
@param String historyJson = "[]"
@param int historySize = 0
@param java.util.List<Poe2Craft.PoolLine> pool = java.util.List.of()
@param java.util.List<net.luversof.web.gate.poe.CraftPoolGroups.Family<Poe2Craft.PoolLine>> poolGroups = java.util.List.of()
@param java.util.List<Poe2Craft.Omen> omens = java.util.List.of()
@param java.util.List<String> activeOmens = java.util.List.of()
@param String tier = ""
@param String classLabel = ""
@param java.util.List<Poe2Craft.Currency> currencies = java.util.List.of()
@param java.util.List<Poe2Craft.PoolLine> calcPool = java.util.List.of()
@param java.util.List<Poe2Craft.PoolLine> calcDesecrated = java.util.List.of()
@param java.util.List<Poe2Craft.EssenceOption> essences = java.util.List.of()
@param java.util.List<Poe2Craft.Bone> bones = java.util.List.of()
@param String stashJson = ""
@param String stashRollsJson = "{}"
@param Poe2Craft.View stashView = null
@param String rollsJson = "{}"
@param java.util.Map<String, Long> rolls = java.util.Map.of()
@param String importNote = null
@param boolean imported = false

<%-- PoE2 제작 에뮬레이터(10-15 C351 — 사용자 요청 "제작 에뮬레이터 · 시뮬레이터", P5). 풀(클래스 · 방어 속성) · 아이템 레벨을 고르면 새 일반 아이템,
     화폐(등급) · 징조를 써 본다. 옵션 가중치는 게임 데이터에 없어(GGG 비공개) 균등으로 굴린다 — 화면이 밝힌다. --%>
!{
	boolean ko = PoeText.isKorean();
	String pageTitle = ko ? "제작 에뮬레이터" : "Crafting Emulator";
	java.util.Map<String, List<Poe2.ModPoolSummary>> byClass = new java.util.LinkedHashMap<>();
	for (String cat : List.of("weapon", "offhand", "armour", "jewellery", "jewel", "flask", "other")) {
		for (Poe2.ModPoolSummary p : pools) {
			String c = p.category() == null ? "other" : p.category();
			if (c.equals(cat)) byClass.computeIfAbsent(ko && p.itemClassKo() != null ? p.itemClassKo() : p.itemClass(), k -> new java.util.ArrayList<>()).add(p);
		}
	}
}

@template._layout.poeLayout(
	title = pageTitle,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	menu = "poe2",
	contentPoe = @`
		<div class="space-y-4" data-craft2-page>
			@template.poe2.header(title = pageTitle, meta = meta)
			<div class="text-sm text-base-content/70 -mt-2">${ko ? "화폐와 징조를 하나씩 써 보며 결과와 다음 옵션을 봅니다. PoE2 옵션 가중치는 게임 데이터에 없어(비공개) 붙을 수 있는 옵션 중 균등으로 굴립니다." : "Apply currency and omens one step at a time. PoE2 modifier weights are not in the game data (hidden), so modifiers are rolled uniformly among those that can appear."} <a href="#craft2-calc" class="link link-primary" data-craft2-calc-jump>${ko ? "→ 제작 시뮬레이터(가장 적은 비용) · 다단계 전략" : "→ Crafting simulator (cheapest) · multi-step strategy"}</a></div>

			<%-- 제작할 아이템 검색(10-21 C528, PoE1 C527 짝) — 베이스 이름(영 · 한)으로 풀 · 베이스를 한 번에 --%>
			<div class="max-w-xl space-y-1" data-craft-base-search>
				<input type="search" name="q" class="input input-sm input-bordered w-full" autocomplete="off" placeholder="${ko ? "제작할 아이템 검색 — 베이스 이름(예: 다부진 미트, Stocky Mitts)" : "Search a craftable item — base name (e.g. Stocky Mitts)"}" aria-label="${ko ? "제작할 아이템 검색" : "Search a craftable item"}"
				       hx-get="/poe2/htmx/craft/base-search" hx-trigger="input changed delay:300ms, search" hx-target="#craft2-base-hits" hx-swap="innerHTML">
				<div id="craft2-base-hits" aria-live="polite"></div>
			</div>

			<%-- 바꾸면 만들던 아이템 · 되돌리기 이력이 초기화된다 — 이력이 있으면 확인, 취소하면 되돌림(C442 UX QA) --%>
			<form method="get" action="/poe2/craft" class="flex flex-wrap items-end gap-3" data-craft2-picker data-confirm-dirty="#craft2-emu input[name=history]" data-confirm-text="${ko ? "아이템 종류나 아이템 레벨을 바꾸면 지금 만들던 아이템과 되돌리기 이력이 초기화됩니다(보관한 재조합 재료는 남음). 계속할까요?" : "Changing the item class or item level resets the item you are crafting and its undo history (stored recombinator item stays). Continue?"}"
			      hx-get="/poe2/craft" hx-trigger="change" hx-target="[data-craft2-page]" hx-select="[data-craft2-page]" hx-swap="outerHTML" hx-push-url="true" hx-include="#craft2-emu input[name=stash], #craft2-emu input[name=stashRolls]">
				<label class="form-control min-w-0">
					<span class="label-text text-xs">${ko ? "아이템 종류" : "Item class"}</span>
					<select name="pool" class="select select-sm select-bordered min-w-0">
						@for(var e : byClass.entrySet())
							<optgroup label="${e.getKey()}">
								@for(Poe2.ModPoolSummary p : e.getValue())
									<option value="${p.key()}" selected="${p.key().equals(activeKey)}">${e.getKey()}@if(p.variant() != null && !p.variant().isEmpty()) · ${ko && p.variantKo() != null ? p.variantKo() : p.variant()}@endif</option>
								@endfor
							</optgroup>
						@endfor
					</select>
				</label>
				@if(!bases.isEmpty())
					<%-- 베이스(C493) — 옵션 풀은 같고 카드 이름이 이 베이스 --%>
					<label class="form-control min-w-0 max-w-full">
						<span class="label-text text-xs">${ko ? "베이스" : "Base"}</span>
						<select name="base" class="select select-sm select-bordered max-w-full" data-craft-base-select>
							@for(var b : bases)
								<option value="${b.name()}" selected="${b.name().equals(baseName)}">${ko && b.nameKo() != null && !b.nameKo().isBlank() ? b.nameKo() : b.name()}${b.dropLevel() == null ? "" : (ko ? " (레벨 " : " (lvl ") + b.dropLevel() + ")"}</option>
							@endfor
						</select>
					</label>
				@endif
				<label class="form-control w-28">
					<span class="label-text text-xs">${ko ? "아이템 레벨" : "Item level"}</span>
					<input type="number" name="ilvl" min="1" max="100" value="${ilvl}" class="input input-sm input-bordered">
				</label>
			</form>

			<%-- 게임 아이템 붙여넣기(10-20 C486, PoE1 C485 와 같은 칸) — 이 화면 아무 데서나 Ctrl+V 해도 연다(common.ts) --%>
			<details class="rounded-box border border-base-300 bg-base-100 px-3 py-2" data-craft-import>
				<summary class="cursor-pointer text-sm font-medium">${ko ? "게임 아이템 붙여넣기" : "Paste an in-game item"} <span class="text-xs font-normal text-base-content/60">${ko ? "— 게임에서 아이템에 마우스를 올리고 Ctrl+C(옵션 이름 · 등급까지는 Ctrl+Alt+C), 이 화면에서 Ctrl+V · Path of Building 아이템 글 · 빌드 코드(빌드 아이템 중 고르기)도" : "— hover the item in game, press Ctrl+C (Ctrl+Alt+C for names and tiers), then Ctrl+V on this page · Path of Building item text or a build code (pick a build item) works too"}</span></summary>
				<form class="mt-2 space-y-2" hx-post="/poe2/htmx/craft/import" hx-target="[data-craft-import-msg]" hx-swap="innerHTML" data-craft-import-form>
					<textarea name="text" rows="8" class="textarea textarea-bordered w-full font-mono text-xs" aria-label="${ko ? "게임에서 복사한 아이템 글" : "Item text copied from the game"}" placeholder="Item Class: Gloves&#10;Rarity: Rare&#10;Grim Grasp&#10;Stocky Mitts&#10;--------&#10;Item Level: 80&#10;--------&#10;+110 to maximum Life" data-craft-import-text></textarea>
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
				@template.poe2.htmx.craftEmu(view = view, error = error, lastAction = lastAction, added = added, stateJson = stateJson, historyJson = historyJson, historySize = historySize, pool = pool, poolGroups = poolGroups, omens = omens, activeOmens = activeOmens, tier = tier, classLabel = classLabel, baseName = baseName, baseNameKo = baseNameKo, craftBase = craftBase, itemName = itemName, itemNameKo = itemNameKo, spamNote = null, currencies = currencies, essences = essences, bones = bones, selEssence = "", selBone = "", foresee = "", preview = null, previewAction = null, previewArg = null, previewOmens = null, spentJson = "[]", spent = null, historyRows = java.util.List.of(), historyStartUndo = null, historyDropped = 0, rollsJson = rollsJson, rolls = rolls, rollHistJson = "[]", stashJson = stashJson, stashRollsJson = stashRollsJson, stashView = stashView, recombChance = "", recombLSel = java.util.List.of(), recombRSel = java.util.List.of(), stashRolls = java.util.Map.of(), recommend = null, optJob = null)
			@else
				<p class="text-sm text-base-content/70">${ko ? "제작 데이터를 불러오지 못했습니다." : "Crafting data is not available."}</p>
			@endif

			@if(!calcPool.isEmpty())
				<%-- 시뮬레이터(10-15 C352) — 계열마다 최소 등급을 고르면 방법별(카오스 반복 · 엑잘 쌓기 · 연금 연타) 성공률 · 성공 한 번당 비용(poe.ninja 시세, 디바인) --%>
				!{
					java.util.Map<String, java.util.List<Poe2Craft.PoolLine>> fams = new java.util.LinkedHashMap<>();
					for (Poe2Craft.PoolLine p : calcPool) if (p.family() != null) fams.computeIfAbsent(p.gen() + ":" + p.family(), k -> new java.util.ArrayList<>()).add(p);
				}
				<section id="craft2-calc" class="space-y-3 border-t border-base-300 pt-4 scroll-mt-24" data-craft2-calc data-no-success-toast>
					<h2 class="text-lg font-semibold">${ko ? "제작 시뮬레이터 — 가장 적은 비용" : "Crafting simulator — cheapest method"}</h2>
					<p class="text-sm text-base-content/70">${ko ? "목표 등급을 고르면 화폐 · 등급 · 징조 · 에센스 · 훼손 · 분열 · 새 베이스를 모두 섞어 지금 아이템에서 가장 싼 길을 찾습니다(같이 못 쓰는 조합은 제작 규칙대로 막음, 베이스 값 제외, 옵션 가중치는 비공개라 균등 가정)." : "Pick target tiers — the cheapest path from the current item mixing orbs, tiers, omens, essences, desecration, fracturing and new bases (invalid combinations blocked by the crafting rules; base price excluded; weights assumed uniform)."}</p>
					<%-- 가격 직접 입력(10-21 C530, PoE1 C529 짝) — 디바인 기준, 비우면 시세. 이 브라우저에 저장 --%>
					@if(!craftPriceList.isEmpty())
						<details class="rounded-box border border-base-300 bg-base-100 px-3 py-2" data-craft-prices>
							<summary class="cursor-pointer text-sm font-medium">${ko ? "가격 직접 입력" : "Custom prices"} <span class="text-xs font-normal text-base-content/60" data-craft-prices-count></span> <span class="text-xs font-normal text-base-content/60">${ko ? "— 디바인 기준, 비우면 시세(회색 글씨) · 이 브라우저에 저장" : "— in divine, blank uses the market price (grey) · saved in this browser"}</span></summary>
							<div class="mt-2 grid gap-1 sm:grid-cols-2 lg:grid-cols-3 text-xs">
								@for(String[] pr : craftPriceList)
									<label class="flex items-center gap-2 min-w-0"><span class="min-w-0 flex-1 wrap-anywhere">${ko ? pr[1] : pr[0]}</span><input type="number" min="0" step="any" inputmode="decimal" class="input input-xs input-bordered w-24 shrink-0" placeholder="${pr[2]}" aria-label="${(ko ? pr[1] : pr[0]) + (ko ? " 가격(디바인)" : " price (divine)")}" data-craft-price-name="${pr[0]}"></label>
								@endfor
							</div>
							<button type="button" class="btn btn-xs btn-ghost border border-base-300 mt-2" data-craft-prices-reset>${ko ? "시세로 되돌리기" : "Reset to market prices"}</button>
						</details>
					@endif
					<div class="grid gap-4 items-start lg:grid-cols-[minmax(0,26rem)_minmax(0,1fr)]" data-craft2-calc-workspace>
					<form hx-post="/poe2/htmx/craft/calc" hx-target="#craft2-calc-result" hx-swap="outerHTML" hx-include="#craft2-emu input[name=state]" hx-disabled-elt="find [data-craft2-calc-run], find [data-craft2-strategy-run]" hx-indicator="[data-craft2-calc-busy]" class="space-y-2 min-w-0 lg:sticky lg:top-20 lg:max-h-[calc(100vh-6rem)] lg:flex lg:flex-col" data-craft2-calc-form>
						<input type="hidden" name="poolKey" value="${activeKey}">
						<input type="hidden" name="ilvl" value="${ilvl}">
						<%-- 목표 고르기(C461 — PoE1 C458 과 같은 한 화면): 검색 · 고른 줄 맨 위로 · 칸 안 스크롤 --%>
						<div class="space-y-1 lg:flex lg:flex-col lg:min-h-0 lg:flex-1" data-craft2-calc-targets>
							<input type="search" class="input input-sm input-bordered w-full" placeholder="${ko ? "옵션 검색 — 생명력, 저항, 공격 속도…" : "Search modifiers — life, resistance, attack speed…"}" data-filter-input="#craft2-calc-mods [data-craft2-calc-family]" aria-label="${ko ? "옵션 검색" : "Search modifiers"}" autocomplete="off">
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
							<div id="craft2-calc-mods" class="flex flex-col gap-0.5 max-h-[22rem] lg:max-h-none lg:flex-1 lg:min-h-0 overflow-y-auto rounded-box border border-base-300 p-1.5" data-picked-list>
								@for(var fe : fams.entrySet())
									!{
										java.util.List<Poe2Craft.PoolLine> tiers = fe.getValue().stream().filter(t -> t.tierRank() != null).sorted(java.util.Comparator.comparing(Poe2Craft.PoolLine::tierRank)).toList();
										Poe2Craft.PoolLine top = tiers.isEmpty() ? fe.getValue().get(0) : tiers.get(0);
										java.util.List<String> tx = ko && top.ko() != null && !top.ko().isEmpty() ? top.ko() : top.en();
										boolean pre = fe.getKey().startsWith("prefix:");
									}
									<label class="flex items-center gap-2 text-xs min-w-0 rounded px-1 py-0.5" data-craft2-calc-family="${fe.getKey()}" data-craft-calc-ids="${String.join(" ", tiers.stream().map(t -> t.id() + "=" + t.tierRank()).toList())}" data-picked-row>
										<span class="text-[10px] font-mono text-base-content/60 w-6 shrink-0">${pre ? (ko ? "접두" : "P") : (ko ? "접미" : "S")}</span>
										<select name="t_${fe.getKey()}" class="select select-xs select-bordered w-24 shrink-0 h-7 min-h-0" aria-label="${ko ? "최소 등급" : "Minimum tier"}">
											<option value="">-</option>
											@for(Poe2Craft.PoolLine t : tiers)
												<option value="${t.tierRank()}">T1@if(t.tierRank() > 1)~T${t.tierRank()}@endif</option>
											@endfor
										</select>
										<span class="min-w-0 wrap-anywhere">@for(int i = 0; i < tx.size(); i++)$unsafe{PoeText.highlightValues(tx.get(i))}@if(i < tx.size() - 1), @endif@endfor@if((ko ? top.tagsKo() : top.tags()) != null && !(ko ? top.tagsKo() : top.tags()).isEmpty())<span class="text-[10px] text-base-content/60" data-craft-calc-tags> — ${String.join(", ", ko ? top.tagsKo() : top.tags())}</span>@endif</span>
										<input type="checkbox" name="t_grp:${fe.getKey()}" value="1" class="checkbox checkbox-xs ml-auto shrink-0" title="${ko ? "묶음에 넣기 — 위 '묶음'에서 고른 개수 이상 채우면 충족(등급을 고르면 그 등급 이상)" : "Add to the group — met when at least the chosen number of the group is hit (picked tier or better)"}" aria-label="${ko ? "묶음에 넣기" : "Add to group"}" data-craft-calc-grp>
									</label>
								@endfor
							</div>
						</div>
						<%-- 빈 자리 남기기(10-20 C521, PoE1 C519 짝 · craftofexile 계산기 'open affix') — 목표 옵션을 채운 뒤에도 그 쪽에 남아야 할 빈 자리.
						     방법 비교 · 적중 확률 · 연타 · 전략 모의 · 최적 조합 반영(반쪽 재조합은 두 반쪽을 곱해 합치는 계산이라 그땐 뺀다) --%>
						<div class="flex flex-wrap items-center gap-2 text-xs" data-craft-calc-open>
							<span class="text-base-content/70" title="${ko ? "목표 옵션을 다 채운 뒤에도 남아 있어야 할 빈 자리 — 다음 제작을 위해(미공개 훼손 옵션도 자리를 차지)" : "Open slots that must remain after the targets are met — for the next step (unrevealed desecrated mods take a slot too)"}">${ko ? "빈 자리 남기기" : "Keep open"}</span>
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
						@if(!calcDesecrated.isEmpty())
							<%-- 훼손된 속성 목표(C420) — 체크하면 '연금 → 훼손 → 드러내기(목표 먼저)' 방법(+ 심연의 메아리 · 군주 징조)도 비교 --%>
							<details class="rounded-box border border-base-300 bg-base-100" data-craft2-calc-dese>
								<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "훼손된 속성 목표" : "Desecrated modifier targets"} <span class="text-xs font-normal text-base-content/70">· ${calcDesecrated.size()}${ko ? "종 · 고르면 뼈 훼손 → 드러내기 방법도 비교" : " · picking one adds bone desecration → reveal methods"}</span></summary>
								<div class="px-3 pb-3 grid gap-1 md:grid-cols-2">
									<%-- 접두 먼저(C422 UX QA — 두 칸에 행 순서로 채워 접두 · 접미가 번갈아 섞였다) --%>
									@for(Poe2Craft.PoolLine d : calcDesecrated.stream().sorted(java.util.Comparator.comparing((Poe2Craft.PoolLine x) -> "prefix".equals(x.gen()) ? 0 : 1)).toList())
										!{ java.util.List<String> dtx = ko && d.ko() != null && !d.ko().isEmpty() ? d.ko() : d.en(); String lichKo = d.family() == null ? null : switch (d.family()) { case "ulaman" -> ko ? "울라만" : "Ulaman"; case "amanamu" -> ko ? "아마나무" : "Amanamu"; case "kurgal" -> ko ? "쿠르갈" : "Kurgal"; default -> d.family(); }; }
										<label class="flex items-start gap-2 text-xs min-w-0" data-craft2-calc-dese-mod="${d.id()}">
											<input type="checkbox" name="t_dese:${d.id()}" value="1" class="checkbox checkbox-xs mt-0.5">
											<span class="min-w-0 wrap-anywhere">${"prefix".equals(d.gen()) ? (ko ? "접두 " : "P ") : (ko ? "접미 " : "S ")}${String.join(", ", dtx == null ? java.util.List.of() : dtx)}@if(lichKo != null) <span class="text-base-content/60">(${lichKo})</span>@endif</span>
										</label>
									@endfor
								</div>
							</details>
						@endif
						<div class="flex flex-wrap items-center gap-2" data-craft2-calc-run-row>
						<%-- 지금 아이템에서 시작(C357) — 에뮬레이터 아이템 그대로(분열 베이스 등): 카오스 반복 · 전략만, 새 베이스가 필요한 방법은 빠진다 --%>
						<label class="flex items-center gap-1 text-xs" title="${ko ? "에뮬레이터의 지금 아이템에서 시작합니다(직접 편집으로 만든 분열 베이스 등). 카오스 반복 · 다단계 전략에 쓰이고, 새 베이스가 필요한 방법은 빠집니다" : "Start from the current emulator item (e.g. a fractured base built with direct edit). Applies to chaos spam and strategies; base-requiring methods are skipped"}"><input type="checkbox" name="fromState" value="1" class="checkbox checkbox-xs" data-craft2-from-state checked>${ko ? "에뮬레이터의 지금 아이템에서 시작" : "Start from the emulator item"}</label>
						<button type="submit" class="btn btn-sm btn-primary" data-craft2-calc-run>${ko ? "최소 비용 찾기" : "Find the cheapest way"}</button>
						<span class="htmx-indicator text-xs text-base-content/70" data-craft2-calc-busy role="status"><span class="loading loading-spinner loading-xs align-middle"></span> ${ko ? "시뮬레이션 중(최대 10초)…" : "Simulating (up to 10 s)…"}</span>
						</div>
						<%-- 최적 조합은 표와 동시에 시작(C461) — 백그라운드 작업 --%>
						<div hidden hx-post="/poe2/htmx/craft/optimize" hx-trigger="submit from:closest form" hx-include="closest form, #craft2-emu input[name=state]" hx-target="#craft2-opt-result" hx-swap="outerHTML" data-craft2-optimize-trigger></div>
						<details class="rounded-box border border-base-300 bg-base-100" data-craft2-calc-advanced>
							<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "고급 — 반쪽 재조합 확률 · 다단계 전략 직접 모의" : "Advanced — half recombination chance · simulate your own strategy"}</summary>
							<div class="px-3 pb-3 space-y-3">
						<%-- 반쪽 재조합(C409) — 재조합 성공 확률(인게임 화면 값)을 넣으면 목표를 접두 · 접미로 나눠 따로 만든 뒤 합치는 방법도 비교 --%>
						<label class="flex flex-wrap items-center gap-1 text-xs min-w-0" title="${ko ? "재조합기 성공 확률은 고른 속성에 따라 정해지지만 공식이 공개되지 않아, 인게임 재조합기에서 목표 속성을 골랐을 때 뜨는 값을 넣습니다. 비우면 이 방법은 비교하지 않습니다" : "The Recombinator chance depends on the selected modifiers but its formula is unpublished — enter the value the in-game Recombinator shows for the target modifiers. Leave empty to skip"}"><span class="min-w-0 wrap-anywhere">${ko ? "반쪽 재조합도 비교 · 최적 조합에도 재조합기 — 재조합 성공 확률(인게임 값, 선택)" : "Also use the Recombinator (table and optimal plan) — Recombinator chance (in-game, optional)"}</span> <input type="number" name="recombChance" min="1" max="100" step="1" class="input input-xs input-bordered w-20" data-craft2-recomb-chance>%</label>
						<%-- 다단계 전략(C355) — 같은 목표로 직접 엮은 단계를 모의(화폐 · 에센스 · 뼈 · 드러내기 + 단계마다 징조 하나) --%>
						!{
							java.util.List<String[]> acts2 = new java.util.ArrayList<>();
							for (Poe2Craft.Currency c : currencies) if (!"CATALYST".equals(c.action())) acts2.add(new String[] {ko ? "화폐" : "Currency", c.action() + (c.tier() == null || c.tier().isEmpty() ? "" : ":" + c.tier()), ko ? c.nameKo() : c.name()});
							// 기폭제(C389 검토) — 이 베이스에 맞는 종류만, 한 단계 = 20%까지(쓴 개수만큼 비용)
							String ak2 = activeKey == null ? "" : activeKey;
							for (Poe2Craft.Currency c : currencies) if ("CATALYST".equals(c.action()) && c.tier() != null && (c.tier().startsWith("jewel:") ? ak2.startsWith("Jewel|") : (ak2.startsWith("Ring|") || ak2.startsWith("Amulet|")))) acts2.add(new String[] {ko ? "기폭제" : "Catalyst", "CATALYST:" + c.tier() + "*20", (ko ? c.nameKo() + " — 20%까지" : c.name() + " — to 20%")});
							for (Poe2Craft.EssenceOption e : essences) acts2.add(new String[] {ko ? "에센스" : "Essence", "ESSENCE:" + e.slug(), ko ? e.nameKo() : e.name()});
							for (Poe2Craft.Bone b : bones) acts2.add(new String[] {ko ? "훼손" : "Desecration", "DESECRATE:" + b.slug(), ko ? b.nameKo() : b.name()});
							acts2.add(new String[] {ko ? "훼손" : "Desecration", "REVEAL", ko ? "드러내기(목표에 맞는 것 먼저)" : "Reveal (target first)"});
							java.util.Map<String, java.util.List<String[]>> actGroups2 = new java.util.LinkedHashMap<>();
							for (String[] a : acts2) actGroups2.computeIfAbsent(a[0], k -> new java.util.ArrayList<>()).add(a);
							java.util.Set<String> craftOmenKeys = java.util.Set.of("SinistralExaltation", "DextralExaltation", "GreaterExaltation", "HomogenisingExaltation", "CatalysingExaltation", "SinistralCoronation", "DextralCoronation", "HomogenisingCoronation", "SinistralAlchemy", "DextralAlchemy", "Whittling", "SinistralErasure", "DextralErasure", "SinistralAnnulment", "DextralAnnulment", "GreaterAnnulment", "SinistralCrystallisation", "DextralCrystallisation", "SinistralNecromancy", "DextralNecromancy", "Sovereign", "Liege", "Blackblooded", "Light", "AbyssalEchoes", "Putrefaction");
						}
						<details class="rounded-box border border-base-300 bg-base-100" data-craft2-strategy-editor>
							<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "다단계 전략 — 단계 직접 엮기" : "Multi-step strategy — build steps"} <span class="text-xs font-normal text-base-content/70">· ${ko ? "줄마다 화폐 · 징조 · 언제까지 · 최대, \"다음 줄과 묶음\"이면 묶음 반복" : "currency · omen · until · max per row; grouped rows repeat together"}</span></summary>
							<div class="px-3 pb-3 space-y-2">
								<%-- 프리셋(C425 UX QA · C429 — 빈 6줄 아래 맨 끝에 있어 찾기 어려웠다 → 표 위로) — 단계 표가 다 비어 처음엔 무엇을 넣을지 몰랐다): 고르면 단계 표 대신 그 전략, 목표는 위에서 고른 것 --%>
								<label class="flex flex-wrap items-center gap-2 text-xs min-w-0"><span>${ko ? "전략" : "Strategy"}</span>
									<select name="preset" class="select select-xs select-bordered min-w-0" aria-label="${ko ? "전략 프리셋" : "Strategy preset"}" data-craft2-strategy-preset>
										<option value="custom">${ko ? "직접 엮기(아래 단계 표)" : "Custom (steps below)"}</option>
										<option value="alch-chaos">${ko ? "예시: 연금 → 카오스(목표까지, 최대 300)" : "Example: Alchemy → Chaos (until targets, max 300)"}</option>
										<option value="exalt3">${ko ? "예시: 진화 → 확장 → 제왕 → 엑잘(목표까지, 최대 3)" : "Example: Transmute → Augment → Regal → Exalt (until targets, max 3)"}</option>
										<option value="dese-echo">${ko ? "예시: 연금 → 훼손(보존된 뼈) → 드러내기(심연의 메아리)" : "Example: Alchemy → Desecrate (Preserved bone) → Reveal (Abyssal Echoes)"}</option>
									</select>
									<span class="text-base-content/70">${ko ? "프리셋을 고르면 아래 단계 표 대신 그 전략으로 모의합니다" : "A preset replaces the step table below"}</span>
								</label>
								@for(int i = 1; i <= 6; i++)
									<div class="flex flex-wrap items-center gap-2" data-craft2-step-row="${i}">
										<span class="text-xs text-base-content/70 w-4 shrink-0">${i}</span>
										<select name="s${i}" class="select select-xs select-bordered min-w-0 flex-1" aria-label="${ko ? "화폐" : "Currency"} ${i}">
											<option value="">${ko ? "(안 씀)" : "(none)"}</option>
											@for(var g : actGroups2.entrySet())
												<optgroup label="${g.getKey()}">
													@for(String[] a : g.getValue())
														<option value="${a[1]}">${a[2]}</option>
													@endfor
												</optgroup>
											@endfor
										</select>
										<select name="o${i}" class="select select-xs select-bordered min-w-0" aria-label="${ko ? "징조" : "Omen"} ${i}">
											<option value="">${ko ? "징조 없음" : "No omen"}</option>
											@for(Poe2Craft.Omen o : omens)
												@if(craftOmenKeys.contains(o.key()))<option value="${o.key()}">${ko ? o.nameKo() : o.name()}</option>@endif
											@endfor
										</select>
										<select name="u${i}" class="select select-xs select-bordered min-w-0" aria-label="${ko ? "언제까지" : "Until"} ${i}">
											<option value="">${ko ? "한 번" : "Once"}</option>
											<option value="all">${ko ? "목표 전부까지" : "Until all targets"}</option>
											<option value="pre">${ko ? "접두 목표까지" : "Until prefix targets"}</option>
											<option value="suf">${ko ? "접미 목표까지" : "Until suffix targets"}</option>
										</select>
										<input type="number" name="m${i}" min="1" max="1000" placeholder="${ko ? "최대 횟수" : "max uses"}" class="input input-xs input-bordered w-28" aria-label="${ko ? "최대 횟수" : "Max uses"} ${i}">
										<label class="flex items-center gap-1 text-xs"><input type="checkbox" name="g${i}" value="1" class="checkbox checkbox-xs">${ko ? "다음 줄과 묶음" : "Group with next"}</label>
									</div>
								@endfor
								<button type="submit" class="btn btn-sm" hx-post="/poe2/htmx/craft/strategy" hx-target="#craft2-strategy-result" hx-swap="outerHTML" hx-disabled-elt="this" hx-indicator="[data-craft2-calc-busy]" data-craft2-strategy-run>${ko ? "전략 모의" : "Simulate strategy"}</button>
							</div>
						</details>
							</div>
						</details>
					</form>
					<div class="min-w-0 space-y-3 lg:sticky lg:top-20 lg:max-h-[calc(100vh-6rem)] lg:overflow-y-auto lg:pr-1" data-craft2-calc-results>
						<div id="craft2-opt-result" class="rounded-box border border-dashed border-base-300 p-4 text-sm text-base-content/70" data-craft-optimize-empty>${ko ? "목표를 고르고 '최소 비용 찾기'를 누르면 여기에 가장 싼 제작 흐름(상태마다 쓸 수단)이, 아래에 방법별 비교 표가 나옵니다." : "Pick targets and press 'Find the cheapest way' — the cheapest crafting flow appears here and the per-method table below."}</div>
						@template.poe2.htmx.craftCalc(calc = null, targetLabels = java.util.List.of(), nameKo = java.util.Map.of())
						@template.poe2.htmx.craftStrategy(result = null, targetLabels = java.util.List.of(), nameKo = java.util.Map.of(), argName = java.util.Map.of())
					</div>
					</div>
				</section>
			@endif
		</div>
	`
)
