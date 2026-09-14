@param String text = ""
@param String visible = ""
!{
	// 화면 값이 축약(9억 9,117만)이거나 근거 설명(43종목 중 35종목이 수익)일 때, 정확값·근거를 보조기술에만 덧붙인다.
	// 2026-09-10 실측: 이 정보들이 title 속성에만 있었고, title 은 role 없는 요소에서 접근성 트리에 올라가지 않아
	// 마우스 호버 전용이었다(주식 화면 6개에서 22건).
	// 단, 화면 값이 이미 전체 자릿수면(배당 달력 탭 24건 실측) 덧붙이는 순간 같은 금액을 두 번 읽히는 순손해가 된다.
	// 그래서 사이트 단위가 아니라 값 단위로 판단한다 - 숫자가 같으면 내보내지 않는다.
	// 화면 값은 절대 aria-hidden 하지 않는다. text 가 대체가 아니라 보충인 경우(승률)가 있어 가리면 값이 사라진다.
	// 이 파일에는 빈 줄도, @if 밖의 글자도 두면 안 된다. JTE 는 그것을 그대로 출력해 호출한 자리의 숫자 뒤에
	// 개행을 끼워 넣는다 - 실측 2026-09-10: DividendCalendarSubtotalTest 의 ">숫자<" 매칭이 깨져 행 합계가 0 이 됐다.
	String srText = text == null ? "" : text;
	String srVisible = visible == null ? "" : visible;
	String textDigits = srText.replaceAll("[^0-9]", "");
	String visibleDigits = srVisible.replaceAll("[^0-9]", "");
	boolean redundant = !textDigits.isEmpty() && textDigits.equals(visibleDigits);
}
@if(!srText.isEmpty() && !redundant)<span class="sr-only"> ${srText}</span>@endif