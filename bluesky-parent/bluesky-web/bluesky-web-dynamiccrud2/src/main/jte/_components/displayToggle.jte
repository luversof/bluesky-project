<%-- 사용 화면 메뉴 목록 노출 켜고 끄기(프로젝트 · 메인 메뉴 이름 옆). 아이콘은 지금 상태(눈 = 보임, 빗금 눈 = 숨김)이고 누르면 반대로 바뀐다.
     상태는 아이콘으로 충분해 "숨김" 배지는 두지 않는다(사용자 의견). 이모지(👁)는 컬러로 그려져 테마 · 다른 아이콘 색과 안 맞아 currentColor SVG 로 그린다.
     action 은 display=true|false 를 받는 POST 주소. back 이 있으면 끝나고 그 프로젝트 화면으로 돌아간다(왼쪽 프로젝트 목록에서 다른 프로젝트를 껐다 켜도 보던 프로젝트 그대로) --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@param String action
@param String name
@param boolean shown
@param String back = null

<%-- is-hidden-state: 숨김 상태 표시는 줄에 올리지 않아도 늘 보인다(main.css .reveal-icons) --%>
<form method="post" action="${action}" class="inline-flex ${shown ? "" : "is-hidden-state"}">
	<%-- boolean 을 그대로 넣으면 JTE 가 불리언 속성으로 그려 값이 비므로 문자열로 --%>
	<input type="hidden" name="display" value="${String.valueOf(!shown)}" />
	@if(back != null)<input type="hidden" name="back" value="${back}" />@endif
	<button class="icon-btn" aria-label="${shown ? t("display.hide", name) : t("display.show", name)}"
		data-tip="${shown ? t("display.hide.title") : t("display.show.title")}">
		@template._components.eyeIcon(slashed = !shown)
	</button>
</form>
