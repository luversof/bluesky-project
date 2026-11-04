@import net.luversof.web.gate.bookkeeping.constant.CategoryKind
@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@import net.luversof.web.gate.bookkeeping.support.BudgetView
@import net.luversof.web.gate.bookkeeping.controller.BookkeepingSettingsViewController.SettingsPage
@param SettingsPage page

<%-- 월 예산: 전체 + 보이는 지출 대분류. 한 폼으로 저장하고 바뀐 칸만 보낸다. 빈 칸 = 예산 없음. --%>
!{ String base = "/bookkeeping/" + page.book().id() + "/settings"; }
!{ String currency = page.book().baseCurrency(); }
<section aria-labelledby="bk-set-budgets" class="mb-6">
	<h2 id="bk-set-budgets" class="text-base font-semibold mb-2">${BookkeepingView.msg("bookkeeping.settings.budgets")}</h2>
	@if(page.budgets() == null)
		<p role="status" class="text-sm text-base-content/60">${BookkeepingView.msg("bookkeeping.budget.unavailable")}</p>
	@else
		<form method="post" action="${base}/budgets" class="rounded-box border border-base-200 bg-base-100 p-4">
			<p class="text-xs text-base-content/60 mb-3">${BookkeepingView.msg("bookkeeping.settings.budget.help", currency)}</p>
			<div class="grid gap-3 sm:grid-cols-2">
				<label class="form-control w-full">
					<span class="label-text text-sm mb-1 font-medium">${BookkeepingView.msg("bookkeeping.budget.total")}</span>
					<input type="text" inputmode="decimal" name="budget.total" value="${BudgetView.input(page.budgets(), null)}" class="input input-sm input-bordered w-full" />
				</label>
				@for(var c : page.categories())
					@if(c.kind() == CategoryKind.EXPENSE && c.parentId() == null && !c.hidden())
						<label class="form-control w-full">
							<span class="label-text text-sm mb-1">${c.name()}</span>
							<input type="text" inputmode="decimal" name="budget.${c.id().toString()}" value="${BudgetView.input(page.budgets(), c.id())}" class="input input-sm input-bordered w-full" />
						</label>
					@endif
				@endfor
			</div>
			<button type="submit" class="btn btn-sm btn-primary mt-3">${BookkeepingView.msg("bookkeeping.settings.budget.save")}</button>
		</form>
	@endif
</section>
