@import net.luversof.web.dynamiccrud2.domain.ParamSetting.Role
@import net.luversof.web.dynamiccrud2.domain.QueryKind
@import net.luversof.web.dynamiccrud2.admin.service.QueryService.QueryInspection

@param QueryKind kind
@param QueryInspection inspection

@template._components.alert(message = inspection.error(), cssClass = "mb-3")
@template._components.alert(message = inspection.passed() == null ? null : "✓ " + inspection.passed(), tone = "success", cssClass = "mb-3")
@for(var warning : inspection.warnings())
	@template._components.alert(message = warning, tone = "warning", cssClass = "mb-2")
@endfor

@if(!inspection.params().isEmpty())
	<h3 class="font-semibold mt-4 mb-2">파라미터</h3>
	<table class="table" aria-label="파라미터">
		<thead><tr><th>이름</th><th>역할</th><th>필수</th><th>기본값</th><th>조건</th></tr></thead>
		<tbody>
			@for(var param : inspection.params())
				!{var prefix = "p." + param.name();}
				!{var locked = kind == QueryKind.READ && !param.optionalCapable();}
				<tr>
					<td class="font-mono">${param.name()}</td>
					<td>
						<select class="select select-sm" name="${prefix}.role" aria-label="${param.name()} 역할">
							@for(var role : Role.values())
								<option value="${role.name()}" selected="${param.setting().role() == role}">
									${role == Role.INPUT ? "입력" : role == Role.ROW_KEY ? "행 키(선택한 행에서)" : "검색"}
								</option>
							@endfor
						</select>
					</td>
					<td>
						@if(locked)
							<input type="hidden" name="${prefix}.required" value="true" />
							<input class="checkbox" type="checkbox" checked disabled aria-label="${param.name()} 필수(바꿀 수 없음)" title="OR · 서브쿼리 안이거나 해석할 수 없는 쿼리라 필수로 고정" />
						@else
							<input class="checkbox" type="checkbox" name="${prefix}.required" value="true" checked="${param.setting().required()}" aria-label="${param.name()} 필수" />
						@endif
					</td>
					<td><input class="input input-sm w-36" name="${prefix}.defaultValue" value="${param.setting().defaultValue()}" aria-label="${param.name()} 기본값" /></td>
					<td class="font-mono text-xs">${param.expression()}</td>
				</tr>
			@endfor
		</tbody>
	</table>
@endif

@if(!inspection.columns().isEmpty())
	<h3 class="font-semibold mt-4 mb-2">결과 컬럼</h3>
	<p class="hint mb-2">별칭이 있으면 화면 제목으로 씁니다. 다른 테이블 컬럼은 그 테이블의 필드로, 계산 결과는 이 메뉴 테이블의 조회 전용 필드로 저장 때 등록됩니다.</p>
	<table class="table" aria-label="결과 컬럼">
		<thead><tr><th>결과 이름</th><th>원래 위치</th><th>종류</th></tr></thead>
		<tbody>
			@for(var column : inspection.columns())
				<tr>
					<td>${column.label()}</td>
					<td class="font-mono text-xs">${column.tableName() == null ? "-" : column.tableName() + "." + (column.columnName() == null ? "*" : column.columnName())}</td>
					<td>${switch (column.kind()) { case COLUMN -> "컬럼"; case EXPRESSION -> "계산(조회 전용)"; case ALL, ALL_TABLE -> "전체 컬럼"; }}</td>
				</tr>
			@endfor
		</tbody>
	</table>
@endif
