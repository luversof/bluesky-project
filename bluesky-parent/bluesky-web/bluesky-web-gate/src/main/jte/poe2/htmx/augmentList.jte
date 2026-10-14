@import java.util.List
@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe2.Poe2View
@import static net.luversof.web.gate.poe2.Poe2View.nz
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param List<Poe2.Augment> items
@param Integer total = 0
<%-- 장착 대상 필터 — 있으면 그 아이템에 넣었을 때의 효과 줄만 보인다(나머지 대상 줄은 접는다) --%>
@param String itemClass = null

!{ boolean korean = PoeText.isKorean(); }

@if(total == null || total == 0)
	@template._components.ui.emptyState(message = MessageUtil.getMessage("poe2.nodata"), actionLabel = MessageUtil.getMessage("layout.menu.poe2.admin"), actionHref = "/poe2/admin")
@else
	<div class="text-xs text-base-content/60 mb-2">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.list.count"), items.size(), total)}</div>
	@if(items.isEmpty())
		@template._components.ui.emptyState(message = MessageUtil.getMessage("poe.list.empty"))
	@else
		<div class="grid grid-cols-1 md:grid-cols-2 2xl:grid-cols-3 gap-2 items-start">
			@for(Poe2.Augment a : items)
				!{ List<Poe2.AugmentEffect> effects = a.effects().stream().filter(e -> Poe2View.appliesTo(e, itemClass)).toList(); }
				<article class="rounded-lg bg-base-100 border border-base-200 px-3 py-2">
					<div class="flex flex-wrap items-center gap-1.5 min-w-0">
						@if(a.image() != null)
							<img src="/poe-data/poe2/icons/${a.image()}" class="w-8 h-8 object-contain shrink-0" alt="" loading="lazy">
						@endif
						<span class="font-semibold truncate text-[#aa9e82]">${PoeText.name(a.nameKo(), a.name())}</span>
						<span class="shrink-0 badge badge-xs badge-ghost">${PoeText.name(a.kindKo(), a.kind())}</span>
						@if(nz(a.level()) > 0)
							<span class="ml-auto shrink-0 text-[10px] text-base-content/60">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.augments.level"), a.level())}</span>
						@endif
					</div>
					@if(korean)
						<div class="text-[11px] text-base-content/60 truncate">${a.name()}</div>
					@endif
					<dl class="mt-1.5 space-y-1 text-sm">
						@for(Poe2.AugmentEffect e : effects)
							<div class="grid grid-cols-[minmax(0,6.5rem)_minmax(0,1fr)] gap-2">
								<dt class="text-xs text-base-content/60 pt-0.5">${PoeText.name(e.targetKo(), e.target())}</dt>
								<dd class="[overflow-wrap:anywhere]">
									@for(String line : PoeText.lines(e.linesKo(), e.lines()))
										<div class="text-[#8888ff]">${line}</div>
									@endfor
								</dd>
							</div>
						@endfor
					</dl>
					<%-- 결속 효과(10-09 C238 — 게임 ClientStrings ItemDisplayShamanOnly "결속됨:" · SoulCoreStats.BondedStats, poe2db 사막 룬과 같은 분류별 줄) --%>
					@if(effects.stream().anyMatch(e -> e.bondedLines() != null && !e.bondedLines().isEmpty()))
						<div class="mt-1.5 text-xs text-base-content/60" data-augment-bonded>${MessageUtil.getMessage("poe2.augments.bonded")}</div>
						<dl class="mt-1 space-y-1 text-sm">
							@for(Poe2.AugmentEffect e : effects)
								@if(e.bondedLines() != null && !e.bondedLines().isEmpty())
									<div class="grid grid-cols-[minmax(0,6.5rem)_minmax(0,1fr)] gap-2">
										<dt class="text-xs text-base-content/60 pt-0.5">${PoeText.name(e.targetKo(), e.target())}</dt>
										<dd class="[overflow-wrap:anywhere]">
											@for(String line : PoeText.lines(e.bondedLinesKo(), e.bondedLines()))
												<div class="text-[#8888ff]">${line}</div>
											@endfor
										</dd>
									</div>
								@endif
							@endfor
						</dl>
					@endif
					<%-- 중첩 개수(10-13 C297 — CurrencyItems.StackSize, 게임 ClientStrings ItemDisplayStackSize · ItemDisplayStackSizeValue. poe2db 사막 룬 · 뱀 우상 "1 / 10", 1 짜리 · 행 없는 것은 줄 없음) --%>
					@if(nz(a.stackSize()) > 1)
						<div class="mt-1 text-[11px] text-base-content/60" data-augment-stack>${MessageUtil.getMessage("poe2.augments.stack")}: ${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.augments.stack.value"), 1, a.stackSize())}</div>
					@endif
					@if(a.limitText() != null || a.limitTextKo() != null)
						<div class="mt-1 text-[11px] text-base-content/60">${MessageUtil.getMessage("poe2.augments.limit")}: ${PoeText.name(a.limitTextKo(), a.limitText())}</div>
					@endif
				</article>
			@endfor
		</div>
	@endif
@endif
