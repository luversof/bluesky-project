@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe.dto.PoeBuild
@import net.luversof.web.gate.poe.dto.EngineResult

@param EngineResult engineResult = null
@param boolean engineError = false
@param boolean withMerc = false

@if(engineError)
	<div class="alert alert-error text-sm">${MessageUtil.getMessage("poe.build.engine.error")}</div>
@elseif(engineResult != null)
	<div class="space-y-2 rounded-box border border-primary/25 bg-primary/5 p-3">
		<div class="flex items-center gap-2">
			<span class="text-xs font-bold text-primary">${MessageUtil.getMessage("poe.build.engine.title")}</span>
			<span class="badge badge-ghost badge-sm font-mono">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.build.engine.duration"), engineResult.durationMs())}</span>
			@if(withMerc)
				<span class="badge badge-outline badge-sm">${MessageUtil.getMessage("poe.build.engine.withmerc")}</span>
			@endif
		</div>
		<div class="grid grid-cols-[repeat(auto-fit,minmax(min(8rem,100%),1fr))] sm:grid-cols-4 lg:grid-cols-7 gap-2">
			@for(PoeBuild.PlayerStat stat : engineResult.stats())
				<div class="rounded-box bg-base-100 px-3 py-2">
					!{ String statLabel = MessageUtil.getMessage("poe.build.stat." + stat.key()); }
					<%-- 키가 없으면 MessageUtil 은 "" — 빈 이름표 대신 영어 키(10-25 Q176: 흡혈 · 희귀도 칸이 숫자만 나왔다) --%>
					<div class="text-[11px] font-semibold text-base-content/60">${statLabel == null || statLabel.isBlank() ? stat.key() : statLabel}</div>
					<div class="text-sm font-bold font-mono">${stat.value()}</div>
				</div>
			@endfor
		</div>
	</div>
@endif
