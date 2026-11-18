<%-- 쪽 번호 이동: « ‹ 1 … 10 › ». 번호는 10 쪽씩 묶는다. 갈 곳이 없는 처음 · 이전 · 다음 · 끝은 흐리게 둬
     단추 자리가 흔들리지 않게 한다. pageUrl 은 쪽 번호(0 부터)만 붙이면 되는 주소. 쪽이 하나면 아무것도 내지 않는다 --%>
@param int current
@param int lastPage
@param String pageUrl
@param int blockSize = 10

@if(lastPage > 0)
	!{var blockStart = current / blockSize * blockSize;}
	!{var blockEnd = Math.min(blockStart + blockSize - 1, lastPage);}
	<nav class="mt-4 flex flex-wrap items-center justify-center gap-1 text-sm" aria-label="페이지">
		@if(current > 0)
			<a class="page-btn" href="${pageUrl}0" aria-label="처음 쪽">«</a>
			<a class="page-btn" href="${pageUrl}${current - 1}" aria-label="이전 쪽">‹</a>
		@else
			<span class="page-btn page-btn-disabled" aria-hidden="true">«</span>
			<span class="page-btn page-btn-disabled" aria-hidden="true">‹</span>
		@endif
		@for(var n = blockStart; n <= blockEnd; n++)
			@if(n == current)
				<span class="page-btn page-btn-current" aria-current="page">${n + 1}</span>
			@else
				<a class="page-btn" href="${pageUrl}${n}">${n + 1}</a>
			@endif
		@endfor
		@if(current < lastPage)
			<a class="page-btn" href="${pageUrl}${current + 1}" aria-label="다음 쪽">›</a>
			<a class="page-btn" href="${pageUrl}${lastPage}" aria-label="마지막 쪽(${lastPage + 1})">»</a>
		@else
			<span class="page-btn page-btn-disabled" aria-hidden="true">›</span>
			<span class="page-btn page-btn-disabled" aria-hidden="true">»</span>
		@endif
	</nav>
@endif
