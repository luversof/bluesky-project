@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.net.URLEncoder
@import java.nio.charset.StandardCharsets
@import java.text.DecimalFormat
@import java.util.List
@import net.luversof.web.gate.stock.util.StockFormatUtil
@import net.luversof.web.gate.stock.dto.response.MonthlyDividendPayoutResponse

@param String peekSymbol = ""
@param List<MonthlyDividendPayoutResponse> peekPayouts = java.util.List.of()
@param int peekTotalCount = 0
@param boolean peekFailed = false

<%-- 관리 > 월배당 기준: 프로필 행 안에서 펼치는 지급 이력(사용자 요청 2026-09-29, A안). 최근 몇 건만 싣고 전체 · 편집은 종목을 골라 아래 목록에서.
     금액 · 분배율 서식은 아래 '저장된 지급 이력' 표와 같다. --%>
!{
	// 주당 금액은 원 단위보다 작을 수 있다(32.25 원) - 있는 자리만(최대 넷째) 적고 통화 단위는 로케일대로(StockFormatUtil.fullKrw).
	java.util.function.Function<BigDecimal, String> perShare = value -> {
		BigDecimal safe = value != null ? value : BigDecimal.ZERO;
		return StockFormatUtil.fullKrw(safe, Math.max(0, Math.min(4, safe.stripTrailingZeros().scale())));
	};
	DecimalFormat percentFormat = new DecimalFormat("#,##0.00");
	List<MonthlyDividendPayoutResponse> rows = peekPayouts != null ? peekPayouts : List.<MonthlyDividendPayoutResponse>of();
	// 분배율은 출처에서 오지 않아 대부분 비어 있다(아래 목록과 같은 규칙) - 값이 있을 때만 열을 낸다.
	boolean showRate = rows.stream().anyMatch(row -> row.distributionRatePct() != null);
}
<div class="rounded-box bg-base-200/60 px-3 py-2" data-payout-peek="${peekSymbol}">
	@if(peekFailed)
		<p class="text-sm text-error" role="alert" data-payout-peek-failed>${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.peek.failed")}</p>
	@elseif(rows.isEmpty())
		<p class="text-sm text-base-content/70" data-payout-peek-empty>${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.empty")}</p>
	@else
		<div class="overflow-x-auto">
			<table class="table table-xs" aria-label="${java.text.MessageFormat.format(MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.peek.table"), peekSymbol)}">
				<thead>
					<tr>
						<th scope="col" class="whitespace-nowrap">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.record.date")}</th>
						<th scope="col" class="whitespace-nowrap">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.pay.date")}</th>
						@if(showRate)
							<th scope="col" class="whitespace-nowrap text-right">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.distribution.rate")}</th>
						@endif
						<th scope="col" class="whitespace-nowrap text-right">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.dividend.amount")}</th>
						<th scope="col" class="whitespace-nowrap text-right">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.taxable.base")}</th>
					</tr>
				</thead>
				<tbody>
					@for(MonthlyDividendPayoutResponse row : rows)
						<tr data-payout-peek-item>
							<td class="whitespace-nowrap tabular-nums">${String.valueOf(row.recordDate())}</td>
							<td class="whitespace-nowrap tabular-nums">${String.valueOf(row.payDate())}</td>
							@if(showRate)
								<td class="text-right tabular-nums">@if(row.distributionRatePct() != null)${percentFormat.format(row.distributionRatePct())}%@else-@endif</td>
							@endif
							<td class="text-right tabular-nums">${perShare.apply(row.dividendAmountPerShare())}</td>
							<td class="text-right tabular-nums">@if(row.taxableBasePerShare() == null)<span class="text-base-content/60" data-taxable-unknown>${MessageUtil.getMessage("stock.monthly.taxable.unknown")}</span>@else${perShare.apply(row.taxableBasePerShare())}@endif</td>
						</tr>
					@endfor
				</tbody>
			</table>
		</div>
		<p class="mt-1 text-xs text-base-content/70" data-payout-peek-more>
			${java.text.MessageFormat.format(MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.peek.more"), String.valueOf(peekTotalCount), String.valueOf(rows.size()))}
			<a href="/stock/admin?tab=monthly-reference&symbol=${URLEncoder.encode(peekSymbol != null ? peekSymbol : "", StandardCharsets.UTF_8)}#monthly-payout-list" class="link link-primary ml-1">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.peek.all")}</a>
		</p>
	@endif
</div>
