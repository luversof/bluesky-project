// htmx 요청이 실패하면(로그인 만료 · 권한 없음 · 서버 오류) 화면에 아무 변화가 없어 단추가 안 듣는 것처럼 보인다 - 사유를 알린다.
document.addEventListener('htmx:responseError', (event) => {
  const status = event.detail.xhr.status;
  if (status === 401) {
    // 로그인이 풀렸다 - 화면을 다시 열면 서버가 로그인으로 보낸다
    location.reload();
    return;
  }
  // 문구는 layout body 의 data-msg-* (지금 언어)
  const msg = document.body.dataset;
  alert(status === 403 ? msg.msgForbidden : msg.msgFailed.replace('{0}', status));
});
document.addEventListener('htmx:sendError', () => {
  alert(document.body.dataset.msgOffline);
});
