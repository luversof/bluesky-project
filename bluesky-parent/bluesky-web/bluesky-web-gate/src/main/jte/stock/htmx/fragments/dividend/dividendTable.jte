@import java.util.List
@import java.math.BigDecimal
@import java.text.DecimalFormat
@import java.time.format.DateTimeFormatter
@import net.luversof.web.gate.stock.dto.response.DividendView
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.stock.util.StockDetailLinkUtil

@param List<DividendView> dividendList
<%-- 날짜 칸에 쓸 존. 이 화면의 기간 표시·필터와 같은 존이어야 한다(부모가 요청 타임존으로 푼 값). --%>
@param java.time.ZoneId zone
@param String sort
@param DecimalFormat decimalFormat
@param java.util.function.Function<BigDecimal, String> percentFormat
@param String detailSummaryLabel
@param BigDecimal totalGrossAmount
@param BigDecimal totalNetAmount
@param BigDecimal totalTax
@param BigDecimal totalTaxableAmount
@param String accountLabel
@param String stockNameLabel
@param String grossAmountLabel
@param String netAmountLabel
@param String taxLabel
@param String taxableAmountLabel
@param String totalLabel
<%-- 빈 기간일 때 '기간 넓히기' 를 띄울지. 부모 화면이 '전체' 인지 알고 있다. --%>
@param boolean widenRangeAction = false

!{
	String zeroAmountTitle = MessageUtil.getMessage("stock.dividend.zero.amount.title");
	// 합계 줄에서 합계가 성립하지 않는 열(수량·기준 가격·기준일 원금·수익률)의 "-".
	// 이 행에는 다른 까닭이 없어 낭독기가 하이픈 네 번만 듣게 된다.
	String noTotalTitle = MessageUtil.getMessage("common.total.none");
	String sortField = sort != null ? sort.split(",")[0] : "";
	String sortDir = sort != null && sort.contains(",") ? sort.split(",")[1] : "";
	DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	// 기준 가격·기준일 원금이 비는 줄이 왜 비었는지 그 자리에서 말한다. 실측 2026-09-11(전체 기간 202건 중 5건):
	// 하나금융지주 2건은 매매 기록 자체가 없고, 삼성SDI·NAVER·HK이노엔 3건은 결산배당인데 배당 기준일이 기록돼 있지 않아
	// 지급일로 되짚는다 - 그 사이에 팔았으니 그 날 보유가 0 이다(api 의 원장 점검도 같은 이유로 이 3건을 지적하지 않는다).
	// 이 줄들은 수익률 분자·분모에서 함께 빠진다. 수익률 칸의 근거(stock.dividend.yield.basis.excluded)와 짝이다.
	// 근거는 "항상 보이는" 수익률 칸 한 곳에만 단다. 세 칸에 달았더니 행 하나가 1,146B 가 되어
	// 행당 1KB 상한(DividendTableCompactOutputTest)을 넘었다 - 실측 2026-09-11. 기준 가격·기준일 원금 칸은
	// xl 이상에서만 보이고 그때도 이 칸이 바로 옆이다.
	java.util.function.Function<DividendView, String> basisMissingTitle = item -> {
		if (item.principalCost() != null) {
			return null;
		}
		java.time.Instant basisInstant = item.recordDate() != null ? item.recordDate() : item.payDate();
		if (basisInstant == null) {
			return null;
		}
		String basisDay = dateFormatter.format(basisInstant.atZone(zone));
		String note = java.text.MessageFormat.format(MessageUtil.getMessage("stock.dividend.basis.missing"), basisDay);
		// "기준일이 따로 적혀 있지 않다" 는 비어 있는 경우만이 아니다 - 실측 2026-09-11: 이 5건은 모두 기준일이
		// 지급일과 같은 날로 적혀 있었다. api 의 원장 점검도 같은 기준(hasOwnRecordDate: 기준일이 있고 지급일과 다를 것)으로
		// 이 줄들을 지적하지 않는다. 그래서 같은 날이면 "지급일로 되짚었다" 고 밝힌다.
		boolean ownRecordDate = item.recordDate() != null && item.payDate() != null
			&& !dateFormatter.format(item.recordDate().atZone(zone)).equals(dateFormatter.format(item.payDate().atZone(zone)));
		return ownRecordDate ? note : note + " " + MessageUtil.getMessage("stock.dividend.basis.missing.paydate");
	};
	String dateLabel = MessageUtil.getMessage("common.label.date");
	String quantityLabel = MessageUtil.getMessage("common.label.quantity");
	String referencePriceLabel = MessageUtil.getMessage("stock.column.reference.price");
	String basisCostLabel = MessageUtil.getMessage("stock.dividend.yield.column.basis.cost");
	String yieldOnBasisCostLabel = MessageUtil.getMessage("stock.dividend.yield.column.yield.on.basis.cost");
	String noDividendHistoryLabel = MessageUtil.getMessage("stock.dividend.message.no.period.history");
}

	<!-- Collapsible Detail List -->
	<details id="detailSection" class="mb-4" data-persist-open="dividendDetail" data-page-section="${detailSummaryLabel}">
		<summary class="cursor-pointer select-none flex items-center gap-2 font-semibold text-sm p-3 bg-base-200 rounded-box hover:bg-base-300 transition-colors">
			<svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 transition-transform detail-arrow" viewBox="0 0 20 20" fill="currentColor">
				<path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
			</svg>
			${detailSummaryLabel}
		</summary>

		<div id="dividendListContainer" class="flex flex-col relative mt-2">
			<div class="flex-1 min-h-0 overflow-x-auto table-head-sticky-xl">
				<table class="table w-full table-sm table-pin-rows pin-top-nav" aria-label="${detailSummaryLabel}">
					<%-- 정렬 머리는 <th> 에 hx-get 만 있어 마우스 전용이었다(실측 2026-09-09: 포커스 불가). tabindex 로 닿게 하고(Enter/Space 는 common.ts),
					     정렬 상태는 ▲▼ 글자 외에 aria-sort 로도 알린다. --%>
					<thead>
					<tr class="bg-base-200">
						<th scope="col" class="cursor-pointer hover:bg-base-300" tabindex="0" aria-sort="${sortField.equals("payDate") ? (sortDir.equals("asc") ? "ascending" : "descending") : "none"}" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
							hx-vals='{"sort": "payDate,${sortField.equals("payDate") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
							${dateLabel} @if(sortField.equals("payDate"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
						</th>
						<th scope="col" class="cursor-pointer hover:bg-base-300" tabindex="0" aria-sort="${sortField.equals("stockItemName") ? (sortDir.equals("asc") ? "ascending" : "descending") : "none"}" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
							hx-vals='{"sort": "stockItemName,${sortField.equals("stockItemName") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
							${stockNameLabel} @if(sortField.equals("stockItemName"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
						</th>
						<th scope="col" class="cursor-pointer hover:bg-base-300 hidden sm:table-cell" tabindex="0" aria-sort="${sortField.equals("accountName") ? (sortDir.equals("asc") ? "ascending" : "descending") : "none"}" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
							hx-vals='{"sort": "accountName,${sortField.equals("accountName") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
							${accountLabel} @if(sortField.equals("accountName"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
						</th>
						<th scope="col" class="text-right cursor-pointer hover:bg-base-300" tabindex="0" aria-sort="${sortField.equals("grossAmount") ? (sortDir.equals("asc") ? "ascending" : "descending") : "none"}" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
							hx-vals='{"sort": "grossAmount,${sortField.equals("grossAmount") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
							${grossAmountLabel} @if(sortField.equals("grossAmount"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
						</th>
						<th scope="col" class="text-right cursor-pointer hover:bg-base-300" tabindex="0" aria-sort="${sortField.equals("netAmount") ? (sortDir.equals("asc") ? "ascending" : "descending") : "none"}" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
							hx-vals='{"sort": "netAmount,${sortField.equals("netAmount") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
							${netAmountLabel} @if(sortField.equals("netAmount"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
						</th>
						<th scope="col" class="text-right hidden md:table-cell">${quantityLabel}</th>
						<th scope="col" class="text-right hidden xl:table-cell print:table-cell">${referencePriceLabel}</th>
						<th scope="col" class="text-right hidden xl:table-cell print:table-cell">${basisCostLabel}</th>
						<th scope="col" class="text-right">${yieldOnBasisCostLabel}</th>
						<th scope="col" class="text-right cursor-pointer hover:bg-base-300 hidden lg:table-cell print:table-cell" tabindex="0" aria-sort="${sortField.equals("tax") ? (sortDir.equals("asc") ? "ascending" : "descending") : "none"}" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
							hx-vals='{"sort": "tax,${sortField.equals("tax") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
							${taxLabel} @if(sortField.equals("tax"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
						</th>
						<th scope="col" class="text-right cursor-pointer hover:bg-base-300" tabindex="0" aria-sort="${sortField.equals("taxableAmount") ? (sortDir.equals("asc") ? "ascending" : "descending") : "none"}" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListFragment" hx-select="#dividendListFragment" hx-swap="outerHTML" hx-include="#dividendSearchForm"
							hx-vals='{"sort": "taxableAmount,${sortField.equals("taxableAmount") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
							${taxableAmountLabel} @if(sortField.equals("taxableAmount"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
						</th>
					</tr>
					</thead>
					<tbody>
					@for(DividendView item : dividendList)
					<tr>
						<td>${item.payDate() != null ? dateFormatter.format(item.payDate().atZone(zone)) : ""}</td>
						<td class="font-bold"><a href="${StockDetailLinkUtil.item(item.stockItemId() != null ? item.stockItemId().toString() : "")}" class="link link-hover">${item.stockItemName()}</a></td>
						<td class="hidden sm:table-cell"><a href="${StockDetailLinkUtil.account(item.accountId() != null ? item.accountId().toString() : "")}" class="link link-hover">${item.accountName()}</a></td>
						<td class="text-right amount-value">${item.grossAmount() != null ? decimalFormat.format(item.grossAmount()) : "0"}</td>
						<td class="text-right text-dividend font-bold amount-value">${item.netAmount() != null ? decimalFormat.format(item.netAmount()) : "0"}</td>
						<td class="text-right hidden md:table-cell amount-value">${item.quantity() != null ? decimalFormat.format(item.quantity()) : "-"}</td>
						<td class="text-right hidden xl:table-cell print:table-cell amount-value">${item.referencePrice() != null ? decimalFormat.format(item.referencePrice()) : "-"}</td>
						<td class="text-right hidden xl:table-cell print:table-cell amount-value">${item.principalCost() != null ? decimalFormat.format(item.principalCost()) : "-"}</td>
						<td class="text-right text-dividend font-medium" title="${basisMissingTitle.apply(item)}">${percentFormat.apply(item.yieldOnCostPct())}@if(basisMissingTitle.apply(item) != null)<span class="sr-only">${basisMissingTitle.apply(item)}</span>@endif</td>
						<%-- 이 표에서 "-" 는 두 뜻으로 읽힌다 - 0 원(세금 147 · 과세금액 119 줄)과 값 없음(기준 가격 등 5 줄, 까닭 tooltip 있음).
						     0 을 그대로 적으면 표가 0 으로 도배되므로(2026-09-08 결정) 기호는 두고 0 쪽에만 까닭을 붙인다. --%>
						<td class="text-right hidden lg:table-cell print:table-cell amount-value" title="${item.tax() != null && item.tax().signum() == 0 ? zeroAmountTitle : null}" aria-label="${item.tax() != null && item.tax().signum() == 0 ? zeroAmountTitle : null}">${item.tax() != null && item.tax().signum() != 0 ? decimalFormat.format(item.tax()) : "-"}</td>
						<td class="text-right amount-value" title="${item.taxableAmount() != null && item.taxableAmount().signum() == 0 ? zeroAmountTitle : null}" aria-label="${item.taxableAmount() != null && item.taxableAmount().signum() == 0 ? zeroAmountTitle : null}">${item.taxableAmount() != null && item.taxableAmount().signum() != 0 ? decimalFormat.format(item.taxableAmount()) : "-"}</td>
					</tr>
					@endfor
					@if(dividendList == null || dividendList.isEmpty())
					<tr>
						<td colspan="11">@template._components.ui.emptyState(message = noDividendHistoryLabel, widenRangeAction = widenRangeAction)</td>
					</tr>
					@endif
					</tbody>
					<tfoot>
						<tr class="font-bold bg-base-200">
							<%-- 합계 라벨의 colspan 은 고정인데 그 범위 안의 계좌 열은 sm 미만에서 숨는다. 그대로 두면 합계 줄이 헤더보다
						     한 칸 넓어져 값이 다른 열 밑에 붙는다 - 실측 2026-09-11 375px: 헤더 보임 6 칸인데 합계 줄은 7 칸.
						     숨는 열은 colspan 밖으로 빼고 헤더와 같은 클래스를 단 빈 칸으로 둔다. --%>
							<th scope="row" colspan="2" class="text-center sm:text-right sm:pr-4">${totalLabel}</th>
							<td class="hidden sm:table-cell"></td>
							<td class="text-right font-mono amount-value">${totalGrossAmount != null ? decimalFormat.format(totalGrossAmount) : "0"}</td>
							<td class="text-right font-mono text-dividend amount-value">${totalNetAmount != null ? decimalFormat.format(totalNetAmount) : "0"}</td>
							<td class="text-right hidden md:table-cell" aria-label="${noTotalTitle}"><span aria-hidden="true" title="${noTotalTitle}">-</span></td>
							<td class="text-right hidden xl:table-cell print:table-cell" aria-label="${noTotalTitle}"><span aria-hidden="true" title="${noTotalTitle}">-</span></td>
							<td class="text-right hidden xl:table-cell print:table-cell" aria-label="${noTotalTitle}"><span aria-hidden="true" title="${noTotalTitle}">-</span></td>
							<td class="text-right" aria-label="${noTotalTitle}"><span aria-hidden="true" title="${noTotalTitle}">-</span></td>
							<td class="text-right font-mono hidden lg:table-cell print:table-cell amount-value">@if(totalTax == null || totalTax.signum() == 0)<span class="text-base-content/60" aria-hidden="true" title="${zeroAmountTitle}">-</span><span class="sr-only">${zeroAmountTitle}</span>@else${decimalFormat.format(totalTax)}@endif</td>
							<td class="text-right font-mono amount-value">@if(totalTaxableAmount == null || totalTaxableAmount.signum() == 0)<span class="text-base-content/60" aria-hidden="true" title="${zeroAmountTitle}">-</span><span class="sr-only">${zeroAmountTitle}</span>@else${decimalFormat.format(totalTaxableAmount)}@endif</td>
						</tr>
					</tfoot>
				</table>
			</div>

		</div>
	</details>
