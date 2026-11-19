<%-- 데이터 출처(메뉴를 만들 때 고른 connection · 테이블과 DB 종류). 만든 뒤 바꿀 수 없는 값이라 입력칸 대신 라벨 + 값으로 보인다.
     메뉴 관리 기본 정보 · 필드 관리가 같이 쓴다 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import net.luversof.web.dynamiccrud2.domain.DbTable

@param DbTable dbTable
@param String databaseType
@param String targetNoun = net.luversof.web.dynamiccrud2.config.Msg.t("target.table")
@param String cssClass = ""

<dl class="grid gap-x-6 gap-y-1 grid-cols-[auto_1fr] sm:flex sm:flex-wrap sm:items-baseline ${cssClass}">
	<div class="contents sm:flex sm:items-baseline sm:gap-2">
		<dt class="label">connection</dt>
		<dd class="font-mono text-sm">${dbTable.getConnectionName()}</dd>
	</div>
	<div class="contents sm:flex sm:items-baseline sm:gap-2">
		<dt class="label">${t("source.dbType")}</dt>
		<dd class="text-sm">${databaseType}</dd>
	</div>
	<div class="contents sm:flex sm:items-baseline sm:gap-2">
		<dt class="label">${targetNoun}</dt>
		<dd class="font-mono text-sm">${dbTable.getCatalogName() == null || dbTable.getSchemaName() != null ? "" : dbTable.getCatalogName() + "."}${dbTable.getSchemaName() == null ? "" : dbTable.getSchemaName() + "."}${dbTable.getTableName()}</dd>
	</div>
</dl>
