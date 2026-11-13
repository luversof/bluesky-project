@import net.luversof.web.gate.bookkeeping.constant.AssetKind
@import net.luversof.web.gate.bookkeeping.support.AssetGroups
@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@import net.luversof.web.gate.bookkeeping.support.CardBillView
@import net.luversof.web.gate.bookkeeping.httpexchange.BookClient.Book
@param Book book
@param AssetGroups.Page page
@param CardBillView cardBills = null
@param boolean isAuthenticated = false
@param String username = ""
@param String profile = "local"

<%-- 자산 화면: 종류별 자산과 잔액. 금액은 색 없이 부호로(음수 = 카드 · 대출 잔액). 자산을 누르면 월 화면에서 그 자산의 내역만 본다. --%>
!{ String title = BookkeepingView.msg("bookkeeping.title.assets"); }
@template._layout.bookkeepingLayout(
	title = book.name() + " " + title,
	bookId = book.id(),
	active = "assets",
	isAuthenticated = isAuthenticated,
	username = username,
	profile = profile,
	content = @`
		@template._components.ui.pageHeader(title = book.name(), description = title)

		@if(page.groups().isEmpty())
			@template._components.ui.emptyState(message = BookkeepingView.msg("bookkeeping.assets.empty"), boxed = true)
		@else
			<section aria-labelledby="bk-assets-total-title" class="mb-4">
				<h2 id="bk-assets-total-title" class="text-sm text-base-content/60 mb-1">${BookkeepingView.msg("bookkeeping.assets.total")}</h2>
				<div class="flex flex-wrap gap-x-6 gap-y-1">
					@for(var t : page.totals())
						<p class="text-xl font-semibold amount-value">${BookkeepingView.amount(t.amount(), t.currency())} <span class="text-sm font-normal text-base-content/60">${t.currency()}</span></p>
					@endfor
				</div>
			</section>

			@for(var g : page.groups())
				!{ String groupTitle = BookkeepingView.msg("bookkeeping.assetKind." + g.kind().name()); }
				<section aria-labelledby="bk-kind-${g.kind().name()}" class="mb-4">
					<div class="flex flex-wrap items-baseline justify-between gap-2 mb-1">
						<h2 id="bk-kind-${g.kind().name()}" class="text-base font-semibold">${groupTitle}</h2>
						<p class="text-sm text-base-content/60">
							@for(var s : g.subtotals())
								<span class="amount-value ml-3">${BookkeepingView.amount(s.amount(), s.currency())} ${s.currency()}</span>
							@endfor
						</p>
					</div>
					<ul class="divide-y divide-base-200 rounded-box border border-base-200 bg-base-100">
						@for(var a : g.assets())
							<li>
								<a href="${BookkeepingView.href(book.id(), null, "assetId", a.asset().id().toString())}" class="flex items-center gap-3 px-3 py-2 hover:bg-base-200">
									<span class="min-w-0 flex-1">
										<span class="block truncate text-sm" title="${a.asset().name()}">${a.asset().name()}</span>
										@if(a.asset().kind() == AssetKind.CREDIT_CARD && a.asset().paymentDay() != null)
											<span class="block text-xs text-base-content/60">${BookkeepingView.msg("bookkeeping.assets.paymentDay", a.asset().paymentDay())}</span>
										@endif
										@if(a.asset().kind() == AssetKind.CREDIT_CARD && cardBills != null && cardBills.has(a.asset().id()))
											<span class="block text-xs text-base-content/60 amount-value">${BookkeepingView.msg("bookkeeping.assets.cardBill", BookkeepingView.amount(cardBills.thisMonth(a.asset().id()), a.asset().currency()), BookkeepingView.amount(cardBills.nextMonth(a.asset().id()), a.asset().currency()))}</span>
										@endif
									</span>
									<span class="shrink-0 text-right text-sm font-semibold amount-value">${BookkeepingView.amount(a.balance(), a.asset().currency())} <span class="text-xs font-normal text-base-content/60">${a.asset().currency()}</span></span>
								</a>
							</li>
						@endfor
					</ul>
				</section>
			@endfor
		@endif

		@if(cardBills != null && cardBills.failed())
			<p role="status" class="mt-2 text-sm text-base-content/60">${BookkeepingView.msg("bookkeeping.assets.cardBill.unavailable")}</p>
		@endif

		@if(!page.hidden().isEmpty())
			<details class="mt-6">
				<summary class="cursor-pointer text-sm text-base-content/60">${BookkeepingView.msg("bookkeeping.assets.hidden", page.hidden().size())}</summary>
				<ul class="mt-2 divide-y divide-base-200 rounded-box border border-base-200 bg-base-100">
					@for(var a : page.hidden())
						<li>
							<a href="${BookkeepingView.href(book.id(), null, "assetId", a.asset().id().toString())}" class="flex items-center gap-3 px-3 py-2 hover:bg-base-200 text-base-content/70">
								<span class="min-w-0 flex-1 truncate text-sm" title="${a.asset().name()}">${a.asset().name()}</span>
								<span class="shrink-0 text-sm amount-value">${BookkeepingView.amount(a.balance(), a.asset().currency())} ${a.asset().currency()}</span>
							</a>
						</li>
					@endfor
				</ul>
			</details>
		@endif
	`
)
