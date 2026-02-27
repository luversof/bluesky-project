@param gg.jte.Content content
@param String title = "Bluesky Board"
@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"

@template._layout.defaultLayout(
	title = title,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	content = @`
		<script src="https://unpkg.com/dayjs"></script>
		<script src="https://unpkg.com/dayjs/locale/ko"></script>
		<script>
			dayjs.locale('ko');
		</script>
		<script src="https://unpkg.com/dayjs/plugin/relativeTime"></script>
		<script>
			dayjs.extend(globalThis.dayjs_plugin_relativeTime);
		</script>
		<script type="module" src="/js/board/board.js"></script>

		<div class="flex flex-col lg:flex-row min-h-[calc(100vh-4rem)]">
			<div class="lg:w-56 shrink-0 border-r border-base-200 bg-base-100">
				<div class="sticky top-16 h-[calc(100vh-4rem)] overflow-y-auto py-4">
					@template._components.navmenu(menu = "board", classAppend = "w-full")
				</div>
			</div>
			<div class="flex-1 min-w-0 p-4 relative">
				${content}
			</div>
		</div>
	`
)
