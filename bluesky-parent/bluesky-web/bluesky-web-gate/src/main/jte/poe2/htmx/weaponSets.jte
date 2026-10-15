@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText

@param Integer mainSet = null
@param java.util.List<Poe2.RefineSetMetrics> sets = null
@param java.util.List<Poe2.SetRoleView> roles = null

<%-- 무기 세트별 전/후(10-01 사용자 요청: "패시브 트리를 슬롯 1·2 나눠 찍는 경우가 대다수 — 무기 슬롯 1, 2 에 대해 시뮬레이션").
     PoB-PoE2 는 켜진 세트 하나로만 계산하므로 두 세트를 따로 잰 값이다. 다듬기는 주 세트(DPS 가 큰 쪽)를 켠 채 했다.
     세트를 안 나누는 빌드면 그리지 않는다. 시뮬레이션 결과(simResult)와 빌드 화면 자동 다듬기(buildRefineStatus)가 같이 쓴다. --%>
@if(sets != null && !sets.isEmpty())
	!{
		boolean ko = PoeText.isKorean();
		java.util.function.Function<Double, String> fmt = v -> {
			if (v == null) return "-";
			double n = v;
			if (n >= 1_000_000_000d) return String.format("%.2fB", n / 1_000_000_000d);
			if (n >= 1_000_000d) return String.format("%.2fM", n / 1_000_000d);
			if (n >= 10_000d) return String.format("%.1fk", n / 1_000d);
			return String.format("%,.0f", n);
		};
		java.util.function.BiFunction<Double, Double, String> chg = (a, b) -> {
			if (a == null || b == null || a <= 0) return "-";
			double p = (b / a - 1) * 100;
			return Math.abs(p) < 0.05 ? "0.0%" : String.format("%+.1f%%", p);
		};
		// 닌자 세트 성격(10-15 C332 사용자 요청 "세트 1 · 2 가 어떤 특성에 치중해 계산한 것인지 보여 달라") — 시뮬레이션 결과만 싣는다(빌드 자동 다듬기는 null)
		boolean hasRoles = roles != null && !roles.isEmpty();
		java.util.function.Function<String, String> trait = k -> switch (k == null ? "" : k) {
			case "damage" -> ko ? "공격력" : "damage";
			case "crit" -> ko ? "치명타" : "crit";
			case "dot" -> ko ? "지속 피해" : "damage over time";
			case "area" -> ko ? "광역" : "area";
			case "boss" -> ko ? "보스·단일" : "boss / single target";
			case "speed" -> ko ? "속도" : "speed";
			case "accuracy" -> ko ? "명중" : "accuracy";
			case "buff" -> ko ? "버프·오라" : "buffs / auras";
			case "minion" -> ko ? "소환수" : "minions";
			case "defense" -> ko ? "방어" : "defence";
			default -> k;
		};
		java.util.function.Function<String, String> kindLabel = k -> "attack".equals(k) ? (ko ? "공격" : "Attack") : "defense".equals(k) ? (ko ? "방어" : "Defence") : "support".equals(k) ? (ko ? "지원" : "Support") : "-";
		java.util.function.Function<String, String> objLabel = o -> "dps".equals(o) ? "DPS" : "ehp".equals(o) ? (ko ? "생존(EHP)" : "Survival (EHP)") : (ko ? "균형" : "Balanced");
	}
	<div class="space-y-1" data-weapon-sets>
		<div class="text-sm font-semibold">${ko ? "무기 세트별" : "By weapon set"}
			@if(hasRoles)
				<span class="font-normal text-xs text-base-content/60">${ko ? "— 세트마다 패시브·무기가 달라 따로 계산. 주 세트는 고른 목표로, 다른 세트는 닌자 실빌드들이 그 세트를 쓰는 역할(공격·방어·지원)에 맞춘 목표로 다듬었습니다" : "— passives and weapons differ per set, computed separately. The main set was refined for your objective, the other set for the role ninja builds give it (attack, defence, support)"}</span>
			@else
				<span class="font-normal text-xs text-base-content/60">${ko ? "— 세트마다 패시브·무기가 달라 따로 계산(다듬기는 주 세트 기준)" : "— passives and weapons differ per set, computed separately (refined on the main set)"}</span>
			@endif
		</div>
		<div class="overflow-x-auto">
			<table class="table table-sm w-auto">
				<thead>
					<tr>
						<th>${ko ? "무기 세트" : "Weapon set"}</th>
						<th class="text-right">DPS</th>
						<th class="text-right">${MessageUtil.getMessage("poe.refine.change")}</th>
						<th class="text-right">EHP</th>
						<th class="text-right">${MessageUtil.getMessage("poe.refine.change")}</th>
						@if(hasRoles)
							<th class="hidden sm:table-cell" data-set-role-head>${ko ? "닌자 세트 성격" : "Ninja set role"}</th>
							<th class="hidden sm:table-cell">${ko ? "다듬은 목표" : "Refined for"}</th>
						@endif
					</tr>
				</thead>
				<tbody>
					@for(Poe2.RefineSetMetrics s : sets)
						!{
							boolean main = mainSet != null && s.set() != null && mainSet.intValue() == s.set().intValue();
							Double bd = s.before() == null ? null : s.before().dps();
							Double ad = s.after() == null ? null : s.after().dps();
							Double be = s.before() == null ? null : s.before().ehp();
							Double ae = s.after() == null ? null : s.after().ehp();
						}
						<tr class="${main ? "font-semibold" : ""}" data-weapon-set="${s.set()}">
							<td class="whitespace-nowrap">${ko ? "세트 " + s.set() : "Set " + s.set()}@if(main) <span class="badge badge-primary badge-xs">${ko ? "주 세트" : "main"}</span>@endif</td>
							<td class="text-right font-mono">${fmt.apply(bd)} → ${fmt.apply(ad)}</td>
							<td class="text-right font-mono">${chg.apply(bd, ad)}</td>
							<td class="text-right font-mono">${fmt.apply(be)} → ${fmt.apply(ae)}</td>
							<td class="text-right font-mono">${chg.apply(be, ae)}</td>
							@if(hasRoles)
								!{ Poe2.SetRoleView role = roles.stream().filter(x -> s.set() != null && x.set() == s.set().intValue()).findFirst().orElse(null); }
								@if(role == null)
									<td class="hidden sm:table-cell">-</td><td class="hidden sm:table-cell">-</td>
								@else
									!{
										// 역할 배지와 같은 말(방어 세트의 "방어")은 빼고 — "방어 · 방어" 로 겹쳤다
										String focusText = role.focus() == null ? "" : String.join(" · ", role.focus().stream().filter(f -> !("defense".equals(role.kind()) && "defense".equals(f))).map(trait).toList());
										String weaponText = "none".equals(role.weapon()) ? (ko ? "무기 없음" : "no weapon") : (ko && role.weaponKo() != null && !role.weaponKo().isBlank() ? role.weaponKo() : role.weapon().replace("+", " + "));
									}
									<td class="hidden sm:table-cell min-w-0" data-set-role="${role.set()}" data-set-kind="${role.kind()}">
										<span class="badge badge-sm ${"attack".equals(role.kind()) ? "badge-warning badge-outline" : "defense".equals(role.kind()) ? "badge-info badge-outline" : "badge-ghost"}">${kindLabel.apply(role.kind())}</span>
										@if(!focusText.isEmpty())<span class="ml-1">${focusText}</span>@endif
										<div class="text-xs text-base-content/60">${weaponText} · ${ko ? "닌자 " + role.n() + "명 중 " + role.weaponCount() + "명" : role.weaponCount() + " of " + role.n() + " ninja builds"}</div>
									</td>
									<td class="hidden sm:table-cell whitespace-nowrap" data-set-objective="${role.objective()}">${objLabel.apply(role.objective())}@if(!role.main())<div class="text-xs text-base-content/60">${ko ? "세트 역할 기준" : "by set role"}</div>@endif</td>
								@endif
							@endif
						</tr>
					@endfor
				</tbody>
			</table>
		</div>
		@if(hasRoles)
			<%-- 좁은 화면(< 640px)에선 성격 · 목표 열이 표 밖으로 밀려 가로 스크롤해야 보였다 — 표 아래 세트별 한 줄로 --%>
			<ul class="sm:hidden space-y-1 text-xs" data-set-roles-compact>
				@for(Poe2.SetRoleView role : roles)
					!{
						String focusText = role.focus() == null ? "" : String.join(" · ", role.focus().stream().filter(f -> !("defense".equals(role.kind()) && "defense".equals(f))).map(trait).toList());
						String weaponText = "none".equals(role.weapon()) ? (ko ? "무기 없음" : "no weapon") : (ko && role.weaponKo() != null && !role.weaponKo().isBlank() ? role.weaponKo() : role.weapon().replace("+", " + "));
					}
					<li class="min-w-0 wrap-anywhere">
						<span class="font-semibold">${ko ? "세트 " + role.set() : "Set " + role.set()}</span>
						<span class="badge badge-xs ${"attack".equals(role.kind()) ? "badge-warning badge-outline" : "defense".equals(role.kind()) ? "badge-info badge-outline" : "badge-ghost"}">${kindLabel.apply(role.kind())}</span>
						@if(!focusText.isEmpty())${focusText}@endif
						<span class="text-base-content/60">· ${weaponText} · ${ko ? "목표 " : "for "}${objLabel.apply(role.objective())}</span>
					</li>
				@endfor
			</ul>
		@endif
	</div>
@endif
