@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param Poe2.BuildSummary build = null
@param String error = null
@param String detail = null

!{
	boolean korean = PoeText.isKorean();
}

@if(error != null)
	<div class="alert alert-warning text-sm" role="alert">
		<span>${"empty".equals(error) ? MessageUtil.getMessage("poe2.build.empty") : MessageUtil.getMessage("poe2.build.invalid")}</span>
	</div>
@elseif(build != null)
	!{
		String className = PoeText.name(build.classNameKo(), build.className());
		String asc = PoeText.name(build.ascendancyKo(), build.ascendancy());
		Poe2.BuildTree tree = build.tree();
	}
	<div class="space-y-4">
		<%-- 머리: 직업 · 전직 · 레벨 --%>
		<div class="flex flex-wrap items-baseline gap-3">
			<h2 class="text-xl font-bold">${asc != null ? asc : (className != null ? className : "?")}</h2>
			@if(asc != null && className != null)<span class="text-base-content/60">${className}</span>@endif
			@if(build.level() != null)<span class="badge badge-ghost">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.build.level"), build.level())}</span>@endif
		</div>

		<%-- 스탯 (PoB 가 저장한 값) --%>
		@if(build.stats() != null && !build.stats().isEmpty())
			<section class="space-y-1.5">
				<h3 class="text-sm font-semibold">${MessageUtil.getMessage("poe2.build.stats")}</h3>
				<div class="grid grid-cols-[repeat(auto-fit,minmax(min(8rem,100%),1fr))] sm:grid-cols-4 lg:grid-cols-6 gap-2">
					@for(Poe2.BuildStat s : build.stats())
						!{
							String key = s.key().toLowerCase(java.util.Locale.ROOT);
							String label = "spirit".equals(key) ? MessageUtil.getMessage("poe2.build.stat.spirit") : MessageUtil.getMessage("poe.build.stat." + key, s.key());
							boolean pct = key.endsWith("resist");
							double v = s.value();
							String shown = pct ? String.format("%.0f%%", v) : Math.abs(v) >= 100 || v == Math.rint(v) ? String.format("%,.0f", v) : String.format("%.2f", v);
						}
						<div class="rounded-box border border-base-200 bg-base-100 px-3 py-2">
							<div class="text-xs text-base-content/60">${label}</div>
							<div class="font-mono text-sm font-semibold">${shown}</div>
						</div>
					@endfor
				</div>
			</section>
		@endif

		<%-- 엔진 재계산(약 2초) — 요약을 먼저 보이고 붙인다. 코드는 위 입력칸 것을 그대로(13KB 라 POST), 조회라 "완료" 토스트는 뺀다 --%>
		<div hx-post="/poe2/htmx/build/recalc" hx-trigger="load" hx-include="#poe2BuildImport [name=code]" hx-swap="outerHTML" data-no-success-toast>
			<div class="flex items-center gap-2 text-sm text-base-content/60"><span class="loading loading-spinner loading-xs"></span>${korean ? "PoB-PoE2 엔진으로 다시 계산하는 중…" : "Recalculating with the PoB-PoE2 engine…"}</div>
		</div>
		<%-- 업그레이드 가이드(약 3초) — 재계산과 나란히 따로 부른다 --%>
		<div hx-post="/poe2/htmx/build/guide" hx-trigger="load" hx-include="#poe2BuildImport [name=code]" hx-swap="outerHTML" data-no-success-toast>
			<div class="flex items-center gap-2 text-sm text-base-content/60"><span class="loading loading-spinner loading-xs"></span>${korean ? "업그레이드 가이드를 계산하는 중… (장비 칸·보조젬·패시브·고유 아이템 교체 — 레벨 100 빌드는 10초 안팎)" : "Computing the upgrade guide…"}</div>
		</div>

		<%-- 스킬 묶음 --%>
		@if(build.skills() != null && !build.skills().isEmpty())
			<section class="space-y-1.5">
				<h3 class="text-sm font-semibold">${MessageUtil.getMessage("poe2.build.skills")}</h3>
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-2">
					@for(Poe2.BuildSkill g : build.skills())
						<div class="rounded-lg border ${Boolean.TRUE.equals(g.main()) ? "border-primary/60" : "border-base-200"} bg-base-100 px-3 py-2 ${Boolean.FALSE.equals(g.enabled()) ? "opacity-60" : ""}">
							<div class="flex items-center gap-2 text-xs text-base-content/60 mb-1">
								@if(g.slot() != null)<span>${net.luversof.web.gate.poe2.Poe2View.slotName(g.slot())}</span>@endif
								@if(Boolean.TRUE.equals(g.main()))<span class="badge badge-xs badge-primary">${MessageUtil.getMessage("poe2.build.main")}</span>@endif
								@if(Boolean.FALSE.equals(g.enabled()))<span class="badge badge-xs badge-ghost">${MessageUtil.getMessage("poe2.build.disabled")}</span>@endif
							</div>
							<ul class="space-y-0.5">
								@for(Poe2.BuildGem gem : g.gems())
									!{
										String nameColor = gem.color() == null ? "" : switch (gem.color()) { case "str" -> "poe-gem-red"; case "dex" -> "poe-gem-green"; case "int" -> "poe-gem-blue"; default -> "poe-gem-white"; };
									}
									<li class="flex items-center gap-2 text-sm ${Boolean.TRUE.equals(gem.support()) ? "pl-4" : "font-semibold"} ${Boolean.FALSE.equals(gem.enabled()) ? "line-through opacity-60" : ""}">
										@if(gem.image() != null)
											<img src="/poe-data/poe2/icons/${gem.image()}" class="w-6 h-6 object-contain shrink-0" alt="" loading="lazy">
										@endif
										@if(gem.slug() != null)
											<a class="poe-hover link-hover ${nameColor}" href="/poe2/gems/${gem.slug()}"
											   hx-get="/poe2/htmx/gems/detail?slug=${gem.slug()}${gem.level() != null ? "&level=" + gem.level() : ""}" hx-target="#poePreview" hx-trigger="mouseenter delay:100ms" hx-swap="innerHTML" data-build-gem>${PoeText.name(gem.nameKo(), gem.name())}</a>
										@else
											<span>${gem.name()}</span>
										@endif
										<%-- 레벨 — 툴팁도 빌드의 레벨로 연다(10-01: 툴팁이 1레벨로 열렸다). PoE1 은 "레벨/품질" 이지만 PoE2 엔 젬 품질이 없어(전부 0) 레벨만 --%>
										@if(gem.level() != null)<span class="ml-auto text-xs font-mono text-base-content/60" data-gem-lq>${gem.level()}</span>@endif
									</li>
								@endfor
							</ul>
						</div>
					@endfor
				</div>
			</section>
		@endif

		<%-- 장비 --%>
		@if(build.items() != null && !build.items().isEmpty())
			<section class="space-y-1.5">
				<h3 class="text-sm font-semibold">${MessageUtil.getMessage("poe2.build.items")}</h3>
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-2">
					@for(Poe2.BuildItem it : build.items())
						!{
							String rarityClass = switch (it.rarity() == null ? "" : it.rarity()) { case "UNIQUE" -> "poe-name-unique"; case "RARE" -> "poe-name-rare"; case "MAGIC" -> "poe-name-magic"; default -> "poe-name-normal"; };
							String shownName = PoeText.name(it.nameKo(), it.name());
						}
						<div class="flex gap-3 rounded-lg border border-base-200 bg-base-100 px-3 py-2 ${it.mods() != null && !it.mods().isEmpty() ? "poe-hover cursor-help hover:border-primary/40" : ""}">
							<div class="w-10 h-12 shrink-0 flex items-center justify-center">
								@if(it.image() != null)<img src="/poe-data/poe2/icons/${it.image()}" class="max-w-10 max-h-12 object-contain" alt="" loading="lazy">@endif
							</div>
							<div class="min-w-0 flex-1">
								<div class="text-xs text-base-content/60">${net.luversof.web.gate.poe2.Poe2View.slotName(it.slot())}</div>
								@if(it.uniqueSlug() != null)
									<%-- 고유도 카드의 인라인 툴팁(이 빌드가 굴린 값, 10-04 C140) — 링크는 고유 상세 화면으로 --%>
									<a class="font-semibold ${rarityClass} link-hover" href="/poe2/uniques/${it.uniqueSlug()}" data-build-unique="${it.uniqueSlug()}">${shownName}</a>
								@else
									<div class="font-semibold ${rarityClass} truncate">${shownName}</div>
								@endif
								@if(!"UNIQUE".equals(it.rarity()) && !"NORMAL".equals(it.rarity()) || it.baseTypeKo() != null && "UNIQUE".equals(it.rarity()))
									<div class="text-xs text-base-content/60 truncate">
										@if(it.baseSlug() != null)
											<a class="link-hover" href="/poe2/items/${it.baseSlug()}">${PoeText.name(it.baseTypeKo(), it.baseType())}</a>
										@else
											${it.baseType()}
										@endif
									</div>
								@endif
								@if(it.mods() != null && !it.mods().isEmpty() && !"UNIQUE".equals(it.rarity()))
									<%-- 레어 · 마법은 PoE1 빌드 요약처럼 옵션 세 줄 요약 + 올리면 인게임 툴팁(10-04 C132 — 예전엔 "옵션 N개" 접이식 목록).
									     한국어는 API 가 옵션 풀·고유·증강물 영/한 쌍으로 옮긴 줄(못 옮긴 줄은 영문 그대로) --%>
									<div class="text-[11px] leading-4 text-[#8888ff]/80 line-clamp-3 mt-0.5">${String.join(" / ", korean && it.modsKo() != null && it.modsKo().size() == it.mods().size() ? it.modsKo() : it.mods())}</div>
								@endif
							</div>
							@if(it.mods() != null && !it.mods().isEmpty())
								<div data-poe-tip class="hidden">@template.poe2.htmx.buildItemTooltip(item = it)</div>
							@endif
						</div>
					@endfor
				</div>
			</section>
		@endif

		<%-- 패시브 --%>
		@if(tree != null)
			<section class="space-y-1.5">
				<div class="flex flex-wrap items-baseline gap-3">
					<h3 class="text-sm font-semibold">${MessageUtil.getMessage("poe2.build.tree")}</h3>
					<span class="text-xs text-base-content/60">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.build.points"), tree.allocated(), tree.ascendancyPoints())}</span>
					@if(build.treeLink() != null)
						<a class="link link-primary text-sm" href="${build.treeLink()}">${MessageUtil.getMessage("poe2.build.opentree")}</a>
					@endif
				</div>
				@for(String kind : java.util.List.of("ascnodes", "keystones", "notables"))
					!{ java.util.List<Poe2.BuildNode> list = "keystones".equals(kind) ? tree.keystones() : "notables".equals(kind) ? tree.notables() : tree.ascendancyNodes(); }
					@if(list != null && !list.isEmpty())
						<div class="flex flex-wrap items-center gap-1.5">
							<span class="text-xs font-semibold text-base-content/60 w-16 shrink-0">${MessageUtil.getMessage("poe2.build." + kind)}</span>
							@for(Poe2.BuildNode n : list)
								<span class="badge badge-sm ${"keystones".equals(kind) ? "badge-warning badge-outline" : "badge-ghost"}" title="${n.name()}">${PoeText.name(n.nameKo(), n.name())}</span>
							@endfor
						</div>
					@endif
				@endfor
			</section>
		@endif
	</div>
@endif
