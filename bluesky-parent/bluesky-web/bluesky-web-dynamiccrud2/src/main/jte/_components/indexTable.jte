<%-- 테이블 하나의 인덱스 표: 이름 · 컬럼(순서) · 종류. 검색칸은 "첫 컬럼"으로 찾을 때 인덱스를 쓴다 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import net.luversof.web.dynamiccrud2.admin.service.SearchService.TableIndexes

@param TableIndexes table

<div>
	<h3 class="text-sm font-semibold mb-1">${table.title()}</h3>
	@if(table.error() != null)
		@template._components.emptyState(message = table.error())
	@elseif(table.indexes().isEmpty())
		@template._components.emptyState(message = t("index.empty"))
	@else
		<table class="table table-compact" aria-label="${t("index.tableLabel", table.title())}">
			<thead><tr><th>${t("index.col.name")}</th><th>${t("index.col.columns")}</th><th>${t("index.col.kind")}</th></tr></thead>
			<tbody>
				@for(var index : table.indexes())
					<tr>
						<td class="font-mono text-xs whitespace-nowrap">${index.name()}</td>
						<td class="font-mono text-xs">
							@for(var i = 0; i < index.columns().size(); i++)
								@if(i > 0)<span class="hint"> → </span>@endif
								<span class="${i == 0 ? "font-semibold" : ""}">${index.columns().get(i)}</span>
							@endfor
						</td>
						<td class="whitespace-nowrap">
							@if(index.primary())<span class="badge badge-primary">PK</span>
							@elseif(index.unique())<span class="badge">unique</span>
							@else<span class="badge">${t("index.kind.normal")}</span>@endif
							@if(index.partial())<span class="badge ml-1" title="${t("index.kind.partial.title")}">${t("index.kind.partial")}</span>@endif
							@if(index.expression())<span class="badge ml-1" title="${t("index.kind.expression.title")}">${t("index.kind.expression")}</span>@endif
						</td>
					</tr>
				@endfor
			</tbody>
		</table>
	@endif
</div>
