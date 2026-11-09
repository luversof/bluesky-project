@import gg.jte.Content
@param String text
@param Content content
@param Content detail = null
@param String placement = "bottom"
@param boolean focusable = true
@param String cssClass = ""
!{
	// 화면의 모든 툴팁은 이 조각 하나로 그린다(사용자 요청 2026-11-10: 화면마다 제각각이던 툴팁 통합). 모양 · 여닫기 · 끝 맞춤 · Esc 닫기는
	// CSS .tooltip(main.css)과 common.ts 가 한곳에서 맡는다. 기존 요소에 설명만 붙일 때(옛 title 자리)는 그 요소에 class="tooltip" + data-tip 을 단다 - 같은 CSS 다(TooltipConsistencyTest).
	// text: 말풍선 문구이자 낭독기 문구. detail 을 주면 여러 줄 내용 상자(tooltip-content)를 띄우고 text 는 낭독기 이름으로만 쓴다.
	// placement: "bottom"(기본) | "top" - 가로 스크롤 표의 본문 칸은 "top" 으로 둔다(아래로 펴면 마지막 줄에서 상자에 잘린다).
	// focusable=false: 트리거 안에 링크처럼 초점을 받는 요소가 있을 때 - 탭 정지가 겹치지 않게 감싼 칸은 초점을 받지 않고, 안쪽 초점에서 연다.
	// 이 파일에는 빈 줄도, 태그 밖 글자도 두면 안 된다(srExact 와 같은 까닭 - 호출한 자리에 개행이 끼어든다).
}
<span class="tooltip inline-block${"top".equals(placement) ? "" : " tooltip-bottom"}${cssClass.isEmpty() ? "" : " " + cssClass}" role="${focusable ? "note" : null}" tabindex="${focusable ? "0" : null}" aria-label="${focusable ? text : null}" data-tip="${detail == null ? text : null}" data-tooltip-inner-focus="${focusable ? null : "true"}">@if(detail != null)<span class="tooltip-content">${detail}</span>@endif${content}</span>