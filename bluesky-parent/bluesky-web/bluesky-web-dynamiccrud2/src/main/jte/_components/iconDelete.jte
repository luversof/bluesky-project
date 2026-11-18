<%-- 이름 옆 삭제 단추(확인 후 POST). 휴지통 SVG(currentColor, 눈 아이콘과 같은 선 굵기) - × 는 취소 · 빼기에 쓴다 --%>
@param String action
@param String label
@param String confirm

<form method="post" action="${action}" class="inline-flex">
	<button class="icon-btn icon-btn-danger" aria-label="${label}" title="삭제" data-confirm="${confirm}">
		<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
			<path d="M3 6h18" />
			<path d="M8 6V4h8v2" />
			<path d="M19 6l-1 14H6L5 6" />
			<path d="M10 11v6M14 11v6" />
		</svg>
	</button>
</form>
