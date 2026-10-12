@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param Poe2.Meta meta = null
@param Poe2.Gem gem
@param Integer level = null
@param boolean simulatable = false

!{ String title = PoeText.name(gem.nameKo(), gem.name()); }

<%-- PoE2 젬 상세 — 호버 툴팁을 바닥에 두고, 목록으로 돌아가는 길을 둔다. --%>
@template._layout.poeLayout(
	title = title,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	menu = "poe2",
	contentPoe = @`
		<div class="space-y-4">
			@template.poe2.header(title = title, meta = meta, backHref = "/poe2", backLabel = MessageUtil.getMessage("poe2.gems.title"),
				subtitle = PoeText.isKorean() ? gem.name() : null, actions = @`
			@if(simulatable)
				<%-- PoE1 젬 상세와 같은 바로가기 — 시뮬레이터가 이 스킬을 고른 채 열린다(10-01) --%>
				<a class="btn btn-sm btn-primary btn-outline" href="/poe2/sim?skill=${java.net.URLEncoder.encode(gem.name(), java.nio.charset.StandardCharsets.UTF_8)}" data-optimize-skill>${MessageUtil.getMessage("poe.gems.optimize")}</a>
			@endif
			`)
			<div class="flex flex-wrap gap-2 text-xs">
				<span class="badge badge-ghost">${MessageUtil.getMessage("poe2.gems.kind." + gem.kind())}</span>
				<span class="badge badge-ghost">${MessageUtil.getMessage("poe.gems.color." + ("str".equals(gem.color()) ? "red" : "dex".equals(gem.color()) ? "green" : "int".equals(gem.color()) ? "blue" : "white"))}</span>
				<%-- 젬 유형(PoB gemType = 젬 태그 이름) — 한국어는 같은 자리의 tagsKo. Support 는 종류 배지(보조)와 겹쳐 뺀다 --%>
				!{
					int gemTypeIdx = gem.tags() == null || gem.gemType() == null ? -1 : gem.tags().indexOf(gem.gemType());
					String gemTypeLabel = PoeText.isKorean() && gemTypeIdx >= 0 && gem.tagsKo() != null && gemTypeIdx < gem.tagsKo().size() ? gem.tagsKo().get(gemTypeIdx) : gem.gemType();
				}
				@if(gem.gemType() != null && !"Support".equals(gem.gemType()))<span class="badge badge-ghost">${gemTypeLabel}</span>@endif
			</div>
			@if(gem.levels() != null && gem.levels().size() > 1)
				!{
					Integer shown = level != null ? level : gem.levels().get(gem.levels().size() - 1).level();
					java.util.List<Integer> steps = new java.util.ArrayList<>();
					for (Poe2.GemLevel l : gem.levels()) if (l.level() == 1 || l.level() % 5 == 0 || l.level().equals(gem.maxLevel())) steps.add(l.level());
				}
				<nav class="flex flex-wrap items-center gap-1.5" aria-label="${MessageUtil.getMessage("poe.gems.detail.level")}">
					<span class="text-xs font-semibold text-base-content/60">${MessageUtil.getMessage("poe.gems.detail.level")}</span>
					@for(Integer st : steps)
						<a class="poe-chip ${st.equals(shown) ? "poe-chip-active" : ""}" href="/poe2/gems/${gem.slug()}?level=${st}" aria-current="${st.equals(shown) ? "page" : null}">${st}</a>
					@endfor
				</nav>
			@endif
			<div class="flex flex-wrap items-start gap-6">
				@if(gem.image() != null)
					<img src="/poe-data/poe2/icons/${gem.image()}" class="max-h-64 max-w-40 object-contain drop-shadow-[0_2px_6px_rgba(0,0,0,0.5)]" alt="${gem.name()}">
				@endif
				<div class="max-w-md w-full sm:w-auto">
					@template.poe2.htmx.gemTooltip(gem = gem, level = level, pageMode = true)
				</div>
				<%-- 레벨별 진행표(PoE1 젬 화면과 같은 자리) — 레이어는 한 레벨만 보여서 레벨마다 무엇이 느는지 한눈에. 값이 한 번도 없는 열은 뺀다 --%>
				@if(gem.levels() != null && gem.levels().size() > 1)
					!{
						boolean ko = PoeText.isKorean();
						Integer shownRow = level != null ? level : gem.levels().get(gem.levels().size() - 1).level();
						boolean hasCost = gem.levels().stream().anyMatch(l -> l.cost() != null && l.cost() > 0);
						boolean hasRes = gem.levels().stream().anyMatch(l -> l.reservation() != null && l.reservation() > 0);
						boolean hasCd = gem.levels().stream().anyMatch(l -> l.cooldownMs() != null && l.cooldownMs() > 0);
						boolean hasCrit = gem.levels().stream().anyMatch(l -> l.critChance() != null && l.critChance() > 0);
						boolean hasReq = gem.levels().stream().anyMatch(l -> l.requiredLevel() != null);
					}
					<div class="flex-1 min-w-0 sm:min-w-[20rem] rounded-2xl bg-base-100 p-4 shadow-sm">
						<h2 class="font-bold mb-2">${MessageUtil.getMessage("poe.gems.progression")}</h2>
						<div class="overflow-x-auto">
							<table class="table table-xs">
								<thead>
									<tr>
										<th>${MessageUtil.getMessage("poe.gems.detail.level")}</th>
										@if(hasReq)<th>${MessageUtil.getMessage("poe.gems.detail.required.level")}</th>@endif
										<%-- 레벨별 요구 능력치(10-09 C237 — PoB2 getGemStatRequirement, 툴팁 C227 과 같은 식. 보조 젬은 0 이라 열 안 보임) --%>
										@if(hasReq && !"support".equals(gem.kind()))<th>${MessageUtil.getMessage("poe.gems.col.attributes")}</th>@endif
										@if(hasCost)<th>${MessageUtil.getMessage("poe.gems.detail.cost")}</th>@endif
										@if(hasRes)<th>${MessageUtil.getMessage("poe2.gems.reservation")}</th>@endif
										@if(hasCd)<th>${MessageUtil.getMessage("poe.gems.detail.cooldown")}</th>@endif
										@if(hasCrit)<th>${MessageUtil.getMessage("poe.gems.col.crit")}</th>@endif
										<th>${MessageUtil.getMessage("poe.gems.col.effects")}</th>
									</tr>
								</thead>
								<tbody>
									@for(Poe2.GemLevel l : gem.levels())
										<tr class="${l.level().equals(shownRow) ? "bg-primary/10" : ""}">
											<td class="font-mono"><a class="link link-hover" href="/poe2/gems/${gem.slug()}?level=${l.level()}">${l.level()}</a></td>
											@if(hasReq)<td class="font-mono" data-req-col>@if(l.requiredLevel() != null)${l.requiredLevel()}@endif</td>@endif
											@if(hasReq && !"support".equals(gem.kind()))
												!{ int lvReq = l.requiredLevel() == null ? 1 : l.requiredLevel(); int ar = net.luversof.web.gate.poe.PoeGemRequirements.poe2Stat(lvReq, false, gem.reqStr()); int ad = net.luversof.web.gate.poe.PoeGemRequirements.poe2Stat(lvReq, false, gem.reqDex()); int ai = net.luversof.web.gate.poe.PoeGemRequirements.poe2Stat(lvReq, false, gem.reqInt());
												   java.util.List<String> attrCells = new java.util.ArrayList<>(); if (ar > 0) attrCells.add(ar + " " + MessageUtil.getMessage("poe.items.detail.str")); if (ad > 0) attrCells.add(ad + " " + MessageUtil.getMessage("poe.items.detail.dex")); if (ai > 0) attrCells.add(ai + " " + MessageUtil.getMessage("poe.items.detail.int")); }
												<td class="font-mono whitespace-nowrap" data-gem-level-attrs>${String.join(", ", attrCells)}</td>
											@endif
											@if(hasCost)<td class="font-mono whitespace-nowrap">@if(l.cost() != null && l.cost() > 0)${PoeText.costValue(l.cost(), l.costType())}@endif</td>@endif
											@if(hasRes)<td class="font-mono whitespace-nowrap">@if(l.reservation() != null && l.reservation() > 0)${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.gems.spirit.value"), l.reservation())}@endif</td>@endif
											@if(hasCd)<td class="font-mono whitespace-nowrap">@if(l.cooldownMs() != null && l.cooldownMs() > 0)${String.format("%.2f", l.cooldownMs() / 1000.0)}s@endif</td>@endif
											@if(hasCrit)<td class="font-mono">@if(l.critChance() != null && l.critChance() > 0)${String.format("%.2f", l.critChance())}%@endif</td>@endif
											<td class="text-xs whitespace-pre-line">${String.join("\n", PoeText.lines(l.statLinesKo(), l.statLines()))}</td>
										</tr>
									@endfor
								</tbody>
							</table>
						</div>
					</div>
				@endif
				<%-- 태그(PoE1 젬 화면과 같은 칸, 10-12 C265 짝 맞춤) --%>
				!{java.util.List<String> displayTags = PoeText.lines(gem.tagsKo(), gem.tags());}
				@if(displayTags != null && !displayTags.isEmpty())
					<div class="rounded-2xl bg-base-100 p-4 shadow-sm" data-gem-tags>
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
	`
)
