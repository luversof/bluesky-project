@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.util.CspNonceHolder

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param Poe2.Meta meta = null
@param Poe2.SimOptions options = null
@param String preSkill = ""
@param java.util.List<Poe2.Unique> forceUniques = java.util.List.of()
@param String preUnique = ""
@param String treeClass = ""
@param String treeAsc = ""
@param String treeNodes = ""
@param String treeAttrs = ""
@param String treeSets = ""

<%-- PoE2 시뮬레이터 — PoE1 시뮬(/poe/sim)과 같은 화면: 탭(시뮬레이션 / 젬 DPS 랭킹) · 전직·메인 스킬(검색형)·적 시나리오 → 실행 → 결과 ·
     최근 결과 이력과 지난-결과 뷰어(10-01 "PoE1 에 맞춰"). 탭 스크립트·이력 뷰어·랭킹 조각은 PoE1 sim.jte 를 그대로 옮기고 id/URL 만 poe2.
     목표는 PoE1 처럼 실빌드 성향으로 자동(미리보기에 표시). 결과는 빈 빌드에서 고르지 않고 poe.ninja 실빌드 후보에서 출발해 목표에 맞춰 다듬는다
     (PoE1 검증에서 빈 빌드 최적화는 생존력이 실빌드의 0.17~0.55배, 2026-09-30). --%>
!{
	String pageTitle = MessageUtil.getMessage("poe2.sim.title");
	boolean ko = PoeText.isKorean();
	java.util.List<Poe2.SimChoice> skills = options == null || options.skills() == null ? java.util.List.of() : options.skills();
	java.util.List<Poe2.SimChoice> ascs = options == null || options.ascendancies() == null ? java.util.List.of() : options.ascendancies();
	// 탭 스타일은 PoE1 시뮬·아이템 탭과 동일 — 같은 사이트에서 탭이 두 가지로 보이면 안 된다
	String tabActive = "tab tab-lg flex-1 font-bold tab-active !bg-primary !text-primary-content shadow-sm";
	String tabInactive = "tab tab-lg flex-1 text-base-content/60 hover:text-base-content";
}

@template._layout.poeLayout(
	title = pageTitle,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	menu = "poe2",
	contentPoe = @`
		<div class="space-y-4">
			@template.poe2.header(title = pageTitle, meta = meta)
			<%-- 시뮬레이션 / 젬 DPS 랭킹 탭 — PoE1 과 같다. 패널은 둘 다 DOM 에 남긴다(진행 중 폴링 유지). --%>
			<div id="poe2SimTabs" class="tabs tabs-box w-full bg-base-200/70 p-1" role="tablist"
			     data-tab-active="${tabActive}" data-tab-inactive="${tabInactive}">
				<button type="button" role="tab" data-poe2-sim-tab="sim" class="${tabActive}">${MessageUtil.getMessage("poe.sim.opt.title")}</button>
				<button type="button" role="tab" data-poe2-sim-tab="ranking" class="${tabInactive}">${MessageUtil.getMessage("poe.sim.ranking.title")}</button>
			</div>
			<div data-poe2-sim-panel="sim" class="space-y-3 rounded-2xl bg-base-100 p-4 shadow-sm">
				<div class="text-sm text-base-content/60 max-w-3xl">${ko
					? "전직과 주 스킬을 고르고 실행하면, poe.ninja 에서 그 전직 · 스킬을 쓰는 실빌드들의 공통 특성(많이 찍는 패시브 — 무기 세트 1 · 2 따로, 절반 이상이 끼는 고유 · 보조 젬)을 참고해 최선의 빌드를 찾습니다. 실빌드들의 장비 · 젬을 PoB-PoE2 엔진으로 다시 계산해 가장 나은 구성에서 출발하고, 공통 트리 · 고유 · 보조 젬, 가이드 교체, 패시브 재분배(기여가 거의 없는 특화를 빼고 그 점수로 더 나은 패시브 — 무기 세트 1 · 2 점수도 따로)를 하나씩 실제로 계산해 목표 점수가 오르는 것만 적용합니다(그런 교체가 더 없으면 목표 축 손해 없이 다른 축이 오르는 것도). 목표는 실빌드 성향으로 정해집니다(아래 미리보기). 내 PoB 빌드를 고칠 점은 빌드 화면의 업그레이드 가이드에서 봅니다."
					: "Pick an ascendancy and main skill and run: the simulator looks for the best build using the common traits of poe.ninja builds with that ascendancy and skill (most-taken passives — weapon sets 1 and 2 separately — and the uniques and supports at least half of them use). It recalculates their gear and gems with the PoB-PoE2 engine to start from the strongest setup, then measures the common tree, uniques, supports, guide swaps and passive respecs (refund a near-zero notable and spend the points on a better passive — weapon set 1 and 2 points separately) one by one and keeps only what raises the objective (when nothing does, also what improves the other axis with no loss on the objective). The objective follows the real-build lean (preview below). To improve your own PoB build, use the upgrade guide on the build page."}</div>
				@if(skills.isEmpty())
					@template._components.ui.emptyState(message = MessageUtil.getMessage("poe.sim.options.error"))
				@else
					<form class="space-y-3" data-no-success-toast
					      hx-post="/poe2/htmx/sim/start" hx-target="#poe2SimArea" hx-swap="innerHTML">
						<div class="flex flex-wrap items-end gap-3">
							<%-- 두 선택은 서로의 선택에 따라 "많이 쓰는 순"으로 재배치 — 한쪽이 바뀌면 이 조각만 다시 그린다(PoE1 simSelects 와 같은 방식).
								     ⚠ 트리거는 컨테이너가 받는 버블링 change — "change from:(선택자)" 는 처음 그린 select 에만 걸려,
								       조각을 한 번 다시 그린 뒤의 변경은 조용히 무시됐다(10-01 탐침: 전직을 바꾼 뒤 스킬 변경이 반영 안 됨).
								       이름으로 거른다(PoE1 과 같다) — 검색형 위젯의 검색칸도 change 를 내서(포커스를 잃을 때) 옛 스킬 값으로 먼저 요청이 나갔다(10-01). --%>
							<%-- 다시 그리기 트리거는 전직 · 스킬 select 자체에 있다(simSelects.jte, 10-25 Q178 — 거름식 change[…] 은 CSP eval 위반) --%>
							<div id="poe2SimSelects" class="contents">
								@template.poe2.htmx.simSelects(skills = skills, ascendancies = ascs, popularAscendancies = options.popularAscendancies() == null ? java.util.List.of() : options.popularAscendancies(), classes = options.classes() == null ? java.util.List.of() : options.classes(), skill = preSkill, ascendancy = treeAsc)
							</div>
							<div class="flex min-w-0 flex-wrap items-center gap-2">
								<span class="self-center sm:whitespace-nowrap text-xs text-base-content/60" title="${MessageUtil.getMessage("poe.sim.objective.auto.title")}">${MessageUtil.getMessage("poe.sim.objective.auto")}</span>
								<%-- 시나리오 문구는 PoE1 과 같은 메시지 키. "플레이어 설정 그대로"는 PoE2 만(실빌드에서 출발하므로 그 사람의 설정이 있다) --%>
								<%-- 내 트리에서 출발(10-02) — 트리 화면 "→ 시뮬"로 왔을 때만. 같은 직업 실빌드의 장비·젬에 이 트리를 끼우고 다듬는다(트리는 안 바뀐다) --%>
								@if(!treeNodes.isBlank())
									!{ int treeCount = treeNodes.split(",").length; }
									<%-- PoE1 의 "에디터에서 확정한 트리 (노드) N … 해제" 띠와 같은 문구 · 차림(10-12 C270). 직업 · 전직은 PoE2 만(트리 직업으로 후보를 거른다) --%>
									<div class="flex items-center gap-2 rounded border border-primary/40 bg-primary/5 px-3 py-1.5 text-xs" data-sim-tree>
										<span class="font-semibold text-primary">${MessageUtil.getMessage("poe.sim.opt.fixedtree")}</span>
										<span class="font-mono text-base-content/60">${treeCount}</span>
										!{
											// 직업 · 전직 이름은 화면 언어로(10-12 — 한국어 화면에도 "Warrior" 원문이 나왔다). 선택지에 없으면 원문
											String treeClassLabel = treeClass; String treeAscLabel = treeAsc;
											if (options != null && options.classes() != null) for (Poe2.SimClassGroup g : options.classes()) {
												if (treeClass.equals(g.name())) treeClassLabel = PoeText.name(g.nameKo(), g.name());
												if (g.ascendancies() != null) for (Poe2.SimChoice ch : g.ascendancies()) if (treeAsc.equals(ch.name())) treeAscLabel = PoeText.name(ch.nameKo(), ch.name());
											}
										}
										<span class="text-base-content/70">${treeClassLabel}@if(!treeAsc.isBlank()) · ${treeAscLabel}@endif</span>
										<a class="link link-primary ml-auto" href="/poe2/sim">${MessageUtil.getMessage("poe.sim.opt.fixedtree.clear")}</a>
										<input type="hidden" name="treeClass" value="${treeClass}">
										<input type="hidden" name="treeNodes" value="${treeNodes}">
										<input type="hidden" name="treeAttrs" value="${treeAttrs}">
										<input type="hidden" name="treeSets" value="${treeSets}">
									</div>
								@endif
								<%-- 고정 고유 — PoE1 시뮬 "고유 고정"의 짝(10-01). 다듬기 전에 끼우고 그 칸은 다시 안 바꾼다. 방어구·장신구만(무기는 스킬 요구와 얽힌다) --%>
								<div class="w-56 max-w-full" data-sim-unique>
									<select name="unique" data-poe-multi data-placeholder="${MessageUtil.getMessage("poe.sim.opt.uniques.placeholder")}" class="select select-sm select-bordered w-full" title="${MessageUtil.getMessage("poe2.sim.opt.unique")}">
										<option value="" selected="${preUnique.isEmpty()}">${MessageUtil.getMessage("poe2.sim.opt.unique.none")}</option>
										@for(Poe2.Unique u : forceUniques)
											<option value="${u.slug()}" selected="${u.slug().equals(preUnique)}">${PoeText.name(u.nameKo(), u.name())} · ${net.luversof.web.gate.poe.PoeClassNames.poe2(u.itemClassKo(), u.itemClass())}</option>
										@endfor
										<%-- 고유 상세에서 고른 변형(unique=slug:번호, C96) — 목록엔 고유당 하나라 그 변형 항목을 따로 고른 채로 --%>
										!{
											String preVariantLabel = null;
											if (preUnique.matches(".+:[0-9]+")) {
												String pvSlug = preUnique.substring(0, preUnique.lastIndexOf(':'));
												int pvIdx = Integer.parseInt(preUnique.substring(preUnique.lastIndexOf(':') + 1));
												for (Poe2.Unique u : forceUniques) {
													if (u.slug().equals(pvSlug) && u.variants() != null) {
														for (Poe2.UniqueVariant v : u.variants()) {
															if (v.index() != null && v.index() == pvIdx) preVariantLabel = PoeText.name(u.nameKo(), u.name()) + " (" + PoeText.name(v.nameKo(), v.name()) + ")";
														}
													}
												}
											}
										}
										@if(preVariantLabel != null)
											<option value="${preUnique}" selected>${preVariantLabel}</option>
										@endif
									</select>
								</div>
								<select name="scenario" class="select select-sm select-bordered" title="${MessageUtil.getMessage("poe.sim.opt.scenario")}">
									<option value="None">${MessageUtil.getMessage("poe.sim.opt.scenario.none")}</option>
									<option value="Boss">${MessageUtil.getMessage("poe.sim.opt.scenario.boss")}</option>
									<option value="Pinnacle" selected>${MessageUtil.getMessage("poe.sim.opt.scenario.pinnacle")}</option>
									<option value="Uber">${MessageUtil.getMessage("poe.sim.opt.scenario.uber")}</option>
									<option value="keep">${MessageUtil.getMessage("poe2.sim.opt.scenario.keep")}</option>
								</select>
								@if(isAuthenticated)
									<button type="submit" class="btn btn-primary btn-sm" data-sim-run>${MessageUtil.getMessage("poe.sim.opt.run")}</button>
								@else
									<button type="button" class="btn btn-primary btn-sm btn-disabled" disabled>${MessageUtil.getMessage("poe.sim.opt.run")}</button>
									<a class="link link-primary text-xs" href="${loginUrl}">${MessageUtil.getMessage("poe.sim.opt.login")}</a>
								@endif
							</div>
						</div>
						<%-- 실빌드 성향 미리보기 — 스킬·전직이 바뀔 때마다(선택 조각이 다시 그려진 뒤) 갱신 --%>
						<div id="poe2SimHint"
						     hx-get="/poe2/htmx/sim/archetype"
						     hx-trigger="load, htmx:afterSettle from:#poe2SimSelects"
						     hx-include="#poe2SimSelects select"
						     hx-target="this" hx-swap="innerHTML"></div>
					</form>
					<%-- 순서·미리보기·후보의 근거 = poe.ninja 스냅샷(버전) — 새 버전이 나오면 API 가 다시 받는다(30분마다 확인) --%>
					!{
						String fetched = null;
						try {
							if (options.fetchedAt() != null) fetched = java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm").format(java.time.Instant.parse(options.fetchedAt()).atZone(java.time.ZoneId.systemDefault()));
						} catch (RuntimeException ignored) {
							fetched = null;
						}
						String league = options.league() == null ? "" : options.league();
						String snap = options.snapshot() == null ? "-" : options.snapshot();
					}
					<div class="text-xs text-base-content/60" data-sim-snapshot>${ko
						? "poe.ninja " + league + " 스냅샷 " + snap + (fetched == null ? "" : " (" + fetched + " 받음)") + " · 새 버전이 나오면 자동으로 다시 받습니다"
						: "poe.ninja " + league + " snapshot " + snap + (fetched == null ? "" : " (fetched " + fetched + ")") + " · refreshed automatically when a new version is published"}</div>
				@endif
				<div id="poe2SimArea" aria-live="polite"></div>
				<%-- 최근 결과 이력 — 로드 시 채우고, 새 결과가 끝나면 상태 조각이 OOB 로 갱신(PoE1 #poeOptimizeHistory 와 같다) --%>
				<div id="poe2SimHistory" class="mt-3" hx-get="/poe2/htmx/sim/history" hx-trigger="load" hx-swap="innerHTML"></div>
				<%-- 지난 결과 뷰어 — 실행 중 폴링(#poe2SimArea)과 분리(PoE1 #poeHistoryResultViewWrap 과 같다) --%>
				<style>#poe2SimHistory button.poe-hist-active{border-color:var(--color-primary);background-color:color-mix(in oklch, var(--color-primary) 12%, transparent);font-weight:600}</style>
				<div id="poe2HistoryResultViewWrap" class="mt-3 hidden">
					<div class="flex items-center justify-between mb-1.5">
						<div class="text-xs font-semibold text-base-content/60">${MessageUtil.getMessage("poe.sim.saved.show")}</div>
						<button type="button" class="btn btn-ghost btn-xs" data-poe2-hist-close>${MessageUtil.getMessage("poe.common.close")}</button>
					</div>
					<div id="poe2HistoryResultView"></div>
				</div>
			</div>

			<%-- 젬 DPS 랭킹 (탭) — PoE1 과 같은 배치(기준 캐릭터에 젬 하나씩, 무기 요구에 맞춘 표준 무기). 제목은 탭 라벨이 대신한다 --%>
			<div data-poe2-sim-panel="ranking" class="hidden space-y-3 rounded-2xl bg-base-100 p-4 shadow-sm">
				@template.poe2.htmx.rankingWrap()
				<div id="poe2SimRanking"></div>
				<div id="poe2GemDetail"></div>
			</div>

			<%-- 메인 스킬 검색형 선택 — PoE1 과 같은 위젯(단일 선택 모드) --%>
			<script src="/js/poe/multiSelect.js"></script>
			<%-- 탭 전환·지난 결과 뷰어 — PoE1 sim.jte 스크립트를 옮겼다(?tab= 과 sessionStorage 로 보던 탭 유지, replaceState) --%>
			<script nonce="${CspNonceHolder.getNonce()}">
			(function () {
				// 인라인 onclick 대신(CSP 강제 대비, 10-12 C284) — 이력 항목 선택 표시 · 실행 중 중지 단추 잠금. 조각이 다시 그려져도 되게 문서에 한 번만 건다
				if (!window.__poeSimClicks) {
					window.__poeSimClicks = true;
					document.addEventListener('click', function (e) {
						var t = e.target && e.target.closest ? e.target : null;
						if (!t) return;
						var item = t.closest('button[data-hist-item]');
						if (item) {
							var list = document.getElementById(item.dataset.histItem);
							if (list) list.querySelectorAll('button.poe-hist-active').forEach(function (b) { b.classList.remove('poe-hist-active'); });
							item.classList.add('poe-hist-active');
						}
						var busy = t.closest('button[data-busy-label]');
						if (busy && !busy.disabled) { busy.disabled = true; busy.textContent = busy.dataset.busyLabel; }
					});
				}
				var TABS = ['sim', 'ranking'];
				var bar = document.getElementById('poe2SimTabs');
				if (!bar) return;
				var ACTIVE = bar.dataset.tabActive;
				var INACTIVE = bar.dataset.tabInactive;
				function show(name) {
					if (TABS.indexOf(name) < 0) name = 'sim';
					document.querySelectorAll('[data-poe2-sim-panel]').forEach(function (p) {
						p.classList.toggle('hidden', p.dataset.poe2SimPanel !== name);
					});
					document.querySelectorAll('[data-poe2-sim-tab]').forEach(function (b) {
						b.className = b.dataset.poe2SimTab === name ? ACTIVE : INACTIVE;
						b.setAttribute('aria-selected', String(b.dataset.poe2SimTab === name));
					});
					try { sessionStorage.setItem('poe2SimTab', name); } catch (e) { /* 프라이빗 모드 등 */ }
					try {
						var u = new URL(window.location.href);
						u.searchParams.set('tab', name);
						history.replaceState(null, '', u);
					} catch (e) { /* 구형 브라우저 */ }
				}
				document.querySelectorAll('[data-poe2-sim-tab]').forEach(function (b) {
					b.addEventListener('click', function () { show(b.dataset.poe2SimTab); });
				});
				var initial = new URLSearchParams(window.location.search).get('tab');
				if (!initial) { try { initial = sessionStorage.getItem('poe2SimTab'); } catch (e) { /* 무시 */ } }
				show(initial || 'sim');

				// 지난 결과 뷰어 — 결과가 들어오면 펼치고 목록을 화면 위로(PoE1 과 같다), 닫으면 비우고 활성 표시 해제
				var view = document.getElementById('poe2HistoryResultView');
				var wrap = document.getElementById('poe2HistoryResultViewWrap');
				if (view && wrap) {
					view.addEventListener('htmx:afterSwap', function () {
						if (!view.innerHTML.trim()) return;
						wrap.classList.remove('hidden');
						var h = document.getElementById('poe2SimHistory');
						if (h) h.scrollIntoView({ block: 'start' });
					});
					var close = wrap.querySelector('[data-poe2-hist-close]');
					if (close) close.addEventListener('click', function () {
						view.innerHTML = '';
						wrap.classList.add('hidden');
						document.querySelectorAll('#poe2SimHistory button.poe-hist-active').forEach(function (b) { b.classList.remove('poe-hist-active'); });
					});
				}
			})();
			</script>
		</div>
	`
)
