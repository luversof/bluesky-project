<%-- 고른 값 하나를 보이는 작은 칩([이름 ×]). 고르지 않은 것도 숨긴 채(입력 disabled) 그려 두고 "+ 추가" 로 고르면 보인다
     (js/ui.js picklist). 폼 값: name = key(화면 순서대로) --%>
@param String key
@param String label
@param String name
@param boolean picked

<span class="chip" data-pick-key="${key}" data-pick-label="${label}" hidden="${!picked}">
	<input type="hidden" name="${name}" value="${key}" disabled="${!picked}" />
	<span class="truncate">${label}</span>
	<button type="button" class="chip-remove" data-pick-remove aria-label="${label} 빼기" title="빼기">×</button>
</span>
