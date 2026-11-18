<%-- combobox 후보 한 줄(이름 + 작은 설명). js/ui.js 가 빼낸 항목을 되돌릴 때도 같은 모양으로 만든다 --%>
@param String value
@param String label
@param String sub = null

<li role="option" aria-selected="false" data-value="${value}" data-label="${label}">${label}@if(sub != null) <span class="combobox-sub">${sub}</span>@endif</li>
