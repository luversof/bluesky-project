@import net.luversof.web.gate.bookkeeping.constant.EntryType
@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@import net.luversof.web.gate.bookkeeping.support.RecurringView
@import net.luversof.web.gate.bookkeeping.controller.BookkeepingSettingsViewController.SettingsPage
@param SettingsPage page

<%-- 반복 내역: 만들기는 입력 모달의 "반복으로 저장", 여기서는 보기 · 지우기만(이미 넣은 내역은 남는다). --%>
!{ String base = "/bookkeeping/" + page.book().id() + "/settings"; }
!{ var assets = net.luversof.web.gate.bookkeeping.support.BookkeepingView.assetsById(page.assets()); }
!{ var categories = net.luversof.web.gate.bookkeeping.support.BookkeepingView.categoriesById(page.categories()); }
<section aria-labelledby="bk-set-recurrings" class="mb-6">
	<h2 id="bk-set-recurrings" class="text-base font-semibold mb-2">${BookkeepingView.msg("bookkeeping.recurring.title")}</h2>
	@if(page.recurrings() == null)
		<p role="status" class="text-sm text-base-content/60">${BookkeepingView.msg("bookkeeping.recurring.unavailable")}</p>
	@elseif(page.recurrings().isEmpty())
		<p class="text-sm text-base-content/60">${BookkeepingView.msg("bookkeeping.recurring.empty")}</p>
	@else
		<ul class="divide-y divide-base-200 rounded-box border border-base-200 bg-base-100">
			@for(var r : page.recurrings())
				!{ String currency = BookkeepingView.assetCurrency(assets, r.assetId()); }
				<li class="flex flex-wrap items-center gap-3 px-3 py-2">
					<span class="badge badge-sm">${BookkeepingView.msg("bookkeeping.type." + r.type().name())}</span>
					<span class="min-w-0 flex-1 text-sm">
						<span class="block truncate">${RecurringView.title(r, assets, categories)}</span>
						<span class="block text-xs text-base-content/60">${RecurringView.cycleLabel(r)} · ${BookkeepingView.msg("bookkeeping.recurring.period", r.startDate().toString(), r.endDate() == null ? "" : r.endDate().toString())}</span>
					</span>
					<span class="shrink-0 text-sm amount-value">${BookkeepingView.amount(r.amount(), currency)}</span>
					<form method="post" action="${base}/recurrings/${r.id().toString()}/delete">
						<button type="submit" class="btn btn-xs btn-ghost">${BookkeepingView.msg("bookkeeping.recurring.delete")}</button>
					</form>
				</li>
			@endfor
		</ul>
	@endif
</section>
