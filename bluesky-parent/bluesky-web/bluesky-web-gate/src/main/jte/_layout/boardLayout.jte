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

		<div class="flex flex-col lg:flex-row">
			<div class="grid bg-base-100">
				@template._components.navmenu(menu = "board", classAppend = "bg-base-100 w-56")
			</div>
			<div class="grid grow mx-4">
				${content}
			</div>
		</div>
	`
)
