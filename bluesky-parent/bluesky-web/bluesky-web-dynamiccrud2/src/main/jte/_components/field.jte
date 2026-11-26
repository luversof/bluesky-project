<%-- 입력 한 칸: 라벨 + 입력(content) + 도움말. 입력의 id 를 forId 로 넘겨 라벨과 잇는다.
     hint 는 입력 아래 늘 보이는 짧은 글, help 는 라벨 옆 ⓘ 툴팁(_components/help) --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@param String label
@param String forId
@param gg.jte.Content content
@param String hint = null
@param String help = null
@param boolean required = false
@param String cssClass = ""
<%-- inline: 라벨을 입력 왼쪽 한 줄에(사용 화면 검색 - 높이를 줄인다, 사용자 요청 11-26) --%>
@param boolean inline = false

<div class="${inline ? "flex items-center gap-2" : ""} ${cssClass}">
	<div class="flex items-center gap-1 ${inline ? "shrink-0" : ""}">
		<label class="label ${inline ? "mb-0 whitespace-nowrap" : ""}" for="${forId}">${label}@if(required) <span class="text-error">*</span>@endif</label>
		@if(help != null)@template._components.help(text = help, label = t("common.helpFor", label))@endif
	</div>
	${content}
	@if(hint != null)<p class="hint mt-1">${hint}</p>@endif
</div>
