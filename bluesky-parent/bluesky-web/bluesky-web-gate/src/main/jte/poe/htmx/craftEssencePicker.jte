@import net.luversof.web.gate.poe.PoeText
@import net.luversof.web.gate.poe.dto.PoeCraft

@param java.util.List<PoeCraft.Essence> essences
@param java.util.Map<String, String> blocked = java.util.Map.of()
@param String selectLabel = ""
@param String noneLabel = null

<%-- 에센스 고르기(10-21 C547 사용자 요청 "에센스 셀렉트가 너무 길다 — 종류를 아이콘으로 고르고 등급을 고르는 식") — 105개(종류 25 × 등급 1~7)를 한 셀렉트에
     늘어놓던 것을 종류 아이콘 한 줄 + 고른 종류의 등급 단추 + 보장 옵션 문구로. 값의 주인은 숨은 select[name=essence](sr-only · Tab · 화면 읽기에서 뺌 — 조작은 단추, 탐침 · 열린 값 유지용) 그대로라
     서버 · 열린 값 유지(data-keep-values) · 적중 확률은 바뀌지 않는다. 단추 → select 값 · change, select → 단추 표시(common.ts syncEssencePicker).
     noneLabel 이 있으면 '안 함'(값 "") 단추(계산기) --%>
!{
	boolean ko = PoeText.isKorean();
	String[] tierKo = {"속삭이는", "불평하는", "흐느끼는", "울부짖는", "절규하는", "악을 쓰는", "귀를 찢는"};
	String[] tierEn = {"Whispering", "Muttering", "Weeping", "Wailing", "Screaming", "Shrieking", "Deafening"};
	java.util.Map<String, java.util.List<PoeCraft.Essence>> fams = new java.util.LinkedHashMap<>();
	for (PoeCraft.Essence e : essences) fams.computeIfAbsent(e.family() == null || e.family().isBlank() ? e.slug() : e.family(), k -> new java.util.ArrayList<>()).add(e);
	// 범위로(10-22 Q7 — 최댓값만 보여 '+105 인데 +91 이 붙었다'로 읽혔다)
	java.util.function.Function<PoeCraft.Essence, String> modText = e -> {
		boolean useKo = ko && e.ko() != null && !e.ko().isEmpty();
		java.util.List<String> mx = useKo ? e.ko() : (e.en() == null ? java.util.List.<String>of() : e.en());
		java.util.List<String> mn = useKo ? e.koMin() : e.enMin();
		java.util.List<String> out = new java.util.ArrayList<>();
		for (int i = 0; i < mx.size(); i++) out.add(net.luversof.web.gate.poe.PoeModRange.range(mn != null && i < mn.size() ? mn.get(i) : null, mx.get(i)));
		return String.join(", ", out);
	};
}
<select name="essence" class="sr-only" tabindex="-1" aria-hidden="true" aria-label="${selectLabel}" data-craft-ess-select>
	@if(noneLabel != null)<option value="">${noneLabel}</option>@endif
	@for(PoeCraft.Essence e : essences)
		<option value="${e.slug()}" disabled="${blocked.containsKey("ESSENCE:" + e.slug())}">${ko ? e.nameKo() : e.name()} — ${modText.apply(e)}@if(blocked.containsKey("ESSENCE:" + e.slug())) (${blocked.get("ESSENCE:" + e.slug())})@endif</option>
	@endfor
</select>
<div class="flex flex-wrap gap-1" role="group" aria-label="${selectLabel}" data-craft-ess-families>
	@if(noneLabel != null)
		<button type="button" class="btn btn-ghost btn-sm h-auto min-h-0 w-[4.5rem] flex-col gap-0.5 p-1 border border-base-300" data-craft-ess-pick="" data-desc="${noneLabel}" aria-pressed="false" title="${noneLabel}"><span class="w-7 h-7 grid place-items-center text-lg leading-none">−</span><span class="text-[10px] leading-tight whitespace-normal">${noneLabel}</span></button>
	@endif
	@for(var fe : fams.entrySet())
		!{
			java.util.List<PoeCraft.Essence> fl = fe.getValue().stream().sorted(java.util.Comparator.comparingInt(PoeCraft.Essence::tier)).toList();
			PoeCraft.Essence top = fl.get(fl.size() - 1);
			String fk = top.nameKo() == null || top.nameKo().isBlank() ? top.name() : top.nameKo();
			for (String w : tierKo) if (fk.startsWith(w + " ")) fk = fk.substring(w.length() + 1);
			if (fk.endsWith("의 에센스")) fk = fk.substring(0, fk.length() - 5);
			String fen = top.name();
			for (String w : tierEn) if (fen.startsWith(w + " ")) fen = fen.substring(w.length() + 1);
			if (fen.startsWith("Essence of ")) fen = fen.substring(11);
			boolean anyOk = fl.stream().anyMatch(x -> !blocked.containsKey("ESSENCE:" + x.slug()));
		}
		<button type="button" class="btn btn-ghost btn-sm h-auto min-h-0 w-[4.5rem] flex-col gap-0.5 p-1 border border-base-300 ${anyOk ? null : "text-base-content/60"}" data-craft-ess-fam="${fe.getKey()}" aria-pressed="false" title="${(ko ? fk + "의 에센스" : "Essence of " + fen) + " · " + fl.size() + (ko ? "등급" : (fl.size() == 1 ? " tier" : " tiers")) + (anyOk ? "" : (ko ? " — 지금 아이템에 못 씀" : " — not usable on this item"))}">
			@if(top.icon() != null)<img src="/poe-assets/${top.icon()}" alt="" class="w-7 h-7 object-contain ${anyOk ? null : "grayscale opacity-60"}" loading="lazy">@else<span class="w-7 h-7"></span>@endif
			<span class="text-[10px] leading-tight whitespace-normal wrap-anywhere">${ko ? fk : fen}</span>
		</button>
	@endfor
</div>
@for(var fe : fams.entrySet())
	!{ java.util.List<PoeCraft.Essence> fl = fe.getValue().stream().sorted(java.util.Comparator.comparingInt(PoeCraft.Essence::tier)).toList(); }
	<div class="flex flex-wrap items-center gap-1" data-craft-ess-tiers="${fe.getKey()}" role="group" aria-label="${ko ? "등급" : "Tier"}" hidden>
		@for(PoeCraft.Essence e : fl)
			!{
				String why = blocked.get("ESSENCE:" + e.slug());
				String tw = e.tier() >= 1 && e.tier() <= 7 ? (ko ? tierKo[e.tier() - 1] : tierEn[e.tier() - 1]) : (ko ? e.nameKo() : e.name());
				String desc = (ko ? e.nameKo() : e.name()) + " — " + modText.apply(e) + (why == null ? "" : " (" + why + ")");
			}
			<button type="button" class="btn btn-xs btn-ghost border border-base-300 gap-1" data-craft-ess-pick="${e.slug()}" data-tier="${e.tier()}" data-desc="${desc}" data-why="${why}" aria-pressed="false" disabled="${why != null}" title="${desc}">@if(e.icon() != null)<img src="/poe-assets/${e.icon()}" alt="" class="w-4 h-4 object-contain" loading="lazy">@endif${tw}</button>
		@endfor
	</div>
@endfor
<p class="text-xs min-w-0 wrap-anywhere text-base-content/80" data-craft-ess-desc aria-live="polite"></p>
