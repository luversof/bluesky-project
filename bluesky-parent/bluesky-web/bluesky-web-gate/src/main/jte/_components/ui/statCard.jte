@param String label
@param String value
@param String valueTitle = ""
@param String sub = ""
@param String tone = ""
@param boolean amount = true
@param String href = null
@param String icon = ""
@param String accent = "primary"

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
		<span class="stat-card-label">${label}</span>
		@if(href != null)
		<span class="stat-card-go" aria-hidden="true">→</span>
		@endif
	</div>
	<div class="stat-card-value ${amount ? "amount-value" : ""} ${toneClass}" title="${valueTitle}">${value}</div>
	@if(sub != null && !sub.isEmpty())
	<div class="stat-card-sub ${subClass}">${sub}</div>
	@endif
	@if(href != null)
	<a href="${href}" class="stat-card-cover"><span class="sr-only">${label}</span></a>
	@endif
</div>
