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
			<span class="badge">선택한 행 ${form.rows().size()}개</span>
		@endif
	</div>
	@template._components.alert(message = error, cssClass = "")
	@for(var row : form.rows())
		<input type="hidden" name="rows" value="${row}" />
	@endfor
	@if(form.fields().isEmpty() && query.getKind() == QueryKind.DELETE)
		<p class="text-sm">선택한 행을 지웁니다. 되돌릴 수 없습니다.</p>
	@endif
	<div class="grid gap-3 sm:grid-cols-2">
		@for(var field : form.fields())
			!{var value = inputs.containsKey(field.name()) ? inputs.get(field.name()) : field.value();}
			@template._components.field(label = field.header(), forId = "v-" + field.name(), required = field.required() && !field.readonly(),
				hint = field.readonly() ? "수정할 수 없는 값입니다." : null, content = @`
				@template._components.typedInput(id = "v-" + field.name(), name = "v." + field.name(), value = value,
					inputType = field.inputType(), required = field.required(), maxLength = field.maxLength(), readonly = field.readonly())
			`)
		@endfor
	</div>
	@template._components.formActions(content = @`
		<button type="button" class="btn btn-sm btn-ghost" data-modal-close>닫기</button>
		<button class="btn btn-sm ${query.getKind() == QueryKind.DELETE ? "btn-error" : "btn-primary"}">실행</button>
	`)
</form>
