@import java.math.BigDecimal
@import java.text.DecimalFormat
@import java.util.List
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.stock.util.DividendPeriodBreakdown
@import net.luversof.web.gate.stock.util.StockFormatUtil

@param List<DividendPeriodBreakdown.Row> yearlyRows = java.util.List.of()
@param List<DividendPeriodBreakdown.Row> monthlyRows = java.util.List.of()
@param DecimalFormat decimalFormat = new DecimalFormat("#,##0")

!{
	// 배당 화면에는 월별 막대 차트만 있어 "그 해에 얼마 받았나" 를 숫자로 읽을 수 없었다(사용자 요청 2026-09-10).
	// 차트와 같은 지급을 지급일 기준으로 묶어 표로도 보여 준다. 합계 줄은 행을 그대로 더한 값이라 행과 어긋나지 않는다.
	String title = MessageUtil.getMessage("stock.dividend.period.title");
	String description = MessageUtil.getMessage("stock.dividend.period.desc");
	String yearTabLabel = MessageUtil.getMessage("stock.dividend.period.tab.year");
	String monthTabLabel = MessageUtil.getMessage("stock.dividend.period.tab.month");
	String periodLabel = MessageUtil.getMessage("stock.dividend.period.col.period");
	String countLabel = MessageUtil.getMessage("stock.dividend.period.col.count");
	String totalLabel = MessageUtil.getMessage("stock.dividend.period.total");
	String emptyLabel = MessageUtil.getMessage("stock.dividend.period.empty");
	String grossLabel = MessageUtil.getMessage("stock.dividend.gross");
	String taxLabel = MessageUtil.getMessage("stock.dividend.tax");
	String netLabel = MessageUtil.getMessage("stock.dividend.net");
	// 조회 구간에 그 해가 온전히 담기지 않았으면 밝힌다(자산 성장 연도별 성과와 같은 표기).
	String partialLabel = MessageUtil.getMessage("stock.asset.growth.yearly.partial");
	DividendPeriodBreakdown.Row yearlyTotal = DividendPeriodBreakdown.total(yearlyRows, totalLabel);
	DividendPeriodBreakdown.Row monthlyTotal = DividendPeriodBreakdown.total(monthlyRows, totalLabel);
}

<div class="card bg-base-100 border border-base-200 mb-4">
	<div class="card-body p-4">
		<h2 class="font-bold text-sm" data-page-section>${title}</h2>
		<p class="text-xs text-base-content/60 mb-3">${description}</p>

		<%-- 짧은 구간을 고르면 연도별은 한 줄이라 아무것도 말해 주지 않는다 - 그럴 때는 월별을 먼저 보여 준다
		     (자산 성장의 기간별 손익이 같은 이유로 달 단위를 먼저 보여 준다). 사용자가 고른 탭이 있으면 그것이 우선. --%>
		<div role="tablist" class="tabs tabs-box w-full bg-base-200/70 p-1 mb-3" data-panel-tab-group="dividendPeriod" data-panel-tab-default="${yearlyRows != null && yearlyRows.size() > 1 ? "year" : "month"}">
			<a role="tab" data-panel-tab="year" aria-controls="dividendPeriodPanel-year" class="tab flex-1 cursor-pointer text-base-content/60 hover:text-base-content" tabindex="0">${yearTabLabel}</a>
			<a role="tab" data-panel-tab="month" aria-controls="dividendPeriodPanel-month" class="tab flex-1 cursor-pointer text-base-content/60 hover:text-base-content" tabindex="0">${monthTabLabel}</a>
		</div>

		<div id="dividendPeriodPanel-year" role="tabpanel" data-panel-group="dividendPeriod" data-panel="year">
			<%-- 인쇄에서는 탭이 없어 두 표가 나란히 찍힌다 - 어느 표인지 이름이 있어야 한다(자산 성장 패널들과 같은 방식). --%>
			<h3 class="mb-2 text-sm font-bold">${yearTabLabel}</h3>
			@if(yearlyRows == null || yearlyRows.isEmpty())
				<p class="text-sm text-base-content/60 py-4">${emptyLabel}</p>
			@else
				<div class="overflow-x-auto">
					<table class="table table-zebra table-sm w-full" aria-label="${title} ${yearTabLabel}">
						<thead>
							<tr>
								<th scope="col">${periodLabel}</th>
								<th scope="col" class="text-right">${countLabel}</th>
								<th scope="col" class="text-right">${grossLabel}</th>
								<th scope="col" class="text-right">${taxLabel}</th>
								<th scope="col" class="text-right">${netLabel}</th>
							</tr>
						</thead>
						<tbody>
							@for(DividendPeriodBreakdown.Row row : yearlyRows)
								<tr>
									<th scope="row" class="font-medium">
										${row.label()}
										@if(row.partial())
											<%-- 그 해가 조회 구간에 다 담기지 않았다. 구간을 밝히지 않으면 그 해 전체로 오해한다(자산 성장 연도별 성과와 같은 표기). --%>
											<div class="text-[11px] font-normal leading-4 text-base-content/60">
												${partialLabel}
												@if(row.coveredFrom() != null && row.coveredTo() != null)
													${row.coveredFrom().toString().substring(5)} ~ ${row.coveredTo().toString().substring(5)}
												@endif
											</div>
										@endif
									</th>
									<td class="text-right tabular-nums">${decimalFormat.format(row.count())}</td>
									<td class="text-right font-mono amount-value">${decimalFormat.format(StockFormatUtil.displayWon(row.grossAmount()))}</td>
									<td class="text-right font-mono amount-value">${decimalFormat.format(StockFormatUtil.displayWon(row.tax()))}</td>
									<td class="text-right font-mono font-bold amount-value text-dividend">${decimalFormat.format(StockFormatUtil.displayWon(row.netAmount()))}</td>
								</tr>
							@endfor
							<tr class="border-t-2 border-base-content/20 font-semibold">
								<th scope="row">${totalLabel}</th>
								<td class="text-right tabular-nums">${decimalFormat.format(yearlyTotal.count())}</td>
								<td class="text-right font-mono amount-value">${decimalFormat.format(StockFormatUtil.displayWon(yearlyTotal.grossAmount()))}</td>
								<td class="text-right font-mono amount-value">${decimalFormat.format(StockFormatUtil.displayWon(yearlyTotal.tax()))}</td>
								<td class="text-right font-mono font-bold amount-value text-dividend">${decimalFormat.format(StockFormatUtil.displayWon(yearlyTotal.netAmount()))}</td>
							</tr>
						</tbody>
					</table>
				</div>
			@endif
		</div>

		<div id="dividendPeriodPanel-month" role="tabpanel" data-panel-group="dividendPeriod" data-panel="month">
			<%-- 인쇄에서는 탭이 없어 두 표가 나란히 찍힌다 - 어느 표인지 이름이 있어야 한다(자산 성장 패널들과 같은 방식). --%>
			<h3 class="mb-2 text-sm font-bold">${monthTabLabel}</h3>
			@if(monthlyRows == null || monthlyRows.isEmpty())
				<p class="text-sm text-base-content/60 py-4">${emptyLabel}</p>
			@else
				<div class="overflow-x-auto">
					<table class="table table-zebra table-sm w-full" aria-label="${title} ${monthTabLabel}">
						<thead>
							<tr>
								<th scope="col">${periodLabel}</th>
								<th scope="col" class="text-right">${countLabel}</th>
								<th scope="col" class="text-right">${grossLabel}</th>
								<th scope="col" class="text-right">${taxLabel}</th>
								<th scope="col" class="text-right">${netLabel}</th>
							</tr>
						</thead>
						<tbody>
							@for(DividendPeriodBreakdown.Row row : monthlyRows)
								<tr>
									<th scope="row" class="font-medium">${row.label()}</th>
									<td class="text-right tabular-nums">${decimalFormat.format(row.count())}</td>
									<td class="text-right font-mono amount-value">${decimalFormat.format(StockFormatUtil.displayWon(row.grossAmount()))}</td>
									<td class="text-right font-mono amount-value">${decimalFormat.format(StockFormatUtil.displayWon(row.tax()))}</td>
									<td class="text-right font-mono font-bold amount-value text-dividend">${decimalFormat.format(StockFormatUtil.displayWon(row.netAmount()))}</td>
								</tr>
							@endfor
							<tr class="border-t-2 border-base-content/20 font-semibold">
								<th scope="row">${totalLabel}</th>
								<td class="text-right tabular-nums">${decimalFormat.format(monthlyTotal.count())}</td>
								<td class="text-right font-mono amount-value">${decimalFormat.format(StockFormatUtil.displayWon(monthlyTotal.grossAmount()))}</td>
								<td class="text-right font-mono amount-value">${decimalFormat.format(StockFormatUtil.displayWon(monthlyTotal.tax()))}</td>
								<td class="text-right font-mono font-bold amount-value text-dividend">${decimalFormat.format(StockFormatUtil.displayWon(monthlyTotal.netAmount()))}</td>
							</tr>
						</tbody>
					</table>
				</div>
			@endif
		</div>
	</div>
</div>
