<%-- 프로젝트 설정 변경 이력(프로젝트 관리 → "설정 변경 이력"): 프로젝트 · 메인 메뉴 · 그 아래 메뉴 · 쿼리 · 검색 설정. 지운 메뉴도 여기서 본다 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import net.luversof.web.dynamiccrud2.domain.Project
@import net.luversof.web.dynamiccrud2.admin.service.SettingLogService.Filter
@import net.luversof.web.dynamiccrud2.admin.service.SettingLogService.HistoryPage

@param Project project
@param Filter settingFilter
@param HistoryPage settings
@param String pageUrl
@param java.util.List<net.luversof.web.dynamiccrud2.admin.controller.Crumb> breadcrumb = null

@template._layout.layout(title = t("admin.history.projectTitle", project.getName()), breadcrumb = breadcrumb, content = @`
	@template.admin.settingHistory(filter = settingFilter, settings = settings, action = "/admin/project/" + project.getId() + "/history", pageUrl = pageUrl)
`)
