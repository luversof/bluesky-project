@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe.dto.PoeBuild

@param PoeBuild build = null
@param boolean importError = false
@param boolean engineAvailable = false

@if(importError)
	<div class="alert alert-error text-sm">${MessageUtil.getMessage("poe.build.error")}</div>
@elseif(build != null)
	!{
		String classLine = net.luversof.web.gate.poe.PoeText.name(build.classNameKo(), build.className())
			+ (build.ascendClassName().isEmpty() ? "" : " · " + net.luversof.web.gate.poe.PoeText.name(build.ascendClassNameKo(), build.ascendClassName()));
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
				@if(engineAvailable)
					<div class="flex items-center gap-2">
						<button type="button" class="btn btn-sm btn-outline btn-primary"
						        hx-post="/poe/htmx/build/recalc"
						        hx-include="#poeBuildImport textarea"
						        hx-target="#poeEngineResult"
						        hx-swap="innerHTML"
						        hx-disabled-elt="this">${MessageUtil.getMessage("poe.build.engine.run")}</button>
						<span class="text-xs text-base-content/50">${MessageUtil.getMessage("poe.build.engine.hint")}</span>
					</div>
					<div id="poeEngineResult"></div>
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
									<span class="badge badge-ghost badge-sm shrink-0">${net.luversof.web.gate.poe.PoeText.name(skillGroup.slotKo(), skillGroup.slot())}</span>
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
										        hx-swap="innerHTML">${net.luversof.web.gate.poe.PoeText.name(gem.nameKo(), gem.name())} <span class="opacity-60 font-mono">${gem.level()}/${gem.quality()}</span></button>
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
								boolean isUnique = "UNIQUE".equals(item.rarity()) || "RELIC".equals(item.rarity());
								String itemColor = isUnique ? "text-orange-500"
									: "RARE".equals(item.rarity()) ? "text-yellow-500"
									: "MAGIC".equals(item.rarity()) ? "text-blue-400" : "";
								String tipClass = isUnique ? "poe-rar-unique" : "RARE".equals(item.rarity()) ? "poe-rar-rare"
									: "MAGIC".equals(item.rarity()) ? "poe-rar-magic" : "poe-rar-white";
								String tipColor = isUnique ? "#af6025" : "RARE".equals(item.rarity()) ? "#ffff77"
									: "MAGIC".equals(item.rarity()) ? "#8888ff" : "#c8c8c8";
								String displayName = net.luversof.web.gate.poe.PoeText.name(item.nameKo(), item.name());
								String displayBase = net.luversof.web.gate.poe.PoeText.name(item.baseTypeKo(), item.baseType());
								java.util.List<String> displayMods = net.luversof.web.gate.poe.PoeText.lines(item.modLinesKo(), item.modLines());
									boolean showMods = !isUnique && displayMods != null && !displayMods.isEmpty();
								String itemClassLabel = item.base() != null ? net.luversof.web.gate.poe.PoeText.name(item.base().itemClassKo(), item.base().itemClass()) : null;
							}
							@if(isUnique && item.uniqueSlug() != null)
								<div class="poe-hover flex items-start gap-2 rounded-box bg-base-200/40 px-3 py-2 text-left cursor-help hover:bg-base-200 transition-colors"
								     hx-get="/poe/htmx/uniques/detail?slug=${item.uniqueSlug()}"
								     hx-target="#poePreview"
								     hx-trigger="mouseenter delay:100ms"
								     hx-swap="innerHTML">
									@if(!item.slot().isEmpty())
										<span class="badge badge-ghost badge-sm shrink-0 w-20 justify-center mt-0.5">${net.luversof.web.gate.poe.PoeText.name(item.slotKo(), item.slot())}</span>
									@endif
									<div class="min-w-0 text-sm truncate"><span class="font-semibold ${itemColor}">${displayName}</span>@if(!item.baseType().equals(item.name()))<span class="text-xs text-base-content/50 ml-1">${displayBase}</span>@endif</div>
								</div>
							@else
								<div class="poe-hover flex items-start gap-2 rounded-box bg-base-200/40 px-3 py-2 text-left cursor-help hover:bg-base-200 transition-colors">
									@if(!item.slot().isEmpty())
										<span class="badge badge-ghost badge-sm shrink-0 w-20 justify-center mt-0.5">${net.luversof.web.gate.poe.PoeText.name(item.slotKo(), item.slot())}</span>
									@endif
									<div class="min-w-0 text-sm">
										<div class="truncate"><span class="font-semibold ${itemColor}">${item.name()}</span>@if(!item.baseType().equals(item.name()))<span class="text-xs text-base-content/50 ml-1">${displayBase}</span>@endif</div>
										@if(showMods)
											<div class="text-[11px] leading-4 text-[#8888ff]/80 line-clamp-3">${String.join(" / ", displayMods)}</div>
										@endif
									</div>
									<div data-poe-tip class="hidden">
										@template.poe.htmx.itemTooltip(rarityClass = tipClass, nameColor = tipColor, rarityBorder = tipColor, name = item.name(), base = displayBase, itemClass = itemClassLabel, baseItem = item.base(), modLines = displayMods)
									</div>
								</div>
							@endif
							@endfor
					</div>
			`)
		@endif
	</div>
@endif
