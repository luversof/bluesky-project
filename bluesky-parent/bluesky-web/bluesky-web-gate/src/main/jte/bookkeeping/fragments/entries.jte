@import net.luversof.web.gate.bookkeeping.constant.CategoryKind
@import net.luversof.web.gate.bookkeeping.constant.EntryType
@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@import net.luversof.web.gate.bookkeeping.controller.BookkeepingViewController.MonthPage
@param MonthPage page

<%-- 검색(내용 · 메모 · 분류 · 자산, 기간은 이 달 또는 고른 날)과 날짜별 내역. 줄을 누르면 수정 모달이 열린다. --%>
!{ var book = page.book(); }
!{ var month = page.range().month(); }
!{ String title = BookkeepingView.msg("bookkeeping.entries.title"); }
<section aria-labelledby="bk-entries-title">
	<h2 id="bk-entries-title" class="text-base font-semibold mb-2">${title}</h2>
	<form method="get" action="/bookkeeping/${book.id().toString()}" class="flex flex-wrap items-end gap-2 mb-3" role="search">
		<input type="hidden" name="month" value="${month.toString()}" />
		@if(page.day() != null)
			<input type="hidden" name="day" value="${page.day().toString()}" />
		@endif
		<label class="form-control">
			<span class="label-text text-xs mb-1">${BookkeepingView.msg("bookkeeping.search.q")}</span>
			<input type="search" name="q" value="${page.q()}" maxlength="100" class="input input-sm input-bordered w-40" />
		</label>
		<label class="form-control">
			<span class="label-text text-xs mb-1">${BookkeepingView.msg("bookkeeping.field.category")}</span>
			<select name="categoryId" class="select select-sm select-bordered">
				<option value="">${BookkeepingView.msg("bookkeeping.search.all")}</option>
				@for(var kind : CategoryKind.values())
					<optgroup label="${BookkeepingView.msg("bookkeeping.kind." + kind.name())}">
						@for(var o : BookkeepingView.categoryOptions(page.categories(), kind, page.categoryId()))
							<option value="${o.id().toString()}" selected="${o.id().equals(page.categoryId())}">${o.child() ? "  · " + o.label() : o.label()}</option>
						@endfor
					</optgroup>
				@endfor
			</select>
		</label>
		<label class="form-control">
			<span class="label-text text-xs mb-1">${BookkeepingView.msg("bookkeeping.field.asset")}</span>
			<select name="assetId" class="select select-sm select-bordered">
				<option value="">${BookkeepingView.msg("bookkeeping.search.all")}</option>
				@for(var o : BookkeepingView.assetOptions(page.assets(), page.assetId()))
					<option value="${o.id().toString()}" selected="${o.id().equals(page.assetId())}">${o.label()}</option>
				@endfor
			</select>
		</label>
		<button type="submit" class="btn btn-sm">${BookkeepingView.msg("bookkeeping.search.submit")}</button>
		@if(page.day() != null || !page.q().isEmpty() || page.categoryId() != null || page.assetId() != null)
			<a href="${BookkeepingView.href(book.id(), month)}" class="btn btn-sm btn-ghost">${BookkeepingView.msg("bookkeeping.search.clear")}</a>
		@endif
	</form>
	@if(page.day() != null)
		<p class="text-sm mb-2">${BookkeepingView.msg("bookkeeping.entries.day", BookkeepingView.dayLabel(page.day()))}</p>
	@endif

	@if(page.entries().isEmpty())
		@template._components.ui.emptyState(message = BookkeepingView.msg("bookkeeping.entries.empty"), boxed = true)
	@else
		!{ java.time.LocalDate current = null; }
		<ul class="divide-y divide-base-200 rounded-box border border-base-200 bg-base-100">
			@for(var e : page.entries())
				@if(!e.entryDate().equals(current))
					!{ current = e.entryDate(); }
					<li class="bg-base-200/50 px-3 py-1 text-xs font-semibold text-base-content/70">${BookkeepingView.dayLabel(e.entryDate())}</li>
				@endif
				!{ String currency = BookkeepingView.assetCurrency(page.assetsById(), e.assetId()); }
				!{ String sign = e.type() == EntryType.INCOME ? "+" : e.type() == EntryType.EXPENSE ? "-" : ""; }
				<li>
					<a href="${BookkeepingView.href(book.id(), month, "day", BookkeepingView.str(page.day()), "q", page.q(), "categoryId", BookkeepingView.str(page.categoryId()), "assetId", BookkeepingView.str(page.assetId()), "edit", e.id().toString())}" class="flex items-center gap-3 px-3 py-2 hover:bg-base-200">
						<span class="badge badge-sm shrink-0 ${e.type() == EntryType.INCOME ? "badge-info" : e.type() == EntryType.EXPENSE ? "badge-error" : "badge-ghost"}">${BookkeepingView.msg("bookkeeping.type." + e.type().name())}</span>
						<span class="min-w-0 flex-1">
							<span class="block truncate text-sm" title="${BookkeepingView.entryTitle(e, page.assetsById(), page.categoriesById())}">${BookkeepingView.entryTitle(e, page.assetsById(), page.categoriesById())}</span>
							@if(e.content() != null || e.type() != EntryType.TRANSFER)
								!{ String sub = (e.content() == null ? "" : e.content() + " · ") + BookkeepingView.assetName(page.assetsById(), e.assetId()); }
								<span class="block truncate text-xs text-base-content/60" title="${sub}">${sub}</span>
							@endif
						</span>
						<span class="shrink-0 text-right text-sm font-semibold amount-value">
							${sign}${BookkeepingView.amount(e.amount(), currency)}
							@if(e.type() == EntryType.TRANSFER && !currency.equals(BookkeepingView.assetCurrency(page.assetsById(), e.toAssetId())))
								<span class="block text-xs font-normal text-base-content/60">→ ${BookkeepingView.amount(e.toAmount(), BookkeepingView.assetCurrency(page.assetsById(), e.toAssetId()))} ${BookkeepingView.assetCurrency(page.assetsById(), e.toAssetId())}</span>
							@endif
						</span>
					</a>
				</li>
			@endfor
		</ul>
	@endif
</section>
