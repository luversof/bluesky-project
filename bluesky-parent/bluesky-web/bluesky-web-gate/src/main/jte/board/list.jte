@import net.luversof.web.gate.board.domain.Board

@param Board board
@param String title = "게시판 목록"
@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"

@template._layout.boardLayout(
	title = title,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	content = @`
		<div class="container mx-auto px-4 py-8" data-board-mode="list" data-board-alias="${board.alias()}" data-is-authenticated="${isAuthenticated}">
			<!-- 헤더 -->
			@template._components.ui.pageHeader(
				title = board.jsonConfig() != null && board.jsonConfig().get("name") != null ? board.jsonConfig().get("name").toString() : "",
				description = board.jsonConfig() != null && board.jsonConfig().get("description") != null ? board.jsonConfig().get("description").toString() : "",
				size = "text-3xl")
			
			<!-- 로딩 인디케이터 -->
			<div id="indicator" class="text-center my-10 hidden">
				<span class="loading loading-ring loading-lg"></span>
			</div>
			
			<!-- 게시글 목록 -->
			<div id="boardList"
				class="bg-base-100 rounded-box shadow-xl"
				hx-get="/board/htmx/${board.alias()}/list"
				hx-trigger="load delay:0s, listHtmxTrigger"
				hx-swap="transition:true"
				hx-indicator="#indicator"
				data-page-param-from-query="page">
			</div>
		</div>
	`
)
