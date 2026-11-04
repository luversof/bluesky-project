@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@import net.luversof.web.gate.bookkeeping.controller.BookkeepingViewController.MonthPage
@param MonthPage page

<%-- 외화 합계가 있는 달만: 기준통화 환산 합계 + 이 달 환율 입력. 환율이 없는 통화는 빼고 더하고 "환율 없음"으로 알린다. --%>
!{ var fx = page.fx(); }
@if(fx != null)
	<section aria-labelledby="bk-fx-title" class="mb-4 rounded-box border border-base-200 bg-base-100 p-4">
		<h2 id="bk-fx-title" class="text-sm font-semibold mb-2">${BookkeepingView.msg("bookkeeping.fx.title", fx.base())}</h2>
		@if(fx.failed())
			<p role="status" class="text-sm text-base-content/60">${BookkeepingView.msg("bookkeeping.fx.unavailable")}</p>
		@else
			<dl class="grid grid-cols-3 gap-2 text-sm">
				<div><dt class="text-base-content/60">${BookkeepingView.msg("bookkeeping.summary.income")}</dt><dd class="font-semibold amount-value">${BookkeepingView.amount(fx.income(), fx.base())}</dd></div>
				<div><dt class="text-base-content/60">${BookkeepingView.msg("bookkeeping.summary.expense")}</dt><dd class="font-semibold amount-value">${BookkeepingView.amount(fx.expense(), fx.base())}</dd></div>
				<div><dt class="text-base-content/60">${BookkeepingView.msg("bookkeeping.summary.net")}</dt><dd class="font-semibold amount-value">${BookkeepingView.amount(fx.net(), fx.base())}</dd></div>
			</dl>
			@if(!fx.missing().isEmpty())
				<p role="status" class="mt-2 text-xs"><span class="badge badge-warning badge-sm">${BookkeepingView.msg("bookkeeping.fx.missing", String.join(", ", fx.missing()))}</span></p>
			@endif
			<details class="mt-2" open="${!fx.missing().isEmpty()}">
				<summary class="cursor-pointer text-xs text-base-content/60">${BookkeepingView.msg("bookkeeping.fx.edit", BookkeepingView.monthLabel(page.range().month()))}</summary>
				<form method="post" action="/bookkeeping/${page.book().id().toString()}/fx-rates" class="mt-2 flex flex-wrap items-end gap-3">
					<input type="hidden" name="month" value="${page.range().month().toString()}" />
					@for(var c : fx.foreign())
						<label class="form-control">
							<span class="label-text text-xs mb-1">${BookkeepingView.msg("bookkeeping.fx.rate", c, fx.base())}</span>
							<input type="text" inputmode="decimal" name="rate.${c}" value="${fx.input(c)}" class="input input-sm input-bordered w-32" />
						</label>
					@endfor
					<button type="submit" class="btn btn-sm">${BookkeepingView.msg("bookkeeping.fx.save")}</button>
				</form>
			</details>
		@endif
	</section>
@endif
