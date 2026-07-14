@import java.util.List
@import net.luversof.web.gate.poe.service.PoeUniqueItem
@import io.github.luversof.boot.context.support.MessageUtil

@param List<PoeUniqueItem> items
@param int totalCount = 0

!{
	String countLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.count"), items.size(), totalCount);
	String emptyLabel = MessageUtil.getMessage("poe.gems.empty");
}

@if(totalCount == 0)
	@template._components.ui.emptyState(message = MessageUtil.getMessage("poe.gems.no.data"), hint = "tools/poe-extract: node parse-uniques.mjs")
@else
<div class="text-xs text-base-content/50 mb-2">${countLabel}</div>
@if(items.isEmpty())
	@template._components.ui.emptyState(message = emptyLabel)
@else
	<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
		@for(PoeUniqueItem item : items)
			!{
				String displayName = item.nameKo() != null ? item.nameKo() : item.name();
				String displayBase = item.baseTypeKo() != null ? item.baseTypeKo() : item.baseType();
			}
			<div class="card bg-base-100 shadow-sm border border-base-200 cursor-pointer transition-colors duration-300 hover:border-orange-400/50 hover:duration-75"
			     hx-get="/poe/htmx/uniques/detail?slug=${item.slug()}"
			     hx-target="#poeUniqueDetail"
			     hx-swap="innerHTML">
				<div class="card-body p-4 gap-1">
					<div class="flex items-baseline gap-2">
						<span class="font-bold text-orange-600 dark:text-orange-400 truncate" title="${item.name()}">${displayName}</span>
						<span class="text-xs text-base-content/40 truncate">${item.name()}</span>
					</div>
					<div class="flex items-center gap-2 text-xs text-base-content/60">
						<span class="truncate">${displayBase}</span>
						@if(item.requiredLevel() != null)
							<span class="ml-auto shrink-0 text-base-content/40">Lv ${item.requiredLevel()}</span>
						@endif
					</div>
					@if(!item.explicits().isEmpty())
						<p class="text-xs leading-5 text-base-content/50 line-clamp-2">${String.join(" / ", item.explicits())}</p>
					@endif
				</div>
			</div>
		@endfor
	</div>
@endif
@endif
