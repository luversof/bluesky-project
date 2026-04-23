// DateRangePicker (TypeScript)
type PickerState = { start: string; end: string; mode: string };

function fmtDate(d: Date): string {
  return (
    d.getFullYear() +
    "-" +
    String(d.getMonth() + 1).padStart(2, "0") +
    "-" +
    String(d.getDate()).padStart(2, "0")
  );
}

const DateRangePicker = (function () {
  function create(cfg: any) {
    const _s: PickerState = { start: "", end: "", mode: "" };
    try { console.debug && console.debug("DRP:create", { globalKey: cfg && cfg.globalKey, root: cfg && cfg.rootSelector }); } catch(e) {}

    const isCallback = () => typeof cfg.onApply === "function";
    const activeClass = () => cfg.activeClass || "btn-primary";

    const el = (id?: string) => (id ? document.getElementById(id) : null);
    const btns = (root?: Element | Document) =>
      cfg.btnClass ? Array.from((root || document).querySelectorAll("." + cfg.btnClass)) : [] as Element[];

    function clearActive(root?: Element | Document) {
      btns(root).forEach((b) => b.classList.remove(activeClass()));
    }

    const prevClassName = cfg.prevClass || "date-range-prev";
    const nextClassName = cfg.nextClass || "date-range-next";

    function updatePrevNextState() {
      try {
        const prevEls = Array.from(document.querySelectorAll("." + prevClassName)) as HTMLButtonElement[];
        const nextEls = Array.from(document.querySelectorAll("." + nextClassName)) as HTMLButtonElement[];
        const start = getStart();
        const end = getEnd();
        const mode = getMode();
        const maxDate = new Date(maxDateStr() + "T00:00:00");
        const minDate = cfg.minDate ? new Date(cfg.minDate + "T00:00:00") : null;
        let disablePrev = false;
        let disableNext = false;
        if (mode === "all" || !start || !end) {
          disablePrev = true;
          disableNext = true;
        } else {
          const sDate = new Date(start + "T00:00:00");
          const eDate = new Date(end + "T00:00:00");
          disableNext = eDate >= maxDate;
          if (minDate) {
            disablePrev = sDate <= minDate;
          }
        }
        prevEls.forEach((el) => {
          el.disabled = disablePrev;
          if (disablePrev) {
            el.classList.add("opacity-40");
            el.setAttribute("aria-disabled", "true");
          } else {
            el.classList.remove("opacity-40");
            el.removeAttribute("aria-disabled");
          }
        });
        nextEls.forEach((el) => {
          el.disabled = disableNext;
          if (disableNext) {
            el.classList.add("opacity-40");
            el.setAttribute("aria-disabled", "true");
          } else {
            el.classList.remove("opacity-40");
            el.removeAttribute("aria-disabled");
          }
        });
      } catch (e) {
        // swallow
      }
    }

    function getStart(): string {
      return isCallback() ? _s.start : (el(cfg.startId) as HTMLInputElement | null)?.value || "";
    }
    function getEnd(): string {
      return isCallback() ? _s.end : (el(cfg.endId) as HTMLInputElement | null)?.value || "";
    }
    function getMode(): string {
      return isCallback() ? _s.mode : (el(cfg.rangeModeId) as HTMLInputElement | null)?.value || "";
    }

    function maxDateStr() {
      return cfg.maxDate || fmtDate(new Date());
    }

    function applyRange(startStr: string, endStr: string, modeStr: string) {
      try {
        try { console.debug && console.debug("DRP:applyRange", { start: startStr, end: endStr, mode: modeStr, globalKey: cfg && cfg.globalKey }); } catch(e) {}
        if (isCallback()) {
          _s.start = startStr;
          _s.end = endStr;
          _s.mode = modeStr;
          cfg.onApply(startStr, endStr, modeStr);
          try {
            if (cfg.globalKey && typeof sessionStorage !== "undefined") {
              const tz = Intl?.DateTimeFormat?.().resolvedOptions().timeZone || null;
              sessionStorage.setItem(
                cfg.globalKey,
                JSON.stringify({ start: startStr || "", end: endStr || "", mode: modeStr || "", timeZone: tz || "" })
              );
            }
          } catch (e) {}
        } else {
          const se = el(cfg.startId) as HTMLInputElement | null;
          const ee = el(cfg.endId) as HTMLInputElement | null;
          const me = el(cfg.rangeModeId) as HTMLInputElement | null;
          if (se) se.value = startStr || "";
          if (ee) ee.value = endStr || "";
          if (me) me.value = modeStr || "";
          try {
            function localDateToInstantIso(ds: string, addDays?: number) {
              if (!ds) return "";
              const parts = ds.split("-");
              const y = Number.parseInt(parts[0], 10);
              const m = Number.parseInt(parts[1], 10) - 1;
              const d = Number.parseInt(parts[2], 10);
              const dt = new Date(y, m, d + (addDays || 0), 0, 0, 0, 0);
              return dt.toISOString();
            }
            if (cfg.instantStartId) {
              const instSe = el(cfg.instantStartId) as HTMLInputElement | null;
              if (instSe) instSe.value = startStr ? localDateToInstantIso(startStr, 0) : "";
            }
            if (cfg.instantEndId) {
              const instEe = el(cfg.instantEndId) as HTMLInputElement | null;
              if (instEe) instEe.value = endStr ? localDateToInstantIso(endStr, 1) : "";
            }
            try {
              const tz = Intl?.DateTimeFormat?.().resolvedOptions().timeZone || "UTC";
              if (cfg.timeZoneId) {
                const tzEl = el(cfg.timeZoneId) as HTMLInputElement | null;
                if (tzEl) tzEl.value = tz || "UTC";
              }
            } catch (e) {}
          } catch (e) {}
          try { updatePrevNextState(); } catch (e) {}
          const form = el(cfg.formId) as HTMLFormElement | null;
          if (form) {
            if (typeof form.requestSubmit === "function") {
              form.requestSubmit();
            } else {
              try {
                const ev = new Event("submit", { bubbles: true, cancelable: true });
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

    function doSet(months: any, btn: Element | null) {
      try { console.debug && console.debug("DRP:doSet", { months: months, btn: btn, root: cfg && cfg.rootSelector }); } catch(e) {}
      clearActive();
      if (btn) {
        // add active style to clicked button and remove ghost style
        try { btn.classList.add(activeClass()); btn.classList.remove('btn-ghost'); } catch(e) {}
      }
      const maxStr = maxDateStr();
      const maxDate = new Date(maxStr + "T00:00:00");
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      let startStr = "", endStr = "", modeStr = "";
      if (months === 0) {
        startStr = ""; endStr = ""; modeStr = "all";
      } else if (months === "mtd") {
        startStr = fmtDate(new Date(maxDate.getFullYear(), maxDate.getMonth(), 1));
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
        const atDataStart = !atDataEnd && !!cfg.minDate && !!curStart && curStart <= cfg.minDate;
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
      // ensure visual state: toggle classes within configured rootSelector
      try {
        const root = cfg.rootSelector ? (document.querySelector(cfg.rootSelector) || document) : document;
        btns(root).forEach((b) => { b.classList.remove(activeClass()); b.classList.add('btn-ghost'); });
        if (btn) { btn.classList.add(activeClass()); btn.classList.remove('btn-ghost'); }
      } catch(e) {}
      applyRange(startStr, endStr, modeStr);
    }

    function doJumpToEdge(direction: string) {
      const mode = getMode();
      if (mode === "all") return;
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
        } else if (mode === "mtd") {
          startStr = fmtDate(new Date(maxDate.getFullYear(), maxDate.getMonth(), 1));
        } else if (mode === "ytd") {
          startStr = maxDate.getFullYear() + "-01-01";
        } else {
          const cs = getStart(), ce = getEnd();
          if (!cs || !ce) return;
          const ms = new Date(ce + "T00:00:00").getTime() - new Date(cs + "T00:00:00").getTime();
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
          endStr = minYear === maxDate.getFullYear() ? maxStr : minYear + "-12-31";
        } else {
          const cs = getStart(), ce = getEnd();
          if (!cs || !ce) return;
          const ms = new Date(ce + "T00:00:00").getTime() - new Date(cs + "T00:00:00").getTime();
          if (ms <= 0) return;
          let ne = new Date(minD.getTime() + ms);
          if (ne > maxDate) ne = new Date(maxDate);
          startStr = fmtDate(minD);
          endStr = fmtDate(ne);
        }
      }
      applyRange(startStr, endStr, getMode());
    }

    function doShift(dir: number) {
      try { console.debug && console.debug("DRP:doShift", { dir: dir, mode: getMode(), root: cfg && cfg.rootSelector }); } catch(e) {}
      const start = getStart(), end = getEnd(), mode = getMode();
      if (!start || mode === "all") return;
      const maxStr = maxDateStr();
      const maxDate = new Date(maxStr + "T00:00:00");
      clearActive();
      const isMtd = mode === "mtd";
      const isYtd = !isMtd && (mode === "ytd" || (mode === "" && start.slice(5) === "01-01"));
      let newStart = "", newEnd = "";
      const newMode = isMtd ? "1" : isYtd ? "12" : mode;
      if (isMtd) {
        const curFirst = new Date(start + "T00:00:00");
        const newFirst = new Date(curFirst.getFullYear(), curFirst.getMonth() + dir, 1);
        const newLast = new Date(curFirst.getFullYear(), curFirst.getMonth() + dir + 1, 0);
        const thisMonthFirst = new Date(maxDate.getFullYear(), maxDate.getMonth(), 1);
        if (dir > 0 && newFirst > thisMonthFirst) return;
        newStart = fmtDate(newFirst);
        newEnd = fmtDate(newLast > maxDate ? maxDate : newLast);
      } else if (isYtd) {
        const newYear = Number.parseInt(start.slice(0, 4), 10) + dir;
        if (dir > 0 && newYear > maxDate.getFullYear()) return;
        newStart = newYear + "-01-01";
        newEnd = newYear === maxDate.getFullYear() ? maxStr : newYear + "-12-31";
      } else if (mode && !isNaN(+mode) && +mode > 0) {
        const months = +mode;
        const s = new Date(start + "T00:00:00");
        const e = end ? new Date(end + "T00:00:00") : new Date(s);
        s.setMonth(s.getMonth() + dir * months);
        e.setMonth(e.getMonth() + dir * months);
        if (dir > 0 && s > maxDate) return;
        if (dir > 0 && e > maxDate) e.setTime(maxDate.getTime());
        newStart = fmtDate(s);
        newEnd = fmtDate(e);
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
      // reactivate appropriate button inside configured root
      try {
        const root = cfg.rootSelector ? document.querySelector(cfg.rootSelector) || document : document;
        btns(root).forEach((b: Element) => { b.classList.remove(activeClass()); b.classList.add('btn-ghost'); });
        Array.from((root as Element).querySelectorAll("." + cfg.btnClass)).forEach((b: Element) => {
          if ((b.getAttribute("onclick") || "").indexOf("set(" + newMode + ",") !== -1) {
            b.classList.add(activeClass()); b.classList.remove('btn-ghost');
          }
        });
      } catch (e) {}
      applyRange(newStart, newEnd, newMode);
    }

    try { updatePrevNextState(); } catch (e) {}

    // on init: if globalKey present, try to initialize from session (scoped by rootSelector)
    try {
      if (cfg.globalKey && !getMode() && !getStart() && !getEnd()) {
        const raw = sessionStorage.getItem(cfg.globalKey);
        if (raw) {
          try {
            const obj = JSON.parse(raw);
            if (obj) {
              try { console.debug && console.debug("DRP:initFromSession", { obj: obj, root: cfg && cfg.rootSelector }); } catch(e) {}
              const root = cfg.rootSelector ? document.querySelector(cfg.rootSelector) || document : document;
              if (obj.mode) {
                const btn = (root as Element).querySelector('.' + (cfg.btnClass || '')) as Element | null;
                doSet(obj.mode, btn || null);
              } else if (obj.start && obj.end) {
                applyRange(obj.start, obj.end, obj.mode || '');
              }
            }
          } catch (e) {}
        }
      }
    } catch (e) {}

    return {
      set: (months: any, btn: Element | null) => doSet(months, btn),
      jumpToEdge: (direction: string) => doJumpToEdge(direction),
      shift: (dir: number) => doShift(dir),
      setMinDate: (d: string) => { cfg.minDate = d; },
      setMaxDate: (d: string) => { cfg.maxDate = d; },
      setState: (start: string, end: string, mode: string) => {
        if (isCallback()) { _s.start = start; _s.end = end; _s.mode = mode; }
        clearActive();
      },
      getState: () => ({ start: getStart(), end: getEnd(), mode: getMode() })
    };
  }

  return { create, fmt: fmtDate };
})();

// expose to global
(globalThis as any).DateRangePicker = DateRangePicker as any;
