@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient.Category
@import net.luversof.web.gate.bookkeeping.controller.BookkeepingSettingsViewController.SettingsPage
@param SettingsPage page
@param Category c
@param boolean child

!{ String base = "/bookkeeping/" + page.book().id() + "/settings/categories/" + c.id(); }
<li class="py-2 ${child ? "pl-5" : ""}">
	@if(c.id().equals(page.editCategory()))
		<form method="post" action="${base}" class="flex flex-wrap items-center gap-2">
			<input type="text" name="name" value="${c.name()}" required maxlength="100" aria-label="${BookkeepingView.msg("bookkeeping.settings.category.name")}" class="input input-sm input-bordered w-36" />
			<label class="flex items-center gap-1 text-xs">
				<input type="checkbox" name="hidden" value="1" checked="${c.hidden()}" class="checkbox checkbox-sm" />
				${BookkeepingView.msg("bookkeeping.settings.hidden")}
			</label>
			<button type="submit" class="btn btn-xs btn-primary">${BookkeepingView.msg("bookkeeping.settings.save")}</button>
			<a href="/bookkeeping/${page.book().id().toString()}/settings" class="btn btn-xs btn-ghost">${BookkeepingView.msg("bookkeeping.settings.cancel")}</a>
		</form>
	@else
		<div class="flex flex-wrap items-center gap-2">
			<span class="min-w-0 flex-1 text-sm wrap-anywhere ${c.hidden() ? "text-base-content/50" : ""}">${child ? "· " : ""}${c.name()}
				@if(c.hidden())<span class="badge badge-sm badge-ghost ml-1">${BookkeepingView.msg("bookkeeping.settings.hidden")}</span>@endif
			</span>
			<form method="post" action="${base}/move" class="contents">
				<input type="hidden" name="direction" value="up" />
				<button type="submit" class="btn btn-xs btn-ghost" aria-label="${BookkeepingView.msg("bookkeeping.settings.up", c.name())}">↑</button>
			</form>
			<form method="post" action="${base}/move" class="contents">
				<input type="hidden" name="direction" value="down" />
				<button type="submit" class="btn btn-xs btn-ghost" aria-label="${BookkeepingView.msg("bookkeeping.settings.down", c.name())}">↓</button>
			</form>
			<a href="/bookkeeping/${page.book().id().toString()}/settings?editCategory=${c.id().toString()}" class="btn btn-xs btn-ghost">${BookkeepingView.msg("bookkeeping.settings.edit")}</a>
			<form method="post" action="${base}/delete" class="contents">
				<button type="submit" class="btn btn-xs btn-ghost text-error">${BookkeepingView.msg("bookkeeping.settings.delete")}</button>
			</form>
		</div>
	@endif
</li>
