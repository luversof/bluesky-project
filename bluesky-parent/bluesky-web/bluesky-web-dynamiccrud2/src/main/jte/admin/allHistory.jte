<%-- 전체 설정 변경 이력(프로젝트 관리 왼쪽 목록 아래 "전체 설정 변경 이력"): 모든 프로젝트의 설정 변경 + 어느 프로젝트에도 속하지 않는 변경(프로젝트 순서) --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import net.luversof.web.dynamiccrud2.admin.service.SettingLogService.Filter
@import net.luversof.web.dynamiccrud2.admin.service.SettingLogService.HistoryPage

@param Filter settingFilter
@param HistoryPage settings
@param String pageUrl
@param java.util.List<net.luversof.web.dynamiccrud2.admin.controller.Crumb> breadcrumb = null

@template._layout.layout(title = t("admin.history.allTitle"), breadcrumb = breadcrumb, content = @`
	@template.admin.settingHistory(filter = settingFilter, settings = settings, action = "/admin/history", pageUrl = pageUrl)
`)
