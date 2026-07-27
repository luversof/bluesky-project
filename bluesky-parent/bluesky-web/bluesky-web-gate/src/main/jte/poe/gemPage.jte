@import net.luversof.web.gate.poe.dto.PoeGem
@import net.luversof.web.gate.poe.dto.PoeGemLevel
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param PoeGem gem
@param int displayLevel = 20

!{
	String displayName = PoeText.name(gem.nameKo(), gem.name());
	String patchLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.patch"), patch);
	boolean ko = PoeText.isKorean();
}

@template._layout.poeLayout(
	title = displayName,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentPoe = @`
		<div class="space-y-4">
			<div class="flex flex-wrap items-baseline gap-3">
				<a href="/poe" class="link link-hover text-sm text-base-content/60">← ${MessageUtil.getMessage("poe.gems.title")}</a>
				<h1 class="text-2xl font-bold">${displayName}</h1>
				@if(ko)<span class="text-base-content/40 text-sm">${gem.name()}</span>@endif
				<span class="badge badge-ghost">${patchLabel}</span>
				@if(!gem.isSupport())
					<a class="btn btn-sm btn-primary btn-outline ml-auto" href="/poe/sim?skills=${gem.slug()}">${net.luversof.web.gate.poe.PoeText.isKorean() ? "이 스킬로 최적화 →" : "Optimize with this skill →"}</a>
				@endif
			</div>

			<div class="flex flex-wrap gap-6 items-start">
				<%-- 호버 레이어(툴팁)를 그대로 바닥에 — 레벨 전환은 ?level= 링크(pageMode) --%>
				<div class="shrink-0" data-gem-tooltip>
					@template.poe.htmx.gemDetail(gem = gem, displayLevel = displayLevel, pageMode = true)
				</div>

				<div class="flex-1 min-w-[20rem] space-y-4">
					<%-- 레벨별 진행표 — 레이어에 없는 상세 정보 --%>
					<div class="rounded-2xl bg-base-100 p-4 shadow-sm">
						<h2 class="font-bold mb-2">${ko ? "레벨별 진행" : "Level progression"}</h2>
						<div class="overflow-x-auto">
							<table class="table table-xs">
								<thead>
									<tr>
										<th>${ko ? "레벨" : "Level"}</th>
										<th>${ko ? "요구 레벨" : "Req. level"}</th>
										<th>${ko ? "소모" : "Cost"}</th>
										<th>${ko ? "피해 배율" : "Multiplier"}</th>
										<th>${ko ? "치명타" : "Crit"}</th>
										<th>${ko ? "부여 효과" : "Grants"}</th>
									</tr>
								</thead>
								<tbody>
									@for(PoeGemLevel l : gem.levels())
										<tr class="${l.level() == displayLevel ? "bg-primary/10" : ""}">
											<td class="font-mono">${l.level()}</td>
											<td class="font-mono">${l.requiredLevel()}</td>
											<td class="font-mono">@if(l.cost() != null && l.cost() > 0)${l.cost()} ${l.costType()}@endif</td>
											<td class="font-mono">@if(l.baseMultiplier() != null)${String.format("%.0f", l.baseMultiplier())}%@elseif(l.damageEffectiveness() != null)${String.format("%.0f", l.damageEffectiveness())}%@endif</td>
											<td class="font-mono">@if(l.critChance() != null)${String.format("%.2f", l.critChance())}%@endif</td>
											<td class="text-xs whitespace-pre-line">${String.join("\n", PoeText.lines(l.statLinesKo(), l.statLines()))}</td>
										</tr>
									@endfor
								</tbody>
							</table>
						</div>
					</div>

					!{java.util.List<String> displayQuality = PoeText.lines(gem.qualityStatLinesKo(), gem.qualityStatLines());}
					@if(displayQuality != null && !displayQuality.isEmpty())
						<div class="rounded-2xl bg-base-100 p-4 shadow-sm">
							<h2 class="font-bold mb-2">${ko ? "퀄리티 효과 (20% 기준)" : "Quality (at 20%)"}</h2>
							@for(String line : displayQuality)
								<div class="text-sm text-[#8888ff]">$unsafe{PoeText.highlightValues(line)}</div>
							@endfor
						</div>
					@endif

					!{java.util.List<String> displayTags = PoeText.lines(gem.tagsKo(), gem.tags());}
					@if(displayTags != null && !displayTags.isEmpty())
						<div class="rounded-2xl bg-base-100 p-4 shadow-sm">
							<h2 class="font-bold mb-2">${ko ? "태그" : "Tags"}</h2>
							<div class="flex flex-wrap gap-1.5">
								@for(String tag : displayTags)
									<span class="badge badge-outline badge-sm">${tag}</span>
								@endfor
							</div>
						</div>
					@endif
				</div>
			</div>
		</div>
	`
)
