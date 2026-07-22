@import java.util.List
@import net.luversof.web.gate.poe.dto.PoeBaseItem
@import io.github.luversof.boot.context.support.MessageUtil

@param List<PoeBaseItem> items
@param int totalCount = 0
@param int matchedCount = 0

!{
	String countLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.count"), items.size(), totalCount);
	String emptyLabel = MessageUtil.getMessage("poe.gems.empty");
	boolean capped = matchedCount > items.size();
	String cappedLabel = capped ? java.text.MessageFormat.format(MessageUtil.getMessage("poe.items.list.capped"), matchedCount, items.size()) : null;
}

@if(totalCount == 0)
	@template._components.ui.emptyState(message = MessageUtil.getMessage("poe.gems.no.data"), hint = "tools/poe-extract: node parse-items.mjs")
@else
<div class="text-xs text-base-content/50 mb-2">${capped ? cappedLabel : countLabel}</div>
@if(items.isEmpty())
	@template._components.ui.emptyState(message = emptyLabel)
@else
	<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
		@for(PoeBaseItem item : items)
			!{
				String displayName = net.luversof.web.gate.poe.PoeText.name(item.nameKo(), item.name());
				String displayClass = item.itemClassKo() != null ? item.itemClassKo() : item.itemClass();
				// 카드 요약 수치: 방어구는 방어 스탯, 무기는 피해/공속
				StringBuilder summary = new StringBuilder();
				if (item.armour() != null) {
					if (item.armour().armourMax() > 0) summary.append("AR ").append(item.armour().armourMin()).append("-").append(item.armour().armourMax()).append("  ");
					if (item.armour().evasionMax() > 0) summary.append("EV ").append(item.armour().evasionMin()).append("-").append(item.armour().evasionMax()).append("  ");
					if (item.armour().energyShieldMax() > 0) summary.append("ES ").append(item.armour().energyShieldMin()).append("-").append(item.armour().energyShieldMax()).append("  ");
					if (item.armour().wardMax() > 0) summary.append("WD ").append(item.armour().wardMin()).append("-").append(item.armour().wardMax());
				}
				if (item.weapon() != null) {
					summary.append(item.weapon().damageMin()).append("-").append(item.weapon().damageMax())
						.append(" · ").append(String.format("%.2f", item.weapon().attacksPerSecond())).append("/s")
						.append(" · ").append(String.format("%.2f", item.weapon().critChance())).append("%");
				}
			}
			<div class="poe-hover card bg-base-100 shadow-sm border border-base-200 cursor-pointer transition-colors duration-300 hover:border-primary/40 hover:duration-75"
			     hx-get="/poe/htmx/items/detail?slug=${item.slug()}"
			     hx-target="#poePreview"
			     hx-trigger="mouseenter delay:100ms"
			     hx-swap="innerHTML">
				<div class="card-body p-4 gap-1">
					<div class="flex gap-3">
						<!-- 베이스 아이콘 — 고유 아이템 목록과 동일 규격(아이콘 파일명 = 베이스 slug) -->
						<img src="/poe-assets/items/${item.slug()}.png" class="w-12 h-12 object-contain shrink-0 self-start drop-shadow-[0_1px_2px_rgba(0,0,0,0.5)]" alt="" loading="lazy">
						<div class="min-w-0 flex-1 space-y-1">
					<div class="flex items-baseline gap-2">
						<span class="font-bold truncate" title="${item.name()}">${displayName}</span>
						@if(net.luversof.web.gate.poe.PoeText.isKorean())
							<span class="text-xs text-base-content/40 truncate">${item.name()}</span>
						@endif
						<span class="badge badge-xs badge-ghost ml-auto shrink-0">${displayClass}</span>
					</div>
					<div class="flex items-center gap-2 text-xs text-base-content/60">
						<span class="font-mono truncate">${summary.toString()}</span>
						<span class="ml-auto shrink-0 text-base-content/40">Lv ${item.dropLevel()}</span>
					</div>
					@if(!item.implicits().isEmpty())
						!{String implicitPreview = net.luversof.web.gate.poe.PoeText.name(item.implicits().get(0).ko(), item.implicits().get(0).en());}
						<p class="text-xs leading-5 text-sky-600/70 dark:text-sky-300/70 line-clamp-1">${implicitPreview}</p>
					@endif
						</div>
					</div>
				</div>
			</div>
		@endfor
	</div>
@endif
@endif
