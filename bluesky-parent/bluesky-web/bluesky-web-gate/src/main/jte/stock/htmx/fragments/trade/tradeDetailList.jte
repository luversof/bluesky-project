@import net.luversof.web.gate.stock.util.StockFormatUtil
@import java.util.List
@import java.util.Map
@import java.util.UUID
@import java.math.BigDecimal
@import java.time.format.DateTimeFormatter
@import java.text.DecimalFormat
@import net.luversof.web.gate.stock.dto.response.TradeResponse
@import net.luversof.web.gate.stock.constant.TradeType
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.stock.util.StockDetailLinkUtil

@param List<TradeResponse> tradeList
<%-- 날짜 칸에 쓸 존. 이 화면의 기간 표시·필터와 같은 존이어야 한다(부모가 요청 타임존으로 푼 값). --%>
@param java.time.ZoneId zone
@param int totalItems
@param Map<UUID, String> accountNames
@param String sort
@param BigDecimal totalFee
@param BigDecimal totalTax
@param BigDecimal totalRealizedProfit
@param DecimalFormat decimalFormat
@param java.util.function.Function<Object, String> countMessage
@param String accountLabel
@param String stockLabel
@param String realizedProfitLabel
@param String feeLabel
@param String tradingTaxLabel
@param String buyLabel
<%-- 빈 기간일 때 '기간 넓히기' 를 띄울지. 부모 화면이 '전체' 인지 알고 있다. --%>
@param boolean widenRangeAction = false
@param String sellLabel

!{
	String sortField = sort != null ? sort.split(",")[0] : "";
	String sortDir = sort != null && sort.contains(",") ? sort.split(",")[1] : "";
	DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	String detailListLabel = MessageUtil.getMessage("stock.trade.label.detail.list");
	// 0 원과 "해당 없음" 을 숫자 0 으로 적으면 잰 값처럼 읽힌다. 같은 화면의 기간 쪼갬 표와 연도별 세금 · 비용 표는
	// 이미 "-" 와 까닭을 단다 - 실측 2026-09-13: 이 표만 매매 258 건 중 거래세 228 행 · 수수료 51 행이 "0" 이었고,
	// 매수 203 행의 실현 손익 칸은 아예 빈 칸이라 낭독기에 아무것도 남지 않았다.
	String zeroAmountTitle = MessageUtil.getMessage("common.amount.zero.title");
	String buyNoRealizedTitle = MessageUtil.getMessage("stock.trade.realized.buy.none");
	String noDataTitle = MessageUtil.getMessage("common.message.no.data");
	String dateLabel = MessageUtil.getMessage("common.label.date");
	String typeLabel = MessageUtil.getMessage("common.label.type");
	String unitPriceLabel = MessageUtil.getMessage("stock.trade.column.unit.price");
	String quantityLabel = MessageUtil.getMessage("common.label.quantity");
	// 머리글은 짧게(영어 "Qty") - 영어 1280px 에서 표가 상자보다 16px 넓어 가로 스크롤이 생겼다(2026-10-02). 낭독기 · 툴팁에는 원래 이름.
	String quantityShortLabel = MessageUtil.getMessage("stock.trade.col.quantity.short");
	String amountLabel = MessageUtil.getMessage("common.label.amount");
	String noTradeDataLabel = MessageUtil.getMessage("stock.trade.message.no.data");
	String totalLabelText = MessageUtil.getMessage("common.label.total");
	java.util.function.Function<TradeType, String> tradeTypeLabel = type -> type == TradeType.BUY ? buyLabel : sellLabel;
}

	<!-- Collapsible Detail List -->
	<details id="tradeDetailSection" class="mb-4" data-persist-open="tradeDetail" data-page-section="${detailListLabel}">
		<summary class="cursor-pointer select-none flex items-center gap-2 font-semibold text-sm p-3 bg-base-200 rounded-box hover:bg-base-300 transition-colors">
			<svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 transition-transform detail-arrow" viewBox="0 0 20 20" fill="currentColor">
				<path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
			</svg>
			${detailListLabel} (${countMessage.apply(totalItems)})
		</summary>

		<div id="tradeListContainer" class="flex flex-col relative mt-2">
			<div class="flex-1 min-h-0 overflow-x-auto table-head-sticky-xl">
				<table class="table w-full table-sm table-pin-rows pin-top-nav" aria-label="${detailListLabel}">
					<thead>
					<tr class="bg-base-200">
						<th scope="col" class="cursor-pointer hover:bg-base-300" tabindex="0" aria-sort="${sortField.equals("tradeDate") ? (sortDir.equals("asc") ? "ascending" : "descending") : "none"}"
							hx-get="/stock/htmx/trade/list" hx-target="#tradeListFragment" hx-select="#tradeListFragment" hx-swap="outerHTML" hx-include="#tradeSearchForm"
							hx-vals='{"sort": "tradeDate,${sortField.equals("tradeDate") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
							${dateLabel} @if(sortField.equals("tradeDate"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
						</th>
						<th scope="col">${typeLabel}</th>
						<th scope="col" class="cursor-pointer hover:bg-base-300" tabindex="0" aria-sort="${sortField.equals("stockItemName") ? (sortDir.equals("asc") ? "ascending" : "descending") : "none"}"
							hx-get="/stock/htmx/trade/list" hx-target="#tradeListFragment" hx-select="#tradeListFragment" hx-swap="outerHTML" hx-include="#tradeSearchForm"
							hx-vals='{"sort": "stockItemName,${sortField.equals("stockItemName") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
							${stockLabel} @if(sortField.equals("stockItemName"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
						</th>
						<%-- 수수료·거래세 열은 lg(1024px)에서 폈는데, 그 폭에서 사이드바(224px)가 함께 나타나 본문 열이 991px -> 768px 로 줄어든다.
						     실측 2026-09-10: 1023px 은 8열 · 표 991px · 넘침 0 인데 1024px 은 10열 · 표 1,033px · 컨테이너 768px 로 넘침 265px 이 됐다
						     (1100px 189 · 1200px 89 · 1280px 9 · 1300px 0). 넓혔는데 더 나빠지는 구간이라 실제로 들어가는 폭에서 편다. --%>
						<th scope="col" class="hidden sm:table-cell">${accountLabel}</th>
						<th scope="col" class="text-right">${unitPriceLabel}</th>
						<th scope="col" class="text-right" aria-label="${quantityLabel}"><abbr title="${quantityLabel}" class="no-underline">${quantityShortLabel}</abbr></th>
						<th scope="col" class="text-right cursor-pointer hover:bg-base-300" tabindex="0" aria-sort="${sortField.equals("amount") ? (sortDir.equals("asc") ? "ascending" : "descending") : "none"}"
							hx-get="/stock/htmx/trade/list" hx-target="#tradeListFragment" hx-select="#tradeListFragment" hx-swap="outerHTML" hx-include="#tradeSearchForm"
							hx-vals='{"sort": "amount,${sortField.equals("amount") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
							${amountLabel} @if(sortField.equals("amount"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
						</th>
						<th scope="col" class="text-right cursor-pointer hover:bg-base-300 hidden min-[1300px]:table-cell print:table-cell" tabindex="0" aria-sort="${sortField.equals("fee") ? (sortDir.equals("asc") ? "ascending" : "descending") : "none"}"
							hx-get="/stock/htmx/trade/list" hx-target="#tradeListFragment" hx-select="#tradeListFragment" hx-swap="outerHTML" hx-include="#tradeSearchForm"
							hx-vals='{"sort": "fee,${sortField.equals("fee") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
							${feeLabel} @if(sortField.equals("fee"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
						</th>
						<th scope="col" class="text-right hidden min-[1300px]:table-cell print:table-cell">${tradingTaxLabel}</th>
						<th scope="col" class="text-right cursor-pointer hover:bg-base-300" tabindex="0" aria-sort="${sortField.equals("realizedProfit") ? (sortDir.equals("asc") ? "ascending" : "descending") : "none"}"
							hx-get="/stock/htmx/trade/list" hx-target="#tradeListFragment" hx-select="#tradeListFragment" hx-swap="outerHTML" hx-include="#tradeSearchForm"
							hx-vals='{"sort": "realizedProfit,${sortField.equals("realizedProfit") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
							${realizedProfitLabel} @if(sortField.equals("realizedProfit"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
						</th>
					</tr>
					</thead>
					<tbody>
					@for(TradeResponse trade : tradeList)
					<tr>
						<td class="whitespace-nowrap">${trade.tradeDate() != null ? dateFormatter.format(trade.tradeDate().atZone(zone)) : ""}</td>
						<td>
							<span class="${trade.type() == TradeType.BUY ? "badge badge-error badge-outline badge-sm" : "badge badge-info badge-outline badge-sm"}">
								${tradeTypeLabel.apply(trade.type())}
							</span>
						</td>
						<td class="font-bold whitespace-nowrap"><a href="${StockDetailLinkUtil.item(trade.stockItemId() != null ? trade.stockItemId().toString() : "")}" class="link link-hover">${trade.stockItemName()}</a></td>
						<td class="whitespace-nowrap hidden sm:table-cell"><a href="${StockDetailLinkUtil.account(trade.accountId() != null ? trade.accountId().toString() : "")}" class="link link-hover">${accountNames != null ? accountNames.getOrDefault(trade.accountId(), "") : ""}</a></td>
						<td class="text-right font-mono amount-value">${trade.price() != null ? decimalFormat.format(trade.price()) : "0"}</td>
						<td class="text-right font-mono amount-value">${decimalFormat.format(trade.quantity())}</td>
						<td class="text-right font-mono amount-value">${trade.amount() != null ? decimalFormat.format(trade.amount()) : "0"}</td>
						<td class="text-right text-xs text-base-content/60 amount-value hidden min-[1300px]:table-cell print:table-cell">@if(trade.fee() == null || trade.fee().signum() == 0)<span class="text-base-content/60" aria-hidden="true" title="${zeroAmountTitle}">-</span><span class="sr-only">${zeroAmountTitle}</span>@else${decimalFormat.format(trade.fee())}@endif</td>
						<td class="text-right text-xs text-base-content/60 amount-value hidden min-[1300px]:table-cell print:table-cell">@if(trade.tax() == null || trade.tax().signum() == 0)<span class="text-base-content/60" aria-hidden="true" title="${zeroAmountTitle}">-</span><span class="sr-only">${zeroAmountTitle}</span>@else${decimalFormat.format(trade.tax())}@endif</td>
						<td class="text-right font-bold font-mono amount-value ${trade.realizedProfit() != null ? (trade.realizedProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss") : ""}">
							<%-- 손익은 부호가 뜻을 바꾼다 - 실측 2026-09-12: 이 열의 55 값 중 음수 6 개만 "-" 를 달고 양수 49 개는
								 맨 숫자였다. 색만으로 방향을 말하면 고대비 모드(실측: 손익 색이 모두 검정 한 가지로 합쳐짐)와
								 색을 구분하기 어려운 눈에는 뜻이 사라진다. 다른 화면은 StockFormatUtil.signedWon 으로 이미 부호를 붙인다. --%>
							@if(trade.realizedProfit() != null && trade.type() == TradeType.SELL)${StockFormatUtil.signedWon(StockFormatUtil.displayWon(trade.realizedProfit()))}@else<span class="text-base-content/60" aria-hidden="true" title="${trade.type() == TradeType.BUY ? buyNoRealizedTitle : noDataTitle}">-</span><span class="sr-only">${trade.type() == TradeType.BUY ? buyNoRealizedTitle : noDataTitle}</span>@endif
						</td>
					</tr>
					@endfor
					@if(tradeList == null || tradeList.isEmpty())
					<tr>
						<td colspan="10">@template._components.ui.emptyState(message = noTradeDataLabel, widenRangeAction = widenRangeAction)</td>
					</tr>
					@endif
					</tbody>
					<tfoot>
						<tr class="font-bold bg-base-200">
							<%-- 합계 라벨의 colspan 은 고정인데 그 범위 안의 계좌 열은 sm 미만에서 숨는다. 그대로 두면 합계 줄이 헤더보다
						     한 칸 넓어져 값이 다른 열 밑에 붙는다 - 실측 2026-09-11 375px: 헤더 보임 7 칸인데 합계 줄은 8 칸.
						     숨는 열은 colspan 밖으로 빼고 헤더와 같은 클래스를 단 빈 칸으로 둔다. --%>
							<th scope="row" colspan="3" class="text-right pr-4">${totalLabelText}</th>
							<td class="hidden sm:table-cell"></td>
							<td colspan="2"></td>
							<td class="text-right font-mono amount-value"></td>
							<td class="text-right font-mono amount-value hidden min-[1300px]:table-cell print:table-cell">${totalFee != null ? decimalFormat.format(totalFee) : "0"}</td>
							<td class="text-right font-mono amount-value hidden min-[1300px]:table-cell print:table-cell">${totalTax != null ? decimalFormat.format(totalTax) : "0"}</td>
							<td class="text-right font-mono amount-value ${totalRealizedProfit != null && totalRealizedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
								${totalRealizedProfit != null ? StockFormatUtil.signedWon(StockFormatUtil.displayWon(totalRealizedProfit)) : "0"}
							</td>
						</tr>
					</tfoot>
				</table>
			</div>

		</div>
	</details>
