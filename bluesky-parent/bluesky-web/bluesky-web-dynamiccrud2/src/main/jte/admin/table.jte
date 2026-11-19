@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import java.util.List
@import net.luversof.web.dynamiccrud2.domain.DbField
@import net.luversof.web.dynamiccrud2.domain.DbTable
@import net.luversof.web.dynamiccrud2.domain.InputType

@param DbTable dbTable
@param List<DbField> fields
@param InputType[] inputTypes
@param java.util.UUID menuId = null
@param net.luversof.web.dynamiccrud2.admin.service.SearchService.TableIndexes indexes
@param java.util.Set<String> indexLeading = null
@param String databaseType
@param String targetNoun = net.luversof.web.dynamiccrud2.config.Msg.t("target.table")
@param java.util.List<net.luversof.web.dynamiccrud2.admin.controller.Crumb> breadcrumb = null

!{var tableId = dbTable.getId().toString();}
!{var menuQuery = menuId == null ? "" : "?menu=" + menuId;}

@template._layout.layout(title = t("admin.table.title", targetNoun, dbTable.getTableName()), breadcrumb = breadcrumb, content = @`
	<%-- 정렬 허용을 정할 때 보도록 필드 표 위에 둔다(메뉴 관리 화면과 같은 표) --%>
	@template._components.card(title = t("index.tableLabel", targetNoun), help = t("admin.table.index.help"), body = @`
		@template._components.indexTable(table = indexes)
	`)
	<%-- 열 설명은 표 머리의 ⓘ(설명은 아래로 펼쳐져 행 위에 뜬다, 오른쪽 열은 왼쪽으로 - help alignEnd). 카드 도움말은 공통 설명만.
	     "필수" 열은 없다 - 입력 필수는 작업마다 쿼리 화면 파라미터에서 정한다(2026-11-19 사용자 결정) --%>
	@template._components.card(title = t("admin.table.fields"), help = t("admin.table.fields.help", targetNoun), actions = @`
		<span class="hint">${t("admin.table.readAt", dbTable.getColumnsReadDate() == null ? "-" : dbTable.getColumnsReadDate().toString())}</span>
		<form method="post" action="/admin/table/${tableId}/resync${menuQuery}">
			<button class="btn btn-sm">${t("admin.table.resync")}</button>
		</form>
	`, body = @`
	@template._components.sourceInfo(dbTable = dbTable, databaseType = databaseType, targetNoun = targetNoun, cssClass = "mb-3 pb-3 border-b border-base-300")
	<div class="overflow-x-auto">
		<table class="table" aria-label="${t("admin.table.fields")}">
			<thead>
				<tr>
					<th>${t("admin.table.col.column")}</th><th>${t("admin.table.col.dbType")}</th>
					@for(var col : java.util.List.of("label", "inputType", "maxLength", "sort", "update"))
						<th class="whitespace-nowrap">${t("admin.table.col." + col)}
							@template._components.help(text = t("admin.table.col." + col + ".help"), label = t("admin.table.col.helpLabel", t("admin.table.col." + col)),
								alignEnd = col.equals("sort") || col.equals("update"))</th>
					@endfor
					<th></th>
				</tr>
			</thead>
			<tbody>
				@for(var field : fields)
					!{var formId = "f-" + field.getColumnName().hashCode();}
					<tr>
						<td>
							<span class="font-mono">${field.getColumnName()}</span>
							@if(field.isVirtual())<span class="badge ml-1">${t("admin.table.virtual")}</span>@endif
							@if(field.isMissingInDb())<span class="badge badge-warning ml-1">${t("admin.table.missing")}</span>@endif
							@if(field.isAutoIncrement())<span class="badge ml-1">${t("admin.table.autoIncrement")}</span>@endif
							<form id="${formId}" method="post" action="/admin/table/${tableId}/field${menuQuery}">
								<input type="hidden" name="columnName" value="${field.getColumnName()}" />
							</form>
						</td>
						<td class="text-xs">${field.getDataType()}<br /><span class="hint">${field.getDbComment()}</span></td>
						<td><input class="input input-sm w-40" form="${formId}" name="label" value="${field.getLabel()}" placeholder="${field.getDbComment() == null ? field.getColumnName() : field.getDbComment()}" aria-label="${t("admin.table.aria.label", field.getColumnName())}" /></td>
						<td>
							<select class="select select-sm" form="${formId}" name="inputType" aria-label="${t("admin.table.aria.inputType", field.getColumnName())}">
								@for(var type : inputTypes)
									<option value="${type.name()}" selected="${type == field.getInputType()}">${type.name()}</option>
								@endfor
							</select>
						</td>
						<td><input class="input input-sm w-24" type="number" min="1" form="${formId}" name="maxLength" value="${field.getMaxLength()}" aria-label="${t("admin.table.aria.maxLength", field.getColumnName())}" /></td>
						<td>
							<%-- 체크하면(정렬 허용) 인덱스를 쓰는지 보인다(.sort-cell:has(:checked), CSS 만) --%>
							<div class="sort-cell flex items-center gap-1">
								<input class="checkbox" type="checkbox" form="${formId}" name="sortable" value="true" checked="${field.isSortable()}" disabled="${field.isVirtual()}" aria-label="${t("admin.table.aria.sort", field.getColumnName())}" />
								@if(indexLeading != null && !field.isVirtual())
									@if(indexLeading.contains(field.getColumnName()))
										<span class="index-badge index-ok sort-index whitespace-nowrap" title="${t("admin.table.sortIndex.ok.title")}">✓ ${t("admin.table.sortIndex.ok")}</span>
									@else
										<span class="index-badge index-warn sort-index whitespace-nowrap" title="${t("admin.table.sortIndex.warn.title")}">⚠ ${t("admin.table.sortIndex.warn")}</span>
									@endif
								@endif
								@if(field.isSortableUserSet())<span class="hint whitespace-nowrap">${t("admin.table.userSet")}</span>@endif
							</div>
						</td>
						<td>
							<input class="checkbox" type="checkbox" form="${formId}" name="updatable" value="true" checked="${field.isUpdatable()}" disabled="${field.isVirtual()}" aria-label="${t("admin.table.aria.update", field.getColumnName())}" />
							@if(field.isUpdatableUserSet())<span class="hint">${t("admin.table.userSet")}</span>@endif
						</td>
						<td><button class="btn btn-sm" form="${formId}">${t("common.save")}</button></td>
					</tr>
				@endfor
			</tbody>
		</table>
	</div>
	`)
`)
