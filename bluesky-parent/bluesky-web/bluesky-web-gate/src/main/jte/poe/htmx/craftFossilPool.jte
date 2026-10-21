@import java.util.List
@import net.luversof.web.gate.poe.dto.PoeCraft
@import net.luversof.web.gate.poe.PoeText
@param int fossilPicked = 0
@param List<PoeCraft.PoolLine> fossilLines = List.of()
<%-- 화석 조합 기준 첫 옵션 확률(10-20 C504, craftofexile 화석 옵션 풀) — 계열마다 한 줄(등급 합계 · 최상위 문구), 확률 높은 순 30계열, 접두 · 접미 합계 --%>
!{
	boolean ko = PoeText.isKorean();
	java.util.function.Function<java.util.List<String>, java.util.List<String>> nz = x -> x == null ? java.util.List.of() : x;
	double preSum = fossilLines.stream().filter(p -> "prefix".equals(p.gen())).mapToDouble(PoeCraft.PoolLine::chance).sum();
	// 같은 계열(등급 사다리)은 한 줄 — 합계 확률, 문구는 최상위 등급, 등급 범위 표시
	java.util.LinkedHashMap<String, java.util.List<PoeCraft.PoolLine>> fams = new java.util.LinkedHashMap<>();
	for (PoeCraft.PoolLine p : fossilLines) fams.computeIfAbsent(p.gen() + ":" + p.family(), k -> new java.util.ArrayList<>()).add(p);
	java.util.List<java.util.List<PoeCraft.PoolLine>> groups = new java.util.ArrayList<>(fams.values());
	groups.sort(java.util.Comparator.comparingDouble((java.util.List<PoeCraft.PoolLine> g) -> -g.stream().mapToDouble(PoeCraft.PoolLine::chance).sum()));
}
@if(fossilPicked == 0)
	<p class="text-xs text-base-content/70" data-craft-fossil-pool-empty>${ko ? "화석을 하나 이상 고르세요." : "Pick at least one fossil."}</p>
@elseif(fossilLines.isEmpty())
	<p class="text-xs text-error" data-craft-fossil-pool-empty>${ko ? "이 조합으로 붙을 수 있는 옵션이 없습니다." : "No modifier can roll with this combination."}</p>
@else
	<div class="text-xs text-base-content/70 mb-1">${ko ? "첫 옵션 확률(분열 옵션은 남기고 레어로 다시 굴릴 때) · 접두 " : "First-modifier odds (reforged as rare, fractured kept) · prefixes "}${String.format("%.1f%%", preSum * 100)} · ${ko ? "접미 " : "suffixes "}${String.format("%.1f%%", (1 - preSum) * 100)}</div>
	<div class="overflow-x-auto max-h-[22rem] overflow-y-auto rounded-box border border-base-300">
		<table class="table table-xs">
			<thead class="sticky top-0 bg-base-200"><tr><th class="text-right">${ko ? "확률" : "Chance"}</th><th>${ko ? "자리" : "Slot"}</th><th>${ko ? "옵션" : "Modifier"}</th></tr></thead>
			<tbody>
				@for(java.util.List<PoeCraft.PoolLine> g : groups.subList(0, Math.min(30, groups.size())))
					!{
						PoeCraft.PoolLine p = g.stream().min(java.util.Comparator.comparing((PoeCraft.PoolLine x) -> x.tierRank() == null ? 99 : x.tierRank())).orElse(g.get(0));
						int lowT = g.stream().mapToInt(x -> x.tierRank() == null ? 0 : x.tierRank()).max().orElse(0);
						double gs = g.stream().mapToDouble(PoeCraft.PoolLine::chance).sum();
						java.util.List<String> mx = ko && !nz.apply(p.ko()).isEmpty() ? p.ko() : nz.apply(p.en());
						java.util.List<String> mn = ko && !nz.apply(p.koMin()).isEmpty() ? p.koMin() : nz.apply(p.enMin());
					}
					<tr data-craft-fossil-pool-line="${p.id()}" data-craft-fossil-pool-tiers="${g.size()}">
						<td class="text-right font-mono whitespace-nowrap">${String.format("%.2f%%", gs * 100)}</td>
						<td class="font-mono text-[10px] whitespace-nowrap">${"prefix".equals(p.gen()) ? (ko ? "접두" : "P") : (ko ? "접미" : "S")}@if(p.tierRank() != null) T${p.tierRank()}@if(lowT > p.tierRank())~T${lowT}@endif@endif</td>
						<td class="min-w-0 wrap-anywhere">@for(int i = 0; i < mx.size(); i++)$unsafe{PoeText.highlightValues(net.luversof.web.gate.poe.PoeModRange.range(i < mn.size() ? mn.get(i) : null, mx.get(i)))}@if(i < mx.size() - 1)<br>@endif@endfor</td>
					</tr>
				@endfor
			</tbody>
		</table>
	</div>
	@if(groups.size() > 30)<div class="text-[11px] text-base-content/60 mt-1">${ko ? "외 " + (groups.size() - 30) + "계열(확률 낮은 순)" : "and " + (groups.size() - 30) + " more families"}</div>@endif
@endif
