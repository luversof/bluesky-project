<%-- 비었을 때 안내 한 줄. 표 안에서는 colspan 을 주면 행으로 낸다 --%>
@param String message
@param int colspan = 0
@param String cssClass = ""

@if(colspan > 0)
	<tr><td class="hint py-6 ${cssClass}" colspan="${colspan}">${message}</td></tr>
@else
	<p class="hint ${cssClass}">${message}</p>
@endif
