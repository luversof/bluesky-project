@param String name = ""

<%-- 지표 카드 앞머리 아이콘. 24x24 stroke 아이콘만 쓴다(선 굵기 1.8, currentColor).
     이름을 모르면 아무것도 내지 않는다 - 빈 네모가 남는 것보다 없는 편이 낫다. --%>
@if("trend-up".equals(name))
	<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 17l6-6 4 4 7-7"/><path d="M14 8h6v6"/></svg>
@elseif("exchange".equals(name))
	<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 8h13"/><path d="M14 5l3 3-3 3"/><path d="M20 16H7"/><path d="M10 13l-3 3 3 3"/></svg>
@elseif("gift".equals(name))
	<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="9" width="18" height="11" rx="2"/><path d="M3 13h18"/><path d="M12 9v11"/><path d="M12 9S9.5 4.5 7.5 5.5 9 9 12 9zM12 9s2.5-4.5 4.5-3.5S15 9 12 9z"/></svg>
@elseif("target".equals(name))
	<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="8"/><circle cx="12" cy="12" r="4"/><circle cx="12" cy="12" r="1"/></svg>
@elseif("wallet".equals(name))
	<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="6" width="18" height="13" rx="2"/><path d="M3 10h18"/><circle cx="17" cy="14" r="1.2"/></svg>
@endif
