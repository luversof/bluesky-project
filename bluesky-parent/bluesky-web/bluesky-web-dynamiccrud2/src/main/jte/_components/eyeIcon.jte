<%-- 노출 상태 눈 그림(currentColor SVG): 보임 = 눈, 숨김(slashed) = 빗금 눈. 단추(displayToggle) 안 그림과
     단추가 없는 자리(왼쪽 프로젝트 목록 · 메뉴 줄)의 "숨김" 표시가 같이 쓴다. label 이 있으면 그림 자체가 읽히는 이름을 가진다 --%>
@param boolean slashed
@param String label = null

<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
	role="${label == null ? null : "img"}" aria-label="${label}" aria-hidden="${label == null ? "true" : null}" class="shrink-0">
	@if(label != null)<title>${label}</title>@endif
	<path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z" />
	<circle cx="12" cy="12" r="3" />
	@if(slashed)<line x1="3" y1="3" x2="21" y2="21" />@endif
</svg>
