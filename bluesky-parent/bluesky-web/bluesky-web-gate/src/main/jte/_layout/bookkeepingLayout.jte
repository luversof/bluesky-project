@import java.util.UUID
@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@param gg.jte.Content content
@param String title = ""
@param UUID bookId = null
@param String active = ""
@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"

<%-- 가계부 화면 껍데기. 가계부 안이면(bookId) 화면 사이 이동 링크를 둔다 - 화면을 바꾸는 링크라 탭 모양을 쓰지 않는다. --%>
!{ String suffix = BookkeepingView.msg("bookkeeping.title.suffix"); }
@template._layout.defaultLayout(
	title = title == null || title.isEmpty() ? suffix : title + " · " + suffix,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	content = @`
		<div class="mx-auto w-full max-w-5xl p-4">
			@if(bookId != null)
				<nav aria-label="${BookkeepingView.msg("bookkeeping.nav.label")}" class="mb-4 flex flex-wrap items-center gap-2">
					<a href="/bookkeeping/${bookId.toString()}" class="btn btn-sm ${"month".equals(active) ? "btn-primary" : "btn-ghost"}" aria-current="${"month".equals(active) ? "page" : null}">${BookkeepingView.msg("bookkeeping.nav.month")}</a>
					<a href="/bookkeeping/${bookId.toString()}/assets" class="btn btn-sm ${"assets".equals(active) ? "btn-primary" : "btn-ghost"}" aria-current="${"assets".equals(active) ? "page" : null}">${BookkeepingView.msg("bookkeeping.nav.assets")}</a>
					<a href="/bookkeeping/${bookId.toString()}/stats" class="btn btn-sm ${"stats".equals(active) ? "btn-primary" : "btn-ghost"}" aria-current="${"stats".equals(active) ? "page" : null}">${BookkeepingView.msg("bookkeeping.nav.stats")}</a>
					<a href="/bookkeeping/${bookId.toString()}/settings" class="btn btn-sm ${"settings".equals(active) ? "btn-primary" : "btn-ghost"}" aria-current="${"settings".equals(active) ? "page" : null}">${BookkeepingView.msg("bookkeeping.nav.settings")}</a>
					<a href="/bookkeeping?all=1" class="btn btn-sm btn-ghost ml-auto">${BookkeepingView.msg("bookkeeping.nav.books")}</a>
				</nav>
			@endif
			${content}
		</div>
	`
)
