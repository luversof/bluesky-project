@import java.util.List
@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param Poe2.TreeEval eval = null
@param String error = null

!{
	boolean ko = PoeText.isKorean();
}
<%-- 트리 계산 결과 — 레벨 90 캐릭터에 찍은 노드만(장비 없음, 스킬을 고르면 시뮬 랭킹과 같은 표준 무기). 스탯 이름은 빌드 재계산과 같은 메시지 키(10-01) --%>
@if(error != null)
	<div class="text-xs text-warning px-1 py-2" data-tree-eval-error>${"noclass".equals(error) ? (ko ? "먼저 직업을 고르세요." : "Pick a class first.") : (ko ? "계산할 수 없는 트리입니다." : "This tree can't be evaluated.")}</div>
@elseif(eval != null && eval.error() != null)
	<div class="text-xs text-warning px-1 py-2" data-tree-eval-error>${ko ? "엔진 계산 실패: " : "Engine failed: "}${eval.error()}</div>
@elseif(eval != null)
	<div class="text-[11px] text-stone-400 px-1 pb-1" data-tree-eval-meta>
		${ko ? "레벨 90 · 노드 " : "Level 90 · nodes "}${eval.nodes()}@if(eval.skill() != null) · ${eval.skill()}@endif · ${eval.elapsedMs()}ms
	</div>
	!{
		boolean twoSets = eval.rowsSet2() != null;
		java.util.Map<String, Double> set2 = new java.util.HashMap<>();
		if (twoSets) for (Poe2.TreeEvalRow r2 : eval.rowsSet2()) set2.put(r2.key(), r2.value());
	}
	<table class="table table-xs" data-tree-eval>
		@if(twoSets)
			<%-- 무기 세트 전용 노드가 있으면 세트 I · II 를 켠 계산을 나란히(10-02, 빌드 재계산과 같은 방식) --%>
			<thead><tr><th></th><th class="text-right text-[#e0803a]" data-eval-set="1">${ko ? "세트 I" : "Set I"}</th><th class="text-right text-[#4aa3df]" data-eval-set="2">${ko ? "세트 II" : "Set II"}</th></tr></thead>
		@endif
		<tbody>
			@for(Poe2.TreeEvalRow r : eval.rows())
				!{
					String key = r.key().toLowerCase(java.util.Locale.ROOT);
					String label = "spirit".equals(key) ? MessageUtil.getMessage("poe2.build.stat.spirit") : MessageUtil.getMessage("poe.build.stat." + key, r.key());
					double v = r.value() == null ? 0 : r.value();
					String shown = key.endsWith("resist") ? String.format("%.0f%%", v)
						: key.contains("dps") ? (v >= 1_000_000 ? String.format("%.2fM", v / 1_000_000) : v >= 10_000 ? String.format("%.1fk", v / 1_000) : String.format("%,.0f", v))
						: key.contains("movement") ? String.format("%.2f", v)
						: String.format("%,.0f", v);
				}
				@if(!(key.contains("dps") && eval.skill() == null))
					!{
						String shown2 = "";
						if (twoSets && set2.get(r.key()) != null) {
							double v2 = set2.get(r.key());
							shown2 = key.endsWith("resist") ? String.format("%.0f%%", v2)
								: key.contains("dps") ? (v2 >= 1_000_000 ? String.format("%.2fM", v2 / 1_000_000) : v2 >= 10_000 ? String.format("%.1fk", v2 / 1_000) : String.format("%,.0f", v2))
								: key.contains("movement") ? String.format("%.2f", v2)
								: String.format("%,.0f", v2);
						}
					}
					<tr data-eval-key="${r.key()}"><td class="text-stone-300">${label}</td><td class="text-right font-mono text-sky-300">${shown}</td>@if(twoSets)<td class="text-right font-mono text-sky-300" data-eval-set2>${shown2}</td>@endif</tr>
				@endif
			@endfor
		</tbody>
	</table>
	@if(eval.skill() == null)
		<div class="text-[10px] text-stone-500 px-1 pt-1">${ko ? "스킬을 고르면 표준 무기로 그 스킬 DPS 도 계산합니다." : "Pick a skill to also compute its DPS with a standard weapon."}</div>
	@endif
@endif
