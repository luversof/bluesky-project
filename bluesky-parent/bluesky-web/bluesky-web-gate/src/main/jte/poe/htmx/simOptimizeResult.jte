@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe.dto.PoeBuild
@import net.luversof.web.gate.poe.dto.PoeOptimizeResult

@param PoeOptimizeResult result = null

@if(result != null)
	!{
		String gemLabel = result.gemNameKo() != null ? result.gemNameKo() : result.gemName();
		String objectiveLabel = MessageUtil.getMessage("poe.sim.opt.objective." + result.objective());
		String nodesParam = String.join(",", result.treeNodeIds().stream().map(String::valueOf).toList());
		String treeLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.sim.opt.result.tree"), result.treeNodeIds().size());
		String durationLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.sim.opt.duration"), result.durationMs() / 1000, result.evalCount());
	}
	<div class="space-y-3 rounded-box border border-primary/25 bg-primary/5 p-4 mt-2">
		<div class="flex flex-wrap items-center gap-2">
			<span class="text-sm font-bold text-primary">${MessageUtil.getMessage("poe.sim.opt.result.title")}</span>
			<span class="font-bold">${gemLabel}</span>
			<span class="badge badge-primary badge-outline badge-sm">${result.classNameKo()}${result.ascendancyKo() != null ? " · " + result.ascendancyKo() : ""}</span>
			@if(result.bloodline() != null)
				<span class="badge badge-sm text-rose-500 border-rose-500/40">${MessageUtil.getMessage("poe.sim.opt.result.bloodline")}: ${net.luversof.web.gate.poe.PoeText.name(result.bloodlineKo(), result.bloodline())}</span>
			@endif
			<span class="badge badge-ghost badge-sm">${objectiveLabel}</span>
			@if(result.scenarioKo() != null)
				<span class="badge badge-error badge-outline badge-sm">${MessageUtil.getMessage("poe.sim.opt.scenario")}: ${result.scenarioKo()}</span>
			@endif
			@if(result.combatBuffs())
				<span class="badge badge-success badge-outline badge-sm">${MessageUtil.getMessage("poe.sim.opt.buffs")}</span>
			@endif
			<span class="badge badge-ghost badge-sm font-mono">${durationLabel}</span>
		</div>

		<div class="flex flex-wrap items-baseline gap-3">
			<span class="text-sm text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.baseline")} <span class="font-mono">${result.baselineValue()}</span></span>
			<span class="text-lg font-bold font-mono text-primary">→ ${result.finalValue()}</span>
		</div>

		@if(!result.stats().isEmpty())
			<div class="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-7 gap-2">
				@for(PoeBuild.PlayerStat stat : result.stats())
					<div class="rounded-box bg-base-100 px-3 py-2">
						<div class="text-[11px] font-semibold text-base-content/50">${MessageUtil.getMessage("poe.build.stat." + stat.key())}</div>
						<div class="text-sm font-bold font-mono">${stat.value()}</div>
					</div>
				@endfor
			</div>
		@endif

		@if(result.defenseHits() != null && !result.defenseHits().isEmpty())
			!{
				java.util.Map<String, String> defColor = java.util.Map.of(
					"physical", "text-base-content/70",
					"fire", "text-red-500",
					"cold", "text-sky-400",
					"lightning", "text-yellow-400",
					"chaos", "text-fuchsia-500");
			}
			<div class="space-y-1.5">
				<span class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.defense")}</span>
				<div class="grid grid-cols-3 sm:grid-cols-5 gap-2">
					@for(PoeOptimizeResult.DefenseHit hit : result.defenseHits())
						<div class="rounded-box bg-base-100 px-3 py-2">
							<div class="text-[11px] font-semibold ${defColor.getOrDefault(hit.type(), "text-base-content/50")}">${MessageUtil.getMessage("poe.sim.opt.result.defense." + hit.type())}</div>
							<div class="text-sm font-bold font-mono">${hit.value()}</div>
						</div>
					@endfor
				</div>
			</div>
		@endif

		@if(!result.supports().isEmpty())
			<div class="flex flex-wrap items-center gap-1.5">
				<span class="text-xs font-semibold text-base-content/50 mr-1">${MessageUtil.getMessage("poe.sim.opt.result.supports")}</span>
				@for(PoeOptimizeResult.SupportPick support : result.supports())
					<span class="badge badge-info badge-outline badge-sm">${net.luversof.web.gate.poe.PoeText.name(support.nameKo(), support.name())}</span>
				@endfor
			</div>
		@endif

		@if(!result.treeNodeIds().isEmpty())
			<div class="flex flex-wrap items-center gap-1.5">
				<span class="text-xs font-semibold text-base-content/50 mr-1">${treeLabel}</span>
				@for(String notable : result.treeNotables())
					<span class="badge badge-warning badge-outline badge-sm">${notable}</span>
				@endfor
				<a class="link link-primary text-sm ml-1" href="/poe/tree?nodes=${nodesParam}">${MessageUtil.getMessage("poe.build.summary.tree.view")}</a>
			</div>
		@endif

		@if(result.jewels() != null && !result.jewels().isEmpty())
			<div class="flex flex-wrap items-center gap-1.5">
				<span class="text-xs font-semibold text-base-content/50 mr-1">${MessageUtil.getMessage("poe.sim.opt.result.jewels")}</span>
				@for(PoeOptimizeResult.SupportPick jewel : result.jewels())
					<span class="badge badge-sm text-cyan-500 border-cyan-500/40">${net.luversof.web.gate.poe.PoeText.name(jewel.nameKo(), jewel.name())}</span>
				@endfor
			</div>
		@endif

		@if(!result.items().isEmpty())
			<div class="space-y-1.5">
				<span class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.items")}</span>
				<div class="flex flex-col gap-1">
					@for(PoeOptimizeResult.ItemPick item : result.items())
						<div class="flex flex-wrap items-baseline gap-x-2 gap-y-0.5 text-sm">
							<span class="badge badge-ghost badge-sm shrink-0 w-16 justify-center">${item.slotKo()}</span>
							@if("RARE".equals(item.rarity()))
								<span class="font-semibold text-yellow-500">${MessageUtil.getMessage("poe.sim.opt.result.rare")} · ${item.name()}</span>
								@if(!item.modLines().isEmpty())
									<span class="text-xs text-base-content/60">${String.join(" / ", item.modLines())}</span>
								@endif
							@else
								<span class="font-semibold text-orange-500">${net.luversof.web.gate.poe.PoeText.name(item.nameKo(), item.name())}</span>
								@if(!item.modLines().isEmpty())
									<span class="text-xs text-base-content/60">${String.join(" / ", item.modLines())}</span>
								@endif
							@endif
						</div>
					@endfor
				</div>
			</div>
		@endif

		@if(!result.tierComparisons().isEmpty())
			<div class="space-y-1.5">
				<span class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.tiers")}</span>
				<div class="overflow-x-auto rounded-box bg-base-100">
					<table class="table table-xs">
						<thead>
							<tr>
								<th>${MessageUtil.getMessage("poe.sim.opt.result.items")}</th>
								@for(PoeOptimizeResult.TierRow row : result.tierComparisons().get(0).rows())
									<th class="text-right">${row.label()}</th>
								@endfor
							</tr>
						</thead>
						<tbody>
							@for(PoeOptimizeResult.SlotTierCompare compare : result.tierComparisons())
								<tr>
									<td class="font-semibold">${compare.slotKo()}</td>
									@for(PoeOptimizeResult.TierRow row : compare.rows())
										<td class="text-right font-mono">${row.value()}</td>
									@endfor
								</tr>
							@endfor
						</tbody>
					</table>
				</div>
			</div>
		@endif

		@if(result.scenarioMatrix() != null && !result.scenarioMatrix().isEmpty())
			<div class="space-y-1.5">
				<span class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.matrix")}</span>
				<div class="overflow-x-auto rounded-box bg-base-100">
					<table class="table table-xs">
						<thead>
							<tr>
								<th>${MessageUtil.getMessage("poe.sim.opt.scenario")}</th>
								<th class="text-right">${MessageUtil.getMessage("poe.sim.opt.result.matrix.buffoff")}</th>
								<th class="text-right">${MessageUtil.getMessage("poe.sim.opt.result.matrix.buffon")}</th>
							</tr>
						</thead>
						<tbody>
							@for(PoeOptimizeResult.ScenarioCell cell : result.scenarioMatrix())
								<tr>
									<td class="font-semibold">${cell.scenarioKo()}</td>
									<td class="text-right font-mono text-base-content/60">${cell.dpsBuffOff()}</td>
									<td class="text-right font-mono text-primary">${cell.dpsBuffOn()}</td>
								</tr>
							@endfor
						</tbody>
					</table>
				</div>
			</div>
		@endif

		<div class="space-y-1">
			<div class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.code")}</div>
			<textarea readonly rows="3" class="textarea textarea-bordered w-full font-mono text-xs leading-5">${result.pobCode()}</textarea>
		</div>
	</div>
@endif
