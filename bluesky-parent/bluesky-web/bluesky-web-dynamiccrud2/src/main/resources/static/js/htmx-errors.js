// htmx 요청이 실패하면(로그인 만료 · 권한 없음 · 서버 오류) 화면에 아무 변화가 없어 단추가 안 듣는 것처럼 보인다 - 사유를 알린다.
document.addEventListener('htmx:responseError', (event) => {
  const status = event.detail.xhr.status;
  if (status === 401) {
    // 로그인이 풀렸다 - 화면을 다시 열면 서버가 로그인으로 보낸다
    location.reload();
    return;
  }
  alert(status === 403 ? '이 작업을 할 권한이 없습니다.' : `요청을 처리하지 못했습니다(${status}). 잠시 뒤 다시 시도하세요.`);
});
document.addEventListener('htmx:sendError', () => {
  alert('서버에 연결하지 못했습니다. 네트워크를 확인하세요.');
});
