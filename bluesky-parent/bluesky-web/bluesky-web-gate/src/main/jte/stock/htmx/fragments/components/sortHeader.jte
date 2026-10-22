<%-- 서버 정렬 표의 머리칸 하나: 누르면(htmx) 같은 목록을 그 열 기준으로 다시 받는다. 지금 정렬 열이면 다음은 반대 방향, 아니면 오름차순.
	 2026-10-22 까지 배당 상세 표(7 칸)와 매매 상세 표(5 칸)가 같은 th 를 칸마다 통째로 적고 있었다. 매매 쪽은 정렬 화살표를 낭독기에서
	 숨기지 않아("검은 아래쪽 삼각형") 배당 쪽(2026-09-30 에 고침)과 갈려 있었다 - 이제 둘 다 aria-sort 가 뜻을 전하고 화살표는 숨긴다. --%>
@param String field
@param String label
<%-- th 의 class 전체(정렬 커서 · 정렬 · 좁은 폭에서 숨김 · 인쇄 복원). 열마다 달라 부르는 쪽이 그대로 준다. --%>
@param String cellClass
@param String sortField
@param String sortDir
@param String contentUrl
@param String fragmentId
@param String formId

!{
	boolean active = sortField != null && sortField.equals(field);
	boolean ascending = "asc".equals(sortDir);
	String fragmentSelector = "#" + fragmentId;
	String formSelector = "#" + formId;
}
<th scope="col" class="${cellClass}" tabindex="0" aria-sort="${active ? (ascending ? "ascending" : "descending") : "none"}" hx-get="${contentUrl}" hx-target="${fragmentSelector}" hx-select="${fragmentSelector}" hx-swap="outerHTML" hx-include="${formSelector}"
	hx-vals='{"sort": "${field},${active && ascending ? "desc" : "asc"}"}'>
	${label} @if(active)<span aria-hidden="true">${ascending ? "▲" : "▼"}</span>@endif
</th>
