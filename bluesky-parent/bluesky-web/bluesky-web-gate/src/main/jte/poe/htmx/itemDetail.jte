@import net.luversof.web.gate.poe.dto.PoeBaseItem
@import net.luversof.web.gate.poe.dto.ModFamily
@import net.luversof.web.gate.poe.dto.ModTier
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param PoeBaseItem item
@param java.util.List<ModFamily> modFamilies = null

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
	String strLabel = MessageUtil.getMessage("poe.build.stat.armour");
	String displayName = PoeText.name(item.nameKo(), item.name());
	String displayClass = PoeText.name(item.itemClassKo(), item.itemClass());

	StringBuilder requirementText = new StringBuilder();
	if (item.reqStr() > 0) requirementText.append("힘 ").append(item.reqStr()).append("  ");
	if (item.reqDex() > 0) requirementText.append("민첩 ").append(item.reqDex()).append("  ");
	if (item.reqInt() > 0) requirementText.append("지능 ").append(item.reqInt());
	// 일반(흰색) 아이템 색
	String rarity = "#c8c8c8";
	boolean hasProps = (item.armour() != null && (item.armour().armourMax() > 0 || item.armour().evasionMax() > 0 || item.armour().energyShieldMax() > 0 || item.armour().wardMax() > 0)) || item.weapon() != null;
}

<%-- PoE 게임 아이템 툴팁 (일반/흰색) --%>
	<div class="poe-tooltip poe-rar-white w-80 max-h-[80vh] overflow-y-auto shadow-2xl" style="border-color:${rarity}">
		<div class="poe-tt-band">
			<div class="text-center font-bold text-[15px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:#e6e6e6">${displayName}</div>
			<div class="text-center text-[12px] leading-tight text-[#c8c8c8] drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]">${displayClass}</div>
		</div>
		@if(PoeText.isKorean())
			<div class="px-4 pt-1 text-center text-[10px] text-white/35">${item.name()}</div>
		@endif

		<%-- 아이템 클래스 속성 (방어/무기 수치) --%>
		@if(hasProps)
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-[12px] space-y-0.5">
				@if(item.weapon() != null)
					<div class="flex justify-between"><span class="text-[#7f7f7f]">${physicalDamageLabel}</span><span class="text-[#8888ff] font-medium">${item.weapon().damageMin()}-${item.weapon().damageMax()}</span></div>
					<div class="flex justify-between"><span class="text-[#7f7f7f]">${critChanceLabel}</span><span class="text-white">${String.format("%.2f", item.weapon().critChance())}%</span></div>
					<div class="flex justify-between"><span class="text-[#7f7f7f]">${attacksPerSecondLabel}</span><span class="text-white">${String.format("%.2f", item.weapon().attacksPerSecond())}</span></div>
				@endif
				@if(item.armour() != null)
					@if(item.armour().armourMax() > 0)
						<div class="flex justify-between"><span class="text-[#7f7f7f]">${armourLabel}</span><span class="text-[#8888ff] font-medium">${item.armour().armourMin()}-${item.armour().armourMax()}</span></div>
					@endif
					@if(item.armour().evasionMax() > 0)
						<div class="flex justify-between"><span class="text-[#7f7f7f]">${evasionLabel}</span><span class="text-[#8888ff] font-medium">${item.armour().evasionMin()}-${item.armour().evasionMax()}</span></div>
					@endif
					@if(item.armour().energyShieldMax() > 0)
						<div class="flex justify-between"><span class="text-[#7f7f7f]">${energyShieldLabel}</span><span class="text-[#8888ff] font-medium">${item.armour().energyShieldMin()}-${item.armour().energyShieldMax()}</span></div>
					@endif
					@if(item.armour().wardMax() > 0)
						<div class="flex justify-between"><span class="text-[#7f7f7f]">${wardLabel}</span><span class="text-[#8888ff] font-medium">${item.armour().wardMin()}-${item.armour().wardMax()}</span></div>
					@endif
				@endif
			</div>
		@endif

		<%-- 요구 사항 / 드랍 레벨 --%>
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-[12px] space-y-0.5">
			<div class="flex justify-between"><span class="text-[#7f7f7f]">${dropLevelLabel}</span><span class="text-white">${item.dropLevel()}</span></div>
			@if(requirementText.length() > 0)
				<div class="flex justify-between"><span class="text-[#7f7f7f]">${requirementLabel}</span><span class="text-white">${requirementText.toString()}</span></div>
			@endif
		</div>

		<%-- 암시 모드 --%>
		@if(!item.implicits().isEmpty())
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-2 text-center space-y-0.5">
				@for(var implicitLine : item.implicits())
					<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(PoeText.name(implicitLine.ko(), implicitLine.en()))}</div>
				@endfor
			</div>
		@endif

		<%-- 가능한 모드 티어 (커브레이티드 모드 풀 기준) — 접두/접미별, 티어(T1=최상)·요구 아이템 레벨 --%>
		@if(modFamilies != null && !modFamilies.isEmpty())
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-2">
				<div class="text-center text-[11px] font-semibold text-white/50 mb-1">${MessageUtil.getMessage("poe.items.detail.mod.tiers")}</div>
				@for(String gen : new String[]{"prefix", "suffix"})
					!{
						java.util.List<ModFamily> group = modFamilies.stream().filter(f -> gen.equals(f.gen())).toList();
					}
					@if(!group.isEmpty())
						<div class="text-[10px] font-semibold text-[#7f7f7f] mt-1.5 mb-0.5">${MessageUtil.getMessage("poe.items.detail.mod." + gen)}</div>
						@for(ModFamily family : group)
							<div class="mb-1">
								@for(int t = 0; t < family.tiers().size(); t++)
									!{
										ModTier tier = family.tiers().get(t);
										java.util.List<String> lines = PoeText.lines(tier.ko(), tier.en());
									}
									<div class="flex items-baseline gap-1.5 text-[11px] leading-4">
										<span class="shrink-0 text-[#7f7f7f] w-11">T${t + 1}·i${tier.level()}</span>
										<span class="text-[#8888ff]">$unsafe{PoeText.highlightValues(String.join(", ", lines))}</span>
									</div>
								@endfor
							</div>
						@endfor
					@endif
				@endfor
			</div>
		@endif
	</div>
