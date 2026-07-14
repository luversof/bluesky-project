@import net.luversof.web.gate.poe.service.PoeUniqueItem
@import io.github.luversof.boot.context.support.MessageUtil

@param PoeUniqueItem item

!{
	String closeLabel = MessageUtil.getMessage("common.button.close");
	String requiredLevelLabel = MessageUtil.getMessage("poe.gems.detail.required.level");
	String leagueLabel = MessageUtil.getMessage("poe.uniques.detail.league");
	String displayName = item.nameKo() != null ? item.nameKo() : item.name();
	String displayBase = item.baseTypeKo() != null ? item.baseTypeKo() : item.baseType();
}

<%-- 고유 아이템 툴팁 — 게임의 유니크(주황) 스타일 --%>
<div class="fixed inset-0 z-[90] flex items-center justify-center bg-black/60 p-4" data-overlay>
	<div class="w-full max-w-md rounded-box overflow-hidden bg-neutral text-neutral-content shadow-2xl border border-orange-900/60 max-h-[85vh] overflow-y-auto">
		<div class="relative px-4 py-3 bg-orange-950/50 border-b border-orange-900/60 text-center">
			<div class="text-lg font-bold text-orange-400">${displayName}</div>
			<div class="text-sm text-orange-200/70">${displayBase}</div>
			<div class="text-xs text-neutral-content/40 mt-0.5">${item.name()} · ${item.baseType()}</div>
			<button type="button" class="btn btn-ghost btn-xs absolute right-2 top-2 text-neutral-content/60" data-overlay-close aria-label="${closeLabel}">✕</button>
		</div>

		<div class="px-4 py-2 text-sm space-y-1">
			@if(item.requiredLevel() != null)
				<div class="flex justify-between"><span class="text-neutral-content/60">${requiredLevelLabel}</span><span class="font-mono">${item.requiredLevel()}</span></div>
			@endif
			@if(item.league() != null)
				<div class="flex justify-between"><span class="text-neutral-content/60">${leagueLabel}</span><span class="text-xs">${item.league()}</span></div>
			@endif
		</div>

		@if(!item.implicits().isEmpty())
			<div class="px-4 pb-2">
				<div class="border-t border-neutral-content/15 pt-2 space-y-0.5 text-center">
					@for(String implicitLine : item.implicits())
						<div class="text-sm text-neutral-content/70 whitespace-pre-line">${implicitLine}</div>
					@endfor
				</div>
			</div>
		@endif

		@if(!item.explicits().isEmpty())
			<div class="px-4 pb-4">
				<div class="border-t border-neutral-content/15 pt-2 space-y-0.5 text-center">
					@for(String explicitLine : item.explicits())
						<div class="text-sm text-indigo-300 whitespace-pre-line">${explicitLine}</div>
					@endfor
				</div>
			</div>
		@endif
	</div>
</div>
