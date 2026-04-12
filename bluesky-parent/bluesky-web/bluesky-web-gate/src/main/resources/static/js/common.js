"use strict";
// URL query parameter 처리
const param = (() => {
  let _params = new URLSearchParams(globalThis.location.search);
  return {
    refreshUrl() {
      globalThis.history.replaceState(null, "", "?" + _params.toString());
    },
    getParams() {
      return _params;
    },
    getParam(paramKey) {
      if (_params.get(paramKey) === "") _params.delete(paramKey);
      return _params.get(paramKey) === null ? null : _params.get(paramKey);
    },
    setParam(paramKey, paramValue) {
      if (paramValue === null || paramValue === "") {
        _params.delete(paramKey);
      } else {
        _params.set(paramKey, paramValue);
      }
      this.refreshUrl();
    },
    deleteParam(paramKey) {
      _params.delete(paramKey);
    },
    resetParam() {
      _params = new URLSearchParams();
      this.refreshUrl();
    },
    getRequestPage() {
      const page = this.getParam("page");
      return (page === null ? 1 : Number.parseInt(page, 10)) - 1;
    },
  };
})();
// 전역으로 노출
globalThis.param = param;
// HTMX beforeSwap 이벤트 처리
document.addEventListener("htmx:beforeSwap", (event) => {
  var _a;
  if ("hx-indicator" in event.target.attributes) {
    const indicator =
      (_a = document.querySelector(
        event.target.getAttribute("hx-indicator"),
      )) === null || _a === void 0
        ? void 0
        : _a.cloneNode(true);
    if (indicator) {
      indicator.style.display = "block";
      event.target.innerHTML = "";
      event.target.appendChild(indicator);
    }
  }
});
// HTMX 에러 응답 처리 (4xx/5xx) - 스피너 대신 에러 메시지 표시
document.addEventListener("htmx:responseError", (event) => {
  const xhr = event.detail.xhr;
  const target = event.detail.target || event.target;
  let message = "오류가 발생했습니다.";
  try {
    const json = JSON.parse(xhr.responseText);
    if (json.title) message = json.title;
    if (json.detail && json.detail !== "Failed to write request")
      message = json.detail;
  } catch (e) {
    /* JSON 파싱 실패 시 기본 메시지 사용 */
  }
  if (target) {
    target.innerHTML = `<div class="alert alert-error my-2"><svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-6 w-6" fill="none" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg><span>${message} (${xhr.status})</span></div>`;
  }
});
// HTMX 네트워크 에러 처리
document.addEventListener("htmx:sendError", (event) => {
  const target = event.detail.target || event.target;
  if (target) {
    target.innerHTML = `<div class="alert alert-error my-2"><svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-6 w-6" fill="none" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg><span>서버에 연결할 수 없습니다.</span></div>`;
  }
});
