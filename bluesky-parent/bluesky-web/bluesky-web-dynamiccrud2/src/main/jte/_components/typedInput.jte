<%-- 필드 입력 형식(InputType)에 맞는 입력칸 하나: 여러 줄 · 참/거짓 · 숫자 · 날짜 · 날짜시간 · 글자.
     검색칸(사용 화면)과 작업 폼(입력 · 수정)이 같이 쓴다 --%>
@import net.luversof.web.dynamiccrud2.domain.InputType

@param String id
@param String name
@param String value
@param InputType inputType
@param boolean required = false
@param Integer maxLength = null
@param boolean small = false
@param boolean readonly = false

@if(inputType == InputType.TEXTAREA)
	<textarea id="${id}" class="textarea w-full" name="${name}" rows="4" maxlength="${maxLength}" required="${required && !readonly}" readonly="${readonly}">${value}</textarea>
@elseif(inputType == InputType.BOOLEAN)
	<select id="${id}" class="select ${small ? "select-sm" : "w-full"}" name="${name}" required="${required && !readonly}" disabled="${readonly}">
		<option value="">-</option>
		<option value="true" selected="${"true".equals(value)}">true</option>
		<option value="false" selected="${"false".equals(value)}">false</option>
	</select>
@else
	<input id="${id}" class="input ${small ? "input-sm" : "w-full"}" name="${name}" value="${value}" required="${required && !readonly}" maxlength="${maxLength}" readonly="${readonly}"
		type="${inputType == InputType.NUMBER ? "number" : inputType == InputType.DATE ? "date" : inputType == InputType.DATETIME ? "datetime-local" : "text"}"
		step="${inputType == InputType.NUMBER ? "any" : inputType == InputType.DATETIME ? "1" : null}" />
@endif
