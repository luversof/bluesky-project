// [data-toggle="대상 id"] 단추가 대상의 hidden 을 켜고 끈다(+ 추가 · ✎ 이름 바꾸기 입력칸).
// 같은 대상을 가리키는 단추들의 aria-expanded 를 맞추고, 열리면 첫 입력칸에 초점을 준다.
document.addEventListener('click', (e) => {
  const button = e.target.closest('[data-toggle]');
  if (!button) return;
  const target = document.getElementById(button.dataset.toggle);
  if (!target) return;
  target.hidden = !target.hidden;
  document
    .querySelectorAll('[data-toggle="' + button.dataset.toggle + '"][aria-expanded]')
    .forEach((b) => b.setAttribute('aria-expanded', String(!target.hidden)));
  if (!target.hidden) {
    const input = target.querySelector('input:not([type=hidden])');
    if (input) {
      input.focus();
      input.select();
    }
  }
});
