<%-- 메뉴 검색 설정으로 만든 검색칸 하나. 범위는 부터 ~ 까지 두 칸(f.결과이름 · f.결과이름.to) --%>
@import net.luversof.web.dynamiccrud2.domain.SearchOperator
@import net.luversof.web.dynamiccrud2.menu.service.MenuPageService.FilterField

@param FilterField field
@param int index

!{var id = "f-" + index;}

@template._components.field(label = field.header() + (field.operator() == SearchOperator.RANGE ? "" : " · " + field.operator().title()), forId = id, content = @`
	@if(field.operator() == SearchOperator.RANGE)
		<div class="flex items-center gap-1">
			@template._components.typedInput(id = id, name = "f." + field.label(), value = field.value(), inputType = field.inputType(), small = true, options = field.options())
			<span class="hint" aria-hidden="true">~</span>
			@template._components.typedInput(id = id + "-to", name = "f." + field.label() + ".to", value = field.valueTo(), inputType = field.inputType(), small = true, options = field.options())
		</div>
	@else
		@template._components.typedInput(id = id, name = "f." + field.label(), value = field.value(), inputType = field.inputType(), small = true, options = field.options())
	@endif
`)
