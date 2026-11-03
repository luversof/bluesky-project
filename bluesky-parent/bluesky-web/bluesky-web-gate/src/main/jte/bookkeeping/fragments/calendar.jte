@import java.time.DayOfWeek
@import java.time.format.TextStyle
@import org.springframework.context.i18n.LocaleContextHolder
@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@import net.luversof.web.gate.bookkeeping.controller.BookkeepingViewController.MonthPage
@param MonthPage page

<%-- 달력: 날짜를 누르면 그날 내역만 보인다. 기간은 월 시작일 기준이라 달력 달과 다를 수 있다.
     금액은 부호(+수입 · -지출)로만 가른다 - 손익 색과 헷갈리지 않게 색을 박지 않는다(AmountColorConventionTest). --%>
!{ var book = page.book(); }
!{ var month = page.range().month(); }
!{ String title = BookkeepingView.msg("bookkeeping.calendar.title"); }
<section aria-labelledby="bk-calendar-title" class="mb-4">
	<h2 id="bk-calendar-title" class="text-base font-semibold mb-2">${title}</h2>
	<div class="overflow-x-auto">
		<table class="table table-fixed w-full min-w-[20rem] text-xs" aria-label="${title}">
			<thead>
				<tr>
					@for(var dow : new DayOfWeek[] {DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY})
						<th scope="col" class="text-center px-1">${dow.getDisplayName(TextStyle.SHORT, LocaleContextHolder.getLocale())}</th>
					@endfor
				</tr>
			</thead>
			<tbody>
				@for(var week : page.weeks())
					<tr>
						@for(var cell : week)
							@if(cell.date() == null)
								<td class="p-1"></td>
							@else
								<td class="p-1 align-top">
									<a href="${BookkeepingView.href(book.id(), month, "day", cell.date().toString())}" class="block rounded-box p-1 min-h-14 hover:bg-base-200 ${cell.date().equals(page.day()) ? "bg-primary/10 ring-1 ring-primary" : ""}" aria-current="${cell.date().equals(page.day()) ? "date" : null}">
										<span class="font-semibold ${cell.today() ? "text-primary" : ""}">${cell.date().getDayOfMonth()}</span>
										@for(var t : cell.totals())
											@if(t.income().signum() > 0)
												!{ String income = "+" + BookkeepingView.amount(t.income(), t.currency()) + (t.currency().equals(book.baseCurrency()) ? "" : " " + t.currency()); }
												<span class="block truncate amount-value" title="${income}">${income}</span>
											@endif
											@if(t.expense().signum() > 0)
												!{ String expense = "-" + BookkeepingView.amount(t.expense(), t.currency()) + (t.currency().equals(book.baseCurrency()) ? "" : " " + t.currency()); }
												<span class="block truncate text-base-content/70 amount-value" title="${expense}">${expense}</span>
											@endif
										@endfor
									</a>
								</td>
							@endif
						@endfor
					</tr>
				@endfor
			</tbody>
		</table>
	</div>
</section>
