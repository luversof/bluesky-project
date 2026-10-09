@import java.util.List
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText

@param List<Poe2.SimGemRank> ranking = java.util.List.of()
@param String rankingPatch = ""
@param java.util.List<String> seasons = java.util.List.of()
@param String season = ""
@param String against = ""
@param String againstPatch = ""
@param java.util.Map<String, net.luversof.web.gate.poe.RankCompare.Change> changes = java.util.Map.of()

<%-- PoE2 젬 DPS 랭킹 표 — PoE1 poe/htmx/simRanking.jte 를 그대로 옮겼다(같은 메시지 키·열). PoE1 과 다른 점: 젬 색이 str/dex/int,
     행을 누르면 PoE2 젬 상세(#poe2GemDetail). --%>
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

<div data-ranking-root>
@if(!seasons.isEmpty())
	@template.poe.rankSeasonBar(seasons = seasons, season = season, against = against, url = "/poe2/htmx/sim/ranking", againstPatch = againstPatch)
@endif
@if(ranking.isEmpty())
	@template._components.ui.emptyState(message = emptyLabel)
@else
	!{ boolean comparing = !against.isBlank(); boolean ko = PoeText.isKorean(); }
	<div class="space-y-2" data-sim-ranking>
		<div class="text-xs text-base-content/60">${basisLabel}</div>
		<div class="overflow-x-auto rounded-2xl bg-base-100 shadow-sm">
			<table class="table table-sm">
				<thead>
					<tr>
						<th class="w-14 text-right">${rankLabel}</th>
						@if(comparing)
							<th class="w-14 text-center whitespace-nowrap" title="${ko ? "비교 시즌 대비 순위 변화" : "Rank change vs compared season"}">${ko ? "변화" : "Δ"}</th>
							<th class="w-20 text-right whitespace-nowrap">${ko ? "DPS 변화" : "DPS Δ"}</th>
						@endif
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
							Poe2.SimGemRank rankEntry = ranking.get(rankIndex0);
							int rankIndex = rankIndex0 + 1;
							String gemColor = "str".equals(rankEntry.color()) ? "#d20000" : "dex".equals(rankEntry.color()) ? "#00a000" : "int".equals(rankEntry.color()) ? "#0064e0" : "#aaaaaa";
						}
						<tr class="cursor-pointer hover:bg-base-200/60 transition-colors"
						    hx-get="/poe2/htmx/gems/detail?slug=${rankEntry.slug()}"
						    hx-target="#poe2GemDetail"
						    hx-swap="innerHTML">
							<td class="text-right font-mono text-base-content/60">${rankIndex}</td>
							@template.poe.rankChangeCells(change = changes.get(rankEntry.slug()), comparing = comparing)
							<td>
								<span class="inline-block w-2 h-2 rounded-full align-middle mr-1.5" style="background:${gemColor}"></span>
								<span class="font-semibold">${PoeText.name(rankEntry.nameKo(), rankEntry.name())}</span>
								<span class="text-xs text-base-content/60 ml-1">${rankEntry.name()}</span>
							</td>
							<td class="hidden sm:table-cell text-xs text-base-content/60">${rankEntry.weapon() != null ? rankEntry.weapon() : "-"}</td>
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
</div>
