"use strict";
// 상세 조회 필터(<details data-detail-filter>)의 접힘/펼침 상태를 전역 공통으로 유지한다.
// - localStorage 단일 키로 모든 페이지의 필터가 같은 상태를 공유(페이지별 설정 불필요).
// - htmx 스왑(기간/필터 변경)으로 필터가 다시 렌더돼도 직전 상태를 복원한다.
// import/export 없이 classic <script src> 로 로드한다(globalDateRange.ts 와 동일).
(function () {
    var KEY = "stockDetailFilterOpen";
    var SELECTOR = "[data-detail-filter]";
    function isOpen() {
        try {
            var v = typeof localStorage !== "undefined" ? localStorage.getItem(KEY) : null;
            return v === null ? true : v === "1"; // 기본값: 펼침
        }
        catch (e) {
            return true;
        }
    }
    function applyState() {
        var open = isOpen();
        var els = document.querySelectorAll(SELECTOR);
        for (var i = 0; i < els.length; i++) {
            var el = els[i];
            if (el.open !== open)
                el.open = open;
        }
    }
    // <details> 의 toggle 이벤트는 버블링하지 않으므로 capture 단계에서 위임 수신한다.
    document.addEventListener("toggle", function (e) {
        var t = e.target;
        if (!t || typeof t.matches !== "function" || !t.matches(SELECTOR))
            return;
        try {
            localStorage.setItem(KEY, t.open ? "1" : "0");
        }
        catch (err) { }
        applyState(); // 같은 페이지의 다른 필터도 동일 상태로 동기화
    }, true);
    function init() {
        applyState();
    }
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    }
    else {
        init();
    }
    // htmx 스왑 직후 복원
    document.addEventListener("htmx:afterSwap", function () {
        applyState();
    });
})();
