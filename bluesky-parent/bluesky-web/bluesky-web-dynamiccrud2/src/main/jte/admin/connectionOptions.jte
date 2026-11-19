<%-- connection 검색 후보(_components/combobox 목록 칸에 들어간다) --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import java.util.List

@param List<String> names

@for(var name : names)
	<li role="option" aria-selected="false" data-value="${name}">${name}</li>
@endfor
@if(names.isEmpty())
	<li aria-disabled="true">${t("admin.menuCreate.connEmpty")}</li>
@endif
