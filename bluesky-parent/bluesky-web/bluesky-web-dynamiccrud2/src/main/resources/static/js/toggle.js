// [data-toggle="대상 id"] 단추가 대상의 hidden 을 켜고 끈다(+ 추가 · ✎ 이름 바꾸기 입력칸).
// 같은 대상을 가리키는 단추들의 aria-expanded 를 맞추고, 열리면 첫 입력칸에 초점을 준다.
// [data-hidden-by="대상 id"] 는 대상이 열려 있는 동안 숨는다 - 표시 줄이 그 자리에서 입력칸으로 바뀌는 수정(✎).
// 그 자리 수정 중 Esc 는 취소다.
function toggleTarget(id, show) {
  const target = document.getElementById(id);
  if (!target) return;
  target.hidden = show === undefined ? !target.hidden : !show;
  document
    .querySelectorAll('[data-toggle="' + id + '"][aria-expanded]')
    .forEach((b) => b.setAttribute('aria-expanded', String(!target.hidden)));
  document.querySelectorAll('[data-hidden-by="' + id + '"]').forEach((el) => {
    el.hidden = !target.hidden;
  });
  if (!target.hidden) {
    const input = target.querySelector('input:not([type=hidden])');
    if (input) {
      input.focus();
      input.select();
    }
  } else {
    // 닫으면 그 자리의 ✎ 단추로 초점을 돌려준다
    document.querySelector('[data-toggle="' + id + '"][aria-expanded]')?.focus();
  }
}

document.addEventListener('click', (e) => {
  const button = e.target.closest('[data-toggle]');
  if (button) toggleTarget(button.dataset.toggle);
});

document.addEventListener('keydown', (e) => {
  if (e.key !== 'Escape') return;
  const form = e.target.closest('form[id]');
  // form.id 는 안의 <input name="id"> 에 가려지므로 속성으로 읽는다
  const id = form && form.getAttribute('id');
  if (id && document.querySelector('[data-hidden-by="' + id + '"]')) {
    e.preventDefault();
    toggleTarget(id, false);
  }
});
