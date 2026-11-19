<%-- 화면 안 탭(주소가 바뀌는 링크 묶음, 지금 탭은 aria-current). 모양은 상단 앱 이동(.app-nav)과 같은 묶은 단추 --%>
@import java.util.List
@import net.luversof.web.dynamiccrud2.admin.controller.Crumb

@param List<Crumb> items
@param int current
@param String label
@param String cssClass = "mb-4"

<nav class="app-nav w-fit ${cssClass}" aria-label="${label}">
	@for(var i = 0; i < items.size(); i++)
		<a href="${items.get(i).href()}" class="app-nav-link" aria-current="${i == current ? "page" : null}">${items.get(i).label()}</a>
	@endfor
</nav>
