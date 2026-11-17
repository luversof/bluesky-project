@import java.util.Map
@import net.luversof.web.dynamiccrud2.setting.domain.InputType
@import net.luversof.web.dynamiccrud2.setting.domain.QueryKind
@import net.luversof.web.dynamiccrud2.use.service.UseService.ActionForm

@param ActionForm form
@param String error
@param Map<String, String> inputs

!{var menuId = form.menu().getId().toString();}
!{var query = form.query();}

<form class="card grid gap-3" hx-post="/use/menu/${menuId}/action/${query.getId().toString()}/run" hx-target="#actionPanel">
	<div class="flex items-center gap-2">
		<h2 class="font-semibold">${query.getName()}</h2>
		@if(!form.rows().isEmpty())
			<span class="badge">선택한 행 ${form.rows().size()}개</span>
		@endif
	</div>
	@if(error != null)
		<div class="alert alert-error" role="alert">${error}</div>
	@endif
	@for(var row : form.rows())
		<input type="hidden" name="rows" value="${row}" />
	@endfor
	@if(form.fields().isEmpty() && query.getKind() == QueryKind.DELETE)
		<p class="text-sm">선택한 행을 지웁니다. 되돌릴 수 없습니다.</p>
	@endif
	<div class="grid gap-3 sm:grid-cols-2">
		@for(var field : form.fields())
			!{var value = inputs.containsKey(field.name()) ? inputs.get(field.name()) : field.value();}
			<div>
				<label class="label" for="v-${field.name()}">${field.header()}@if(field.required()) <span class="text-error">*</span>@endif</label>
				@if(field.inputType() == InputType.TEXTAREA)
					<textarea id="v-${field.name()}" class="textarea w-full" name="v.${field.name()}" rows="4" maxlength="${field.maxLength()}" required="${field.required()}">${value}</textarea>
				@elseif(field.inputType() == InputType.BOOLEAN)
					<select id="v-${field.name()}" class="select w-full" name="v.${field.name()}" required="${field.required()}">
						<option value="">-</option>
						<option value="true" selected="${"true".equals(value)}">true</option>
						<option value="false" selected="${"false".equals(value)}">false</option>
					</select>
				@else
					<input id="v-${field.name()}" class="input w-full" name="v.${field.name()}" value="${value}" required="${field.required()}" maxlength="${field.maxLength()}"
						type="${field.inputType() == InputType.NUMBER ? "number" : field.inputType() == InputType.DATE ? "date" : field.inputType() == InputType.DATETIME ? "datetime-local" : "text"}"
						step="${field.inputType() == InputType.NUMBER ? "any" : field.inputType() == InputType.DATETIME ? "1" : null}" />
				@endif
			</div>
		@endfor
	</div>
	<div class="flex gap-2">
		<button class="btn btn-sm ${query.getKind() == QueryKind.DELETE ? "btn-error" : "btn-primary"}">실행</button>
		<button type="button" class="btn btn-sm btn-ghost" onclick="this.closest('#actionPanel').innerHTML = ''">닫기</button>
	</div>
</form>
