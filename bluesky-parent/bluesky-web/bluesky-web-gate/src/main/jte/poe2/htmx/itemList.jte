@import java.util.List
@import net.luversof.web.gate.poe2.dto.Poe2
@import static net.luversof.web.gate.poe2.Poe2View.nz
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param List<Poe2.BaseItem> items
@param Integer total = 0
@param int limit = 0
@param String listQ = ""
@param String listItemClass = ""
@param String listAttr = "all"

!{
	boolean korean = PoeText.isKorean();
	// 아이템 클래스별로 묶고, 안에서는 요구 레벨 순(게임에서 얻는 순서)
	java.util.Map<String, List<Poe2.BaseItem>> byClass = new java.util.LinkedHashMap<>();
	for (Poe2.BaseItem b : items) byClass.computeIfAbsent(net.luversof.web.gate.poe.PoeClassNames.poe2(b.itemClassKo(), b.itemClass()), k -> new java.util.ArrayList<>()).add(b);
	byClass.values().forEach(l -> l.sort(java.util.Comparator.comparingInt((Poe2.BaseItem b) -> nz(b.reqLevel())).thenComparing(Poe2.BaseItem::name)));
	// 더 보기(10-13 C306) — PoE1 베이스 목록과 같은 90개씩. 보이는 순서(분류 묶음 → 요구 레벨)대로 앞에서 자른 뒤 다시 묶는다
	int pageSize = 90;
	int shown = Math.min(limit <= 0 ? pageSize : Math.max(limit, pageSize), items.size());
	boolean capped = items.size() > shown;
	if (capped) {
		java.util.Map<String, List<Poe2.BaseItem>> cut = new java.util.LinkedHashMap<>();
		int left = shown;
		for (java.util.Map.Entry<String, List<Poe2.BaseItem>> e : byClass.entrySet()) {
			if (left <= 0) break;
			List<Poe2.BaseItem> part = e.getValue().subList(0, Math.min(left, e.getValue().size()));
			cut.put(e.getKey(), part);
			left -= part.size();
		}
		byClass = cut;
	}
	String moreUrl = "/poe2/htmx/items?limit=" + (shown + pageSize)
		+ "&q=" + java.net.URLEncoder.encode(listQ, java.nio.charset.StandardCharsets.UTF_8)
		+ "&itemClass=" + java.net.URLEncoder.encode(listItemClass, java.nio.charset.StandardCharsets.UTF_8)
		+ "&attr=" + java.net.URLEncoder.encode(listAttr, java.nio.charset.StandardCharsets.UTF_8);
	String moreLabel = capped ? java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.list.more"), Math.min(shown + pageSize, items.size()) - shown) : null;
}

@if(total == null || total == 0)
	@template._components.ui.emptyState(message = MessageUtil.getMessage("poe2.nodata"), actionLabel = MessageUtil.getMessage("layout.menu.poe2.admin"), actionHref = "/poe2/admin")
@else
	<div class="text-xs text-base-content/60 mb-2">${capped ? java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.list.capped"), items.size(), shown) : java.text.MessageFormat.format(MessageUtil.getMessage("poe.list.count"), items.size(), total)}</div>
	@if(items.isEmpty())
		@template._components.ui.emptyState(message = MessageUtil.getMessage("poe.list.empty"))
	@else
		<div class="space-y-4">
			@for(java.util.Map.Entry<String, List<Poe2.BaseItem>> group : byClass.entrySet())
				<section>
					<h2 class="text-sm font-semibold mb-1.5">${group.getKey()} <span class="text-xs font-normal text-base-content/60">${group.getValue().size()}</span></h2>
					<%-- 카드 = PoE1 베이스 목록과 같은 카드(10-12 C274): 인게임 인벤토리 칸 프레임(베이스의 실제 가로×세로 칸, PoE1 과 같은 58px 단위) · 굵은 이름 ·
					     요약 수치(방어/피해·DPS/회복) + 요구 레벨 · 암시 옵션 첫 줄. 분류 배지 대신 구획 제목이 분류를 보인다 --%>
					<div class="grid grid-cols-1 md:grid-cols-2 gap-3">
						@for(Poe2.BaseItem b : group.getValue())
							!{ int cw = b.width() != null && b.width() > 0 ? b.width() : 2; int ch = b.height() != null && b.height() > 0 ? b.height() : 2; int unit = 58; }
							<div class="poe-hover card bg-base-100 shadow-sm border border-base-200 cursor-help transition-colors duration-300 hover:border-primary/40 hover:duration-75"
							     hx-get="/poe2/htmx/items/detail?slug=${b.slug()}" hx-target="#poePreview" hx-trigger="mouseenter delay:100ms" hx-swap="innerHTML">
								<div class="card-body p-3 gap-1">
									<div class="flex gap-3 items-start">
										<div class="poe-item-slot shrink-0" style="width:${cw * unit}px;height:${ch * unit}px">
											@if(b.image() != null)
												<img src="/poe-data/poe2/icons/${b.image()}" class="max-w-full max-h-full object-contain drop-shadow-[0_1px_3px_rgba(0,0,0,0.7)]" alt="" loading="lazy">
											@endif
										</div>
										<div class="min-w-0 flex-1 space-y-1">
											<div class="flex items-baseline gap-2 flex-wrap">
												<a class="font-bold text-[15px] truncate link-hover" href="/poe2/items/${b.slug()}" title="${b.name()}">${PoeText.name(b.nameKo(), b.name())}</a>
												@if(korean)
													<span class="text-xs text-base-content/60 truncate">${b.name()}</span>
												@endif
											</div>
											!{ String summary = net.luversof.web.gate.poe2.Poe2View.cardSummary(b); List<String> impl = net.luversof.web.gate.poe2.Poe2View.implicitLines(b); }
											<div class="flex items-center gap-2 text-[13px] text-base-content/70">
												<span class="font-mono truncate" data-card-summary>${summary}</span>
												<span class="ml-auto shrink-0 text-base-content/60">${MessageUtil.getMessage("poe.gems.label.drop.level")} ${nz(b.reqLevel())}</span>
											</div>
											@if(!impl.isEmpty())
												<p class="text-xs leading-5 text-sky-600/80 dark:text-sky-300/80 line-clamp-1" data-card-implicit>${impl.get(0)}</p>
											@endif
										</div>
									</div>
								</div>
							</div>
						@endfor
					</div>
				</section>
			@endfor
		</div>
		@if(capped)
			<%-- 더 보기(C306) — PoE1 목록과 같은 단추 · 문구, 한 단계(90개)씩 늘려 같은 자리에 다시 그린다 --%>
			<div class="flex justify-center pt-3">
				<button type="button" class="btn btn-sm btn-outline"
				        hx-get="${moreUrl}"
				        hx-target="#poe2ItemList"
				        hx-swap="innerHTML"
				        hx-disabled-elt="this">${moreLabel}</button>
			</div>
		@endif
	@endif
@endif
