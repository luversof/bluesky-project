<%-- 사용 화면 메뉴 목록 노출 켜고 끄기(프로젝트 · 메인 메뉴 이름 옆). 아이콘은 지금 상태(눈 = 보임, 빗금 눈 = 숨김)이고 누르면 반대로 바뀐다.
     상태는 아이콘으로 충분해 "숨김" 배지는 두지 않는다(사용자 의견). 이모지(👁)는 컬러로 그려져 테마 · 다른 아이콘 색과 안 맞아 currentColor SVG 로 그린다.
     action 은 display=true|false 를 받는 POST 주소 --%>
@param String action
@param String name
@param boolean shown

<form method="post" action="${action}" class="inline-flex">
	<%-- boolean 을 그대로 넣으면 JTE 가 불리언 속성으로 그려 값이 비므로 문자열로 --%>
	<input type="hidden" name="display" value="${String.valueOf(!shown)}" />
	<button class="icon-btn" aria-label="${name} ${shown ? "메뉴 목록에서 숨기기" : "메뉴 목록에 다시 보이기"}"
		title="${shown ? "보이는 중 - 누르면 사용 화면 메뉴 목록에서 숨깁니다(주소로는 열림)" : "숨김 - 누르면 사용 화면 메뉴 목록에 다시 보입니다"}">
		@template._components.eyeIcon(slashed = !shown)
	</button>
</form>
