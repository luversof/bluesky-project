<%-- 테이블 정보 카드(접힘): 테이블마다 컬럼 표(_components/columnTable) → 인덱스 표(_components/indexTable).
     쿼리 추가 · 편집 화면(펼침 - 쿼리를 쓰며 본다)과 메뉴 관리 화면 검색 설정 위(접힘 - 검색칸마다 인덱스 사용 여부가 이미 보여 필요할 때만)가 같이 쓴다 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@param java.util.List<net.luversof.web.dynamiccrud2.admin.service.SearchService.TableIndexes> tables
@param boolean open = true
@param String targetNoun = net.luversof.web.dynamiccrud2.config.Msg.t("target.table")
@param String rememberKey = null

@if(!tables.isEmpty())
	@template._components.card(title = t("columns.tableInfo", targetNoun), help = t("columns.tableInfo.help"), collapsible = true, open = open,
		rememberKey = rememberKey, body = @`
		<div class="grid gap-6">
			@for(var table : tables)
				<div class="grid gap-3">
					@if(table.meta() != null)
						<div>
							<h3 class="text-sm font-semibold mb-1">${t("columns.label", table.title())}</h3>
							@template._components.columnTable(meta = table.meta(), title = table.title())
						</div>
					@endif
					@template._components.indexTable(table = table, heading = t("index.tableLabel", table.title()))
				</div>
			@endfor
		</div>
	`)
@endif
