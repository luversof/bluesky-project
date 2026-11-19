<%-- 검색해서 고르는 입력칸(js/ui.js 의 combobox). 목록은 입력칸 바로 아래 같은 시작 위치에 뜬다.
     고른 값은 hidden(name)에 들어가고, 목록에서 고르지 않으면 제출할 수 없다(보이는 칸이 입력 검사를 맡는다).
     후보 목록(li[role=option][data-value])은 셋 중 하나로 채운다:
     - searchUrl: 입력할 때마다 서버에서 찾는다(keyword=입력값)
     - loadUrl + loadTrigger: 다른 값이 정해질 때 한 번 받아 오고, 입력은 받아 온 목록 안에서 거른다
     - options: 화면을 그릴 때 넣어 두고 입력으로 거른다(검색 설정의 "+ 추가" 처럼 고르면 바로 무언가 하는 칸)
     name 이 없으면 값은 폼으로 가지 않고, required=false 면 비워 둬도 제출된다.
     mustPick: 비워 둬도 되지만 글자를 쳤으면 목록에서 골라야 제출된다(찾기 조건 칸). value · label 은 처음 고른 값 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@param String id
@param String name = null
@param String placeholder = null
@param String searchUrl = null
@param String loadUrl = null
@param String loadTrigger = null
@param String loadInclude = null
@param gg.jte.Content options = null
@param boolean required = true
@param String emptyText = net.luversof.web.dynamiccrud2.config.Msg.t("common.noCandidates")
@param String cssClass = "w-full"
@param String ariaLabel = null
@param boolean mustPick = false
@param String value = null
@param String label = null

<div class="combobox ${cssClass}" data-combobox data-local="${loadUrl != null || options != null}" data-empty-text="${emptyText}">
	<input id="${id}-input" class="input w-full" type="text" role="combobox" autocomplete="off" required="${required}" data-must-pick="${mustPick}" value="${label}"
		aria-autocomplete="list" aria-expanded="false" aria-controls="${id}-list" placeholder="${placeholder}" aria-label="${ariaLabel}" data-combobox-input
		hx-get="${searchUrl}" hx-trigger="${searchUrl == null ? null : "input changed delay:250ms, focus once"}"
		hx-target="${searchUrl == null ? null : "#" + id + "-list"}" hx-vals="${searchUrl == null ? null : "js:{keyword: document.getElementById('" + id + "-input').value}"}" />
	<input id="${id}" type="hidden" name="${name}" value="${value}" data-combobox-value />
	<ul id="${id}-list" class="combobox-list" role="listbox" data-combobox-list hidden
		hx-get="${loadUrl}" hx-trigger="${loadTrigger}" hx-include="${loadInclude}">@if(options != null)${options}@endif</ul>
</div>
