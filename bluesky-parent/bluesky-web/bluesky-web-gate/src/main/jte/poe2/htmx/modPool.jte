@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe2.Poe2View
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.util.CspNonceHolder

@param Poe2.ModPool pool
<%-- 접두/접미 제목 단계 — 모드 화면(htmx)은 h1 바로 아래라 h2(PoE1 modClass 와 같음), 베이스 상세는 "전체 제작 모드" h2 아래라 h3(10-02 C20: h1→h3 건너뜀) --%>
@param boolean nested = false

!{
	java.util.List<Poe2.ModGroup> prefixes = pool.groups().stream().filter(g -> "prefix".equals(g.gen())).toList();
	java.util.List<Poe2.ModGroup> suffixes = pool.groups().stream().filter(g -> "suffix".equals(g.gen())).toList();
	boolean korean = PoeText.isKorean();
}

<%-- 옵션 풀 — 접두/접미 두 칸. 계열마다 1티어(최고) 문구를 머리에, 펼치면 모든 티어(레벨 · 가중치).
     data-mod-family · data-tier-ilvl 등은 PoE1 모드 검색 + 아이템 레벨 막대(poe/modFilterBar.jte)가 읽는다(10-01). --%>
<div class="space-y-2">
	<div class="flex flex-wrap items-baseline gap-2 text-sm">
		<span class="font-semibold">${net.luversof.web.gate.poe.PoeClassNames.poe2(pool.itemClassKo(), pool.itemClass())}@if(pool.variant() != null) · ${PoeText.name(pool.variantKo(), pool.variant())}@endif</span>
		<span class="text-xs text-base-content/60">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.mods.bases"), pool.bases() == null ? 0 : pool.bases().size())}</span>
	</div>
	<div class="text-xs text-base-content/60">${MessageUtil.getMessage("poe2.mods.hint")}</div>
	<div class="grid grid-cols-1 lg:grid-cols-2 gap-4 items-start">
		@for(String gen : java.util.List.of("prefix", "suffix"))
			!{
				java.util.List<Poe2.ModGroup> groups = "prefix".equals(gen) ? prefixes : suffixes;
				// 확률 분모 = 이 칸(접두/접미) 모든 티어 가중치 합 — PoE1 modFamilyCard 의 poolWeight 와 같은 정의(10-01)
				long poolWeight = 0;
				for (Poe2.ModGroup pg : groups) for (Poe2.ModTier pt : pg.tiers()) poolWeight += pt.weight() == null ? 0 : pt.weight();
			}
			<section class="space-y-1.5">
				@if(nested)
					<h3 class="font-semibold text-sm">${MessageUtil.getMessage("poe2.mods." + gen)} <span class="text-xs font-normal text-base-content/60">${groups.size()}</span></h3>
				@else
					<h2 class="font-semibold text-sm">${MessageUtil.getMessage("poe2.mods." + gen)} <span class="text-xs font-normal text-base-content/60">${groups.size()}</span></h2>
				@endif
				@for(Poe2.ModGroup g : groups)
					!{
						Poe2.ModTier top = g.tiers().get(0);
						java.util.List<String> topLines = Poe2View.tierLines(top);
						// 계열 등장 확률(아무 티어) = 계열 티어 가중치 합 / 칸 합 — PoE1 modFamilyCard 의 famOdds 와 같다(제작 계산기 곱셈용)
						long famWeight = 0;
						for (Poe2.ModTier ft : g.tiers()) famWeight += ft.weight() == null ? 0 : ft.weight();
						String famOdds = poolWeight > 0 ? String.format(java.util.Locale.ROOT, "%.4f", famWeight * 100.0 / poolWeight) : null;
					}
					<details class="rounded-lg bg-base-100 border border-base-200" data-mod-family data-family-odds="${famOdds}" data-family-gen="${gen}">
						<summary class="cursor-pointer px-3 py-2 text-sm">
							@if(famOdds != null)
								<%-- 제작 계산기 선택 — PoE1 과 같은 체크박스(동시 확률에 포함) --%>
								<input type="checkbox" class="checkbox checkbox-xs poe2-craft-pick mr-1 align-middle" title="${korean ? "동시 확률 계산에 포함" : "Include in combined odds"}" aria-label="${korean ? "동시 확률 계산에 포함" : "Include in combined odds"}">
							@endif
							<span class="text-[#8888ff]" data-family-stats data-join=" / ">${String.join(" / ", topLines)}</span>
							<%-- 최상위 티어 요구 아이템 레벨 — PoE1 과 같은 i 배지(레벨 막대가 가용 최상위로 바꿔 준다) --%>
							<span class="ml-1 text-[10px] font-mono text-base-content/60" data-top-ilvl title="${korean ? "최상위 티어 요구 아이템 레벨" : "Top tier required item level"}">i${top.level()}</span>
							<span class="ml-1 text-xs text-base-content/60" data-family-tiers data-tiers-format="${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.mods.tiers"), "{0}")}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.mods.tiers"), g.tiers().size())}</span>
						</summary>
						<div class="overflow-x-auto px-2 pb-2">
							<table class="table table-xs">
								<thead>
									<tr>
										<th scope="col">${MessageUtil.getMessage("poe2.mods.tier")}</th>
										<th scope="col">${MessageUtil.getMessage("poe2.mods.level")}</th>
										<th scope="col">${MessageUtil.getMessage("poe2.mods.title")}</th>
										<th scope="col" class="text-right">${MessageUtil.getMessage("poe2.mods.weight")}</th>
										@if(poolWeight > 0)
											<th scope="col" class="text-right" title="${korean ? "무작위 롤 한 번에 이 티어가 뜰 확률" : "Chance per random roll"}">${korean ? "확률" : "Odds"}</th>
										@endif
									</tr>
								</thead>
								<tbody>
									@for(int i = 0; i < g.tiers().size(); i++)
										!{ Poe2.ModTier t = g.tiers().get(i); }
										<tr data-tier-ilvl="${t.level()}">
											<td class="whitespace-nowrap">T${i + 1}</td>
											<td>${t.level()}</td>
											<td>
												<div data-tier-lines>
													@for(String line : Poe2View.tierLines(t))
														<div>${line}</div>
													@endfor
												</div>
												<div class="text-[10px] text-base-content/60">${PoeText.name(t.nameKo(), t.name())}@if(korean && t.name() != null) · ${t.name()}@endif</div>
											</td>
											<td class="text-right">${t.weight()}</td>
											@if(poolWeight > 0)
												!{ double pct = (t.weight() == null ? 0 : t.weight()) * 100.0 / poolWeight; }
												<td class="text-right font-mono text-[11px] text-base-content/60" data-tier-odds title="${korean ? "평균 1/" : "avg 1 in "}${pct > 0 ? String.valueOf(Math.round(100.0 / pct)) : "∞"}">${pct >= 1 ? String.format("%.1f%%", pct) : String.format("%.2f%%", pct)}</td>
											@endif
										</tr>
									@endfor
								</tbody>
							</table>
						</div>
					</details>
				@endfor
			</section>
		@endfor
	</div>
</div>

<%-- 제작 계산기 — PoE1 모드 화면(poe/htmx/modClass.jte)과 같은 막대(10-01): 체크한 계열들의 "한 롤 동시 등장" 근사 확률(독립 가정, 자리 경쟁 미반영이라
     실제보다 낙관). PoE2 옵션 풀엔 작업대 제작·멀티모드가 없어 접두/접미 각 3칸 규칙만. 조각이 htmx 로 다시 와도 한 번만 묶는다. --%>
<div id="poe2CraftCalc" class="sticky bottom-2 z-10 mt-3 hidden flex-wrap items-center gap-2 rounded-box border border-primary/30 bg-base-100 px-3 py-2 text-sm shadow-lg">
	<span class="font-semibold text-primary" id="poe2CraftCalcLabel"></span>
	<span class="font-mono" id="poe2CraftCalcValue"></span>
	<span class="text-[11px] text-base-content/60">${korean ? "독립 가정 근사 — 접두/접미 자리 경쟁 미반영(실제는 더 낮음)" : "independence approximation"}</span>
	<button type="button" class="btn btn-ghost btn-xs ml-auto" id="poe2CraftCalcClear">${korean ? "선택 해제" : "Clear"}</button>
</div>
<script nonce="${CspNonceHolder.getNonce()}">
	(function () {
		if (window.__poe2CraftCalcBound) return;
		window.__poe2CraftCalcBound = true;
		var ko = ${korean};
		function update() {
			var calc = document.getElementById("poe2CraftCalc");
			if (!calc) return;
			var label = document.getElementById("poe2CraftCalcLabel");
			var value = document.getElementById("poe2CraftCalcValue");
			var picked = [].filter.call(document.querySelectorAll(".poe2-craft-pick"), function (c) { return c.checked; });
			if (!picked.length) { calc.classList.add("hidden"); calc.classList.remove("flex"); return; }
			var p = 1, prefixN = 0, suffixN = 0;
			picked.forEach(function (c) {
				var card = c.closest("[data-family-odds]");
				p *= parseFloat(card.getAttribute("data-family-odds")) / 100;
				if (card.getAttribute("data-family-gen") === "prefix") prefixN++; else suffixN++;
			});
			calc.classList.remove("hidden"); calc.classList.add("flex");
			label.textContent = (ko ? "동시 확률 (" : "Combined odds (") + picked.length + (ko ? "개, 1롤)" : " mods, per roll)");
			// 게임 규칙: 접두/접미 각 최대 3 — 초과 조합은 확률이 아니라 불가
			if (prefixN > 3 || suffixN > 3) {
				value.setAttribute("data-impossible", "");
				value.textContent = ko ? "불가 — " + (prefixN > 3 ? "접두" : "접미") + " 3개 초과" : "impossible (max 3 " + (prefixN > 3 ? "prefixes" : "suffixes") + ")";
				return;
			}
			value.removeAttribute("data-impossible");
			var pct = p * 100;
			value.textContent = (pct >= 0.01 ? pct.toFixed(2) : pct.toExponential(1)) + "% · " + (ko ? "평균 1/" : "avg 1 in ") + Math.round(1 / p).toLocaleString();
		}
		// summary 안 체크박스는 클릭의 활성 대상이 체크박스라 계열이 펼쳐지지 않는다(탐침으로 확인) — 따로 막지 않는다
		document.addEventListener("change", function (e) {
			if (e.target && e.target.classList && e.target.classList.contains("poe2-craft-pick")) update();
		});
		document.addEventListener("click", function (e) {
			if (!e.target || !e.target.closest || !e.target.closest("#poe2CraftCalcClear")) return;
			document.querySelectorAll(".poe2-craft-pick").forEach(function (c) { c.checked = false; });
			update();
		});
		// 다른 풀로 바뀌어 체크가 사라지면 막대도 정리
		document.body.addEventListener("htmx:afterSwap", update);
	})();
</script>
