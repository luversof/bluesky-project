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
				@if(ko)<span class="text-base-content/60 text-sm">${gem.name()}</span>@endif
				<span class="badge badge-ghost">${patchLabel}</span>
				@if(gem.isVaal())
					<%-- 바알 스킬은 브라우저 표시 전용 — 소울/지속 업타임 미모델링으로 최적화기에서 제외한다 --%>
					<span class="ml-auto text-[11px] px-2 py-1 rounded bg-[#d20000]/12 text-[#d20000] border border-[#d20000]/40" title="${ko ? "바알 스킬은 소울/지속 업타임을 모델링하지 않아 최적화기에서 제외됩니다" : "Vaal skills are excluded from the optimizer (souls/uptime not modeled)"}">${ko ? "바알 스킬 · 타락 (브라우저 전용)" : "Vaal skill · corrupted (browse only)"}</span>
				@elseif(!gem.isSupport())
					<a class="btn btn-sm btn-primary btn-outline ml-auto" href="/poe/sim?skills=${gem.slug()}">${MessageUtil.getMessage("poe.gems.optimize")}</a>
				@endif
			</div>
			<%-- 종류 · 색 배지 — PoE2 젬 상세와 같은 줄(10-12 C283) --%>
			<div class="flex flex-wrap gap-2 text-xs" data-gem-badges>
				<span class="badge badge-ghost">${MessageUtil.getMessage(gem.isSupport() ? "poe.gems.type.support" : "poe.gems.type.active")}</span>
				@if(gem.color() != null)<span class="badge badge-ghost">${MessageUtil.getMessage("poe.gems.color." + gem.color(), gem.color())}</span>@endif
			</div>

			<div class="flex flex-wrap gap-6 items-start">
				<%-- 젬 그림 — PoE2 젬 상세 · PoE1 고유/아이템 상세와 같은 자리(10-12 C283) --%>
				<img src="/poe-assets/gems/${gem.slug()}.png?v=${net.luversof.web.gate.poe.config.PoeIconVersion.current()}" class="max-h-64 max-w-40 object-contain drop-shadow-[0_2px_6px_rgba(0,0,0,0.5)]" alt="${gem.name()}" data-gem-art>
				<%-- 호버 레이어(툴팁)를 그대로 바닥에 — 레벨 전환은 ?level= 링크(pageMode) --%>
				<div class="shrink-0" data-gem-tooltip>
					@template.poe.htmx.gemDetail(gem = gem, displayLevel = displayLevel, pageMode = true)
				</div>

				<div class="flex-1 min-w-0 sm:min-w-[20rem] space-y-4">
					<%-- 레벨별 진행표 — 레이어에 없는 상세 정보 --%>
					<div class="rounded-2xl bg-base-100 p-4 shadow-sm">
						<h2 class="font-bold mb-2">${MessageUtil.getMessage("poe.gems.progression")}</h2>
						<%-- 값이 하나도 없는 열은 그리지 않는다 — PoE2 젬 상세와 같다(10-12 C283: 바알 젬 등에서 빈 열 셋이 표를 좁혀 "레벨" 머리가 두 줄로 접혔다) --%>
						!{
							boolean hasAttrs = gem.levels().stream().anyMatch(l -> net.luversof.web.gate.poe.PoeGemRequirements.stat(l.requiredLevel(), gem.isSupport(), gem.reqStrPct()) + net.luversof.web.gate.poe.PoeGemRequirements.stat(l.requiredLevel(), gem.isSupport(), gem.reqDexPct()) + net.luversof.web.gate.poe.PoeGemRequirements.stat(l.requiredLevel(), gem.isSupport(), gem.reqIntPct()) > 0);
							boolean hasCost = gem.levels().stream().anyMatch(l -> l.cost() != null && l.cost() > 0);
							boolean hasMult = gem.levels().stream().anyMatch(l -> l.baseMultiplier() != null || l.damageEffectiveness() != null);
							boolean hasCrit = gem.levels().stream().anyMatch(l -> l.critChance() != null);
						}
						<div class="overflow-x-auto">
							<table class="table table-xs">
								<thead>
									<tr>
										<th>${MessageUtil.getMessage("poe.gems.detail.level")}</th>
										<th>${MessageUtil.getMessage("poe.gems.detail.required.level")}</th>
										<%-- 레벨별 요구 능력치(10-09 C237 — poedb 레벨 표 열, PoB getGemStatRequirement = 툴팁 C225 와 같은 식) --%>
										@if(hasAttrs)<th>${MessageUtil.getMessage("poe.gems.col.attributes")}</th>@endif
										@if(hasCost)<th>${MessageUtil.getMessage("poe.gems.detail.cost")}</th>@endif
										@if(hasMult)<th>${MessageUtil.getMessage("poe.gems.col.multiplier")}</th>@endif
										@if(hasCrit)<th>${MessageUtil.getMessage("poe.gems.col.crit")}</th>@endif
										<th>${MessageUtil.getMessage("poe.gems.col.effects")}</th>
									</tr>
								</thead>
								<tbody>
									@for(PoeGemLevel l : gem.levels())
										<tr class="${l.level() == displayLevel ? "bg-primary/10" : ""}">
											<td class="font-mono">${l.level()}</td>
											<td class="font-mono">${l.requiredLevel()}</td>
											!{ int ar = net.luversof.web.gate.poe.PoeGemRequirements.stat(l.requiredLevel(), gem.isSupport(), gem.reqStrPct()); int ad = net.luversof.web.gate.poe.PoeGemRequirements.stat(l.requiredLevel(), gem.isSupport(), gem.reqDexPct()); int ai = net.luversof.web.gate.poe.PoeGemRequirements.stat(l.requiredLevel(), gem.isSupport(), gem.reqIntPct());
											   java.util.List<String> attrCells = new java.util.ArrayList<>(); if (ar > 0) attrCells.add(ar + " " + MessageUtil.getMessage("poe.items.detail.str")); if (ad > 0) attrCells.add(ad + " " + MessageUtil.getMessage("poe.items.detail.dex")); if (ai > 0) attrCells.add(ai + " " + MessageUtil.getMessage("poe.items.detail.int")); }
											@if(hasAttrs)<td class="font-mono whitespace-nowrap" data-gem-level-attrs>${String.join(", ", attrCells)}</td>@endif
											@if(hasCost)<td class="font-mono">@if(l.cost() != null && l.cost() > 0)${net.luversof.web.gate.poe.PoeText.costValue(l.cost(), l.costType())}@endif</td>@endif
											@if(hasMult)<td class="font-mono">@if(l.baseMultiplier() != null)${String.format("%.0f", l.baseMultiplier())}%@elseif(l.damageEffectiveness() != null)${String.format("%.0f", l.damageEffectiveness())}%@endif</td>@endif
											@if(hasCrit)<td class="font-mono">@if(l.critChance() != null)${String.format("%.2f", l.critChance())}%@endif</td>@endif
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
							<h2 class="font-bold mb-2">${MessageUtil.getMessage("poe.gems.quality.title")}</h2>
							@for(String line : displayQuality)
								<div class="text-sm text-[#8888ff]">$unsafe{PoeText.highlightValues(line)}</div>
							@endfor
						</div>
					@endif

					!{java.util.List<String> displayTags = PoeText.lines(gem.tagsKo(), gem.tags());}
					@if(displayTags != null && !displayTags.isEmpty())
						<div class="rounded-2xl bg-base-100 p-4 shadow-sm">
							<h2 class="font-bold mb-2">${MessageUtil.getMessage("poe.gems.tags")}</h2>
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
