@import net.luversof.web.gate.poe.PoeText
@import net.luversof.web.gate.poe.dto.PoeCraft

@param PoeCraft.OptimizeResult opt = null
@param String optError = null
@param java.util.List<String> targetLabels = java.util.List.of()
@param String unit = "c"
@param String game = "poe1"
@param String base = "/poe"
@param String jobId = null

<%-- 최적 조합(10-19 C457 사용자 요청 "에센스 · 제작대 · 야수 같은 다양한 방법을 조합해 원하는 아이템을 뽑는 최소 비용") — 계산기 표 아래.
     제작 과정을 상태(희귀도 · 맞은 목표 · 잡옵션 · 막힌 자리 · 제작대 옵션)로 보고 상태마다 가장 싼 수단을 찾은 정책, 그 정책을 실제 아이템에 굴려 잰 비용. --%>
!{
	boolean ko = PoeText.isKorean();
	java.util.function.Function<Double, String> num = v -> v == null ? "-" : v >= 100 ? String.format("%,.0f", v) : v >= 1 ? String.format("%,.1f", v) : String.format("%,.3f", v);
	String err = optError != null ? optError : opt == null ? (ko ? "결과가 없습니다" : "No result") : opt.error();
	Double gain = opt == null || opt.expectedCost() == null || opt.upper() == null || opt.upper() <= 0 ? null : 1 - opt.expectedCost() / opt.upper();
}
<section id="${"/poe2".equals(base) ? "craft2-opt-result" : "craft-opt-result"}" class="rounded-box border border-primary/40 bg-base-100 p-3 space-y-3" data-craft-optimize-result>
	<h3 class="text-base font-semibold">${ko ? "최적 조합" : "Optimal combination"} <span class="text-xs font-normal text-base-content/70">· ${"poe2".equals(game) ? (ko ? "화폐 · 등급 · 징조 · 에센스 · 훼손 · 분열 · 재조합기 · 새 베이스를 섞어 상태마다 가장 싼 수단" : "orbs, tiers, omens, essences, desecration, fracturing, the Recombinator and new bases mixed — the cheapest action in every state") : ko ? "에센스 · 화석 · 수확 · 제작대(메타 제작 포함) · 짐승 · 각인 · 분열 · 화폐를 섞어 상태마다 가장 싼 수단" : "essences, fossils, harvest, bench (incl. meta-crafts), beasts, imprints, fracturing and orbs mixed — the cheapest action in every state"}</span></h3>
	@if(!targetLabels.isEmpty())
		<ol class="text-xs text-base-content/70 flex flex-wrap gap-x-3 gap-y-0.5" data-craft-optimize-targets>
			@for(int i = 0; i < targetLabels.size(); i++)
				<li><span class="font-semibold">${ko ? "목표 " : "Target "}${i + 1}</span> ${targetLabels.get(i)}</li>
			@endfor
		</ol>
	@endif
	@if(err != null)
		<div role="alert" class="alert alert-warning py-2 text-sm" data-craft-optimize-error>${err}</div>
	@else
		<div class="flex flex-wrap items-baseline gap-x-4 gap-y-1">
			<div><span class="text-xs text-base-content/70">${ko ? "성공 한 번당 기대 비용" : "Expected cost per success"}</span>
				<div class="text-2xl font-bold font-mono" data-craft-optimize-cost="${opt.expectedCost()}">${num.apply(opt.expectedCost())}${unit}@if(opt.stderr() != null)<span class="text-sm font-normal text-base-content/70"> ±${num.apply(opt.stderr() * 1.96)}</span>@endif</div>
			</div>
			@if(opt.upper() != null && opt.upper() < 1e8)
				<div class="text-sm" data-craft-optimize-compare>
					${ko ? "한 가지 방법만 반복할 때 최저" : "Cheapest single method"} (${opt.upperLabel()}): <span class="font-mono">${num.apply(opt.upper())}${unit}</span>
					@if(gain != null && gain > 0.005)
						<span class="badge badge-success badge-sm" data-craft-optimize-gain>${ko ? String.format("%.0f%% 저렴", gain * 100) : String.format("%.0f%% cheaper", gain * 100)}</span>
					@elseif(gain != null)
						<span class="badge badge-ghost badge-sm">${ko ? "한 가지 방법 반복보다 나은 조합을 못 찾음" : "No better combination found"}</span>
					@endif
				</div>
			@endif
		</div>
		@if(jobId != null)
			<%-- 에뮬레이터 따라 하기(C464) — 에뮬레이터가 이 id 로 지금 아이템의 다음 수단을 받는다 --%>
			<input type="hidden" name="optJob" value="${jobId}">
			<p class="text-xs text-base-content/70" data-craft-optimize-follow>${ko ? "위 에뮬레이터에서 화폐를 쓰면 이 조합이 지금 아이템에 쓸 다음 수단을 알려 줍니다(10분 동안)." : "Use currency in the emulator above and it shows this plan's next action for the current item (for 10 minutes)."}</p>
		@endif
		@if(opt.path() != null && !opt.path().isEmpty())
			<div class="space-y-1">
				<div class="text-xs font-medium text-base-content/70">${ko ? "흔한 흐름(성공한 회차 중 비용이 중간인 것 — 같은 수단 연속은 ×n)" : "Typical run (median-cost success — repeats as ×n)"}</div>
				<p class="text-sm leading-relaxed wrap-anywhere" data-craft-optimize-path>${opt.path().length() > 900 ? opt.path().substring(0, opt.path().lastIndexOf(" → ", 900)) + " → …" : opt.path()}</p>
			</div>
		@endif
		@if(!opt.rules().isEmpty())
			<details class="rounded-box border border-base-300" data-craft-optimize-rules open>
				<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "상태별 결정" : "Decision per state"} <span class="text-xs font-normal text-base-content/70">· ${ko ? "자주 지나는 상태 — 목표 번호는 위 '목표' 순서" : "most visited states — target numbers follow the target list"}</span></summary>
				<div class="px-3 pb-3 overflow-x-auto">
					<table class="table table-xs">
						<thead><tr><th>${ko ? "상태" : "State"}</th><th>${ko ? "쓸 수단" : "Action"}</th><th class="text-right">${ko ? "지나는 회차" : "Visited"}</th><th class="text-right">${ko ? "남은 기대 비용" : "Cost to go"}</th></tr></thead>
						<tbody>
							@for(PoeCraft.OptimizeRule r : opt.rules())
								<tr data-craft-optimize-rule>
									<td class="min-w-[12rem] break-keep">${r.state()}</td>
									<td class="min-w-[9rem] break-keep font-medium">${r.action()}</td>
									<td class="text-right font-mono">${String.format("%.0f%%", r.share() * 100)}</td>
									<td class="text-right font-mono">${r.remaining() == null ? "-" : num.apply(r.remaining()) + unit}</td>
								</tr>
							@endfor
						</tbody>
					</table>
				</div>
			</details>
		@endif
		@if(!opt.perSuccess().isEmpty())
			<details class="rounded-box border border-base-300" data-craft-optimize-spend>
				<summary class="cursor-pointer select-none px-3 py-2 text-sm font-medium">${ko ? "성공 한 번당 쓰는 화폐" : "Currency per success"}</summary>
				<div class="px-3 pb-3 overflow-x-auto">
					<table class="table table-xs">
						<tbody>
							@for(var e : opt.perSuccess().entrySet())
								<tr><td class="wrap-anywhere">${ko ? opt.namesKo().getOrDefault(e.getKey(), e.getKey()) : e.getKey()}</td><td class="text-right font-mono">${num.apply(e.getValue())}</td></tr>
							@endfor
						</tbody>
					</table>
				</div>
			</details>
		@endif
		<details class="text-xs text-base-content/70">
			<summary class="cursor-pointer select-none">${ko ? "어떻게 계산했나" : "How this is computed"}</summary>
			<p class="mt-1">${ko ? "제작 과정을 상태(희귀도 · 맞은 목표 · 쪽마다 남는 잡옵션 수 · 잡옵션이 목표 자리를 막는지 · 제작대 옵션)로 보고, 상태마다 쓸 수 있는 수단의 결과를 같은 제작 엔진으로 표본을 떠 확률을 만든 뒤 상태별 최소 기대 비용(가치 반복)을 구했습니다. 어느 상태에서든 '정제(PoE2 는 새 베이스) → 이 조합으로 처음부터'를 할 수 있어 그 값(다시 시작 비용 + 처음 상태 값, 뉴턴으로 풂)과 아는 가장 싼 방법(표의 첫 줄) 중 싼 쪽을 넘김 값으로 두었고, 정한 정책을 실제 아이템에 다시 굴려 비용을 쟀습니다(넘김도 실제로 다시 시작, ± = 95% 구간). 짐승 단가는 poe.ninja 짐승 시세(레시피에 필요한 짐승 합)." : "Crafting is modelled as states (rarity, targets met, unwanted modifiers per side, blocked target slots, bench modifiers); each action's outcomes are sampled with the same crafting engine, minimum expected costs per state come from value iteration, giving up is valued at the cheaper of 'scour (new base in PoE2) and restart with this plan' (restart cost + start value, solved by Newton's method) and the best single method, and the resulting policy is replayed on real items, restarts included (± = 95% interval). Beast recipe prices are the sum of the required beasts on poe.ninja."}</p>
			<%-- 적용한 제작 규칙(C460 사용자 요청 "병행 안 되는 제작 규칙도 모두 체크") — 상태마다 수단을 제작 엔진으로 실제 적용해 보므로 엔진이 막는 조합은 쓰이지 않는다 --%>
			<div class="mt-2" data-craft-optimize-rule-list>
				<div class="font-medium">${ko ? "적용한 제작 규칙(엔진이 막는 조합은 계산에서 빠짐)" : "Crafting rules applied (combinations the engine rejects are never used)"}</div>
				<ul class="list-disc pl-5 space-y-0.5">
					@if("poe2".equals(game) && ko)
						<li>희귀도 — 진화 · 연금 = 일반, 확장 · 제왕 = 매직, 엑잘 · 카오스 = 레어, 소멸 = 매직/레어(PoE2 엔 정제가 없어 다시 시작은 새 베이스)</li>
						<li>자리 — 매직 접두 1 · 접미 1, 레어 3 · 3, 같은 옵션 그룹은 하나만, 상위 · 완벽 화폐는 최소 속성 레벨 · 아이템 레벨이 모자라면 못 씀</li>
						<li>징조 — 같은 화폐의 좌 · 우 징조를 함께 걸면 막힘, 좌측 = 접두만 · 우측 = 접미만, 상위 찬미 = 옵션 둘, 절사 = 가장 낮은 레벨 옵션을 지움</li>
						<li>에센스 — 보통은 매직 → 레어 + 보장 옵션, 완벽은 레어에서 하나 지우고 보장(결정화 징조로 쪽 지정)</li>
						<li>훼손 — 한 아이템에 훼손 하나, 드러내기는 제시된 셋 중 하나(목표 먼저) · 분열은 옵션 4개 이상 레어, 분열 옵션은 안 지워짐 · 타락 · 축성되면 더 못 바꿈</li>
						<li>새 베이스 — 베이스 값은 빼고 계산(시뮬레이터 표와 같은 가정)</li>
						<li>재조합기 — 재조합 성공 확률(인게임 값)을 넣었을 때만: 한쪽(접두/접미) 목표를 다 채운 매직 · 레어에 다른 쪽 반쪽(표의 가장 싼 방법으로 따로 만듦)을 합침, 성공 = 완성 · 실패 = 둘 다 잃음, 주얼 제외</li>
					@elseif("poe2".equals(game))
						<li>Rarity — Transmute/Alchemy on normal, Augment/Regal on magic, Exalt/Chaos on rare, Annul on magic or rare (no scouring in PoE2, restarts use a new base)</li>
						<li>Slots — magic 1 · 1, rare 3 · 3, one modifier per group, greater/perfect orbs need their minimum modifier level and item level</li>
						<li>Omens — left and right omens of the same orb cannot be combined, sinistral = prefixes only, dextral = suffixes only, greater exaltation = two modifiers, whittling removes the lowest-level modifier</li>
						<li>Essences — normal: magic to rare with a guaranteed modifier, perfect: remove one on a rare and add the guaranteed one (crystallisation omens pick the side)</li>
						<li>Desecration — one per item, reveal picks one of three (target first) · fracturing needs a rare with 4+ modifiers, fractured modifiers stay · corrupted/sanctified items cannot change</li>
						<li>New base — base price excluded (same assumption as the simulator table)</li>
						<li>Recombinator — only with an entered in-game chance: a magic/rare with one side's targets complete is combined with the other side's half (made separately with the cheapest table method); success completes the item, failure loses both; not for jewels</li>
					@elseif(ko)
						<li>희귀도 — 진화 · 연금 = 일반, 변화 · 확장 · 제왕 = 매직, 카오스 · 엑잘 · 짐승 맞바꾸기 · 수확 재련 = 레어, 소멸 · 정제 = 매직/레어</li>
						<li>자리 — 매직 접두 1 · 접미 1, 레어 3 · 3, 같은 옵션 그룹은 하나만, 아이템 레벨 · 화폐 등급이 허용하는 등급만</li>
						<li>에센스 — 속삭이는 ~ 울부짖는(1~4 등급)은 일반 아이템에만, 비명 등급부터 · 타락 에센스는 레어 재련, 보장 옵션 쪽이 잠겼거나 자리가 없으면 못 씀</li>
						<li>제작대 — 제작 속성은 하나(‘제작 속성 최대 3개’ 메타가 있으면 셋), 같은 그룹 · 빈 자리, 메타 제작은 접미/접두 자리를 차지</li>
						<li>메타 제작 — 접두어/접미어 변경 불가면 카오스 · 소멸 · 정제 · 화석 · 수확 · 짐승 제거가 그쪽을 건드리지 않음, 공격/시전 속성 부여 불가면 그 옵션이 안 붙음</li>
						<li>짐승 — 맞바꾸기는 레어만, 붙일 쪽에 빈 자리 · 지울 쪽에 지울 옵션(분열 · 잠긴 쪽 제외)이 있어야</li>
						<li>엘드리치 — 갑옷 · 장화 · 장갑 · 투구, 쉐이퍼 · 엘더 · 정복자 영향 아이템엔 불가, 섬뜩한 화폐는 우세한 쪽만(동률이면 못 씀)</li>
						<li>장막 — 장막 옵션은 하나, 해제는 제시된 셋 중 하나 · 분열 옵션은 지워지지 않음 · 타락하면 더 못 바꿈</li>
						<li>분열의 오브 — 옵션 4개 이상 · 영향력 없는 레어, 이미 분열이면 못 씀, 분열된 옵션은 이후 어떤 재련에도 남음</li>
						<li>수확 — 원소 바꾸기는 첫 해당 옵션을 같은 등급의 다른 원소로(바뀐 옵션과 같은 그룹이 있으면 못 씀) · 유형 옵션 추가 + 무작위 제거는 영향력 없는 아이템만 · 같은 유형 더/덜 재련은 지금 옵션 유형에 달림</li>
						<li>각인(짐승) — 매직 아이템만 저장, 되돌리면 각인은 사라짐(한 번에 하나)</li>
					@else
						<li>Rarity — Transmute/Alchemy on normal, Alteration/Augment/Regal on magic, Chaos/Exalt/beast swaps/harvest reforges on rare, Annul/Scour on magic or rare</li>
						<li>Slots — magic 1 prefix · 1 suffix, rare 3 · 3, one modifier per group, tiers allowed by item level and currency tier</li>
						<li>Essences — Whispering to Wailing (tiers 1–4) on normal items only, Screaming and up and corrupted essences reforge rares; blocked if the guaranteed side is locked or full</li>
						<li>Fracturing Orb — non-influenced rare with 4+ modifiers, not already fractured; the fractured modifier survives every later reforge</li>
						<li>Harvest — element swaps turn the first matching modifier into the same tier of another element (blocked if that group already exists) · add-a-type/remove-random needs a non-influenced item · more/less likely reforges depend on the current modifier types</li>
						<li>Imprint (beast) — stores a magic item only; restoring consumes the imprint (one at a time)</li>
						<li>Bench — one crafted modifier (three with the multimod meta-craft), group and free slot required; meta-crafts take a slot</li>
						<li>Meta-crafts — prefix/suffix locks keep that side from chaos, annul, scour, fossils, harvest and beast removals; cannot-roll metas exclude those modifiers</li>
						<li>Beasts — swaps on rares only, needing a free slot on the added side and a removable modifier on the other (not fractured or locked)</li>
						<li>Eldritch — armour pieces only, not on Shaper/Elder/conqueror items; eldritch orbs act on the dominant side only (none on a tie)</li>
						<li>Veiled — one veiled modifier, unveil picks one of three · fractured modifiers stay · corrupted items cannot change</li>
					@endif
				</ul>
			</div>
			<p class="mt-1">${opt.note()}</p>
			<p class="mt-1">${ko ? "고려한 수단: " : "Actions considered: "}${String.join(" · ", opt.methods())}</p>
		</details>
	@endif
</section>
