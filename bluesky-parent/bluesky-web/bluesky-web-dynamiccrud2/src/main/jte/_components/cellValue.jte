<%-- 목록 칸 하나(td). 값은 필드 입력 형식(테이블 필드 관리)에 맞춰 보이기만 바꾼다 - 행 JSON · 엑셀 · 이력은 원래 값 그대로.
     일시 = "날짜 시각"(T 대신 빈칸), 날짜 = 앞 10자, 참거짓 = ✓ / –(화면 낭독기는 예 / 아니오), 숫자 = 오른쪽 정렬(자릿수 구분은 안 함 -
     id · 연도 같은 숫자가 1,234 로 보이면 오히려 읽기 어렵다). title 에는 원래 값.
     필드 값 목록(options, 저장값 → 표시)에 있는 값은 표시 글자로 보인다(형식보다 먼저).
     날짜 형식(dateFormat, 필드 표시 설정)이 있으면 날짜 · 일시는 그 형식으로(읽을 수 없으면 원래 값) --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import java.util.Map
@import net.luversof.web.dynamiccrud2.domain.InputType
@param String value
@param InputType inputType = InputType.TEXT
@param boolean nullMark = false
@param Map<String, String> options = java.util.Map.of()
@param String dateFormat = null

!{var raw = value == null ? "" : value;}
!{var type = inputType == null ? InputType.TEXT : inputType;}
!{var lower = raw.toLowerCase();}
!{var yes = lower.equals("true") || lower.equals("t") || lower.equals("1") || lower.equals("y");}
!{var no = lower.equals("false") || lower.equals("f") || lower.equals("0") || lower.equals("n");}

@if(value == null && nullMark)
	<td class="hint">NULL</td>
@elseif(options != null && options.containsKey(raw))
	<td class="max-w-xs truncate" title="${raw}">${options.get(raw)}</td>
@elseif(type == InputType.BOOLEAN && (yes || no))
	<td class="text-center" title="${raw}"><span aria-hidden="true">${yes ? "✓" : "–"}</span><span class="sr-only">${t(yes ? "common.yes" : "common.no")}</span></td>
@elseif(type == InputType.NUMBER)
	<td class="max-w-xs truncate text-right tabular-nums" title="${raw}">${raw}</td>
@elseif((type == InputType.DATETIME || type == InputType.DATE) && dateFormat != null)
	<td class="whitespace-nowrap tabular-nums" title="${raw}">${net.luversof.web.dynamiccrud2.menu.service.ValueConverter.formatDate(raw, dateFormat)}</td>
@elseif(type == InputType.DATETIME)
	<td class="whitespace-nowrap tabular-nums" title="${raw}">${raw.replace('T', ' ')}</td>
@elseif(type == InputType.DATE)
	<td class="whitespace-nowrap tabular-nums" title="${raw}">${raw.length() > 10 ? raw.substring(0, 10) : raw}</td>
@else
	<td class="max-w-xs truncate" title="${raw}">${raw}</td>
@endif
