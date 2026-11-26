<%-- 메뉴 검색 설정으로 만든 검색칸 하나. 범위는 부터 ~ 까지 두 칸(f.결과이름 · f.결과이름.to).
     셀렉트 목록(options)은 검색 설정의 고르기 방식(값 목록 · DB 값 전체)에서 온다. DB 값이 너무 많으면 입력칸 + 안내 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import net.luversof.web.dynamiccrud2.domain.SearchOperator
@import net.luversof.web.dynamiccrud2.menu.service.MenuPageService.FilterField

@param FilterField field
@param int index

!{var id = "f-" + index;}

@template._components.field(label = field.header() + (field.operator() == SearchOperator.RANGE ? "" : " · " + field.operator().title()), forId = id, required = field.required(), hint = field.tooMany() ? t("menu.search.tooMany") : null, inline = true, content = @`
	@if(field.operator() == SearchOperator.RANGE)
		<div class="flex items-center gap-1">
			@template._components.typedInput(id = id, name = "f." + field.label(), value = field.value(), inputType = field.inputType(), small = true, options = field.options())
			<span class="hint" aria-hidden="true">~</span>
			@template._components.typedInput(id = id + "-to", name = "f." + field.label() + ".to", value = field.valueTo(), inputType = field.inputType(), small = true, options = field.options())
		</div>
	@else
		@template._components.typedInput(id = id, name = "f." + field.label(), value = field.value(), inputType = field.inputType(), small = true, options = field.options(), required = field.required())
	@endif
`)
