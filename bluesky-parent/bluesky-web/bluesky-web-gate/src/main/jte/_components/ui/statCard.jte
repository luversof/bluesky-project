@param String label
@param String value
@param String valueTitle = ""
@param String sub = ""
<%-- 보조줄에 금액이 섞일 때 쓴다. sub 는 통째로 글자라 금액 숨김이 걸리지 않는다
     - 실측 2026-09-11: 대시보드 "매도 9억 9,999만 · 5건" 이 숨김을 켜도 그대로 보였다.
     앞/뒤 글자는 그대로 두고 가운데 금액만 amount-value 로 감싼다. --%>
@param String subBefore = ""
@param String subAmount = ""
@param String subAfter = ""
<%-- 보조줄 끝에 날짜가 붙을 때 쓴다 - 날짜만 한 덩어리로 묶어 "2025-09-/18" 처럼 하이픈에서 갈리지 않게(사용자 요청 2026-09-30
     "날짜 줄바꿈도 날짜만 묶어서"). sub 뒤에 subDate · subDateAfter 가 이어 붙는다. --%>
@param String subDate = ""
@param String subDateAfter = ""
@param String tone = ""
@param boolean amount = true
@param String href = null
@param String icon = ""
@param String accent = "primary"
<%-- 같은 라벨의 카드가 한 화면에 둘 있을 때 범위를 밝힌다(화면에는 구역 제목으로 이미 나뉘어 있다).
     실측 2026-09-11: 대시보드에 "실현 손익" 카드가 둘(전체 225,630,135 · 올해 131,261,409)인데
     라벨 글자가 같아 낭독기로는 구분되지 않았고, 카드를 덮는 링크 이름도 둘 다 "실현 손익" 이었다
     (목적지는 rangeMode=all 과 ytd 로 다르다). --%>
@param String scopeNote = ""

<%-- 지표 카드. 2026-09-10 까지는 회색 상자에 작은 글자 두 줄이었다(라벨 12px, 값 18px, 배경 base-200 한 가지)
     - 카드마다 성격이 달라도 똑같이 보여 화면이 밋밋했다. 앞머리에 성격을 나타내는 아이콘 칩을 두고 값을 키웠다.
     href 가 있으면 카드 전체를 덮는 링크를 깔아 카드 어디를 눌러도 상세로 간다(JTE 는 여는 태그와 닫는 태그를
     @if 로 가를 수 없어 <a> 로 감싸는 대신 덮개를 쓴다). --%>
!{
	String toneClass = tone == null ? "" : tone;
	String subClass = toneClass.isEmpty() ? "text-base-content/60" : toneClass;
	String chip = "stat-chip stat-chip-" + (accent == null || accent.isEmpty() ? "primary" : accent);
}

<div class="stat-card${href != null ? " stat-card-link" : ""}">
	<div class="stat-card-head">
		@if(icon != null && !icon.isEmpty())
		<span class="${chip}">@template._components.ui.statIcon(name = icon)</span>
		@endif
		<span class="stat-card-label">${label}@if(scopeNote != null && !scopeNote.isEmpty())<span class="sr-only"> ${scopeNote}</span>@endif</span>
		@if(href != null)
		<span class="stat-card-go" aria-hidden="true">→</span>
		@endif
	</div>
	<%-- valueTitle 은 2026-09-10 까지 title 속성에만 있었다. title 은 role 없는 div 에서 접근성 트리에 노출되지
	     않아 마우스 호버 전용이었다. 판단 규칙은 srExact 가 갖는다 - 화면 값과 숫자가 같으면 내보내지 않는다. --%>
	<div class="stat-card-value ${amount ? "amount-value" : ""} ${toneClass}" title="${valueTitle}">${value}@template._components.ui.srExact(text = valueTitle, visible = value)</div>
	<%-- sub 와 금액 줄을 함께 넘기면 두 줄이다 - 계좌 상세 실현 손익이 "+5.3% · 판 주식 원가 대비" 와 "이 계좌 기준 +2,063,739 · +33.6%" 를
	     함께 적는다(사용자 선택 2026-09-17). 한 알약에 이으면 두 비율이 어느 금액의 것인지 갈리지 않는다. 하나만 넘기면 예전과 같다. --%>
	@if((sub != null && !sub.isEmpty()) || (subDate != null && !subDate.isEmpty()))
	<div class="stat-card-sub ${subClass}">${sub}@if(subDate != null && !subDate.isEmpty())<span class="whitespace-nowrap date-whole">${subDate}</span>${subDateAfter}@endif</div>
	@endif
	@if(subAmount != null && !subAmount.isEmpty())
	<div class="stat-card-sub ${subClass}">${subBefore}<span class="amount-value">${subAmount}</span>${subAfter}</div>
	@endif
	@if(href != null)
	<a href="${href}" class="stat-card-cover"><span class="sr-only">${label}${scopeNote != null && !scopeNote.isEmpty() ? " " + scopeNote : ""}</span></a>
	@endif
</div>
