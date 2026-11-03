@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@param String error = ""
@param boolean isAuthenticated = false
@param String username = ""
@param String profile = "local"

!{ String title = BookkeepingView.msg("bookkeeping.title.error"); }
@template._layout.bookkeepingLayout(
	title = title,
	isAuthenticated = isAuthenticated,
	username = username,
	profile = profile,
	content = @`
		@template._components.ui.pageHeader(title = title)
		<div role="alert" class="alert alert-error"><span>${error}</span></div>
		<a href="/bookkeeping?all=1" class="btn btn-sm btn-ghost mt-4">${BookkeepingView.msg("bookkeeping.nav.books")}</a>
	`
)
