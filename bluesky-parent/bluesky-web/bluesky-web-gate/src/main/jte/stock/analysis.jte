@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.util.List
@import net.luversof.web.gate.stock.controller.StockAnalysisViewController
@import net.luversof.web.gate.stock.httpexchange.StockAnalysisClient
@import net.luversof.web.gate.stock.support.StockViewSupport

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
@param net.luversof.web.common.menu.domain.Pagination dailyPagination = null

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
				<%-- 종목 · 기간 고르기(사용자 요청 2026-11-20): 화면 전체가 고른 한 종목 기준이다. 기본은 삼성전자 · 3 개월. --%>
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
					<button type="submit" class="btn btn-sm btn-primary">${MessageUtil.getMessage("stock.analysis.runs.show")}</button>
				</form>
				@if(focus != null)
					<p class="text-sm font-semibold" role="status">${StockViewSupport.msg("stock.analysis.focus", focus.name(), focus.symbol())}</p>
				@endif

				<div class="grid stat-card-grid grid-cols-3 gap-3">
					@template._components.ui.statCard(label = MessageUtil.getMessage("stock.analysis.card.predictions"), value = String.valueOf(summary.predictions()), amount = false)
					@template._components.ui.statCard(label = MessageUtil.getMessage("stock.analysis.card.pending"), value = String.valueOf(summary.pending()), amount = false)
					@template._components.ui.statCard(label = MessageUtil.getMessage("stock.analysis.card.late"), value = String.valueOf(summary.late()), amount = false)
				</div>

				<%-- 일자별 적중(사용자 요청 2026-11-20): 기준일마다 그날 회차(운영 회차 우선)의 D+1 · D+5 가 맞았는지. 위에 기간 안 적중 수를 센다. --%>
				<div id="analysis-daily">
				@template._components.ui.card(title = MessageUtil.getMessage("stock.analysis.daily.title"), body = @`
						@if(daily == null || daily.isEmpty())
							<p class="text-sm opacity-70">${MessageUtil.getMessage("stock.analysis.empty")}</p>
						@else
							@if(dailyHits != null)
								<p class="text-sm font-semibold mb-3">${StockViewSupport.msg("stock.analysis.daily.summary",
										String.valueOf(dailyHits.d1Hit()), String.valueOf(dailyHits.d1Graded()), rate.apply(dailyHits.d1RatePct()),
										String.valueOf(dailyHits.d5Hit()), String.valueOf(dailyHits.d5Graded()), rate.apply(dailyHits.d5RatePct()))}</p>
							@endif
							<div class="overflow-x-auto">
								<table class="table data-table" aria-label="${MessageUtil.getMessage("stock.analysis.daily.title")}">
									<thead><tr>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.col.baseDate")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.col.runTime")}</th>
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
												@for(var p : java.util.Arrays.asList(row.d1(), row.d5()))
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

				<%-- 확신도 · 프롬프트 버전별 적중률은 개선이 나아졌는지 보는 데 쓴다(사용자 결정 2026-11-20). 회차별 · 기간별은 표본이 너무 작아 뺐다. --%>
				<div class="grid gap-6 lg:grid-cols-2">
					@for(var section : List.of(
							List.of("stock.analysis.rate.confidence", summary.byConfidence()),
							List.of("stock.analysis.rate.version", summary.byPromptVersion())))
						!{@SuppressWarnings("unchecked") var rows = (List<StockAnalysisClient.RateRow>) section.get(1);}
						@template._components.ui.card(title = MessageUtil.getMessage((String) section.get(0)), body = @`
								@template.stock.htmx.fragments.components.rateTable(rows = rows, label = MessageUtil.getMessage((String) section.get(0)), rate = rate)
				`)
					@endfor
				</div>

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
