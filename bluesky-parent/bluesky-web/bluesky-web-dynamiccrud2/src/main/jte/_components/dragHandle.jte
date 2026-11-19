<%-- 순서 바꾸기 손잡이. [data-reorder-url] 목록의 [data-id] 항목 안에 둔다(js/reorder.js: 끌기 · ↑ ↓ 키) --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@param String name

<button type="button" class="drag-handle" aria-label="${t("component.drag", name)}">⠿</button>
