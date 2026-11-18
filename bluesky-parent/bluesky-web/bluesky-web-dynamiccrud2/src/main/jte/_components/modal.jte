<%-- htmx 조각을 띄우는 레이어. hx-target 을 panelId 로 주면 내용이 들어올 때 열리고, 닫으면 비운다
     (js/modal.js: Esc · 바깥 클릭 · [data-modal-close]). 목록 위에 띄워 스크롤 위치를 바꾸지 않는다 --%>
@param String id
@param String panelId
@param String label

<dialog id="${id}" class="modal" data-modal aria-label="${label}">
	<div id="${panelId}" data-modal-panel></div>
</dialog>
