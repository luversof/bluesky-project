@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.util.List
@import net.luversof.web.gate.stock.httpexchange.StockAnalysisClient
@import net.luversof.web.gate.stock.support.StockViewSupport

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param boolean loadError = false
@param StockAnalysisClient.Summary summary = null
@param List<StockAnalysisClient.RecentPrediction> recent = null
@param StockAnalysisClient.AnalysisInput input = null
@param String keyword = ""
@param StockAnalysisClient.RunComparison runs = null
@param boolean onlyDisagree = false
@param net.luversof.web.common.menu.domain.Pagination recentPagination = null
@param net.luversof.web.common.menu.domain.Pagination runsPagination = null
@param net.luversof.web.common.menu.domain.Pagination itemsPagination = null
@param String recentPageUrl = "/stock/analysis"
@param List<StockAnalysisClient.AnalysisItem> historyOptions = null
@param String historySymbol = null
@param List<StockAnalysisClient.RateRow> historyRates = null
@param List<StockAnalysisClient.RecentPrediction> history = null
@param net.luversof.web.common.menu.domain.Pagination historyPagination = null

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
				<div class="grid stat-card-grid grid-cols-3 gap-3">
					@template._components.ui.statCard(label = MessageUtil.getMessage("stock.analysis.card.predictions"), value = String.valueOf(summary.predictions()), amount = false)
					@template._components.ui.statCard(label = MessageUtil.getMessage("stock.analysis.card.pending"), value = String.valueOf(summary.pending()), amount = false)
					@template._components.ui.statCard(label = MessageUtil.getMessage("stock.analysis.card.late"), value = String.valueOf(summary.late()), amount = false)
				</div>

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

				<div class="grid gap-6 lg:grid-cols-2">
					@for(var section : List.of(
							List.of("stock.analysis.rate.run", summary.byRun() == null ? List.of() : summary.byRun()),
							List.of("stock.analysis.rate.horizon", summary.byHorizon()),
							List.of("stock.analysis.rate.confidence", summary.byConfidence()),
							List.of("stock.analysis.rate.version", summary.byPromptVersion())))
						!{@SuppressWarnings("unchecked") var rows = (List<StockAnalysisClient.RateRow>) section.get(1);}
						@template._components.ui.card(title = MessageUtil.getMessage((String) section.get(0)), body = @`
								@template.stock.htmx.fragments.components.rateTable(rows = rows, label = MessageUtil.getMessage((String) section.get(0)), rate = rate)
				`)
					@endfor
				</div>

				<%-- 회차 비교(사용자 요청 2026-11-10): 그날 돌린 회차들을 열로 놓고 종목 · 기간별 방향 · 신뢰도를 나란히 본다. 방향이 갈린 행은 표시하고 그것만 볼 수 있다. 행은 쪽으로 나눈다(2026-11-15). --%>
				<div id="analysis-runs">
				@template._components.ui.card(title = MessageUtil.getMessage("stock.analysis.runs.title"), body = @`
					@if(runs == null || runs.date() == null)
						<p class="text-sm opacity-70">${MessageUtil.getMessage("stock.analysis.empty")}</p>
					@else
						<form method="get" action="/stock/analysis" class="flex flex-wrap items-center gap-2 mb-3">
							<input type="hidden" name="keyword" value="${keyword}" />
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
														@template._components.ui.tooltip(text = cell.rationale() == null ? cell.direction() : cell.rationale(), placement = "top", content = @`<span class="badge badge-sm badge-outline${cell.hit() == null ? "" : (cell.hit() ? " badge-success" : " badge-error")}">${cell.direction()} ${cell.confidence()}</span>`)
														@if(cell.actualDirection() != null)
															<span class="text-xs opacity-70">${cell.actualDirection()} ${pct.apply(cell.actualChangePct())}</span>
														@endif
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
								url = recentPageUrl,
								includeSelector = "this",
								targetSelector = "#analysis-runs",
								pageParam = "runsPage")
						@endif
						<p class="mt-2 text-xs text-base-content/60">${MessageUtil.getMessage("stock.analysis.runs.note")}</p>
					@endif
				`)
				</div>

				<%-- 종목 검색: 아래 최근 예측 · 종목별 지표와 뉴스를 종목명 · 코드로 거른다. --%>
				<form method="get" action="/stock/analysis" role="search" class="flex flex-wrap items-center gap-2">
					<input name="keyword" type="search" value="${keyword}" aria-label="${MessageUtil.getMessage("stock.analysis.search.placeholder")}" placeholder="${MessageUtil.getMessage("stock.analysis.search.placeholder")}" class="input input-bordered input-sm w-full max-w-xs" />
					<button type="submit" class="btn btn-sm btn-primary">${MessageUtil.getMessage("stock.analysis.search.submit")}</button>
					@if(keyword != null && !keyword.isBlank())
						<a href="/stock/analysis" class="link link-hover text-sm">${MessageUtil.getMessage("stock.analysis.search.reset")}</a>
					@endif
				</form>

				<%-- 종목별 예측(사용자 요청 2026-11-15): 고른 종목의 일자별 예측 · 결과와 버전별 적중률. 리플레이 회차도 버전 이름으로 구분해 함께 본다. --%>
				@if(historyOptions != null && !historyOptions.isEmpty())
				<div id="analysis-history">
				@template._components.ui.card(title = MessageUtil.getMessage("stock.analysis.history.title"), body = @`
						<form method="get" action="/stock/analysis" class="flex flex-wrap items-center gap-2 mb-3">
							<input type="hidden" name="keyword" value="${keyword}" />
							@if(runs != null && runs.date() != null)
								<input type="hidden" name="runDate" value="${runs.date().toString()}" />
							@endif
							<input type="hidden" name="onlyDisagree" value="${onlyDisagree}" />
							<label class="text-sm" for="analysis-history-symbol">${MessageUtil.getMessage("stock.analysis.history.symbol")}</label>
							<select id="analysis-history-symbol" name="historySymbol" class="select select-bordered select-sm">
								@for(var o : historyOptions)
									<option value="${o.symbol()}" selected="${o.symbol().equals(historySymbol)}">${o.name()} (${o.symbol()})</option>
								@endfor
							</select>
							<button type="submit" class="btn btn-sm btn-outline">${MessageUtil.getMessage("stock.analysis.runs.show")}</button>
						</form>
						<h3 class="text-sm font-semibold mb-2">${MessageUtil.getMessage("stock.analysis.history.rates")}</h3>
						@template.stock.htmx.fragments.components.rateTable(rows = historyRates, label = MessageUtil.getMessage("stock.analysis.history.rates"), rate = rate)
						@if(history == null || history.isEmpty())
							<p class="text-sm opacity-70 mt-4">${MessageUtil.getMessage("stock.analysis.empty")}</p>
						@else
							<div class="overflow-x-auto mt-4">
								<table class="table data-table" aria-label="${MessageUtil.getMessage("stock.analysis.history.title")}">
									<thead><tr>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.col.baseDate")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.col.horizon")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.col.runTime")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.col.direction")}</th>
										<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.col.confidence")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.col.result")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.col.rationale")}</th>
									</tr></thead>
									<tbody>
										@for(var p : history)
											<tr>
												<td class="whitespace-nowrap">${String.valueOf(p.baseDate())}</td>
												<td>D+${p.horizonDays()}</td>
												<td class="whitespace-nowrap">${p.createdDate() == null ? "-" : runTime.format(p.createdDate())} <span class="opacity-60 text-xs">${p.promptVersion()}</span>
													@if(p.replay() != null)
														<span class="badge badge-sm badge-outline badge-info">${MessageUtil.getMessage("stock.analysis.runs.replay")} ${p.replay()}</span>
													@endif
												</td>
												<td>${p.direction()}</td>
												<td class="text-right">${p.confidence()}</td>
												<td class="whitespace-nowrap">@template.stock.htmx.fragments.components.predictionResult(p = p, pct = pct)</td>
												<td class="text-xs min-w-64">${p.rationale()}</td>
											</tr>
										@endfor
									</tbody>
								</table>
							</div>
							@if(historyPagination != null && historyPagination.getTotalPage() > 1)
								@template._components.htmxPagination(
									pagination = historyPagination,
									url = recentPageUrl,
									includeSelector = "this",
									targetSelector = "#analysis-history",
									pageParam = "historyPage")
							@endif
						@endif
				`)
				</div>
				@endif

				<%-- 최근 예측은 쪽으로 나눈다(사용자 요청 2026-11-15). 쪽 버튼은 이 화면을 다시 받아 이 영역만 바꾼다. --%>
				<div id="analysis-recent">
				@template._components.ui.card(title = MessageUtil.getMessage("stock.analysis.recent.title"), body = @`
						@if(recent == null || recent.isEmpty())
							<p class="text-sm opacity-70">${MessageUtil.getMessage("stock.analysis.empty")}</p>
						@else
							<div class="overflow-x-auto">
								<table class="table data-table" aria-label="${MessageUtil.getMessage("stock.analysis.recent.title")}">
									<thead><tr>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.col.runTime")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.col.baseDate")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.col.stock")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.col.horizon")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.col.direction")}</th>
										<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.col.confidence")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.col.result")}</th>
										<th scope="col">${MessageUtil.getMessage("stock.analysis.col.rationale")}</th>
									</tr></thead>
									<tbody>
										@for(var p : recent)
											<tr>
												<td class="whitespace-nowrap">${p.createdDate() == null ? "-" : runTime.format(p.createdDate())} <span class="opacity-60 text-xs">#${p.runSeq()} ${p.promptVersion()}</span></td>
												<td class="whitespace-nowrap">${String.valueOf(p.baseDate())}</td>
												<td class="whitespace-nowrap">${p.name()} <span class="opacity-60 text-xs">${p.symbol()}</span></td>
												<td>D+${p.horizonDays()}</td>
												<td>${p.direction()}</td>
												<td class="text-right">${p.confidence()}</td>
												<td class="whitespace-nowrap">@template.stock.htmx.fragments.components.predictionResult(p = p, pct = pct)</td>
												<td class="text-xs min-w-64">${p.rationale()}</td>
											</tr>
										@endfor
									</tbody>
								</table>
							</div>
							@if(recentPagination != null && recentPagination.getTotalPage() > 1)
								@template._components.htmxPagination(
									pagination = recentPagination,
									url = recentPageUrl,
									includeSelector = "this",
									targetSelector = "#analysis-recent",
									pageParam = "recentPage")
							@endif
						@endif
				`)
				</div>

				<%-- 종목 현황도 쪽으로 나눈다(2026-11-15) - 종목마다 뉴스가 붙어 한 줄이 길다. --%>
				<div id="analysis-items">
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
										<th scope="col">${MessageUtil.getMessage("stock.analysis.col.news")}</th>
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
												<td class="text-xs min-w-64">
													<ul class="space-y-1">
														@for(var n : item.news().stream().limit(3).toList())
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
												</td>
											</tr>
										@endfor
									</tbody>
								</table>
							</div>
							@if(itemsPagination != null && itemsPagination.getTotalPage() > 1)
								@template._components.htmxPagination(
									pagination = itemsPagination,
									url = recentPageUrl,
									includeSelector = "this",
									targetSelector = "#analysis-items",
									pageParam = "itemsPage")
							@endif
						@endif
				`)
				</div>
			@endif
		</div>
	`
)
