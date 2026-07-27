@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe.dto.PoeBuild
@import net.luversof.web.gate.poe.dto.PoeOptimizeResult

@param PoeOptimizeResult result = null
@param java.util.Map<String, String> tattooIcons = null

@if(result != null)
	!{
		String gemLabel = net.luversof.web.gate.poe.PoeText.name(result.gemNameKo(), result.gemName());
		String objectiveLabel = MessageUtil.getMessage("poe.sim.opt.objective." + result.objective());
		String nodesParam = String.join(",", result.treeNodeIds().stream().map(String::valueOf).toList());
		// 트리 에디터가 직업/전직까지 복원하도록 함께 넘긴다(안 넘기면 사이온으로 열려 편집 시 트리가 날아감)
		String treeLink = "/poe/tree?nodes=" + nodesParam
			+ (result.className() != null ? "&class=" + java.net.URLEncoder.encode(result.className(), java.nio.charset.StandardCharsets.UTF_8) : "")
			+ (result.ascendancy() != null ? "&asc=" + java.net.URLEncoder.encode(result.ascendancy(), java.nio.charset.StandardCharsets.UTF_8) : "")
			// 클러스터/주얼 구성도 함께 — 없으면 트리 화면에서 생성 노드가 사라져 결과 수치와 어긋난다
			+ (result.treeClusters() != null && !result.treeClusters().isBlank() ? "&c=" + java.net.URLEncoder.encode(result.treeClusters(), java.nio.charset.StandardCharsets.UTF_8) : "")
			+ (result.treeJewels() != null && !result.treeJewels().isBlank() ? "&j=" + java.net.URLEncoder.encode(result.treeJewels(), java.nio.charset.StandardCharsets.UTF_8) : "")
			+ (result.treeTattoos() != null && !result.treeTattoos().isBlank() ? "&tt=" + java.net.URLEncoder.encode(result.treeTattoos(), java.nio.charset.StandardCharsets.UTF_8) : "")
			// 마스터리 선택 — 빠지면 트리 화면 평가가 결과보다 약하게 나온다(표시=실제)
			+ (result.treeMasteries() != null && !result.treeMasteries().isBlank() ? "&masteries=" + java.net.URLEncoder.encode(result.treeMasteries(), java.nio.charset.StandardCharsets.UTF_8) : "")
			// 도유 — 빠지면 트리 화면에서 도유 노터블이 사라져 결과 수치와 어긋난다
			+ (result.treeAnoint() != null ? "&an=" + result.treeAnoint() : "")
			// 혈맹 — 안 실으면 혈맹 서브트리 노드가 트리 화면에서 통째로 숨는다(노터블 focus 실패 실사고)
			+ (result.bloodline() != null ? "&bloodline=" + java.net.URLEncoder.encode(result.bloodline(), java.nio.charset.StandardCharsets.UTF_8) : "");
		String treeLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.sim.opt.result.tree"), result.treeNodeIds().size());
		String durationLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.sim.opt.duration"), result.durationMs() / 1000, result.evalCount());
		// 현실성 점검 — 원소 저항 미달(75 미만)이나 유효 체력 바닥이면 인게임에서 못 쓰는 유리대포다
		java.util.function.Function<String, Integer> statInt = key -> {
			for (PoeBuild.PlayerStat st : result.stats()) {
				if (st.key().equals(key)) {
					try { return (int) Double.parseDouble(st.value().replace(",", "")); } catch (Exception e) { return null; }
				}
			}
			return null;
		};
		Integer fireRes = statInt.apply("fireresist");
		Integer coldRes = statInt.apply("coldresist");
		Integer lightRes = statInt.apply("lightningresist");
		Integer ehpValue = statInt.apply("totalehp");
		java.util.List<String> uncapped = new java.util.ArrayList<>();
		if (fireRes != null && fireRes < 75) uncapped.add(MessageUtil.getMessage("poe.sim.opt.result.defense.fire"));
		if (coldRes != null && coldRes < 75) uncapped.add(MessageUtil.getMessage("poe.sim.opt.result.defense.cold"));
		if (lightRes != null && lightRes < 75) uncapped.add(MessageUtil.getMessage("poe.sim.opt.result.defense.lightning"));
		Integer netRegen = statInt.apply("netliferegen");
		boolean unsustainable = netRegen != null && netRegen < 0;
		boolean fragile = (ehpValue != null && ehpValue < 10000) || !uncapped.isEmpty() || unsustainable;
		// 스탯 셀 위험 색 — PoB 처럼 미달 저항은 빨강, 음수 순생명재생도 빨강. 그 외는 중립.
		java.util.function.BiFunction<String, String, String> statColor = (key, val) -> {
			Integer n;
			try { n = (int) Double.parseDouble(val.replace(",", "").replace("x", "")); } catch (Exception e) { return ""; }
			switch (key) {
				case "fireresist": case "coldresist": case "lightningresist":
					return n < 75 ? "text-rose-500" : "";
				case "netliferegen":
					return n < 0 ? "text-rose-500" : "";
				default:
					return "";
			}
		};
	}
	<div class="space-y-3 rounded-box border border-primary/25 bg-primary/5 p-4 mt-2">
		<div class="flex flex-wrap items-center gap-2">
			<span class="text-sm font-bold text-primary">${MessageUtil.getMessage("poe.sim.opt.result.title")}</span>
			<span class="font-bold">${gemLabel}</span>
			<span class="badge badge-primary badge-outline badge-sm">${net.luversof.web.gate.poe.PoeText.name(result.classNameKo(), result.className())}${result.ascendancyKo() != null ? " · " + net.luversof.web.gate.poe.PoeText.name(result.ascendancyKo(), result.ascendancy()) : ""}</span>
			@if(result.bloodline() != null)
				<span class="badge badge-sm text-rose-500 border-rose-500/40">${MessageUtil.getMessage("poe.sim.opt.result.bloodline")}: ${net.luversof.web.gate.poe.PoeText.name(result.bloodlineKo(), result.bloodline())}</span>
			@endif
			<span class="badge badge-ghost badge-sm">${objectiveLabel}</span>
			@if(result.scenarioKo() != null)
				<span class="badge badge-error badge-outline badge-sm">${MessageUtil.getMessage("poe.sim.opt.scenario")}: ${net.luversof.web.gate.poe.PoeText.name(result.scenarioKo(), result.scenario())}</span>
			@endif
			@if(result.combatBuffs())
				<span class="badge badge-success badge-outline badge-sm">${MessageUtil.getMessage("poe.sim.opt.buffs")}</span>
			@endif
			<span class="badge badge-ghost badge-sm font-mono">${durationLabel}</span>
		</div>

		@if(fragile)
			<div class="flex flex-wrap items-center gap-2 rounded border border-warning/50 bg-warning/10 px-3 py-2 text-xs">
				<span class="font-bold text-warning">${MessageUtil.getMessage("poe.sim.opt.result.fragile")}</span>
				@if(!uncapped.isEmpty())
					<span class="text-base-content/70">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.sim.opt.result.fragile.res"), String.join(", ", uncapped))}</span>
				@endif
				@if(ehpValue != null && ehpValue < 10000)
					<span class="text-base-content/70">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.sim.opt.result.fragile.ehp"), ehpValue)}</span>
				@endif
				@if(unsustainable)
					<span class="font-semibold text-rose-500">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.sim.opt.result.unsustainable"), netRegen)}</span>
				@endif
				<span class="text-base-content/50 ml-auto">${MessageUtil.getMessage("poe.sim.opt.result.fragile.hint")}</span>
			</div>
		@endif

		<div class="flex flex-wrap items-baseline gap-3">
			<span class="text-sm text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.baseline")} <span class="font-mono">${result.baselineValue()}</span></span>
			<span class="text-lg font-bold font-mono text-primary">→ ${result.finalValue()}</span>
			<span class="text-[11px] text-base-content/40">${net.luversof.web.gate.poe.PoeText.isKorean() ? "(모든 레어 최상위 티어 가정)" : "(all rares at top tier)"}</span>
		</div>

		<%-- 어픽스 예산 축: "필수 옵션 수"에 따른 현실적 DPS 곡선 — 레어가 있을 때만. 필수 많을수록 finalValue 에 수렴 --%>
		@if(result.affixBudget() != null && !result.affixBudget().isEmpty())
			<div class="flex flex-wrap items-center gap-x-4 gap-y-1 rounded-box bg-base-100 px-3 py-2 text-sm"
			     title="${net.luversof.web.gate.poe.PoeText.isKorean() ? "레어 아이템의 필수 옵션 N개만 최상위 티어로 확보하고 나머지는 중위 티어라고 가정했을 때의 값. 실제 제작 난이도에 가까운 현실적 기대치." : "Value assuming only N essential rare mods reach top tier, the rest are mid-tier"}">
				<span class="font-semibold text-base-content/70">${net.luversof.web.gate.poe.PoeText.isKorean() ? "어픽스 예산(현실적 제작)" : "Affix budget (realistic)"}</span>
				@for(var point : result.affixBudget())
					<span class="flex items-baseline gap-1">
						<span class="text-[11px] text-base-content/50">${net.luversof.web.gate.poe.PoeText.isKorean() ? "필수 " + point.essentialCount() + "개" : point.essentialCount() + " essential"}</span>
						<span class="font-mono text-warning">${point.value()}</span>
					</span>
				@endfor
			</div>
		@endif

		@if(!result.stats().isEmpty())
			<div class="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-7 gap-2">
				@for(PoeBuild.PlayerStat stat : result.stats())
					<div class="rounded-box bg-base-100 px-3 py-2">
						<div class="text-[11px] font-semibold text-base-content/50">${MessageUtil.getMessage("poe.build.stat." + stat.key())}</div>
						<div class="text-sm font-bold font-mono ${statColor.apply(stat.key(), stat.value())}">${stat.value()}</div>
					</div>
				@endfor
			</div>
		@endif

		@if(result.defenseHits() != null && !result.defenseHits().isEmpty())
			!{
				java.util.Map<String, String> defColor = java.util.Map.of(
					"physical", "text-base-content/70",
					"fire", "text-red-500",
					"cold", "text-sky-400",
					"lightning", "poe-def-lightning",
					"chaos", "text-fuchsia-500");
			}
			<div class="space-y-1.5">
				<span class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.defense")}</span>
				<div class="grid grid-cols-3 sm:grid-cols-5 gap-2">
					@for(PoeOptimizeResult.DefenseHit hit : result.defenseHits())
						<div class="rounded-box bg-base-100 px-3 py-2">
							<div class="text-[11px] font-semibold ${defColor.getOrDefault(hit.type(), "text-base-content/50")}">${MessageUtil.getMessage("poe.sim.opt.result.defense." + hit.type())}</div>
							<div class="text-sm font-bold font-mono">${hit.value()}</div>
						</div>
					@endfor
				</div>
			</div>
		@endif

		@if(!result.supports().isEmpty())
			<div class="flex flex-wrap items-center gap-1.5">
				<span class="text-xs font-semibold text-base-content/50 mr-1">${MessageUtil.getMessage("poe.sim.opt.result.supports")}</span>
				@for(PoeOptimizeResult.SupportPick support : result.supports())
					<span class="badge badge-info badge-outline badge-sm">${net.luversof.web.gate.poe.PoeText.name(support.nameKo(), support.name())}</span>
				@endfor
			</div>
		@endif

		@if(result.auras() != null && !result.auras().isEmpty())
			<div class="flex flex-wrap items-center gap-1.5">
				<span class="text-xs font-semibold text-base-content/50 mr-1">${MessageUtil.getMessage("poe.sim.opt.result.auras")}</span>
				@for(PoeOptimizeResult.SupportPick aura : result.auras())
					<span class="badge badge-success badge-outline badge-sm">${net.luversof.web.gate.poe.PoeText.name(aura.nameKo(), aura.name())}</span>
				@endfor
			</div>
		@endif

		@if(result.blockedAuras() != null && !result.blockedAuras().isEmpty())
			<div class="flex flex-wrap items-center gap-1.5">
				<span class="text-xs font-semibold text-base-content/50 mr-1" title="${MessageUtil.getMessage("poe.sim.opt.result.blocked.hint")}">${MessageUtil.getMessage("poe.sim.opt.result.blocked")}</span>
				@for(PoeOptimizeResult.BlockedAura blocked : result.blockedAuras())
					<span class="badge badge-sm badge-ghost text-base-content/50 line-through decoration-base-content/30">${net.luversof.web.gate.poe.PoeText.name(blocked.nameKo(), blocked.name())}</span>
					<span class="text-[10px] font-mono text-base-content/40 -ml-1">−${blocked.shortfall()}</span>
				@endfor
			</div>
		@endif

		<!-- 속성 부족으로 실제로는 못 입는 장비 — 조용히 두면 게임에서 쓸 수 없는 빌드를 그대로 믿게 된다 -->
		@if(result.unmetRequirements() != null && !result.unmetRequirements().isEmpty())
			<div class="rounded border border-warning/40 bg-warning/10 px-3 py-2 text-xs space-y-1">
				<div class="font-semibold text-warning">${MessageUtil.getMessage("poe.sim.opt.result.unmet")}</div>
				@for(PoeOptimizeResult.UnmetRequirement unmet : result.unmetRequirements())
					<div class="font-mono text-base-content/70">${net.luversof.web.gate.poe.PoeText.name(unmet.nameKo(), unmet.name())} — ${MessageUtil.getMessage("poe.build.stat." + unmet.attribute())} ${unmet.required()} / ${unmet.actual()}</div>
				@endfor
			</div>
		@endif

		@if(result.additionalSkills() != null && !result.additionalSkills().isEmpty())
			<div class="flex flex-wrap items-center gap-1.5">
				<span class="text-xs font-semibold text-base-content/50 mr-1">${MessageUtil.getMessage("poe.sim.opt.result.extraskills")}</span>
				@for(PoeOptimizeResult.SupportPick extra : result.additionalSkills())
					<span class="badge badge-accent badge-outline badge-sm">${net.luversof.web.gate.poe.PoeText.name(extra.nameKo(), extra.name())}</span>
				@endfor
			</div>
		@endif


		@if(!result.treeNodeIds().isEmpty())
			<div class="flex flex-wrap items-center gap-1.5">
				<span class="text-xs font-semibold text-base-content/50 mr-1">${treeLabel}</span>
				!{java.util.List<Integer> nids = result.treeNotableIds();}
				@for(int ni = 0; ni < result.treeNotables().size(); ni++)
					!{int nid = nids != null && ni < nids.size() ? nids.get(ni) : 0;}
					@if(nid > 0)
						<%-- 노터블 클릭 → 트리에서 그 노드를 중앙에(도유 목록과 같은 focus 딥링크) --%>
						<a class="badge badge-warning badge-outline badge-sm link-hover" href="${treeLink}&focus=${nid}">${result.treeNotables().get(ni)}</a>
					@else
						<span class="badge badge-warning badge-outline badge-sm">${result.treeNotables().get(ni)}</span>
					@endif
				@endfor
				<a class="link link-primary text-sm ml-1" href="${treeLink}">${MessageUtil.getMessage("poe.build.summary.tree.view")}</a>
			</div>
		@endif

		<!-- 마스터리 선택 — 자동 채택된 효과("양손 숙련 — 적중 피해 60% 증가")가 안 보이면 어디서 온 수치인지 알 수 없다 -->
		@if(result.treeMasteryLabels() != null && !result.treeMasteryLabels().isEmpty())
			<div class="space-y-1.5">
				<span class="text-xs font-semibold text-base-content/50">${net.luversof.web.gate.poe.PoeText.isKorean() ? "마스터리" : "Masteries"}</span>
				<div class="flex flex-col gap-1">
					@for(String label : result.treeMasteryLabels())
						<div class="flex items-baseline gap-2 text-sm">
							<span class="badge badge-outline badge-sm shrink-0 w-16 justify-center border-sky-400 text-sky-700 dark:text-sky-300">◇ ${net.luversof.web.gate.poe.PoeText.isKorean() ? "숙련" : "Mastery"}</span>
							<span class="text-sky-700 dark:text-sky-300 whitespace-normal">${label}</span>
						</div>
					@endfor
				</div>
			</div>
		@endif

		<!-- 문신 구성 — 문신은 패시브를 통째로 교체하므로, 안 보이면 트리 노드 목록과 실제 계산이 어긋나 보인다 -->
		@if(result.treeTattooLabels() != null && !result.treeTattooLabels().isEmpty())
			<div class="space-y-1.5">
				<span class="text-xs font-semibold text-base-content/50">${net.luversof.web.gate.poe.PoeText.isKorean() ? "문신" : "Tattoos"}</span>
				<div class="flex flex-col gap-1">
					@for(String label : result.treeTattooLabels())
						!{
							String tattooNameKo = label.contains(" ×") ? label.substring(0, label.lastIndexOf(" ×")) : label;
							String tattooIconPath = tattooIcons != null ? tattooIcons.get(tattooNameKo) : null;
						}
						<div class="flex items-center gap-2 text-sm">
							<span class="badge badge-outline badge-sm shrink-0 w-16 justify-center border-purple-400 text-purple-700 dark:text-purple-300">${net.luversof.web.gate.poe.PoeText.isKorean() ? "문신" : "Tattoo"}</span>
							@if(tattooIconPath != null)
								<img src="/poe-assets/${tattooIconPath}" class="w-5 h-5 object-contain shrink-0" alt="" loading="lazy" onerror="this.remove()">
							@endif
							<span class="text-purple-700 dark:text-purple-300">${label}</span>
						</div>
					@endfor
				</div>
			</div>
		@endif

		<!-- 클러스터 주얼 구성 — 노터블만 보이면 "어떤 주얼로 얻은 건지"를 알 수 없다 -->
		@if(result.treeClusters() != null && !result.treeClusters().isBlank())
			<div class="space-y-1.5">
				<span class="text-xs font-semibold text-base-content/50">${net.luversof.web.gate.poe.PoeText.isKorean() ? "클러스터" : "Clusters"}</span>
				<div class="flex flex-col gap-1">
					@for(String entry : result.treeClusters().split(","))
						!{
							String[] parts = entry.split(":");
							String sizeKo = parts.length > 1 ? switch (parts[1]) { case "Large" -> "대형"; case "Medium" -> "중형"; case "Small" -> "소형"; default -> parts[1]; } : "";
							String sizeLabel = net.luversof.web.gate.poe.PoeText.isKorean() ? sizeKo : (parts.length > 1 ? parts[1] : "");
							String nodeCount = parts.length > 2 ? parts[2] : "";
						}
						<div class="flex items-center gap-2 text-sm">
							<span class="badge badge-secondary badge-outline badge-sm shrink-0 w-16 justify-center">${sizeLabel}</span>
							<span class="text-base-content/80">${nodeCount}${net.luversof.web.gate.poe.PoeText.isKorean() ? "노드" : ""}@if(parts.length > 4 && !parts[4].isBlank()) ·
								@for(String notable : parts[4].split("[|]"))
									<a class="link-hover underline decoration-dotted" href="/poe/clusters?q=${java.net.URLEncoder.encode(notable, java.nio.charset.StandardCharsets.UTF_8)}" title="${net.luversof.web.gate.poe.PoeText.isKorean() ? "클러스터 노터블 사전에서 보기" : "Open in cluster notables"}">${notable}</a>
								@endfor
							@endif</span>
						</div>
					@endfor
				</div>
			</div>
		@endif

		@if(result.jewels() != null && !result.jewels().isEmpty())
			<div class="space-y-1.5">
				<span class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.jewels")}</span>
				<div class="flex flex-col gap-1">
					@for(PoeOptimizeResult.SupportPick jewel : result.jewels())
						<div class="flex items-center gap-2 text-sm">
							<span class="badge badge-outline badge-sm shrink-0 w-16 justify-center border-cyan-500/40 text-cyan-500">${net.luversof.web.gate.poe.PoeText.isKorean() ? "주얼" : "Jewel"}</span>
							<span class="text-cyan-500">${net.luversof.web.gate.poe.PoeText.name(jewel.nameKo(), jewel.name())}</span>
						</div>
					@endfor
				</div>
			</div>
		@endif

		@if(!result.items().isEmpty())
			!{ boolean resultHasFlask = result.items().stream().anyMatch(i -> i.slot() != null && i.slot().startsWith("Flask")); }
			<div class="space-y-1.5">
				<span class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.items")}</span>
				<%-- 플라스크 상시 활성은 PoB 빌드 계획의 표준 가정 — 명중 +2000 처럼 가정은 화면에 드러낸다(표시=실제) --%>
				@if(resultHasFlask)
					<span class="ml-2 text-[11px] text-base-content/40">${net.luversof.web.gate.poe.PoeText.isKorean() ? "· 플라스크는 상시 활성 가정(PoB 표준)" : "· Flasks assumed permanently active"}</span>
				@endif
				<div class="flex flex-col gap-1">
					@for(PoeOptimizeResult.ItemPick item : result.items())
						!{
							// 로케일별 모드 라인 — 결과 페이지가 유일한 ko 전용 표면이었던 갭 해소(EN 은 modLinesEn, 없으면 ko 폴백)
							java.util.List<String> shownModLines =
								net.luversof.web.gate.poe.PoeText.isKorean() || item.modLinesEn() == null || item.modLinesEn().isEmpty()
									? item.modLines() : item.modLinesEn();
						}
						<div class="flex flex-wrap items-baseline gap-x-2 gap-y-0.5 text-sm">
							<span class="badge badge-ghost badge-sm shrink-0 w-16 justify-center">${net.luversof.web.gate.poe.PoeText.name(item.slotKo(), item.slot())}</span>
							@if("RARE".equals(item.rarity()))
								<%-- 레어의 slug = 베이스 아이템 상세(모드 풀·에센스·작업대) — 결과에서 제작 정보로 바로 이동.
								     호버 = 인게임식 툴팁(유니크 호버 레이어 파리티) — 데이터가 이미 여기 있어 서버 호출 없이 CSS 로만 띄운다 --%>
								<span class="relative group">
									@if(item.slug() != null)
										<a class="font-semibold text-yellow-500 link-hover" href="/poe/items/${item.slug()}">${MessageUtil.getMessage("poe.sim.opt.result.rare")} · ${net.luversof.web.gate.poe.PoeText.name(item.nameKo(), item.name())}</a>
									@else
										<span class="font-semibold text-yellow-500">${MessageUtil.getMessage("poe.sim.opt.result.rare")} · ${net.luversof.web.gate.poe.PoeText.name(item.nameKo(), item.name())}</span>
									@endif
									<div class="poe-tooltip poe-rar-rare w-80 shadow-2xl hidden group-hover:block absolute left-0 top-full z-30" data-rare-tooltip>
										<div class="poe-tt-band poe-tt-band-2">
											<div class="text-center font-bold text-[15px] leading-tight text-[#ffff77] drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]">${net.luversof.web.gate.poe.PoeText.name(item.nameKo(), item.name())}</div>
											<div class="text-center text-[13px] leading-tight text-[#ffff77] drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]">${MessageUtil.getMessage("poe.sim.opt.result.rare")}</div>
										</div>
										!{
											// (엘드리치) 임플리싯은 구분선 위(인게임 배치), 나머지는 아래 — 마커별 인게임 배색. EN 마커도 인식
											java.util.List<String> rtImplicits = new java.util.ArrayList<>();
											java.util.List<String> rtExplicits = new java.util.ArrayList<>();
											for (String ml : shownModLines) {
												if (ml.startsWith("(엘드리치)") || ml.startsWith("(eldritch)")) rtImplicits.add(ml.substring(ml.indexOf(") ") + 2));
												else rtExplicits.add(ml);
											}
										}
										@if(!rtImplicits.isEmpty())
											<div class="poe-tt-sepimg"></div>
											<div class="px-4 py-1.5 text-[12px] space-y-0.5 text-center">
												@for(String ml : rtImplicits)
													<div class="text-[#d0a2f7]">${ml}</div>
												@endfor
											</div>
										@endif
										<div class="poe-tt-sepimg"></div>
										<div class="px-4 py-1.5 text-[12px] space-y-0.5 text-center">
											@for(String ml : rtExplicits)
												!{
													int rtEnd = ml.startsWith("(") ? ml.indexOf(") ") : -1;
													String rtMk = rtEnd > 0 ? ml.substring(1, rtEnd) : null;
													String rtRest = rtMk != null ? ml.substring(rtEnd + 2) : ml;
												}
												<div class="text-[#8888ff]">@if(rtMk != null)<span class="text-[10px] text-white/35">(${rtMk})</span> @endif${rtRest}</div>
											@endfor
										</div>
									</div>
								</span>
								@if(!shownModLines.isEmpty())
									@template.poe.htmx.resultModLines(modLines = shownModLines)
								@endif
							@else
								<%-- 유니크: 목록과 같은 호버 레이어(인게임 툴팁) + 상세 페이지 링크 — 결과에서 바로 아이템을 확인할 수 있게 --%>
								@if(item.slug() != null)
									<a class="poe-hover font-semibold text-orange-500 link-hover cursor-pointer"
									   href="/poe/uniques/${item.slug()}"
									   hx-get="/poe/htmx/uniques/detail?slug=${item.slug()}"
									   hx-target="#poePreview"
									   hx-trigger="mouseenter delay:100ms"
									   hx-swap="innerHTML">${net.luversof.web.gate.poe.PoeText.name(item.nameKo(), item.name())}</a>
								@else
									<span class="font-semibold text-orange-500">${net.luversof.web.gate.poe.PoeText.name(item.nameKo(), item.name())}</span>
								@endif
								@if(!shownModLines.isEmpty())
									@template.poe.htmx.resultModLines(modLines = shownModLines)
								@endif
							@endif
						</div>
					@endfor
				</div>
			</div>
		@endif

		@if(!result.tierComparisons().isEmpty())
			<div class="space-y-1.5">
				<span class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.tiers")}</span>
				<div class="overflow-x-auto rounded-box bg-base-100">
					<table class="table table-xs">
						<thead>
							<tr>
								<th>${MessageUtil.getMessage("poe.sim.opt.result.items")}</th>
								@for(PoeOptimizeResult.TierRow row : result.tierComparisons().get(0).rows())
									<th class="text-right">${row.label()}</th>
								@endfor
							</tr>
						</thead>
						<tbody>
							@for(PoeOptimizeResult.SlotTierCompare compare : result.tierComparisons())
								<tr>
									<td class="font-semibold">${net.luversof.web.gate.poe.PoeText.name(compare.slotKo(), compare.slot())}</td>
									@for(PoeOptimizeResult.TierRow row : compare.rows())
										<td class="text-right font-mono">${row.value()}</td>
									@endfor
								</tr>
							@endfor
						</tbody>
					</table>
				</div>
			</div>
		@endif

		@if(result.scenarioMatrix() != null && !result.scenarioMatrix().isEmpty())
			<div class="space-y-1.5">
				<span class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.matrix")}</span>
				<div class="overflow-x-auto rounded-box bg-base-100">
					<table class="table table-xs">
						<thead>
							<tr>
								<th>${MessageUtil.getMessage("poe.sim.opt.scenario")}</th>
								<th class="text-right">${MessageUtil.getMessage("poe.sim.opt.result.matrix.buffoff")}</th>
								<th class="text-right">${MessageUtil.getMessage("poe.sim.opt.result.matrix.buffon")}</th>
							</tr>
						</thead>
						<tbody>
							@for(PoeOptimizeResult.ScenarioCell cell : result.scenarioMatrix())
								<tr>
									<td class="font-semibold">${net.luversof.web.gate.poe.PoeText.name(cell.scenarioKo(), cell.scenario())}</td>
									<td class="text-right font-mono text-base-content/60">${cell.dpsBuffOff()}</td>
									<td class="text-right font-mono text-primary">${cell.dpsBuffOn()}</td>
								</tr>
							@endfor
						</tbody>
					</table>
				</div>
			</div>
		@endif

		<div class="space-y-1">
			<div class="flex items-center gap-2">
				<div class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.code")}</div>
				<%-- 클립보드 복사 — PoB 에 붙여넣는 게 이 코드의 유일한 용도라 원클릭이 맞다 --%>
				<button type="button" class="btn btn-ghost btn-xs" id="poePobCopy"
				        onclick="navigator.clipboard.writeText(this.closest('div.space-y-1').querySelector('textarea').value).then(() => { this.textContent = '${net.luversof.web.gate.poe.PoeText.isKorean() ? "복사됨 ✓" : "Copied ✓"}'; setTimeout(() => { this.textContent = '${net.luversof.web.gate.poe.PoeText.isKorean() ? "복사" : "Copy"}'; }, 1500); })">${net.luversof.web.gate.poe.PoeText.isKorean() ? "복사" : "Copy"}</button>
			</div>
			<textarea readonly rows="3" class="textarea textarea-bordered w-full font-mono text-xs leading-5">${result.pobCode()}</textarea>
		</div>
	</div>
@endif
