@import java.util.List
@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param List<Poe2.Unique> items
@param Integer total = 0
@param int limit = 0
@param String listQ = ""
@param String listItemClass = ""

!{
	boolean korean = PoeText.isKorean();
	List<Poe2.Unique> sorted = new java.util.ArrayList<>(items);
	sorted.sort(java.util.Comparator.comparing((Poe2.Unique u) -> net.luversof.web.gate.poe.PoeClassNames.poe2(u.itemClassKo(), u.itemClass()) == null ? "" : net.luversof.web.gate.poe.PoeClassNames.poe2(u.itemClassKo(), u.itemClass()))
		.thenComparing(u -> PoeText.name(u.nameKo(), u.name())));
	// 더 보기(10-13 C306) — PoE1 고유 목록과 같은 90개씩(PoeHtmxController.LIST_LIMIT · listLimit). 예전엔 443개 900KB 를 한 번에 그렸다
	int pageSize = 90;
	int shown = Math.min(limit <= 0 ? pageSize : Math.max(limit, pageSize), sorted.size());
	boolean capped = sorted.size() > shown;
	List<Poe2.Unique> visible = sorted.subList(0, shown);
	String moreUrl = "/poe2/htmx/uniques?limit=" + (shown + pageSize)
		+ "&q=" + java.net.URLEncoder.encode(listQ, java.nio.charset.StandardCharsets.UTF_8)
		+ "&itemClass=" + java.net.URLEncoder.encode(listItemClass, java.nio.charset.StandardCharsets.UTF_8);
	String moreLabel = capped ? java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.list.more"), Math.min(shown + pageSize, sorted.size()) - shown) : null;
}

@if(total == null || total == 0)
	@template._components.ui.emptyState(message = MessageUtil.getMessage("poe2.nodata"), actionLabel = MessageUtil.getMessage("layout.menu.poe2.admin"), actionHref = "/poe2/admin")
@else
	<div class="text-xs text-base-content/60 mb-2">${capped ? java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.list.capped"), sorted.size(), shown) : java.text.MessageFormat.format(MessageUtil.getMessage("poe.list.count"), items.size(), total)}</div>
	@if(items.isEmpty())
		@template._components.ui.emptyState(message = MessageUtil.getMessage("poe.list.empty"))
	@else
		<%-- 카드 = PoE1 고유 목록(poe/htmx/uniqueList.jte)과 같은 구성(10-01: PoE2 는 이름 · 베이스만): 베이스 + 요구 레벨 + 리그 배지 + 변형 배지,
		     인게임 툴팁 구조대로 고정(암시) 옵션 → 구분선 → 접두/접미 옵션(모드 파랑, 수치 강조). 변형이 있으면 기본 변형의 옵션. --%>
		<div class="grid grid-cols-1 md:grid-cols-2 gap-3">
			@for(Poe2.Unique u : visible)
				!{
					Poe2.UniqueVariant dv = null;
					if (u.defaultVariant() != null && u.variants() != null) {
						for (Poe2.UniqueVariant v : u.variants()) {
							if (v.index() != null && v.index().intValue() == u.defaultVariant().intValue()) dv = v;
						}
					}
					List<String> imps = dv != null ? PoeText.lines(dv.implicitsKo(), dv.implicits()) : PoeText.lines(u.implicitsKo(), u.implicits());
					List<String> exps = dv != null ? PoeText.lines(dv.explicitsKo(), dv.explicits()) : PoeText.lines(u.explicitsKo(), u.explicits());
					if (imps == null) imps = List.of();
					if (exps == null) exps = List.of();
				}
				<div class="poe-hover card bg-base-100 shadow-sm border border-base-200 cursor-pointer transition-colors duration-300 hover:border-orange-400/50 hover:duration-75"
				     hx-get="/poe2/htmx/uniques/detail?slug=${u.slug()}" hx-target="#poePreview" hx-trigger="mouseenter delay:100ms" hx-swap="innerHTML" data-unique-card>
					<div class="card-body p-3">
					<div class="flex gap-3 items-start">
						<%-- 인게임 인벤토리 칸 프레임 — PoE1 고유 목록과 같은 58px 단위(10-12 C280). 칸 수는 아이콘 그림 크기에서(없으면 분류 기본값) --%>
						!{ int[] ucells = u.image() == null ? null : net.luversof.web.gate.poe.config.PoeDataVersion.iconCells("/poe-data/poe2/icons/" + u.image());
						   if (ucells == null) ucells = net.luversof.web.gate.poe.PoeText.invCells(u.itemClass()); int uunit = 58; }
						<div class="poe-item-slot shrink-0" style="width:${ucells[0] * uunit}px;height:${ucells[1] * uunit}px">
							@if(u.image() != null)
								<img src="/poe-data/poe2/icons/${u.image()}" class="max-w-full max-h-full object-contain drop-shadow-[0_1px_3px_rgba(0,0,0,0.7)]" alt="" loading="lazy">
							@endif
						</div>
						<div class="min-w-0 flex-1 space-y-1">
							<div class="flex items-baseline gap-2 min-w-0">
								<a class="font-bold poe-name-unique truncate link-hover" href="/poe2/uniques/${u.slug()}" title="${u.name()}">${PoeText.name(u.nameKo(), u.name())}</a>
								@if(korean)
									<span class="text-xs text-base-content/60 truncate">${u.name()}</span>
								@endif
							</div>
							<div class="flex flex-wrap items-center gap-x-2 gap-y-0.5 text-[11px] text-base-content/60">
								<span class="truncate">${PoeText.name(u.baseTypeKo(), u.baseType())} · ${net.luversof.web.gate.poe.PoeClassNames.poe2(u.itemClassKo(), u.itemClass())}</span>
								@if(u.requiredLevel() != null && u.requiredLevel() > 1)
									<span class="text-base-content/60" data-req-level>${MessageUtil.getMessage("poe.gems.detail.required.level")} ${u.requiredLevel()}</span>
								@endif
								@if(u.league() != null && !u.league().isBlank())
									<span class="badge badge-ghost badge-xs" data-league title="${MessageUtil.getMessage("poe.uniques.league.title")}">${u.league()}</span>
								@endif
								@if(u.variants() != null && u.variants().size() > 1)
									<span class="badge badge-outline badge-xs border-fuchsia-500/40 text-fuchsia-500" title="${MessageUtil.getMessage("poe.uniques.variants.title")}">${MessageUtil.getMessage("poe.uniques.variant")} ${u.variants().size()}</span>
								@endif
							</div>
							@if(!imps.isEmpty() || !exps.isEmpty())
								<div class="mt-1 space-y-0.5" data-card-mods>
									@for(String imp : imps)
										<div class="text-[11px] leading-5 text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(imp)}</div>
									@endfor
									@if(!imps.isEmpty() && !exps.isEmpty())
										<div class="my-1 border-t border-base-content/15"></div>
									@endif
									@for(String ex : exps)
										<div class="text-[11px] leading-5 text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(ex)}</div>
									@endfor
								</div>
							@endif
						</div>
					</div>
					</div>
				</div>
			@endfor
		</div>
		@if(capped)
			<%-- 더 보기(C306) — PoE1 목록과 같은 단추 · 문구, 한 단계(90개)씩 늘려 같은 자리에 다시 그린다 --%>
			<div class="flex justify-center pt-3">
				<button type="button" class="btn btn-sm btn-outline"
				        hx-get="${moreUrl}"
				        hx-target="#poe2UniqueList"
				        hx-swap="innerHTML"
				        hx-disabled-elt="this">${moreLabel}</button>
			</div>
		@endif
	@endif
@endif
