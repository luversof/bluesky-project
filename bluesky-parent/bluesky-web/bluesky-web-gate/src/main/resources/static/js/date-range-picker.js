"use strict";
function fmtDate(d) {
    return (d.getFullYear() +
        "-" +
        String(d.getMonth() + 1).padStart(2, "0") +
        "-" +
        String(d.getDate()).padStart(2, "0"));
}
const DateRangePicker = (function () {
    function create(cfg) {
        const _s = { start: "", end: "", mode: "" };
        const isCallback = () => typeof cfg.onApply === "function";
        const activeClass = () => cfg.activeClass || "btn-primary";
        const el = (id) => (id ? document.getElementById(id) : null);
        const btns = (root) => cfg.btnClass
            ? Array.from((root || document).querySelectorAll("." + cfg.btnClass))
            : [];
        function clearActive(root) {
            btns(root).forEach((b) => b.classList.remove(activeClass()));
        }
        const prevClassName = cfg.prevClass || "date-range-prev";
        const nextClassName = cfg.nextClass || "date-range-next";
        function canShift(dir) {
            try {
                const start = getStart();
                const end = getEnd();
                const mode = getMode();
                if (!start || mode === "all")
                    return false;
                const maxDate = new Date(maxDateStr() + "T00:00:00");
                const minDate = cfg.minDate ? new Date(cfg.minDate + "T00:00:00") : null;
                // Quick boundary checks: if current view already reaches data edge,
                // disallow shifting further in that direction.
                try {
                    const sDate = start ? new Date(start + "T00:00:00") : null;
                    const eDate = end ? new Date(end + "T00:00:00") : null;
                    if (dir > 0 && eDate && eDate >= maxDate)
                        return false;
                    if (dir < 0 && sDate && minDate && sDate <= minDate)
                        return false;
                }
                catch (e) { }
                const isMtd = mode === "mtd";
                const isYtd = !isMtd && (mode === "ytd" || (mode === "" && start.slice(5) === "01-01"));
                if (isMtd) {
                    const curFirst = new Date(start + "T00:00:00");
                    const newFirst = new Date(curFirst.getFullYear(), curFirst.getMonth() + dir, 1);
                    const thisMonthFirst = new Date(maxDate.getFullYear(), maxDate.getMonth(), 1);
                    if (dir > 0 && newFirst > thisMonthFirst)
                        return false;
                    if (dir < 0 && minDate)
                        return newFirst >= minDate;
                    return true;
                }
                if (isYtd) {
                    const newYear = Number.parseInt(start.slice(0, 4), 10) + dir;
                    if (dir > 0 && newYear > maxDate.getFullYear())
                        return false;
                    if (dir < 0 && minDate) {
                        const ns = new Date(newYear + "-01-01T00:00:00");
                        return ns >= minDate;
                    }
                    return true;
                }
                if (mode && !isNaN(+mode) && +mode > 0) {
                    const months = +mode;
                    const s = new Date(start + "T00:00:00");
                    const e = end ? new Date(end + "T00:00:00") : new Date(s);
                    s.setMonth(s.getMonth() + dir * months);
                    e.setMonth(e.getMonth() + dir * months);
                    if (dir > 0 && s > maxDate)
                        return false;
                    if (dir < 0 && minDate)
                        return s >= minDate;
                    return true;
                }
                // Free range
                if (!end)
                    return false;
                const s = new Date(start + "T00:00:00");
                const e = new Date(end + "T00:00:00");
                const ms = e.getTime() - s.getTime();
                if (ms <= 0)
                    return false;
                const ns = new Date(s.getTime() + dir * ms);
                const ne = new Date(e.getTime() + dir * ms);
                // when shifting forward, new end must not exceed maxDate
                if (dir > 0 && ne > maxDate)
                    return false;
                // when shifting backward, new start must not be before minDate
                if (dir < 0 && minDate)
                    return ns >= minDate;
                return true;
            }
            catch (e) {
                return false;
            }
        }
        function updatePrevNextState() {
            try {
                const root = cfg.rootSelector ? (document.querySelector(cfg.rootSelector) || document) : document;
                const prevEls = Array.from(root.querySelectorAll("." + prevClassName));
                const nextEls = Array.from(root.querySelectorAll("." + nextClassName));
                const start = getStart();
                const end = getEnd();
                const mode = getMode();
                const maxDate = new Date(maxDateStr() + "T00:00:00");
                const minDate = cfg.minDate
                    ? new Date(cfg.minDate + "T00:00:00")
                    : null;
                let disablePrev = !canShift(-1);
                let disableNext = !canShift(1);
                prevEls.forEach((el) => {
                    el.disabled = disablePrev;
                    if (disablePrev) {
                        el.classList.add("opacity-40");
                        el.setAttribute("aria-disabled", "true");
                        // ensure visual dimming even if template used inline styles
                        try {
                            el.style.opacity = "0.2";
                        }
                        catch (e) { }
                    }
                    else {
                        el.classList.remove("opacity-40");
                        el.removeAttribute("aria-disabled");
                        try {
                            el.style.opacity = "";
                        }
                        catch (e) { }
                    }
                });
                nextEls.forEach((el) => {
                    el.disabled = disableNext;
                    if (disableNext) {
                        el.classList.add("opacity-40");
                        el.setAttribute("aria-disabled", "true");
                        try {
                            el.style.opacity = "0.2";
                        }
                        catch (e) { }
                    }
                    else {
                        el.classList.remove("opacity-40");
                        el.removeAttribute("aria-disabled");
                        try {
                            el.style.opacity = "";
                        }
                        catch (e) { }
                    }
                });
            }
            catch (e) {
                // swallow
            }
        }
        function getStart() {
            if (isCallback())
                return _s.start;
            const dEl = el(cfg.startId);
            if (dEl && dEl.value)
                return dEl.value;
            // fallback to instant input if date input empty
            const instEl = el(cfg.instantStartId);
            if (instEl && instEl.value) {
                try {
                    const dt = new Date(instEl.value);
                    if (!isNaN(dt.getTime()))
                        return fmtDate(dt);
                }
                catch (e) { }
            }
            return "";
        }
        function getEnd() {
            if (isCallback())
                return _s.end;
            const dEl = el(cfg.endId);
            if (dEl && dEl.value)
                return dEl.value;
            const instEl = el(cfg.instantEndId);
            if (instEl && instEl.value) {
                try {
                    const dt = new Date(instEl.value);
                    if (!isNaN(dt.getTime()))
                        return fmtDate(new Date(dt.getFullYear(), dt.getMonth(), dt.getDate()));
                }
                catch (e) { }
            }
            return "";
        }
        function getMode() {
            var _a;
            return isCallback()
                ? _s.mode
                : ((_a = el(cfg.rangeModeId)) === null || _a === void 0 ? void 0 : _a.value) || "";
        }
        function maxDateStr() {
            return cfg.maxDate || fmtDate(new Date());
        }
        function applyRange(startStr, endStr, modeStr) {
            var _a, _b, _c;
            try {
                if (isCallback()) {
                    _s.start = startStr;
                    _s.end = endStr;
                    _s.mode = modeStr;
                    cfg.onApply(startStr, endStr, modeStr);
                    try {
                        if (cfg.globalKey && typeof sessionStorage !== "undefined") {
                            const tz = ((_a = Intl === null || Intl === void 0 ? void 0 : Intl.DateTimeFormat) === null || _a === void 0 ? void 0 : _a.call(Intl).resolvedOptions().timeZone) || null;
                            sessionStorage.setItem(cfg.globalKey, JSON.stringify({
                                start: startStr || "",
                                end: endStr || "",
                                mode: modeStr || "",
                                timeZone: tz || "",
                            }));
                        }
                    }
                    catch (e) { }
                    // ensure prev/next buttons reflect new state in callback mode as well
                    try {
                        updatePrevNextState();
                        // run again after a short delay to let charts/DOM finish animating
                        setTimeout(() => {
                            try {
                                updatePrevNextState();
                            }
                            catch (e) { }
                        }, 80);
                    }
                    catch (e) { }
                }
                else {
                    const se = el(cfg.startId);
                    const ee = el(cfg.endId);
                    const me = el(cfg.rangeModeId);
                    if (se)
                        se.value = startStr || "";
                    if (ee)
                        ee.value = endStr || "";
                    if (me)
                        me.value = modeStr || "";
                    try {
                        function localDateToInstantIso(ds, addDays) {
                            if (!ds)
                                return "";
                            const parts = ds.split("-");
                            const y = Number.parseInt(parts[0], 10);
                            const m = Number.parseInt(parts[1], 10) - 1;
                            const d = Number.parseInt(parts[2], 10);
                            const dt = new Date(y, m, d + (addDays || 0), 0, 0, 0, 0);
                            return dt.toISOString();
                        }
                        if (cfg.instantStartId) {
                            const instSe = el(cfg.instantStartId);
                            if (instSe)
                                instSe.value = startStr
                                    ? localDateToInstantIso(startStr, 0)
                                    : "";
                        }
                        if (cfg.instantEndId) {
                            const instEe = el(cfg.instantEndId);
                            if (instEe)
                                instEe.value = endStr ? localDateToInstantIso(endStr, 1) : "";
                        }
                        try {
                            const tz = ((_b = Intl === null || Intl === void 0 ? void 0 : Intl.DateTimeFormat) === null || _b === void 0 ? void 0 : _b.call(Intl).resolvedOptions().timeZone) || "UTC";
                            if (cfg.timeZoneId) {
                                const tzEl = el(cfg.timeZoneId);
                                if (tzEl)
                                    tzEl.value = tz || "UTC";
                            }
                        }
                        catch (e) { }
                        // persist selection to sessionStorage even when we're using form submit mode
                        try {
                            if (cfg.globalKey && typeof sessionStorage !== "undefined") {
                                const tz2 = ((_c = Intl === null || Intl === void 0 ? void 0 : Intl.DateTimeFormat) === null || _c === void 0 ? void 0 : _c.call(Intl).resolvedOptions().timeZone) || null;
                                sessionStorage.setItem(cfg.globalKey, JSON.stringify({
                                    start: startStr || "",
                                    end: endStr || "",
                                    mode: modeStr || "",
                                    timeZone: tz2 || "",
                                }));
                            }
                        }
                        catch (e) { }
                    }
                    catch (e) { }
                    try {
                        updatePrevNextState();
                        setTimeout(() => { try {
                            updatePrevNextState();
                        }
                        catch (e) { } }, 80);
                    }
                    catch (e) { }
                    const form = el(cfg.formId);
                    if (form) {
                        if (typeof form.requestSubmit === "function") {
                            form.requestSubmit();
                        }
                        else {
                            try {
                                const ev = new Event("submit", {
                                    bubbles: true,
                                    cancelable: true,
                                });
                                const prevented = !form.dispatchEvent(ev);
                                if (!prevented) {
                                    const tmp = document.createElement("button");
                                    tmp.type = "submit";
                                    tmp.style.display = "none";
                                    form.appendChild(tmp);
                                    tmp.click();
                                    tmp.remove();
                                }
                            }
                            catch (e) {
                                try {
                                    const tmp2 = document.createElement("button");
                                    tmp2.type = "submit";
                                    tmp2.style.display = "none";
                                    form.appendChild(tmp2);
                                    tmp2.click();
                                    tmp2.remove();
                                }
                                catch (e2) {
                                    form.submit();
                                }
                            }
                        }
                    }
                }
            }
            catch (e) { }
        }
        // Apply range to inputs and update UI without submitting the form.
        function applyRangeNoSubmit(startStr, endStr, modeStr) {
            var _a;
            try {
                const se = el(cfg.startId);
                const ee = el(cfg.endId);
                const me = el(cfg.rangeModeId);
                if (se)
                    se.value = startStr || "";
                if (ee)
                    ee.value = endStr || "";
                if (me)
                    me.value = modeStr || "";
                try {
                    function localDateToInstantIso(ds, addDays) {
                        if (!ds)
                            return "";
                        const parts = ds.split("-");
                        const y = Number.parseInt(parts[0], 10);
                        const m = Number.parseInt(parts[1], 10) - 1;
                        const d = Number.parseInt(parts[2], 10);
                        const dt = new Date(y, m, d + (addDays || 0), 0, 0, 0, 0);
                        return dt.toISOString();
                    }
                    if (cfg.instantStartId) {
                        const instSe = el(cfg.instantStartId);
                        if (instSe)
                            instSe.value = startStr ? localDateToInstantIso(startStr, 0) : "";
                    }
                    if (cfg.instantEndId) {
                        const instEe = el(cfg.instantEndId);
                        if (instEe)
                            instEe.value = endStr ? localDateToInstantIso(endStr, 1) : "";
                    }
                    try {
                        const tz = ((_a = Intl === null || Intl === void 0 ? void 0 : Intl.DateTimeFormat) === null || _a === void 0 ? void 0 : _a.call(Intl).resolvedOptions().timeZone) || "UTC";
                        if (cfg.timeZoneId) {
                            const tzEl = el(cfg.timeZoneId);
                            if (tzEl)
                                tzEl.value = tz || "UTC";
                        }
                    }
                    catch (e) { }
                }
                catch (e) { }
                try {
                    updatePrevNextState();
                    setTimeout(() => { try {
                        updatePrevNextState();
                    }
                    catch (e) { } }, 80);
                }
                catch (e) { }
            }
            catch (e) { }
        }
        function doSet(months, btn) {
            clearActive();
            if (btn) {
                // add active style to clicked button and remove ghost style
                try {
                    btn.classList.add(activeClass());
                    btn.classList.remove("btn-ghost");
                }
                catch (e) { }
            }
            const maxStr = maxDateStr();
            const maxDate = new Date(maxStr + "T00:00:00");
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            let startStr = "", endStr = "", modeStr = "";
            if (months === 0) {
                startStr = "";
                endStr = "";
                modeStr = "all";
            }
            else if (months === "mtd") {
                startStr = fmtDate(new Date(maxDate.getFullYear(), maxDate.getMonth(), 1));
                endStr = maxStr;
                modeStr = "mtd";
            }
            else if (months === "ytd") {
                startStr = maxDate.getFullYear() + "-01-01";
                endStr = maxStr;
                modeStr = "ytd";
            }
            else {
                const curEnd = getEnd();
                const curStart = getStart();
                const todayStr = fmtDate(today);
                const atDataEnd = !curEnd || curEnd >= todayStr;
                const atDataStart = !atDataEnd && !!cfg.minDate && !!curStart && curStart <= cfg.minDate;
                if (atDataStart) {
                    const minD = new Date(cfg.minDate + "T00:00:00");
                    let e = new Date(minD);
                    e.setMonth(e.getMonth() + months);
                    if (e > maxDate)
                        e = new Date(maxDate);
                    startStr = fmtDate(minD);
                    endStr = fmtDate(e);
                }
                else {
                    const s = new Date(maxDate);
                    s.setMonth(s.getMonth() - months);
                    startStr = fmtDate(s);
                    endStr = maxStr;
                }
                modeStr = String(months);
            }
            // ensure visual state: toggle classes within configured rootSelector
            try {
                const root = cfg.rootSelector
                    ? document.querySelector(cfg.rootSelector) || document
                    : document;
                btns(root).forEach((b) => {
                    b.classList.remove(activeClass());
                    b.classList.add("btn-ghost");
                });
                if (btn) {
                    btn.classList.add(activeClass());
                    btn.classList.remove("btn-ghost");
                }
            }
            catch (e) { }
            applyRange(startStr, endStr, modeStr);
        }
        function doJumpToEdge(direction) {
            const mode = getMode();
            if (mode === "all")
                return;
            const maxStr = maxDateStr();
            const maxDate = new Date(maxStr + "T00:00:00");
            clearActive();
            let startStr = "", endStr = "";
            if (direction === "end") {
                endStr = maxStr;
                if (mode && !isNaN(Number(mode)) && +mode > 0) {
                    const s = new Date(maxDate);
                    s.setMonth(s.getMonth() - +mode);
                    startStr = fmtDate(s);
                }
                else if (mode === "mtd") {
                    startStr = fmtDate(new Date(maxDate.getFullYear(), maxDate.getMonth(), 1));
                }
                else if (mode === "ytd") {
                    startStr = maxDate.getFullYear() + "-01-01";
                }
                else {
                    const cs = getStart(), ce = getEnd();
                    if (!cs || !ce)
                        return;
                    const ms = new Date(ce + "T00:00:00").getTime() -
                        new Date(cs + "T00:00:00").getTime();
                    if (ms <= 0)
                        return;
                    startStr = fmtDate(new Date(maxDate.getTime() - ms));
                }
            }
            else {
                if (!cfg.minDate)
                    return;
                const minD = new Date(cfg.minDate + "T00:00:00");
                if (mode && !isNaN(Number(mode)) && +mode > 0) {
                    let e = new Date(minD);
                    e.setMonth(e.getMonth() + +mode);
                    if (e > maxDate)
                        e = new Date(maxDate);
                    startStr = fmtDate(minD);
                    endStr = fmtDate(e);
                }
                else if (mode === "mtd") {
                    const first = new Date(minD.getFullYear(), minD.getMonth(), 1);
                    const last = new Date(minD.getFullYear(), minD.getMonth() + 1, 0);
                    startStr = fmtDate(first);
                    endStr = fmtDate(last > maxDate ? maxDate : last);
                }
                else if (mode === "ytd") {
                    const minYear = minD.getFullYear();
                    startStr = minYear + "-01-01";
                    endStr =
                        minYear === maxDate.getFullYear() ? maxStr : minYear + "-12-31";
                }
                else {
                    const cs = getStart(), ce = getEnd();
                    if (!cs || !ce)
                        return;
                    const ms = new Date(ce + "T00:00:00").getTime() -
                        new Date(cs + "T00:00:00").getTime();
                    if (ms <= 0)
                        return;
                    let ne = new Date(minD.getTime() + ms);
                    if (ne > maxDate)
                        ne = new Date(maxDate);
                    startStr = fmtDate(minD);
                    endStr = fmtDate(ne);
                }
            }
            applyRange(startStr, endStr, getMode());
        }
        function doShift(dir) {
            const start = getStart(), end = getEnd(), mode = getMode();
            if (!start || mode === "all")
                return;
            const maxStr = maxDateStr();
            const maxDate = new Date(maxStr + "T00:00:00");
            clearActive();
            const isMtd = mode === "mtd";
            const isYtd = !isMtd &&
                (mode === "ytd" || (mode === "" && start.slice(5) === "01-01"));
            let newStart = "", newEnd = "";
            const newMode = isMtd ? "1" : isYtd ? "12" : mode;
            if (isMtd) {
                const curFirst = new Date(start + "T00:00:00");
                const newFirst = new Date(curFirst.getFullYear(), curFirst.getMonth() + dir, 1);
                const newLast = new Date(curFirst.getFullYear(), curFirst.getMonth() + dir + 1, 0);
                const thisMonthFirst = new Date(maxDate.getFullYear(), maxDate.getMonth(), 1);
                if (dir > 0 && newFirst > thisMonthFirst)
                    return;
                newStart = fmtDate(newFirst);
                newEnd = fmtDate(newLast > maxDate ? maxDate : newLast);
            }
            else if (isYtd) {
                const newYear = Number.parseInt(start.slice(0, 4), 10) + dir;
                if (dir > 0 && newYear > maxDate.getFullYear())
                    return;
                newStart = newYear + "-01-01";
                newEnd =
                    newYear === maxDate.getFullYear() ? maxStr : newYear + "-12-31";
            }
            else if (mode && !isNaN(+mode) && +mode > 0) {
                const months = +mode;
                const s = new Date(start + "T00:00:00");
                const e = end ? new Date(end + "T00:00:00") : new Date(s);
                s.setMonth(s.getMonth() + dir * months);
                e.setMonth(e.getMonth() + dir * months);
                if (dir > 0 && s > maxDate)
                    return;
                if (dir > 0 && e > maxDate)
                    e.setTime(maxDate.getTime());
                newStart = fmtDate(s);
                newEnd = fmtDate(e);
            }
            else {
                if (!end)
                    return;
                const s = new Date(start + "T00:00:00");
                const e = new Date(end + "T00:00:00");
                const ms = e.getTime() - s.getTime();
                if (ms <= 0)
                    return;
                const ns = new Date(s.getTime() + dir * ms);
                const ne = new Date(e.getTime() + dir * ms);
                if (dir > 0 && ns > maxDate)
                    return;
                if (dir > 0 && ne > maxDate)
                    ne.setTime(maxDate.getTime());
                newStart = fmtDate(ns);
                newEnd = fmtDate(ne);
            }
            // reactivate appropriate button inside configured root
            try {
                const root = cfg.rootSelector
                    ? document.querySelector(cfg.rootSelector) || document
                    : document;
                btns(root).forEach((b) => {
                    b.classList.remove(activeClass());
                    b.classList.add("btn-ghost");
                });
                Array.from(root.querySelectorAll("." + cfg.btnClass)).forEach((b) => {
                    if ((b.getAttribute("onclick") || "").indexOf("set(" + newMode + ",") !== -1) {
                        b.classList.add(activeClass());
                        b.classList.remove("btn-ghost");
                    }
                });
            }
            catch (e) { }
            applyRange(newStart, newEnd, newMode);
        }
        try {
            updatePrevNextState();
        }
        catch (e) { }
        // on init: prefer sessionStorage value if cfg.globalKey provided
        try {
            if (cfg.globalKey) {
                const raw = sessionStorage.getItem(cfg.globalKey);
                if (raw) {
                    try {
                        const obj = JSON.parse(raw);
                        if (obj) {
                            const root = cfg.rootSelector
                                ? document.querySelector(cfg.rootSelector) || document
                                : document;
                            if (obj.mode) {
                                // try to find a matching button inside the configured root
                                let foundBtn = null;
                                try {
                                    const candidates = Array.from(root.querySelectorAll("." + (cfg.btnClass || "")));
                                    for (const c of candidates) {
                                        const onclick = c.getAttribute("onclick") || "";
                                        if (onclick.indexOf("set(" + obj.mode + ",") !== -1 ||
                                            onclick.indexOf("set('" + obj.mode + "'") !== -1) {
                                            foundBtn = c;
                                            break;
                                        }
                                    }
                                }
                                catch (e) { }
                                // Set visual state and inputs only — do NOT submit form to avoid HTMX reload loops
                                try {
                                    // activate matching button visuals
                                    const rootEl = cfg.rootSelector
                                        ? document.querySelector(cfg.rootSelector) || document
                                        : document;
                                    btns(rootEl).forEach((b) => {
                                        b.classList.remove(activeClass());
                                        b.classList.add("btn-ghost");
                                    });
                                    if (foundBtn) {
                                        foundBtn.classList.add(activeClass());
                                        foundBtn.classList.remove("btn-ghost");
                                    }
                                }
                                catch (e) { }
                                applyRangeNoSubmit(obj.start || "", obj.end || "", obj.mode || "");
                            }
                            else if (obj.start && obj.end) {
                                applyRangeNoSubmit(obj.start, obj.end, obj.mode || "");
                            }
                        }
                    }
                    catch (e) { }
                }
            }
        }
        catch (e) { }
        return {
            set: (months, btn) => doSet(months, btn),
            jumpToEdge: (direction) => doJumpToEdge(direction),
            shift: (dir) => doShift(dir),
            setMinDate: (d) => {
                cfg.minDate = d;
            },
            setMaxDate: (d) => {
                cfg.maxDate = d;
            },
            setState: (start, end, mode) => {
                if (isCallback()) {
                    _s.start = start;
                    _s.end = end;
                    _s.mode = mode;
                }
                clearActive();
            },
            getState: () => ({ start: getStart(), end: getEnd(), mode: getMode() }),
            canShift: (dir) => {
                try {
                    return canShift(dir);
                }
                catch (e) {
                    return false;
                }
            },
        };
    }
    return { create, fmt: fmtDate };
})();
// expose to global
globalThis.DateRangePicker = DateRangePicker;
