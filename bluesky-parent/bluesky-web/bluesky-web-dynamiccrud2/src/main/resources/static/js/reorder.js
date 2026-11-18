// 순서 바꾸기: [data-reorder-url] 목록의 바로 아래 [data-id] 항목을 ⠿ 손잡이로 끌거나(드래그 앤 드롭),
// 손잡이에 초점을 두고 위 · 아래 화살표로 옮긴다. 바뀐 순서는 ids=… 로 data-reorder-url 에 보낸다.
// 목록은 겹칠 수 있다(프로젝트 안 메인 메뉴) - 각 목록은 자기 바로 아래 항목만 다룬다.
document.querySelectorAll('[data-reorder-url]').forEach((list) => {
  const ownItem = (el) => {
    const item = el && el.closest('[data-id]');
    return item && item.parentElement === list ? item : null;
  };
  const items = () => [...list.children].filter((c) => c.dataset.id);
  let dragged = null;

  // 주소가 비어 있으면 바로 저장하지 않는다 - 폼 안 목록(검색칸)은 저장 단추를 누를 때 화면 순서대로 보낸다
  const save = () =>
    list.dataset.reorderUrl &&
    fetch(list.dataset.reorderUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams(items().map((item) => ['ids', item.dataset.id])),
    }).then((response) => {
      // 실패(세션 만료 등)면 서버 순서로 되돌려 보여 준다
      if (!response.ok) location.reload();
    });

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
    e.dataTransfer.effectAllowed = 'move';
    item.classList.add('opacity-50');
  });
  list.addEventListener('dragover', (e) => {
    if (!dragged) return;
    const over = ownItem(e.target);
    e.preventDefault();
    if (!over || over === dragged) return;
    const rect = over.getBoundingClientRect();
    const after = e.clientY > rect.top + rect.height / 2;
    list.insertBefore(dragged, after ? over.nextSibling : over);
  });
  list.addEventListener('drop', (e) => {
    if (dragged) e.preventDefault();
  });
  list.addEventListener('dragend', () => {
    if (!dragged) return;
    dragged.classList.remove('opacity-50');
    dragged.draggable = false;
    dragged = null;
    save();
  });

  list.addEventListener('keydown', (e) => {
    const handle = e.target.closest('.drag-handle');
    const item = handle && ownItem(handle);
    if (!item) return;
    const prev = item.previousElementSibling;
    const next = item.nextElementSibling;
    if (e.key === 'ArrowUp' && prev && prev.dataset.id) {
      list.insertBefore(item, prev);
    } else if (e.key === 'ArrowDown' && next && next.dataset.id) {
      list.insertBefore(next, item);
    } else {
      return;
    }
    e.preventDefault();
    e.stopPropagation();
    handle.focus();
    save();
  });
});
