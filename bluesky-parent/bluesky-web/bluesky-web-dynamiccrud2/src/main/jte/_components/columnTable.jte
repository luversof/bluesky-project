<%-- 테이블 하나의 컬럼 표(쿼리 작성 화면): 이름 · 타입(길이) · NULL 허용 · 기본값 · 키(PK · unique · 자동 증가) · 주석.
     입력 · 수정 · 삭제 쿼리를 쓸 때 어떤 값을 넣어야 하는지(NOT NULL · 기본값 · 자동 증가) 보도록 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import net.luversof.web.dynamiccrud2.adapter.TableMeta

@param TableMeta meta
@param String title

<div class="relative overflow-x-auto">
	<table class="table table-compact" aria-label="${t("columns.label", title)}">
		<thead><tr>
			<th>${t("admin.table.col.column")}</th><th>${t("columns.col.type")}</th><th>${t("columns.col.null")}</th>
			<th>${t("columns.col.default")}</th><th>${t("columns.col.key")}</th><th>${t("columns.col.comment")}</th>
		</tr></thead>
		<tbody>
			@for(var column : meta.columns())
				!{var unique = !meta.primaryKey().contains(column.name()) && meta.uniqueKeys().stream().anyMatch(k -> k.contains(column.name()));}
				<tr>
					<td class="font-mono text-xs whitespace-nowrap ${meta.primaryKey().contains(column.name()) ? "font-semibold" : ""}">${column.name()}</td>
					<%-- 길이는 글자 · 이진 · 고정 소수 타입에만(정수 · 참거짓의 JDBC 크기 int4(10) · bool(1) 은 뜻이 없다) --%>
					!{var sized = column.size() != null && column.size() > 0 && column.size() < Integer.MAX_VALUE
						&& column.typeName() != null && column.typeName().toLowerCase().matches(".*(char|binary|numeric|decimal|bit).*");}
					<td class="font-mono text-xs whitespace-nowrap">${column.typeName()}@if(sized)(${column.size()})@endif</td>
					<td class="whitespace-nowrap">
						@if(column.nullable())<span class="hint">${t("common.yes")}</span>@else<span class="badge">${t("columns.notNull")}</span>@endif
					</td>
					<td class="font-mono text-xs max-w-xs truncate" title="${column.defaultValue()}">${column.defaultValue()}</td>
					<td class="whitespace-nowrap">
						@if(meta.primaryKey().contains(column.name()))<span class="badge badge-primary">PK</span>@endif
						@if(unique)<span class="badge">unique</span>@endif
						@if(column.autoIncrement())<span class="badge ml-1">${t("columns.autoIncrement")}</span>@endif
					</td>
					<td class="text-xs hint max-w-xs truncate" title="${column.comment()}">${column.comment()}</td>
				</tr>
			@endfor
		</tbody>
	</table>
</div>
