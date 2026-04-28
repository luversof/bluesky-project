"use strict";
function fmtDate(d) {
  return (
    d.getFullYear() +
    "-" +
    String(d.getMonth() + 1).padStart(2, "0") +
    "-" +
    String(d.getDate()).padStart(2, "0")
  );
}
const DateRangePicker = (function () {
  function create(cfg) {
    const _s = { start: "", end: "", mode: "" };
    const isCallback = () => typeof cfg.onApply === "function";
    const activeClass = () => cfg.activeClass || "btn-primary";
    const el = (id) => (id ? document.getElementById(id) : null);
    const btns = (root) =>
      cfg.btnClass
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
        if (!start || mode === "all") return false;
        const maxDate = new Date(maxDateStr() + "T00:00:00");
        const minDate = cfg.minDate
          ? new Date(cfg.minDate + "T00:00:00")
          : null;
        // Quick boundary checks: if current view already reaches data edge,
        // disallow shifting further in that direction.
        try {
          const sDate = start ? new Date(start + "T00:00:00") : null;
          const eDate = end ? new Date(end + "T00:00:00") : null;
          if (dir > 0 && eDate && eDate >= maxDate) return false;
          if (dir < 0 && sDate && minDate && sDate <= minDate) return false;
        } catch (e) {}
        const isMtd = mode === "mtd";
        const isYtd =
          !isMtd &&
          (mode === "ytd" || (mode === "" && start.slice(5) === "01-01"));
        if (isMtd) {
          const curFirst = new Date(start + "T00:00:00");
          const newFirst = new Date(
            curFirst.getFullYear(),
            curFirst.getMonth() + dir,
            1,
          );
          const thisMonthFirst = new Date(
            maxDate.getFullYear(),
            maxDate.getMonth(),
            1,
          );
          if (dir > 0 && newFirst > thisMonthFirst) return false;
          if (dir < 0 && minDate) return newFirst >= minDate;
          return true;
        }
        if (isYtd) {
          const newYear = Number.parseInt(start.slice(0, 4), 10) + dir;
          if (dir > 0 && newYear > maxDate.getFullYear()) return false;
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
          if (dir > 0 && s > maxDate) return false;
          if (dir < 0 && minDate) return s >= minDate;
          return true;
        }
        // Free range
        if (!end) return false;
        const s = new Date(start + "T00:00:00");
        const e = new Date(end + "T00:00:00");
        const ms = e.getTime() - s.getTime();
        if (ms <= 0) return false;
        const ns = new Date(s.getTime() + dir * ms);
        const ne = new Date(e.getTime() + dir * ms);
        // when shifting forward, new end must not exceed maxDate
        if (dir > 0 && ne > maxDate) return false;
        // when shifting backward, new start must not be before minDate
        if (dir < 0 && minDate) return ns >= minDate;
        return true;
      } catch (e) {
        return false;
      }
    }
    function updatePrevNextState() {
      try {
        const root = cfg.rootSelector
          ? document.querySelector(cfg.rootSelector) || document
          : document;
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
            try {
              el.style.opacity = "0.2";
            } catch (e) {}
          } else {
            el.classList.remove("opacity-40");
            el.removeAttribute("aria-disabled");
            try {
              el.style.opacity = "";
            } catch (e) {}
          }
        });
        nextEls.forEach((el) => {
          el.disabled = disableNext;
          if (disableNext) {
            el.classList.add("opacity-40");
            el.setAttribute("aria-disabled", "true");
            try {
              el.style.opacity = "0.2";
            } catch (e) {}
          } else {
            el.classList.remove("opacity-40");
            el.removeAttribute("aria-disabled");
            try {
              el.style.opacity = "";
            } catch (e) {}
          }
        });
      } catch (e) {
        // swallow
      }
    }
    function getStart() {
      if (isCallback()) return _s.start;
      const dEl = el(cfg.startId);
      if (dEl && dEl.value) return dEl.value;
      const instEl = el(cfg.instantStartId);
      if (instEl && instEl.value) {
        try {
          const dt = new Date(instEl.value);
          if (!isNaN(dt.getTime())) return fmtDate(dt);
        } catch (e) {}
      }
      return "";
    }
    function getEnd() {
      if (isCallback()) return _s.end;
      const dEl = el(cfg.endId);
      if (dEl && dEl.value) return dEl.value;
      const instEl = el(cfg.instantEndId);
      if (instEl && instEl.value) {
        try {
          const dt = new Date(instEl.value);
          if (!isNaN(dt.getTime()))
            return fmtDate(
              new Date(dt.getFullYear(), dt.getMonth(), dt.getDate()),
            );
        } catch (e) {}
      }
      return "";
    }
    function getMode() {
      var _a;
      return isCallback()
        ? _s.mode
        : ((_a = el(cfg.rangeModeId)) === null || _a === void 0
            ? void 0
            : _a.value) || "";
    }
    function maxDateStr() {
      return cfg.maxDate || fmtDate(new Date());
    }
    function applyRange(startStr, endStr, modeStr) {
      var _a, _b, _c;
      function localDateToInstantIso(ds, addDays) {
        if (!ds) return "";
        const parts = ds.split("-");
        const y = Number.parseInt(parts[0], 10);
        const m = Number.parseInt(parts[1], 10) - 1;
        const d = Number.parseInt(parts[2], 10);
        const dt = new Date(y, m, d + (addDays || 0), 0, 0, 0, 0);
        return dt.toISOString();
      }
      function setGlobalHiddenInputs(sStr, eStr, mStr) {
        try {
          const gStart = document.getElementById("globalStartInstantInput");
          const gEnd = document.getElementById("globalEndInstantInput");
          const gTz = document.getElementById("globalTimeZoneInput");
          const gMode = document.getElementById("globalRangeModeInput");
          const tzVal =
            (Intl &&
              Intl.DateTimeFormat &&
              Intl.DateTimeFormat().resolvedOptions &&
              Intl.DateTimeFormat().resolvedOptions().timeZone) ||
            (Intl &&
              Intl.DateTimeFormat &&
              Intl.DateTimeFormat().call &&
              Intl.DateTimeFormat().call(Intl).resolvedOptions().timeZone) ||
            "";
          if (gStart) gStart.value = sStr ? localDateToInstantIso(sStr, 0) : "";
          if (gEnd) gEnd.value = eStr ? localDateToInstantIso(eStr, 1) : "";
          if (gTz) gTz.value = tzVal || "";
          if (gMode) gMode.value = mStr || "";
        } catch (e) {}
      }
      try {
        if (isCallback()) {
          _s.start = startStr;
          _s.end = endStr;
          _s.mode = modeStr;
          cfg.onApply(startStr, endStr, modeStr);
          try {
            if (cfg.globalKey && typeof sessionStorage !== "undefined") {
              const tz =
                ((_a =
                  Intl === null || Intl === void 0
                    ? void 0
                    : Intl.DateTimeFormat) === null || _a === void 0
                  ? void 0
                  : _a.call(Intl).resolvedOptions().timeZone) || null;
              sessionStorage.setItem(
                cfg.globalKey,
                JSON.stringify({
                  start: startStr || "",
                  end: endStr || "",
                  mode: modeStr || "",
                  timeZone: tz || "",
                }),
              );
            }
          } catch (e) {}
          try {
            // also update layout-level hidden inputs so HTMX initial loads
            // that include them will send the requested instants to server
            setGlobalHiddenInputs(startStr || "", endStr || "", modeStr || "");
          } catch (e) {}
          try {
            if (
              typeof window !== "undefined" &&
              typeof window.dispatchEvent === "function"
            ) {
              window.dispatchEvent(
                new CustomEvent("globalDateRange:changed", {
                  detail: {
                    start: startStr || "",
                    end: endStr || "",
                    mode: modeStr || "",
                  },
                }),
              );
            }
          } catch (e) {}
          try {
            updatePrevNextState();
            setTimeout(() => {
              try {
                updatePrevNextState();
              } catch (e) {}
            }, 80);
          } catch (e) {}
        } else {
          const se = el(cfg.startId);
          const ee = el(cfg.endId);
          const me = el(cfg.rangeModeId);
          if (se) se.value = startStr || "";
          if (ee) ee.value = endStr || "";
          if (me) me.value = modeStr || "";
          try {
            function localDateToInstantIso(ds, addDays) {
              if (!ds) return "";
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
              const tz =
                ((_b =
                  Intl === null || Intl === void 0
                    ? void 0
                    : Intl.DateTimeFormat) === null || _b === void 0
                  ? void 0
                  : _b.call(Intl).resolvedOptions().timeZone) || "UTC";
              if (cfg.timeZoneId) {
                const tzEl = el(cfg.timeZoneId);
                if (tzEl) tzEl.value = tz || "UTC";
              }
            } catch (e) {}
            try {
              if (cfg.globalKey && typeof sessionStorage !== "undefined") {
                const tz2 =
                  ((_c =
                    Intl === null || Intl === void 0
                      ? void 0
                      : Intl.DateTimeFormat) === null || _c === void 0
                    ? void 0
                    : _c.call(Intl).resolvedOptions().timeZone) || null;
                sessionStorage.setItem(
                  cfg.globalKey,
                  JSON.stringify({
                    start: startStr || "",
                    end: endStr || "",
                    mode: modeStr || "",
                    timeZone: tz2 || "",
                  }),
                );
              }
            } catch (e) {}
            try {
              // update layout-level hidden inputs as well
              setGlobalHiddenInputs(
                startStr || "",
                endStr || "",
                modeStr || "",
              );
            } catch (e) {}
            try {
              if (
                typeof window !== "undefined" &&
                typeof window.dispatchEvent === "function"
              ) {
                window.dispatchEvent(
                  new CustomEvent("globalDateRange:changed", {
                    detail: {
                      start: startStr || "",
                      end: endStr || "",
                      mode: modeStr || "",
                    },
                  }),
                );
              }
            } catch (e) {}
          } catch (e) {}
          try {
            updatePrevNextState();
            setTimeout(() => {
              try {
                updatePrevNextState();
              } catch (e) {}
            }, 80);
          } catch (e) {}
          const form = el(cfg.formId);
          // Record appliedKey before submitting so newly-loaded fragments
          // can detect that this exact globalDateRange was already applied
          // and skip re-applying (prevents double-submit/refresh overriding).
          try {
            const appliedKey =
              "__globalDateRangeApplied:" +
              (cfg.formId || cfg.rootSelector || cfg.btnClass || "");
            try {
              var appliedValue = null;
              try {
                if (cfg.globalKey && typeof sessionStorage !== "undefined") {
                  appliedValue = sessionStorage.getItem(cfg.globalKey);
                }
              } catch (e) {}
              if (!appliedValue) {
                try {
                  appliedValue = JSON.stringify({
                    start: startStr || "",
                    end: endStr || "",
                    mode: modeStr || "",
                  });
                } catch (e2) {
                  appliedValue = "1";
                }
              }
              try {
                sessionStorage.setItem(appliedKey, appliedValue);
              } catch (e3) {}
            } catch (e) {}
          } catch (e) {}
          if (form) {
            if (typeof form.requestSubmit === "function") form.requestSubmit();
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
              } catch (e) {
                try {
                  const tmp2 = document.createElement("button");
                  tmp2.type = "submit";
                  tmp2.style.display = "none";
                  form.appendChild(tmp2);
                  tmp2.click();
                  tmp2.remove();
                } catch (e2) {
                  form.submit();
                }
              }
            }
          }
        }
      } catch (e) {}
    }
    // Apply range to inputs and update UI without submitting the form.
    function applyRangeNoSubmit(startStr, endStr, modeStr) {
      var _a;
      try {
        const se = el(cfg.startId);
        const ee = el(cfg.endId);
        const me = el(cfg.rangeModeId);
        if (se) se.value = startStr || "";
        if (ee) ee.value = endStr || "";
        if (me) me.value = modeStr || "";
        try {
          function localDateToInstantIso(ds, addDays) {
            if (!ds) return "";
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
            const tz =
              ((_a =
                Intl === null || Intl === void 0
                  ? void 0
                  : Intl.DateTimeFormat) === null || _a === void 0
                ? void 0
                : _a.call(Intl).resolvedOptions().timeZone) || "UTC";
            if (cfg.timeZoneId) {
              const tzEl = el(cfg.timeZoneId);
              if (tzEl) tzEl.value = tz || "UTC";
            }
          } catch (e) {}
          try {
            // update global hidden inputs so HTMX includes current instants
            const gStart = document.getElementById("globalStartInstantInput");
            const gEnd = document.getElementById("globalEndInstantInput");
            const gTz = document.getElementById("globalTimeZoneInput");
            const gMode = document.getElementById("globalRangeModeInput");
            if (gStart)
              gStart.value = startStr ? localDateToInstantIso(startStr, 0) : "";
            if (gEnd)
              gEnd.value = endStr ? localDateToInstantIso(endStr, 1) : "";
            if (gTz)
              gTz.value =
                ((_a = Intl) === null || _a === void 0
                  ? void 0
                  : _a.DateTimeFormat) && Intl.DateTimeFormat().resolvedOptions
                  ? Intl.DateTimeFormat().resolvedOptions().timeZone
                  : "";
            if (gMode) gMode.value = modeStr || "";
          } catch (e) {}
        } catch (e) {}
        try {
          updatePrevNextState();
          setTimeout(() => {
            try {
              updatePrevNextState();
            } catch (e) {}
          }, 80);
        } catch (e) {}
      } catch (e) {}
    }
    function doSet(months, btn) {
      clearActive();
      if (btn) {
        try {
          btn.classList.add(activeClass());
          btn.classList.remove("btn-ghost");
        } catch (e) {}
      }
      const maxStr = maxDateStr();
      const maxDate = new Date(maxStr + "T00:00:00");
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      let startStr = "",
        endStr = "",
        modeStr = "";
      if (months === 0) {
        startStr = "";
        endStr = "";
        modeStr = "all";
      } else if (months === "mtd") {
        startStr = fmtDate(
          new Date(maxDate.getFullYear(), maxDate.getMonth(), 1),
        );
        endStr = maxStr;
        modeStr = "mtd";
      } else if (months === "ytd") {
        startStr = maxDate.getFullYear() + "-01-01";
        endStr = maxStr;
        modeStr = "ytd";
      } else {
        const curEnd = getEnd();
        const curStart = getStart();
        const todayStr = fmtDate(today);
        const atDataEnd = !curEnd || curEnd >= todayStr;
        const atDataStart =
          !atDataEnd && !!cfg.minDate && !!curStart && curStart <= cfg.minDate;
        if (atDataStart) {
          const minD = new Date(cfg.minDate + "T00:00:00");
          let e = new Date(minD);
          e.setMonth(e.getMonth() + months);
          if (e > maxDate) e = new Date(maxDate);
          startStr = fmtDate(minD);
          endStr = fmtDate(e);
        } else {
          const s = new Date(maxDate);
          s.setMonth(s.getMonth() - months);
          startStr = fmtDate(s);
          endStr = maxStr;
        }
        modeStr = String(months);
      }
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
      } catch (e) {}
      applyRange(startStr, endStr, modeStr);
    }
    function doJumpToEdge(direction) {
      const mode = getMode();
      if (mode === "all") return;
      const maxStr = maxDateStr();
      const maxDate = new Date(maxStr + "T00:00:00");
      clearActive();
      let startStr = "",
        endStr = "";
      if (direction === "end") {
        endStr = maxStr;
        if (mode && !isNaN(Number(mode)) && +mode > 0) {
          const s = new Date(maxDate);
          s.setMonth(s.getMonth() - +mode);
          startStr = fmtDate(s);
        } else if (mode === "mtd") {
          startStr = fmtDate(
            new Date(maxDate.getFullYear(), maxDate.getMonth(), 1),
          );
        } else if (mode === "ytd") {
          startStr = maxDate.getFullYear() + "-01-01";
        } else {
          const cs = getStart(),
            ce = getEnd();
          if (!cs || !ce) return;
          const ms =
            new Date(ce + "T00:00:00").getTime() -
            new Date(cs + "T00:00:00").getTime();
          if (ms <= 0) return;
          startStr = fmtDate(new Date(maxDate.getTime() - ms));
        }
      } else {
        if (!cfg.minDate) return;
        const minD = new Date(cfg.minDate + "T00:00:00");
        if (mode && !isNaN(Number(mode)) && +mode > 0) {
          let e = new Date(minD);
          e.setMonth(e.getMonth() + +mode);
          if (e > maxDate) e = new Date(maxDate);
          startStr = fmtDate(minD);
          endStr = fmtDate(e);
        } else if (mode === "mtd") {
          const first = new Date(minD.getFullYear(), minD.getMonth(), 1);
          const last = new Date(minD.getFullYear(), minD.getMonth() + 1, 0);
          startStr = fmtDate(first);
          endStr = fmtDate(last > maxDate ? maxDate : last);
        } else if (mode === "ytd") {
          const minYear = minD.getFullYear();
          startStr = minYear + "-01-01";
          endStr =
            minYear === maxDate.getFullYear() ? maxStr : minYear + "-12-31";
        } else {
          const cs = getStart(),
            ce = getEnd();
          if (!cs || !ce) return;
          const ms =
            new Date(ce + "T00:00:00").getTime() -
            new Date(cs + "T00:00:00").getTime();
          if (ms <= 0) return;
          let ne = new Date(minD.getTime() + ms);
          if (ne > maxDate) ne = new Date(maxDate);
          startStr = fmtDate(minD);
          endStr = fmtDate(ne);
        }
      }
      applyRange(startStr, endStr, getMode());
    }
    function doShift(dir) {
      const start = getStart(),
        end = getEnd(),
        mode = getMode();
      if (!start || mode === "all") return;
      const maxStr = maxDateStr();
      const maxDate = new Date(maxStr + "T00:00:00");
      clearActive();
      const isMtd = mode === "mtd";
      const isYtd =
        !isMtd &&
        (mode === "ytd" || (mode === "" && start.slice(5) === "01-01"));
      let newStart = "",
        newEnd = "";
      let newMode = isMtd ? "1" : isYtd ? "12" : mode;
      if (isMtd) {
        const curFirst = new Date(start + "T00:00:00");
        const newFirst = new Date(
          curFirst.getFullYear(),
          curFirst.getMonth() + dir,
          1,
        );
        const newLast = new Date(
          curFirst.getFullYear(),
          curFirst.getMonth() + dir + 1,
          0,
        );
        const thisMonthFirst = new Date(
          maxDate.getFullYear(),
          maxDate.getMonth(),
          1,
        );
        if (dir > 0 && newFirst > thisMonthFirst) return;
        newStart = fmtDate(newFirst);
        newEnd = fmtDate(newLast > maxDate ? maxDate : newLast);
      } else if (isYtd) {
        const newYear = Number.parseInt(start.slice(0, 4), 10) + dir;
        if (dir > 0 && newYear > maxDate.getFullYear()) return;
        newStart = newYear + "-01-01";
        newEnd =
          newYear === maxDate.getFullYear() ? maxStr : newYear + "-12-31";
      } else if (mode && !isNaN(+mode) && +mode > 0) {
        const months = +mode;
        // Special-case 12-month presets: if the current view appears to be
        // a calendar year (start == Jan-01 or end == Dec-31) treat the shift
        // as a calendar-year shift instead of a relative month shift. This
        // avoids incorrect results when the picker's internal start/end were
        // snapped to label boundaries (e.g. '2024-12-01').
        try {
          const isCalendarYearView =
            (start && start.slice(5) === "01-01") ||
            (end && end.slice(5) === "12-31");
          if (months === 12 && isCalendarYearView) {
            // Anchor to the year from either start (Jan-01) or end (Dec-31).
            var anchorYear = null;
            try {
              if (start && start.slice(5) === "01-01")
                anchorYear = Number.parseInt(start.slice(0, 4), 10);
              else if (end && end.slice(5) === "12-31")
                anchorYear = Number.parseInt(end.slice(0, 4), 10);
            } catch (e) {
              anchorYear = null;
            }
            if (anchorYear === null || isNaN(anchorYear)) {
              // Fallback to using start's year if parsing failed
              anchorYear = start
                ? Number.parseInt(start.slice(0, 4), 10)
                : null;
            }
            if (anchorYear !== null && !isNaN(anchorYear)) {
              const newYear = anchorYear + dir;
              newStart = newYear + "-01-01";
              newEnd =
                newYear === maxDate.getFullYear() ? maxStr : newYear + "-12-31";
              newMode =
                newEnd === maxStr && newStart.slice(5) === "01-01"
                  ? "ytd"
                  : String(months);
            } else {
              // If we couldn't determine anchor year, fall back to relative shift
              const s = new Date(start + "T00:00:00");
              const e = end ? new Date(end + "T00:00:00") : new Date(s);
              s.setMonth(s.getMonth() + dir * months);
              e.setMonth(e.getMonth() + dir * months);
              if (dir > 0 && s > maxDate) return;
              if (dir > 0 && e > maxDate) e.setTime(maxDate.getTime());
              newStart = fmtDate(s);
              newEnd = fmtDate(e);
            }
          } else {
            const s = new Date(start + "T00:00:00");
            const e = end ? new Date(end + "T00:00:00") : new Date(s);
            s.setMonth(s.getMonth() + dir * months);
            e.setMonth(e.getMonth() + dir * months);
            if (dir > 0 && s > maxDate) return;
            if (dir > 0 && e > maxDate) e.setTime(maxDate.getTime());
            newStart = fmtDate(s);
            newEnd = fmtDate(e);
          }
        } catch (e) {
          // On any unexpected error, gracefully bail out
          return;
        }
      } else {
        if (!end) return;
        const s = new Date(start + "T00:00:00");
        const e = new Date(end + "T00:00:00");
        const ms = e.getTime() - s.getTime();
        if (ms <= 0) return;
        const ns = new Date(s.getTime() + dir * ms);
        const ne = new Date(e.getTime() + dir * ms);
        if (dir > 0 && ns > maxDate) return;
        if (dir > 0 && ne > maxDate) ne.setTime(maxDate.getTime());
        newStart = fmtDate(ns);
        newEnd = fmtDate(ne);
      }
      // If the shifted range matches "this year" (Jan-01 .. maxDate), prefer 'ytd' mode
      try {
        if (newStart && newEnd) {
          if (newEnd === maxStr && newStart.slice(5) === "01-01") {
            newMode = "ytd";
          }
        }
      } catch (e) {}
      try {
        const root = cfg.rootSelector
          ? document.querySelector(cfg.rootSelector) || document
          : document;
        btns(root).forEach((b) => {
          b.classList.remove(activeClass());
          b.classList.add("btn-ghost");
        });
        Array.from(root.querySelectorAll("." + cfg.btnClass)).forEach((b) => {
          try {
            const onclick =
              b.getAttribute && b.getAttribute("onclick")
                ? b.getAttribute("onclick") || ""
                : "";
            const m = onclick.match(
              /set\(\s*(?:'([^']+)'|"([^"]+)"|([^,\)\s]+))/,
            );
            const arg = m ? m[1] || m[2] || m[3] : null;
            if (arg === newMode) {
              b.classList.add(activeClass());
              b.classList.remove("btn-ghost");
            }
          } catch (e) {}
        });
      } catch (e) {}
      applyRange(newStart, newEnd, newMode);
    }
    try {
      updatePrevNextState();
    } catch (e) {}
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
              // try to find a matching button inside the configured root
              let foundBtn = null;
              try {
                const candidates = Array.from(
                  root.querySelectorAll("." + (cfg.btnClass || "")),
                );
                for (const c of candidates) {
                  const onclick = c.getAttribute("onclick") || "";
                  if (obj.mode) {
                    if (
                      onclick.indexOf("set(" + obj.mode + ",") !== -1 ||
                      onclick.indexOf("set('" + obj.mode + "'") !== -1
                    ) {
                      foundBtn = c;
                      break;
                    }
                  }
                }
                // If explicit mode not present but start/end exist, try to infer which quick-button
                if (!foundBtn && obj.start && obj.end) {
                  for (const c of candidates) {
                    try {
                      const onclick = c.getAttribute("onclick") || "";
                      const m = onclick.match(
                        /set\(\s*(?:'([^']+)'|"([^"]+)"|([^,\)\s]+))/,
                      );
                      if (!m) continue;
                      const arg = m[1] || m[2] || m[3];
                      let months = arg;
                      if (/^\d+$/.test(String(arg))) months = Number(arg);
                      const computeRange = (monthsParam) => {
                        const maxStr = maxDateStr();
                        const maxDate = new Date(maxStr + "T00:00:00");
                        const today = new Date();
                        today.setHours(0, 0, 0, 0);
                        let startStr = "",
                          endStr = "",
                          modeStr = "";
                        if (monthsParam === 0) {
                          startStr = "";
                          endStr = "";
                          modeStr = "all";
                        } else if (monthsParam === "mtd") {
                          startStr = fmtDate(
                            new Date(
                              maxDate.getFullYear(),
                              maxDate.getMonth(),
                              1,
                            ),
                          );
                          endStr = maxStr;
                          modeStr = "mtd";
                        } else if (monthsParam === "ytd") {
                          startStr = maxDate.getFullYear() + "-01-01";
                          endStr = maxStr;
                          modeStr = "ytd";
                        } else {
                          const curEnd = getEnd();
                          const curStart = getStart();
                          const todayStr = fmtDate(today);
                          const atDataEnd = !curEnd || curEnd >= todayStr;
                          const atDataStart =
                            !atDataEnd &&
                            !!cfg.minDate &&
                            !!curStart &&
                            curStart <= cfg.minDate;
                          if (atDataStart) {
                            const minD = new Date(cfg.minDate + "T00:00:00");
                            let e = new Date(minD);
                            e.setMonth(e.getMonth() + monthsParam);
                            if (e > maxDate) e = new Date(maxDate);
                            startStr = fmtDate(minD);
                            endStr = fmtDate(e);
                          } else {
                            const s = new Date(maxDate);
                            s.setMonth(s.getMonth() - monthsParam);
                            startStr = fmtDate(s);
                            endStr = maxStr;
                          }
                          modeStr = String(monthsParam);
                        }
                        return { start: startStr, end: endStr, mode: modeStr };
                      };
                      const r = computeRange(months);
                      if (r.start === obj.start && r.end === obj.end) {
                        foundBtn = c;
                        obj.mode = r.mode || obj.mode;
                        break;
                      }
                    } catch (e) {
                      /* ignore candidate errors */
                    }
                  }
                }
              } catch (e) {}
              // Set visual state and inputs only — do NOT submit form to avoid HTMX reload loops
              try {
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
              } catch (e) {}
              applyRangeNoSubmit(
                obj.start || "",
                obj.end || "",
                obj.mode || "",
              );
              // One-time apply: call callback or submit form once per-fragment to ensure data loads
              try {
                const appliedKey =
                  "__globalDateRangeApplied:" +
                  (cfg.formId || cfg.rootSelector || cfg.btnClass || "");
                if (!sessionStorage.getItem(appliedKey)) {
                  if (isCallback()) {
                    try {
                      _s.start = obj.start || "";
                      _s.end = obj.end || "";
                      _s.mode = obj.mode || "";
                      if (typeof cfg.onApply === "function") {
                        cfg.onApply(_s.start, _s.end, _s.mode);
                      }
                    } catch (e) {}
                  } else {
                    const formEl = el(cfg.formId);
                    if (formEl) {
                      try {
                        if (typeof formEl.requestSubmit === "function")
                          formEl.requestSubmit();
                        else formEl.submit();
                      } catch (e) {}
                    } else {
                      try {
                        applyRange(
                          obj.start || "",
                          obj.end || "",
                          obj.mode || "",
                        );
                      } catch (e) {}
                    }
                  }
                  try {
                    var appliedValue = null;
                    try {
                      if (
                        cfg.globalKey &&
                        typeof sessionStorage !== "undefined"
                      ) {
                        appliedValue = sessionStorage.getItem(cfg.globalKey);
                      }
                    } catch (e2) {}
                    if (!appliedValue) {
                      // Fallback to storing the explicit start/end/mode object
                      try {
                        appliedValue = JSON.stringify({
                          start: obj.start || "",
                          end: obj.end || "",
                          mode: obj.mode || "",
                        });
                      } catch (e3) {
                        appliedValue = "1";
                      }
                    }
                    sessionStorage.setItem(appliedKey, appliedValue);
                  } catch (e) {}
                }
              } catch (e) {}
            }
          } catch (e) {}
        }
      }
    } catch (e) {}
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
          if (mode !== undefined && mode !== null && mode !== "") {
            _s.mode = mode;
          }
        }
        // Only update visual quick-button state when an explicit mode value
        // (non-empty) is provided. Avoid clearing active button when caller
        // passes an empty-string mode (common when only start/end were stored).
        if (mode !== undefined && mode !== null && mode !== "") {
          try {
            const root = cfg.rootSelector
              ? document.querySelector(cfg.rootSelector) || document
              : document;
            btns(root).forEach((b) => {
              try {
                b.classList.remove(activeClass());
                b.classList.add("btn-ghost");
              } catch (e) {}
            });
            if (mode) {
              try {
                Array.from(
                  root.querySelectorAll("." + (cfg.btnClass || "")),
                ).forEach((b) => {
                  const onclick =
                    b.getAttribute && b.getAttribute("onclick")
                      ? b.getAttribute("onclick") || ""
                      : "";
                  if (
                    onclick.indexOf("set(" + mode + ",") !== -1 ||
                    onclick.indexOf("set('" + mode + "'") !== -1
                  ) {
                    try {
                      b.classList.add(activeClass());
                      b.classList.remove("btn-ghost");
                    } catch (e) {}
                  }
                });
              } catch (e) {}
            }
          } catch (e) {}
        }
      },
      getState: () => ({ start: getStart(), end: getEnd(), mode: getMode() }),
      canShift: (dir) => {
        try {
          return canShift(dir);
        } catch (e) {
          return false;
        }
      },
    };
  }
  return { create, fmt: fmtDate };
})();
// expose to global
globalThis.DateRangePicker = DateRangePicker;
