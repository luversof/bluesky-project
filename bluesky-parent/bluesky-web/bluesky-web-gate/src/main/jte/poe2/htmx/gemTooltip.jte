@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param Poe2.Gem gem
<%-- 보일 레벨 — null 이면 최대 레벨(목록 호버). 레벨 수치가 없는 젬(메타 등)은 설명만. --%>
@param Integer level = null
<%-- 상세 페이지에 박힌 툴팁이면 true — 페이지가 레벨 단추를 툴팁 밖에 따로 두므로 안쪽 단추는 그리지 않는다 --%>
@param boolean pageMode = false

!{
	// 인게임 젬 툴팁 — 이름은 젬 희귀도 색(청록), 태그 줄 회색, 설명 금색, 보조 설명은 설명과 같은 자리(PoE1 툴팁과 같은 배치)
	String nameColor = "#1ba29b";
	String label = "text-[#7f7f7f]";
	String value = "text-white";
	java.util.List<String> tags = PoeText.lines(gem.tagsKo(), gem.tags());
	String description = PoeText.name(gem.descriptionKo(), gem.description());
	String supportText = PoeText.name(gem.supportTextKo(), gem.supportText());
	java.util.List<String> family = PoeText.lines(gem.familyKo(), gem.family());
	Poe2.GemLevel lv = null;
	if (gem.levels() != null && !gem.levels().isEmpty()) {
		lv = gem.levels().get(gem.levels().size() - 1);
		if (level != null) for (Poe2.GemLevel x : gem.levels()) if (x.level() != null && x.level().equals(level)) lv = x;
	}
	java.util.List<String> statLines = lv == null ? java.util.List.of() : PoeText.lines(lv.statLinesKo(), lv.statLines());
	// 요구 능력치 비율(%) — PoE2 젬은 레벨이 오를수록 요구치가 이 비율로 는다. 어느 능력치를 요구하는지가 핵심이라 이름만 보인다.
	java.util.List<String> attrs = new java.util.ArrayList<>();
	if (gem.reqStr() != null && gem.reqStr() > 0) attrs.add(MessageUtil.getMessage("poe.items.detail.str"));
	if (gem.reqDex() != null && gem.reqDex() > 0) attrs.add(MessageUtil.getMessage("poe.items.detail.dex"));
	if (gem.reqInt() != null && gem.reqInt() > 0) attrs.add(MessageUtil.getMessage("poe.items.detail.int"));
}

<div class="poe-tooltip poe-rar-gem poe-tt-w-gem max-h-[80vh] overflow-y-auto shadow-2xl" style="border-color:${nameColor}">
	<div class="poe-tt-band">
		<div class="text-center font-bold text-[15px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${nameColor}">${PoeText.name(gem.nameKo(), gem.name())}</div>
	</div>
	@if(tags != null && !tags.isEmpty())
		<div class="px-4 pt-1 text-center text-[11.5px] text-[#7f7f7f]">${String.join(", ", tags)}</div>
	@endif
	<div class="poe-tt-sepimg"></div>
	<div class="px-4 py-1.5 text-[12px] text-center space-y-0.5">
		@if(lv != null && gem.maxLevel() != null && gem.maxLevel() > 1)
			<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.level")}: </span><span class="${value}">${lv.level()}</span><span class="${label}"> / ${gem.maxLevel()}</span></div>
		@endif
		@if(gem.tier() != null && gem.tier() > 0)
			<div><span class="${label}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.gems.tier"), gem.tier())}</span></div>
		@endif
		@if(lv != null && lv.cost() != null && lv.cost() > 0)
			<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.cost")}: </span><span class="${value}">${lv.cost()} ${PoeText.costType(lv.costType())}</span></div>
		@endif
		@if(lv != null && lv.reservation() != null && lv.reservation() > 0)
			<div><span class="${label}">${MessageUtil.getMessage("poe2.gems.reservation")}: </span><span class="${value}">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.gems.spirit.value"), lv.reservation())}</span></div>
		@endif
		@if(gem.castTimeMs() != null && gem.castTimeMs() > 0 && !"support".equals(gem.kind()))
			<div><span class="${label}">${MessageUtil.getMessage("poe.gems.label.cast.time")}: </span><span class="${value}">${String.format("%.2f", gem.castTimeMs() / 1000.0)}s</span></div>
		@endif
		@if(lv != null && lv.cooldownMs() != null && lv.cooldownMs() > 0)
			<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.cooldown")}: </span><span class="${value}">${String.format("%.2f", lv.cooldownMs() / 1000.0)}s</span></div>
		@endif
		@if(lv != null && lv.critChance() != null && lv.critChance() > 0)
			<div><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.crit.chance")}: </span><span class="${value}">${String.format("%.2f", lv.critChance())}%</span></div>
		@endif
		@if(gem.weaponRequirements() != null)
			<div><span class="${label}">${MessageUtil.getMessage("poe2.gems.weapon")}: </span><span class="${value}">${gem.weaponRequirements()}</span></div>
		@endif
		<%-- 요구 사항 — PoE1 젬 툴팁과 같은 "요구 레벨 N, 능력치" 줄(10-01: 레벨이 없었다). 요구 레벨 = PoB levelRequirement --%>
		@if(lv != null && lv.requiredLevel() != null)
			<div data-gem-req><span class="${label}">${MessageUtil.getMessage("poe.gems.detail.required.level")} </span><span class="${value}">${lv.requiredLevel()}</span>@if(!attrs.isEmpty())<span class="${label}">, </span><span class="${value}">${String.join(", ", attrs)}</span>@endif</div>
		@elseif(!attrs.isEmpty())
			<div><span class="${label}">${MessageUtil.getMessage("poe.items.detail.requirement")}: </span><span class="${value}">${String.join(", ", attrs)}</span></div>
		@endif
	</div>
	@if(description != null && !description.isBlank())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-[12px] leading-snug text-center text-[#b4966a] whitespace-pre-line">${description}</div>
	@endif
	@if(!statLines.isEmpty())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center space-y-0.5">
			@for(String line : statLines)
				<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(line)}</div>
			@endfor
		</div>
	@endif
	@if(supportText != null && !supportText.isBlank())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-[12px] leading-snug text-center text-[#b4966a] whitespace-pre-line">${supportText}</div>
	@endif
	@if(family != null && !family.isEmpty())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-[11.5px] text-center">
			<span class="${label}">${MessageUtil.getMessage("poe2.gems.family")}: </span><span class="text-[#8888ff]">${String.join(", ", family)}</span>
			@if(Boolean.TRUE.equals(gem.lineage()))<span class="ml-1 text-[#af6025]">(${MessageUtil.getMessage("poe2.gems.lineage")})</span>@endif
			<div class="text-[10.5px] text-white/40 mt-0.5">${MessageUtil.getMessage("poe2.gems.family.hint")}</div>
		</div>
	@endif
	@if(PoeText.isKorean())
		<div class="px-4 pb-2 text-center text-[10px] text-white/30">${gem.name()}</div>
	@endif
	<%-- 레벨 단추 + 종류 배지 — PoE1 젬 툴팁(poe/htmx/gemDetail.jte)과 같은 자리·모양(10-01). 호버 레이어는 클릭하면 고정돼 단추를 누를 수 있다.
	     레벨 1 · 5의 배수 · 최대 레벨. 상세 페이지(pageMode)는 툴팁 밖에 같은 단추가 있어 여기선 뺀다. --%>
	@if(!pageMode && lv != null && gem.levels().size() > 1)
		!{
			java.util.List<Integer> steps = new java.util.ArrayList<>();
			for (Poe2.GemLevel x : gem.levels()) if (x.level() != null && (x.level() == 1 || x.level() % 5 == 0 || x.level().equals(gem.maxLevel()))) steps.add(x.level());
		}
		<div class="px-4 py-2 border-t border-white/10" data-gem-levels>
			<div class="flex flex-wrap gap-1 items-center">
				<span class="text-[11px] text-white/40 mr-1">${MessageUtil.getMessage("poe.gems.detail.level")}</span>
				@for(Integer step : steps)
					<button type="button"
					        class="px-1.5 py-0.5 rounded text-[11px] ${step.equals(lv.level()) ? "bg-[#1ba29b] text-white" : "bg-white/5 text-white/60 hover:bg-white/10"}"
					        hx-get="/poe2/htmx/gems/detail?slug=${gem.slug()}&level=${step}"
					        hx-target="#poePreview"
					        hx-swap="innerHTML">${step}</button>
				@endfor
				<span class="ml-auto text-[10px] px-1.5 py-0.5 rounded border ${"support".equals(gem.kind()) ? "border-fuchsia-500/40 text-fuchsia-400" : "border-cyan-500/40 text-cyan-400"}">${"support".equals(gem.kind()) ? MessageUtil.getMessage("poe.gems.type.support") : MessageUtil.getMessage("poe.gems.type.active")}</span>
			</div>
		</div>
	@endif
</div>
