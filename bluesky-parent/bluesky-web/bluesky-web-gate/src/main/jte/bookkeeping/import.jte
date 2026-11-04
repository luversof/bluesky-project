@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@import net.luversof.web.gate.bookkeeping.support.CsvImport
@import net.luversof.web.gate.bookkeeping.controller.BookkeepingImportController.ImportPage
@param ImportPage page
@param String error = null
@param boolean isAuthenticated = false
@param String username = ""
@param String profile = "local"

<%-- CSV 가져오기: 올리기 → 미리보기(오류가 있으면 넣기를 막는다) → 넣기. 형식은 내보내기와 같다. CSV 글자는 숨은 칸으로 다시 보낸다. --%>
!{ var book = page.book(); }
!{ String title = BookkeepingView.msg("bookkeeping.import.title"); }
!{ String base = "/bookkeeping/" + book.id() + "/import"; }
!{ var result = page.result(); }
@template._layout.bookkeepingLayout(
	title = book.name() + " " + title,
	bookId = book.id(),
	active = "month",
	isAuthenticated = isAuthenticated,
	username = username,
	profile = profile,
	content = @`
		@template._components.ui.pageHeader(title = book.name(), description = title)
		@if(error != null)
			<div role="alert" class="alert alert-error mb-4"><span>${error}</span></div>
		@endif
		@if(page.done() != null)
			<div role="status" class="alert alert-success mb-4"><span>${BookkeepingView.msg("bookkeeping.import.done", page.done())}</span> <a href="/bookkeeping/${book.id().toString()}" class="link">${BookkeepingView.msg("bookkeeping.import.toMonth")}</a></div>
		@endif

		<section aria-labelledby="bk-import-upload" class="mb-6 rounded-box border border-base-200 bg-base-100 p-4">
			<h2 id="bk-import-upload" class="text-base font-semibold mb-2">${BookkeepingView.msg("bookkeeping.import.upload")}</h2>
			<p class="text-sm text-base-content/70 mb-3">${BookkeepingView.msg("bookkeeping.import.help", CsvImport.MAX_ROWS)}</p>
			<form method="post" action="${base}" enctype="multipart/form-data" class="flex flex-wrap items-end gap-3">
				<label class="form-control">
					<span class="label-text text-xs mb-1">${BookkeepingView.msg("bookkeeping.import.file")}</span>
					<input type="file" name="file" accept=".csv,text/csv" required class="text-sm" />
				</label>
				<button type="submit" class="btn btn-sm">${BookkeepingView.msg("bookkeeping.import.preview")}</button>
			</form>
		</section>

		@if(result != null)
			<section aria-labelledby="bk-import-preview">
				<h2 id="bk-import-preview" class="text-base font-semibold mb-2">${BookkeepingView.msg("bookkeeping.import.previewTitle")}</h2>
				@if(result.fileError() != null)
					<div role="alert" class="alert alert-error mb-4"><span>${result.fileError()}</span></div>
				@else
					<p class="text-sm mb-3">${BookkeepingView.msg("bookkeeping.import.counts", result.rows().size(), result.errors().size())}</p>
					@if(!result.errors().isEmpty())
						<div role="alert" class="alert alert-warning mb-3"><span>${BookkeepingView.msg("bookkeeping.import.fixErrors")}</span></div>
						<ul class="mb-4 divide-y divide-base-200 rounded-box border border-base-200 bg-base-100 text-sm">
							@for(var e : result.errors().subList(0, Math.min(result.errors().size(), 50)))
								<li class="px-3 py-2"><span class="font-semibold">${BookkeepingView.msg("bookkeeping.import.line", e.line())}</span> ${e.message()}</li>
							@endfor
						</ul>
					@endif
					@if(!result.rows().isEmpty())
						<ul class="mb-4 divide-y divide-base-200 rounded-box border border-base-200 bg-base-100 text-sm">
							@for(var r : result.rows().subList(0, Math.min(result.rows().size(), 20)))
								<li class="px-3 py-2"><span class="text-base-content/60">${BookkeepingView.msg("bookkeeping.import.line", r.line())}</span> <span class="amount-value">${r.summary()}</span></li>
							@endfor
						</ul>
						@if(result.rows().size() > 20)
							<p class="text-xs text-base-content/60 mb-3">${BookkeepingView.msg("bookkeeping.import.more", result.rows().size() - 20)}</p>
						@endif
					@endif
					@if(result.importable())
						<form method="post" action="${base}/confirm">
							<textarea name="csv" hidden>${page.csv()}</textarea>
							<button type="submit" class="btn btn-primary">${BookkeepingView.msg("bookkeeping.import.confirm", result.rows().size())}</button>
						</form>
					@endif
				@endif
			</section>
		@endif
	`
)
