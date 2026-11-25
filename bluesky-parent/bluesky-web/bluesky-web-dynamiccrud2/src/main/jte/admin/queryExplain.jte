<%-- 쿼리 화면 "실행 계획" 결과(htmx 조각, admin/queryEditor 의 #q-explain). 실행하지 않고 본 계획 - 인덱스를 타는지(Index Scan · key) 확인용 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import net.luversof.web.dynamiccrud2.admin.service.QueryService.ExplainResult

@param ExplainResult explain

@template._components.alert(message = explain.error(), cssClass = "")
@if(explain.error() == null)
	<div class="grid gap-2">
		<h3 class="font-semibold">${t("inspect.explain.title")}</h3>
		@if(explain.approximate())
			@template._components.alert(message = t("inspect.explain.approximate"), tone = "info", cssClass = "")
		@endif
		<pre class="code text-xs overflow-x-auto whitespace-pre p-3 rounded-[var(--radius-field)] bg-base-200">@for(var line : explain.lines())${line}
@endfor</pre>
	</div>
@endif
