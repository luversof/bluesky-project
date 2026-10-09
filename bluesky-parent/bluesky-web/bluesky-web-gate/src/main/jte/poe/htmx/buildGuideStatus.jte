@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe.dto.PoeUpgradeGuide

@param PoeUpgradeGuide.Status status = null
@param boolean loginRequired = false

<%-- 업그레이드 가이드 상태/결과. 제안마다 측정한 증감(%)을 DPS · EHP 로 나눠 보여준다 —
     합성 목표값 하나만 보이면 "목표값은 올랐는데 DPS 는 내려가 고장처럼 보이는" 일이 생긴다. --%>
@if(loginRequired)
	<div class="text-sm text-base-content/60">${MessageUtil.getMessage("poe.build.guide.login")}</div>
@elseif(status == null)
	<div class="text-sm text-error">${MessageUtil.getMessage("poe.build.guide.unavailable")}</div>
@elseif(Boolean.TRUE.equals(status.running()))
	<div class="flex flex-wrap items-center gap-2 text-sm">
		<span class="loading loading-spinner loading-xs text-primary"></span>
		<span class="font-semibold">${status.phase()}</span>
		@if(status.total() != null && status.total() > 0)
			<span class="font-mono text-base-content/60">${status.done()}/${status.total()}</span>
		@endif
		<span class="text-xs text-base-content/60">${MessageUtil.getMessage("poe.build.guide.running.hint")}</span>
	</div>
@elseif(status.error() != null)
	<div class="text-sm text-error">${status.error()}</div>
@elseif(status.result() != null)
	!{ PoeUpgradeGuide.Result result = status.result(); }
	<div class="space-y-3">
		<div class="flex flex-wrap items-baseline gap-x-3 gap-y-1 text-sm">
			<span class="font-semibold">${MessageUtil.getMessage("poe.build.guide.title")}</span>
			<span class="text-base-content/60">${result.mainSkill()}</span>
			@if(result.baseline() != null)
				<span class="font-mono text-xs text-base-content/60">DPS ${String.format("%,.0f", result.baseline().dps())} · EHP ${String.format("%,.0f", result.baseline().ehp())}@if(result.baseline().hasMinion()) · <span class="whitespace-nowrap">${MessageUtil.getMessage("poe.build.guide.minion")} ${String.format("%,.0f", result.baseline().minionEhp())}</span>@endif</span>
			@endif
		</div>
		@if(result.suggestions() == null || result.suggestions().isEmpty())
			<div class="text-sm text-base-content/60">${MessageUtil.getMessage("poe.build.guide.none")}</div>
		@else
			<ol class="space-y-2">
				@for(PoeUpgradeGuide.Suggestion s : result.suggestions())
					<li class="rounded border border-base-300 px-3 py-2 text-sm">
						<div class="flex flex-wrap items-center gap-2">
							@if("merc".equals(s.kind()))
								<span class="badge badge-info badge-outline badge-sm">${MessageUtil.getMessage("poe.build.guide.kind.merc")}</span>
							@elseif("free".equals(s.kind()))
								<span class="badge badge-success badge-outline badge-sm">${MessageUtil.getMessage("poe.build.guide.kind.free")}</span>
							@elseif("support".equals(s.kind()))
								<span class="badge badge-primary badge-outline badge-sm">${MessageUtil.getMessage("poe.build.guide.kind.support")}</span>
							@else
								<span class="badge badge-ghost badge-sm">${MessageUtil.getMessage("poe.build.guide.kind.item")}</span>
							@endif
							<span class="font-semibold">${s.title()}</span>
							@template.poe.htmx.buildGuideDelta(label = "DPS", pct = s.dpsPct())
							@template.poe.htmx.buildGuideDelta(label = "EHP", pct = s.ehpPct())
							@template.poe.htmx.buildGuideDelta(label = MessageUtil.getMessage("poe.build.guide.minion"), pct = s.minionPct())
						</div>
						<div class="mt-1 text-xs text-base-content/70">${s.detail()}</div>
						<%-- 이 칸에 끼워 재 본 고유 아이템 — 이름은 인게임 고유 색 + 호버 툴팁(빌드 요약과 같은 레이어).
						     새 탭으로 여는 까닭: 같은 탭으로 가면 몇 분 걸린 분석 결과가 화면에서 사라진다. --%>
						@if(s.picks() != null && !s.picks().isEmpty())
							<ul class="mt-2 space-y-1">
								@for(PoeUpgradeGuide.ItemPick p : s.picks())
									<li>
										<div class="flex flex-wrap items-center gap-x-2 gap-y-1">
											<span class="text-xs text-base-content/60" aria-hidden="true">→</span>
											@if("RARE".equals(p.rarity()))
												<%-- 레어 목표 — 지금 아이템과 같은 베이스에 2티어 옵션을 얹은 것. 이름은 인게임 레어 색, 옵션은 아래 줄에. --%>
												<span class="badge badge-outline badge-sm">${MessageUtil.getMessage("poe.build.guide.rare")}</span>
												<span class="font-semibold poe-name-rare">${net.luversof.web.gate.poe.PoeText.name(p.baseTypeKo(), p.baseType())}</span>
											@else
												<a class="poe-hover font-semibold poe-name-unique link-hover cursor-pointer"
												   href="/poe/uniques/${p.slug()}" target="_blank" rel="noopener"
												   hx-get="/poe/htmx/uniques/detail?slug=${p.slug()}"
												   hx-target="#poePreview"
												   hx-trigger="mouseenter delay:100ms"
												   hx-swap="innerHTML">${net.luversof.web.gate.poe.PoeText.name(p.nameKo(), p.name())}</a>
											@endif
											<%-- 거래소(10-08 C171, 빌드 카드와 같은 배지) — 고유는 이름, 레어 목표는 베이스 + 옵션(그 티어 최저 롤 이상) --%>
											@if(net.luversof.web.gate.poe.PoeText.tradeUrl(p.tradeQuery()) != null)
												<a class="badge badge-xs badge-outline badge-info shrink-0" target="_blank" rel="noopener" href="${net.luversof.web.gate.poe.PoeText.tradeUrl(p.tradeQuery())}" data-guide-trade
												   title="${net.luversof.web.gate.poe.PoeText.isKorean() ? ("RARE".equals(p.rarity()) ? "베이스 + 옵션(그 티어 최저 롤 이상)으로 거래소 검색(즉시 구입 · 가격순)" : "이 고유 아이템을 거래소에서 검색(즉시 구입 · 가격순)") : ("RARE".equals(p.rarity()) ? "Search trade — base + mods at tier minimum" : "Search trade for this unique")}">${net.luversof.web.gate.poe.PoeText.isKorean() ? "거래소" : "Trade"}</a>
											@endif
											@template.poe.htmx.buildGuideDelta(label = "DPS", pct = p.dpsPct())
											@template.poe.htmx.buildGuideDelta(label = "EHP", pct = p.ehpPct())
											@template.poe.htmx.buildGuideDelta(label = MessageUtil.getMessage("poe.build.guide.minion"), pct = p.minionPct())
											@if(p.metaPercent() != null)
												<span class="badge badge-ghost badge-sm">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.build.guide.meta"), p.metaPercent())}</span>
											@endif
											@if(p.needs() != null)
												<%-- 그대로는 못 끼는 추천 — 요구 능력치를 다른 곳(트리·주얼·다른 장비)에서 채워야 한다. --%>
												<span class="badge badge-warning badge-outline badge-sm">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.build.guide.needs"), p.needs())}</span>
											@endif
										</div>
										@if(p.mods() != null && !p.mods().isEmpty())
											<div class="mt-0.5 pl-5 text-xs leading-5 text-base-content/60">${String.join(" · ", p.mods())}</div>
										@endif
										@if(p.itemText() != null)
											<%-- PoB 아이템 텍스트 복사(10-02, PoE2 가이드와 짝) — 엔진이 잰 그 아이템. PoB "Create custom" 에 붙여 넣는다(common.ts [data-copy-target]) --%>
											!{ String copyId = "poeRareText-" + Integer.toHexString(System.identityHashCode(p)); boolean ko = net.luversof.web.gate.poe.PoeText.isKorean(); }
											<div class="mt-0.5 pl-5 flex items-center gap-2 text-xs">
												<textarea id="${copyId}" class="sr-only" readonly tabindex="-1" aria-hidden="true" data-rare-item-text>${p.itemText()}</textarea>
												<button type="button" class="btn btn-ghost btn-xs" data-copy-target="#${copyId}" data-copied-label="${ko ? "복사됨 ✓" : "Copied ✓"}">${ko ? "아이템 텍스트 복사" : "Copy item text"}</button>
												<span class="text-base-content/60">${ko ? "PoB 아이템 탭 “Create custom” 에 붙여 넣기" : "paste into PoB Items → Create custom"}</span>
											</div>
										@endif
									</li>
								@endfor
							</ul>
						@endif
					</li>
				@endfor
			</ol>
		@endif
		!{
			boolean anyMeta = result.suggestions() != null && result.suggestions().stream()
				.anyMatch(x -> x.picks() != null && x.picks().stream().anyMatch(p -> p.metaPercent() != null));
		}
		<div class="text-xs text-base-content/60">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.build.guide.footer"), result.evaluations(), Math.round(result.durationMs() / 1000.0))}</div>
		@if(anyMeta)
			<div class="text-xs text-base-content/60">${MessageUtil.getMessage("poe.build.guide.meta.note")}</div>
		@endif
		@if(result.baseline() != null && result.baseline().hasMinion())
			<div class="text-xs text-base-content/60">${MessageUtil.getMessage("poe.build.guide.minion.note")}</div>
		@endif
	</div>
@endif
