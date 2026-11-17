@import java.util.List
@import net.luversof.web.dynamiccrud2.adapter.TableRef

@param List<TableRef> tables
@param String error = null

@if(error != null)
	<option value="">${error}</option>
@else
	<option value="">고르세요(${tables.size()}개)</option>
	@for(var table : tables)
		!{var value = (table.catalog() == null ? "" : table.catalog()) + "|" + (table.schema() == null ? "" : table.schema()) + "|" + table.name();}
		<option value="${value}">${table.schema() == null ? "" : table.schema() + "."}${table.name()}</option>
	@endfor
@endif
