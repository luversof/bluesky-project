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
// 테마 토글([data-theme-toggle]): light/dark 전환 + localStorage 저장.
// 초기 적용은 defaultLayout <head>의 early-apply 스크립트가 담당한다.
document.addEventListener("click", (event) => {
    const toggle = event.target.closest("[data-theme-toggle]");
    if (!toggle)
        return;
    const html = document.documentElement;
    const next = html.getAttribute("data-theme") === "dark" ? "light" : "dark";
    html.setAttribute("data-theme", next);
    try {
        localStorage.setItem("theme", next);
    }
    catch (_a) {
        // localStorage 불가 환경(사생활 보호 모드 등)에서는 저장 없이 전환만
    }
});
// 활동 내역 [캘린더|타임라인|목록] 뷰 전환/복원 + 캘린더 날짜 상세 토글.
// 프래그먼트 인라인 스크립트가 아니라 문서 위임 + htmx:afterSettle 복원으로 처리해서
// htmx 스왑의 스크립트 실행 타이밍과 무관하게 항상 동작하게 한다.
const ACTIVITY_VIEW_KEY = "activityViewMode";
const ACTIVITY_TAB_ACTIVE_CLASSES = [
    "tab-active",
    "font-bold",
    "!bg-primary",
    "!text-primary-content",
    "shadow-sm",
];
function applyActivityView(root, rawMode) {
    const mode = rawMode === "timeline" || rawMode === "list" ? rawMode : "calendar";
    const panels = {
        calendar: root.querySelector("#activityCalendarView"),
        timeline: root.querySelector("#activityTimelineView"),
        list: root.querySelector("#activityListView"),
    };
    Object.keys(panels).forEach((key) => {
        const panel = panels[key];
        if (panel)
            panel.classList.toggle("hidden", key !== mode);
    });
    root.querySelectorAll("[data-activity-view-tab]").forEach((tab) => {
        const isActive = tab.getAttribute("data-activity-view-tab") === mode;
        ACTIVITY_TAB_ACTIVE_CLASSES.forEach((cls) => tab.classList.toggle(cls, isActive));
        tab.classList.toggle("text-base-content/60", !isActive);
    });
}
function restoreActivityView() {
    const root = document.getElementById("activityListFragment");
    if (!root)
        return;
    let saved = null;
    try {
        saved = localStorage.getItem(ACTIVITY_VIEW_KEY);
    }
    catch (e) {
        saved = null;
    }
    applyActivityView(root, saved);
}
document.addEventListener("click", (event) => {
    const target = event.target;
    if (!target || !target.closest)
        return;
    const tab = target.closest("[data-activity-view-tab]");
    if (tab) {
        const root = tab.closest("#activityListFragment");
        if (!root)
            return;
        const mode = tab.getAttribute("data-activity-view-tab");
        applyActivityView(root, mode);
        try {
            localStorage.setItem(ACTIVITY_VIEW_KEY, mode === "timeline" || mode === "list" ? mode : "calendar");
        }
        catch (e) {
            // localStorage 불가 환경에서는 저장 없이 전환만
        }
        return;
    }
    const cell = target.closest("[data-cal-date]");
    if (cell) {
        const root = cell.closest("#activityListFragment");
        if (!root)
            return;
        const dateKey = cell.getAttribute("data-cal-date");
        const monthKey = cell.getAttribute("data-cal-month");
        const panel = root.querySelector('[data-cal-panel="' + monthKey + '"]');
        if (!panel)
            return;
        const detail = panel.querySelector('[data-cal-detail="' + dateKey + '"]');
        if (!detail)
            return;
        const isSameOpen = panel.classList.contains("is-open") &&
            !detail.classList.contains("hidden");
        if (isSameOpen) {
            panel.classList.remove("is-open");
            cell.classList.remove("bg-base-200");
            return;
        }
        panel
            .querySelectorAll("[data-cal-detail]")
            .forEach((other) => other.classList.add("hidden"));
        root
            .querySelectorAll("[data-cal-date].bg-base-200")
            .forEach((selected) => selected.classList.remove("bg-base-200"));
        detail.classList.remove("hidden");
        panel.classList.add("is-open");
        cell.classList.add("bg-base-200");
    }
});
document.addEventListener("DOMContentLoaded", restoreActivityView);
document.addEventListener("htmx:afterSettle", restoreActivityView);
// CSP 대응: hx-on:/hx-vals="js:" 는 htmx 가 eval 로 실행해 nonce 기반 CSP 와 함께 쓸 수 없다.
// 아래 데이터 속성 + 문서 위임으로 대체한다.
// [data-reload-after-request]: htmx 요청 성공 시 페이지 새로고침 (관리 데이터 갱신 버튼 등)
document.addEventListener("htmx:afterRequest", (event) => {
    var _a, _b, _c;
    const el = (_b = (_a = event.target) === null || _a === void 0 ? void 0 : _a.closest) === null || _b === void 0 ? void 0 : _b.call(_a, "[data-reload-after-request]");
    if (el && ((_c = event.detail) === null || _c === void 0 ? void 0 : _c.successful)) {
        globalThis.location.reload();
    }
});
// [data-params-from-query]: 페이지 최초 로드 fragment 요청에 현재 URL 쿼리를 병합한다.
// 필터 조건이 URL 에 남아 있으면(아래 data-sync-url 로 기록됨) 새로고침/공유 시 그대로 복원된다.
// URL 의 키는 hx-include(전역 기간 입력 등)로 들어온 같은 키를 덮어쓴다.
document.addEventListener("htmx:configRequest", (event) => {
    var _a, _b;
    const el = (_a = event.detail) === null || _a === void 0 ? void 0 : _a.elt;
    if (!((_b = el === null || el === void 0 ? void 0 : el.matches) === null || _b === void 0 ? void 0 : _b.call(el, "[data-params-from-query]")))
        return;
    const merged = {};
    new URLSearchParams(globalThis.location.search).forEach((value, key) => {
        (merged[key] = merged[key] || []).push(value);
    });
    for (const key in merged) {
        event.detail.parameters[key] =
            merged[key].length > 1 ? merged[key] : merged[key][0];
    }
});
// [data-sync-url="<fragment 경로>"]: 화면 래퍼에 지정한 목록 엔드포인트로의 GET 이 성공하면
// 그 조회 조건을 페이지 URL 에 반영한다(replaceState — 히스토리 오염 없음).
// 요청 주체(elt)는 스왑 대상 div 일 수 있으므로, 스왑에서 살아남는 래퍼에서 경로를 대조한다.
document.addEventListener("htmx:afterRequest", (event) => {
    var _a, _b, _c, _d, _e;
    const el = (_a = event.detail) === null || _a === void 0 ? void 0 : _a.elt;
    if (!((_b = event.detail) === null || _b === void 0 ? void 0 : _b.successful))
        return;
    if (((_c = event.detail.requestConfig) === null || _c === void 0 ? void 0 : _c.verb) !== "get")
        return;
    const syncRoot = (_d = el === null || el === void 0 ? void 0 : el.closest) === null || _d === void 0 ? void 0 : _d.call(el, "[data-sync-url]");
    if (!syncRoot)
        return;
    const responseUrl = ((_e = event.detail.xhr) === null || _e === void 0 ? void 0 : _e.responseURL) || "";
    let pathname = "";
    let query = "";
    try {
        const parsed = new URL(responseUrl);
        pathname = parsed.pathname;
        query = parsed.search;
    }
    catch (e) {
        return;
    }
    if (pathname !== syncRoot.getAttribute("data-sync-url"))
        return;
    globalThis.history.replaceState(null, "", globalThis.location.pathname + query);
});
// [data-page-param-from-query="page"]: 현재 URL 쿼리의 페이지 번호를 요청 파라미터로 전달 (없으면 1)
document.addEventListener("htmx:configRequest", (event) => {
    var _a, _b, _c;
    const key = (_c = (_b = (_a = event.detail) === null || _a === void 0 ? void 0 : _a.elt) === null || _b === void 0 ? void 0 : _b.dataset) === null || _c === void 0 ? void 0 : _c.pageParamFromQuery;
    if (key) {
        event.detail.parameters[key] =
            new URLSearchParams(globalThis.location.search).get(key) || "1";
    }
});
// [data-overlay] 레이어 닫기: X 버튼([data-overlay-close]), 배경 클릭, ESC
document.addEventListener("click", (event) => {
    var _a, _b, _c;
    const target = event.target;
    const closeButton = (_a = target.closest) === null || _a === void 0 ? void 0 : _a.call(target, "[data-overlay-close]");
    if (closeButton) {
        (_b = closeButton.closest("[data-overlay]")) === null || _b === void 0 ? void 0 : _b.remove();
        return;
    }
    if ((_c = target.matches) === null || _c === void 0 ? void 0 : _c.call(target, "[data-overlay]")) {
        target.remove();
    }
});
document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape")
        return;
    const overlays = document.querySelectorAll("[data-overlay]");
    if (overlays.length)
        overlays[overlays.length - 1].remove();
});
// [data-empty-widen-range]: 빈 상태 CTA — 같은 화면의 기간 프리셋 '전체' 버튼을 눌러 기간을 넓힌다
document.addEventListener("click", (event) => {
    var _a, _b;
    const cta = (_b = (_a = event.target).closest) === null || _b === void 0 ? void 0 : _b.call(_a, "[data-empty-widen-range]");
    if (!cta)
        return;
    const allButton = document.querySelector('[data-picker-action="set"][data-picker-arg="0"]');
    allButton === null || allButton === void 0 ? void 0 : allButton.click();
});
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
