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

!{var menuId = menu.getId().toString();}

@template._layout.layout(title = t(queryId == null ? "admin.query.title.add" : "admin.query.title.edit", kind.name()), breadcrumb = breadcrumb, content = @`
	@template._components.alert(message = error)

	<form method="post" action="/admin/menu/${menuId}/query">
		<input type="hidden" name="kind" value="${kind.name()}" />
		@if(queryId != null)
			<input type="hidden" name="queryId" value="${queryId.toString()}" />
		@endif
		@template._components.card(title = t("admin.menu.basic"), body = @`
		<div class="grid gap-4 sm:grid-cols-2">
			@template._components.field(label = t("field.name"), forId = "name", content = @`
				<input id="name" class="input w-full" name="name" value="${name}" maxlength="100" placeholder="${t("admin.query.name.placeholder")}" />
			`)
			@if(kind != QueryKind.READ)
				@template._components.field(label = t("admin.query.authority"), forId = "authority", content = @`
					<input id="authority" class="input w-full" name="authority" value="${authority}" maxlength="50" placeholder="${t("admin.query.authority.placeholder")}" />
				`)
			@endif
		</div>
		`)
		@template._components.card(title = t("admin.query.col.query"), actions = @`
			<span class="hint">${t("admin.query.test.hint")}</span>
			<button type="button" class="btn btn-sm" hx-post="/admin/menu/${menuId}/query/inspect" hx-include="closest form" hx-target="#inspection" hx-swap="innerHTML">${t("admin.query.test")}</button>
		`, body = @`
		<div>
			<textarea aria-label="${t("admin.query.col.query")}" id="queryText" class="textarea code w-full" name="queryText" rows="12" spellcheck="false" required>${queryText}</textarea>
			<p class="hint mt-1">
				$unsafe{t("admin.query.paramHint")}
				@if(kind == QueryKind.READ)
					$unsafe{t("admin.query.readHint")}
				@endif
			</p>
		</div>
		<div id="inspection" class="mt-4">
			@template.admin.queryInspection(kind = kind, inspection = inspection, menuId = menu.getId())
		</div>
		`)
		@template._components.formActions(content = @`
			<a class="btn btn-ghost" href="/admin/menu/${menuId}">${t("common.cancel")}</a>
			<button class="btn btn-primary">${t("common.save")}</button>
		`)
	</form>
`)
