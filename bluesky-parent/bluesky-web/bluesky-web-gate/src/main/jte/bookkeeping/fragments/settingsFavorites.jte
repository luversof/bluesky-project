@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@import net.luversof.web.gate.bookkeeping.controller.BookkeepingSettingsViewController.SettingsPage
@param SettingsPage page

<%-- 즐겨찾기: 만들기는 입력 모달의 "즐겨찾기로 저장", 여기서는 보기 · 지우기만. --%>
!{ String base = "/bookkeeping/" + page.book().id() + "/settings"; }
<section aria-labelledby="bk-set-favorites" class="mb-6">
	<h2 id="bk-set-favorites" class="text-base font-semibold mb-2">${BookkeepingView.msg("bookkeeping.favorite.title")}</h2>
	@if(page.favorites() == null)
		<p role="status" class="text-sm text-base-content/60">${BookkeepingView.msg("bookkeeping.favorite.unavailable")}</p>
	@elseif(page.favorites().isEmpty())
		<p class="text-sm text-base-content/60">${BookkeepingView.msg("bookkeeping.favorite.empty")}</p>
	@else
		<ul class="divide-y divide-base-200 rounded-box border border-base-200 bg-base-100">
			@for(var fav : page.favorites())
				<li class="flex flex-wrap items-center justify-between gap-2 px-3 py-2">
					<span class="text-sm"><span class="badge badge-sm mr-2">${BookkeepingView.msg("bookkeeping.type." + fav.type().name())}</span>${fav.name()}</span>
					<form method="post" action="${base}/favorites/${fav.id().toString()}/delete">
						<button type="submit" class="btn btn-xs btn-ghost">${BookkeepingView.msg("bookkeeping.favorite.delete")}</button>
					</form>
				</li>
			@endfor
		</ul>
	@endif
</section>
