@import java.util.List
@import net.luversof.web.dynamiccrud2.setting.domain.DbField
@import net.luversof.web.dynamiccrud2.setting.domain.DbTable
@import net.luversof.web.dynamiccrud2.setting.domain.InputType

@param DbTable dbTable
@param List<DbField> fields
@param InputType[] inputTypes

!{var tableId = dbTable.getId().toString();}

@template._layout.layout(title = "필드: " + dbTable.getTableName(), content = @`
	<p class="mb-4 text-sm"><a class="hover:underline" href="/setting">← 설정</a></p>
	<div class="card mb-4 flex flex-wrap items-center gap-3">
		<span class="text-sm">${dbTable.getConnectionName()} · ${dbTable.getSchemaName() == null ? "" : dbTable.getSchemaName() + "."}${dbTable.getTableName()}</span>
		<span class="hint">이 테이블을 쓰는 모든 메뉴가 이 필드 설정을 같이 씁니다. 마지막으로 읽은 시각: ${dbTable.getColumnsReadDate() == null ? "-" : dbTable.getColumnsReadDate().toString()}</span>
		<form method="post" action="/setting/table/${tableId}/resync" class="ml-auto">
			<button class="btn btn-sm">컬럼 다시 읽기</button>
		</form>
	</div>

	<div class="overflow-x-auto">
		<table class="table">
			<thead>
				<tr><th>컬럼</th><th>DB 타입 · 주석</th><th>라벨</th><th>입력 형식</th><th>필수</th><th>최대 길이</th><th>정렬</th><th></th></tr>
			</thead>
			<tbody>
				@for(var field : fields)
					!{var formId = "f-" + field.getColumnName().hashCode();}
					<tr>
						<td>
							<span class="font-mono">${field.getColumnName()}</span>
							@if(field.isVirtual())<span class="badge ml-1">조회 전용</span>@endif
							@if(field.isMissingInDb())<span class="badge badge-warning ml-1">DB에 없음</span>@endif
							@if(field.isAutoIncrement())<span class="badge ml-1">자동 증가</span>@endif
							<form id="${formId}" method="post" action="/setting/table/${tableId}/field">
								<input type="hidden" name="columnName" value="${field.getColumnName()}" />
							</form>
						</td>
						<td class="text-xs">${field.getDataType()}<br /><span class="hint">${field.getDbComment()}</span></td>
						<td><input class="input input-sm w-40" form="${formId}" name="label" value="${field.getLabel()}" placeholder="${field.getDbComment() == null ? field.getColumnName() : field.getDbComment()}" aria-label="${field.getColumnName()} 라벨" /></td>
						<td>
							<select class="select select-sm" form="${formId}" name="inputType" aria-label="${field.getColumnName()} 입력 형식">
								@for(var type : inputTypes)
									<option value="${type.name()}" selected="${type == field.getInputType()}">${type.name()}</option>
								@endfor
							</select>
						</td>
						<td><input class="checkbox" type="checkbox" form="${formId}" name="required" value="true" checked="${field.isRequired()}" aria-label="${field.getColumnName()} 필수" /></td>
						<td><input class="input input-sm w-24" type="number" min="1" form="${formId}" name="maxLength" value="${field.getMaxLength()}" aria-label="${field.getColumnName()} 최대 길이" /></td>
						<td>
							<input class="checkbox" type="checkbox" form="${formId}" name="sortable" value="true" checked="${field.isSortable()}" disabled="${field.isVirtual()}" aria-label="${field.getColumnName()} 정렬 허용" />
							@if(field.isSortableUserSet())<span class="hint">직접 지정</span>@endif
						</td>
						<td><button class="btn btn-sm" form="${formId}">저장</button></td>
					</tr>
				@endfor
			</tbody>
		</table>
	</div>
	<p class="hint mt-3">정렬은 처음에 인덱스 첫 컬럼만 허용합니다. 인덱스 없는 컬럼 정렬은 큰 테이블에서 느립니다.</p>
`)
