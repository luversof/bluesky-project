@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@import net.luversof.web.gate.bookkeeping.httpexchange.MemberClient.InvitePreview
@param String token
@param InvitePreview preview = null
@param String error = null
@param boolean isAuthenticated = false
@param String username = ""
@param String profile = "local"

!{ String title = BookkeepingView.msg("bookkeeping.title.invite"); }
@template._layout.bookkeepingLayout(
	title = title,
	isAuthenticated = isAuthenticated,
	username = username,
	profile = profile,
	content = @`
		@template._components.ui.pageHeader(title = title)
		@if(error != null)
			<div role="alert" class="alert alert-error mb-4"><span>${error}</span></div>
		@endif
		@if(preview != null)
			@template._components.ui.card(body = @`
				<p class="text-lg font-semibold wrap-anywhere">${preview.bookName()}</p>
				@if(preview.used())
					<div role="alert" class="alert alert-warning mt-3"><span>${BookkeepingView.msg("bookkeeping.invite.used")}</span></div>
				@elseif(preview.expired())
					<div role="alert" class="alert alert-warning mt-3"><span>${BookkeepingView.msg("bookkeeping.invite.expired")}</span></div>
				@else
					<p class="text-sm text-base-content/60 mt-1">${BookkeepingView.msg("bookkeeping.invite.description")}</p>
					<form method="post" action="/bookkeeping/invite/${token}/accept" class="mt-4">
						<button type="submit" class="btn btn-primary">${BookkeepingView.msg("bookkeeping.invite.accept")}</button>
					</form>
				@endif
			`)
		@endif
		<a href="/bookkeeping?all=1" class="btn btn-sm btn-ghost mt-4">${BookkeepingView.msg("bookkeeping.nav.books")}</a>
	`
)
