@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe.service.PoeBuild
@import net.luversof.web.gate.poe.service.PoeOptimizeResult

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
			<span class="badge badge-primary badge-outline badge-sm">${result.classNameKo()}</span>
			<span class="badge badge-ghost badge-sm">${objectiveLabel}</span>
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

		@if(!result.supports().isEmpty())
			<div class="flex flex-wrap items-center gap-1.5">
				<span class="text-xs font-semibold text-base-content/50 mr-1">${MessageUtil.getMessage("poe.sim.opt.result.supports")}</span>
				@for(PoeOptimizeResult.SupportPick support : result.supports())
					<span class="badge badge-info badge-outline badge-sm">${support.nameKo() != null ? support.nameKo() : support.name()}</span>
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

		@if(!result.items().isEmpty())
			<div class="flex flex-wrap items-center gap-1.5">
				<span class="text-xs font-semibold text-base-content/50 mr-1">${MessageUtil.getMessage("poe.sim.opt.result.items")}</span>
				@for(PoeOptimizeResult.ItemPick item : result.items())
					<span class="badge badge-sm text-orange-500 border-orange-500/40">${item.slotKo()}: ${item.nameKo() != null ? item.nameKo() : item.name()}</span>
				@endfor
			</div>
		@endif

		<div class="space-y-1">
			<div class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.code")}</div>
			<textarea readonly rows="3" class="textarea textarea-bordered w-full font-mono text-xs leading-5">${result.pobCode()}</textarea>
		</div>
	</div>
@endif
