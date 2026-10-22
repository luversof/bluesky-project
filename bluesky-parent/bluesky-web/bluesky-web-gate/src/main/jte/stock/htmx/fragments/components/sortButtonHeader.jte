<%-- 브라우저 정렬 표의 머리칸 하나: 누르면 assetStatus.ts · dividendHistory.ts 가 data-sort-key 열로 줄을 다시 세운다(서버 왕복 없음).
	 2026-10-22 까지 자산 현황 표(18 칸)와 배당 수익 분석 카드(8 칸)가 같은 th > button 을 칸마다 8 줄씩 적고 있었다.
	 내용이 단순 이름이 아닌 칸(용어 풀이 · 줄바꿈 규칙)은 그 자리에 그대로 둔다. --%>
@param String sortKey
<%-- "text" | "number" --%>
@param String sortType
@param String label
<%-- th 의 class(정렬 · 좁은 폭에서 숨김 등). null 이면 class 속성 자체가 빠진다. --%>
@param String cellClass = null
<%-- 버튼 class 전체. Tailwind 가 소스 글자로 모으므로 부르는 쪽이 통째로 준다. --%>
@param String buttonClass
<th scope="col" class="${cellClass}">
	<button type="button"
			class="${buttonClass}"
			data-sort-key="${sortKey}"
			data-sort-type="${sortType}">
		<span>${label}</span>
		<span data-sort-indicator aria-hidden="true" class="opacity-60">↕</span>
	</button>
</th>
