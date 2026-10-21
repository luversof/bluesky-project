@import net.luversof.web.gate.poe.dto.PoeCraft
@param PoeCraft.DistResult dist = null
@param java.util.Map<String, String> distLabels = java.util.Map.of()
@param String distAct = "CHAOS"
<%-- 결과 분포(10-21 C533, craftofexile 대량 모의) — 고른 화폐를 지금 아이템에 여러 번 따로 써서 계열마다 붙는 비율 · 그중 1등급 · 옵션 개수 --%>
!{ boolean ko = net.luversof.web.gate.poe.PoeText.isKorean(); }
<div id="craft-dist-result" class="space-y-1 text-xs" data-craft-dist>
	@if(dist == null)
	@elseif(dist.error() != null)
		<p class="text-error" data-craft-dist-error>${dist.error()}</p>
	@else
		<p class="text-base-content/70" data-craft-dist-samples="${dist.samples()}">${ko ? dist.samples() + "번 모의 · 결과에 그 계열이 있는 비율(한 아이템에서 한 번만 셈) · T1 = 그중 1등급" : dist.samples() + " runs · share of results with that family (counted once per item) · T1 = top tier"}</p>
		@if(dist.affixes() != null && !dist.affixes().isEmpty())
			<p class="text-base-content/70" data-craft-dist-affixes>${ko ? "옵션 개수: " : "Modifier count: "}@for(var e : dist.affixes().entrySet())<span class="mr-2">${e.getKey()}${ko ? "개 " : " · "}${String.format("%.1f%%", e.getValue() * 100)}</span>@endfor</p>
		@endif
		<div class="overflow-x-auto max-h-80 overflow-y-auto">
			<table class="table table-xs">
				<thead><tr><th>${ko ? "쪽" : "Side"}</th><th>${ko ? "옵션" : "Modifier"}</th><th class="text-right">${ko ? "붙을 확률" : "Share"}</th><th class="text-right">T1</th></tr></thead>
				<tbody>
					@for(int i = 0; i < Math.min(40, dist.lines().size()); i++)
						!{ PoeCraft.DistLine l = dist.lines().get(i); String lab = distLabels.get(l.gen() + "|" + l.family()); }
						<tr data-craft-dist-line="${l.gen()}:${l.family()}">
							<td class="whitespace-nowrap">${"prefix".equals(l.gen()) ? (ko ? "접두" : "P") : (ko ? "접미" : "S")}</td>
							<td class="min-w-0 wrap-anywhere">$unsafe{net.luversof.web.gate.poe.PoeText.highlightValues(lab == null ? l.family() : lab)}</td>
							<td class="text-right font-mono" data-craft-dist-share>${String.format(l.share() < 0.01 ? "%.2f%%" : "%.1f%%", l.share() * 100)}</td>
							<td class="text-right font-mono">${String.format(l.top() < 0.01 ? "%.2f%%" : "%.1f%%", l.top() * 100)}</td>
						</tr>
					@endfor
				</tbody>
			</table>
		</div>
	@endif
</div>
