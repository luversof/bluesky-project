@import net.luversof.web.gate.util.CspNonceHolder
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
		<script src="/js/vendor/dayjs.min.js"></script>
		<script src="/js/vendor/dayjs.locale.ko.js"></script>
		<script nonce="${CspNonceHolder.getNonce()}">
			dayjs.locale('ko');
		</script>
		<script src="/js/vendor/dayjs.plugin.relativeTime.js"></script>
		<script nonce="${CspNonceHolder.getNonce()}">
			dayjs.extend(globalThis.dayjs_plugin_relativeTime);
		</script>
		<script type="module" src="/js/board/board.js"></script>

		<div class="flex flex-col lg:flex-row min-h-[calc(100vh-4rem)]">
			<div class="lg:w-56 shrink-0">
				<div class="fixed top-16 left-0 z-40 h-[calc(100vh-4rem)] w-56 overflow-y-auto border-r border-base-200 bg-base-100 py-4">
					@template._components.navmenu(menu = "board", classAppend = "w-full")
				</div>
			</div>
			<div class="flex-1 min-w-0 p-4 relative">
				${content}
			</div>
		</div>
	`
)
