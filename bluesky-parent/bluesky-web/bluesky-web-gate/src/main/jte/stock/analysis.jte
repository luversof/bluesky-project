@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.util.List
@import net.luversof.web.gate.stock.controller.StockAnalysisViewController
@import net.luversof.web.gate.stock.httpexchange.StockAnalysisClient
@import net.luversof.web.gate.stock.support.StockViewSupport
@import net.luversof.web.gate.stock.util.StockAnalysisLabelUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param boolean loadError = false
@param StockAnalysisClient.Summary summary = null
@param StockAnalysisClient.AnalysisInput input = null
@param StockAnalysisClient.RunComparison runs = null
@param boolean onlyDisagree = false
@param net.luversof.web.common.menu.domain.Pagination runsPagination = null
@param String pageUrl = "/stock/analysis"
@param List<StockAnalysisClient.AnalysisItem> symbolOptions = null
@param String symbol = null
@param String period = "3m"
@param java.util.Map<String, String> periods = java.util.Map.of()
@param StockAnalysisClient.AnalysisItem focus = null
@param List<StockAnalysisViewController.DailyRow> daily = null
@param StockAnalysisViewController.DailyHits dailyHits = null
@param List<StockAnalysisViewController.ConfidenceCut> confidenceCuts = null
@param net.luversof.web.common.menu.domain.Pagination dailyPagination = null
@param StockAnalysisClient.ReleaseNotes releases = null
@param StockAnalysisViewController.VersionCompare versionCompare = null
@param List<String> dailyVersions = List.of()
@param String adoptedVersion = null
@param String dailyVersion = "live"
@param List<String> slots = List.of()
@param String dailySlot = "PREMARKET"
@param StockAnalysisViewController.Upcoming upcoming = null
@param List<StockAnalysisViewController.SlotHits> slotHits = null

!{java.util.function.Function<BigDecimal, String> pct = v -> v == null ? "-" : (v.signum() > 0 ? "+" : "") + v.stripTrailingZeros().toPlainString() + "%";}
!{java.util.function.Function<BigDecimal, String> num = v -> v == null ? "-" : v.stripTrailingZeros().toPlainString();}
!{java.util.function.Function<BigDecimal, String> rate = v -> v == null ? "-" : v.toPlainString() + "%";}
!{java.time.format.DateTimeFormatter runTime = java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(java.time.ZoneId.of("Asia/Seoul"));}

@template._layout.stockLayout(
	pageTitle = MessageUtil.getMessage("stock.analysis.title"),
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentStock = @`
		<div class="space-y-6">
			@template._components.ui.pageHeader(title = MessageUtil.getMessage("stock.analysis.title"), description = MessageUtil.getMessage("stock.analysis.note"))

			@if(loadError || summary == null)
				<div role="alert" class="alert alert-warning">${MessageUtil.getMessage("stock.analysis.loadError")}</div>
			@else
				<%-- 종목 · 기간 고르기(사용자 요청 2026-11-20): 화면 전체가 고른 한 종목 기준이다. 기본은 한 종목 · 3 개월. --%>
				<form method="get" action="/stock/analysis" class="flex flex-wrap items-center gap-2">
					<label class="text-sm" for="analysis-symbol">${MessageUtil.getMessage("stock.analysis.history.symbol")}</label>
					<select id="analysis-symbol" name="symbol" class="select select-bordered select-sm">
						@if(symbolOptions != null)
							@for(var o : symbolOptions)
								<option value="${o.symbol()}" selected="${o.symbol().equals(symbol)}">${o.name()} (${o.symbol()})</option>
							@endfor
						@endif
					</select>
					<label class="text-sm" for="analysis-period">${MessageUtil.getMessage("stock.analysis.period")}</label>
					<select id="analysis-period" name="period" class="select select-bordered select-sm">
						@for(var p : periods.entrySet())
							<option value="${p.getKey()}" selected="${p.getKey().equals(period)}">${MessageUtil.getMessage(p.getValue())}</option>
						@endfor
					</select>
					<%-- 일자별 적중 버전(사용자 요청 2026-11-22): 기본은 가장 최근 채택 버전. "운영 회차 우선"은 그날 실제로 돈 회차(버전 섞임). --%>
					<label class="text-sm" for="analysis-version">${MessageUtil.getMessage("stock.analysis.version")}</label>
					<select id="analysis-version" name="version" class="select select-bordered select-sm">
						@for(var v : dailyVersions)
							<option value="${v}" selected="${v.equals(dailyVersion)}">${v.equals(adoptedVersion) ? StockViewSupport.msg("stock.analysis.version.adopted", v) : v}</option>
						@endfor
						<option value="live" selected="${"live".equals(dailyVersion)}">${MessageUtil.getMessage("stock.analysis.version.live")}</option>
					</select>
					<%-- 분석 시점(2026-11-27): 버전은 같은 (기준일, 시점) 에서만 비교한다. 기본은 08:30 장전가 뒤. --%>
					<label class="text-sm" for="analysis-slot">${MessageUtil.getMessage("stock.analysis.slot")}</label>
					<select id="analysis-slot" name="slot" class="select select-bordered select-sm">
						@for(var sl : slots)
							<option value="${sl}" selected="${sl.equals(dailySlot)}">${StockAnalysisLabelUtil.slot(sl)}</option>
						@endfor
					</select>
					<button type="submit" class="btn btn-sm btn-primary">${MessageUtil.getMessage("stock.analysis.runs.show")}</button>
				</form>
				@if(focus != null)
					<p class="text-sm font-semibold" role="status">${StockViewSupport.msg("stock.analysis.focus", focus.name(), focus.symbol())}</p>
				@endif
				@if(adoptedVersion != null)
					<p class="text-sm" role="status"><span class="badge badge-sm badge-outline badge-primary">${MessageUtil.getMessage("stock.analysis.adopted.badge")}</span> ${StockViewSupport.msg("stock.analysis.adopted.now", adoptedVersion)}</p>
				@endif

				<div class="grid stat-card-grid grid-cols-3 gap-3">
					@template._components.ui.statCard(label = MessageUtil.getMessage("stock.analysis.card.predictions"), value = String.valueOf(summary.predictions()), amount = false)
					@template._components.ui.statCard(label = MessageUtil.getMessage("stock.analysis.card.pending"), value = String.valueOf(summary.pending()), amount = false)
					@template._components.ui.statCard(label = MessageUtil.getMessage("stock.analysis.card.late"), value = String.valueOf(summary.late()), amount = false)
				</div>

				<%-- 다음 거래일 예측(사용자 요청 2026-11-27): 가장 최근 기준일의 회차를 모두, 늦게 만든 것부터. 아무 때나 돌린 회차가 쌓인다. --%>
				@if(upcoming != null && !upcoming.runs().isEmpty())
					@template._components.ui.card(title = StockViewSupport.msg("stock.analysis.upcoming.title", String.valueOf(upcoming.targetDate()), String.valueOf(upcoming.baseDate())), body = @`
						<div class="overflow-x-auto">
							<table class="table data-table" aria-label="${MessageUtil.getMessage("stock.analysis.upcoming.aria")}">
								<thead><tr>
									<th scope="col">${MessageUtil.getMessage("stock.analysis.col.runTime")}</th>
									<th scope="col">${MessageUtil.getMessage("stock.analysis.slot")}</th>
									<th scope="col">${MessageUtil.getMessage("stock.analysis.version")}</th>
									<th scope="col">D+1</th>
									<th scope="col">D+5</th>
								</tr></thead>
								<tbody>
									@for(var r : upcoming.runs())
										<tr>
											<td class="whitespace-nowrap">${r.createdDate() == null ? "-" : runTime.format(r.createdDate())}
												<span class="badge badge-sm badge-outline ${r.live() ? "badge-primary" : "badge-info"}">${MessageUtil.getMessage(r.live() ? "stock.analysis.upcoming.live" : "stock.analysis.upcoming.backfill")}</span>
											</td>
											<td class="whitespace-nowrap">${StockAnalysisLabelUtil.slot(r.slot())}</td>
											<td class="whitespace-nowrap">${r.version()}</td>
											@for(var p : java.util.Arrays.asList(r.d1(), r.d5()))
												<td class="whitespace-nowrap">
													@if(p == null)
														-
													@else
														@template.stock.htmx.fragments.components.predictionBadge(direction = p.direction(), confidence = p.confidence(), rationale = p.rationale(), hit = p.hit(), actualDirection = p.actualDirection(), actualChangePct = p.actualChangePct(), pct = pct)
													@endif
												</td>
											@endfor
										</tr>
									@endfor
								</tbody>
							</table>
						</div>
						<p class="mt-2 text-xs text-base-content/60">${MessageUtil.getMessage("stock.analysis.upcoming.note")}</p>
					`)
				@endif

				<%-- 시점별 적중(2026-11-27): 고른 버전 · 기간에서 언제 돌린 예측이 얼마나 맞나. 장중은 장이 열린 뒤 만든 늦은 예측. --%>
				@if(slotHits != null && !slotHits.isEmpty())
					@template._components.ui.card(title = MessageUtil.getMessage("stock.analysis.slotHits.title"), body = @`
						<div class="overflow-x-auto">
							<table class="table data-table" aria-label="${MessageUtil.getMessage("stock.analysis.slotHits.title")}">
								<thead><tr>
									<th scope="col">${MessageUtil.getMessage("stock.analysis.slot")}</th>
									<th scope="col" class="text-right">D+1</th>
									<th scope="col" class="text-right">D+5</th>
								</tr></thead>
								<tbody>
									@for(var h : slotHits)
										<tr>
											<td class="whitespace-nowrap">${StockAnalysisLabelUtil.slot(h.slot())}</td>
											<td class="whitespace-nowrap text-right">${h.hits().d1Graded() == 0 ? "-" : StockViewSupport.msg("stock.analysis.slotHits.value", rate.apply(h.hits().d1RatePct()), String.valueOf(h.hits().d1Hit()), String.valueOf(h.hits().d1Graded()))}</td>
											<td class="whitespace-nowrap text-right">${h.hits().d5Graded() == 0 ? "-" : StockViewSupport.msg("stock.analysis.slotHits.value", rate.apply(h.hits().d5RatePct()), String.valueOf(h.hits().d5Hit()), String.valueOf(h.hits().d5Graded()))}</td>
										</tr>
									@endfor
								</tbody>
							</table>
						</div>
						<p class="mt-2 text-xs text-base-content/60">${MessageUtil.getMessage("stock.analysis.slotHits.note")}</p>
					`)
				@endif

				<%-- 일자별 적중(사용자 요청 2026-11-20): 기준일마다 그날 회차(운영 회차 우선)의 D+1 · D+5 가 맞았는지. 위에 기간 안 적중 수를 센다. --%>
				<div id="analysis-daily">
				@template._components.ui.card(title = "live".equals(dailyVersion) ? MessageUtil.getMessage("stock.analysis.daily.title.live") : StockViewSupport.msg("stock.analysis.daily.title.version", dailyVersion), body = @`
						@if(daily == null || daily.isEmpty())
							<p class="text-sm opacity-70">${MessageUtil.getMessage("stock.analysis.empty")}</p>
						@else
							@if(dailyHits != null)
								<p class="text-sm font-semibold mb-3">${StockViewSupport.msg("stock.analysis.daily.summary",
										String.valueOf(dailyHits.d1Hit()), String.valueOf(dailyHits.d1Graded()), rate.apply(dailyHits.d1RatePct()),
										String.valueOf(dailyHits.d5Hit()), String.valueOf(dailyHits.d5Graded()), rate.apply(dailyHits.d5RatePct()))}</p>
							@endif
							<%-- 확신 높은 날만(사용자 요청 2026-11-23): 확신도 기준 이상인 날만 따르면 몇 일을 따르고 얼마나 맞나. 위 표와 같은 버전 · 기간. --%>
							@if(dailyHits != null && confidenceCuts != null && confidenceCuts.size() > 1)
								<div class="overflow-x-auto mb-4">
									<table class="table data-table" aria-label="${MessageUtil.getMessage("stock.analysis.cut.title")}">
										<caption class="text-left text-sm font-semibold">${MessageUtil.getMessage("stock.analysis.cut.title")}</caption>
										<thead><tr>
											<th scope="col">${MessageUtil.getMessage("stock.analysis.cut.col.min")}</th>
											<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.cut.col.d1")}</th>
											<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.cut.col.d5")}</th>
										</tr></thead>
										<tbody>
											@for(var c : confidenceCuts)
												<tr>
													<td class="whitespace-nowrap">${c.min() == 0 ? MessageUtil.getMessage("stock.analysis.cut.all") : StockViewSupport.msg("stock.analysis.cut.min", String.valueOf(c.min()))}</td>
													@for(var h : List.of(List.of(c.hits().d1Hit(), c.hits().d1Graded(), dailyHits.d1Graded()), List.of(c.hits().d5Hit(), c.hits().d5Graded(), dailyHits.d5Graded())))
														<td class="whitespace-nowrap text-right">${h.get(1) == 0 ? "-" : StockViewSupport.msg("stock.analysis.cut.value",
																rate.apply(StockAnalysisViewController.DailyHits.ratePct(h.get(0), h.get(1))), String.valueOf(h.get(0)), String.valueOf(h.get(1)),
																rate.apply(StockAnalysisViewController.DailyHits.ratePct(h.get(1), h.get(2)).setScale(0, java.math.RoundingMode.HALF_UP)))}</td>
													@endfor
												</tr>
											@endfor
										</tbody>
									</table>
									<p class="mt-1 text-xs text-base-content/60">${MessageUtil.getMessage("stock.analysis.cut.note")}</p>
								</div>
							@endif
							<div class="overflow-x-auto">
								<table class="table data-table" aria-label="${MessageUtil.getMessage("stock.analysis.daily.title")}">
									<thead><tr>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.col.baseDate")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.col.runTime")}</th>
										<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.col.basePrice")}</th>
										<th scope="col">D+1</th>
										<th scope="col">D+5</th>
									</tr></thead>
									<tbody>
										@for(var row : daily)
											!{var run = row.run();}
											<tr>
												<td class="whitespace-nowrap">${String.valueOf(row.baseDate())}</td>
												<td class="whitespace-nowrap">${run.createdDate() == null ? "-" : runTime.format(run.createdDate())} <span class="opacity-60 text-xs">${run.promptVersion()}</span>
													@if(run.replay() != null)
														<span class="badge badge-sm badge-outline badge-info">${MessageUtil.getMessage("stock.analysis.runs.replay")} ${run.replay()}</span>
													@endif
												</td>
												<td class="whitespace-nowrap text-right">${run.basePrice() == null ? "-" : String.format("%,d", run.basePrice().setScale(0, java.math.RoundingMode.HALF_UP).longValue())}</td>
												@for(var p : java.util.Arrays.asList(row.d1(), row.d5()))
													<td class="whitespace-nowrap">
														@if(p == null)
															-
														@else
															@template.stock.htmx.fragments.components.predictionBadge(direction = p.direction(), confidence = p.confidence(), rationale = p.rationale(), hit = p.hit(), actualDirection = p.actualDirection(), actualChangePct = p.actualChangePct(), pct = pct)
															!{var close = StockAnalysisViewController.actualClose(p);}
															@if(close != null)
																<div class="text-xs opacity-70">${StockViewSupport.msg("stock.analysis.daily.price", String.format("%,d", net.luversof.web.gate.stock.util.StockFormatUtil.displayWon(close)), p.targetDate() == null ? "-" : p.targetDate().toString().substring(5))}</div>
															@endif
														@endif
													</td>
												@endfor
											</tr>
										@endfor
									</tbody>
								</table>
							</div>
							@if(dailyPagination != null && dailyPagination.getTotalPage() > 1)
								@template._components.htmxPagination(
									pagination = dailyPagination,
									url = pageUrl,
									includeSelector = "this",
									targetSelector = "#analysis-daily",
									pageParam = "dailyPage")
							@endif
							<p class="mt-2 text-xs text-base-content/60">${MessageUtil.getMessage("stock.analysis.daily.note")}</p>
						@endif
				`)
				</div>

				<%-- 같은 날짜 버전 비교(사용자 요청 2026-11-21): 새 버전으로 예전 날짜를 다시 분석한 재분석 회차와 그날의 예전 버전을 나란히 놓고, 같은 날짜끼리만 적중을 센다. 재분석은 아침 작업이 새 버전마다 지난 60 거래일을 하루 10일씩 돌린다. --%>
				@if(versionCompare != null && !versionCompare.rows().isEmpty())
				@template._components.ui.card(title = MessageUtil.getMessage("stock.analysis.compare.title"), body = @`
						<p class="text-xs text-base-content/60 mb-3">${MessageUtil.getMessage("stock.analysis.compare.note")}</p>
						@if(!versionCompare.vsNewest().isEmpty())
							<ul class="text-sm font-semibold mb-3 space-y-1">
								@for(var h : versionCompare.vsNewest())
									<li>${StockViewSupport.msg("stock.analysis.compare.pair", versionCompare.versions().get(0), h.version(),
											String.valueOf(h.d1Days()), String.valueOf(h.d1NewestHit()), String.valueOf(h.d1Hit()),
											String.valueOf(h.d5Days()), String.valueOf(h.d5NewestHit()), String.valueOf(h.d5Hit()))}</li>
								@endfor
							</ul>
						@endif
						<div class="overflow-x-auto">
							<table class="table data-table" aria-label="${MessageUtil.getMessage("stock.analysis.compare.title")}">
								<thead><tr>
									<th scope="col">${MessageUtil.getMessage("stock.analysis.col.baseDate")}</th>
									@for(var v : versionCompare.versions())
										<th scope="col" class="whitespace-nowrap">${v}
											@if(v.equals(adoptedVersion))
												<span class="badge badge-sm badge-outline badge-primary">${MessageUtil.getMessage("stock.analysis.adopted.badge")}</span>
											@endif
											@if(v.equals(versionCompare.versions().get(0)))
												<span class="badge badge-sm badge-outline badge-primary">${MessageUtil.getMessage("stock.analysis.compare.base")}</span>
											@endif
										</th>
									@endfor
								</tr></thead>
								<tbody>
									@for(var row : versionCompare.rows())
										<tr>
											<td class="whitespace-nowrap">${String.valueOf(row.baseDate())}</td>
											@for(var v : versionCompare.versions())
												!{var cell = row.cells().get(v);}
												<td class="whitespace-nowrap">
													@if(cell == null)
														-
													@else
														@for(var p : java.util.Arrays.asList(cell.d1(), cell.d5()))
															@if(p != null)
																<div class="text-xs"><span class="opacity-60">D+${p.horizonDays()}</span>
																	@template.stock.htmx.fragments.components.predictionBadge(direction = p.direction(), confidence = p.confidence(), rationale = p.rationale(), hit = p.hit(), actualDirection = p.actualDirection(), actualChangePct = p.actualChangePct(), pct = pct)
																</div>
															@endif
														@endfor
													@endif
												</td>
											@endfor
										</tr>
									@endfor
								</tbody>
							</table>
						</div>
				`)
				@endif

				<%-- 확신도 · 프롬프트 버전별 적중률은 개선이 나아졌는지 보는 데 쓴다(사용자 결정 2026-11-20). 회차별 · 기간별은 표본이 너무 작아 뺐다.
				     야간선물 변동 구간별(사용자 요청 2026-11-21): 리플레이에서 0.5% 미만인 날만 50% 아래였던 것이 운영 · 재분석에서도 그런지. --%>
				<div class="grid gap-6 lg:grid-cols-2">
					@for(var section : List.of(
							List.of("stock.analysis.rate.confidence", summary.byConfidence()),
							List.of("stock.analysis.rate.version", summary.byPromptVersion()),
						List.of("stock.analysis.rate.night", summary.byNightMove() == null ? List.of() : summary.byNightMove())))
						!{@SuppressWarnings("unchecked") var rows = (List<StockAnalysisClient.RateRow>) section.get(1);}
						@template._components.ui.card(title = MessageUtil.getMessage((String) section.get(0)), body = @`
								@template.stock.htmx.fragments.components.rateTable(rows = rows, label = MessageUtil.getMessage((String) section.get(0)), rate = rate)
				`)
					@endfor
				</div>

				<%-- 장전가 그림자 집계(사용자 요청 2026-11-21): NXT 08:49 가격 방향으로 D+1 을 예측한 셈 치고 채점한다. 표본 밖(2026-11-24 ~)에서도 운영보다 나은지 본다. 예측을 저장하지 않는다. --%>
				@if(summary.premarketShadow() != null && !summary.premarketShadow().isEmpty())
				@template._components.ui.card(title = MessageUtil.getMessage("stock.analysis.shadow.title"), body = @`
						<p class="text-xs text-base-content/60 mb-3">${MessageUtil.getMessage("stock.analysis.shadow.note")}</p>
						<div class="overflow-x-auto">
							<table class="table data-table" aria-label="${MessageUtil.getMessage("stock.analysis.shadow.title")}">
								<thead><tr>
									<th scope="col">${MessageUtil.getMessage("stock.analysis.shadow.col.period")}</th>
									<th scope="col">${MessageUtil.getMessage("stock.analysis.shadow.col.band")}</th>
									<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.shadow.col.days")}</th>
									<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.shadow.col.hit")}</th>
									<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.shadow.col.paired")}</th>
								</tr></thead>
								<tbody>
									@for(var s : summary.premarketShadow())
										!{String periodKey = "OOS".equals(s.period()) ? "stock.analysis.shadow.period.oos" : "stock.analysis.shadow.period.in";}
										<tr class="${"all".equals(s.band()) ? "font-semibold" : null}">
											<td class="whitespace-nowrap">${MessageUtil.getMessage(periodKey)}</td>
											<td class="whitespace-nowrap">${"all".equals(s.band()) ? MessageUtil.getMessage("stock.analysis.shadow.band.all") : s.band()}</td>
											<td class="text-right">${s.days()}</td>
											<td class="text-right">${StockViewSupport.msg("stock.analysis.shadow.rate", String.valueOf(s.hit()), String.valueOf(s.days()), rate.apply(s.days() == 0 ? null : BigDecimal.valueOf(s.hit() * 100.0 / s.days()).setScale(1, java.math.RoundingMode.HALF_UP)))}</td>
											<td class="text-right whitespace-nowrap">${s.pairedDays() == 0 ? "-" : StockViewSupport.msg("stock.analysis.shadow.pairedValue", String.valueOf(s.pairedDays()), String.valueOf(s.pairedHit()), String.valueOf(s.pairedSystemHit()))}</td>
										</tr>
									@endfor
								</tbody>
							</table>
						</div>
				`)
				@endif

				<%-- 버전 기록(사용자 요청 2026-11-21): 버전마다 바꾼 것 · 리플레이 결과 · 판단, 그리고 다음 개선 후보. 내용은 api-stock resources/analysis/releases.json. --%>
				@if(releases != null && releases.releases() != null && !releases.releases().isEmpty())
				@template._components.ui.card(title = MessageUtil.getMessage("stock.analysis.release.title"), body = @`
						<p class="text-xs text-base-content/60 mb-3">${MessageUtil.getMessage("stock.analysis.release.note")}</p>
							<div class="overflow-x-auto">
								<table class="table data-table" aria-label="${MessageUtil.getMessage("stock.analysis.release.title")}">
									<thead><tr>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.release.col.date")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.release.col.version")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.release.col.area")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.release.col.verdict")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.release.col.change")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.release.col.result")}</th>
									</tr></thead>
									<tbody>
										<%-- 가장 위(최신)의 채택 줄이 지금 쓰는 버전(사용자 요청 2026-11-22: 어떤 게 최근 채택인지 보기 편하게). --%>
										!{var current = releases.releases().stream().filter(x -> "ADOPTED".equals(x.verdict())).findFirst().orElse(null);}
										@for(var r : releases.releases())
											!{String verdictKey = switch (r.verdict()) { case "ADOPTED" -> "stock.analysis.release.verdict.adopted"; case "REJECTED" -> "stock.analysis.release.verdict.rejected"; case "HOLD" -> "stock.analysis.release.verdict.hold"; default -> "stock.analysis.release.verdict.baseline"; };}
											!{String verdictClass = switch (r.verdict()) { case "ADOPTED" -> " badge-success"; case "REJECTED" -> " badge-error"; case "HOLD" -> " badge-warning"; default -> ""; };}
											<tr class="${r == current ? "bg-primary/10" : null}">
												<td class="whitespace-nowrap">${String.valueOf(r.date())}</td>
												<td class="whitespace-nowrap font-semibold">${r.version()}
													@if(r == current)
														<span class="badge badge-sm badge-outline badge-primary">${MessageUtil.getMessage("stock.analysis.adopted.badge")}</span>
													@endif
												</td>
												<td class="whitespace-nowrap">${r.area() == null ? "-" : r.area()}</td>
												<td class="whitespace-nowrap"><span class="badge badge-sm badge-outline${verdictClass}">${MessageUtil.getMessage(verdictKey)}</span></td>
												<td class="min-w-64 text-sm">${r.change()}</td>
												<td class="min-w-64 text-xs text-base-content/70">${r.result() == null ? "-" : r.result()}</td>
											</tr>
										@endfor
									</tbody>
								</table>
							</div>
							@if(releases.next() != null && !releases.next().isEmpty())
								<h3 class="text-sm font-semibold mt-5 mb-2">${MessageUtil.getMessage("stock.analysis.release.next")}</h3>
								<div class="overflow-x-auto">
									<table class="table data-table" aria-label="${MessageUtil.getMessage("stock.analysis.release.next")}">
										<thead><tr>
											<th scope="col">${MessageUtil.getMessage("stock.analysis.release.col.priority")}</th>
											<th scope="col">${MessageUtil.getMessage("stock.analysis.release.col.candidate")}</th>
											<th scope="col">${MessageUtil.getMessage("stock.analysis.release.col.judgment")}</th>
										</tr></thead>
										<tbody>
											@for(var n : releases.next())
												!{String priorityKey = switch (n.priority()) { case "HIGH" -> "stock.analysis.release.priority.high"; case "MEDIUM" -> "stock.analysis.release.priority.medium"; default -> "stock.analysis.release.priority.low"; };}
												!{String priorityClass = switch (n.priority()) { case "HIGH" -> " badge-primary"; case "MEDIUM" -> " badge-info"; default -> ""; };}
												<tr>
													<td class="whitespace-nowrap"><span class="badge badge-sm badge-outline${priorityClass}">${MessageUtil.getMessage(priorityKey)}</span></td>
													<td class="min-w-48 font-semibold">${n.title()}</td>
													<td class="min-w-64 text-xs text-base-content/70">${n.judgment()}</td>
												</tr>
											@endfor
										</tbody>
									</table>
								</div>
						@endif
				`)
				@endif

				<%-- 회차 비교(사용자 요청 2026-11-10): 그날 돌린 회차들을 열로 놓고 기간별 방향 · 신뢰도를 나란히 본다. 고른 종목만. 행은 쪽으로 나눈다(2026-11-15). --%>
				<div id="analysis-runs">
				@template._components.ui.card(title = MessageUtil.getMessage("stock.analysis.runs.title"), body = @`
					@if(runs == null || runs.date() == null)
						<p class="text-sm opacity-70">${MessageUtil.getMessage("stock.analysis.empty")}</p>
					@else
						<form method="get" action="/stock/analysis" class="flex flex-wrap items-center gap-2 mb-3">
							@if(symbol != null)
								<input type="hidden" name="symbol" value="${symbol}" />
							@endif
							<input type="hidden" name="period" value="${period}" />
							<label class="text-sm" for="analysis-run-date">${MessageUtil.getMessage("stock.analysis.runs.date")}</label>
							<select id="analysis-run-date" name="runDate" class="select select-bordered select-sm">
								@for(var d : runs.dates())
									<option value="${d.toString()}" selected="${d.equals(runs.date())}">${d.toString()}</option>
								@endfor
							</select>
							<label class="label cursor-pointer gap-2 text-sm">
								<input type="checkbox" name="onlyDisagree" value="true" class="checkbox checkbox-sm" checked="${onlyDisagree}" />
								${MessageUtil.getMessage("stock.analysis.runs.onlyDisagree")}
							</label>
							<button type="submit" class="btn btn-sm btn-outline">${MessageUtil.getMessage("stock.analysis.runs.show")}</button>
						</form>
						<div class="overflow-x-auto">
							<table class="table data-table" aria-label="${MessageUtil.getMessage("stock.analysis.runs.title")}">
								<thead><tr>
									<th scope="col">${MessageUtil.getMessage("stock.analysis.col.stock")}</th>
									<th scope="col">${MessageUtil.getMessage("stock.analysis.col.horizon")}</th>
									@for(var run : runs.runs())
										<th scope="col">
											<div class="whitespace-nowrap">#${run.seq()} ${run.createdDate() == null ? "" : runTime.format(run.createdDate()).substring(6)} ${run.promptVersion()}
												@if(run.replay() != null)
													<span class="badge badge-sm badge-outline badge-info">${MessageUtil.getMessage("stock.analysis.runs.replay")} ${run.replay()}</span>
												@endif
											</div>
											<div class="text-xs font-normal opacity-70 whitespace-nowrap">${StockViewSupport.msg("stock.analysis.runs.summary", rate.apply(run.hitRatePct()), rate.apply(run.flatPct()))}</div>
										</th>
									@endfor
								</tr></thead>
								<tbody>
									@for(var row : runs.rows())
										<tr>
											<td class="whitespace-nowrap">${row.name()} <span class="opacity-60 text-xs">${row.symbol()}</span>
												@if(row.disagree())
													<span class="badge badge-sm badge-outline badge-warning">${MessageUtil.getMessage("stock.analysis.runs.disagree")}</span>
												@endif
											</td>
											<td>D+${row.horizonDays()}</td>
											@for(var run : runs.runs())
												!{var cell = row.cells().get(run.runId().toString());}
												<td class="whitespace-nowrap">
													@if(cell == null)
														-
													@else
														@template.stock.htmx.fragments.components.predictionBadge(direction = cell.direction(), confidence = cell.confidence(), rationale = cell.rationale(), hit = cell.hit(), actualDirection = cell.actualDirection(), actualChangePct = cell.actualChangePct(), pct = pct)
													@endif
												</td>
											@endfor
										</tr>
									@endfor
								</tbody>
							</table>
						</div>
						@if(runsPagination != null && runsPagination.getTotalPage() > 1)
							@template._components.htmxPagination(
								pagination = runsPagination,
								url = pageUrl,
								includeSelector = "this",
								targetSelector = "#analysis-runs",
								pageParam = "runsPage")
						@endif
						<p class="mt-2 text-xs text-base-content/60">${MessageUtil.getMessage("stock.analysis.runs.note")}</p>
					@endif
				`)
				</div>

				<%-- 종목 현황: 고른 종목의 지표 · 뉴스(사용자 요청 2026-11-20 - 전 종목을 늘어놓지 않는다). --%>
				@template._components.ui.card(title = MessageUtil.getMessage("stock.analysis.items.title"), body = @`
						@if(input == null || input.items().isEmpty())
							<p class="text-sm opacity-70">${MessageUtil.getMessage("stock.analysis.empty")}</p>
						@else
							<div class="overflow-x-auto">
								<table class="table data-table" aria-label="${MessageUtil.getMessage("stock.analysis.items.title")}">
									<thead><tr>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.col.stock")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.col.baseDate")}</th>
										<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.col.close")}</th>
										<th scope="col" class="text-right">D-1 @template._components.ui.infoTip(desc = MessageUtil.getMessage("stock.analysis.term.d1"))</th>
										<th scope="col" class="text-right">D-5 @template._components.ui.infoTip(desc = MessageUtil.getMessage("stock.analysis.term.d5"))</th>
										<th scope="col" class="text-right">D-20 @template._components.ui.infoTip(desc = MessageUtil.getMessage("stock.analysis.term.d20"))</th>
										<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.col.ma20Gap")} @template._components.ui.infoTip(desc = MessageUtil.getMessage("stock.analysis.term.ma20Gap"))</th>
										<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.col.volumeRatio")} @template._components.ui.infoTip(desc = MessageUtil.getMessage("stock.analysis.term.volumeRatio"))</th>
									</tr></thead>
									<tbody>
										@for(var item : input.items())
											<tr>
												<td class="whitespace-nowrap">${item.name()} <span class="opacity-60 text-xs">${item.symbol()}</span></td>
												<td class="whitespace-nowrap">${String.valueOf(item.baseDate())}</td>
												<td class="text-right">${num.apply(item.lastClose())}</td>
												<td class="text-right">${pct.apply(item.indicators().change1dPct())}</td>
												<td class="text-right">${pct.apply(item.indicators().change5dPct())}</td>
												<td class="text-right">${pct.apply(item.indicators().change20dPct())}</td>
												<td class="text-right">${pct.apply(item.indicators().closeVsMa20Pct())}</td>
												<td class="text-right">${num.apply(item.indicators().volumeRatio20())}</td>
											</tr>
										@endfor
									</tbody>
								</table>
							</div>
							<%-- 한 종목이라 뉴스는 표 칸에 세 줄로 줄이지 않고 아래에 목록으로 다 보인다. --%>
							@for(var item : input.items())
								<h3 class="text-sm font-semibold mt-4 mb-2">${MessageUtil.getMessage("stock.analysis.col.news")}</h3>
								@if(item.news().isEmpty())
									<p class="text-sm opacity-70">${MessageUtil.getMessage("stock.analysis.empty")}</p>
								@else
									<ul class="space-y-1 text-sm">
										@for(var n : item.news())
											<li>
												<span class="badge badge-outline badge-sm">${n.source()}</span>
												@if(n.url() != null && n.url().startsWith("http"))
													<a class="link link-hover" href="${n.url()}" target="_blank" rel="noopener noreferrer">${n.title()}<span class="sr-only"> ${MessageUtil.getMessage("common.label.opens.new.window")}</span></a>
												@else
													${n.title()}
												@endif
											</li>
										@endfor
									</ul>
								@endif
							@endfor
						@endif
				`)

				<%-- 시장 상황(v2, 2026-11-10): 예측이 보는 공통 근거 - 간밤 미국 · 변동성 · 금리 · 환율 · 국내 지수. --%>
				@if(input != null && input.market() != null && !input.market().lines().isEmpty())
					@template._components.ui.card(title = MessageUtil.getMessage("stock.analysis.market.title"), body = @`
						<div class="overflow-x-auto">
							<table class="table data-table" aria-label="${MessageUtil.getMessage("stock.analysis.market.title")}">
								<thead><tr>
									<th scope="col">${MessageUtil.getMessage("stock.analysis.market.col.indicator")}</th>
									<th scope="col">${MessageUtil.getMessage("stock.analysis.col.baseDate")}</th>
									<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.col.close")}</th>
									<th scope="col" class="text-right">D-1</th>
									<th scope="col" class="text-right">D-5</th>
								</tr></thead>
								<tbody>
									@for(var line : input.market().lines())
										<tr>
											<th scope="row" class="font-medium">${line.name()}</th>
											<td>${line.lastDate() != null ? line.lastDate().toString() : "-"}</td>
											<td class="text-right">${num.apply(line.close())}</td>
											<td class="text-right ${line.change1dPct() == null ? "" : (line.change1dPct().signum() > 0 ? "text-profit" : (line.change1dPct().signum() < 0 ? "text-loss" : ""))}">${pct.apply(line.change1dPct())}</td>
											<td class="text-right ${line.change5dPct() == null ? "" : (line.change5dPct().signum() > 0 ? "text-profit" : (line.change5dPct().signum() < 0 ? "text-loss" : ""))}">${pct.apply(line.change5dPct())}</td>
										</tr>
									@endfor
								</tbody>
							</table>
						</div>
						@if(!input.market().unavailable().isEmpty())
							<p class="mt-2 text-xs text-base-content/60">${StockViewSupport.msg("stock.analysis.market.unavailable", String.join(", ", input.market().unavailable()))}</p>
						@endif
					`)
				@endif
			@endif
		</div>
	`
)
