@import io.github.luversof.boot.context.support.MessageUtil
@import java.util.List
@import net.luversof.web.gate.stock.dto.view.RecordDateBuyByView
@import net.luversof.web.gate.stock.support.StockViewSupport

<%-- 다음 분배금 기준일 · 매수 마감일(2026-10-20 자율 개선) - 적립식으로 사는 날을 정할 때 본다.
     기준일의 2 거래일 전(T+2 결제)까지 사야 그 분배금을 받는다. 이번 달 마감이 지났으면 다음 달 것을 보인다. 계산은 KrxRecordDateUtil.
     필터와 무관해 결과 구역 밖에 둔다(구역 교체 때 같이 바뀔 이유가 없다). --%>
@param List<RecordDateBuyByView> rows = java.util.List.of()
<%-- 대시보드 다가올 배당 카드의 한 줄(2026-10-20) - 제목 · 안내 없이 시기별 마감만. --%>
@param boolean compact = false

@if(rows != null && !rows.isEmpty())
	!{
		java.util.Locale locale = org.springframework.context.i18n.LocaleContextHolder.getLocale();
		java.time.format.DateTimeFormatter dateFormat = java.time.format.DateTimeFormatter.ofPattern(MessageUtil.getMessage("stock.monthly.etf.buyby.date.pattern"), locale);
		boolean anyUncovered = rows.stream().anyMatch(row -> !row.holidaysCovered());
	}
	@if(compact)
	<p class="mt-2 flex flex-wrap items-baseline gap-x-3 gap-y-1 text-[11px] text-base-content/60" data-record-buyby data-record-buyby-compact>
		<span class="font-medium text-base-content/70">${MessageUtil.getMessage("stock.monthly.etf.buyby.compact.title")}</span>
		@for(RecordDateBuyByView row : rows)
			<span class="min-w-0 wrap-anywhere" data-record-buyby-window="${row.payoutWindow()}">${"MID_MONTH".equals(row.payoutWindow()) ? MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.mid.month") : MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.month.end")} <span class="font-mono text-base-content/80" data-buyby-date>${row.buyByDate().format(dateFormat)}</span> <span data-buyby-days-left="${row.daysLeft()}">(${row.daysLeft() == 0 ? MessageUtil.getMessage("stock.monthly.etf.buyby.today") : StockViewSupport.msg("stock.monthly.etf.buyby.days.left", String.valueOf(row.daysLeft()))})</span></span>
		@endfor
	</p>
	@else
	<section class="rounded-box border border-base-300 bg-base-100 px-3 py-2 text-sm" data-record-buyby aria-labelledby="recordBuyByTitle">
		<div class="flex flex-wrap items-baseline gap-x-4 gap-y-1">
			<h2 id="recordBuyByTitle" class="font-semibold">${MessageUtil.getMessage("stock.monthly.etf.buyby.title")}</h2>
			@for(RecordDateBuyByView row : rows)
				<span class="min-w-0 wrap-anywhere" data-record-buyby-window="${row.payoutWindow()}">
					<span class="badge badge-sm badge-ghost">${"MID_MONTH".equals(row.payoutWindow()) ? MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.mid.month") : MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.month.end")}</span>
					<span class="text-base-content/60">${MessageUtil.getMessage("stock.monthly.etf.buyby.record")}</span>
					<span class="font-mono" data-record-date>${row.recordDate().format(dateFormat)}</span>
					<span class="text-base-content/60">·</span>
					<span class="font-mono font-semibold" data-buyby-date>${StockViewSupport.msg("stock.monthly.etf.buyby.until", row.buyByDate().format(dateFormat))}</span>
					<span class="${row.daysLeft() == 0 ? "inline-block rounded border border-warning/30 bg-warning/10 px-1.5 py-0.5 text-base-content text-xs" : "text-xs text-base-content/60"}" data-buyby-days-left="${row.daysLeft()}">${row.daysLeft() == 0 ? MessageUtil.getMessage("stock.monthly.etf.buyby.today") : StockViewSupport.msg("stock.monthly.etf.buyby.days.left", String.valueOf(row.daysLeft()))}</span>
				</span>
			@endfor
		</div>
		<p class="mt-1 text-xs text-base-content/60" data-record-buyby-note>${MessageUtil.getMessage("stock.monthly.etf.buyby.note")}@if(anyUncovered) ${MessageUtil.getMessage("stock.monthly.etf.buyby.uncovered")}@endif</p>
	</section>
	@endif
@endif
