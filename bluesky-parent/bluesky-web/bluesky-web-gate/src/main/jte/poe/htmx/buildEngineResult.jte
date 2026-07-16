@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe.dto.PoeBuild
@import net.luversof.web.gate.poe.dto.EngineResult

@param EngineResult engineResult = null
@param boolean engineError = false

@if(engineError)
	<div class="alert alert-error text-sm">${MessageUtil.getMessage("poe.build.engine.error")}</div>
@elseif(engineResult != null)
	<div class="space-y-2 rounded-box border border-primary/25 bg-primary/5 p-3">
		<div class="flex items-center gap-2">
			<span class="text-xs font-bold text-primary">${MessageUtil.getMessage("poe.build.engine.title")}</span>
			<span class="badge badge-ghost badge-sm font-mono">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.build.engine.duration"), engineResult.durationMs())}</span>
		</div>
		<div class="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-7 gap-2">
			@for(PoeBuild.PlayerStat stat : engineResult.stats())
				<div class="rounded-box bg-base-100 px-3 py-2">
					<div class="text-[11px] font-semibold text-base-content/50">${MessageUtil.getMessage("poe.build.stat." + stat.key())}</div>
					<div class="text-sm font-bold font-mono">${stat.value()}</div>
				</div>
			@endfor
		</div>
	</div>
@endif
