@import org.springframework.data.domain.Page
@import net.luversof.web.gate.board.domain.BoardArticle
@import net.luversof.web.gate.interceptor.PaginationInterceptor.Pagination
@import io.github.luversof.boot.context.support.MessageUtil
@import java.time.format.DateTimeFormatter
@import java.time.ZoneId

@param Page<BoardArticle> page
@param Pagination pagePagination

<div class="overflow-x-auto">
	<table class="table table-zebra">
		<!-- head -->
		<thead>
			<tr>
				<th class="w-20">번호</th>
				<th>${MessageUtil.getMessage("boardArticle.title")}</th>
				<th class="w-32 text-center">작성자</th>
				<th class="w-32 text-center">${MessageUtil.getMessage("boardArticle.createdDate")}</th>
			</tr>
		</thead>
		<tbody>
			@if(page.getContent().isEmpty())
				<tr>
					<td colspan="4" class="text-center py-10">
						<div class="flex flex-col items-center gap-2">
							<svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
								<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
							</svg>
							<span class="text-gray-500">${MessageUtil.getMessage("boardArticle.noData")}</span>
						</div>
					</td>
				</tr>
			@else
				@for(int i = 0; i < page.getContent().size(); i++)
					!{var boardArticle = page.getContent().get(i);}
					<tr class="cursor-pointer transition-colors" data-boardArticleId="${boardArticle.id() != null ? boardArticle.id().toString() : ""}">
						<td class="text-center">${page.getTotalElements() - (page.getNumber() * page.getSize()) - i}</td>
						<td>
							<div class="font-medium">
								<span>${boardArticle.title()}</span>
								@if(boardArticle.commentCount() != null && boardArticle.commentCount() > 0)
									<span class="ml-2 text-xs text-gray-500">(${boardArticle.commentCount()})</span>
								@endif
							</div>
						</td>
						<td class="text-center text-sm">${boardArticle.username()}</td>
						<td class="text-center text-sm" data-date="${boardArticle.createdDate() != null ? boardArticle.createdDate().toString() : ""}">${boardArticle.createdDate() != null ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(boardArticle.createdDate()) : ""}</td>
					</tr>
				@endfor
			@endif
		</tbody>
	</table>
</div>

<div class="flex justify-between items-center mt-6 px-4">
	<div>
		<!-- 왼쪽 여백 -->
	</div>
	<div>
		@template._components.pagination(pagination = pagePagination)
	</div>
	<div>
		<button class="btn btn-primary btn-sm writeButton">
			<svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
				<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
			</svg>
			<span>${MessageUtil.getMessage("board.button.write")}</span>
		</button>
	</div>
</div>
