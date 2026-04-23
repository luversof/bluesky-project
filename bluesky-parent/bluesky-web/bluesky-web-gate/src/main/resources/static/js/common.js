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
            if (_params.get(paramKey) === "")
                _params.delete(paramKey);
            return _params.get(paramKey) === null ? null : _params.get(paramKey);
        },
        setParam(paramKey, paramValue) {
            if (paramValue === null || paramValue === "") {
                _params.delete(paramKey);
            }
            else {
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
        const indicator = (_a = document
            .querySelector(event.target.getAttribute("hx-indicator"))) === null || _a === void 0 ? void 0 : _a.cloneNode(true);
        if (indicator) {
            indicator.style.display = "block";
            event.target.innerHTML = "";
            event.target.appendChild(indicator);
        }
    }
});
