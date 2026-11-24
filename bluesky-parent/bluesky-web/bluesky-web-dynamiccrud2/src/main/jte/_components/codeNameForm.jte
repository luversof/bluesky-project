<%-- 이름 + 주소 이름 두 칸짜리 펼침 폼(프로젝트 · 메인 메뉴 추가와 이름 바꾸기). 처음엔 숨겨 두고
     iconToggle(target = formId) 이나 data-toggle 단추가 연다(js/toggle.js). itemId 가 있으면 수정.
     칸 순서는 화면 표시("이름 /주소")와 같게 이름 → 주소 이름(관리 화면 모든 입력이 이 순서).
     inline: 표시 줄 자리에서 글자 모양 그대로 고친다(밑줄만, Enter 저장 · Esc 취소, ✓ · × 단추). nameClass 는 표시 글자의 크기 · 굵기 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@param String formId
@param String action
@param String noun
@param String itemId = null
@param String code = null
@param String name = null
@param String submitText = net.luversof.web.dynamiccrud2.config.Msg.t("common.add")
@param boolean stacked = false
@param boolean inline = false
@param String nameClass = ""
@param String cssClass = ""

@if(inline)
	<form id="${formId}" method="post" action="${action}" class="flex flex-wrap items-center gap-1 ${cssClass}" hidden>
		@if(itemId != null)<input type="hidden" name="id" value="${itemId}" />@endif
		<input class="inline-edit ${nameClass}" name="name" value="${name}" required maxlength="100" aria-label="${t("component.codeName.name", noun)}" />
		<span class="hint font-mono" aria-hidden="true">/</span>
		@template._components.codeInput(value = code, inline = true, cssClass = "hint", ariaLabel = t("component.codeName.code", noun))
		<button class="icon-btn" aria-label="${submitText}" data-tip="${submitText} (Enter)">✓</button>
		<button type="button" class="icon-btn" data-toggle="${formId}" aria-label="${t("common.cancel")}" data-tip="${t("common.cancel")} (Esc)">×</button>
	</form>
@else
	<form id="${formId}" method="post" action="${action}" class="${stacked ? "card grid gap-2" : "flex flex-wrap items-center gap-2"} ${cssClass}" hidden>
		@if(itemId != null)<input type="hidden" name="id" value="${itemId}" />@endif
		<input class="input input-sm" name="name" value="${name}" required maxlength="100"
			placeholder="${itemId == null ? t("component.codeName.name", noun) : null}" aria-label="${t("component.codeName.name", noun)}" />
		@template._components.codeInput(value = code, small = true, cssClass = stacked ? "" : "w-40",
			placeholder = itemId == null ? t("component.codeName.codePlaceholder") : null, ariaLabel = t("component.codeName.code", noun))
		@template._components.formActions(cssClass = stacked ? "" : "!justify-start", content = @`
			<button class="btn btn-sm btn-primary">${submitText}</button>
			<button type="button" class="btn btn-sm btn-ghost" data-toggle="${formId}">${t("common.cancel")}</button>
		`)
	</form>
@endif
