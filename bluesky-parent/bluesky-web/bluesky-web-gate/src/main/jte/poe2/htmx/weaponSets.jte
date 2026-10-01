@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText

@param Integer mainSet = null
@param java.util.List<Poe2.RefineSetMetrics> sets = null

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
	}
	<div class="space-y-1" data-weapon-sets>
		<div class="text-sm font-semibold">${ko ? "무기 세트별" : "By weapon set"}
			<span class="font-normal text-xs text-base-content/60">${ko ? "— 세트마다 패시브·무기가 달라 따로 계산(다듬기는 주 세트 기준)" : "— passives and weapons differ per set, computed separately (refined on the main set)"}</span>
		</div>
		<div class="overflow-x-auto">
			<table class="table table-sm w-auto">
				<thead>
					<tr>
						<th>${ko ? "무기 세트" : "Weapon set"}</th>
						<th class="text-right">DPS</th>
						<th class="text-right">${ko ? "변화" : "Change"}</th>
						<th class="text-right">EHP</th>
						<th class="text-right">${ko ? "변화" : "Change"}</th>
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
						</tr>
					@endfor
				</tbody>
			</table>
		</div>
	</div>
@endif
