@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import java.util.UUID
@import net.luversof.web.dynamiccrud2.domain.Menu
@import net.luversof.web.dynamiccrud2.domain.QueryKind
@import net.luversof.web.dynamiccrud2.admin.service.QueryService.QueryInspection

@param Menu menu
@param UUID queryId
@param QueryKind kind
@param String name
@param String queryText
@param String authority
@param String error
@param QueryInspection inspection
@param java.util.List<net.luversof.web.dynamiccrud2.admin.controller.Crumb> breadcrumb = null

@template._layout.layout(title = t(queryId == null ? "admin.query.title.add" : "admin.query.title.edit", kind.name()), breadcrumb = breadcrumb, content = @`
	@template.admin.queryEditor(menu = menu, queryId = queryId, kind = kind, name = name, queryText = queryText, authority = authority,
		error = error, inspection = inspection)
`)
