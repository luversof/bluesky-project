@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.util.List
@import net.luversof.web.gate.stock.httpexchange.StockAnalysisClient

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param boolean loadError = false
@param StockAnalysisClient.Summary summary = null
@param List<StockAnalysisClient.RecentPrediction> recent = null
@param StockAnalysisClient.AnalysisInput input = null

!{java.util.function.Function<BigDecimal, String> pct = v -> v == null ? "-" : (v.signum() > 0 ? "+" : "") + v.stripTrailingZeros().toPlainString() + "%";}
!{java.util.function.Function<BigDecimal, String> num = v -> v == null ? "-" : v.stripTrailingZeros().toPlainString();}
!{java.util.function.Function<BigDecimal, String> rate = v -> v == null ? "-" : v.toPlainString() + "%";}

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
				<div class="flex flex-wrap gap-3">
					<div class="stat bg-base-200 rounded-box py-2 px-4 w-auto">
						<div class="stat-title text-xs">${MessageUtil.getMessage("stock.analysis.card.predictions")}</div>
						<div class="stat-value text-2xl">${summary.predictions()}</div>
					</div>
					<div class="stat bg-base-200 rounded-box py-2 px-4 w-auto">
						<div class="stat-title text-xs">${MessageUtil.getMessage("stock.analysis.card.pending")}</div>
						<div class="stat-value text-2xl">${summary.pending()}</div>
					</div>
					<div class="stat bg-base-200 rounded-box py-2 px-4 w-auto">
						<div class="stat-title text-xs">${MessageUtil.getMessage("stock.analysis.card.late")}</div>
						<div class="stat-value text-2xl">${summary.late()}</div>
					</div>
				</div>

				<div class="grid gap-6 lg:grid-cols-3">
					@for(var section : List.of(
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
												<th scope="col" class="text-right">${MessageUtil.getMessage("stock.analysis.col.prevDirection")}</th>
											</tr></thead>
											<tbody>
												@for(var r : rows)
													<tr>
														<td>${r.group()}</td>
														<td>D+${r.horizonDays()}</td>
														<td class="text-right">${r.graded()}</td>
														<td class="text-right font-semibold">${rate.apply(r.hitRatePct())}</td>
														<td class="text-right">${rate.apply(r.alwaysUpRatePct())}</td>
														<td class="text-right">${rate.apply(r.prevDirectionRatePct())}</td>
													</tr>
												@endfor
											</tbody>
										</table>
									</div>
								@endif
				`)
					@endfor
				</div>

				@template._components.ui.card(title = MessageUtil.getMessage("stock.analysis.recent.title"), body = @`
						@if(recent == null || recent.isEmpty())
							<p class="text-sm opacity-70">${MessageUtil.getMessage("stock.analysis.empty")}</p>
						@else
							<div class="overflow-x-auto">
								<table class="table data-table" aria-label="${MessageUtil.getMessage("stock.analysis.recent.title")}">
									<thead><tr>
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
