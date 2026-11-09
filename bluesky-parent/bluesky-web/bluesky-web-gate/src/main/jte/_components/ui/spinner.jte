@param String size = "lg"
@param boolean progress = false
!{
	// 로딩 표시는 이 조각 하나로 그린다(2026-11-10 UI 통합 - 화면마다 loading-spinner 크기 · 색을 따로 골라 4 가지였다).
	// size: "lg"(화면 · 구역을 채우는 로딩, 기본) | "sm"(줄 안 · 단추 옆). 색은 늘 primary.
	// progress=true: 관리 화면 진행 표시줄 옆 스피너 - adminActions 스크립트가 data-progress-spinner 로 찾는다.
	// 이 파일에는 빈 줄도, 태그 밖 글자도 두면 안 된다(srExact 와 같은 까닭 - 호출한 자리에 개행이 끼어든다).
}
<span class="loading loading-spinner text-primary ${"sm".equals(size) ? "loading-sm" : "loading-lg"}" data-progress-spinner="${progress ? "" : null}"></span>