@import java.util.List
@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@import net.luversof.web.gate.bookkeeping.controller.BookkeepingSettingsViewController.SettingsPage
@param SettingsPage page
@param List<String> currencies
@param String error = null
@param boolean isAuthenticated = false
@param String username = ""
@param String profile = "local"

!{ String title = BookkeepingView.msg("bookkeeping.title.settings"); }
@template._layout.bookkeepingLayout(
	title = page.book().name() + " " + title,
	bookId = page.book().id(),
	active = "settings",
	isAuthenticated = isAuthenticated,
	username = username,
	profile = profile,
	content = @`
		@template._components.ui.pageHeader(title = page.book().name(), description = title)
		@if(error != null)
			<div role="alert" class="alert alert-error mb-4"><span>${error}</span></div>
		@endif
		@if(page.notice() != null)
			<div role="status" class="alert alert-success mb-4"><span>${page.notice()}</span></div>
		@endif
		@template.bookkeeping.fragments.settingsBook(page = page, currencies = currencies)
		@template.bookkeeping.fragments.settingsCategories(page = page)
		@template.bookkeeping.fragments.settingsBudgets(page = page)
		@template.bookkeeping.fragments.settingsFavorites(page = page)
		@template.bookkeeping.fragments.settingsRecurrings(page = page)
		@template.bookkeeping.fragments.settingsAssets(page = page, currencies = currencies)
		@template.bookkeeping.fragments.settingsMembers(page = page)
	`
)
