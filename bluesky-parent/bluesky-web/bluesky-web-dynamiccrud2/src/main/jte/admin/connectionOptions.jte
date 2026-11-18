<%-- connection 검색 후보(_components/combobox 목록 칸에 들어간다) --%>
@import java.util.List

@param List<String> names

@for(var name : names)
	<li role="option" aria-selected="false" data-value="${name}">${name}</li>
@endfor
@if(names.isEmpty())
	<li aria-disabled="true">이름에 그 글자가 든 connection 이 없습니다</li>
@endif
