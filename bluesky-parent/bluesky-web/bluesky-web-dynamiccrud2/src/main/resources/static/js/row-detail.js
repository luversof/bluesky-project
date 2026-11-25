// 행 자세히(사용 화면 목록, menu/page.jte): 줄 끝 [data-row-detail] 아이콘 → 그 행의 모든 컬럼을 세로로(제목 · 원래 값 전체) 레이어에 보인다.
// 값은 화면에 이미 있는 것(칸의 title = 원래 값, JSON 은 ui.js 가 펼쳐 둔 것)이라 서버에 다시 묻지 않는다.
// 레이어의 수정 · 삭제 단추는 목록의 같은 단추를 대신 누른다: 수정 = 그 행 줄 끝 단추([data-row-action]),
// 삭제 = 이 행만 고른 뒤 목록 위 선택 삭제 단추([data-row-delete]). 둘 다 레이어를 닫고 작업 폼 레이어가 뜬다.
(() => {
  const holder = document.getElementById('rowDetail');
  const dialog = document.getElementById('rowDialog');
  if (!holder || !dialog) return;
  const panel = dialog.querySelector('[data-modal-panel]');

  const element = (tag, className, text) => {
    const el = document.createElement(tag);
    if (className) el.className = className;
    if (text != null) el.textContent = text;
    return el;
  };

  document.addEventListener('click', (event) => {
    const opener = event.target.closest('[data-row-detail]');
    if (!opener) return;
    const row = opener.closest('tr');
    const heads = [...row.closest('table').tHead.rows[0].cells];
    const list = element('dl', 'row-detail');
    [...row.cells].forEach((cell, i) => {
      if (cell.querySelector('input[name="rows"]') || cell.classList.contains('sticky-end')) return;
      list.append(element('dt', null, heads[i] ? heads[i].textContent.trim() : ''));
      const value = cell.hasAttribute('title') ? cell.title : cell.textContent.trim();
      list.append(element('dd', cell.hasAttribute('data-json') ? 'font-mono text-xs' : null, value));
    });

    const actions = element('div', 'flex flex-wrap justify-end gap-2');
    actions.append(element('button', 'btn btn-sm btn-ghost', holder.dataset.close));
    actions.lastChild.type = 'button';
    actions.lastChild.setAttribute('data-modal-close', '');
      // 글자: 삭제는 목록 위 "선택 삭제" 대신 작업 이름(data-row-delete 값) - 이 행만 지우므로
    const proxy = (original, run, label) => {
      const button = element('button', original.className, label || original.textContent.trim());
      button.type = 'button';
      button.addEventListener('click', () => {
        dialog.close();
        run();
      });
      actions.append(button);
    };
    row.querySelectorAll('[data-row-action]').forEach((button) => proxy(button, () => button.click()));
    const check = row.querySelector('input[name="rows"]');
    if (check) {
      document.querySelectorAll('[data-row-delete]').forEach((button) =>
        proxy(button, () => {
          document.querySelectorAll('input[name="rows"]').forEach((box) => {
            box.checked = box === check;
          });
          button.click();
        }, button.dataset.rowDelete),
      );
    }

    const card = element('div', 'card grid gap-3');
    card.append(element('h2', 'font-semibold', holder.dataset.title), list, actions);
    panel.replaceChildren(card);
    dialog.showModal();
  });
})();
