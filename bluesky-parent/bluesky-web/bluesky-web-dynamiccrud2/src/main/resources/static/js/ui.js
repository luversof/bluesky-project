// 공통 UI 동작. 컴포넌트(jte/_components)가 다는 data-* 표식에 반응한다 - 화면마다 스크립트를 따로 두지 않는다.

// [data-confirm] 단추: 확인을 받은 뒤에만 폼을 보낸다(_components/iconDelete 등)
document.addEventListener('click', (event) => {
  const button = event.target.closest('[data-confirm]');
  if (button && !confirm(button.dataset.confirm)) {
    event.preventDefault();
  }
});

// 처리 중 표시: 누른 단추에 aria-busy(회전 표시는 main.css 의 [aria-busy=true]) + 다시 누르기 막기.
// - 폼 제출(새 화면으로 가는 것): 입력 검사를 통과하고 확인([data-confirm])도 받은 submit 만. 단추를 disabled 로 하면 단추의
//   name · value 가 빠지므로 막지 않고 표시 + 같은 폼 두 번 제출만 막는다. 뒤로 가기로 돌아오면(bfcache) 풀어 준다.
// - htmx 요청(테스트 단추 등): 요청을 보낸 단추, 폼이면 누른 단추에 표시하고 끝나면 푼다. 입력칸(자동완성)이 보낸 요청은 표시하지 않는다.
(() => {
  const busy = (button, on) => {
    if (!button || !button.matches('button, input[type=submit]')) return;
    if (on) {
      button.setAttribute('aria-busy', 'true');
      button.setAttribute('aria-disabled', 'true');
    } else {
      button.removeAttribute('aria-busy');
      button.removeAttribute('aria-disabled');
    }
  };
  document.addEventListener('submit', (event) => {
    const form = event.target;
    if (event.defaultPrevented || form.hasAttribute('hx-post') || form.hasAttribute('hx-get')) return;
    if (form.dataset.submitting) {
      event.preventDefault();
      return;
    }
    form.dataset.submitting = 'true';
    busy(event.submitter, true);
  });
  window.addEventListener('pageshow', (event) => {
    if (!event.persisted) return;
    document.querySelectorAll('form[data-submitting]').forEach((form) => delete form.dataset.submitting);
    document.querySelectorAll('[aria-busy=true]').forEach((button) => busy(button, false));
  });
  const requester = (detail) => detail.requestConfig?.triggeringEvent?.submitter || detail.elt;
  document.addEventListener('htmx:beforeRequest', (event) => busy(requester(event.detail), true));
  document.addEventListener('htmx:afterRequest', (event) => busy(requester(event.detail), false));
})();

// .textarea-autogrow: CSS field-sizing 을 모르는 브라우저(Firefox · Safari)만 입력할 때 높이를 내용에 맞춘다(최대 높이는 CSS)
if (!CSS.supports('field-sizing', 'content')) {
  const fit = (area) => {
    area.style.height = 'auto';
    area.style.height = area.scrollHeight + 2 + 'px';
  };
  document.addEventListener('input', (event) => {
    if (event.target.matches?.('.textarea-autogrow')) fit(event.target);
  });
  const fitAll = () => document.querySelectorAll('.textarea-autogrow').forEach(fit);
  fitAll();
  document.addEventListener('htmx:afterSwap', fitAll);
}

// 접히는 카드 제목 줄의 ⓘ 를 눌러도 카드가 접히지 않게(설명은 초점으로 뜬다)
document.addEventListener('click', (event) => {
  if (event.target.closest('summary .help-btn')) event.preventDefault();
});

// 접히는 카드의 접힘 상태 기억([data-remember-open="키"], _components/card rememberKey): 이 브라우저에만(localStorage).
// 저장소를 못 쓰면(사생활 모드 등) 처음 상태 그대로. 레이어로 들어온 조각(htmx)에도 적용한다
(() => {
  const storeKey = (details) => 'card-open:' + details.dataset.rememberOpen;
  const restore = (root) =>
    root.querySelectorAll?.('details[data-remember-open]').forEach((details) => {
      try {
        const saved = localStorage.getItem(storeKey(details));
        if (saved !== null) details.open = saved === 'true';
      } catch (e) {
        /* 저장소를 못 쓰면 처음 상태 */
      }
    });
  restore(document);
  document.addEventListener('htmx:afterSwap', (event) => restore(event.detail.target));
  // toggle 은 올라가지 않으므로 잡는 단계에서 듣는다
  document.addEventListener(
    'toggle',
    (event) => {
      const details = event.target;
      if (!details.matches?.('details[data-remember-open]')) return;
      try {
        localStorage.setItem(storeKey(details), String(details.open));
      } catch (e) {
        /* 기억만 못 할 뿐 */
      }
    },
    true,
  );
})();

// [data-modal] 레이어(_components/modal): 안의 [data-modal-panel] 에 htmx 조각이 들어오면 열고, 닫으면 비운다.
// 실패해 사유와 함께 다시 그려도 열린 채로 둔다. 바깥(배경)을 누르거나 [data-modal-close] 를 누르면 닫는다.
document.addEventListener('htmx:afterSwap', (event) => {
  const panel = event.detail.target;
  const dialog = panel.closest && panel.closest('dialog[data-modal]');
  if (!dialog || !panel.matches('[data-modal-panel]') || !panel.firstElementChild || dialog.open) return;
  dialog.showModal();
  panel.querySelector('input:not([type=hidden]):not([readonly]), select, textarea')?.focus();
});
document.addEventListener('click', (event) => {
  const dialog = event.target.closest('dialog[data-modal]');
  if (!dialog) return;
  if (event.target === dialog || event.target.closest('[data-modal-close]')) dialog.close();
});
document.querySelectorAll('dialog[data-modal]').forEach((dialog) =>
  dialog.addEventListener('close', () => {
    dialog.querySelector('[data-modal-panel]').innerHTML = '';
  }),
);

// combobox(_components/combobox): 입력하면 후보 목록(li[role=option][data-value])을 입력칸 바로 아래에 보이고,
// 고르면 보이는 칸에 이름 · hidden 에 값을 넣는다. 목록에서 고르지 않은 글자로는 제출하지 못한다(보이는 칸의 입력 검사).
// data-local 이면 받아 온 목록 안에서 입력으로 거르고, 아니면 서버가 거른 목록(htmx)을 그대로 보인다.
(() => {
  const parts = (box) => ({
    input: box.querySelector('[data-combobox-input]'),
    value: box.querySelector('[data-combobox-value]'),
    list: box.querySelector('[data-combobox-list]'),
  });
  const options = (list) => [...list.querySelectorAll('li[role=option]:not([hidden])')];
  const setValid = (box) => {
    const { input, value } = parts(box);
    // 필수가 아닌 칸("+ 추가" 처럼 고르면 바로 무언가 하는 칸)은 남은 글자로 폼 제출을 막지 않는다
    // 찾기 조건 칸(data-must-pick)은 비워 둘 수 있지만 친 글자는 목록에서 골라야 한다
    const mustPick = input.required || input.hasAttribute('data-must-pick');
    input.setCustomValidity(!mustPick || value.value || !input.value ? '' : document.body.dataset.msgPick);
  };
  const open = (box) => {
    const { input, list } = parts(box);
    // 후보가 아직 오지 않았으면 빈 상자를 띄우지 않는다
    if (!list.querySelector('li:not([hidden])')) {
      close(box);
      return;
    }
    list.hidden = false;
    input.setAttribute('aria-expanded', 'true');
  };
  const close = (box) => {
    const { input, list } = parts(box);
    list.hidden = true;
    input.setAttribute('aria-expanded', 'false');
    input.removeAttribute('aria-activedescendant');
    list.querySelectorAll('[aria-selected=true]').forEach((li) => li.setAttribute('aria-selected', 'false'));
  };
  const filter = (box) => {
    const { input, list } = parts(box);
    if (box.dataset.local === undefined) return;
    const keyword = input.value.trim().toLowerCase();
    let shown = 0;
    list.querySelectorAll('li[role=option]').forEach((li) => {
      // 이름(data-label)으로만 거른다 - 작은 설명(인덱스 이름 등)에 걸려 엉뚱한 후보가 남지 않게
      li.hidden = keyword !== '' && !(li.dataset.label || li.textContent).toLowerCase().includes(keyword);
      if (!li.hidden) shown++;
    });
    let empty = list.querySelector('[data-combobox-empty]');
    if (!empty) {
      empty = document.createElement('li');
      empty.dataset.comboboxEmpty = '';
      empty.className = 'combobox-empty';
      empty.textContent = box.dataset.emptyText || document.body.dataset.msgNoCandidates;
      list.append(empty);
    }
    empty.hidden = shown > 0 || list.querySelector('li[role=option]') === null;
  };
  const choose = (box, li) => {
    const { input, value } = parts(box);
    input.value = li.dataset.label || li.textContent.trim();
    value.value = li.dataset.value;
    setValid(box);
    close(box);
    // 다음 칸(예: 테이블 목록)이 이 값을 보고 다시 불러오게 알린다
    value.dispatchEvent(new Event('change', { bubbles: true }));
  };
  const move = (box, step) => {
    const { input, list } = parts(box);
    const items = options(list);
    if (items.length === 0) return;
    open(box);
    const current = items.findIndex((li) => li.getAttribute('aria-selected') === 'true');
    const next = items[(current + step + items.length) % items.length];
    items.forEach((li) => li.setAttribute('aria-selected', String(li === next)));
    if (!next.id) next.id = list.id + '-' + Math.random().toString(36).slice(2, 8);
    input.setAttribute('aria-activedescendant', next.id);
    next.scrollIntoView({ block: 'nearest' });
  };

  document.addEventListener('input', (event) => {
    const box = event.target.closest('[data-combobox]');
    if (!box || !event.target.matches('[data-combobox-input]')) return;
    parts(box).value.value = '';
    setValid(box);
    filter(box);
    open(box);
  });
  document.addEventListener('focusin', (event) => {
    const box = event.target.closest('[data-combobox]');
    if (box && event.target.matches('[data-combobox-input]') && parts(box).list.childElementCount > 0) {
      filter(box);
      open(box);
    }
  });
  document.addEventListener('focusout', (event) => {
    const box = event.target.closest('[data-combobox]');
    if (!box || !event.target.matches('[data-combobox-input]')) return;
    // 글자를 정확히 다 쳤으면 고른 것으로 본다
    const { input, value, list } = parts(box);
    if (!value.value && input.value) {
      const exact = [...list.querySelectorAll('li[role=option]')].find(
        (li) => (li.dataset.label || li.textContent.trim()) === input.value.trim(),
      );
      if (exact) choose(box, exact);
    }
    close(box);
  });
  document.addEventListener('keydown', (event) => {
    const box = event.target.closest('[data-combobox]');
    if (!box || !event.target.matches('[data-combobox-input]')) return;
    const { list } = parts(box);
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      move(box, event.key === 'ArrowDown' ? 1 : -1);
    } else if (event.key === 'Enter' && !list.hidden) {
      const active = list.querySelector('li[aria-selected=true]');
      if (active) {
        event.preventDefault();
        choose(box, active);
      }
    } else if (event.key === 'Escape' && !list.hidden) {
      event.preventDefault();
      close(box);
    }
  });
  // 마우스로 고를 때 입력칸이 먼저 초점을 잃어 목록이 닫히지 않게 mousedown 에서 고른다
  document.addEventListener('mousedown', (event) => {
    const li = event.target.closest('[data-combobox-list] li[role=option]');
    if (!li || li.getAttribute('aria-disabled') === 'true') return;
    event.preventDefault();
    choose(li.closest('[data-combobox]'), li);
  });
  document.addEventListener('htmx:afterSwap', (event) => {
    const list = event.detail.target;
    if (!list.matches || !list.matches('[data-combobox-list]')) return;
    const box = list.closest('[data-combobox]');
    const { input, value } = parts(box);
    if (box.dataset.local !== undefined) {
      // 앞 칸이 바뀌어 목록을 새로 받았다 - 고른 값을 비운다
      input.value = '';
      value.value = '';
      setValid(box);
    }
    filter(box);
    if (document.activeElement === input) open(box);
  });
})();

// 추가형 목록([data-picklist]): 항목([data-pick-key])은 고르지 않은 것도 숨긴 채(입력 disabled) 그려 두고,
// 안의 combobox 에서 고르면 그 항목을 보이며 맨 끝으로, × ([data-pick-remove]) 를 누르면 숨기고 후보로 되돌린다.
// 폼으로 가는 순서는 화면 순서다(⠿ 로 바꾼 순서 포함).
(() => {
  const escapeKey = (key) => (window.CSS && CSS.escape ? CSS.escape(key) : key.replace(/"/g, '\\"'));
  const setEnabled = (item, enabled) =>
    item.querySelectorAll('input, select').forEach((el) => {
      el.disabled = !enabled;
    });
  const refreshEmpty = (picklist) => {
    const items = picklist.querySelector('[data-picklist-items]');
    const empty = picklist.querySelector('[data-picklist-empty]');
    if (empty) empty.hidden = !!items.querySelector('[data-pick-key]:not([hidden])');
  };

  document.addEventListener('change', (event) => {
    const value = event.target;
    const picklist = value.closest && value.closest('[data-picklist]');
    if (!picklist || !value.matches('[data-combobox-value]') || !value.value) return;
    const box = value.closest('[data-combobox]');
    const items = picklist.querySelector('[data-picklist-items]');
    const item = items.querySelector('[data-pick-key="' + escapeKey(value.value) + '"]');
    box.querySelector('[data-combobox-list] li[data-value="' + escapeKey(value.value) + '"]')?.remove();
    box.querySelector('[data-combobox-input]').value = '';
    value.value = '';
    if (!item) return;
    item.hidden = false;
    setEnabled(item, true);
    items.append(item);
    refreshEmpty(picklist);
    (item.querySelector('select') || item.querySelector('[data-pick-remove]'))?.focus();
  });

  document.addEventListener('click', (event) => {
    const button = event.target.closest('[data-pick-remove]');
    const picklist = button && button.closest('[data-picklist]');
    if (!picklist) return;
    const item = button.closest('[data-pick-key]');
    item.hidden = true;
    setEnabled(item, false);
    // 후보 목록으로 되돌린다(_components/pickOption 과 같은 모양)
    const list = picklist.querySelector('[data-combobox-list]');
    const li = document.createElement('li');
    li.setAttribute('role', 'option');
    li.setAttribute('aria-selected', 'false');
    li.dataset.value = item.dataset.pickKey;
    li.dataset.label = item.dataset.pickLabel;
    li.textContent = item.dataset.pickLabel;
    if (item.dataset.pickSub) {
      const sub = document.createElement('span');
      sub.className = 'combobox-sub';
      sub.textContent = ' ' + item.dataset.pickSub;
      li.append(sub);
    }
    list.prepend(li);
    refreshEmpty(picklist);
    picklist.querySelector('[data-combobox-input]')?.focus();
  });
})();

// 검색 방식을 바꾸면 인덱스 사용 문구도 바꾼다(_components/searchFieldRow: option 의 data-index-ok · data-index-text)
document.addEventListener('change', (event) => {
  const select = event.target.closest('[data-index-select]');
  if (!select) return;
  const option = select.selectedOptions[0];
  const badge = select.parentElement.querySelector('[data-index-badge]');
  if (!option || !badge) return;
  const ok = option.dataset.indexOk === 'true';
  badge.textContent = (ok ? '✓ ' : '⚠ ') + option.dataset.indexText;
  badge.title = option.dataset.indexText;
  badge.classList.toggle('index-ok', ok);
  badge.classList.toggle('index-warn', !ok);
});

// 전체 선택: [data-check-all="이름"] 이 같은 표의 input[name=이름] 체크박스를 모두 켜고 끈다.
// 하나씩 바꾸면 머리 칸이 전체 · 일부(indeterminate) · 없음을 따라간다
document.addEventListener('change', (event) => {
  const target = event.target;
  const scope = target.closest('table') || document;
  if (target.matches('[data-check-all]')) {
    scope
      .querySelectorAll('input[type=checkbox][name="' + target.dataset.checkAll + '"]')
      .forEach((box) => {
        box.checked = target.checked;
      });
    return;
  }
  if (target.matches('input[type=checkbox][name]')) {
    const all = scope.querySelector('[data-check-all="' + target.name + '"]');
    if (!all) return;
    const boxes = [...scope.querySelectorAll('input[type=checkbox][name="' + target.name + '"]')];
    const checked = boxes.filter((box) => box.checked).length;
    all.checked = checked > 0 && checked === boxes.length;
    all.indeterminate = checked > 0 && checked < boxes.length;
  }
});

// 라이트 · 다크 테마([data-theme-toggle]). 고른 값은 이 브라우저에만 남긴다(저장이 막혀도 화면은 동작)
function syncThemeButtons() {
  const dark = document.documentElement.dataset.theme === 'dark';
  document.querySelectorAll('[data-theme-toggle]').forEach((button) => {
    button.setAttribute('aria-pressed', String(dark));
  });
}
document.addEventListener('click', (event) => {
  if (!event.target.closest('[data-theme-toggle]')) return;
  const next = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
  document.documentElement.dataset.theme = next;
  try {
    localStorage.setItem('dc2-theme', next);
  } catch (e) {
    // 저장소를 못 쓰면 이번 화면에서만 바뀐다
  }
  syncThemeButtons();
});
syncThemeButtons();

// 도움말(_components/help): 설명이 뜰 때 오른쪽(화면 · 가로 스크롤 상자)으로 넘치면 왼쪽으로 펼친다(.help-end 와 같은 모양).
// 자리가 화면 폭 · 배치에 따라 달라져(오른쪽 끝 버튼, 줄의 마지막 입력칸) 화면마다 alignEnd 를 정할 수 없어서 뜰 때마다 잰다.
const placeHelp = (event) => {
  const help = event.target.closest?.('.help');
  if (!help || help.classList.contains('help-end')) return;
  const tip = help.querySelector('.help-tip');
  help.classList.remove('help-flip');
  const box = help.closest('.overflow-x-auto');
  const right = box ? box.getBoundingClientRect().right : document.documentElement.clientWidth;
  if (tip.getBoundingClientRect().right > right) help.classList.add('help-flip');
};
document.addEventListener('mouseover', placeHelp);
document.addEventListener('focusin', placeHelp);
