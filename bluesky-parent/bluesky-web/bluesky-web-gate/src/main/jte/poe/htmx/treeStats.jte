@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe.dto.PoeBuild
@import net.luversof.web.gate.poe.dto.PoeTreeEvaluation

@param PoeTreeEvaluation treeEval = null
@param boolean treeEvalError = false

@if(treeEvalError)
	<div class="rounded border border-error/40 bg-error/10 px-3 py-2 text-xs text-error">${MessageUtil.getMessage("poe.tree.eval.error")}</div>
@elseif(treeEval != null)
	!{
		String gemLabel = net.luversof.web.gate.poe.PoeText.name(treeEval.gemNameKo(), treeEval.gemName());
		String classLabel = net.luversof.web.gate.poe.PoeText.name(treeEval.classNameKo(), treeEval.className());
		String meta = java.text.MessageFormat.format(
			MessageUtil.getMessage("poe.tree.eval.meta"), treeEval.nodeCount(), treeEval.durationMs() / 1000.0);
	}
	<div class="space-y-2">
		<div class="flex flex-wrap items-center gap-1.5 text-xs">
			<span class="badge badge-primary badge-outline badge-sm">${classLabel}${treeEval.ascendancy() != null ? " · " + treeEval.ascendancy() : ""}</span>
			<span class="badge badge-accent badge-outline badge-sm">${gemLabel}</span>
			<span class="text-base-content/50 font-mono">${meta}</span>
		</div>
		<div class="grid grid-cols-2 gap-1.5">
			@for(PoeBuild.PlayerStat stat : treeEval.stats())
				<div class="rounded bg-base-100/60 px-2 py-1">
					<div class="text-[10px] text-base-content/50">${MessageUtil.getMessage("poe.build.stat." + stat.key())}</div>
					<div class="text-xs font-bold font-mono">${stat.value()}</div>
				</div>
			@endfor
		</div>
		<div class="text-[10px] text-base-content/40 leading-4">${MessageUtil.getMessage("poe.tree.eval.hint")}</div>
	</div>
@endif
