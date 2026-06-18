@import net.luversof.web.gate.board.domain.Board
@import net.luversof.web.gate.board.domain.BoardArticle
@import io.github.luversof.boot.context.support.MessageUtil
@import java.time.format.DateTimeFormatter
@import java.time.ZoneId

@param Board board
@param BoardArticle boardArticle
@param java.util.UUID boardArticleId
@param boolean isOwner = false
@param java.util.UUID currentUserId = null
@param String title = "게시글 보기"
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
		<div class="container mx-auto px-4 py-8" data-board-mode="view"
			data-board-id="${board.id() != null ? board.id().toString() : ""}"
			data-board-alias="${board.alias()}"
			data-board-article-id="${boardArticleId != null ? boardArticleId.toString() : ""}"
			data-is-authenticated="${isAuthenticated}"
			data-current-user-id="${currentUserId != null ? currentUserId.toString() : ""}"
			data-comment-delete-label="${MessageUtil.getMessage("board.comment.delete")}"
			data-comment-delete-confirm="${MessageUtil.getMessage("board.comment.deleteConfirm")}"
			data-comment-required="${MessageUtil.getMessage("board.comment.required")}"
			data-comment-create-success="${MessageUtil.getMessage("board.comment.createSuccess")}"
			data-comment-delete-success="${MessageUtil.getMessage("board.comment.deleteSuccess")}">
			
			<div class="max-w-4xl mx-auto">
				<!-- 게시글 헤더 -->
				<div class="bg-base-100 rounded-box shadow-xl p-6 mb-4">
					<h1 class="text-3xl font-bold mb-4">${boardArticle.title()}</h1>
					
					<div class="flex items-center justify-between text-sm text-gray-600 pb-4 border-b">
						<div class="flex items-center gap-4">
							<div class="flex items-center gap-1">
								<svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
									<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
								</svg>
								<span>${boardArticle.username()}</span>
							</div>
							<div class="flex items-center gap-1">
								<svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
									<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
								</svg>
								<span>${boardArticle.createdDate() != null ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(boardArticle.createdDate()) : ""}</span>
							</div>
						</div>
					</div>
				</div>
				
				<!-- 게시글 내용 -->
				<div class="bg-base-100 rounded-box shadow-xl p-6 mb-4">
					<article class="prose max-w-none">
						<div>$unsafe{boardArticle.content()}</div>
					</article>
				</div>
				
				<!-- 댓글 영역 -->
				<div class="bg-base-100 rounded-box shadow-xl p-6 mb-6" id="commentSection">
					<div class="flex items-center justify-between mb-4">
						<h2 class="text-2xl font-bold">${MessageUtil.getMessage("board.comment.title")}</h2>
						<div class="text-sm text-gray-500">
							<span>${MessageUtil.getMessage("board.comment.countPrefix")}</span>
							<span id="commentCount" class="font-semibold">0</span>
						</div>
					</div>
					
					<div id="commentList" class="space-y-4" data-empty-text="${MessageUtil.getMessage("board.comment.empty")}">
						<p class="text-sm text-gray-500">${MessageUtil.getMessage("board.comment.empty")}</p>
					</div>
					<button id="commentLoadMore" type="button" class="btn btn-ghost w-full mt-4 hidden">${MessageUtil.getMessage("board.comment.loadMore")}</button>
					
					@if(isAuthenticated)
						<div class="mt-6">
							<form id="commentForm" class="space-y-3">
								<label class="block text-sm font-medium" for="commentContent">${MessageUtil.getMessage("board.comment.placeholder")}</label>
								<textarea id="commentContent" class="textarea textarea-bordered w-full" rows="4"
									placeholder="${MessageUtil.getMessage("board.comment.placeholder")}"></textarea>
								<div class="flex justify-end gap-2">
									<button type="button" class="btn btn-primary commentSubmitButton">${MessageUtil.getMessage("board.comment.submit")}</button>
								</div>
							</form>
						</div>
					@else
						<div class="mt-6 text-sm text-gray-500">${MessageUtil.getMessage("board.comment.loginRequired")}</div>
					@endif
				</div>
				
				<!-- 액션 버튼 -->
				<div class="flex justify-between items-center">
					<div>
						<button class="btn btn-ghost listButton">
							<svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
								<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
							</svg>
							<span>${MessageUtil.getMessage("board.button.list")}</span>
						</button>
					</div>
					<div class="flex gap-2">
						<button class="btn btn-primary writeButton">
							<svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
								<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
							</svg>
							<span>${MessageUtil.getMessage("board.button.write")}</span>
						</button>
						@if(isOwner)
							<button class="btn btn-secondary modifyButton" data-boardArticleId="${boardArticle.id().toString()}">
								<svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
									<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
								</svg>
								<span>${MessageUtil.getMessage("board.button.modify")}</span>
							</button>
							<button class="btn btn-error deleteButton" data-boardArticleId="${boardArticle.id().toString()}">
								<svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
									<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
								</svg>
								<span>${MessageUtil.getMessage("board.button.delete")}</span>
							</button>
						@endif
					</div>
				</div>
			</div>
		</div>
	`
)
