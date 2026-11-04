@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@import net.luversof.web.gate.bookkeeping.controller.BookkeepingViewController.MonthPage
@param MonthPage page

<%-- 예산 대비 지출(기준통화). 예산이 없으면 아무것도 그리지 않는다. 불러오지 못하면 한 줄 안내만. --%>
!{ var budget = page.budget(); }
!{ String currency = page.book().baseCurrency(); }
@if(budget.failed())
	<p role="status" class="mb-4 text-sm text-base-content/60">${BookkeepingView.msg("bookkeeping.budget.unavailable")}</p>
@elseif(!budget.lines().isEmpty())
	<section aria-labelledby="bk-budget-title" class="mb-4 rounded-box border border-base-200 bg-base-100 p-4">
		<h2 id="bk-budget-title" class="text-sm font-semibold mb-3">${BookkeepingView.msg("bookkeeping.budget.title")} <span class="font-normal text-base-content/60">(${currency})</span></h2>
		<ul class="grid gap-3 sm:grid-cols-2">
			@for(var line : budget.lines())
				<li>
					<div class="flex flex-wrap items-baseline justify-between gap-2 text-sm">
						<span class="font-medium">${line.name()}</span>
						<span class="amount-value">${BookkeepingView.amount(line.spent(), currency)} / ${BookkeepingView.amount(line.budget(), currency)}</span>
					</div>
					<progress class="w-full h-2" value="${line.bar()}" max="100" aria-label="${line.name()} ${line.percent()}%"></progress>
					<p class="text-xs text-base-content/60">
						${line.percent()}% ·
						@if(line.over())
							<span class="badge badge-warning badge-sm">${BookkeepingView.msg("bookkeeping.budget.over", BookkeepingView.amount(line.left(), currency))}</span>
						@else
							${BookkeepingView.msg("bookkeeping.budget.left", BookkeepingView.amount(line.left(), currency))}
						@endif
					</p>
				</li>
			@endfor
		</ul>
		@if(!budget.missing().isEmpty())
			<p class="mt-2 text-xs text-base-content/60">${BookkeepingView.msg("bookkeeping.budget.missingFx", String.join(", ", budget.missing()))}</p>
		@endif
	</section>
@endif
