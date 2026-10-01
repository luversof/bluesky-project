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

<%-- PoE2 시뮬레이터 — PoE1 시뮬(/poe/sim)과 같은 화면: 탭(시뮬레이션 / 젬 DPS 랭킹) · 전직·메인 스킬(검색형)·적 시나리오 → 실행 → 결과 ·
     최근 결과 이력과 지난-결과 뷰어(10-01 "PoE1 에 맞춰"). 탭 스크립트·이력 뷰어·랭킹 조각은 PoE1 sim.jte 를 그대로 옮기고 id/URL 만 poe2.
     목표는 PoE1 처럼 실빌드 성향으로 자동(미리보기에 표시). 결과는 빈 빌드에서 고르지 않고 poe.ninja 실빌드 후보에서 출발해 목표에 맞춰 다듬는다
     (PoE1 검증에서 빈 빌드 최적화는 생존력이 실빌드의 0.17~0.55배, 2026-09-30). --%>
!{
	String pageTitle = MessageUtil.getMessage("layout.menu.poe2.sim");
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
				<button type="button" role="tab" data-poe2-sim-tab="sim" class="${tabActive}">${pageTitle}</button>
				<button type="button" role="tab" data-poe2-sim-tab="ranking" class="${tabInactive}">${MessageUtil.getMessage("poe.sim.ranking.title")}</button>
			</div>
			<div data-poe2-sim-panel="sim" class="space-y-3 rounded-2xl bg-base-100 p-4 shadow-sm">
				<div class="text-sm text-base-content/60 max-w-3xl">${ko
					? "전직과 메인 스킬을 고르고 실행하면, 그 조건의 poe.ninja 실빌드 후보를 받아 PoB-PoE2 엔진으로 다시 계산하고 목표에 가장 맞는 빌드를 골라 자동으로 다듬습니다. 목표는 실빌드 성향으로 정해집니다(아래 미리보기)."
					: "Pick an ascendancy and main skill and run: real-build candidates from poe.ninja are recalculated with the PoB-PoE2 engine, the best fit for the objective is chosen and refined automatically. The objective follows the real-build lean (preview below)."}</div>
				@if(skills.isEmpty())
					@template._components.ui.emptyState(message = ko ? "시뮬레이터 선택지를 불러오지 못했습니다. 잠시 뒤 다시 열어 주세요." : "Could not load simulator options. Please try again shortly.")
				@else
					<form class="space-y-3" data-no-success-toast
					      hx-post="/poe2/htmx/sim/start" hx-target="#poe2SimArea" hx-swap="innerHTML">
						<div class="flex flex-wrap items-end gap-3">
							<%-- 두 선택은 서로의 선택에 따라 "많이 쓰는 순"으로 재배치 — 한쪽이 바뀌면 이 조각만 다시 그린다(PoE1 simSelects 와 같은 방식).
								     ⚠ 트리거는 컨테이너가 받는 버블링 change — "change from:(선택자)" 는 처음 그린 select 에만 걸려,
								       조각을 한 번 다시 그린 뒤의 변경은 조용히 무시됐다(10-01 탐침: 전직을 바꾼 뒤 스킬 변경이 반영 안 됨).
								       이름으로 거른다(PoE1 과 같다) — 검색형 위젯의 검색칸도 change 를 내서(포커스를 잃을 때) 옛 스킬 값으로 먼저 요청이 나갔다(10-01). --%>
							<div id="poe2SimSelects" class="contents"
							     hx-get="/poe2/htmx/sim/selects"
							     hx-trigger="change[target.name==='skill'||target.name==='ascendancy']"
							     hx-include="#poe2SimSelects select"
							     hx-target="this" hx-swap="innerHTML">
								@template.poe2.htmx.simSelects(skills = skills, ascendancies = ascs, popularAscendancies = options.popularAscendancies() == null ? java.util.List.of() : options.popularAscendancies(), classes = options.classes() == null ? java.util.List.of() : options.classes(), skill = preSkill, ascendancy = "")
							</div>
							<div class="flex min-w-0 flex-wrap items-center gap-2">
								<span class="self-center whitespace-nowrap text-xs text-base-content/60" title="${ko ? "선택한 스킬×전직의 poe.ninja 실빌드 성향으로 목표를 자동 결정" : "Objective follows the real-build lean"}">${ko ? "목표: 실빌드 성향 자동" : "Objective: auto (real-build lean)"}</span>
								<%-- 시나리오 문구는 PoE1 과 같은 메시지 키. "플레이어 설정 그대로"는 PoE2 만(실빌드에서 출발하므로 그 사람의 설정이 있다) --%>
								<%-- 고정 고유 — PoE1 시뮬 "고유 고정"의 짝(10-01). 다듬기 전에 끼우고 그 칸은 다시 안 바꾼다. 방어구·장신구만(무기는 스킬 요구와 얽힌다) --%>
								<div class="w-56" data-sim-unique>
									<select name="unique" data-poe-multi data-placeholder="${ko ? "고정 고유(선택)…" : "Lock a unique (optional)…"}" class="select select-sm select-bordered w-full" title="${ko ? "고정 고유" : "Locked unique"}">
										<option value="" selected="${preUnique.isEmpty()}">${ko ? "고정 고유 없음" : "No locked unique"}</option>
										@for(Poe2.Unique u : forceUniques)
											<option value="${u.slug()}" selected="${u.slug().equals(preUnique)}">${PoeText.name(u.nameKo(), u.name())} · ${PoeText.name(u.itemClassKo(), u.itemClass())}</option>
										@endfor
									</select>
								</div>
								<select name="scenario" class="select select-sm select-bordered" title="${MessageUtil.getMessage("poe.sim.opt.scenario")}">
									<option value="None">${MessageUtil.getMessage("poe.sim.opt.scenario.none")}</option>
									<option value="Boss">${MessageUtil.getMessage("poe.sim.opt.scenario.boss")}</option>
									<option value="Pinnacle" selected>${MessageUtil.getMessage("poe.sim.opt.scenario.pinnacle")}</option>
									<option value="Uber">${MessageUtil.getMessage("poe.sim.opt.scenario.uber")}</option>
									<option value="keep">${ko ? "플레이어 설정 그대로" : "Keep player setting"}</option>
								</select>
								@if(isAuthenticated)
									<button type="submit" class="btn btn-primary btn-sm" data-sim-run>${ko ? "시뮬레이션 실행" : "Run simulation"}</button>
								@else
									<button type="button" class="btn btn-primary btn-sm btn-disabled" disabled>${ko ? "시뮬레이션 실행" : "Run simulation"}</button>
									<a class="link link-primary text-xs" href="${loginUrl}">${ko ? "로그인하면 실행할 수 있습니다" : "Sign in to run"}</a>
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
					<div class="text-xs text-base-content/50" data-sim-snapshot>${ko
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
						<div class="text-xs font-semibold text-base-content/60">${ko ? "지난 결과 보기" : "Saved result"}</div>
						<button type="button" class="btn btn-ghost btn-xs" data-poe2-hist-close>✕ ${ko ? "닫기" : "Close"}</button>
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
