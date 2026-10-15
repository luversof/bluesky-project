@import net.luversof.web.gate.poe.PoeText
@import net.luversof.web.gate.poe.PoeModRange
@import net.luversof.web.gate.poe.dto.PoeCraft

@param PoeCraft.View view
@param String error = null
@param String lastAction = null
@param java.util.List<String> added = java.util.List.of()
@param String stateJson = "{}"
@param String historyJson = "[]"
@param int historySize = 0
@param java.util.List<PoeCraft.PoolLine> pool = java.util.List.of()
@param java.util.List<PoeCraft.Essence> essences = java.util.List.of()
@param String classLabel = ""

<%-- PoE1 제작 에뮬레이터 결과 구역(10-15 C335) — 화폐를 누르면 이 구역만 바꿔 끼운다. 아이템 상태 · 되돌리기 이력은 숨은 칸(서버는 상태를 안 든다).
     툴팁은 인게임 배색(일반 흰색 · 매직 파랑 · 레어 노랑 헤더, 옵션 파랑), 줄마다 그 풀 안 등급(T1 = 최상위)과 수치 범위. --%>
!{
	boolean ko = PoeText.isKorean();
	PoeCraft.Item item = view.item();
	String rarity = item.rarity() == null ? "NORMAL" : item.rarity();
	String rarCls = switch (rarity) { case "MAGIC" -> "poe-rar-magic"; case "RARE" -> "poe-rar-rare"; default -> "poe-rar-white"; };
	String headColor = switch (rarity) { case "MAGIC" -> "#8888ff"; case "RARE" -> "#ffff77"; default -> "#c8c8c8"; };
	// 테두리 = 이름 색(다른 아이템 툴팁 buildSummary → itemTooltip 의 rarityBorder 와 같은 규칙)
	String borderColor = headColor;
	java.util.List<PoeCraft.Line> lines = view.lines() == null ? java.util.List.of() : view.lines();
	long prefixes = lines.stream().filter(l -> "prefix".equals(l.gen())).count();
	long suffixes = lines.stream().filter(l -> "suffix".equals(l.gen())).count();
	// 매직 이름 = 접두 이름 + 베이스 + 접미 이름(인게임과 같은 조합). 레어는 무작위 이름 대신 "레어 아이템"
	String magicName = classLabel;
	if ("MAGIC".equals(rarity)) {
		String pre = lines.stream().filter(l -> "prefix".equals(l.gen())).map(l -> ko && l.nameKo() != null ? l.nameKo() : l.name()).findFirst().orElse(null);
		String suf = lines.stream().filter(l -> "suffix".equals(l.gen())).map(l -> ko && l.nameKo() != null ? l.nameKo() : l.name()).findFirst().orElse(null);
		magicName = (pre != null ? pre + " " : "") + classLabel + (suf != null ? " " + suf : "");
	}
	boolean normal = "NORMAL".equals(rarity), magic = "MAGIC".equals(rarity), rare = "RARE".equals(rarity);
	int max = view.maxPerGen();
	boolean full = prefixes >= max && suffixes >= max;
	boolean removable = lines.stream().anyMatch(l -> !l.fractured());
	record Btn(String action, String icon, String ko, String en, boolean enabled) {}
	java.util.List<Btn> btns = java.util.List.of(
		new Btn("TRANSMUTE", "currencyupgradetomagic", "변화의 오브", "Orb of Transmutation", normal),
		new Btn("ALTERATION", "currencyrerollmagic", "변경의 오브", "Orb of Alteration", magic),
		new Btn("AUGMENTATION", "currencyaddmodtomagic", "확장의 오브", "Orb of Augmentation", magic && lines.size() < 2),
		new Btn("REGAL", "currencyupgrademagictorare", "제왕의 오브", "Regal Orb", magic),
		new Btn("ALCHEMY", "currencyupgradetorare", "연금술의 오브", "Orb of Alchemy", normal),
		new Btn("CHAOS", "currencyrerollrare", "카오스 오브", "Chaos Orb", rare),
		new Btn("EXALT", "currencyaddmodtorare", "엑잘티드 오브", "Exalted Orb", rare && !full),
		new Btn("ANNUL", "currencyremovemod", "소멸의 오브", "Orb of Annulment", !normal && removable),
		new Btn("SCOUR", "currencyconverttonormal", "정제의 오브", "Orb of Scouring", !normal));
	String poolTitle = normal ? (ko ? "다음 옵션 확률 — 레어로 만들 때(연금 · 에센스)" : "Next modifier odds — when made rare (alchemy, essence)")
		: (ko ? "다음 옵션 확률 — 지금 아이템에 붙을 수 있는 것" : "Next modifier odds — what can still roll on this item");
	java.util.function.Function<java.util.List<String>, java.util.List<String>> nz = l -> l == null ? java.util.List.of() : l;
}
<%-- 화폐 클릭은 조회가 아니라 시험 적용이라 "처리되었습니다" 토스트를 띄우지 않는다(누를 때마다 쌓였다) --%>
<div id="craft-emu" class="grid gap-4 lg:grid-cols-[minmax(0,24rem)_minmax(0,1fr)] items-start" data-craft-rarity="${rarity}" data-no-success-toast>
	<form hx-post="/poe/htmx/craft" hx-target="#craft-emu" hx-swap="outerHTML" class="space-y-3 min-w-0">
		<input type="hidden" name="itemClass" value="${item.itemClass()}">
		<input type="hidden" name="variant" value="${item.variant()}">
		<input type="hidden" name="influence" value="${item.influence()}">
		<input type="hidden" name="ilvl" value="${item.ilvl()}">
		<input type="hidden" name="classLabel" value="${classLabel}">
		<input type="hidden" name="state" value="${stateJson}">
		<input type="hidden" name="history" value="${historyJson}">

		<div class="poe-tooltip ${rarCls} w-full max-w-[24rem]" style="border-color:${borderColor}" data-craft-tooltip>
			<div class="poe-tt-band">
				<div class="text-center font-bold text-[15px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${headColor}" data-craft-name>${rare ? (ko ? "레어 아이템" : "Rare Item") : magicName}</div>
				@if(rare)<div class="text-center text-[13px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${headColor}">${classLabel}</div>@endif
			</div>
			<div class="px-4 py-2 text-center text-[13px] text-white/60">
				${ko ? "아이템 레벨: " : "Item Level: "}<span class="text-white">${item.ilvl()}</span>
			</div>
			@if(!lines.isEmpty())
				<div class="poe-tt-sepimg"></div>
				<div class="px-4 py-2 space-y-0.5 text-center text-[13px]">
					@for(PoeCraft.Line l : lines)
						!{
							java.util.List<String> mx = ko && !nz.apply(l.ko()).isEmpty() ? l.ko() : nz.apply(l.en());
							java.util.List<String> mn = ko && !nz.apply(l.koMin()).isEmpty() ? l.koMin() : nz.apply(l.enMin());
							boolean isNew = added != null && added.contains(l.id());
							String color = l.fractured() ? "#a29162" : l.crafted() ? "#b4b4ff" : "#8888ff";
						}
						<div class="${isNew ? "bg-white/10 rounded" : ""}" style="color:${color}" data-craft-line="${l.id()}" data-craft-gen="${l.gen()}" data-craft-new="${isNew ? "1" : null}">
							<span class="text-[10px] font-mono text-white/40 mr-0.5" title="${ko ? (l.nameKo() != null ? l.nameKo() : l.name()) + (("prefix".equals(l.gen())) ? " · 접두" : " · 접미") + " · 아이템 레벨 " + l.ilvl() : l.name() + (("prefix".equals(l.gen())) ? " · prefix" : " · suffix") + " · item level " + l.ilvl()}">${"prefix".equals(l.gen()) ? (ko ? "접두" : "P") : (ko ? "접미" : "S")}@if(l.tierRank() != null) T${l.tierRank()}@endif@if(l.essence()) ${ko ? "에센스" : "Ess"}@endif</span>
							@for(int i = 0; i < mx.size(); i++)
								<div class="inline">$unsafe{PoeText.highlightValues(PoeModRange.range(i < mn.size() ? mn.get(i) : null, mx.get(i)))}</div>@if(i < mx.size() - 1)<br>@endif
							@endfor
						</div>
					@endfor
				</div>
			@endif
		</div>
		<div class="text-xs text-base-content/60" data-craft-slots>
			${ko ? "접두 " : "Prefixes "}${prefixes}/${max} · ${ko ? "접미 " : "Suffixes "}${suffixes}/${max}
		</div>

		@if(error != null)
			<div role="alert" class="alert alert-warning py-2 text-sm" data-craft-error>${error}</div>
		@endif

		<div class="grid grid-cols-3 sm:grid-cols-5 gap-2" data-craft-actions>
			@for(Btn b : btns)
				<button type="submit" name="action" value="${b.action()}" class="btn btn-sm h-auto min-h-12 flex-col gap-0.5 py-1 ${b.action().equals(lastAction) ? "btn-primary" : "btn-ghost border border-base-300"}" data-craft-action="${b.action()}" disabled="${!b.enabled()}" title="${ko ? b.ko() : b.en()}">
					<img src="/poe-assets/currency/${b.icon()}.png" alt="" class="w-7 h-7 object-contain" loading="lazy">
					<span class="text-[10px] leading-tight whitespace-normal">${ko ? b.ko().replace(" 오브", "").replace("의", "") : b.en().replace("Orb of ", "").replace(" Orb", "")}</span>
				</button>
			@endfor
		</div>

		@if(!essences.isEmpty())
			<div class="flex flex-wrap items-center gap-2" data-craft-essence>
				<select name="essence" class="select select-sm select-bordered min-w-0 flex-1" aria-label="${ko ? "에센스" : "Essence"}">
					@for(PoeCraft.Essence e : essences)
						<option value="${e.slug()}">${ko ? e.nameKo() : e.name()} — ${String.join(", ", ko && e.ko() != null && !e.ko().isEmpty() ? e.ko() : e.en())}</option>
					@endfor
				</select>
				<button type="submit" name="action" value="ESSENCE" class="btn btn-sm" data-craft-action="ESSENCE" disabled="${magic}">${ko ? "에센스 사용" : "Use essence"}</button>
			</div>
		@endif

		<div class="flex flex-wrap items-center gap-2">
			<button type="submit" name="action" value="UNDO" class="btn btn-sm btn-outline" data-craft-action="UNDO" disabled="${historySize == 0}">${ko ? "되돌리기" : "Undo"} (${historySize})</button>
			<button type="submit" name="action" value="RESET" class="btn btn-sm btn-ghost" data-craft-action="RESET">${ko ? "새 일반 아이템" : "New normal item"}</button>
		</div>
		<p class="text-xs text-base-content/60">
			${ko ? "옵션 수(매직 1~2 반반 · 레어 4/5/6 = 8:3:1)는 게임 데이터에 없어 커뮤니티 측정값을 씁니다. 수치는 범위로 표시합니다." : "Affix counts (magic 1–2 at 50/50, rare 4/5/6 at 8:3:1) are not in the game data; community-measured values are used. Values are shown as ranges."}
		</p>
	</form>

	<section class="min-w-0 space-y-2" data-craft-pool>
		<h2 class="font-semibold text-sm">${poolTitle}</h2>
		@if(pool.isEmpty())
			<p class="text-sm text-base-content/60">${ko ? "더 붙을 수 있는 옵션이 없습니다." : "No more modifiers can roll."}</p>
		@else
			@for(String gen : java.util.List.of("prefix", "suffix"))
				!{
					java.util.List<PoeCraft.PoolLine> g = pool.stream().filter(p -> gen.equals(p.gen())).toList();
					double sum = g.stream().mapToDouble(PoeCraft.PoolLine::chance).sum();
				}
				@if(!g.isEmpty())
					<details class="collapse collapse-arrow border border-base-300 bg-base-100" open>
						<summary class="collapse-title min-h-0 py-2 text-sm font-medium">
							${"prefix".equals(gen) ? (ko ? "접두" : "Prefixes") : (ko ? "접미" : "Suffixes")} · ${g.size()}${ko ? "개" : ""} · ${String.format("%.1f%%", sum * 100)}
						</summary>
						<div class="collapse-content overflow-x-auto">
							<table class="table table-xs">
								<thead><tr><th>${ko ? "등급" : "Tier"}</th><th>${ko ? "옵션" : "Modifier"}</th><th class="text-right">${ko ? "아이템 레벨" : "iLvl"}</th><th class="text-right">${ko ? "가중치" : "Weight"}</th><th class="text-right">${ko ? "확률" : "Chance"}</th></tr></thead>
								<tbody>
									@for(PoeCraft.PoolLine p : g)
										!{
											java.util.List<String> mx = ko && !nz.apply(p.ko()).isEmpty() ? p.ko() : nz.apply(p.en());
											java.util.List<String> mn = ko && !nz.apply(p.koMin()).isEmpty() ? p.koMin() : nz.apply(p.enMin());
										}
										<tr data-craft-pool-line="${p.id()}">
											<td class="font-mono text-xs">@if(p.tierRank() != null)T${p.tierRank()}@endif</td>
											<td class="min-w-0 wrap-anywhere">
												@for(int i = 0; i < mx.size(); i++)
													<div>$unsafe{PoeText.highlightValues(PoeModRange.range(i < mn.size() ? mn.get(i) : null, mx.get(i)))}</div>
												@endfor
											</td>
											<td class="text-right font-mono">${p.ilvl()}</td>
											<td class="text-right font-mono">${p.weight()}</td>
											<td class="text-right font-mono" data-craft-chance>${String.format("%.2f%%", p.chance() * 100)}</td>
										</tr>
									@endfor
								</tbody>
							</table>
						</div>
					</details>
				@endif
			@endfor
		@endif
	</section>
</div>
