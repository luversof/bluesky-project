@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import java.util.Map
@import net.luversof.web.dynamiccrud2.domain.QueryKind
@import net.luversof.web.dynamiccrud2.menu.service.MenuPageService.ActionForm

@param ActionForm form
@param String error
@param Map<String, String> inputs
@param String menuPath

!{var query = form.query();}

<form class="card grid gap-3" hx-post="${menuPath}/action/${query.getId().toString()}/run" hx-target="#actionPanel">
	<div class="flex items-center gap-2">
		<h2 class="font-semibold">${query.getName()}</h2>
		@if(!form.rows().isEmpty())
			<span class="badge">${t("menu.action.selectedRows", form.rows().size())}</span>
		@endif
	</div>
	@template._components.alert(message = error, cssClass = "")
	@for(var row : form.rows())
		<input type="hidden" name="rows" value="${row}" />
	@endfor
	@if(form.fields().isEmpty() && query.getKind() == QueryKind.DELETE)
		<p class="text-sm">${t("menu.action.deleteWarn")}</p>
	@endif
	<div class="grid gap-3 sm:grid-cols-2">
		@for(var field : form.fields())
			!{var value = inputs.containsKey(field.name()) ? inputs.get(field.name()) : field.value();}
			@template._components.field(label = field.header(), forId = "v-" + field.name(), required = field.required() && !field.readonly(),
				hint = field.readonly() ? t("menu.action.readonly") : null, content = @`
				@template._components.typedInput(id = "v-" + field.name(), name = "v." + field.name(), value = value,
					inputType = field.inputType(), required = field.required(), maxLength = field.maxLength(), readonly = field.readonly(), options = field.options())
			`)
		@endfor
	</div>
	@template._components.formActions(content = @`
		<button type="button" class="btn btn-sm btn-ghost" data-modal-close>${t("common.close")}</button>
		<button class="btn btn-sm ${query.getKind() == QueryKind.DELETE ? "btn-error" : "btn-primary"}">${t("menu.action.run")}</button>
	`)
</form>
