@import net.luversof.web.gate.bookkeeping.constant.CategoryKind
@import net.luversof.web.gate.bookkeeping.constant.EntryType
@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@import net.luversof.web.gate.bookkeeping.support.RecurringView
@import net.luversof.web.gate.bookkeeping.controller.BookkeepingViewController.MonthPage
@param MonthPage page

<%-- 내역 입력 · 수정 모달. ?new=1 / ?edit= 로 서버가 열어 그린다(JS 없음). 종류 버튼은 같은 모달을 그 종류로 다시 연다 -
     종류마다 받는 칸이 달라서다(수입 · 지출 = 분류, 이체 = 받는 자산 · 받는 금액). 닫기는 모달 없는 같은 화면 주소. --%>
!{ var book = page.book(); }
!{ var month = page.range().month(); }
!{ var f = page.form(); }
!{ boolean editing = f.entryId() != null; }
!{ String closeHref = BookkeepingView.href(book.id(), month, "day", BookkeepingView.str(page.day()), "q", page.q(), "categoryId", BookkeepingView.str(page.categoryId()), "assetId", BookkeepingView.str(page.assetId())); }
!{ String title = BookkeepingView.msg(editing ? "bookkeeping.entry.edit" : "bookkeeping.entry.add"); }
<%-- 이 게이트는 daisyUI 플러그인이 없어(main.css 가 컴포넌트를 직접 정의, modal 없음) 유틸리티로 그린다. 배경 링크가 닫기다. --%>
<div class="fixed inset-0 z-50 flex items-center justify-center p-4" role="dialog" aria-modal="true" aria-labelledby="bk-entry-title">
	<a href="${closeHref}" class="absolute inset-0 bg-black/40" aria-label="${BookkeepingView.msg("bookkeeping.entry.close")}"></a>
	<div class="relative w-full max-w-lg max-h-[90vh] overflow-y-auto rounded-box bg-base-100 p-6 shadow-xl">
		<h2 id="bk-entry-title" class="text-lg font-bold mb-3">${title}</h2>
		<a href="${closeHref}" class="btn btn-sm btn-circle btn-ghost absolute right-2 top-2" aria-label="${BookkeepingView.msg("bookkeeping.entry.close")}">✕</a>

		@if(!editing && !page.favorites().isEmpty())
			<div class="mb-3 flex flex-wrap items-center gap-2" role="group" aria-label="${BookkeepingView.msg("bookkeeping.favorite.title")}">
				<span class="text-xs text-base-content/60">${BookkeepingView.msg("bookkeeping.favorite.title")}</span>
				@for(var fav : page.favorites())
					<a href="${BookkeepingView.href(book.id(), month, "new", "1", "fav", fav.id().toString(), "date", f.date())}" class="btn btn-xs btn-outline">${fav.name()}</a>
				@endfor
			</div>
		@endif
		@if(!editing)
			<div class="join mb-4" role="group" aria-label="${BookkeepingView.msg("bookkeeping.field.type")}">
				@for(var t : EntryType.values())
					<a href="${BookkeepingView.href(book.id(), month, "new", "1", "type", t.name(), "date", f.date())}" class="btn btn-sm join-item ${t == f.type() ? "btn-primary" : ""}" aria-current="${t == f.type() ? "true" : null}">${BookkeepingView.msg("bookkeeping.type." + t.name())}</a>
				@endfor
			</div>
		@else
			<p class="mb-4"><span class="badge">${BookkeepingView.msg("bookkeeping.type." + f.type().name())}</span></p>
		@endif

		<form method="post" action="/bookkeeping/${book.id().toString()}/entries" class="grid gap-3">
			<input type="hidden" name="type" value="${f.type().name()}" />
			<input type="hidden" name="month" value="${month.toString()}" />
			@if(editing)
				<input type="hidden" name="entryId" value="${f.entryId().toString()}" />
			@endif
			<div class="grid grid-cols-2 gap-3">
				<label class="form-control">
					<span class="label-text mb-1">${BookkeepingView.msg("bookkeeping.field.date")}</span>
					<input type="date" name="entryDate" value="${f.date()}" required class="input input-bordered w-full" />
				</label>
				<label class="form-control">
					<span class="label-text mb-1">${BookkeepingView.msg("bookkeeping.field.time")}</span>
					<input type="time" name="entryTime" value="${f.time()}" class="input input-bordered w-full" />
				</label>
			</div>
			<label class="form-control">
				<span class="label-text mb-1">${BookkeepingView.msg("bookkeeping.field.amount")}</span>
				<input type="text" name="amount" value="${f.amount()}" required inputmode="decimal" autocomplete="off" class="input input-bordered w-full" />
			</label>
			<label class="form-control">
				<span class="label-text mb-1">${BookkeepingView.msg(f.type() == EntryType.INCOME ? "bookkeeping.field.asset.income" : f.type() == EntryType.EXPENSE ? "bookkeeping.field.asset.expense" : "bookkeeping.field.asset.from")}</span>
				<select name="assetId" required class="select select-bordered w-full">
					@for(var o : BookkeepingView.assetOptions(page.assets(), f.assetId()))
						<option value="${o.id().toString()}" selected="${o.id().equals(f.assetId())}">${o.label()}</option>
					@endfor
				</select>
			</label>

			@if(f.type() == EntryType.TRANSFER)
				<label class="form-control">
					<span class="label-text mb-1">${BookkeepingView.msg("bookkeeping.field.asset.to")}</span>
					<select name="toAssetId" required class="select select-bordered w-full">
						@for(var o : BookkeepingView.assetOptions(page.assets(), f.toAssetId()))
							<option value="${o.id().toString()}" selected="${o.id().equals(f.toAssetId())}">${o.label()}</option>
						@endfor
					</select>
				</label>
				<label class="form-control">
					<span class="label-text mb-1">${BookkeepingView.msg("bookkeeping.field.toAmount")}</span>
					<input type="text" name="toAmount" value="${f.toAmount()}" inputmode="decimal" autocomplete="off" class="input input-bordered w-full" />
					<span class="label-text-alt text-base-content/60 mt-1">${BookkeepingView.msg("bookkeeping.field.toAmount.hint")}</span>
				</label>
			@else
				!{ var kind = f.type() == EntryType.INCOME ? CategoryKind.INCOME : CategoryKind.EXPENSE; }
				<label class="form-control">
					<span class="label-text mb-1">${BookkeepingView.msg("bookkeeping.field.category")}</span>
					<select name="${kind == CategoryKind.INCOME ? "incomeCategoryId" : "expenseCategoryId"}" required class="select select-bordered w-full">
						@for(var o : BookkeepingView.categoryOptions(page.categories(), kind, f.categoryId()))
							<option value="${o.id().toString()}" selected="${o.id().equals(f.categoryId())}">${o.child() ? "  · " + o.label() : o.label()}</option>
						@endfor
					</select>
				</label>
				@if(f.type() == EntryType.EXPENSE)
					<label class="form-control">
						<span class="label-text mb-1">${BookkeepingView.msg("bookkeeping.field.installment")}</span>
						<select name="installmentMonths" class="select select-bordered w-full">
							<option value="">${BookkeepingView.msg("bookkeeping.field.installment.none")}</option>
							@for(int n = 2; n <= 36; n++)
								<option value="${n}" selected="${f.installmentMonths() != null && f.installmentMonths() == n}">${BookkeepingView.msg("bookkeeping.field.installment.months", n)}</option>
							@endfor
						</select>
						<span class="label-text-alt text-base-content/60 mt-1">${BookkeepingView.msg("bookkeeping.field.installment.hint")}</span>
					</label>
				@endif
			@endif

			<label class="form-control">
				<span class="label-text mb-1">${BookkeepingView.msg("bookkeeping.field.content")}</span>
				<input type="text" name="content" value="${f.content()}" maxlength="200" class="input input-bordered w-full" />
			</label>
			<label class="form-control">
				<span class="label-text mb-1">${BookkeepingView.msg("bookkeeping.field.memo")}</span>
				<textarea name="memo" maxlength="1000" rows="2" class="textarea textarea-bordered w-full">${f.memo()}</textarea>
			</label>

			<%-- 저장이 먼저 와야 Enter 가 "저장"이 된다(폼의 첫 제출 단추). --%>
			<div class="mt-2 flex flex-wrap justify-end gap-2">
				<button type="submit" class="btn btn-primary">${BookkeepingView.msg("bookkeeping.entry.save")}</button>
				@if(!editing)
					<button type="submit" name="next" value="continue" class="btn btn-outline">${BookkeepingView.msg("bookkeeping.entry.saveAndContinue")}</button>
				@endif
			</div>

			<%-- 같은 폼을 즐겨찾기로 보낸다(formaction). 틀이라 필수 칸 검사를 건너뛴다(formnovalidate). --%>
			<details class="mt-1">
				<summary class="cursor-pointer text-xs text-base-content/60">${BookkeepingView.msg("bookkeeping.favorite.saveAs")}</summary>
				<div class="mt-2 flex flex-wrap items-end gap-2">
					<label class="form-control">
						<span class="label-text text-xs mb-1">${BookkeepingView.msg("bookkeeping.favorite.name")}</span>
						<input type="text" name="favoriteName" maxlength="100" class="input input-sm input-bordered" />
					</label>
					<button type="submit" formaction="/bookkeeping/${book.id().toString()}/favorites" formnovalidate class="btn btn-sm">${BookkeepingView.msg("bookkeeping.favorite.save")}</button>
				</div>
			</details>

			<%-- 반복으로 저장: 시작일 = 위 날짜, 주기의 일 · 요일 · 월도 그 날짜에서 딴다. 필수 칸 검사는 그대로(금액 · 자산 필요). --%>
			!{ var cycleDate = BookkeepingView.parseDate(f.date()); }
			@if(cycleDate != null)
				<details>
					<summary class="cursor-pointer text-xs text-base-content/60">${BookkeepingView.msg("bookkeeping.recurring.saveAs")}</summary>
					<div class="mt-2 flex flex-wrap items-end gap-2">
						<label class="form-control">
							<span class="label-text text-xs mb-1">${BookkeepingView.msg("bookkeeping.recurring.cycle")}</span>
							<select name="cycle" class="select select-sm select-bordered">
								@for(var c : RecurringView.CYCLES)
									<option value="${c}">${RecurringView.cycleOption(c, cycleDate)}</option>
								@endfor
							</select>
						</label>
						<label class="form-control">
							<span class="label-text text-xs mb-1">${BookkeepingView.msg("bookkeeping.recurring.endDate")}</span>
							<input type="date" name="endDate" class="input input-sm input-bordered" />
						</label>
						<button type="submit" formaction="/bookkeeping/${book.id().toString()}/recurrings" class="btn btn-sm">${BookkeepingView.msg("bookkeeping.recurring.save")}</button>
					</div>
					<p class="mt-1 text-xs text-base-content/60">${BookkeepingView.msg("bookkeeping.recurring.saveHint")}</p>
				</details>
			@endif
		</form>

		@if(editing)
			<form method="post" action="/bookkeeping/${book.id().toString()}/entries/${f.entryId().toString()}/delete" class="mt-2">
				<input type="hidden" name="month" value="${month.toString()}" />
				<button type="submit" class="btn btn-sm btn-ghost text-error">${BookkeepingView.msg("bookkeeping.entry.delete")}</button>
			</form>
		@endif
	</div>
</div>
