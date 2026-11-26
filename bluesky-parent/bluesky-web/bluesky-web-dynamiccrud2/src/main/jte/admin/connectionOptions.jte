<%-- connection 검색 후보(_components/combobox 목록 칸에 들어간다). 이름 옆 흐린 글자 = 읽은 출처(connectionInfo reader key, 예 postgresql-datasource),
     고르면 입력칸엔 이름만(data-label). DMT2 UDP-6560 의 출처(mongodb · dbconn) 표시와 같은 자리 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import java.util.List
@import net.luversof.web.dynamiccrud2.connection.ConnectionNameService.ConnectionName

@param List<ConnectionName> names

@for(var name : names)
	@template._components.pickOption(value = name.name(), label = name.name(), sub = name.source())
@endfor
@if(names.isEmpty())
	<li aria-disabled="true">${t("admin.menuCreate.connEmpty")}</li>
@endif
