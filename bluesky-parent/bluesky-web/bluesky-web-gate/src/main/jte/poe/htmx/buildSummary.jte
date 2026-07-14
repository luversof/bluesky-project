@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe.service.PoeBuild

@param PoeBuild build = null
@param boolean importError = false

@if(importError)
	<div class="alert alert-error text-sm">${MessageUtil.getMessage("poe.build.error")}</div>
@elseif(build != null)
	!{
		String classLine = build.classNameKo() + (build.ascendClassName().isEmpty() ? "" : " · " + build.ascendClassNameKo());
		String levelLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.build.summary.level"), build.level());
		String nodesLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.build.summary.tree.nodes"), build.passiveNodeIds().size());
		String nodesParam = String.join(",", build.passiveNodeIds().stream().map(String::valueOf).toList());
	}
	<div class="space-y-4">
		<%-- 캐릭터 요약 --%>
		@template._components.ui.card(bodyClass = "gap-3", body = @`
				<div class="flex flex-wrap items-center gap-3">
					<div class="text-lg font-bold">${classLine}</div>
					<span class="badge badge-primary badge-outline">${levelLabel}</span>
					@if(!build.treeVersion().isEmpty())
						<span class="badge badge-ghost">${MessageUtil.getMessage("poe.build.summary.tree")} ${build.treeVersion()}</span>
					@endif
					@if(!build.passiveNodeIds().isEmpty())
						<span class="badge badge-ghost">${nodesLabel}</span>
						<a class="link link-primary text-sm ml-auto" href="/poe/tree?nodes=${nodesParam}">${MessageUtil.getMessage("poe.build.summary.tree.view")}</a>
					@endif
				</div>
				@if(!build.stats().isEmpty())
					<div class="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-7 gap-2">
						@for(PoeBuild.PlayerStat stat : build.stats())
							<div class="rounded-box bg-base-200/60 px-3 py-2">
								<div class="text-[11px] font-semibold text-base-content/50">${MessageUtil.getMessage("poe.build.stat." + stat.key())}</div>
								<div class="text-sm font-bold font-mono">${stat.value()}</div>
							</div>
						@endfor
					</div>
				@endif
		`)

		<%-- 스킬 그룹 --%>
		@if(!build.skillGroups().isEmpty())
			@template._components.ui.card(bodyClass = "gap-3", body = @`
					<h2 class="text-sm font-bold text-base-content/70">${MessageUtil.getMessage("poe.build.skills")}</h2>
					<div class="space-y-2">
						@for(PoeBuild.SkillGroup skillGroup : build.skillGroups())
							<div class="flex flex-wrap items-center gap-1.5 rounded-box bg-base-200/40 px-3 py-2${skillGroup.enabled() ? "" : " opacity-45"}">
								@if(!skillGroup.slot().isEmpty())
									<span class="badge badge-ghost badge-sm shrink-0">${skillGroup.slotKo()}</span>
								@endif
								@if(!skillGroup.enabled())
									<span class="badge badge-sm shrink-0">${MessageUtil.getMessage("poe.build.disabled")}</span>
								@endif
								@for(PoeBuild.BuildGem gem : skillGroup.gems())
									@if(gem.slug() != null)
										<button type="button"
										        class="badge badge-sm cursor-pointer transition-colors ${gem.isSupport() ? "badge-info badge-outline hover:bg-info/10" : "badge-success badge-outline hover:bg-success/10"}"
										        hx-get="/poe/htmx/gems/detail?slug=${gem.slug()}&level=${gem.level()}"
										        hx-target="#poeBuildDetail"
										        hx-swap="innerHTML">${gem.nameKo() != null ? gem.nameKo() : gem.name()} <span class="opacity-60 font-mono">${gem.level()}/${gem.quality()}</span></button>
									@else
										<span class="badge badge-sm badge-ghost">${gem.name()} <span class="opacity-60 font-mono">${gem.level()}/${gem.quality()}</span></span>
									@endif
								@endfor
							</div>
						@endfor
					</div>
			`)
		@endif

		<%-- 장비 --%>
		@if(!build.items().isEmpty())
			@template._components.ui.card(bodyClass = "gap-3", body = @`
					<h2 class="text-sm font-bold text-base-content/70">${MessageUtil.getMessage("poe.build.items")}</h2>
					<div class="grid grid-cols-1 sm:grid-cols-2 gap-1.5">
						@for(PoeBuild.BuildItem item : build.items())
							!{
								String itemColor = "UNIQUE".equals(item.rarity()) || "RELIC".equals(item.rarity()) ? "text-orange-500"
									: "RARE".equals(item.rarity()) ? "text-yellow-500"
									: "MAGIC".equals(item.rarity()) ? "text-blue-400" : "";
								String detailUrl = item.uniqueSlug() != null ? "/poe/htmx/uniques/detail?slug=" + item.uniqueSlug()
									: item.baseSlug() != null ? "/poe/htmx/items/detail?slug=" + item.baseSlug() : null;
							}
							@if(detailUrl != null)
								<button type="button" class="flex items-center gap-2 rounded-box bg-base-200/40 px-3 py-2 text-left cursor-pointer hover:bg-base-200 transition-colors"
								        hx-get="${detailUrl}"
								        hx-target="#poeBuildDetail"
								        hx-swap="innerHTML">
									@if(!item.slot().isEmpty())
										<span class="badge badge-ghost badge-sm shrink-0 w-20 justify-center">${item.slotKo()}</span>
									@endif
									<span class="min-w-0 truncate text-sm">
										<span class="font-semibold ${itemColor}">${item.nameKo() != null ? item.nameKo() : item.name()}</span>
										@if(!item.baseType().equals(item.name()))
											<span class="text-xs text-base-content/50 ml-1">${item.baseTypeKo() != null ? item.baseTypeKo() : item.baseType()}</span>
										@endif
									</span>
								</button>
							@else
								<div class="flex items-center gap-2 rounded-box bg-base-200/40 px-3 py-2">
									@if(!item.slot().isEmpty())
										<span class="badge badge-ghost badge-sm shrink-0 w-20 justify-center">${item.slotKo()}</span>
									@endif
									<span class="min-w-0 truncate text-sm">
										<span class="font-semibold ${itemColor}">${item.name()}</span>
										@if(!item.baseType().equals(item.name()))
											<span class="text-xs text-base-content/50 ml-1">${item.baseTypeKo() != null ? item.baseTypeKo() : item.baseType()}</span>
										@endif
									</span>
								</div>
							@endif
						@endfor
					</div>
			`)
		@endif
	</div>
@endif
