<%-- 테이블 후보(_components/combobox 목록 칸). 값은 catalog|schema|name, 입력칸에서 거른다 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import java.util.List
@import net.luversof.web.dynamiccrud2.adapter.TableRef

@param List<TableRef> tables
@param String error = null

@if(error != null)
	<li aria-disabled="true">${error}</li>
@else
	@for(var table : tables)
		!{var value = (table.catalog() == null ? "" : table.catalog()) + "|" + (table.schema() == null ? "" : table.schema()) + "|" + table.name();}
		!{var label = (table.schema() == null ? "" : table.schema() + ".") + table.name();}
		<li role="option" aria-selected="false" data-value="${value}" data-label="${label}">${label}</li>
	@endfor
	@if(tables.isEmpty())
		<li aria-disabled="true">${t("admin.tables.empty")}</li>
	@endif
@endif
