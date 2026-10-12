@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param Poe2.Meta meta = null
@param Poe2.Unique item
@param Poe2.BaseItem base = null
@param Integer variant = null
@param boolean forceable = false

!{
	String title = PoeText.name(item.nameKo(), item.name());
	Integer active = variant != null ? variant : item.defaultVariant();
}

<%-- PoE2 고유 상세 — 툴팁 + 변형 칩(툴팁 바깥, 인게임엔 라벨이 없으므로). 오른쪽 "정보" 표와 "모드 원문(영문)"은 PoE1 고유 상세
     (poe/uniquePage.jte)와 같은 구성(10-01: PoE2 엔 없었다). --%>
@template._layout.poeLayout(
	title = title,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	menu = "poe2",
	contentPoe = @`
		<div id="poe2UniquePage" class="space-y-4">
			<%-- 거래소에서 찾기(10-08 C173, PoE1 고유 상세 짝) — 리그가 주소 경로에 들어간다 --%>
			@template.poe2.header(title = title, meta = meta, backHref = "/poe2/uniques", backLabel = MessageUtil.getMessage("poe2.uniques.title"),
				subtitle = PoeText.isKorean() ? item.name() : null, actions = @`
				<a class="btn btn-sm btn-outline" target="_blank" rel="noopener" data-unique-trade href="${PoeText.trade2Url(meta == null ? null : meta.tradeLeague(), PoeText.uniqueTradeQuery(item.nameKo(), item.name()))}" title="${MessageUtil.getMessage("poe.uniques.trade.title")}">${MessageUtil.getMessage("poe.common.trade.find")}</a>
				@if(forceable)
					<%-- PoE1 고유 상세와 같은 바로가기 — 시뮬레이터가 이 고유를 고정한 채 열린다(10-01, 방어구·장신구만) --%>
					<a class="btn btn-sm btn-primary btn-outline" href="/poe2/sim?unique=${item.slug()}${variant != null && item.variants() != null && item.variants().size() > 1 ? ":" + variant : ""}" data-optimize-unique>${MessageUtil.getMessage("poe.uniques.optimize")}</a>
				@endif
			`)
			@if(item.variants() != null && item.variants().size() > 24)
				<%-- 변형이 많은 고유(메갈로매니악 874 · 신념의 프리즘 261 — PoB 생성 고유)는 칩 벽 대신 select(10-04 C80, PoE1 uniqueVariants 의 짝).
				     스크립트가 없으면 폼 제출, htmx 가 있으면 고르는 즉시 이 화면을 다시 받아 본문(#poe2UniquePage)만 바꾸고 주소를 민다.
				     hx-select="body" 는 안 된다 — htmx 가 응답을 조각으로 파싱하며 body 를 벗겨 빈 것을 붙였다(탐침이 잡음). --%>
				<form class="flex w-80 max-w-full flex-wrap items-center gap-1.5" method="get" action="/poe2/uniques/${item.slug()}"
				      hx-get="/poe2/uniques/${item.slug()}" hx-trigger="change" hx-target="#poe2UniquePage" hx-select="#poe2UniquePage" hx-swap="outerHTML" hx-push-url="true">
					<label class="text-xs font-semibold text-base-content/60" for="poe2UniqueVariantSelect">${MessageUtil.getMessage("poe.uniques.variant")} ${item.variants().size()}</label>
					<select id="poe2UniqueVariantSelect" name="variant" class="select select-bordered select-sm min-w-0 flex-1">
						@for(Poe2.UniqueVariant v : item.variants())
							<option value="${v.index()}" selected="${v.index() != null && v.index().equals(active)}">${PoeText.name(v.nameKo(), v.name())}</option>
						@endfor
					</select>
					<noscript><button type="submit" class="btn btn-sm">${MessageUtil.getMessage("poe.uniques.variants.show")}</button></noscript>
				</form>
			@elseif(item.variants() != null && !item.variants().isEmpty())
				<div class="flex flex-wrap items-center gap-1.5" aria-label="${MessageUtil.getMessage("poe.uniques.variant")}">
					<span class="text-xs font-semibold text-base-content/60">${MessageUtil.getMessage("poe.uniques.variant")}</span>
					@for(Poe2.UniqueVariant v : item.variants())
						<a class="poe-chip ${v.index() != null && v.index().equals(active) ? "poe-chip-active" : ""}" href="/poe2/uniques/${item.slug()}?variant=${v.index()}">${PoeText.name(v.nameKo(), v.name())}</a>
					@endfor
				</div>
			@endif
			<div class="flex flex-wrap items-start gap-6">
				@if(item.image() != null)
					<img src="/poe-data/poe2/icons/${item.image()}" class="max-h-64 max-w-40 object-contain drop-shadow-[0_2px_6px_rgba(0,0,0,0.5)]" alt="${item.name()}">
				@endif
				<div class="max-w-md w-full sm:w-auto">
					@template.poe2.htmx.uniqueTooltip(item = item, base = base, variant = active)
				</div>
				!{
					boolean ko = PoeText.isKorean();
					java.util.List<String> engMods = item.explicits();
					if (active != null && item.variants() != null) {
						for (Poe2.UniqueVariant v : item.variants()) {
							if (v.index() != null && v.index().equals(active) && v.explicits() != null) engMods = v.explicits();
						}
					}
				}
				<div class="flex-1 min-w-0 sm:min-w-[20rem] space-y-4">
					<div class="rounded-2xl bg-base-100 p-4 shadow-sm" data-unique-info>
						<h2 class="font-bold mb-2">${MessageUtil.getMessage("poe.uniques.info")}</h2>
						<table class="table table-xs [overflow-wrap:anywhere]">
							<tbody>
								<tr><td class="text-base-content/60">${MessageUtil.getMessage("poe.uniques.info.name")}</td><td>${item.name()}</td></tr>
								<tr><td class="text-base-content/60">${MessageUtil.getMessage("poe.uniques.info.base")}</td><td>
									@if(base != null && base.slug() != null)
										<a class="link link-hover" href="/poe2/items/${base.slug()}">${PoeText.name(item.baseTypeKo(), item.baseType())}</a>
									@else
										${PoeText.name(item.baseTypeKo(), item.baseType())}
									@endif
								</td></tr>
								@if(item.itemClass() != null)
									<tr><td class="text-base-content/60">${MessageUtil.getMessage("poe.uniques.info.class")}</td><td>${net.luversof.web.gate.poe.PoeClassNames.poe2(item.itemClassKo(), item.itemClass())}</td></tr>
								@endif
								@if(item.requiredLevel() != null && item.requiredLevel() > 1)
									<tr><td class="text-base-content/60">${MessageUtil.getMessage("poe.gems.detail.required.level")}</td><td>${item.requiredLevel()}</td></tr>
								@endif
								@if(item.league() != null && !item.league().isBlank())
									<tr><td class="text-base-content/60">${MessageUtil.getMessage("poe.uniques.info.league")}</td><td>${item.league()}</td></tr>
								@endif
							</tbody>
						</table>
					</div>
					@if(ko && engMods != null && !engMods.isEmpty())
						<div class="rounded-2xl bg-base-100 p-4 shadow-sm" data-unique-english>
							<h2 class="font-bold mb-2">모드 원문 (영문)</h2>
							@for(String line : engMods)
								<div class="text-xs text-base-content/60 whitespace-pre-line">${line}</div>
							@endfor
						</div>
					@endif
				</div>
			</div>
		</div>
	`
)
