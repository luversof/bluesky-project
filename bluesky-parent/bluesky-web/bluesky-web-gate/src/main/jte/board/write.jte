@import net.luversof.web.gate.board.domain.Board
@import io.github.luversof.boot.context.support.MessageUtil

@param Board board
@param String title = "게시판 글쓰기"
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
		<link rel="stylesheet" href="/css/vendor/easymde.min.css">
		<script src="/js/vendor/easymde.min.js"></script>

		<div class="container mx-auto px-4 py-8" data-board-mode="write" data-board-id="${board.id() != null ? board.id().toString() : ""}" data-board-alias="${board.alias()}">
			<div class="max-w-4xl mx-auto">
				<!-- 헤더 -->
				@template._components.ui.pageHeader(title = "글쓰기", description = "새로운 글을 작성해주세요", size = "text-3xl")
				
				<!-- 작성 폼 -->
				<div class="bg-base-100 rounded-box shadow-xl p-6">
					<div class="form-control mb-6">
						<label class="label" for="title">
							<span class="label-text font-semibold">제목</span>
							<span class="label-text-alt text-error">*</span>
						</label>
						<input type="text" id="title" placeholder="제목을 입력하세요" 
							class="input input-bordered w-full" maxlength="200" />
					</div>
					
					<div class="form-control mb-6">
						<label class="label" for="content">
							<span class="label-text font-semibold">내용</span>
							<span class="label-text-alt text-error">*</span>
						</label>
						<textarea id="content" class="textarea textarea-bordered" rows="15" 
							placeholder="내용을 입력하세요"></textarea>
					</div>
					
					<div class="flex justify-between items-center pt-4 border-t">
						<div class="text-sm text-gray-500">
							<svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 inline" fill="none" viewBox="0 0 24 24" stroke="currentColor">
								<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
							</svg>
							마크다운 문법을 사용할 수 있습니다
						</div>
						<div class="flex gap-2">
							<button class="btn btn-ghost cancelButton">
								<svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
									<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
								</svg>
								<span>${MessageUtil.getMessage("board.button.cancel")}</span>
							</button>
							<button class="btn btn-primary writeButton">
								<svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
									<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
								</svg>
								<span>${MessageUtil.getMessage("board.button.write")}</span>
							</button>
						</div>
					</div>
				</div>
			</div>
		</div>
	`
)
