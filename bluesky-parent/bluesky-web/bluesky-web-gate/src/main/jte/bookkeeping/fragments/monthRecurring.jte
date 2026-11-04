@import net.luversof.web.gate.bookkeeping.constant.EntryType
@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@import net.luversof.web.gate.bookkeeping.support.RecurringView
@import net.luversof.web.gate.bookkeeping.controller.BookkeepingViewController.MonthPage
@param MonthPage page

<%-- 이 달의 아직 안 넣은 반복 회차. 자동으로 넣지 않는다 - "넣기"를 눌러야 내역이 된다. 없으면 그리지 않는다. --%>
!{ var due = page.recurringDue(); }
@if(due == null)
	<p role="status" class="mb-4 text-sm text-base-content/60">${BookkeepingView.msg("bookkeeping.recurring.unavailable")}</p>
@elseif(!due.isEmpty())
	<section aria-labelledby="bk-recurring-title" class="mb-4 rounded-box border border-base-200 bg-base-100 p-4">
		<h2 id="bk-recurring-title" class="text-sm font-semibold mb-2">${BookkeepingView.msg("bookkeeping.recurring.due", due.size())}</h2>
		<ul class="divide-y divide-base-200">
			@for(var d : due)
				!{ var r = d.recurring(); }
				!{ String currency = BookkeepingView.assetCurrency(page.assetsById(), r.assetId()); }
				!{ String sign = r.amount().signum() == 0 ? "" : r.type() == EntryType.INCOME ? "+" : r.type() == EntryType.EXPENSE ? "-" : ""; }
				<li class="flex flex-wrap items-center gap-3 py-2">
					<span class="w-24 shrink-0 text-xs text-base-content/70">${BookkeepingView.dayLabel(d.date())}</span>
					<span class="min-w-0 flex-1 truncate text-sm">${RecurringView.title(r, page.assetsById(), page.categoriesById())}</span>
					<span class="shrink-0 text-sm font-semibold amount-value">${sign}${BookkeepingView.amount(r.amount(), currency)}</span>
					<form method="post" action="/bookkeeping/${page.book().id().toString()}/recurrings/${r.id().toString()}/confirm">
						<input type="hidden" name="date" value="${d.date().toString()}" />
						<input type="hidden" name="month" value="${page.range().month().toString()}" />
						<button type="submit" class="btn btn-xs btn-primary">${BookkeepingView.msg("bookkeeping.recurring.confirm")}</button>
					</form>
				</li>
			@endfor
		</ul>
	</section>
@endif
