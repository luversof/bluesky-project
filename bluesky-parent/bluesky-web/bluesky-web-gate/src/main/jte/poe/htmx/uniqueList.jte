@import java.util.List
@import net.luversof.web.gate.poe.dto.PoeUniqueItem
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param List<PoeUniqueItem> items
@param int totalCount = 0
@param int matchedCount = 0

!{
	String countLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.count"), items.size(), totalCount);
	String emptyLabel = MessageUtil.getMessage("poe.gems.empty");
	boolean capped = matchedCount > items.size();
	String cappedLabel = capped ? java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.list.capped"), matchedCount, items.size()) : null;
}

@if(totalCount == 0)
	@template._components.ui.emptyState(message = MessageUtil.getMessage("poe.gems.no.data"), hint = "tools/poe-extract: node parse-uniques.mjs")
@else
<div class="text-xs text-base-content/50 mb-2">${capped ? cappedLabel : countLabel}</div>
@if(items.isEmpty())
	@template._components.ui.emptyState(message = emptyLabel)
@else
	<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
		@for(PoeUniqueItem item : items)
			!{
				String displayName = PoeText.name(item.nameKo(), item.name());
				String displayBase = PoeText.name(item.baseTypeKo(), item.baseType());
				List<String> displayImplicits = PoeText.lines(item.implicitsKo(), item.implicits());
				List<String> displayExplicits = PoeText.lines(item.explicitsKo(), item.explicits());
				String strLabel = MessageUtil.getMessage("poe.gems.attr.red");
				String dexLabel = MessageUtil.getMessage("poe.gems.attr.green");
				String intLabel = MessageUtil.getMessage("poe.gems.attr.blue");
				String levelLabel = MessageUtil.getMessage("poe.gems.detail.required.level");
			}
			<div class="poe-hover card bg-base-100 shadow-sm border border-base-200 cursor-pointer transition-colors duration-300 hover:border-orange-400/50 hover:duration-75"
			     hx-get="/poe/htmx/uniques/detail?slug=${item.slug()}"
			     hx-target="#poePreview"
			     hx-trigger="mouseenter delay:100ms"
			     hx-swap="innerHTML">
				<div class="card-body p-3.5">
				<div class="flex gap-3">
					@if(item.iconKey() != null)
						<img src="/poe-assets/items/${item.iconKey()}.png" class="w-12 h-12 object-contain shrink-0 self-start drop-shadow-[0_1px_2px_rgba(0,0,0,0.5)]" alt="" loading="lazy">
					@endif
					<div class="min-w-0 flex-1 space-y-1">
					<%-- 제목 (고유색) --%>
					<div class="flex items-baseline gap-2">
						<span class="font-bold text-orange-600 dark:text-orange-400 truncate" title="${item.name()}">${displayName}</span>
						@if(PoeText.isKorean())
							<span class="text-xs text-base-content/40 truncate">${item.name()}</span>
						@endif
					</div>
					<%-- 베이스 + 요구조건(레벨·힘·민첩·지능, 0 은 생략) --%>
					<div class="flex flex-wrap items-center gap-x-2 gap-y-0.5 text-[11px] text-base-content/55">
						<span class="truncate">${displayBase}</span>
						@if(item.requiredLevel() != null)
							<span class="text-base-content/40">${levelLabel} ${item.requiredLevel()}</span>
						@endif
						@if(item.reqStr() != null && item.reqStr() > 0)
							<span class="text-red-500/80">${strLabel} ${item.reqStr()}</span>
						@endif
						@if(item.reqDex() != null && item.reqDex() > 0)
							<span class="text-green-500/80">${dexLabel} ${item.reqDex()}</span>
						@endif
						@if(item.reqInt() != null && item.reqInt() > 0)
							<span class="text-blue-500/80">${intLabel} ${item.reqInt()}</span>
						@endif
					</div>
					<%-- 옵션 — 한 줄에 하나씩 (암시=파랑, 폭발=회색) --%>
					@if(!displayImplicits.isEmpty() || !displayExplicits.isEmpty())
						<div class="mt-0.5 space-y-0.5">
							@for(String imp : displayImplicits)
								<div class="text-[11px] leading-5 text-sky-500/70">${imp}</div>
							@endfor
							@for(String ex : displayExplicits)
								<div class="text-[11px] leading-5 text-base-content/60">${ex}</div>
							@endfor
						</div>
					@endif
					</div>
				</div>
				</div>
			</div>
		@endfor
	</div>
@endif
@endif
