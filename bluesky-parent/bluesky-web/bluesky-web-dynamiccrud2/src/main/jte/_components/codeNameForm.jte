<%-- 이름 + 주소 이름 두 칸짜리 펼침 폼(프로젝트 · 메인 메뉴 추가와 이름 바꾸기). 처음엔 숨겨 두고
     iconToggle(target = formId) 이나 data-toggle 단추가 연다(js/toggle.js). itemId 가 있으면 수정.
     칸 순서는 화면 표시("이름 /주소")와 같게 이름 → 주소 이름(관리 화면 모든 입력이 이 순서).
     inline: 표시 줄 자리에서 글자 모양 그대로 고친다(밑줄만, Enter 저장 · Esc 취소, ✓ · × 단추). nameClass 는 표시 글자의 크기 · 굵기 --%>
@param String formId
@param String action
@param String noun
@param String itemId = null
@param String code = null
@param String name = null
@param String submitText = "추가"
@param boolean stacked = false
@param boolean inline = false
@param String nameClass = ""
@param String cssClass = ""

@if(inline)
	<form id="${formId}" method="post" action="${action}" class="flex flex-wrap items-center gap-1 ${cssClass}" hidden>
		@if(itemId != null)<input type="hidden" name="id" value="${itemId}" />@endif
		<input class="inline-edit ${nameClass}" name="name" value="${name}" required maxlength="100" aria-label="${noun} 이름" />
		<span class="hint font-mono" aria-hidden="true">/</span>
		@template._components.codeInput(value = code, inline = true, cssClass = "hint", ariaLabel = noun + " 주소 이름")
		<button class="icon-btn" aria-label="${submitText}" title="${submitText} (Enter)">✓</button>
		<button type="button" class="icon-btn" data-toggle="${formId}" aria-label="취소" title="취소 (Esc)">×</button>
	</form>
@else
	<form id="${formId}" method="post" action="${action}" class="${stacked ? "card grid gap-2" : "flex flex-wrap items-center gap-2"} ${cssClass}" hidden>
		@if(itemId != null)<input type="hidden" name="id" value="${itemId}" />@endif
		<input class="input input-sm" name="name" value="${name}" required maxlength="100"
			placeholder="${itemId == null ? noun + " 이름" : null}" aria-label="${noun} 이름" />
		@template._components.codeInput(value = code, small = true, cssClass = stacked ? "" : "w-40",
			placeholder = itemId == null ? "주소 이름" : null, ariaLabel = noun + " 주소 이름")
		@template._components.formActions(cssClass = stacked ? "" : "!justify-start", content = @`
			<button class="btn btn-sm btn-primary">${submitText}</button>
			<button type="button" class="btn btn-sm btn-ghost" data-toggle="${formId}">취소</button>
		`)
	</form>
@endif
