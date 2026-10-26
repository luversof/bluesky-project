@import net.luversof.web.gate.poe.PoeText
@import net.luversof.web.gate.poe.PoeModRange
@import net.luversof.web.gate.poe.dto.PoeCraft

@param PoeCraft.CalcExtras extras = null

<%-- 탐광 · 엘드리치 목표(10-21 C549 사용자 요청 "엘드리치 옵션이나 탐광 같은 것도 있어야 — 최종 아이템에 붙는 옵션을 기준으로 최소 비용, 옵션 자체를
     고를 수 있는 게 누락되면 안 된다"). 탐광 = 화석 전용 옵션(계산기가 그 옵션을 여는 화석을 조합에 넣어 비교), 엘드리치 = 섬뜩한 불씨 · 영액 고정 속성
     (명시 옵션 제작과 무관해 따로 재서 더함 · 한쪽에 하나 · 화폐 등급 1~4). 펼친 내용은 칸 안 스크롤(계산기 폼은 화면 높이 고정 칸 — 길면 목표 목록이
     눌린다) --%>
!{
	boolean ko = PoeText.isKorean();
	String[] tierKo = {"하급", "상급", "우수한", "특출난", "정교한", "완벽한"};
	String[] tierEn = {"Lesser", "Greater", "Grand", "Exceptional", "Exquisite", "Perfect"};
	java.util.List<PoeCraft.DelveLine> delve = extras == null || extras.delve() == null ? java.util.List.of() : extras.delve();
	java.util.List<PoeCraft.EldritchFamily> eld = extras == null || extras.eldritch() == null ? java.util.List.of() : extras.eldritch();
	String eldBlocked = extras == null ? null : !ko && extras.eldritchBlockedEn() != null ? extras.eldritchBlockedEn() : extras.eldritchBlocked();
	java.util.List<PoeCraft.EssenceOnly> essOnly = extras == null || extras.essenceOnly() == null ? java.util.List.of() : extras.essenceOnly();
	String[] essKo = {"속삭이는", "불평하는", "흐느끼는", "울부짖는", "절규하는", "악을 쓰는", "귀를 찢는"};
	String[] essEn = {"Whispering", "Muttering", "Weeping", "Wailing", "Screaming", "Shrieking", "Deafening"};
}
<%-- 에센스 전용 옵션(10-21 C551, 같은 요청의 남은 출처) — 자연 풀에 같은 옵션이 없는 에센스 보장 옵션. 가문마다 최소 에센스 등급을 고르면 계산기는 그
     에센스 연타 줄로 채운다(다른 화폐는 이 옵션을 못 붙인다) --%>
@if(!essOnly.isEmpty())
	<details class="rounded-box border border-base-300 bg-base-100" data-craft-calc-essence-only>
		<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "에센스 전용 옵션 목표" : "Essence-only modifier targets"} <span class="text-xs font-normal text-base-content/70">· ${essOnly.size()}${ko ? "종 · 자연 풀에 없어 그 에센스로만 붙음" : " · not in the natural pool, only that essence adds it"}</span></summary>
		<div class="px-3 pb-3 grid gap-1 max-h-60 overflow-y-auto">
			@for(PoeCraft.EssenceOnly f : essOnly)
				!{
					PoeCraft.EssenceTier top = f.tiers().get(f.tiers().size() - 1);
					java.util.List<String> mx = ko && top.ko() != null && !top.ko().isEmpty() ? top.ko() : (top.en() == null ? java.util.List.<String>of() : top.en());
					java.util.List<String> tg = ko ? (f.tagsKo() == null ? java.util.List.<String>of() : f.tagsKo()) : (f.tags() == null ? java.util.List.<String>of() : f.tags());
					String fname = ko ? top.nameKo() : top.name();
					for (String w : ko ? essKo : essEn) if (fname != null && fname.startsWith(w + " ")) fname = fname.substring(w.length() + 1);
				}
				<label class="flex items-center gap-2 text-xs min-w-0 rounded px-1 py-0.5" data-craft-calc-ess-only="${f.family()}">
					<select name="t_ess:${f.family()}" class="select select-xs select-bordered w-28 shrink-0 h-7 min-h-0" aria-label="${ko ? "최소 에센스 등급" : "Minimum essence tier"}">
						<option value="">-</option>
						@for(PoeCraft.EssenceTier t : f.tiers())
							!{ String tw = t.tier() >= 1 && t.tier() <= 7 ? (ko ? essKo[t.tier() - 1] : essEn[t.tier() - 1]) : (ko ? "에센스" : "Essence"); }
							<option value="${t.tier()}">${f.tiers().size() > 1 && t != top ? tw + (ko ? " 이상" : "+") : tw}</option>
						@endfor
					</select>
					@if(f.icon() != null)<img src="/poe-assets/${f.icon()}" alt="" class="w-5 h-5 object-contain shrink-0" loading="lazy">@endif
					<span class="min-w-0 wrap-anywhere">${"prefix".equals(f.gen()) ? (ko ? "접두 " : "P ") : (ko ? "접미 " : "S ")}@for(int i = 0; i < mx.size(); i++)$unsafe{PoeText.highlightValues(mx.get(i))}@if(i < mx.size() - 1), @endif@endfor@if(!tg.isEmpty())<span class="text-[10px] text-base-content/60"> — ${String.join(", ", tg)}</span>@endif
						<span class="block text-[10px] text-base-content/70">${fname}</span></span>
				</label>
			@endfor
		</div>
	</details>
@endif
@if(!delve.isEmpty())
	<details class="rounded-box border border-base-300 bg-base-100" data-craft-calc-delve>
		<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "탐광(화석 전용) 옵션 목표" : "Delve (fossil-only) modifier targets"} <span class="text-xs font-normal text-base-content/70">· ${delve.size()}${ko ? "종 · 고르면 그 옵션을 여는 화석 조합도 비교" : " · picking one also compares fossil combos that unlock it"}</span></summary>
		<div class="px-3 pb-3 grid gap-1 max-h-60 overflow-y-auto">
			@for(PoeCraft.DelveLine d : delve)
				!{
					java.util.List<String> mx = ko && d.ko() != null && !d.ko().isEmpty() ? d.ko() : (d.en() == null ? java.util.List.<String>of() : d.en());
					java.util.List<String> mn = ko && d.koMin() != null && !d.koMin().isEmpty() ? d.koMin() : (d.enMin() == null ? java.util.List.<String>of() : d.enMin());
					java.util.List<String> tg = ko ? (d.tagsKo() == null ? java.util.List.<String>of() : d.tagsKo()) : (d.tags() == null ? java.util.List.<String>of() : d.tags());
					java.util.List<String> fs = ko ? (d.fossilsKo() == null ? java.util.List.<String>of() : d.fossilsKo()) : (d.fossils() == null ? java.util.List.<String>of() : d.fossils());
				}
				<label class="flex items-start gap-2 text-xs min-w-0" data-craft-calc-delve-mod="${d.id()}">
					<input type="checkbox" name="t_delve:${d.id()}" value="1" class="checkbox checkbox-xs mt-0.5">
					<span class="min-w-0 wrap-anywhere">${"prefix".equals(d.gen()) ? (ko ? "접두 " : "P ") : (ko ? "접미 " : "S ")}@for(int i = 0; i < mx.size(); i++)$unsafe{PoeText.highlightValues(PoeModRange.range(i < mn.size() ? mn.get(i) : null, mx.get(i)))}@if(i < mx.size() - 1), @endif@endfor@if(!tg.isEmpty())<span class="text-[10px] text-base-content/60"> — ${String.join(", ", tg)}</span>@endif
						<span class="block text-[10px] text-base-content/70">${ko ? "여는 화석: " : "Fossils: "}${String.join(", ", fs)}</span></span>
				</label>
			@endfor
		</div>
	</details>
@endif
@if(!eld.isEmpty() || eldBlocked != null)
	<details class="rounded-box border border-base-300 bg-base-100" data-craft-calc-eldritch-targets>
		<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "섬뜩한 고정 속성 목표" : "Eldritch implicit targets"} <span class="text-xs font-normal text-base-content/70">· ${ko ? "섬뜩한 불씨(작열의 총주교) · 영액(세계 포식자) — 명시 옵션 비용에 따로 더함 · 한쪽에 하나" : "Eldritch Ember (Exarch) · Ichor (Eater) — added to the explicit cost · one per side"}@if(eldBlocked != null) · ${eldBlocked}@endif</span></summary>
		@if(eldBlocked == null)
			<div class="px-3 pb-3 space-y-2 max-h-72 overflow-y-auto">
				@for(String side : java.util.List.of("exarch", "eater"))
					!{ java.util.List<PoeCraft.EldritchFamily> sf = eld.stream().filter(f -> side.equals(f.side())).toList(); }
					@if(!sf.isEmpty())
						<div class="space-y-0.5" data-craft-calc-eld-side="${side}">
							<div class="text-xs font-medium">${"exarch".equals(side) ? (ko ? "작열의 총주교 — 섬뜩한 불씨" : "Searing Exarch — Eldritch Ember") : (ko ? "세계 포식자 — 섬뜩한 영액" : "Eater of Worlds — Eldritch Ichor")}</div>
							@for(PoeCraft.EldritchFamily f : sf)
								!{
									PoeCraft.EldritchTier rep = f.tiers().stream().filter(t -> t.tier() <= 4).reduce((a, b) -> b).orElse(f.tiers().get(f.tiers().size() - 1));
									java.util.List<String> mx = ko && rep.ko() != null && !rep.ko().isEmpty() ? rep.ko() : (rep.en() == null ? java.util.List.<String>of() : rep.en());
									java.util.List<String> tg = ko ? (f.tagsKo() == null ? java.util.List.<String>of() : f.tagsKo()) : (f.tags() == null ? java.util.List.<String>of() : f.tags());
									double c1 = f.tiers().stream().filter(t -> t.tier() == 1).mapToDouble(PoeCraft.EldritchTier::chance).findFirst().orElse(0);
								}
								<label class="flex items-center gap-2 text-xs min-w-0 rounded px-1 py-0.5" data-craft-calc-eld="${side}:${f.key()}">
									<select name="t_eld:${side}:${f.key()}" class="select select-xs select-bordered w-28 shrink-0 h-7 min-h-0" aria-label="${(ko ? "최소 등급 — " : "Minimum tier — ") + (mx.isEmpty() ? f.key() : mx.get(0))}">
										<option value="">-</option>
										@for(PoeCraft.EldritchTier t : f.tiers())
											<option value="${t.tier()}">${ko ? tierKo[Math.max(0, Math.min(5, t.tier() - 1))] + " 이상" : tierEn[Math.max(0, Math.min(5, t.tier() - 1))] + "+"}</option>
										@endfor
									</select>
									<span class="min-w-0 wrap-anywhere">@for(int i = 0; i < mx.size(); i++)$unsafe{PoeText.highlightValues(mx.get(i))}@if(i < mx.size() - 1), @endif@endfor@if(!tg.isEmpty())<span class="text-[10px] text-base-content/60"> — ${String.join(", ", tg)}</span>@endif<span class="text-[10px] text-base-content/70"> · ${ko ? "한 개에 " : "per orb "}${String.format("%.1f%%", c1 * 100)}</span></span>
								</label>
							@endfor
						</div>
					@endif
				@endfor
			</div>
		@endif
	</details>
@endif
