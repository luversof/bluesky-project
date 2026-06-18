"use strict";
// 전역 날짜 범위(sessionStorage 'globalDateRange') ↔ 페이지 내 hidden input / HTMX 폼 동기화.
// 원래 stockLayout.jte 에 인라인 <script> 로 있던 로직을 그대로 TS 모듈로 옮긴 것.
// 파싱 중 동기 실행되어야 하므로(아래 htmx 의 load 트리거보다 먼저 hidden input 을 채워야 함)
// classic <script src> 로 로드한다.
function input(id) {
    return document.getElementById(id);
}
// 1) 최초 로드 시 sessionStorage 값을 hidden input 에 반영
(function () {
    try {
        const raw = typeof sessionStorage !== "undefined"
            ? sessionStorage.getItem("globalDateRange")
            : null;
        if (raw) {
            const obj = JSON.parse(raw);
            const localToIso = (ds, addDays) => {
                if (!ds)
                    return "";
                const parts = ds.split("-");
                const y = Number(parts[0]);
                const m = Number(parts[1]) - 1;
                const d = Number(parts[2]) + (addDays || 0);
                return new Date(y, m, d, 0, 0, 0, 0).toISOString();
            };
            const gStart = input("globalStartInstantInput");
            const gEnd = input("globalEndInstantInput");
            const gTz = input("globalTimeZoneInput");
            const gMode = input("globalRangeModeInput");
            if (gStart)
                gStart.value = obj.start ? localToIso(obj.start, 0) : "";
            if (gEnd)
                gEnd.value = obj.end ? localToIso(obj.end, 1) : "";
            if (gTz)
                gTz.value = obj.timeZone || "";
            if (gMode)
                gMode.value = obj.mode || "";
        }
    }
    catch (e) { }
})();
// 2) globalDateRange:changed 이벤트를 받아 폼/프래그먼트로 전파
(function () {
    try {
        const localToIso = (ds, addDays) => {
            if (!ds)
                return "";
            const parts = ds.split("-");
            const y = Number(parts[0]);
            const m = Number(parts[1]) - 1;
            const d = Number(parts[2]) + (addDays || 0);
            const dt = new Date(y, m, d, 0, 0, 0, 0);
            return isNaN(dt.getTime()) ? "" : dt.toISOString();
        };
        function applyGlobalDate(obj) {
            try {
                const gStart = input("globalStartInstantInput");
                const gEnd = input("globalEndInstantInput");
                const gTz = input("globalTimeZoneInput");
                const gMode = input("globalRangeModeInput");
                let nextTimeZone = obj.timeZone || obj.timezone || (gTz && gTz.value) || "";
                if (!nextTimeZone) {
                    try {
                        const savedRaw = typeof sessionStorage !== "undefined"
                            ? sessionStorage.getItem("globalDateRange")
                            : null;
                        if (savedRaw) {
                            const saved = JSON.parse(savedRaw);
                            nextTimeZone =
                                saved && (saved.timeZone || saved.timezone)
                                    ? saved.timeZone || saved.timezone || ""
                                    : "";
                        }
                    }
                    catch (e) { }
                }
                if (gStart)
                    gStart.value = obj.start ? localToIso(obj.start, 0) : "";
                if (gEnd)
                    gEnd.value = obj.end ? localToIso(obj.end, 1) : "";
                if (gTz)
                    gTz.value = nextTimeZone || "";
                if (gMode)
                    gMode.value = obj.mode || "";
                // update per-form inputs so HTMX includes the new range when forms submit
                document
                    .querySelectorAll('input[name="startDate"]')
                    .forEach((inp) => {
                    inp.value = obj.start ? localToIso(obj.start, 0) : "";
                });
                document
                    .querySelectorAll('input[name="endDate"]')
                    .forEach((inp) => {
                    inp.value = obj.end ? localToIso(obj.end, 1) : "";
                });
                document
                    .querySelectorAll('input[name="timeZone"]')
                    .forEach((inp) => {
                    inp.value = nextTimeZone || "";
                });
                document
                    .querySelectorAll('input[name="rangeMode"]')
                    .forEach((inp) => {
                    inp.value = obj.mode || "";
                });
            }
            catch (e) { }
        }
        // Listen for global date changes and propagate to forms/fragments
        window.addEventListener("globalDateRange:changed", (ev) => {
            try {
                const detail = (ev === null || ev === void 0 ? void 0 : ev.detail) || null;
                let obj = null;
                if (detail &&
                    (detail.start !== undefined || detail.end !== undefined)) {
                    obj = {
                        start: detail.start || "",
                        end: detail.end || "",
                        mode: detail.mode || "",
                        timeZone: detail.timeZone || detail.timezone || "",
                    };
                }
                else {
                    const raw = typeof sessionStorage !== "undefined"
                        ? sessionStorage.getItem("globalDateRange")
                        : null;
                    if (raw) {
                        try {
                            obj = JSON.parse(raw);
                        }
                        catch (e) {
                            obj = null;
                        }
                    }
                }
                if (!obj)
                    return;
                applyGlobalDate(obj);
                // Refresh trade-history once for this global range (avoid duplicates)
                try {
                    const w = window;
                    const tkey = (obj.start || "") + "::" + (obj.end || "");
                    if (!w.lastGlobalTradeKey || w.lastGlobalTradeKey !== tkey) {
                        try {
                            w.lastGlobalTradeKey = tkey;
                        }
                        catch (e) { }
                        const tradeHistoryPanel = document.getElementById("trade-history-panel");
                        if (tradeHistoryPanel &&
                            typeof w.htmx !== "undefined" &&
                            w.htmx &&
                            typeof w.htmx.ajax === "function") {
                            let url = "/stock/htmx/trade-history";
                            if (obj.start && obj.end)
                                url += "?from=" + obj.start + "&to=" + obj.end;
                            w.htmx.ajax("GET", url, {
                                target: tradeHistoryPanel,
                                swap: "outerHTML",
                            });
                        }
                    }
                }
                catch (e) { }
                // Trigger common HTMX-driven search forms to reload (if present)
                try {
                    const formsToTrigger = [
                        "tradeSearchForm",
                        "realizedProfitSearchForm",
                    ];
                    formsToTrigger.forEach((id) => {
                        try {
                            if (detail && detail.sourceFormId && detail.sourceFormId === id)
                                return;
                            const f = document.getElementById(id);
                            if (!f)
                                return;
                            const hasHx = f.hasAttribute("hx-get") ||
                                f.hasAttribute("hx-post") ||
                                f.querySelector("[hx-get], [hx-post], [hx-put], [hx-delete]");
                            if (hasHx) {
                                if (typeof f.requestSubmit === "function") {
                                    f.requestSubmit();
                                }
                                else {
                                    f.dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));
                                }
                            }
                        }
                        catch (e) { }
                    });
                }
                catch (e) { }
            }
            catch (e) { }
        }, false);
    }
    catch (e) { }
})();
