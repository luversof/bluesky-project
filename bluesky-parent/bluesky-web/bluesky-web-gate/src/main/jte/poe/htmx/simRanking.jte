@import java.util.List
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe.dto.PoeGemRank

@param List<PoeGemRank> ranking = java.util.List.of()
@param String rankingPatch = ""

!{
	String emptyLabel = MessageUtil.getMessage("poe.sim.empty");
	String rankLabel = MessageUtil.getMessage("poe.sim.col.rank");
	String gemLabel = MessageUtil.getMessage("poe.sim.col.gem");
	String weaponLabel = MessageUtil.getMessage("poe.sim.col.weapon");
	String dpsLabel = MessageUtil.getMessage("poe.sim.col.dps");
	String avgLabel = MessageUtil.getMessage("poe.build.stat.averagedamage");
	String speedLabel = MessageUtil.getMessage("poe.build.stat.speed");
	String basisLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.sim.basis"), rankingPatch);
}

@if(ranking.isEmpty())
	@template._components.ui.emptyState(message = emptyLabel)
@else
	<div class="space-y-2">
		<div class="text-xs text-base-content/50">${basisLabel}</div>
		<div class="overflow-x-auto rounded-2xl bg-base-100 shadow-sm">
			<table class="table table-sm">
				<thead>
					<tr>
						<th class="w-14 text-right">${rankLabel}</th>
						<th>${gemLabel}</th>
						<th class="hidden sm:table-cell">${weaponLabel}</th>
						<th class="text-right">${dpsLabel}</th>
						<th class="text-right hidden md:table-cell">${avgLabel}</th>
						<th class="text-right hidden md:table-cell">${speedLabel}</th>
					</tr>
				</thead>
				<tbody>
					@for(int rankIndex0 = 0; rankIndex0 < ranking.size(); rankIndex0++)
						!{
							PoeGemRank rankEntry = ranking.get(rankIndex0);
							int rankIndex = rankIndex0 + 1;
							String gemColor = "red".equals(rankEntry.color()) ? "#d20000" : "green".equals(rankEntry.color()) ? "#00a000" : "blue".equals(rankEntry.color()) ? "#0064e0" : "#aaaaaa";
						}
						<tr class="cursor-pointer hover:bg-base-200/60 transition-colors"
						    hx-get="/poe/htmx/gems/detail?slug=${rankEntry.slug()}"
						    hx-target="#poeGemDetail"
						    hx-swap="innerHTML">
							<td class="text-right font-mono text-base-content/50">${rankIndex}</td>
							<td>
								<span class="inline-block w-2 h-2 rounded-full align-middle mr-1.5" style="background:${gemColor}"></span>
								<span class="font-semibold">${net.luversof.web.gate.poe.PoeText.name(rankEntry.nameKo(), rankEntry.name())}</span>
								<span class="text-xs text-base-content/40 ml-1">${rankEntry.name()}</span>
							</td>
							<td class="hidden sm:table-cell text-xs text-base-content/50">${rankEntry.weapon() != null ? rankEntry.weapon() : "-"}</td>
							<td class="text-right font-mono font-bold">${String.format("%,.0f", rankEntry.dps())}</td>
							<td class="text-right font-mono hidden md:table-cell">${String.format("%,.0f", rankEntry.averageDamage())}</td>
							<td class="text-right font-mono hidden md:table-cell">${String.format("%.2f", rankEntry.speed())}</td>
						</tr>
					@endfor
				</tbody>
			</table>
		</div>
	</div>
@endif
