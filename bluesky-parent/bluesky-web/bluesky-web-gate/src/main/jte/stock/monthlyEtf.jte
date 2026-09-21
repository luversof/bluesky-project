@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.text.DecimalFormat
@import java.util.List
@import net.luversof.web.gate.stock.dto.view.MonthlyEtfRowView
@import net.luversof.web.gate.stock.service.MonthlyEtfViewSupport
@import net.luversof.web.gate.stock.util.StockDetailLinkUtil
@import net.luversof.web.gate.stock.util.StockFormatUtil

<%-- 월배당 ETF 목록. 등록된 월배당 기준 데이터(프로필 + 지급 이력)로 만든 **종목 단위** 정보다.
     사용자 요청 2026-09-21: 내가 받을 배당금(시뮬레이터 월배당 탭)과 성격이 다르므로 화면을 나눴다.
     금액 · 수량 · 예상 배당금은 여기에 없다 - 보유 중인지만 표시한다. --%>
@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param List<MonthlyEtfRowView> monthlyEtfRows = java.util.List.of()
@param int monthlyEtfTotalCount = 0
@param long monthlyEtfHeldCount = 0L
@param long monthlyEtfMissingPayoutCount = 0L
@param String monthlyEtfSort = "display-order"
@param String monthlyEtfDirection = "asc"
@param String monthlyEtfKeyword = ""
@param BigDecimal monthlyEtfMinAnnualYield = null
@param String monthlyEtfPayoutWindow = ""
@param String monthlyEtfHolding = "all"
@param int monthlyEtfPeriod = 12
@param java.util.List<Integer> monthlyEtfPeriods = java.util.List.of(1, 3, 6, 12)

!{
	DecimalFormat amountFormat = new DecimalFormat("#,##0");
	DecimalFormat percentFormat = new DecimalFormat("#,##0.00");
	String referenceManageUrl = "/stock/admin?tab=monthly-reference";
	String simulatorUrl = "/stock/simulator?tab=monthly-dividend";
	// 머리칸 정렬 링크는 화면의 필터를 함께 싣는다 - 안 그러면 정렬 한 번에 검색어 · 필터가 조용히 풀린다
	// (시뮬레이터 표에서 실제로 났던 일, 2026-09-13).
	String baseUrl = "/stock/monthly-etf?"
		+ (monthlyEtfKeyword == null || monthlyEtfKeyword.isBlank() ? ""
			: "keyword=" + java.net.URLEncoder.encode(monthlyEtfKeyword, java.nio.charset.StandardCharsets.UTF_8) + "&")
		+ (monthlyEtfMinAnnualYield == null ? "" : "minAnnualYield=" + monthlyEtfMinAnnualYield.toPlainString() + "&")
		+ (monthlyEtfPayoutWindow == null || monthlyEtfPayoutWindow.isBlank() ? "" : "payoutWindow=" + monthlyEtfPayoutWindow + "&")
		+ ("all".equals(monthlyEtfHolding) ? "" : "holding=" + monthlyEtfHolding + "&")
		+ "period=" + monthlyEtfPeriod + "&";
	// 기간 단추는 자기 기간만 갈아 끼운다 - 나머지 필터와 정렬은 그대로 들고 간다.
	String periodQuery = baseUrl.substring("/stock/monthly-etf?".length())
		.replaceAll("period=[0-9]+&", "")
		+ "sort=" + monthlyEtfSort + "&direction=" + monthlyEtfDirection + "&";
	String minAnnualYieldValue = monthlyEtfMinAnnualYield != null ? monthlyEtfMinAnnualYield.stripTrailingZeros().toPlainString() : "";
	String directionIndicator = "asc".equals(monthlyEtfDirection) ? "↑" : "↓";
	String midMonthLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.mid.month");
	String monthEndLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.month.end");
	String otherWindowLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.other");
	String unknownWindowLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.unknown");
	String latestLabel = MessageUtil.getMessage("stock.simulator.monthly.table.cell.latest.short");
	String averageLabel = MessageUtil.getMessage("stock.simulator.monthly.table.cell.average.short");
}

@template._layout.stockLayout(
	pageTitle = MessageUtil.getMessage("stock.page.monthly.etf.title"),
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentStock = @`
		<div class="space-y-6">
			@template._components.ui.pageHeader(title = MessageUtil.getMessage("stock.page.monthly.etf.title"), description = MessageUtil.getMessage("stock.page.monthly.etf.desc"))

			<div class="grid gap-4 stat-card-grid grid-cols-2 lg:grid-cols-3">
				@template._components.ui.statCard(label = MessageUtil.getMessage("stock.monthly.etf.summary.count"), value = amountFormat.format(monthlyEtfTotalCount), sub = MessageUtil.getMessage("stock.monthly.etf.summary.count.sub"))
				@template._components.ui.statCard(label = MessageUtil.getMessage("stock.monthly.etf.summary.held"), value = amountFormat.format(monthlyEtfHeldCount), sub = MessageUtil.getMessage("stock.monthly.etf.summary.held.sub"))
				@template._components.ui.statCard(label = MessageUtil.getMessage("stock.monthly.etf.summary.missing.payout"), value = amountFormat.format(monthlyEtfMissingPayoutCount), sub = MessageUtil.getMessage("stock.monthly.etf.summary.missing.payout.sub"), tone = monthlyEtfMissingPayoutCount > 0 ? "warning" : "")
			</div>

			@template._components.ui.card(bodyClass = "gap-5", body = @`
				<div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
					<div class="space-y-2">
						<h2 class="card-title text-lg">${MessageUtil.getMessage("stock.monthly.etf.section.table.title")}</h2>
						<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.monthly.etf.section.table.desc")}</p>
					</div>
					<div class="flex flex-wrap items-center gap-2">
						<a href="${referenceManageUrl}" class="btn btn-outline btn-sm h-auto max-w-full whitespace-normal">${MessageUtil.getMessage("stock.monthly.etf.manage")}</a>
						<a href="${simulatorUrl}" class="btn btn-ghost btn-sm h-auto max-w-full whitespace-normal">${MessageUtil.getMessage("stock.monthly.etf.simulator.link")}</a>
					</div>
				</div>

				<form method="get" action="/stock/monthly-etf" class="grid gap-3 rounded-box bg-base-200/40 p-3 md:grid-cols-2 xl:grid-cols-5">
					<input type="hidden" name="sort" value="${monthlyEtfSort}" />
					<input type="hidden" name="direction" value="${monthlyEtfDirection}" />
					<label class="form-control min-w-0 max-w-full">
						<div class="label py-1"><span class="label-text text-xs font-medium">${MessageUtil.getMessage("stock.monthly.etf.filter.keyword")}</span></div>
						<input name="keyword" type="search" value="${monthlyEtfKeyword}" placeholder="${MessageUtil.getMessage("stock.monthly.etf.filter.keyword.placeholder")}" class="input input-bordered input-sm w-full max-w-full" />
					</label>
					<label class="form-control min-w-0 max-w-full">
						<div class="label py-1"><span class="label-text text-xs font-medium">${MessageUtil.getMessage("stock.monthly.etf.filter.min.annual.yield")}</span></div>
						<input name="minAnnualYield" type="number" step="0.01" min="0" value="${minAnnualYieldValue}" class="input input-bordered input-sm w-full max-w-full" />
					</label>
					<label class="form-control min-w-0 max-w-full">
						<div class="label py-1"><span class="label-text text-xs font-medium">${MessageUtil.getMessage("stock.monthly.etf.filter.payout.window")}</span></div>
						<select name="payoutWindow" class="select select-bordered select-sm w-full max-w-full">
							<option value="" selected="${monthlyEtfPayoutWindow == null || monthlyEtfPayoutWindow.isBlank()}">${MessageUtil.getMessage("stock.monthly.etf.filter.payout.window.all")}</option>
							<option value="MID_MONTH" selected="${"MID_MONTH".equals(monthlyEtfPayoutWindow)}">${midMonthLabel}</option>
							<option value="MONTH_END" selected="${"MONTH_END".equals(monthlyEtfPayoutWindow)}">${monthEndLabel}</option>
							<option value="OTHER" selected="${"OTHER".equals(monthlyEtfPayoutWindow)}">${otherWindowLabel}</option>
							<option value="UNKNOWN" selected="${"UNKNOWN".equals(monthlyEtfPayoutWindow)}">${unknownWindowLabel}</option>
						</select>
					</label>
					<label class="form-control min-w-0 max-w-full">
						<div class="label py-1"><span class="label-text text-xs font-medium">${MessageUtil.getMessage("stock.monthly.etf.filter.holding")}</span></div>
						<select name="holding" class="select select-bordered select-sm w-full max-w-full">
							<option value="all" selected="${"all".equals(monthlyEtfHolding)}">${MessageUtil.getMessage("stock.monthly.etf.filter.holding.all")}</option>
							<option value="held" selected="${"held".equals(monthlyEtfHolding)}">${MessageUtil.getMessage("stock.monthly.etf.filter.holding.held")}</option>
							<option value="not-held" selected="${"not-held".equals(monthlyEtfHolding)}">${MessageUtil.getMessage("stock.monthly.etf.filter.holding.not.held")}</option>
						</select>
					</label>
					<div class="flex items-end gap-2">
						<button type="submit" class="btn btn-primary btn-sm max-sm:h-auto max-sm:min-h-0 max-sm:py-2 max-sm:whitespace-normal max-sm:break-keep max-sm:wrap-anywhere">${MessageUtil.getMessage("stock.monthly.etf.filter.apply")}</button>
						<%-- 필터 지우기 링크는 일부러 기준 주소(필터를 싣는 주소)를 쓰지 않는다. --%>
						<a data-filter-reset href="/stock/monthly-etf?sort=${monthlyEtfSort}&direction=${monthlyEtfDirection}" class="btn btn-ghost btn-sm max-sm:h-auto max-sm:min-h-0 max-sm:py-2 max-sm:whitespace-normal max-sm:break-keep max-sm:wrap-anywhere">${MessageUtil.getMessage("stock.monthly.etf.filter.reset")}</a>
					</div>
				</form>

				<%-- 기간을 고르는 단추 한 줄(사용자 결정 2026-09-21). 열을 여덟 개 더 늘리는 대신 한 번에 한 기준만 본다.
				     정렬도 이 기간을 따른다 - 주소에 period 를 싣고 다니므로 정렬 링크를 눌러도 기간이 풀리지 않는다. --%>
				<div class="flex flex-wrap items-center gap-2" role="group" aria-label="${MessageUtil.getMessage("stock.monthly.etf.period.label")}">
					<span class="text-sm font-medium text-base-content/70">${MessageUtil.getMessage("stock.monthly.etf.period.label")}</span>
					@for(int periodOption : monthlyEtfPeriods)
						!{String periodLabel = periodOption == 12
						    ? MessageUtil.getMessage("stock.monthly.etf.period.year")
						    : java.text.MessageFormat.format(MessageUtil.getMessage("stock.monthly.etf.period.month"), periodOption);}
						!{boolean periodSelected = periodOption == monthlyEtfPeriod;}
						<a data-period-option href="/stock/monthly-etf?${periodQuery}period=${periodOption}"
							class="btn btn-sm h-auto min-h-8 whitespace-normal py-2 leading-snug ${periodSelected ? "btn-primary" : "btn-ghost border border-base-300"}"
							aria-current="${periodSelected ? "true" : "false"}">${periodLabel}</a>
					@endfor
				</div>

				@if(monthlyEtfRows == null || monthlyEtfRows.isEmpty())
					<div class="rounded-2xl border border-dashed border-base-300 bg-base-50 px-6 py-10 text-center">
						@if(monthlyEtfTotalCount > 0)
							<p class="text-lg font-semibold text-base-content">${MessageUtil.getMessage("stock.monthly.etf.empty.filtered.title")}</p>
							<p class="mt-2 text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.monthly.etf.empty.filtered.desc")}</p>
						@else
							<p class="text-lg font-semibold text-base-content">${MessageUtil.getMessage("stock.monthly.etf.empty.title")}</p>
							<p class="mt-2 text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.monthly.etf.empty.desc")}</p>
							<a href="${referenceManageUrl}" class="btn btn-primary btn-sm mt-4">${MessageUtil.getMessage("stock.monthly.etf.manage")}</a>
						@endif
					</div>
				@else
					<div class="overflow-x-auto">
						<table class="table table-zebra table-sm whitespace-nowrap print-dense" aria-label="${MessageUtil.getMessage("stock.monthly.etf.section.table.title")}">
							<thead>
								<tr>
									<th scope="col" class="min-w-[9rem] align-bottom" aria-sort="${"symbol".equals(monthlyEtfSort) ? ("asc".equals(monthlyEtfDirection) ? "ascending" : "descending") : "none"}">
										<a href="${baseUrl}sort=symbol&direction=${"symbol".equals(monthlyEtfSort) && "asc".equals(monthlyEtfDirection) ? "desc" : "asc"}" class="link link-hover inline-flex items-center gap-1 font-medium no-underline">
											<span>${MessageUtil.getMessage("stock.monthly.etf.table.header.item")}</span>
											@if("symbol".equals(monthlyEtfSort))<span class="text-xs text-base-content/60" aria-hidden="true">${directionIndicator}</span>@endif
										</a>
									</th>
									<th scope="col" class="align-bottom" aria-sort="${"payout-window".equals(monthlyEtfSort) ? ("asc".equals(monthlyEtfDirection) ? "ascending" : "descending") : "none"}">
										<a href="${baseUrl}sort=payout-window&direction=${"payout-window".equals(monthlyEtfSort) && "asc".equals(monthlyEtfDirection) ? "desc" : "asc"}" class="link link-hover inline-flex items-center gap-1 font-medium no-underline">
											<span>${MessageUtil.getMessage("stock.monthly.etf.table.header.payout.window")}</span>
											@if("payout-window".equals(monthlyEtfSort))<span class="text-xs text-base-content/60" aria-hidden="true">${directionIndicator}</span>@endif
										</a>
									</th>
									<th scope="col" class="align-bottom text-right" aria-sort="${"monthly-dividend".equals(monthlyEtfSort) ? ("asc".equals(monthlyEtfDirection) ? "ascending" : "descending") : "none"}">
										<a href="${baseUrl}sort=monthly-dividend&direction=${"monthly-dividend".equals(monthlyEtfSort) && "desc".equals(monthlyEtfDirection) ? "asc" : "desc"}" class="link link-hover inline-flex items-center justify-end gap-1 font-medium no-underline">
											@if("monthly-dividend".equals(monthlyEtfSort))<span class="text-xs text-base-content/60" aria-hidden="true">${directionIndicator}</span>@endif
											<span>${MessageUtil.getMessage("stock.monthly.etf.table.header.dividend.per.share")}</span>
										</a>
									</th>
									<th scope="col" class="align-bottom text-right" aria-sort="${"taxable-base".equals(monthlyEtfSort) ? ("asc".equals(monthlyEtfDirection) ? "ascending" : "descending") : "none"}">
										<a href="${baseUrl}sort=taxable-base&direction=${"taxable-base".equals(monthlyEtfSort) && "asc".equals(monthlyEtfDirection) ? "desc" : "asc"}" class="link link-hover inline-flex items-center justify-end gap-1 font-medium no-underline">
											@if("taxable-base".equals(monthlyEtfSort))<span class="text-xs text-base-content/60" aria-hidden="true">${directionIndicator}</span>@endif
											<span>${MessageUtil.getMessage("stock.monthly.etf.table.header.taxable")}</span>
										</a>
									</th>
									<th scope="col" class="align-bottom text-right">${MessageUtil.getMessage("stock.monthly.etf.table.header.price")}</th>
									<th scope="col" class="align-bottom text-right" aria-sort="${"annual-yield".equals(monthlyEtfSort) ? ("asc".equals(monthlyEtfDirection) ? "ascending" : "descending") : "none"}">
										<a href="${baseUrl}sort=annual-yield&direction=${"annual-yield".equals(monthlyEtfSort) && "desc".equals(monthlyEtfDirection) ? "asc" : "desc"}" class="link link-hover inline-flex items-center justify-end gap-1 font-medium no-underline">
											@if("annual-yield".equals(monthlyEtfSort))<span class="text-xs text-base-content/60" aria-hidden="true">${directionIndicator}</span>@endif
											<span>${MessageUtil.getMessage("stock.monthly.etf.table.header.yield")}</span>
										</a>
									</th>
									<th scope="col" class="align-bottom text-right" aria-sort="${"period-price".equals(monthlyEtfSort) ? ("asc".equals(monthlyEtfDirection) ? "ascending" : "descending") : "none"}">
										<a href="${baseUrl}sort=period-price&direction=${"period-price".equals(monthlyEtfSort) && "desc".equals(monthlyEtfDirection) ? "asc" : "desc"}" class="link link-hover no-underline">
											@if("period-price".equals(monthlyEtfSort))<span class="text-xs text-base-content/60" aria-hidden="true">${directionIndicator}</span>@endif
											<span>${MessageUtil.getMessage("stock.monthly.etf.table.header.period.price")}</span>
										</a>
									</th>
									<th scope="col" class="align-bottom text-right" aria-sort="${"period-total".equals(monthlyEtfSort) ? ("asc".equals(monthlyEtfDirection) ? "ascending" : "descending") : "none"}">
										<a href="${baseUrl}sort=period-total&direction=${"period-total".equals(monthlyEtfSort) && "desc".equals(monthlyEtfDirection) ? "asc" : "desc"}" class="link link-hover no-underline">
											@if("period-total".equals(monthlyEtfSort))<span class="text-xs text-base-content/60" aria-hidden="true">${directionIndicator}</span>@endif
											<span>${MessageUtil.getMessage("stock.monthly.etf.table.header.period.total")}</span>
											<div class="text-[11px] font-normal text-base-content/60">${MessageUtil.getMessage("stock.monthly.etf.table.header.period.total.desc")}</div>
										</a>
									</th>
									<th scope="col" class="align-bottom text-right" aria-sort="${"payout-count".equals(monthlyEtfSort) ? ("asc".equals(monthlyEtfDirection) ? "ascending" : "descending") : "none"}">
										<a href="${baseUrl}sort=payout-count&direction=${"payout-count".equals(monthlyEtfSort) && "desc".equals(monthlyEtfDirection) ? "asc" : "desc"}" class="link link-hover inline-flex items-center justify-end gap-1 font-medium no-underline">
											@if("payout-count".equals(monthlyEtfSort))<span class="text-xs text-base-content/60" aria-hidden="true">${directionIndicator}</span>@endif
											<span>${MessageUtil.getMessage("stock.monthly.etf.table.header.history")}</span>
										</a>
									</th>
								</tr>
							</thead>
							<tbody>
								@for(MonthlyEtfRowView row : monthlyEtfRows)
									!{String windowLabel = "MID_MONTH".equals(row.payoutWindow()) ? midMonthLabel : ("MONTH_END".equals(row.payoutWindow()) ? monthEndLabel : ("OTHER".equals(row.payoutWindow()) ? otherWindowLabel : unknownWindowLabel));}
									<tr data-monthly-etf-row data-symbol="${row.stockItemSymbol()}">
										<th scope="row" class="align-top font-medium">
											<div class="flex items-center gap-2 whitespace-nowrap">
												<span>${row.stockItemSymbol()}</span>
												@if(row.held())
													<span class="badge badge-xs badge-primary" data-held-badge title="${MessageUtil.getMessage("stock.monthly.etf.table.cell.held.title")}">${MessageUtil.getMessage("stock.monthly.etf.table.cell.held")}</span>
												@endif
												@if(!row.active())
													<span class="badge badge-xs badge-ghost" data-inactive-badge>${MessageUtil.getMessage("stock.monthly.etf.table.cell.inactive")}</span>
												@endif
											</div>
											<div class="mt-1 text-sm font-normal leading-tight text-base-content/70">@if(row.stockItemId() != null)<a class="link link-hover" href="${StockDetailLinkUtil.item(row.stockItemId().toString())}">${row.stockItemName()}</a>@else${row.stockItemName()}@endif</div>
										</th>
										<td class="align-top">${windowLabel}</td>
										<td class="align-top text-right">
											<div class="space-y-1 tabular-nums">
												<div><span class="mr-1 text-[11px] text-base-content/60">${averageLabel}</span><span class="amount-value">${StockFormatUtil.fullKrw(row.averageDividendPerShare1y(), 2)}</span></div>
												<div class="text-sm text-base-content/80"><span class="mr-1 text-[11px] text-base-content/60">${latestLabel}</span><span class="amount-value">${StockFormatUtil.fullKrw(row.latestDividendPerShare(), 2)}</span></div>
											</div>
										</td>
										<td class="align-top text-right">
											<div class="space-y-1 tabular-nums">
												<div>${percentFormat.format(row.averageTaxableBaseRatio1y() != null ? row.averageTaxableBaseRatio1y() : BigDecimal.ZERO)}%</div>
												<div class="text-sm text-base-content/60"><span class="mr-1 text-[11px]">${MessageUtil.getMessage("stock.monthly.etf.table.cell.taxable.per.share")}</span><span class="amount-value">${StockFormatUtil.fullKrw(row.averageTaxableBasePerShare1y(), 2)}</span></div>
											</div>
										</td>
										<%-- 시세가 없는 종목은 현재가 0 원이다. 그대로 두면 "수익률 0.00%" 가 사실처럼 읽힌다(실측 2026-09-21:
										     KODEX 200커버드콜액티브가 이력 2 건인데 0.00%). 값이 없으면 '-' 로 적고 까닭을 함께 말한다. --%>
										!{boolean priceKnown = row.currentPrice() != null && row.currentPrice().signum() > 0;}
										!{boolean priceStale = priceKnown && MonthlyEtfViewSupport.priceStale(row.currentPriceDate(), java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")));}
										<td class="align-top text-right">
											<div class="space-y-1 tabular-nums">
												@if(priceKnown)
													<div class="amount-value">${StockFormatUtil.fullKrw(row.currentPrice(), 0)}</div>
													@if(row.currentPriceDate() != null)
														<div class="text-[11px] ${priceStale ? "text-warning" : "text-base-content/60"}" data-price-date>${row.currentPriceDate().toString()}@if(priceStale) ${MessageUtil.getMessage("stock.monthly.etf.table.cell.price.stale")}@endif</div>
													@endif
												@else
													<div class="text-base-content/60" data-price-missing>${MessageUtil.getMessage("stock.monthly.etf.table.cell.price.missing")}</div>
												@endif
											</div>
										</td>
										<td class="align-top text-right">
											<div class="space-y-1 tabular-nums">
												@if(priceKnown)
													<div class="font-semibold">${percentFormat.format(row.annualYieldPct() != null ? row.annualYieldPct() : BigDecimal.ZERO)}%</div>
													<div class="text-sm text-base-content/60">${MessageUtil.getMessage("stock.monthly.etf.table.cell.monthly.yield")} ${percentFormat.format(row.monthlyYieldPct() != null ? row.monthlyYieldPct() : BigDecimal.ZERO)}%</div>
												@else
													<%-- 수익률은 현재가로 나눈 값이라 시세가 없으면 만들 수 없다 - 0% 라고 적지 않는다. --%>
													<div class="text-base-content/60" data-yield-unknown>${MessageUtil.getMessage("stock.monthly.etf.table.cell.yield.unknown")}</div>
												@endif
											</div>
										</td>
										<%-- 고른 기간의 수익률. 이력이 그 기간을 못 덮으면 값이 없다 - 그때는 언제부터 있는지 적는다. --%>
										!{boolean periodKnown = row.periodPriceReturnPct() != null && row.periodTotalReturnPct() != null;}
										<td class="align-top text-right">
											@if(periodKnown)
												<div class="font-semibold ${row.periodPriceReturnPct().signum() < 0 ? "text-info" : "text-error"}" data-period-price>${percentFormat.format(row.periodPriceReturnPct())}%</div>
												@if(row.periodBaseDate() != null)
													
													<div class="text-[11px] text-base-content/60" data-period-base>${java.text.MessageFormat.format(MessageUtil.getMessage("stock.monthly.etf.table.cell.period.base"), String.valueOf(row.periodBaseDate()))}</div>
												@endif
											@else
												<div class="text-base-content/60" data-period-missing>${row.priceHistoryStartDate() != null ? java.text.MessageFormat.format(MessageUtil.getMessage("stock.monthly.etf.table.cell.period.missing"), String.valueOf(row.priceHistoryStartDate())) : MessageUtil.getMessage("stock.monthly.etf.table.cell.yield.unknown")}</div>
											@endif
										</td>
										<td class="align-top text-right">
											@if(periodKnown)
												<div class="font-semibold ${row.periodTotalReturnPct().signum() < 0 ? "text-info" : "text-error"}" data-period-total>${percentFormat.format(row.periodTotalReturnPct())}%</div>
											@else
												<div class="text-base-content/60">-</div>
											@endif
										</td>
										<td class="align-top text-right">
											<div class="space-y-1 tabular-nums">
												<div>${java.text.MessageFormat.format(MessageUtil.getMessage("stock.monthly.etf.table.cell.payout.count"), amountFormat.format(row.payoutCount()))}</div>
												@if(row.latestPayDate() != null)
													<div class="text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.monthly.etf.table.cell.latest.pay.date")} ${row.latestPayDate().toString()}</div>
												@endif
												@if(row.lastVerifiedDate() != null)
													<div class="text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.monthly.etf.table.cell.verified")} ${row.lastVerifiedDate().toString()}</div>
												@endif
												@if(row.sourceUrl() != null && !row.sourceUrl().isBlank())
													<div class="text-[11px]"><a class="link link-hover" href="${row.sourceUrl()}" target="_blank" rel="noopener noreferrer" aria-label="${row.stockItemName() + " " + MessageUtil.getMessage("stock.monthly.etf.table.cell.source") + " (" + MessageUtil.getMessage("common.label.opens.new.window") + ")"}">${MessageUtil.getMessage("stock.monthly.etf.table.cell.source")}</a></div>
												@endif
											</div>
										</td>
									</tr>
								@endfor
							</tbody>
						</table>
					</div>
				@endif
			`)

			@template._components.ui.card(bodyClass = "gap-4", body = @`
				<h2 class="card-title text-lg">${MessageUtil.getMessage("stock.monthly.etf.criteria.title")}</h2>
				<ul class="list-disc space-y-3 pl-5 text-sm leading-6 text-base-content/70">
					<li>${MessageUtil.getMessage("stock.monthly.etf.criteria.one")}</li>
					<li>${MessageUtil.getMessage("stock.monthly.etf.criteria.two")}</li>
					<li>${MessageUtil.getMessage("stock.monthly.etf.criteria.three")}</li>
				</ul>
			`)
		</div>
	`
)
