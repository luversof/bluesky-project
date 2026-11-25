<%-- 필드 입력 형식(InputType)에 맞는 입력칸 하나: 여러 줄 · 참/거짓 · 숫자 · 날짜 · 날짜시간 · 글자.
     검색칸(사용 화면)과 작업 폼(입력 · 수정)이 같이 쓴다. 필드 값 목록(options)이 있으면 형식과 상관없이 그 목록의 셀렉트 -
     지금 값이 목록에 없으면(목록을 나중에 적은 옛 값) 그 값도 그대로 고를 수 있게 둔다 --%>
@import java.util.Map
@import net.luversof.web.dynamiccrud2.domain.InputType

@param String id
@param String name
@param String value
@param InputType inputType
@param boolean required = false
@param Integer maxLength = null
@param boolean small = false
@param boolean readonly = false
@param Map<String, String> options = java.util.Map.of()
<%-- json: json · jsonb 컬럼 - 여러 줄(내용만큼 커짐) · 고정폭, 열 때 보기 좋게 펼치고 보내기 전에 JSON 형식 검사(js/ui.js [data-json]) --%>
@param boolean json = false

@if(json && (options == null || options.isEmpty()))
	<textarea id="${id}" class="textarea textarea-autogrow code w-full" name="${name}" rows="3" spellcheck="false" data-json
		data-json-invalid="${net.luversof.web.dynamiccrud2.config.Msg.t("component.json.invalid")}"
		required="${required && !readonly}" readonly="${readonly}">${value}</textarea>
@elseif(options != null && !options.isEmpty())
	<select id="${id}" class="select ${small ? "select-sm" : "w-full"}" name="${name}" required="${required && !readonly}" disabled="${readonly}">
		<option value="">-</option>
		@for(var option : options.entrySet())
			<option value="${option.getKey()}" selected="${option.getKey().equals(value)}">${option.getValue()}</option>
		@endfor
		@if(value != null && !value.isEmpty() && !options.containsKey(value))
			<option value="${value}" selected>${value}</option>
		@endif
	</select>
@elseif(inputType == InputType.TEXTAREA)
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
