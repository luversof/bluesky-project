<%-- 주소 이름(code) 입력칸. 규칙은 서버 MenuService.CODE 와 같다(영문 소문자 · 숫자 · -, 1~50자, 첫 글자는 - 아님) --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@param String value = null
@param String id = null
@param String ariaLabel = null
@param String placeholder = null
@param boolean small = false
@param boolean inline = false
@param String cssClass = ""

<%-- inline: 글자 모양 그대로 편집하는 칸(.inline-edit, 그 자리 수정) --%>
<input id="${id}" class="${inline ? "inline-edit" : "input"} ${small && !inline ? "input-sm" : ""} font-mono ${cssClass}" name="code" value="${value}" required maxlength="50"
	pattern="[a-z0-9][a-z0-9\-]{0,49}" title="${t("component.code.title")}" placeholder="${placeholder}" aria-label="${ariaLabel}" />
