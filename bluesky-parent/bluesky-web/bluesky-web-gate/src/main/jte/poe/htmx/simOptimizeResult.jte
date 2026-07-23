@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe.dto.PoeBuild
@import net.luversof.web.gate.poe.dto.PoeOptimizeResult

@param PoeOptimizeResult result = null

@if(result != null)
	!{
		String gemLabel = result.gemNameKo() != null ? result.gemNameKo() : result.gemName();
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
			+ (result.treeAnoint() != null ? "&an=" + result.treeAnoint() : "");
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
		boolean fragile = (ehpValue != null && ehpValue < 10000) || !uncapped.isEmpty();
	}
	<div class="space-y-3 rounded-box border border-primary/25 bg-primary/5 p-4 mt-2">
		<div class="flex flex-wrap items-center gap-2">
			<span class="text-sm font-bold text-primary">${MessageUtil.getMessage("poe.sim.opt.result.title")}</span>
			<span class="font-bold">${gemLabel}</span>
			<span class="badge badge-primary badge-outline badge-sm">${result.classNameKo()}${result.ascendancyKo() != null ? " · " + result.ascendancyKo() : ""}</span>
			@if(result.bloodline() != null)
				<span class="badge badge-sm text-rose-500 border-rose-500/40">${MessageUtil.getMessage("poe.sim.opt.result.bloodline")}: ${net.luversof.web.gate.poe.PoeText.name(result.bloodlineKo(), result.bloodline())}</span>
			@endif
			<span class="badge badge-ghost badge-sm">${objectiveLabel}</span>
			@if(result.scenarioKo() != null)
				<span class="badge badge-error badge-outline badge-sm">${MessageUtil.getMessage("poe.sim.opt.scenario")}: ${result.scenarioKo()}</span>
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
						<div class="text-sm font-bold font-mono">${stat.value()}</div>
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
					"lightning", "text-yellow-400",
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
				@for(String notable : result.treeNotables())
					<span class="badge badge-warning badge-outline badge-sm">${notable}</span>
				@endfor
				<a class="link link-primary text-sm ml-1" href="${treeLink}">${MessageUtil.getMessage("poe.build.summary.tree.view")}</a>
			</div>
		@endif

		<!-- 마스터리 선택 — 자동 채택된 효과("양손 숙련 — 적중 피해 60% 증가")가 안 보이면 어디서 온 수치인지 알 수 없다 -->
		@if(result.treeMasteryLabels() != null && !result.treeMasteryLabels().isEmpty())
			<div class="flex flex-wrap items-center gap-1.5">
				<span class="text-xs font-semibold text-base-content/50 mr-1">${net.luversof.web.gate.poe.PoeText.isKorean() ? "마스터리" : "Masteries"}</span>
				@for(String label : result.treeMasteryLabels())
					<span class="badge badge-outline badge-sm border-sky-400 text-sky-300">◇ ${label}</span>
				@endfor
			</div>
		@endif

		<!-- 문신 구성 — 문신은 패시브를 통째로 교체하므로, 안 보이면 트리 노드 목록과 실제 계산이 어긋나 보인다 -->
		@if(result.treeTattooLabels() != null && !result.treeTattooLabels().isEmpty())
			<div class="flex flex-wrap items-center gap-1.5">
				<span class="text-xs font-semibold text-base-content/50 mr-1">${net.luversof.web.gate.poe.PoeText.isKorean() ? "문신" : "Tattoos"}</span>
				@for(String label : result.treeTattooLabels())
					<span class="badge badge-outline badge-sm border-purple-400 text-purple-300">${label}</span>
				@endfor
			</div>
		@endif

		<!-- 클러스터 주얼 구성 — 노터블만 보이면 "어떤 주얼로 얻은 건지"를 알 수 없다 -->
		@if(result.treeClusters() != null && !result.treeClusters().isBlank())
			<div class="flex flex-wrap items-center gap-1.5">
				<span class="text-xs font-semibold text-base-content/50 mr-1">${net.luversof.web.gate.poe.PoeText.isKorean() ? "클러스터" : "Clusters"}</span>
				@for(String entry : result.treeClusters().split(","))
					!{
						String[] parts = entry.split(":");
						String sizeKo = parts.length > 1 ? switch (parts[1]) { case "Large" -> "대형"; case "Medium" -> "중형"; case "Small" -> "소형"; default -> parts[1]; } : "";
						String sizeLabel = net.luversof.web.gate.poe.PoeText.isKorean() ? sizeKo : (parts.length > 1 ? parts[1] : "");
						String nodeCount = parts.length > 2 ? parts[2] : "";
						String notables = parts.length > 4 && !parts[4].isBlank() ? parts[4].replace("|", ", ") : "";
					}
					<span class="badge badge-secondary badge-outline badge-sm">
						${sizeLabel} ${nodeCount}${net.luversof.web.gate.poe.PoeText.isKorean() ? "노드" : ""}${notables.isBlank() ? "" : " · " + notables}
					</span>
				@endfor
			</div>
		@endif

		@if(result.jewels() != null && !result.jewels().isEmpty())
			<div class="flex flex-wrap items-center gap-1.5">
				<span class="text-xs font-semibold text-base-content/50 mr-1">${MessageUtil.getMessage("poe.sim.opt.result.jewels")}</span>
				@for(PoeOptimizeResult.SupportPick jewel : result.jewels())
					<span class="badge badge-sm text-cyan-500 border-cyan-500/40">${net.luversof.web.gate.poe.PoeText.name(jewel.nameKo(), jewel.name())}</span>
				@endfor
			</div>
		@endif

		@if(!result.items().isEmpty())
			<div class="space-y-1.5">
				<span class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.items")}</span>
				<div class="flex flex-col gap-1">
					@for(PoeOptimizeResult.ItemPick item : result.items())
						<div class="flex flex-wrap items-baseline gap-x-2 gap-y-0.5 text-sm">
							<span class="badge badge-ghost badge-sm shrink-0 w-16 justify-center">${item.slotKo()}</span>
							@if("RARE".equals(item.rarity()))
								<span class="font-semibold text-yellow-500">${MessageUtil.getMessage("poe.sim.opt.result.rare")} · ${net.luversof.web.gate.poe.PoeText.name(item.nameKo(), item.name())}</span>
								@if(!item.modLines().isEmpty())
									<span class="text-xs text-base-content/60">${String.join(" / ", item.modLines())}</span>
								@endif
							@else
								<span class="font-semibold text-orange-500">${net.luversof.web.gate.poe.PoeText.name(item.nameKo(), item.name())}</span>
								@if(!item.modLines().isEmpty())
									<span class="text-xs text-base-content/60">${String.join(" / ", item.modLines())}</span>
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
									<td class="font-semibold">${compare.slotKo()}</td>
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
									<td class="font-semibold">${cell.scenarioKo()}</td>
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
			<div class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.sim.opt.result.code")}</div>
			<textarea readonly rows="3" class="textarea textarea-bordered w-full font-mono text-xs leading-5">${result.pobCode()}</textarea>
		</div>
	</div>
@endif
