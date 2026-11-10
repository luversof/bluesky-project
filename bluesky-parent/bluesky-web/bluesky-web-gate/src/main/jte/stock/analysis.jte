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
								@if(rows.isEmpty())
									<p class="text-sm opacity-70">${MessageUtil.getMessage("stock.analysis.empty")}</p>
								@else
									<div class="overflow-x-auto">
										<table class="table data-table w-full" aria-label="${MessageUtil.getMessage((String) section.get(0))}">
											<thead><tr>
												<th scope="col">${MessageUtil.getMessage("stock.analysis.col.group")}</th>
												<th scope="col">${MessageUtil.getMessage("stock.analysis.col.horizon")}</th>
												<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.col.graded")}</th>
												<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.col.hitRate")}</th>
												<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.col.alwaysUp")}</th>
												<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.col.trend")}</th>
												<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.col.majority")}</th>
												<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.col.edge")}</th>
											</tr></thead>
											<tbody>
												@for(var r : rows)
													<%-- 표본이 적으면 흐리게: 적중률이 우연에 크게 흔들린다. --%>
													<tr class="${r.graded() < 20 ? "opacity-60" : null}">
														<td>${r.group()}</td>
														<td>D+${r.horizonDays()}</td>
														<td class="text-right">${r.graded()}</td>
														<td class="text-right font-semibold">${rate.apply(r.hitRatePct())}</td>
														<td class="text-right">${rate.apply(r.alwaysUpRatePct())}</td>
														<td class="text-right">${rate.apply(r.trendRatePct())}</td>
														<td class="text-right">${rate.apply(r.majorityRatePct())}</td>
														<td class="text-right font-semibold ${r.edgePct() == null ? "" : (r.edgePct().signum() > 0 ? "text-profit" : "text-loss")}">${r.edgePct() == null ? "-" : (r.edgePct().signum() > 0 ? "+" : "") + r.edgePct().toPlainString() + "%p"}</td>
													</tr>
												@endfor
											</tbody>
										</table>
									</div>
									<p class="mt-2 text-xs text-base-content/60">${MessageUtil.getMessage("stock.analysis.rate.note")}</p>
								@endif
				`)
					@endfor
				</div>

				<%-- 종목 검색: 아래 최근 예측 · 종목별 지표와 뉴스를 종목명 · 코드로 거른다. --%>
				<form method="get" action="/stock/analysis" role="search" class="flex flex-wrap items-center gap-2">
					<input name="keyword" type="search" value="${keyword}" aria-label="${MessageUtil.getMessage("stock.analysis.search.placeholder")}" placeholder="${MessageUtil.getMessage("stock.analysis.search.placeholder")}" class="input input-bordered input-sm w-full max-w-xs" />
					<button type="submit" class="btn btn-sm btn-primary">${MessageUtil.getMessage("stock.analysis.search.submit")}</button>
					@if(keyword != null && !keyword.isBlank())
						<a href="/stock/analysis" class="link link-hover text-sm">${MessageUtil.getMessage("stock.analysis.search.reset")}</a>
					@endif
				</form>

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
												<td class="whitespace-nowrap">
													@if(p.hit() == null)
														<span class="badge badge-sm badge-outline">${MessageUtil.getMessage("stock.analysis.result.pending")}</span>
													@else
														<span class="badge badge-sm badge-outline ${p.hit() ? "badge-success" : "badge-error"}">${MessageUtil.getMessage(p.hit() ? "stock.analysis.result.hit" : "stock.analysis.result.miss")}</span>
														<span class="text-xs">${p.actualDirection()} ${pct.apply(p.actualChangePct())}</span>
														@if(Boolean.TRUE.equals(p.late()))
															<span class="badge badge-warning badge-sm badge-outline">${MessageUtil.getMessage("stock.analysis.result.late")}</span>
														@endif
													@endif
												</td>
												<td class="text-xs min-w-64">${p.rationale()}</td>
											</tr>
										@endfor
									</tbody>
								</table>
							</div>
						@endif
				`)

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
						@endif
				`)
			@endif
		</div>
	`
)
