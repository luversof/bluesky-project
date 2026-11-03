@import java.util.List
@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@import net.luversof.web.gate.bookkeeping.httpexchange.BookClient.BookSummary
@param List<BookSummary> books
@param List<String> currencies
@param String error = null
@param boolean isAuthenticated = false
@param String username = ""
@param String profile = "local"

!{ String title = BookkeepingView.msg("bookkeeping.title.index"); }
@template._layout.bookkeepingLayout(
	title = title,
	isAuthenticated = isAuthenticated,
	username = username,
	profile = profile,
	content = @`
		@template._components.ui.pageHeader(title = title, description = BookkeepingView.msg("bookkeeping.index.description"))
		@if(error != null)
			<div role="alert" class="alert alert-error mb-4"><span>${error}</span></div>
		@endif

		@if(books.isEmpty())
			@template._components.ui.emptyState(message = BookkeepingView.msg("bookkeeping.index.empty"), hint = BookkeepingView.msg("bookkeeping.index.empty.hint"), boxed = true)
		@else
			<ul class="grid gap-3 sm:grid-cols-2 mb-6">
				@for(var s : books)
					<li>
						<a href="/bookkeeping/${s.book().id().toString()}" class="card bg-base-100 border border-base-200 rounded-box shadow-sm hover:bg-base-200 transition-colors">
							<div class="card-body p-4">
								<h2 class="card-title text-base wrap-anywhere">${s.book().name()}</h2>
								<p class="text-sm text-base-content/60">${BookkeepingView.msg("bookkeeping.role." + s.role().name())} · ${s.book().baseCurrency()}</p>
							</div>
						</a>
					</li>
				@endfor
			</ul>
		@endif

		@template._components.ui.card(title = BookkeepingView.msg("bookkeeping.create.title"), cssClass = "mt-6", body = @`
			<form method="post" action="/bookkeeping" class="grid gap-3 sm:grid-cols-3 items-end">
				<label class="form-control w-full">
					<span class="label-text mb-1">${BookkeepingView.msg("bookkeeping.field.bookName")}</span>
					<input type="text" name="name" required maxlength="100" class="input input-bordered w-full" />
				</label>
				<label class="form-control w-full">
					<span class="label-text mb-1">${BookkeepingView.msg("bookkeeping.field.baseCurrency")}</span>
					<select name="baseCurrency" class="select select-bordered w-full">
						@for(var c : currencies)
							<option value="${c}">${c}</option>
						@endfor
					</select>
				</label>
				<label class="form-control w-full">
					<span class="label-text mb-1">${BookkeepingView.msg("bookkeeping.field.monthStartDay")}</span>
					<select name="monthStartDay" class="select select-bordered w-full">
						@for(int d = 1; d <= 28; d++)
							<option value="${d}">${BookkeepingView.msg("bookkeeping.field.monthStartDay.option", d)}</option>
						@endfor
					</select>
				</label>
				<div class="sm:col-span-3">
					<button type="submit" class="btn btn-primary">${BookkeepingView.msg("bookkeeping.create.submit")}</button>
				</div>
			</form>
		`)
	`
)
