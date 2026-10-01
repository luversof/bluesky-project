@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param Poe2.BuildRecalc recalc = null
@param String error = null

<%-- 엔진 재계산(PoB-PoE2) — 요약의 스탯 칸 아래. 저장값(코드를 만든 PoB 가 계산해 둔 값)과 이 서버 엔진의 값을 나란히, 그리고 PoB 가 수치엔 반영하지 않는
     "인게임에선 안 되는" 경고 두 가지. 차이가 1% 넘는 줄은 표시 — 코드를 만든 PoB 버전·설정이 달라서일 수 있다. --%>
!{
	boolean korean = PoeText.isKorean();
	// 무기 세트를 나눠 쓰는 빌드(10-01) — 세트 1·2 를 따로 계산해 열을 둘 더 보인다. 저장값·차이는 저장 당시 켜진 세트 기준.
	boolean split = recalc != null && recalc.savedSet() != null;
}
<section class="space-y-1.5 max-w-3xl" data-poe2-recalc>
	<h3 class="text-sm font-semibold">${korean ? "엔진 재계산 (PoB-PoE2)" : "Engine recalculation (PoB-PoE2)"}</h3>
	@if(error != null)
		<div class="text-sm text-warning">${korean ? "코드를 읽지 못해 재계산하지 못했습니다." : "Could not read the code for recalculation."}</div>
	@elseif(recalc == null || !Boolean.TRUE.equals(recalc.available()))
		<div class="text-sm text-base-content/60">${korean ? "이 서버에는 PoB-PoE2 엔진이 없습니다 — 데이터 관리에서 추출을 실행하면 함께 설치됩니다." : "The PoB-PoE2 engine is not installed on this server — run the data extraction in Data Admin."}</div>
	@elseif(recalc.error() != null)
		<div class="text-sm text-warning">${korean ? "엔진 계산에 실패했습니다: " : "Engine calculation failed: "}<span class="font-mono text-xs">${recalc.error().length() > 160 ? recalc.error().substring(0, 160) + "…" : recalc.error()}</span></div>
	@else
		@if(recalc.unappliedSupports() != null && recalc.unappliedSupports() > 0)
			<div role="alert" class="alert alert-warning py-2 text-sm">${korean ? "주 스킬 묶음에서 적용되지 않는 보조젬 " + recalc.unappliedSupports() + "개 — 인게임에선 효과가 없습니다(PoB 는 계산만 건너뜁니다)." : recalc.unappliedSupports() + " support gem(s) in the main skill group do not apply in game."}</div>
		@endif
		@if(Boolean.TRUE.equals(recalc.mainSkillCostWarning()))
			<div role="alert" class="alert alert-warning py-2 text-sm">${korean ? "주 스킬 비용을 치를 자원이 모자랍니다 — 인게임에선 쓸 수 없습니다." : "Not enough resource to pay the main skill's cost — it cannot be used in game."}</div>
		@elseif(recalc.costWarnings() != null && recalc.costWarnings() > 0)
			<div class="text-sm text-warning">${korean ? "비용을 치르지 못하는 스킬 " + recalc.costWarnings() + "개" : recalc.costWarnings() + " skill(s) cannot pay their cost"}</div>
		@endif
		<div class="overflow-x-auto">
			<table class="table table-xs w-auto">
				<thead>
					<tr>
						<th>${korean ? "항목" : "Stat"}</th>
						<th class="text-right">${korean ? "PoB 저장값" : "Saved in PoB"}</th>
						<th class="text-right">${korean ? "엔진 재계산" : "Recalculated"}@if(split)<div class="text-[10px] font-normal text-base-content/50">${korean ? "저장 세트 " + recalc.savedSet() : "saved set " + recalc.savedSet()}</div>@endif</th>
						<th class="text-right">${korean ? "차이" : "Diff"}</th>
						@if(split)
							@for(int ws = 1; ws <= 2; ws++)
								<th class="text-right" data-recalc-set="${ws}">${korean ? "세트 " + ws : "Set " + ws}@if(recalc.mainSet() != null && recalc.mainSet() == ws)<div><span class="badge badge-primary badge-xs">${korean ? "주 세트" : "main"}</span></div>@endif</th>
							@endfor
						@endif
					</tr>
				</thead>
				<tbody>
					@for(Poe2.BuildRecalcRow r : recalc.rows())
						!{
							String key = r.key().toLowerCase(java.util.Locale.ROOT);
							String label = "spirit".equals(key) ? MessageUtil.getMessage("poe2.build.stat.spirit") : MessageUtil.getMessage("poe.build.stat." + key, r.key());
							boolean pct = key.endsWith("resist");
							java.util.function.Function<Double, String> fmt = v -> v == null ? "-" : pct ? String.format("%.0f%%", v) : Math.abs(v) >= 100 || v == Math.rint(v) ? String.format("%,.0f", v) : String.format("%.2f", v);
							// 저항은 이미 % 라 차이를 %p(포인트)로 — 75% → -13% 를 "-117%" 로 보이면 뜻이 없다
							// ⚠ 삼항으로 null 과 double 을 섞으면 자동 언박싱으로 NPE — if 로 나눈다
							Double diff = null;
							if (r.saved() != null && r.computed() != null) {
								if (pct) diff = r.computed() - r.saved();
								else if (r.saved() != 0) diff = (r.computed() - r.saved()) / Math.abs(r.saved()) * 100;
							}
							boolean off = diff != null && Math.abs(diff) > 1;
						}
						<tr class="${off ? "bg-warning/10" : ""}">
							<td>${label}</td>
							<td class="text-right font-mono">${fmt.apply(r.saved())}</td>
							<td class="text-right font-mono">${fmt.apply(r.computed())}</td>
							<td class="text-right font-mono ${off ? "text-warning" : "text-base-content/40"}">@if(diff == null)-@elseif(Math.abs(diff) < 0.05)0@elseif(pct)${String.format("%+.0f", diff)}${korean ? "%p" : " pts"}@else${String.format("%+.1f%%", diff)}@endif</td>
							@if(split)
								<td class="text-right font-mono" data-recalc-set1>${fmt.apply(r.set1())}</td>
								<td class="text-right font-mono" data-recalc-set2>${fmt.apply(r.set2())}</td>
							@endif
						</tr>
					@endfor
				</tbody>
			</table>
		</div>
		@if(split)
			<div class="text-xs text-base-content/60" data-recalc-split-note>${korean ? "무기 세트마다 패시브·무기가 달라 세트 1·2 를 따로 계산했습니다(PoB 는 켜진 세트 하나로만 계산). 주 세트 = DPS 가 큰 쪽." : "Passives and weapons differ per weapon set, so sets 1 and 2 are computed separately (PoB computes only the active set). Main set = higher DPS."}</div>
		@endif
		<div class="text-xs text-base-content/50">${korean ? "차이가 1% 넘는 줄은 코드를 만든 PoB 버전·설정이 이 서버 엔진과 달라서일 수 있습니다." : "Rows differing by over 1% may come from a different PoB version or configuration."} (${recalc.elapsedMs()}ms)</div>
	@endif
</section>
