<%-- htmx 조각을 띄우는 레이어. hx-target 을 panelId 로 주면 내용이 들어올 때 열리고, 닫으면 비운다
     (js/ui.js: Esc · 바깥 클릭 · [data-modal-close]). 목록 위에 띄워 스크롤 위치를 바꾸지 않는다.
     wide: 넓은 폼(쿼리 편집처럼 표 · 긴 입력칸이 있는 것) --%>
@param String id
@param String panelId
@param String label
@param boolean wide = false

<dialog id="${id}" class="modal ${wide ? "modal-wide" : ""}" data-modal aria-label="${label}">
	<div id="${panelId}" data-modal-panel></div>
</dialog>
