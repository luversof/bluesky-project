// 순서 바꾸기: [data-reorder-url] 목록의 바로 아래 [data-id] 항목을 ⠿ 손잡이로 끌거나(드래그 앤 드롭),
// 손잡이에 초점을 두고 위 · 아래 화살표로 옮긴다. 바뀐 순서는 ids=… 로 data-reorder-url 에 보낸다.
// 목록은 겹칠 수 있다(프로젝트 안 메인 메뉴) - 각 목록은 자기 바로 아래 항목만 다룬다.
// 같은 data-reorder-group 목록끼리는 항목을 넘겨 줄 수 있다(메뉴를 다른 메인 메뉴로, 사용자 결정 11-24): 끌어 놓거나, 목록 맨 위 · 아래에서
// 화살표를 한 번 더 누르면 이웃 목록으로 간다. 받은 목록이 자기 순서(옮겨 온 id 포함)를 보내면 서버가 옮기고, 화면은 다시 그린다
// (사용 화면 주소 · 지우기 막힘 등이 바뀐다). 막히면(주소 이름 겹침 등 409) 사유를 알리고 서버 상태로 되돌린다.
(() => {
  const focusKey = 'reorder-focus';
  let dragged = null;
  let from = null;

  const items = (list) => [...list.children].filter((c) => c.dataset.id);
  const groupOf = (list) => list.dataset.reorderGroup || null;
  const groupLists = (group) => [...document.querySelectorAll('[data-reorder-group="' + group + '"]')];

  // 주소가 비어 있으면 바로 저장하지 않는다 - 폼 안 목록(검색칸)은 저장 단추를 누를 때 화면 순서대로 보낸다
  const save = (list, moved, focusId) =>
    list.dataset.reorderUrl &&
    fetch(list.dataset.reorderUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams(items(list).map((item) => ['ids', item.dataset.id])),
    }).then(async (response) => {
      // 실패(세션 만료 · 주소 이름 겹침 등)면 서버 순서로 되돌려 보여 준다
      if (!response.ok) {
        if (response.status === 409) alert(await response.text());
        location.reload();
        return;
      }
      if (moved) {
        try { sessionStorage.setItem(focusKey, focusId); } catch (e) {}
        location.reload();
      }
    });

  // 다른 목록으로 옮긴 뒤 다시 그렸으면 옮긴 항목 손잡이로 초점을 돌려 준다(키보드로 이어서 옮기게)
  try {
    const id = sessionStorage.getItem(focusKey);
    if (id) {
      sessionStorage.removeItem(focusKey);
      document.querySelector('[data-id="' + CSS.escape(id) + '"] .drag-handle')?.focus();
    }
  } catch (e) {}

  document.querySelectorAll('[data-reorder-url]').forEach((list) => {
    const ownItem = (el) => {
      const item = el && el.closest('[data-id]');
      return item && item.parentElement === list ? item : null;
    };
    const accepts = () => dragged && (from === list || (groupOf(list) && groupOf(list) === groupOf(from)));

    // 입력칸 · 글자를 끌지 않도록 손잡이를 누를 때만 끌 수 있게 한다
    list.addEventListener('pointerdown', (e) => {
      const handle = e.target.closest('.drag-handle');
      const item = handle && ownItem(handle);
      if (item) item.draggable = true;
    });
    list.addEventListener('dragstart', (e) => {
      const item = ownItem(e.target);
      if (!item || !item.draggable) return;
      e.stopPropagation();
      dragged = item;
      from = list;
      e.dataTransfer.effectAllowed = 'move';
      item.classList.add('opacity-50');
      document.body.classList.add('reorder-dragging');
    });
    list.addEventListener('dragover', (e) => {
      if (!accepts()) return;
      e.preventDefault();
      e.stopPropagation();
      const over = ownItem(e.target);
      if (over) {
        if (over === dragged) return;
        const rect = over.getBoundingClientRect();
        const after = e.clientY > rect.top + rect.height / 2;
        list.insertBefore(dragged, after ? over.nextSibling : over);
      } else if (dragged.parentElement !== list && items(list).length === 0) {
        // 빈 목록(메뉴가 없는 메인 메뉴): 끄는 동안 놓을 자리가 보인다(main.css .reorder-dragging)
        list.appendChild(dragged);
      }
    });
    list.addEventListener('drop', (e) => {
      if (accepts()) e.preventDefault();
    });

    list.addEventListener('keydown', (e) => {
      const handle = e.target.closest('.drag-handle');
      const item = handle && ownItem(handle);
      if (!item || (e.key !== 'ArrowUp' && e.key !== 'ArrowDown')) return;
      const prev = item.previousElementSibling;
      const next = item.nextElementSibling;
      let target = null;
      if (e.key === 'ArrowUp' && prev && prev.dataset.id) {
        list.insertBefore(item, prev);
      } else if (e.key === 'ArrowDown' && next && next.dataset.id) {
        list.insertBefore(next, item);
      } else if (groupOf(list)) {
        // 맨 위 · 아래에서 한 번 더: 이웃 목록(앞 목록의 끝 / 뒤 목록의 처음)으로
        const lists = groupLists(groupOf(list));
        target = lists[lists.indexOf(list) + (e.key === 'ArrowUp' ? -1 : 1)];
        if (!target) return;
        if (e.key === 'ArrowUp') target.appendChild(item);
        else target.insertBefore(item, target.firstChild);
      } else {
        return;
      }
      e.preventDefault();
      e.stopPropagation();
      handle.focus();
      if (target) save(target, true, item.dataset.id);
      else save(list);
    });
  });

  // 끝난 곳은 놓은 목록 - 처음 목록과 다르면 옮긴 것
  document.addEventListener('dragend', () => {
    if (!dragged) return;
    const item = dragged;
    const to = item.parentElement;
    item.classList.remove('opacity-50');
    item.draggable = false;
    document.body.classList.remove('reorder-dragging');
    dragged = null;
    if (from === to) save(to);
    else save(to, true, item.dataset.id);
    from = null;
  });
})();
