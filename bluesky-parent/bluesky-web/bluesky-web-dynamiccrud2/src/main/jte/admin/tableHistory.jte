<%-- 테이블 필드 변경 이력(필드 화면 제목 줄 → "변경 이력"): 필드 화면에서 고친 것 · DB 다시 읽기로 바뀐 컬럼. 필드는 여러 메뉴가 공유한다 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import net.luversof.web.dynamiccrud2.domain.DbTable
@import net.luversof.web.dynamiccrud2.admin.service.SettingLogService.Filter
@import net.luversof.web.dynamiccrud2.admin.service.SettingLogService.HistoryPage

@param DbTable dbTable
@param java.util.UUID menuId = null
@param Filter settingFilter
@param HistoryPage settings
@param String pageUrl
@param java.util.List<net.luversof.web.dynamiccrud2.admin.controller.Crumb> breadcrumb = null

@template._layout.layout(title = t("admin.history.tableTitle", dbTable.getTableName()), breadcrumb = breadcrumb, content = @`
	@template.admin.settingHistory(filter = settingFilter, settings = settings, action = "/admin/table/" + dbTable.getId() + "/history", pageUrl = pageUrl,
		showTarget = false, keepMenu = menuId == null ? null : menuId.toString())
`)
