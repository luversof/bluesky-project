@import java.util.List

@param List<String> names

@for(var name : names)
	<option value="${name}"></option>
@endfor
