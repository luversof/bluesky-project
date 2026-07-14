@import net.luversof.web.gate.poe.service.PoeBaseItem
@import io.github.luversof.boot.context.support.MessageUtil

@param PoeBaseItem item

!{
	String closeLabel = MessageUtil.getMessage("common.button.close");
	String dropLevelLabel = MessageUtil.getMessage("poe.items.detail.drop.level");
	String requirementLabel = MessageUtil.getMessage("poe.items.detail.requirement");
	String armourLabel = MessageUtil.getMessage("poe.items.detail.armour");
	String evasionLabel = MessageUtil.getMessage("poe.items.detail.evasion");
	String energyShieldLabel = MessageUtil.getMessage("poe.items.detail.energy.shield");
	String wardLabel = MessageUtil.getMessage("poe.items.detail.ward");
	String physicalDamageLabel = MessageUtil.getMessage("poe.items.detail.physical.damage");
	String critChanceLabel = MessageUtil.getMessage("poe.gems.detail.crit.chance");
	String attacksPerSecondLabel = MessageUtil.getMessage("poe.items.detail.attacks.per.second");
	String displayName = item.nameKo() != null ? item.nameKo() : item.name();
	String displayClass = item.itemClassKo() != null ? item.itemClassKo() : item.itemClass();

	StringBuilder requirementText = new StringBuilder();
	if (item.reqStr() > 0) requirementText.append("힘 ").append(item.reqStr()).append("  ");
	if (item.reqDex() > 0) requirementText.append("민첩 ").append(item.reqDex()).append("  ");
	if (item.reqInt() > 0) requirementText.append("지능 ").append(item.reqInt());
}

<%-- 일반 아이템 툴팁 — 게임의 일반(흰색) 아이템 스타일 --%>
<div class="fixed inset-0 z-[90] flex items-center justify-center bg-black/60 p-4" data-overlay>
	<div class="w-full max-w-md rounded-box overflow-hidden bg-neutral text-neutral-content shadow-2xl border border-neutral-content/20 max-h-[85vh] overflow-y-auto">
		<div class="relative px-4 py-3 bg-neutral-focus/60 border-b border-neutral-content/15 text-center">
			<div class="text-lg font-bold text-gray-100">${displayName}</div>
			<div class="text-sm text-neutral-content/60">${displayClass}</div>
			<div class="text-xs text-neutral-content/40 mt-0.5">${item.name()} · ${item.itemClass()}</div>
			<button type="button" class="btn btn-ghost btn-xs absolute right-2 top-2 text-neutral-content/60" data-overlay-close aria-label="${closeLabel}">✕</button>
		</div>

		<div class="px-4 py-3 space-y-1 text-sm">
			@if(item.armour() != null)
				@if(item.armour().armourMax() > 0)
					<div class="flex justify-between"><span class="text-neutral-content/60">${armourLabel}</span><span class="font-mono">${item.armour().armourMin()}-${item.armour().armourMax()}</span></div>
				@endif
				@if(item.armour().evasionMax() > 0)
					<div class="flex justify-between"><span class="text-neutral-content/60">${evasionLabel}</span><span class="font-mono">${item.armour().evasionMin()}-${item.armour().evasionMax()}</span></div>
				@endif
				@if(item.armour().energyShieldMax() > 0)
					<div class="flex justify-between"><span class="text-neutral-content/60">${energyShieldLabel}</span><span class="font-mono">${item.armour().energyShieldMin()}-${item.armour().energyShieldMax()}</span></div>
				@endif
				@if(item.armour().wardMax() > 0)
					<div class="flex justify-between"><span class="text-neutral-content/60">${wardLabel}</span><span class="font-mono">${item.armour().wardMin()}-${item.armour().wardMax()}</span></div>
				@endif
			@endif
			@if(item.weapon() != null)
				<div class="flex justify-between"><span class="text-neutral-content/60">${physicalDamageLabel}</span><span class="font-mono">${item.weapon().damageMin()}-${item.weapon().damageMax()}</span></div>
				<div class="flex justify-between"><span class="text-neutral-content/60">${critChanceLabel}</span><span class="font-mono">${String.format("%.2f", item.weapon().critChance())}%</span></div>
				<div class="flex justify-between"><span class="text-neutral-content/60">${attacksPerSecondLabel}</span><span class="font-mono">${String.format("%.2f", item.weapon().attacksPerSecond())}</span></div>
			@endif
			<div class="flex justify-between"><span class="text-neutral-content/60">${dropLevelLabel}</span><span class="font-mono">${item.dropLevel()}</span></div>
			@if(requirementText.length() > 0)
				<div class="flex justify-between"><span class="text-neutral-content/60">${requirementLabel}</span><span class="font-mono">${requirementText.toString()}</span></div>
			@endif
		</div>

		@if(!item.implicits().isEmpty())
			<div class="px-4 pb-4">
				<div class="border-t border-neutral-content/15 pt-2 space-y-0.5 text-center">
					@for(var implicitLine : item.implicits())
						<div class="text-sm text-sky-300/90 whitespace-pre-line">${implicitLine.ko() != null ? implicitLine.ko() : implicitLine.en()}</div>
					@endfor
				</div>
			</div>
		@endif
	</div>
</div>
