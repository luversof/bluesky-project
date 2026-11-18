<%-- 테이블 하나의 인덱스 표: 이름 · 컬럼(순서) · 종류. 검색칸은 "첫 컬럼"으로 찾을 때 인덱스를 쓴다 --%>
@import net.luversof.web.dynamiccrud2.admin.service.SearchService.TableIndexes

@param TableIndexes table

<div>
	<h3 class="text-sm font-semibold mb-1">${table.title()}</h3>
	@if(table.error() != null)
		@template._components.emptyState(message = table.error())
	@elseif(table.indexes().isEmpty())
		@template._components.emptyState(message = "인덱스가 없습니다. 검색 · 정렬은 큰 테이블에서 느립니다.")
	@else
		<table class="table table-compact" aria-label="${table.title()} 인덱스">
			<thead><tr><th>인덱스</th><th>컬럼(순서)</th><th>종류</th></tr></thead>
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
							@else<span class="badge">일반</span>@endif
							@if(index.partial())<span class="badge ml-1" title="조건(WHERE)에 맞는 행만 담은 인덱스">부분</span>@endif
							@if(index.expression())<span class="badge ml-1" title="컬럼이 아닌 식으로 만든 인덱스 - 검색칸은 못 씀">식</span>@endif
						</td>
					</tr>
				@endfor
			</tbody>
		</table>
	@endif
</div>
