@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@import net.luversof.web.gate.bookkeeping.controller.BookkeepingViewController.MonthPage
@param MonthPage page
@param String error = null
@param boolean isAuthenticated = false
@param String username = ""
@param String profile = "local"

!{ var book = page.book(); }
!{ var month = page.range().month(); }
!{ String monthLabel = BookkeepingView.monthLabel(month); }
@template._layout.bookkeepingLayout(
	title = book.name() + " " + monthLabel,
	bookId = book.id(),
	active = "month",
	isAuthenticated = isAuthenticated,
	username = username,
	profile = profile,
	content = @`
		@template._components.ui.pageHeader(
			title = book.name(),
			description = monthLabel + " (" + BookkeepingView.rangeLabel(page.range()) + ")",
			actions = @`
				<div class="flex flex-wrap items-center gap-2">
					<a href="${BookkeepingView.href(book.id(), month.minusMonths(1), "q", page.q(), "categoryId", BookkeepingView.str(page.categoryId()), "assetId", BookkeepingView.str(page.assetId()))}" class="btn btn-sm btn-outline">${BookkeepingView.msg("bookkeeping.month.prev")}</a>
					<a href="${BookkeepingView.href(book.id(), month.plusMonths(1), "q", page.q(), "categoryId", BookkeepingView.str(page.categoryId()), "assetId", BookkeepingView.str(page.assetId()))}" class="btn btn-sm btn-outline">${BookkeepingView.msg("bookkeeping.month.next")}</a>
					<a href="${BookkeepingView.href(book.id(), month, "new", "1", "date", BookkeepingView.str(page.day()))}" class="btn btn-sm btn-primary">${BookkeepingView.msg("bookkeeping.entry.add")}</a>
				</div>
			`
		)
		@if(error != null)
			<div role="alert" class="alert alert-error mb-4"><span>${error}</span></div>
		@endif

		<section aria-labelledby="bk-summary-title" class="mb-4">
			<h2 id="bk-summary-title" class="sr-only">${BookkeepingView.msg("bookkeeping.summary.title")}</h2>
			@if(page.totals().isEmpty())
				@template._components.ui.emptyState(message = BookkeepingView.msg("bookkeeping.summary.empty"), boxed = true, padding = "py-6")
			@else
				<div class="grid gap-3 sm:grid-cols-2">
					@for(var t : page.totals())
						<div class="card bg-base-100 border border-base-200 rounded-box shadow-sm">
							<div class="card-body p-4">
								<p class="text-xs text-base-content/60">${t.currency()}</p>
								<dl class="grid grid-cols-3 gap-2 text-sm">
									<div><dt class="text-base-content/60">${BookkeepingView.msg("bookkeeping.summary.income")}</dt><dd class="font-semibold amount-value">${BookkeepingView.amount(t.income(), t.currency())}</dd></div>
									<div><dt class="text-base-content/60">${BookkeepingView.msg("bookkeeping.summary.expense")}</dt><dd class="font-semibold amount-value">${BookkeepingView.amount(t.expense(), t.currency())}</dd></div>
									<div><dt class="text-base-content/60">${BookkeepingView.msg("bookkeeping.summary.net")}</dt><dd class="font-semibold amount-value">${BookkeepingView.amount(t.net(), t.currency())}</dd></div>
								</dl>
							</div>
						</div>
					@endfor
				</div>
			@endif
		</section>

		@template.bookkeeping.fragments.monthFx(page = page)
		@template.bookkeeping.fragments.monthBudget(page = page)
		@template.bookkeeping.fragments.monthRecurring(page = page)
		@template.bookkeeping.fragments.calendar(page = page)
		@template.bookkeeping.fragments.entries(page = page)
		<details class="mt-4">
			<summary class="cursor-pointer text-sm text-base-content/70">${BookkeepingView.msg("bookkeeping.export.title")}</summary>
			<form method="get" action="/bookkeeping/${book.id().toString()}/export.csv" class="mt-2 flex flex-wrap items-end gap-3">
				<label class="form-control">
					<span class="label-text text-xs">${BookkeepingView.msg("bookkeeping.export.from")}</span>
					<input type="date" name="from" value="${page.range().from().toString()}" required class="input input-sm input-bordered" />
				</label>
				<label class="form-control">
					<span class="label-text text-xs">${BookkeepingView.msg("bookkeeping.export.to")}</span>
					<input type="date" name="to" value="${page.range().to().toString()}" required class="input input-sm input-bordered" />
				</label>
				@if(page.q() != null)<input type="hidden" name="q" value="${page.q()}" />@endif
				@if(page.categoryId() != null)<input type="hidden" name="categoryId" value="${page.categoryId().toString()}" />@endif
				@if(page.assetId() != null)<input type="hidden" name="assetId" value="${page.assetId().toString()}" />@endif
				<button type="submit" class="btn btn-sm">${BookkeepingView.msg("bookkeeping.export.submit")}</button>
				@if(page.q() != null || page.categoryId() != null || page.assetId() != null)
					<span class="text-xs text-base-content/60">${BookkeepingView.msg("bookkeeping.export.filtered")}</span>
				@endif
			</form>
			<a href="/bookkeeping/${book.id().toString()}/import" class="link text-xs mt-2 inline-block">${BookkeepingView.msg("bookkeeping.import.link")}</a>
		</details>
		@if(page.form().open())
			@template.bookkeeping.fragments.entryModal(page = page)
		@endif
	`
)
