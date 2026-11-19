@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import net.luversof.web.dynamiccrud2.domain.ParamSetting.Role
@import net.luversof.web.dynamiccrud2.domain.QueryKind
@import net.luversof.web.dynamiccrud2.admin.service.QueryService.QueryInspection

@param QueryKind kind
@param QueryInspection inspection
@param java.util.UUID menuId = null

@template._components.alert(message = inspection.error(), cssClass = "mb-3")
@template._components.alert(message = inspection.passed() == null ? null : "✓ " + inspection.passed(), tone = "success", cssClass = "mb-3")
@for(var warning : inspection.warnings())
	@template._components.alert(message = warning, tone = "warning", cssClass = "mb-2")
@endfor

@if(!inspection.params().isEmpty())
	<h3 class="font-semibold mt-4 mb-2">${t("admin.inspect.params")}</h3>
	<table class="table" aria-label="${t("admin.inspect.params")}">
		<thead><tr><th>${t("field.name")}</th><th>${t("admin.inspect.col.role")}</th><th>${t("admin.table.col.required")}</th><th>${t("admin.inspect.col.default")}</th><th>${t("admin.inspect.col.condition")}</th></tr></thead>
		<tbody>
			@for(var param : inspection.params())
				!{var prefix = "p." + param.name();}
				!{var locked = kind == QueryKind.READ && !param.optionalCapable();}
				<tr>
					<td class="font-mono">${param.name()}</td>
					<td>
						<select class="select select-sm" name="${prefix}.role" aria-label="${t("admin.inspect.aria.role", param.name())}">
							@for(var role : Role.values())
								<option value="${role.name()}" selected="${param.setting().role() == role}">
									${t("admin.inspect.role." + role.name())}
								</option>
							@endfor
						</select>
					</td>
					<td>
						@if(locked)
							<input type="hidden" name="${prefix}.required" value="true" />
							<input class="checkbox" type="checkbox" checked disabled aria-label="${t("admin.inspect.aria.requiredLocked", param.name())}" title="${t("admin.inspect.requiredLocked.title")}" />
						@else
							<input class="checkbox" type="checkbox" name="${prefix}.required" value="true" checked="${param.setting().required()}" aria-label="${t("admin.table.aria.required", param.name())}" />
						@endif
					</td>
					<td><input class="input input-sm w-36" name="${prefix}.defaultValue" value="${param.setting().defaultValue()}" aria-label="${t("admin.inspect.aria.default", param.name())}" /></td>
					<td class="font-mono text-xs">${param.expression()}</td>
				</tr>
			@endfor
		</tbody>
	</table>
@endif

@if(!inspection.columns().isEmpty())
	<%-- 쿼리는 무엇을 가져올지, 필드는 어떻게 보일지: 결과 컬럼마다 사용 화면에서 보일 제목 · 형식 · 정렬(테이블 필드 관리 설정).
	     필드 설정은 테이블 단위라 줄마다가 아니라 제목 오른쪽 단추 하나(조인해 테이블이 여럿이면 테이블마다) - 사용자 결정 --%>
	!{var fieldTables = inspection.fieldTables();}
	<div class="flex flex-wrap items-center gap-2 mt-4 mb-2">
		<h3 class="font-semibold">${t("admin.inspect.columns")}</h3>
		@if(menuId != null)
			<span class="ml-auto flex flex-wrap gap-2">
				@for(var table : fieldTables)
					<a class="btn btn-sm" href="/admin/table/${table.id().toString()}?menu=${menuId.toString()}">${t("admin.table.name", t("target.table"))}${fieldTables.size() > 1 ? ": " + table.name() : ""}</a>
				@endfor
			</span>
		@endif
	</div>
	<p class="hint mb-2">${t("admin.inspect.columns.hint")}</p>
	<div class="overflow-x-auto">
	<table class="table" aria-label="${t("admin.inspect.columns")}">
		<thead><tr>
			<th>${t("admin.inspect.col.result")}</th><th>${t("admin.inspect.col.source")}</th><th>${t("index.col.kind")}</th>
			<th>${t("admin.inspect.col.header")}</th><th>${t("admin.table.col.inputType")}</th><th>${t("admin.table.col.sort")}</th>
		</tr></thead>
		<tbody>
			@for(var column : inspection.columns())
				!{var whole = column.kind() == net.luversof.web.dynamiccrud2.adapter.ReadAnalysis.ResultColumn.Kind.ALL || column.kind() == net.luversof.web.dynamiccrud2.adapter.ReadAnalysis.ResultColumn.Kind.ALL_TABLE;}
				!{var view = whole ? null : inspection.viewOf(column.label());}
				<tr>
					<td>${column.label()}</td>
					<td class="font-mono text-xs">${column.tableName() == null ? "-" : column.tableName() + "." + (column.columnName() == null ? "*" : column.columnName())}</td>
					<td>${t("admin.inspect.kind." + column.kind().name())}</td>
					@if(view == null)
						<td colspan="3" class="hint">${t("admin.inspect.view.whole")}</td>
					@else
						<td>
							${view.header()}
							@if(view.labelOverAlias())<span class="hint block text-xs">${t("admin.inspect.view.labelOverAlias", view.alias())}</span>@endif
							@if(view.tableId() == null)<span class="hint block text-xs">${t("admin.inspect.view.unregistered")}</span>@endif
						</td>
						<td class="font-mono text-xs">${view.inputType().name()}</td>
						<td>${view.sortable() ? t("common.yes") : t("common.no")}</td>
					@endif
				</tr>
			@endfor
		</tbody>
	</table>
	</div>
@endif

<%-- 조회 미리 보기: 시험 실행한 앞 몇 행(선택 조건은 빼고, 필수 파라미터는 값 없이). 저장 전에 결과 모양을 본다 --%>
@if(inspection.preview() != null)
	!{var preview = inspection.preview();}
	<h3 class="font-semibold mt-4 mb-2">${t("admin.inspect.preview")}</h3>
	<p class="hint mb-2">
		${t("admin.inspect.preview.hint", preview.rows().size())}
		@if(inspection.previewWithoutValues()) ${t("admin.inspect.preview.required")}@endif
	</p>
	@if(preview.rows().isEmpty())
		@template._components.emptyState(message = t("admin.inspect.preview.empty"))
	@else
		<div class="overflow-x-auto">
			<table class="table" aria-label="${t("admin.inspect.preview")}">
				<%-- 사용 화면과 같은 제목 · 표시 형식(_components/cellValue) --%>
				<thead><tr>@for(var label : preview.labels())<th class="whitespace-nowrap">${inspection.viewOf(label).header()}</th>@endfor</tr></thead>
				<tbody>
					@for(var row : preview.rows())
						<tr>
							@for(var i = 0; i < row.size(); i++)
								!{var value = row.get(i);}
								@template._components.cellValue(value = value == null ? null : net.luversof.web.dynamiccrud2.menu.service.ValueConverter.toText(value),
									inputType = inspection.viewOf(preview.labels().get(i)).inputType(), nullMark = true)
							@endfor
						</tr>
					@endfor
				</tbody>
			</table>
		</div>
	@endif
@endif
