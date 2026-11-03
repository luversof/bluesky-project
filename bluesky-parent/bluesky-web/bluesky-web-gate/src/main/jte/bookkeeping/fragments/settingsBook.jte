@import java.util.List
@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@import net.luversof.web.gate.bookkeeping.controller.BookkeepingSettingsViewController.SettingsPage
@param SettingsPage page
@param List<String> currencies

<%-- 가계부 정보(OWNER 만 고친다 - MEMBER 에게는 읽기만 보인다). 기준통화는 표시용(1차는 환산 없음). --%>
!{ var book = page.book(); }
!{ String base = "/bookkeeping/" + book.id() + "/settings"; }
<section aria-labelledby="bk-set-book" class="mb-6">
	<h2 id="bk-set-book" class="text-base font-semibold mb-2">${BookkeepingView.msg("bookkeeping.settings.book")}</h2>
	@if(page.owner())
		<form method="post" action="${base}/book" class="grid gap-3 sm:grid-cols-3 items-end rounded-box border border-base-200 bg-base-100 p-4">
			<label class="form-control w-full">
				<span class="label-text mb-1">${BookkeepingView.msg("bookkeeping.field.bookName")}</span>
				<input type="text" name="name" value="${book.name()}" required maxlength="100" class="input input-bordered w-full" />
			</label>
			<label class="form-control w-full">
				<span class="label-text mb-1">${BookkeepingView.msg("bookkeeping.field.baseCurrency")}</span>
				<select name="baseCurrency" class="select select-bordered w-full">
					@for(var c : currencies)
						<option value="${c}" selected="${c.equals(book.baseCurrency())}">${c}</option>
					@endfor
				</select>
			</label>
			<label class="form-control w-full">
				<span class="label-text mb-1">${BookkeepingView.msg("bookkeeping.field.monthStartDay")}</span>
				<select name="monthStartDay" class="select select-bordered w-full">
					@for(int d = 1; d <= 28; d++)
						<option value="${d}" selected="${d == book.monthStartDay()}">${BookkeepingView.msg("bookkeeping.field.monthStartDay.option", d)}</option>
					@endfor
				</select>
			</label>
			<div class="sm:col-span-3"><button type="submit" class="btn btn-primary btn-sm">${BookkeepingView.msg("bookkeeping.settings.save")}</button></div>
		</form>
		<details class="mt-3">
			<summary class="cursor-pointer text-sm text-error">${BookkeepingView.msg("bookkeeping.settings.deleteBook")}</summary>
			<form method="post" action="${base}/delete" class="mt-2 flex flex-wrap items-center gap-3 rounded-box border border-error/40 p-4">
				<label class="flex items-center gap-2 text-sm">
					<input type="checkbox" name="confirm" value="1" required class="checkbox checkbox-sm" />
					${BookkeepingView.msg("bookkeeping.settings.deleteBook.confirm")}
				</label>
				<button type="submit" class="btn btn-sm btn-error">${BookkeepingView.msg("bookkeeping.settings.deleteBook.submit")}</button>
			</form>
		</details>
	@else
		<p class="text-sm rounded-box border border-base-200 bg-base-100 p-4">${book.name()} · ${book.baseCurrency()} · ${BookkeepingView.msg("bookkeeping.field.monthStartDay")} ${BookkeepingView.msg("bookkeeping.field.monthStartDay.option", book.monthStartDay())}</p>
	@endif
</section>
