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
@param String targetNoun = "테이블"
@param java.util.List<net.luversof.web.dynamiccrud2.admin.controller.Crumb> breadcrumb = null

!{var tableId = dbTable.getId().toString();}
!{var menuQuery = menuId == null ? "" : "?menu=" + menuId;}

@template._layout.layout(title = targetNoun + " 필드 관리: " + dbTable.getTableName(), breadcrumb = breadcrumb, content = @`
	<%-- 정렬 허용을 정할 때 보도록 필드 표 위에 둔다(메뉴 관리 화면과 같은 표) --%>
	@template._components.card(title = targetNoun + " 인덱스", actions = @`
		<span class="hint">정렬은 인덱스의 첫 컬럼(굵게)일 때 인덱스를 씁니다. 실제 사용 여부는 DB 가 데이터 분포를 보고 정합니다.</span>
	`, body = @`
		@template._components.indexTable(table = indexes)
	`)
	@template._components.card(title = "필드", actions = @`
		<span class="hint">마지막으로 읽은 시각: ${dbTable.getColumnsReadDate() == null ? "-" : dbTable.getColumnsReadDate().toString()}</span>
		<form method="post" action="/admin/table/${tableId}/resync${menuQuery}">
			<button class="btn btn-sm">컬럼 다시 읽기</button>
		</form>
	`, body = @`
	@template._components.sourceInfo(dbTable = dbTable, databaseType = databaseType, targetNoun = targetNoun, cssClass = "mb-3 pb-3 border-b border-base-300")
	<p class="hint mb-2">이 테이블을 쓰는 모든 메뉴가 이 필드 설정을 같이 씁니다.</p>
	<div class="overflow-x-auto">
		<table class="table" aria-label="필드">
			<thead>
				<tr><th>컬럼</th><th>DB 타입 · 주석</th><th>라벨</th><th>입력 형식</th><th>필수</th><th>최대 길이</th><th>정렬</th><th>수정</th><th></th></tr>
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
							<form id="${formId}" method="post" action="/admin/table/${tableId}/field${menuQuery}">
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
							<%-- 체크하면(정렬 허용) 인덱스를 쓰는지 보인다(.sort-cell:has(:checked), CSS 만) --%>
							<div class="sort-cell flex items-center gap-1">
								<input class="checkbox" type="checkbox" form="${formId}" name="sortable" value="true" checked="${field.isSortable()}" disabled="${field.isVirtual()}" aria-label="${field.getColumnName()} 정렬 허용" />
								@if(indexLeading != null && !field.isVirtual())
									@if(indexLeading.contains(field.getColumnName()))
										<span class="index-badge index-ok sort-index whitespace-nowrap" title="어떤 인덱스의 첫 컬럼이라 정렬에 인덱스를 씁니다">✓ 인덱스</span>
									@else
										<span class="index-badge index-warn sort-index whitespace-nowrap" title="어떤 인덱스의 첫 컬럼도 아니라 정렬할 때 인덱스를 못 씁니다. 큰 테이블에서 느립니다.">⚠ 인덱스 안 씀</span>
									@endif
								@endif
								@if(field.isSortableUserSet())<span class="hint whitespace-nowrap">직접 지정</span>@endif
							</div>
						</td>
						<td>
							<input class="checkbox" type="checkbox" form="${formId}" name="updatable" value="true" checked="${field.isUpdatable()}" disabled="${field.isVirtual()}" aria-label="${field.getColumnName()} 수정 가능" />
							@if(field.isUpdatableUserSet())<span class="hint">직접 지정</span>@endif
						</td>
						<td><button class="btn btn-sm" form="${formId}">저장</button></td>
					</tr>
				@endfor
			</tbody>
		</table>
	</div>
	`)
	<p class="hint">정렬은 처음에 인덱스 첫 컬럼만 허용합니다. 정렬을 켜면 그 컬럼이 인덱스를 쓰는지 옆에 보입니다(⚠ 는 큰 테이블에서 느림).</p>
	<p class="hint mt-1">수정은 수정 쿼리에서 값을 바꿀 수 있는지입니다. PK · unique · 자동 증가 컬럼은 처음에 수정 불가이고, 수정 폼에서 읽기 전용으로 보이며 서버도 원래 값을 그대로 씁니다. 입력 쿼리(새 행)에는 적용하지 않습니다.</p>
`)
